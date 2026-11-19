package pl.ans.ipbisr.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.ans.ipbisr.api.dto.BudynekDto;

/** Budynki z dzielnicą, priorytetem (najwyższy z mieszkańców) i liczbą mieszkańców – na potrzeby API. */
@Repository
public class BudynekRepository {

    private static final String ZAPYTANIE = """
            SELECT b.id, b.adres, b.dzielnica_id, d.nazwa AS dzielnica,
                   -- A < B < C alfabetycznie, więc MIN daje najwyższy priorytet; WYKLUCZONY pomijamy
                   (SELECT MIN(m.priorytet) FROM mieszkaniec m
                     WHERE m.budynek_id = b.id AND m.priorytet <> 'WYKLUCZONY') AS priorytet,
                   (SELECT count(*) FROM ewidencja_ludnosci e
                     WHERE e.budynek_id = b.id AND e.status = 'ZYJE') AS liczba_mieszkancow
            FROM budynek b JOIN dzielnica d ON d.id = b.dzielnica_id
            """;

    private final JdbcClient jdbc;

    public BudynekRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static BudynekDto mapuj(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        return new BudynekDto(rs.getInt("id"), rs.getString("adres"), rs.getInt("dzielnica_id"),
                rs.getString("dzielnica"), rs.getString("priorytet"), rs.getInt("liczba_mieszkancow"));
    }

    public List<BudynekDto> szukaj(Integer dzielnicaId) {
        return jdbc.sql(ZAPYTANIE
                        + " WHERE (CAST(:dzielnica AS INTEGER) IS NULL OR b.dzielnica_id = :dzielnica) ORDER BY b.id")
                .param("dzielnica", dzielnicaId).query(BudynekRepository::mapuj).list();
    }

    public Optional<BudynekDto> znajdz(int id) {
        return jdbc.sql(ZAPYTANIE + " WHERE b.id = ?").param(id).query(BudynekRepository::mapuj).optional();
    }
}
