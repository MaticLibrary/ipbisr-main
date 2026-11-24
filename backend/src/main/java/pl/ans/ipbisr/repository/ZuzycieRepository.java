package pl.ans.ipbisr.repository;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.ans.ipbisr.api.dto.ZuzycieDto;

/** Historia zużycia zasobu (dane do wykresu „historia + prognoza”). */
@Repository
public class ZuzycieRepository {

    private final JdbcClient jdbc;

    public ZuzycieRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Zużycie dzień po dniu (rosnąco po dacie). Bez dzielnicy (null) sumuje całe miasto. */
    public List<ZuzycieDto> historia(Integer dzielnicaId, String zasob) {
        return jdbc.sql("""
                        SELECT data, SUM(ilosc) AS ilosc FROM zuzycie_historia
                        WHERE zasob = :zasob AND (CAST(:dzielnica AS INTEGER) IS NULL OR dzielnica_id = :dzielnica)
                        GROUP BY data ORDER BY data
                        """)
                .param("zasob", zasob).param("dzielnica", dzielnicaId)
                .query((rs, i) -> new ZuzycieDto(rs.getObject("data", java.time.LocalDate.class), rs.getDouble("ilosc")))
                .list();
    }
}
