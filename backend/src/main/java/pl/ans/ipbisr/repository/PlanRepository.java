package pl.ans.ipbisr.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.ans.ipbisr.api.dto.DostawaBezposredniaDto;
import pl.ans.ipbisr.api.dto.HarmonogramDto;
import pl.ans.ipbisr.api.dto.PlanDto;
import pl.ans.ipbisr.api.dto.PlanPozycjaDto;
import pl.ans.ipbisr.domain.IloscZasobu;
import pl.ans.ipbisr.domain.ZlecenieWydania;

/** Plan przydziału zasobów (P3): odczyt dla API, decyzja analityka i zlecenia wydania dla magazynu. */
@Repository
public class PlanRepository {

    private static final String NAGLOWEK =
            "SELECT id, prognoza_id, krytyczne_pokryte, status, zatwierdzil FROM plan_przydzialu";

    private final JdbcClient jdbc;

    public PlanRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Najnowszy plan (największe id) razem z pozycjami; puste, gdy żaden plan nie powstał. */
    public Optional<PlanDto> ostatni() {
        return jdbc.sql(NAGLOWEK + " ORDER BY id DESC LIMIT 1").query(PlanRepository::mapuj).optional()
                .map(this::zeSzczegolami);
    }

    public Optional<PlanDto> znajdz(long id) {
        return jdbc.sql(NAGLOWEK + " WHERE id = ?").param(id).query(PlanRepository::mapuj).optional()
                .map(this::zeSzczegolami);
    }

    private static PlanDto mapuj(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        // getObject(…, Long.class) zwraca null dla pustej kolumny (plan bez prognozy, np. po „Ponownym planowaniu”)
        return new PlanDto(rs.getLong("id"), rs.getObject("prognoza_id", Long.class), rs.getBoolean("krytyczne_pokryte"),
                rs.getString("status"), rs.getString("zatwierdzil"), null, null, null, null);
    }

    private PlanDto zeSzczegolami(PlanDto p) {
        return new PlanDto(p.id(), p.prognozaId(), p.krytycznePokryte(), p.status(), p.zatwierdzil(),
                pozycje(p.id()), niedobory(p.id()), dostawy(p.id()), harmonogram(p.id()));
    }

    private List<PlanPozycjaDto> pozycje(long planId) {
        return jdbc.sql("""
                        SELECT budynek_id, zasob, priorytet, przydzielono, potrzeba FROM plan_pozycja
                        WHERE plan_id = ? ORDER BY id""")
                .param(planId)
                .query((rs, i) -> new PlanPozycjaDto(rs.getInt("budynek_id"), rs.getString("zasob"),
                        rs.getString("priorytet"), rs.getDouble("przydzielono"), rs.getDouble("potrzeba")))
                .list();
    }

    /** Niedobór zasobu = suma (potrzeba − przydzielono) po wszystkich pozycjach planu; zasoby bez braków pomijamy. */
    private List<IloscZasobu> niedobory(long planId) {
        return jdbc.sql("""
                        SELECT zasob, SUM(potrzeba - przydzielono) AS brak FROM plan_pozycja
                        WHERE plan_id = ? GROUP BY zasob HAVING SUM(potrzeba - przydzielono) > 0 ORDER BY zasob""")
                .param(planId)
                .query((rs, i) -> new IloscZasobu(rs.getString("zasob"), rs.getDouble("brak")))
                .list();
    }

    private List<DostawaBezposredniaDto> dostawy(long planId) {
        return jdbc.sql("SELECT pesel, zasob, ilosc FROM dostawa_bezposrednia WHERE plan_id = ? ORDER BY id")
                .param(planId)
                .query((rs, i) -> new DostawaBezposredniaDto(rs.getString("pesel").trim(), rs.getString("zasob"),
                        rs.getDouble("ilosc")))
                .list();
    }

    private List<HarmonogramDto> harmonogram(long planId) {
        return jdbc.sql("SELECT punkt_id, okno, budynek_id FROM plan_harmonogram WHERE plan_id = ? ORDER BY id")
                .param(planId)
                .query((rs, i) -> new HarmonogramDto(rs.getInt("punkt_id"), rs.getString("okno"), rs.getInt("budynek_id")))
                .list();
    }

    /** Decyzja analityka: nowy status planu (ZATWIERDZONY / ODRZUCONY) i kto go ustawił. */
    public void ustawStatus(long planId, String status, String zatwierdzil) {
        jdbc.sql("UPDATE plan_przydzialu SET status = ?, zatwierdzil = ? WHERE id = ?")
                .params(status, zatwierdzil, planId).update();
    }

    /**
     * Tworzy zlecenia wydania z zatwierdzonego planu: suma przydziału budynków każdego punktu wydawania, osobno dla zasobu.
     * Najpierw kasuje wcześniejsze zlecenia tego planu, więc ponowienie zadania przez Camundę niczego nie dubluje.
     *
     * @return liczba utworzonych zleceń
     */
    public int utworzZlecenia(long planId) {
        jdbc.sql("DELETE FROM zlecenie_wydania WHERE plan_id = ?").param(planId).update();
        return jdbc.sql("""
                        INSERT INTO zlecenie_wydania (plan_id, punkt_id, zasob, ilosc)
                        SELECT p.plan_id, b.punkt_id, p.zasob, SUM(p.przydzielono)
                        FROM plan_pozycja p JOIN budynek b ON b.id = p.budynek_id
                        WHERE p.plan_id = ?
                        GROUP BY p.plan_id, b.punkt_id, p.zasob
                        HAVING SUM(p.przydzielono) > 0""")
                .param(planId).update();
    }

    public List<ZlecenieWydania> zlecenia(long planId) {
        return jdbc.sql("SELECT punkt_id, zasob, ilosc FROM zlecenie_wydania WHERE plan_id = ? ORDER BY punkt_id, zasob")
                .param(planId)
                .query((rs, i) -> new ZlecenieWydania(rs.getInt("punkt_id"), rs.getString("zasob"), rs.getDouble("ilosc")))
                .list();
    }
}
