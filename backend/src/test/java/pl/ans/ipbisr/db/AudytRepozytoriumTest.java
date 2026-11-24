package pl.ans.ipbisr.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.ans.ipbisr.api.dto.AudytDto;
import pl.ans.ipbisr.service.AudytService;

/** Zapis i odczyt dziennika decyzji na prawdziwym PostgreSQL (kolumna `szczegoly` typu JSONB). */
@Import(AudytService.class)
class AudytRepozytoriumTest extends PostgresJdbcTest {

    @Autowired AudytService audyt;
    @Autowired JdbcTemplate jdbc;

    @Test
    void zwracaNajnowszeWpisyPierwszeZSzczegolamiJakoObiekt() {
        audyt.zapisz("zarzad", "P1b", "ZWERYFIKOWANO", Map.of("decyzja", "POTWIERDZONE"));
        audyt.zapisz("system", "P3a", "ZATWIERDZONO_AUTOMATYCZNIE", Map.of("planId", 7));

        List<AudytDto> wpisy = audyt.ostatnie(100);

        assertThat(wpisy).extracting(AudytDto::akcja).containsExactly("ZATWIERDZONO_AUTOMATYCZNIE", "ZWERYFIKOWANO");
        assertThat(wpisy.get(0).szczegoly()).containsEntry("planId", 7);
        assertThat(wpisy.get(1).szczegoly()).containsEntry("decyzja", "POTWIERDZONE");
        assertThat(wpisy.get(0).czas()).isNotNull();
    }

    @Test
    void limitObcinaListeIWpisBezSzczegolowDajePustyObiekt() {
        jdbc.update("INSERT INTO audyt (aktor, proces, akcja) VALUES ('operator', 'P1c', 'ZGLOSZENIE')");
        audyt.zapisz("zarzad", "P1b", "ZWERYFIKOWANO", Map.of());

        assertThat(audyt.ostatnie(1)).hasSize(1);
        assertThat(audyt.ostatnie(2)).extracting(AudytDto::szczegoly).containsOnly(Map.of());
    }

    @Test
    void limitSpozaZakresuToBlad() {
        assertThatThrownBy(() -> audyt.ostatnie(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> audyt.ostatnie(1001)).isInstanceOf(IllegalArgumentException.class);
    }
}
