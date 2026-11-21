package pl.ans.ipbisr.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.ans.ipbisr.domain.IloscZasobu;

/** Wniosek o pomoc humanitarną (tabela `wniosek_pomoc`): jeden wiersz na zasób, którego brakuje w planie. */
@Repository
public class WniosekRepository {

    private final JdbcClient jdbc;

    public WniosekRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Zapisuje wniosek planu (status WYSLANY); wcześniejszy wniosek tego planu jest zastępowany, więc ponowienie zadania nie dubluje. */
    public void dodaj(long planId, List<IloscZasobu> niedobory) {
        jdbc.sql("DELETE FROM wniosek_pomoc WHERE plan_id = ?").param(planId).update();
        for (IloscZasobu n : niedobory) {
            jdbc.sql("INSERT INTO wniosek_pomoc (plan_id, zasob, ilosc, status) VALUES (?, ?, ?, 'WYSLANY')")
                    .params(planId, n.zasob(), n.ilosc()).update();
        }
    }

    /** Odpowiedź organizacji: ile przyznano na każdy zasób i do kiedy dostawa; zasób bez wzmianki dostaje 0. */
    public void zapiszOdpowiedz(long planId, Map<String, Double> przyznano, LocalDate termin) {
        List<String> zasoby = jdbc.sql("SELECT zasob FROM wniosek_pomoc WHERE plan_id = ?").param(planId)
                .query(String.class).list();
        for (String zasob : zasoby) {
            jdbc.sql("UPDATE wniosek_pomoc SET przyznano = ?, termin = ?, status = 'ODPOWIEDZ_OTRZYMANA' "
                            + "WHERE plan_id = ? AND zasob = ?")
                    .params(przyznano.getOrDefault(zasob, 0.0), termin, planId, zasob).update();
        }
    }

    /** Wnioskowane ilości planu (zasób → ile brakuje). */
    public List<IloscZasobu> wnioskiPlanu(long planId) {
        return jdbc.sql("SELECT zasob, ilosc FROM wniosek_pomoc WHERE plan_id = ? ORDER BY zasob").param(planId)
                .query((rs, i) -> new IloscZasobu(rs.getString("zasob"), rs.getDouble("ilosc")))
                .list();
    }
}
