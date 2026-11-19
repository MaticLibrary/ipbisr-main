package pl.ans.ipbisr.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;
import org.springframework.stereotype.Service;
import pl.ans.ipbisr.domain.NoweZgloszenie;
import pl.ans.ipbisr.domain.Zgloszenie;
import pl.ans.ipbisr.repository.EwidencjaRepository;
import pl.ans.ipbisr.repository.ZgloszenieRepository;

/** Zgłoszenia zdarzeń: rejestracja z API (start P1c) i krok „Przekaż zgłoszenie do centrum”. */
@Service
public class ZgloszenieService {

    static final List<String> RODZAJE = List.of(
            "CHOROBA", "NIEPELNOSPRAWNOSC", "UTRATA_MIESZKANIA", "SMIERC", "ZAGINIECIE");

    /** Szansa, że zgłoszenie inne niż śmierć/zaginięcie trafi do kontroli zarządu (opis procesu P1: 20%). */
    static final double SZANSA_KONTROLI = 0.2;

    private final ZgloszenieRepository zgloszenia;
    private final EwidencjaRepository ewidencja;
    private final KomunikatyService komunikaty;
    private final ProcesyService procesy;
    private final AudytService audyt;
    private final DoubleSupplier losowanie;

    public ZgloszenieService(ZgloszenieRepository zgloszenia, EwidencjaRepository ewidencja,
                             KomunikatyService komunikaty, ProcesyService procesy, AudytService audyt,
                             DoubleSupplier losowanie) {
        this.zgloszenia = zgloszenia;
        this.ewidencja = ewidencja;
        this.komunikaty = komunikaty;
        this.procesy = procesy;
        this.audyt = audyt;
        this.losowanie = losowanie;
    }

    /** `POST /api/zgloszenia`: sprawdza dane, zapisuje zgłoszenie i uruchamia proces P1c. */
    public Zgloszenie zarejestruj(NoweZgloszenie nowe) {
        waliduj(nowe);
        long id = zgloszenia.dodaj(nowe.pesel(), nowe.budynekId(), nowe.rodzaj(), nowe.opis());
        try {
            procesy.uruchomRejestracjeIZakonczZadanie(id);
        } catch (RuntimeException e) {
            zgloszenia.ustawStatus(id, "BLAD_PROCESU");
            throw e;
        }
        Map<String, Object> szczegoly = new HashMap<>();
        szczegoly.put("zgloszenieId", id);
        szczegoly.put("rodzaj", nowe.rodzaj());
        audyt.zapisz("operator", "P1c", "ZAREJESTROWANO", szczegoly);
        return zgloszenia.znajdz(id).orElseThrow();
    }

    private void waliduj(NoweZgloszenie nowe) {
        if (nowe.rodzaj() == null || !RODZAJE.contains(nowe.rodzaj())) {
            throw new IllegalArgumentException("Nieprawidłowy rodzaj zgłoszenia, dozwolone: " + RODZAJE);
        }
        boolean maPesel = nowe.pesel() != null && !nowe.pesel().isBlank();
        if (!maPesel && nowe.budynekId() == null) {
            throw new IllegalArgumentException("Podaj numer PESEL albo ID budynku");
        }
        // zmiana dotycząca konkretnej osoby wymaga PESEL; samym budynkiem można zgłosić tylko utratę mieszkania
        if (!maPesel && !"UTRATA_MIESZKANIA".equals(nowe.rodzaj())) {
            throw new IllegalArgumentException("Zgłoszenie rodzaju " + nowe.rodzaj() + " wymaga numeru PESEL");
        }
        if (maPesel && ewidencja.znajdz(nowe.pesel()).isEmpty()) {
            throw new IllegalArgumentException("Nie ma takiego numeru PESEL w rejestrze ludności");
        }
        if (nowe.budynekId() != null && !ewidencja.budynekIstnieje(nowe.budynekId())) {
            throw new IllegalArgumentException("Nieznany budynek o ID " + nowe.budynekId());
        }
    }

    /**
     * Krok P1c „Przekaż zgłoszenie do centrum”: losuje kontrolę (20%), zapisuje status
     * i wysyła wiadomość „Zgłoszenie zdarzenia”, od której zaczyna się proces P1b.
     */
    public void przekaz(long id) {
        Zgloszenie z = zgloszenia.znajdz(id)
                .orElseThrow(() -> new IllegalStateException("Nie znaleziono zgłoszenia o id " + id));
        boolean kontrola = losowanie.getAsDouble() < SZANSA_KONTROLI;
        zgloszenia.ustawLosowaKontrole(id, kontrola);
        zgloszenia.ustawStatus(id, "PRZEKAZANE");

        Zgloszenie przekazane = new Zgloszenie(z.id(), z.pesel(), z.budynekId(), z.rodzaj(), z.opis(), kontrola,
                z.decyzja(), "PRZEKAZANE", z.utworzono());
        komunikaty.publikuj("Zgłoszenie zdarzenia", String.valueOf(id),
                Map.of("zgloszenie", przekazane.doZmiennych()));
    }
}
