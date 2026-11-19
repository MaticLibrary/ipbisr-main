package pl.ans.ipbisr.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.ans.ipbisr.domain.Zgloszenie;

/** Dostęp do tabeli `zgloszenie`. */
@Repository
public class ZgloszenieRepository {

    private static final String KOLUMNY =
            "id, pesel, budynek_id, rodzaj, opis, losowa_kontrola, decyzja, status, utworzono";

    private final JdbcClient jdbc;

    public ZgloszenieRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static Zgloszenie mapuj(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        String pesel = rs.getString("pesel");
        int budynek = rs.getInt("budynek_id");
        Integer budynekId = rs.wasNull() ? null : budynek;
        return new Zgloszenie(rs.getLong("id"), pesel == null ? null : pesel.trim(), budynekId,
                rs.getString("rodzaj"), rs.getString("opis"), rs.getBoolean("losowa_kontrola"),
                rs.getString("decyzja"), rs.getString("status"),
                rs.getObject("utworzono", java.time.OffsetDateTime.class));
    }

    /** Zapisuje nowe zgłoszenie (status NOWE) i zwraca jego id. */
    public long dodaj(String pesel, Integer budynekId, String rodzaj, String opis) {
        return jdbc.sql("INSERT INTO zgloszenie (pesel, budynek_id, rodzaj, opis) VALUES (?, ?, ?, ?) RETURNING id")
                .params(pesel, budynekId, rodzaj, opis)
                .query(Long.class).single();
    }

    public Optional<Zgloszenie> znajdz(long id) {
        return jdbc.sql("SELECT " + KOLUMNY + " FROM zgloszenie WHERE id = ?").param(id)
                .query(ZgloszenieRepository::mapuj).optional();
    }

    public List<Zgloszenie> wszystkie() {
        return jdbc.sql("SELECT " + KOLUMNY + " FROM zgloszenie ORDER BY id DESC")
                .query(ZgloszenieRepository::mapuj).list();
    }

    public void ustawStatus(long id, String status) {
        jdbc.sql("UPDATE zgloszenie SET status = ? WHERE id = ?").params(status, id).update();
    }

    public void ustawLosowaKontrole(long id, boolean losowaKontrola) {
        jdbc.sql("UPDATE zgloszenie SET losowa_kontrola = ? WHERE id = ?").params(losowaKontrola, id).update();
    }

    /** Kończy obsługę zgłoszenia; `decyzja` jest pusta, gdy zarząd go nie weryfikował. */
    public void zakoncz(long id, String decyzja) {
        jdbc.sql("UPDATE zgloszenie SET decyzja = ?, status = 'ZAKONCZONE' WHERE id = ?").params(decyzja, id).update();
    }
}
