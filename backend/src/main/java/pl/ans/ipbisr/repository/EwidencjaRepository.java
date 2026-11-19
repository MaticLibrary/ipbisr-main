package pl.ans.ipbisr.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.ans.ipbisr.domain.Osoba;

/** Rejestr ludności (tabela `ewidencja_ludnosci`) – system zewnętrzny, w projekcie symulowany. */
@Repository
public class EwidencjaRepository {

    private static final String KOLUMNY = "pesel, budynek_id, status, data_ur, stan_zdrowia, choroby_przewlekle, "
            + "niepelnosprawnosc, utrata_mieszkania, zawod_krytyczny, osoby_na_utrzymaniu";

    private static final RowMapper<Osoba> MAPPER = (ResultSet rs, int i) -> osoba(rs);

    private final JdbcClient jdbc;

    public EwidencjaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static Osoba osoba(ResultSet rs) throws SQLException {
        int wiek = Period.between(rs.getDate("data_ur").toLocalDate(), LocalDate.now()).getYears();
        return new Osoba(rs.getString("pesel").trim(), rs.getInt("budynek_id"), rs.getString("status"), wiek,
                rs.getString("stan_zdrowia"), rs.getBoolean("choroby_przewlekle"), rs.getString("niepelnosprawnosc"),
                rs.getBoolean("utrata_mieszkania"), rs.getBoolean("zawod_krytyczny"),
                rs.getInt("osoby_na_utrzymaniu"));
    }

    public List<Osoba> wszyscy() {
        return jdbc.sql("SELECT " + KOLUMNY + " FROM ewidencja_ludnosci ORDER BY pesel").query(MAPPER).list();
    }

    public Optional<Osoba> znajdz(String pesel) {
        return jdbc.sql("SELECT " + KOLUMNY + " FROM ewidencja_ludnosci WHERE pesel = ?").param(pesel)
                .query(MAPPER).optional();
    }

    public boolean budynekIstnieje(int budynekId) {
        return jdbc.sql("SELECT count(*) FROM budynek WHERE id = ?").param(budynekId).query(Integer.class).single() > 0;
    }

    public List<String> peseleWBudynku(int budynekId) {
        return jdbc.sql("SELECT pesel FROM ewidencja_ludnosci WHERE budynek_id = ?").param(budynekId)
                .query(String.class).list().stream().map(String::trim).toList();
    }

    // Aktualizacje po potwierdzeniu zgłoszenia – rejestr ludności „dowiaduje się” o zmianie od centrum

    public void ustawStatus(String pesel, String status) {
        jdbc.sql("UPDATE ewidencja_ludnosci SET status = ? WHERE pesel = ?").params(status, pesel).update();
    }

    public void ustawStanZdrowia(String pesel, String stanZdrowia) {
        jdbc.sql("UPDATE ewidencja_ludnosci SET stan_zdrowia = ? WHERE pesel = ?").params(stanZdrowia, pesel).update();
    }

    public void ustawNiepelnosprawnosc(String pesel, String niepelnosprawnosc) {
        jdbc.sql("UPDATE ewidencja_ludnosci SET niepelnosprawnosc = ? WHERE pesel = ?")
                .params(niepelnosprawnosc, pesel).update();
    }

    public void ustawUtrateMieszkania(String pesel) {
        jdbc.sql("UPDATE ewidencja_ludnosci SET utrata_mieszkania = TRUE WHERE pesel = ?").param(pesel).update();
    }
}
