package pl.ans.ipbisr.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Wczytuje dane syntetyczne z plików CSV (katalog data/) do bazy.
 * Kolejność tabel jest ważna – najpierw te, na które wskazują klucze obce.
 * Tabela, która już ma dane, jest pomijana, więc ponowny start nie dubluje rekordów.
 * Brakujący plik jest tylko pomijany (generator danych może jeszcze nie istnieć).
 */
@Service
public class CsvDataImporter {

    private static final Logger log = LoggerFactory.getLogger(CsvDataImporter.class);

    /** Typ kolumny – tylko tyle, żeby zamienić tekst z CSV na typ bazy. */
    private enum Typ { TEKST, LICZBA_CALKOWITA, LICZBA, LOGICZNY, DATA }

    /** Opis jednej tabeli: nazwa pliku, nazwa tabeli i typy kolumn (nazwy muszą być w nagłówku CSV). */
    private record Tabela(String plik, String nazwa, List<Kolumna> kolumny) {}

    private record Kolumna(String nazwa, Typ typ) {}

    private static Kolumna k(String nazwa, Typ typ) {
        return new Kolumna(nazwa, typ);
    }

    // Kontrakt plików CSV – opisany też w data/README.md
    private static final List<Tabela> TABELE = List.of(
            new Tabela("dzielnice.csv", "dzielnica", List.of(
                    k("id", Typ.LICZBA_CALKOWITA), k("nazwa", Typ.TEKST))),
            new Tabela("punkty_wydawania.csv", "punkt_wydawania", List.of(
                    k("id", Typ.LICZBA_CALKOWITA), k("nazwa", Typ.TEKST), k("dzielnica_id", Typ.LICZBA_CALKOWITA),
                    k("przepustowosc_kg", Typ.LICZBA))),
            new Tabela("budynki.csv", "budynek", List.of(
                    k("id", Typ.LICZBA_CALKOWITA), k("adres", Typ.TEKST), k("dzielnica_id", Typ.LICZBA_CALKOWITA),
                    k("punkt_id", Typ.LICZBA_CALKOWITA))),
            new Tabela("ewidencja_ludnosci.csv", "ewidencja_ludnosci", List.of(
                    k("pesel", Typ.TEKST), k("imie", Typ.TEKST), k("nazwisko", Typ.TEKST), k("plec", Typ.TEKST),
                    k("data_ur", Typ.DATA), k("zawod", Typ.TEKST), k("zawod_krytyczny", Typ.LOGICZNY),
                    k("budynek_id", Typ.LICZBA_CALKOWITA), k("gospodarstwo_id", Typ.LICZBA_CALKOWITA),
                    k("stan_zdrowia", Typ.TEKST), k("choroby_przewlekle", Typ.LOGICZNY),
                    k("niepelnosprawnosc", Typ.TEKST), k("utrata_mieszkania", Typ.LOGICZNY),
                    k("osoby_na_utrzymaniu", Typ.LICZBA_CALKOWITA), k("status", Typ.TEKST))),
            new Tabela("stany_magazynowe.csv", "stan_magazynowy", List.of(
                    k("zasob", Typ.TEKST), k("ilosc", Typ.LICZBA), k("jednostka", Typ.TEKST))),
            new Tabela("historia_zuzycia.csv", "zuzycie_historia", List.of(
                    k("data", Typ.DATA), k("dzielnica_id", Typ.LICZBA_CALKOWITA), k("zasob", Typ.TEKST),
                    k("ilosc", Typ.LICZBA), k("dzien_tygodnia", Typ.LICZBA_CALKOWITA),
                    k("miesiac", Typ.LICZBA_CALKOWITA), k("temperatura", Typ.LICZBA), k("opady", Typ.LICZBA),
                    k("liczba_mieszkancow", Typ.LICZBA_CALKOWITA), k("procent_seniorow", Typ.LICZBA),
                    k("procent_chorych", Typ.LICZBA), k("poziom_zagrozenia", Typ.LICZBA_CALKOWITA),
                    k("liczba_alarmow_24h", Typ.LICZBA_CALKOWITA),
                    k("liczba_przesiedlonych", Typ.LICZBA_CALKOWITA))));

    private final JdbcTemplate jdbc;

    public CsvDataImporter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Importuje wszystkie znane pliki z podanego katalogu. */
    public void importuj(Path katalog) {
        for (Tabela tabela : TABELE) {
            Path plik = katalog.resolve(tabela.plik());
            if (!Files.exists(plik)) {
                log.warn("Brak pliku {} – pomijam tabelę {}", plik, tabela.nazwa());
                continue;
            }
            if (!tabelaPusta(tabela.nazwa())) {
                log.info("Tabela {} ma już dane – pomijam import", tabela.nazwa());
                continue;
            }
            int liczba = importujTabele(tabela, plik);
            log.info("Zaimportowano {} wierszy do tabeli {}", liczba, tabela.nazwa());
        }
    }

    private boolean tabelaPusta(String nazwa) {
        // nazwa pochodzi ze stałej listy powyżej, nie od użytkownika
        Integer ile = jdbc.queryForObject("SELECT count(*) FROM " + nazwa, Integer.class);
        return ile != null && ile == 0;
    }

    private int importujTabele(Tabela tabela, Path plik) {
        List<String> linie;
        try {
            linie = Files.readAllLines(plik, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Nie można odczytać pliku " + plik, e);
        }
        if (linie.isEmpty()) {
            return 0;
        }

        // pozycja każdej kolumny w nagłówku CSV – kolejność kolumn w pliku nie ma znaczenia
        List<String> naglowek = List.of(linie.get(0).trim().split(",", -1));
        List<Integer> pozycje = new ArrayList<>();
        for (Kolumna kolumna : tabela.kolumny()) {
            int pozycja = naglowek.indexOf(kolumna.nazwa());
            if (pozycja < 0) {
                throw new IllegalStateException(
                        "W pliku " + plik.getFileName() + " brakuje kolumny '" + kolumna.nazwa() + "'");
            }
            pozycje.add(pozycja);
        }

        String kolumnySql = String.join(", ", tabela.kolumny().stream().map(Kolumna::nazwa).toList());
        String znaki = String.join(", ", tabela.kolumny().stream().map(x -> "?").toList());
        String sql = "INSERT INTO " + tabela.nazwa() + " (" + kolumnySql + ") VALUES (" + znaki + ")";

        List<Object[]> wiersze = new ArrayList<>();
        for (String linia : linie.subList(1, linie.size())) {
            if (linia.isBlank()) {
                continue;
            }
            String[] pola = linia.split(",", -1);
            Object[] wartosci = new Object[tabela.kolumny().size()];
            for (int i = 0; i < wartosci.length; i++) {
                wartosci[i] = konwertuj(pola[pozycje.get(i)].trim(), tabela.kolumny().get(i).typ());
            }
            wiersze.add(wartosci);
        }
        jdbc.batchUpdate(sql, wiersze);
        return wiersze.size();
    }

    private Object konwertuj(String tekst, Typ typ) {
        if (tekst.isEmpty()) {
            return null; // puste pole w CSV = NULL w bazie
        }
        return switch (typ) {
            case TEKST -> tekst;
            case LICZBA_CALKOWITA -> Integer.valueOf(tekst);
            case LICZBA -> new java.math.BigDecimal(tekst);
            case LOGICZNY -> Boolean.valueOf(tekst);
            case DATA -> Date.valueOf(LocalDate.parse(tekst));
        };
    }
}
