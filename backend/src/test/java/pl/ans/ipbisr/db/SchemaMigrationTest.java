package pl.ans.ipbisr.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

// Testy schematu Flyway (V1__schemat.sql) – tabele z sekcji 9 planu i podstawowe ograniczenia
class SchemaMigrationTest extends PostgresJdbcTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void migracjaTworzyWszystkieTabeleZPlanu() {
        List<String> tabele = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

        assertThat(tabele).contains(
                "ewidencja_ludnosci", "dzielnica", "punkt_wydawania", "budynek", "mieszkaniec", "zgloszenie",
                "stan_magazynowy", "zuzycie_historia", "prognoza", "prognoza_pozycja", "plan_przydzialu",
                "plan_pozycja", "dostawa_bezposrednia", "wniosek_pomoc", "zlecenie_wydania", "audyt");
    }

    @Test
    void zgloszenieWymagaPeselLubBudynku() {
        // Bez PESEL-u i bez budynku zgłoszenie nie wskazuje nikogo – baza ma to odrzucić
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO zgloszenie (rodzaj, opis) VALUES ('SMIERC', 'brak adresata')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void zgloszenieOdrzucaNieznanyRodzaj() {
        jdbc.update("INSERT INTO dzielnica (id, nazwa) VALUES (1, 'Centrum')");
        jdbc.update("INSERT INTO punkt_wydawania (id, nazwa, dzielnica_id, przepustowosc_kg) VALUES (1, 'P1', 1, 500)");
        jdbc.update("INSERT INTO budynek (id, adres, dzielnica_id, punkt_id) VALUES (1, 'Rynek 1', 1, 1)");

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO zgloszenie (budynek_id, rodzaj) VALUES (1, 'KRADZIEZ')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void priorytetMieszkancaToAbcLubWykluczony() {
        jdbc.update("INSERT INTO dzielnica (id, nazwa) VALUES (1, 'Centrum')");
        jdbc.update("INSERT INTO punkt_wydawania (id, nazwa, dzielnica_id, przepustowosc_kg) VALUES (1, 'P1', 1, 500)");
        jdbc.update("INSERT INTO budynek (id, adres, dzielnica_id, punkt_id) VALUES (1, 'Rynek 1', 1, 1)");
        jdbc.update("""
                INSERT INTO ewidencja_ludnosci (pesel, imie, nazwisko, plec, data_ur, budynek_id, stan_zdrowia,
                    niepelnosprawnosc, status)
                VALUES ('90010112345', 'Jan', 'Kowalski', 'M', '1990-01-01', 1, 'DOBRY', 'BRAK', 'ZYJE')""");

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO mieszkaniec (pesel, budynek_id, priorytet, poziom_potrzeb, status)
                VALUES ('90010112345', 1, 'Z', 'PODSTAWOWY', 'ZYJE')"""))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void audytPrzyjmujeSzczegolyJakJson() {
        jdbc.update("""
                INSERT INTO audyt (aktor, proces, akcja, szczegoly)
                VALUES ('zarzad', 'P1b', 'ZWERYFIKOWANO', '{"decyzja":"POTWIERDZONE"}'::jsonb)""");

        String decyzja = jdbc.queryForObject("SELECT szczegoly->>'decyzja' FROM audyt", String.class);
        assertThat(decyzja).isEqualTo("POTWIERDZONE");
    }
}
