package pl.ans.ipbisr.domain;

import java.util.HashMap;
import java.util.Map;

/** Wynik tabeli DMN dla jednego mieszkańca – to trafia do rejestru potrzeb (tabela `mieszkaniec`). */
public record WynikPriorytetu(String pesel, int budynekId, String status, String priorytet,
                              String poziomPotrzeb, boolean dostawaBezposrednia) {

    public Map<String, Object> doZmiennych() {
        Map<String, Object> m = new HashMap<>();
        m.put("pesel", pesel);
        m.put("budynekId", budynekId);
        m.put("status", status);
        m.put("priorytet", priorytet);
        m.put("poziomPotrzeb", poziomPotrzeb);
        m.put("dostawaBezposrednia", dostawaBezposrednia);
        return m;
    }

    public static WynikPriorytetu zZmiennych(Map<String, Object> m) {
        return new WynikPriorytetu((String) m.get("pesel"), ((Number) m.get("budynekId")).intValue(),
                (String) m.get("status"), (String) m.get("priorytet"), (String) m.get("poziomPotrzeb"),
                (Boolean) m.get("dostawaBezposrednia"));
    }
}
