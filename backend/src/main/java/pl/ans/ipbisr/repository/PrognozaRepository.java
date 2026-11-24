package pl.ans.ipbisr.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.ans.ipbisr.api.dto.PrognozaDto;
import pl.ans.ipbisr.api.dto.PrognozaPozycjaDto;
import pl.ans.ipbisr.domain.IloscZasobu;

/** Odczyt prognoz zapotrzebowania (zapisuje je worker P2) – na potrzeby API i ekranu „Prognoza”. */
@Repository
public class PrognozaRepository {

    private static final String NAGLOWEK = "SELECT id, utworzono, zrodlo, roznica_proc, status FROM prognoza";

    private final JdbcClient jdbc;

    public PrognozaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static PrognozaDto mapuj(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        double roznica = rs.getDouble("roznica_proc");
        Double roznicaProc = rs.wasNull() ? null : roznica; // wasNull dotyczy ostatnio czytanej kolumny
        return new PrognozaDto(rs.getLong("id"), rs.getObject("utworzono", java.time.OffsetDateTime.class),
                rs.getString("zrodlo"), roznicaProc, rs.getString("status"), null);
    }

    /** Najnowsza prognoza (największe id) razem z pozycjami; puste, gdy P2 jeszcze żadnej nie zapisał. */
    public Optional<PrognozaDto> ostatnia() {
        return jdbc.sql(NAGLOWEK + " ORDER BY id DESC LIMIT 1").query(PrognozaRepository::mapuj).optional()
                .map(p -> new PrognozaDto(p.id(), p.utworzono(), p.zrodlo(), p.roznicaProc(), p.status(),
                        pozycje(p.id())));
    }

    /** Historia prognoz od najnowszej, bez pozycji. */
    public List<PrognozaDto> wszystkie() {
        return jdbc.sql(NAGLOWEK + " ORDER BY id DESC").query(PrognozaRepository::mapuj).list();
    }

    /**
     * Prognoza zapotrzebowania dla całego miasta (poziom MIASTO) – z prognozy o podanym id albo, gdy id jest null
     * (proces ruszył od „Ponownego planowania”, a nie od „Prognozy gotowej”), z najnowszej. Brak prognozy = pusta lista.
     */
    public List<IloscZasobu> miasto(Long prognozaId) {
        return jdbc.sql("""
                        SELECT zasob, ilosc FROM prognoza_pozycja
                        WHERE poziom = 'MIASTO'
                          AND prognoza_id = COALESCE(CAST(:id AS BIGINT), (SELECT MAX(id) FROM prognoza))
                        ORDER BY zasob""")
                .param("id", prognozaId)
                .query((rs, i) -> new IloscZasobu(rs.getString("zasob"), rs.getDouble("ilosc")))
                .list();
    }

    private List<PrognozaPozycjaDto> pozycje(long prognozaId) {
        return jdbc.sql("""
                        SELECT poziom, obiekt_id, zasob, ilosc FROM prognoza_pozycja
                        WHERE prognoza_id = ? ORDER BY id
                        """)
                .param(prognozaId)
                .query((rs, i) -> {
                    int obiekt = rs.getInt("obiekt_id");
                    Integer obiektId = rs.wasNull() ? null : obiekt; // dla poziomu MIASTO brak id
                    return new PrognozaPozycjaDto(rs.getString("poziom"), obiektId,
                            rs.getString("zasob"), rs.getDouble("ilosc"));
                }).list();
    }
}
