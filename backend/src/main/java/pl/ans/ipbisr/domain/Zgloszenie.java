package pl.ans.ipbisr.domain;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/** Zgłoszenie zdarzenia od operatora punktu (tabela `zgloszenie`). Pola `?` z kontraktu API mogą być null. */
public record Zgloszenie(long id, String pesel, Integer budynekId, String rodzaj, String opis,
                         boolean losowaKontrola, String decyzja, String status, OffsetDateTime utworzono) {

    /** Zgłoszenie w postaci zmiennej procesu `zgloszenie` (używa jej bramka w P1b i formularz zarządu). */
    public Map<String, Object> doZmiennych() {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("pesel", pesel);
        m.put("budynekId", budynekId);
        m.put("rodzaj", rodzaj);
        m.put("opis", opis);
        m.put("losowaKontrola", losowaKontrola);
        return m;
    }
}
