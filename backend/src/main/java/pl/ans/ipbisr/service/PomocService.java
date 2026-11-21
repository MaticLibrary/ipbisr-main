package pl.ans.ipbisr.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import org.springframework.stereotype.Service;
import pl.ans.ipbisr.domain.IloscZasobu;
import pl.ans.ipbisr.repository.WniosekRepository;

/** Pomoc humanitarna: wniosek z P3a do organizacji (P3b) i odpowiedź organizacji z powrotem do P3a. */
@Service
public class PomocService {

    private final WniosekRepository wnioski;
    private final KomunikatyService komunikaty;
    private final AudytService audyt;

    public PomocService(WniosekRepository wnioski, KomunikatyService komunikaty, AudytService audyt) {
        this.wnioski = wnioski;
        this.komunikaty = komunikaty;
        this.audyt = audyt;
    }

    /**
     * Krok P3a „Wyślij wniosek o pomoc humanitarną”: zapisuje niedobory i wysyła „Wniosek o pomoc” do P3b.
     * Zwraca numer sprawy `idWniosku` – P3a czeka na odpowiedź z tym samym kluczem (receive task).
     */
    public String wyslijWniosek(long idPlanu, List<IloscZasobu> niedobory) {
        String idWniosku = UUID.randomUUID().toString();
        wnioski.dodaj(idPlanu, niedobory);

        // lista dla formularza organizacji: ile wnioskowano i ile przyznano (na start tyle samo – organizacja poprawia)
        List<Map<String, Object>> doUzupelnienia = new ArrayList<>();
        for (IloscZasobu n : niedobory) {
            doUzupelnienia.add(Map.of("zasob", n.zasob(), "wnioskowano", n.ilosc(), "przyznano", n.ilosc()));
        }
        komunikaty.publikuj("Wniosek o pomoc", idWniosku, Map.of("idWniosku", idWniosku, "idPlanu", idPlanu,
                "niedobory", niedobory, "przyznanoIlosci", doUzupelnienia));

        Map<String, Object> szczegoly = new HashMap<>();
        szczegoly.put("planId", idPlanu);
        szczegoly.put("idWniosku", idWniosku);
        szczegoly.put("niedobory", niedobory);
        audyt.zapisz("system", "P3a", "WNIOSEK_WYSLANY", szczegoly);
        return idWniosku;
    }

    /**
     * Koniec P3b „Odpowiedź wysłana”: zapisuje, ile organizacja przyznała, i odsyła „Odpowiedź na wniosek” do P3a.
     *
     * @param przyznanoIlosci lista z formularza: `{zasob, wnioskowano, przyznano}`
     * @param termin          data dostawy `RRRR-MM-DD` z formularza (może być null)
     */
    public void zapiszOdpowiedz(String idWniosku, long idPlanu, List<Map<String, Object>> przyznanoIlosci, String termin) {
        Map<String, Double> przyznano = new LinkedHashMap<>();
        StringJoiner opis = new StringJoiner("; ");
        for (Map<String, Object> p : przyznanoIlosci) {
            String zasob = (String) p.get("zasob");
            double ile = liczba(p.get("przyznano"));
            przyznano.put(zasob, ile);
            opis.add(zasob + " " + formatuj(ile) + " z " + formatuj(liczba(p.get("wnioskowano"))));
        }
        wnioski.zapiszOdpowiedz(idPlanu, przyznano, termin == null ? null : LocalDate.parse(termin));

        // krótki opis dla analityka – pokaże go formularz „Zatwierdź plan przydziału”
        String podsumowanie = "Organizacje humanitarne przyznały: " + opis + "."
                + (termin == null ? "" : " Termin dostawy: " + termin + ".");
        Map<String, Object> zmienne = new HashMap<>();
        zmienne.put("idWniosku", idWniosku);
        zmienne.put("przyznanoIlosci", przyznanoIlosci);
        zmienne.put("termin", termin);
        zmienne.put("podsumowaniePomocy", podsumowanie);
        komunikaty.publikuj("Odpowiedź na wniosek", idWniosku, zmienne);

        Map<String, Object> szczegoly = new HashMap<>();
        szczegoly.put("planId", idPlanu);
        szczegoly.put("idWniosku", idWniosku);
        szczegoly.put("przyznano", przyznano);
        szczegoly.put("termin", termin);
        audyt.zapisz("organizacje", "P3b", "ODPOWIEDZ_NA_WNIOSEK", szczegoly);
    }

    /** Liczba z formularza może przyjść jako int albo double; brak wartości traktujemy jak 0. */
    private static double liczba(Object wartosc) {
        return wartosc == null ? 0.0 : ((Number) wartosc).doubleValue();
    }

    /** 30.0 → „30”, 0.15 → „0.15” (bez zbędnych zer). */
    private static String formatuj(double x) {
        return BigDecimal.valueOf(x).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
