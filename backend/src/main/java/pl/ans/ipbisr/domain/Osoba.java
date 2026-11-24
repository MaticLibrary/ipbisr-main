package pl.ans.ipbisr.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * Mieszkaniec z ewidencji ludności – dane, które potrzebuje tabela DMN „Priorytet”.
 * Wersja „na drucie”: tę samą strukturę (jako mapę) przekazujemy między procesami P1a i P1b.
 */
public record Osoba(String pesel, int budynekId, String status, int wiek, String stanZdrowia,
                    boolean chorobyPrzewlekle, String niepelnosprawnosc, boolean utrataMieszkania,
                    boolean zawodKrytyczny, int osobyNaUtrzymaniu) {

    /** Zmienne wejściowe tabeli DMN (nazwy takie jak w dmn/Priorytet_mieszkancow.dmn). */
    public Map<String, Object> doDmn() {
        Map<String, Object> m = new HashMap<>();
        m.put("status", status);
        m.put("wiek", wiek);
        m.put("stanZdrowia", stanZdrowia);
        m.put("chorobyPrzewlekle", chorobyPrzewlekle);
        m.put("niepelnosprawnosc", niepelnosprawnosc);
        m.put("utrataMieszkania", utrataMieszkania);
        m.put("zawodKrytyczny", zawodKrytyczny);
        m.put("osobyNaUtrzymaniu", osobyNaUtrzymaniu);
        return m;
    }

    /** Zmienne procesu (DMN + pesel i budynek). */
    public Map<String, Object> doZmiennych() {
        Map<String, Object> m = doDmn();
        m.put("pesel", pesel);
        m.put("budynekId", budynekId);
        return m;
    }

    public static Osoba zZmiennych(Map<String, Object> m) {
        return new Osoba((String) m.get("pesel"), ((Number) m.get("budynekId")).intValue(),
                (String) m.get("status"), ((Number) m.get("wiek")).intValue(), (String) m.get("stanZdrowia"),
                (Boolean) m.get("chorobyPrzewlekle"), (String) m.get("niepelnosprawnosc"),
                (Boolean) m.get("utrataMieszkania"), (Boolean) m.get("zawodKrytyczny"),
                ((Number) m.get("osobyNaUtrzymaniu")).intValue());
    }
}
