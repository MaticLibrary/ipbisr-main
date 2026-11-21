package pl.ans.ipbisr.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.ans.ipbisr.api.dto.MagazynDto;
import pl.ans.ipbisr.api.dto.PlanDto;
import pl.ans.ipbisr.domain.GrupaPotrzeb;
import pl.ans.ipbisr.domain.IloscZasobu;
import pl.ans.ipbisr.domain.PunktWydawania;
import pl.ans.ipbisr.domain.ZlecenieWydania;
import pl.ans.ipbisr.repository.MagazynRepository;
import pl.ans.ipbisr.repository.MieszkaniecRepository;
import pl.ans.ipbisr.repository.PlanRepository;
import pl.ans.ipbisr.repository.PrognozaRepository;
import pl.ans.ipbisr.repository.PunktRepository;
import pl.ans.ipbisr.repository.WniosekRepository;

/** SQL procesu P3 na prawdziwym PostgreSQL: magazyn, plan przydziału, wniosek o pomoc, dane wejściowe planowania. */
@Import({MagazynRepository.class, PlanRepository.class, WniosekRepository.class, PunktRepository.class,
        MieszkaniecRepository.class, PrognozaRepository.class})
class PlanRepozytoriaTest extends PostgresJdbcTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired MagazynRepository magazyn;
    @Autowired PlanRepository plany;
    @Autowired WniosekRepository wnioski;
    @Autowired PunktRepository punkty;
    @Autowired MieszkaniecRepository mieszkancy;
    @Autowired PrognozaRepository prognozy;

    @BeforeEach
    void dane() {
        jdbc.update("INSERT INTO dzielnica VALUES (1, 'Centrum')");
        jdbc.update("INSERT INTO punkt_wydawania VALUES (1, 'Punkt 1', 1, 1000), (2, 'Punkt 2', 1, 500)");
        jdbc.update("INSERT INTO budynek VALUES (1, 'Rynek 1', 1, 1), (2, 'Długa 5', 1, 1), (3, 'Krótka 2', 1, 2)");
    }

    // ----- magazyn -----

    @Test
    void magazynZwracaStanyPosortowanePoZasobie() {
        jdbc.update("INSERT INTO stan_magazynowy VALUES ('WODA', 12000, 'l'), ('LEKI', 300, 'zestaw')");

        assertThat(magazyn.wszystkie()).containsExactly(
                new MagazynDto("LEKI", 300.0, "zestaw"), new MagazynDto("WODA", 12000.0, "l"));
    }

    @Test
    void zapisMagazynuNadpisujeIstniejacyZasobIDodajeNowy() {
        jdbc.update("INSERT INTO stan_magazynowy VALUES ('WODA', 12000, 'l')");

        magazyn.zapisz(List.of(new MagazynDto("WODA", 50.0, "l"), new MagazynDto("LEKI", 7.5, "zestaw")));

        assertThat(magazyn.wszystkie()).containsExactly(
                new MagazynDto("LEKI", 7.5, "zestaw"), new MagazynDto("WODA", 50.0, "l"));
    }

    // ----- plan przydziału -----

    private void planZPozycjami() {
        jdbc.update("INSERT INTO plan_przydzialu (id, krytyczne_pokryte, status) VALUES (1, TRUE, 'ZATWIERDZONY')");
        jdbc.update("""
                INSERT INTO plan_przydzialu (id, prognoza_id, krytyczne_pokryte, status, zatwierdzil)
                VALUES (2, NULL, FALSE, 'DO_ZATWIERDZENIA', NULL)""");
        jdbc.update("""
                INSERT INTO plan_pozycja (plan_id, budynek_id, zasob, priorytet, przydzielono, potrzeba) VALUES
                (2, 1, 'WODA', 'A', 60, 60), (2, 2, 'WODA', 'C', 40, 80), (2, 3, 'LEKI', 'A', 0, 40),
                (2, 3, 'WODA', 'B', 10, 10)""");
        jdbc.update("""
                INSERT INTO ewidencja_ludnosci (pesel, imie, nazwisko, plec, data_ur, budynek_id, stan_zdrowia, status)
                VALUES ('11111111111', 'Anna', 'Nowak', 'K', '1940-01-01', 1, 'DOBRY', 'ZYJE')""");
        jdbc.update("INSERT INTO dostawa_bezposrednia (plan_id, pesel, zasob, ilosc) VALUES (2, '11111111111', 'WODA', 4)");
        jdbc.update("INSERT INTO plan_harmonogram (plan_id, punkt_id, okno, budynek_id) VALUES (2, 1, '08:00-10:00', 1)");
    }

    @Test
    void ostatniPlanToTenZNajwiekszymIdZPelnymKontraktem() {
        planZPozycjami();

        PlanDto p = plany.ostatni().orElseThrow();

        assertThat(p.id()).isEqualTo(2);
        assertThat(p.prognozaId()).isNull();
        assertThat(p.krytycznePokryte()).isFalse();
        assertThat(p.status()).isEqualTo("DO_ZATWIERDZENIA");
        assertThat(p.zatwierdzil()).isNull();
        assertThat(p.pozycje()).hasSize(4);
        assertThat(p.pozycje().get(0).budynekId()).isEqualTo(1);
        assertThat(p.pozycje().get(0).przydzielono()).isEqualTo(60.0);
        assertThat(p.dostawyBezposrednie()).singleElement().satisfies(d -> {
            assertThat(d.pesel()).isEqualTo("11111111111");
            assertThat(d.ilosc()).isEqualTo(4.0);
        });
        assertThat(p.harmonogram()).singleElement().satisfies(h -> {
            assertThat(h.punktId()).isEqualTo(1);
            assertThat(h.okno()).isEqualTo("08:00-10:00");
            assertThat(h.budynekId()).isEqualTo(1);
        });
    }

    @Test
    void niedoboryToSumaBrakowPoZasobachBezPelnegoPokrycia() {
        planZPozycjami();

        // LEKI: 40 − 0 = 40; WODA: (60−60) + (80−40) + (10−10) = 40
        assertThat(plany.znajdz(2).orElseThrow().niedobory())
                .containsExactly(new IloscZasobu("LEKI", 40.0), new IloscZasobu("WODA", 40.0));
        assertThat(plany.znajdz(1).orElseThrow().niedobory()).isEmpty();
    }

    @Test
    void brakPlanuZwracaPusteOptional() {
        assertThat(plany.ostatni()).isEmpty();
        assertThat(plany.znajdz(99)).isEmpty();
    }

    @Test
    void ustawStatusZapisujeDecyzjeIKtoZatwierdzil() {
        planZPozycjami();

        plany.ustawStatus(2, "ZATWIERDZONY", "analityk");

        PlanDto p = plany.znajdz(2).orElseThrow();
        assertThat(p.status()).isEqualTo("ZATWIERDZONY");
        assertThat(p.zatwierdzil()).isEqualTo("analityk");
    }

    @Test
    void zleceniaWydaniaSumujaPrzydzialPoPunkcieIZasobie() {
        planZPozycjami();

        int n = plany.utworzZlecenia(2);

        // punkt 1 (budynki 1, 2): WODA 60 + 40; punkt 2 (budynek 3): WODA 10; LEKI 0 pomijamy
        assertThat(n).isEqualTo(2);
        assertThat(plany.zlecenia(2)).containsExactly(
                new ZlecenieWydania(1, "WODA", 100.0), new ZlecenieWydania(2, "WODA", 10.0));
    }

    @Test
    void ponowneTworzenieZlecenOTymSamymPlanieNieDubluje() {
        planZPozycjami();

        plany.utworzZlecenia(2);
        plany.utworzZlecenia(2); // np. Camunda ponowiła zadanie po błędzie

        assertThat(plany.zlecenia(2)).hasSize(2);
    }

    // ----- wniosek o pomoc -----

    @Test
    void wniosekZapisujeNiedoboryIOdpowiedzOrganizacji() {
        planZPozycjami();

        wnioski.dodaj(2, List.of(new IloscZasobu("LEKI", 40.0), new IloscZasobu("WODA", 40.0)));
        wnioski.zapiszOdpowiedz(2, Map.of("LEKI", 30.0), LocalDate.of(2026, 12, 1));

        List<Map<String, Object>> w = jdbc.queryForList(
                "SELECT zasob, ilosc, przyznano, termin, status FROM wniosek_pomoc WHERE plan_id = 2 ORDER BY zasob");
        assertThat(w).hasSize(2);
        assertThat(w.get(0)).containsEntry("zasob", "LEKI").containsEntry("status", "ODPOWIEDZ_OTRZYMANA");
        assertThat(((Number) w.get(0).get("przyznano")).doubleValue()).isEqualTo(30.0);
        assertThat(w.get(0).get("termin").toString()).isEqualTo("2026-12-01");
        // zasób, którego organizacja nie wymieniła w odpowiedzi, dostaje 0
        assertThat(((Number) w.get(1).get("przyznano")).doubleValue()).isZero();
    }

    @Test
    void odczytWnioskowPlanu() {
        planZPozycjami();
        wnioski.dodaj(2, List.of(new IloscZasobu("LEKI", 40.0)));

        assertThat(wnioski.wnioskiPlanu(2)).containsExactly(new IloscZasobu("LEKI", 40.0));
    }

    // ----- dane wejściowe planowania -----

    @Test
    void punktyWydawaniaMajaListeSwoichBudynkow() {
        assertThat(punkty.wszystkie()).containsExactly(
                new PunktWydawania(1, 1000.0, List.of(1, 2)), new PunktWydawania(2, 500.0, List.of(3)));
    }

    @Test
    void grupyPotrzebLiczaMieszkancowPoBudynkuPriorytecieIGrupieWiekowej() {
        jdbc.update("""
                INSERT INTO ewidencja_ludnosci (pesel, imie, nazwisko, plec, data_ur, budynek_id, stan_zdrowia, status) VALUES
                ('11111111111', 'Anna', 'Senior1', 'K', now() - interval '80 years 1 month', 1, 'DOBRY', 'ZYJE'),
                ('22222222222', 'Jan', 'Senior2', 'M', now() - interval '70 years 1 month', 1, 'DOBRY', 'ZYJE'),
                ('33333333333', 'Ola', 'Dziecko', 'K', now() - interval '10 years 1 month', 1, 'DOBRY', 'ZYJE'),
                ('44444444444', 'Ewa', 'Dorosla', 'K', now() - interval '40 years 1 month', 2, 'DOBRY', 'ZYJE'),
                ('55555555555', 'Tom', 'Zmarly', 'M', now() - interval '40 years 1 month', 2, 'DOBRY', 'NIE_ZYJE'),
                ('66666666666', 'Ala', 'Wykluczona', 'K', now() - interval '40 years 1 month', 2, 'DOBRY', 'ZYJE')""");
        jdbc.update("""
                INSERT INTO mieszkaniec (pesel, budynek_id, priorytet, poziom_potrzeb, dostawa_bezposrednia, status) VALUES
                ('11111111111', 1, 'A', 'PODWYZSZONY', TRUE, 'ZYJE'),
                ('22222222222', 1, 'A', 'PODWYZSZONY', TRUE, 'ZYJE'),
                ('33333333333', 1, 'C', 'PODSTAWOWY', FALSE, 'ZYJE'),
                ('44444444444', 2, 'B', 'PODSTAWOWY', FALSE, 'ZYJE'),
                ('55555555555', 2, 'WYKLUCZONY', 'PODSTAWOWY', FALSE, 'NIE_ZYJE'),
                ('66666666666', 2, 'WYKLUCZONY', 'PODSTAWOWY', FALSE, 'ZYJE')""");

        List<GrupaPotrzeb> grupy = mieszkancy.grupyPotrzeb();

        // seniorzy 65+ liczeni razem; zmarli i wykluczeni nie mają potrzeb
        assertThat(grupy).containsExactlyInAnyOrder(
                new GrupaPotrzeb(1, "A", "PODWYZSZONY", "SENIOR", 2),
                new GrupaPotrzeb(1, "C", "PODSTAWOWY", "DZIECKO", 1),
                new GrupaPotrzeb(2, "B", "PODSTAWOWY", "DOROSLY", 1));
    }

    @Test
    void prognozaMiastaDlaPodanegoIdAlboOstatniej() {
        jdbc.update("INSERT INTO prognoza (id, zrodlo, status) VALUES (1, 'ML', 'ZATWIERDZONA'), (2, 'ML', 'ZATWIERDZONA')");
        jdbc.update("""
                INSERT INTO prognoza_pozycja (prognoza_id, poziom, obiekt_id, zasob, ilosc) VALUES
                (1, 'MIASTO', NULL, 'WODA', 100), (2, 'MIASTO', NULL, 'WODA', 900),
                (2, 'DZIELNICA', 1, 'WODA', 500), (2, 'MIASTO', NULL, 'LEKI', 30)""");

        assertThat(prognozy.miasto(1L)).containsExactly(new IloscZasobu("WODA", 100.0));
        // bez id (start od „Ponownego planowania”) bierzemy ostatnią prognozę, tylko poziom MIASTO
        assertThat(prognozy.miasto(null)).containsExactly(new IloscZasobu("LEKI", 30.0), new IloscZasobu("WODA", 900.0));
    }

    @Test
    void prognozaMiastaGdyNieMaZadnejToPustaLista() {
        assertThat(prognozy.miasto(null)).isEmpty();
        assertThat(prognozy.miasto(77L)).isEmpty();
    }
}
