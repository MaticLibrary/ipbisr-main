package pl.ans.ipbisr.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.ans.ipbisr.api.dto.PrognozaDto;
import pl.ans.ipbisr.api.dto.ZuzycieDto;
import pl.ans.ipbisr.repository.PrognozaRepository;
import pl.ans.ipbisr.repository.ZuzycieRepository;

/** Zapytania SQL dla prognoz i historii zużycia (M3) na prawdziwym PostgreSQL. */
@Import({PrognozaRepository.class, ZuzycieRepository.class})
class PrognozaRepozytoriaTest extends PostgresJdbcTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired PrognozaRepository prognozy;
    @Autowired ZuzycieRepository zuzycie;

    @BeforeEach
    void dane() {
        jdbc.update("INSERT INTO dzielnica VALUES (1, 'Centrum'), (2, 'Północ')");
        jdbc.update("""
                INSERT INTO zuzycie_historia (data, dzielnica_id, zasob, ilosc) VALUES
                ('2026-03-02', 1, 'WODA', 200), ('2026-03-01', 1, 'WODA', 100),
                ('2026-03-01', 2, 'WODA', 50),  ('2026-03-01', 1, 'LEKI', 7)
                """);
    }

    @Test
    void ostatniaPrognozaToTaZNajwiekszymIdWrazZPozycjami() {
        jdbc.update("INSERT INTO prognoza (id, zrodlo, roznica_proc, status) VALUES (1, 'ML', 5, 'ZATWIERDZONA')");
        jdbc.update("INSERT INTO prognoza (id, zrodlo, roznica_proc, status) VALUES (2, 'ML', 41.5, 'NOWA')");
        jdbc.update("""
                INSERT INTO prognoza_pozycja (prognoza_id, poziom, obiekt_id, zasob, ilosc) VALUES
                (1, 'MIASTO', NULL, 'WODA', 1), (2, 'MIASTO', NULL, 'WODA', 9000), (2, 'DZIELNICA', 1, 'WODA', 5400)
                """);

        PrognozaDto p = prognozy.ostatnia().orElseThrow();

        assertThat(p.id()).isEqualTo(2);
        assertThat(p.zrodlo()).isEqualTo("ML");
        assertThat(p.roznicaProc()).isEqualTo(41.5);
        assertThat(p.status()).isEqualTo("NOWA");
        assertThat(p.pozycje()).hasSize(2);
        assertThat(p.pozycje().get(0).poziom()).isEqualTo("MIASTO");
        assertThat(p.pozycje().get(0).obiektId()).isNull();
        assertThat(p.pozycje().get(1).obiektId()).isEqualTo(1);
        assertThat(p.pozycje().get(1).ilosc()).isEqualTo(5400.0);
    }

    @Test
    void brakPrognozZwracaPusteOptional() {
        assertThat(prognozy.ostatnia()).isEmpty();
    }

    @Test
    void listaPrognozJestOdNajnowszejBezPozycjiIZNullowaRoznica() {
        jdbc.update("INSERT INTO prognoza (id, zrodlo, status) VALUES (1, 'ML', 'ZATWIERDZONA')");
        jdbc.update("INSERT INTO prognoza (id, zrodlo, roznica_proc, status) VALUES (2, 'OSTATNIA_ZATWIERDZONA', 12.5, 'ZATWIERDZONA')");

        List<PrognozaDto> lista = prognozy.wszystkie();

        assertThat(lista).extracting(PrognozaDto::id).containsExactly(2L, 1L);
        assertThat(lista.get(1).roznicaProc()).isNull();
        assertThat(lista).allSatisfy(p -> assertThat(p.pozycje()).isNull());
    }

    @Test
    void historiaDzielnicyFiltrujePoZasobieISortujeRosnacoPoDacie() {
        List<ZuzycieDto> h = zuzycie.historia(1, "WODA");

        assertThat(h).extracting(ZuzycieDto::data).containsExactly(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 2));
        assertThat(h).extracting(ZuzycieDto::ilosc).containsExactly(100.0, 200.0);
    }

    @Test
    void historiaBezDzielnicySumujeCaleMiastoPoDniach() {
        List<ZuzycieDto> h = zuzycie.historia(null, "WODA");

        assertThat(h).extracting(ZuzycieDto::ilosc).containsExactly(150.0, 200.0);
    }
}
