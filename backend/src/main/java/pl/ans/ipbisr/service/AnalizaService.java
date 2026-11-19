package pl.ans.ipbisr.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import pl.ans.ipbisr.domain.Osoba;
import pl.ans.ipbisr.domain.WynikPriorytetu;
import pl.ans.ipbisr.domain.Zgloszenie;
import pl.ans.ipbisr.repository.EwidencjaRepository;
import pl.ans.ipbisr.repository.MieszkaniecRepository;
import pl.ans.ipbisr.repository.ZgloszenieRepository;

/** Logika procesu P1b: wyznaczenie priorytetów (DMN) i zapis wyników z uwzględnieniem zgłoszenia. */
@Service
public class AnalizaService {

    private final EwidencjaRepository ewidencja;
    private final MieszkaniecRepository mieszkancy;
    private final ZgloszenieRepository zgloszenia;
    private final PriorytetService priorytety;
    private final AudytService audyt;

    public AnalizaService(EwidencjaRepository ewidencja, MieszkaniecRepository mieszkancy,
                          ZgloszenieRepository zgloszenia, PriorytetService priorytety, AudytService audyt) {
        this.ewidencja = ewidencja;
        this.mieszkancy = mieszkancy;
        this.zgloszenia = zgloszenia;
        this.priorytety = priorytety;
        this.audyt = audyt;
    }

    /** Krok „Wyznacz potrzeby i priorytet”: tabela DMN dla KAŻDEGO mieszkańca z listy z rejestru ludności. */
    public List<Map<String, Object>> wyznacz(List<Map<String, Object>> mieszkancyZRejestru) {
        List<Map<String, Object>> wyniki = new ArrayList<>();
        for (Map<String, Object> dane : mieszkancyZRejestru) {
            wyniki.add(priorytety.oblicz(Osoba.zZmiennych(dane)).doZmiennych());
        }
        return wyniki;
    }

    /**
     * Krok „Zapisz wyniki w rejestrze potrzeb”.
     * Jeśli proces wystartował od zgłoszenia, to po decyzji zarządu (albo automatycznie, gdy zgłoszenia
     * nie trzeba było weryfikować) zmiana trafia do rejestru, a priorytet osoby jest liczony od nowa.
     *
     * @param zgloszenie zmienna procesu `zgloszenie` (null, gdy proces ruszył od timera)
     * @param decyzja    POTWIERDZONE / ODRZUCONE; null, gdy zarząd nie weryfikował
     */
    public void zapiszWyniki(List<Map<String, Object>> wyniki, Map<String, Object> zgloszenie, String decyzja,
                             String komentarz) {
        mieszkancy.zapisz(wyniki.stream().map(WynikPriorytetu::zZmiennych).toList());
        if (zgloszenie == null) {
            return;
        }
        long id = ((Number) zgloszenie.get("id")).longValue();
        Zgloszenie z = zgloszenia.znajdz(id)
                .orElseThrow(() -> new IllegalStateException("Nie znaleziono zgłoszenia o id " + id));

        boolean przyjete = !"ODRZUCONE".equals(decyzja);
        if (przyjete) {
            zastosuj(z);
        }
        zgloszenia.zakoncz(id, decyzja);

        Map<String, Object> szczegoly = new HashMap<>();
        szczegoly.put("zgloszenieId", id);
        szczegoly.put("rodzaj", z.rodzaj());
        szczegoly.put("decyzja", decyzja);
        szczegoly.put("komentarz", komentarz);
        if (decyzja != null) {
            audyt.zapisz("zarzad", "P1b", "ZWERYFIKOWANO", szczegoly);
        } else {
            audyt.zapisz("system", "P1b", "PRZYJETO_BEZ_WERYFIKACJI", szczegoly);
        }
    }

    /** Wprowadza zmianę ze zgłoszenia do rejestru ludności i przelicza priorytet każdej dotkniętej osoby. */
    private void zastosuj(Zgloszenie z) {
        List<String> pesele = z.pesel() != null ? List.of(z.pesel()) : ewidencja.peseleWBudynku(z.budynekId());
        List<WynikPriorytetu> przeliczone = new ArrayList<>();
        for (String pesel : pesele) {
            Osoba przed = ewidencja.znajdz(pesel)
                    .orElseThrow(() -> new IllegalStateException("Brak w ewidencji PESEL " + pesel));
            switch (z.rodzaj()) {
                case "SMIERC" -> ewidencja.ustawStatus(pesel, "NIE_ZYJE");
                case "ZAGINIECIE" -> ewidencja.ustawStatus(pesel, "ZAGINIONY");
                // choroba i niepełnosprawność tylko „podnoszą” stan – nie obniżamy cięższego stanu z rejestru
                case "CHOROBA" -> {
                    if ("DOBRY".equals(przed.stanZdrowia())) {
                        ewidencja.ustawStanZdrowia(pesel, "CHOROBA");
                    }
                }
                case "NIEPELNOSPRAWNOSC" -> {
                    if ("BRAK".equals(przed.niepelnosprawnosc()) || "LEKKI".equals(przed.niepelnosprawnosc())) {
                        ewidencja.ustawNiepelnosprawnosc(pesel, "UMIARKOWANY");
                    }
                }
                case "UTRATA_MIESZKANIA" -> ewidencja.ustawUtrateMieszkania(pesel);
                default -> throw new IllegalStateException("Nieznany rodzaj zgłoszenia: " + z.rodzaj());
            }
            Osoba po = ewidencja.znajdz(pesel).orElseThrow();
            przeliczone.add(priorytety.oblicz(po));
        }
        mieszkancy.zapisz(przeliczone);
    }
}
