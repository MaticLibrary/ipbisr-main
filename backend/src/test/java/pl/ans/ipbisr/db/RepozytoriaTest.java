package pl.ans.ipbisr.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.ans.ipbisr.api.dto.BudynekDto;
import pl.ans.ipbisr.api.dto.MieszkaniecDto;
import pl.ans.ipbisr.domain.Osoba;
import pl.ans.ipbisr.domain.WynikPriorytetu;
import pl.ans.ipbisr.repository.BudynekRepository;
import pl.ans.ipbisr.repository.EwidencjaRepository;
import pl.ans.ipbisr.repository.MieszkaniecRepository;
import pl.ans.ipbisr.repository.ZgloszenieRepository;
import pl.ans.ipbisr.service.AudytService;

/** Zapytania SQL repozytoriów P1 na prawdziwym PostgreSQL (Testcontainers). */
@Import({EwidencjaRepository.class, MieszkaniecRepository.class, BudynekRepository.class,
        ZgloszenieRepository.class, AudytService.class})
class RepozytoriaTest extends PostgresJdbcTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired EwidencjaRepository ewidencja;
    @Autowired MieszkaniecRepository mieszkancy;
    @Autowired BudynekRepository budynki;
    @Autowired ZgloszenieRepository zgloszenia;
    @Autowired AudytService audyt;

    @BeforeEach
    void dane() {
        jdbc.update("INSERT INTO dzielnica VALUES (1, 'Centrum'), (2, 'Północ')");
        jdbc.update("INSERT INTO punkt_wydawania VALUES (1, 'Punkt 1', 1, 1000)");
        jdbc.update("INSERT INTO budynek VALUES (1, 'Rynek 1', 1, 1), (2, 'Długa 5', 2, 1), (3, 'Pusta 9', 1, 1)");
        // Anna (80 lat, budynek 1), Jan (zdrowy 30-latek, budynek 1), Ewa (zmarła, budynek 2)
        jdbc.update("""
                INSERT INTO ewidencja_ludnosci (pesel, imie, nazwisko, plec, data_ur, budynek_id, stan_zdrowia, status)
                VALUES ('11111111111', 'Anna', 'Nowak', 'K', now() - interval '80 years 1 month', 1, 'DOBRY', 'ZYJE'),
                       ('22222222222', 'Jan', 'Kowal', 'M', now() - interval '30 years 1 month', 1, 'DOBRY', 'ZYJE'),
                       ('33333333333', 'Ewa', 'Lis', 'K', now() - interval '50 years 1 month', 2, 'DOBRY', 'NIE_ZYJE')
                """);
    }

    @Test
    void ewidencjaLicziWiekIZnajdujeOsoby() {
        List<Osoba> wszyscy = ewidencja.wszyscy();

        assertThat(wszyscy).hasSize(3);
        assertThat(ewidencja.znajdz("11111111111")).get().extracting(Osoba::wiek).isEqualTo(80);
        assertThat(ewidencja.znajdz("00000000000")).isEmpty();
        assertThat(ewidencja.peseleWBudynku(1)).containsExactlyInAnyOrder("11111111111", "22222222222");
        assertThat(ewidencja.budynekIstnieje(2)).isTrue();
        assertThat(ewidencja.budynekIstnieje(99)).isFalse();
    }

    @Test
    void ewidencjaAktualizujeStanOsoby() {
        ewidencja.ustawStatus("11111111111", "NIE_ZYJE");
        ewidencja.ustawStanZdrowia("22222222222", "CHOROBA");
        ewidencja.ustawUtrateMieszkania("22222222222");

        assertThat(ewidencja.znajdz("11111111111")).get().extracting(Osoba::status).isEqualTo("NIE_ZYJE");
        assertThat(ewidencja.znajdz("22222222222")).get()
                .extracting(Osoba::stanZdrowia, Osoba::utrataMieszkania).containsExactly("CHOROBA", true);
    }

    @Test
    void mieszkaniecZapisujeINadpisujeWyniki() {
        mieszkancy.zapisz(List.of(new WynikPriorytetu("11111111111", 1, "ZYJE", "A", "PODWYZSZONY", true)));
        mieszkancy.zapisz(List.of(new WynikPriorytetu("11111111111", 1, "NIE_ZYJE", "WYKLUCZONY", "PODSTAWOWY", false)));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM mieszkaniec", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT priorytet FROM mieszkaniec", String.class)).isEqualTo("WYKLUCZONY");
    }

    @Test
    void szukaMieszkancowZFiltrami() {
        mieszkancy.zapisz(List.of(new WynikPriorytetu("11111111111", 1, "ZYJE", "A", "PODWYZSZONY", true)));

        List<MieszkaniecDto> wszyscy = mieszkancy.szukaj(null, null, null);
        assertThat(wszyscy).hasSize(3);
        // niepoliczony mieszkaniec ma priorytet null i dostawę false
        assertThat(wszyscy).filteredOn(m -> m.pesel().equals("22222222222")).singleElement()
                .satisfies(m -> {
                    assertThat(m.priorytet()).isNull();
                    assertThat(m.dostawaBezposrednia()).isFalse();
                });
        assertThat(mieszkancy.szukaj("A", null, null)).extracting(MieszkaniecDto::pesel)
                .containsExactly("11111111111");
        assertThat(mieszkancy.szukaj(null, "NIE_ZYJE", null)).extracting(MieszkaniecDto::pesel)
                .containsExactly("33333333333");
        assertThat(mieszkancy.szukaj(null, null, 1)).hasSize(2);
        MieszkaniecDto anna = mieszkancy.szukaj("A", "ZYJE", 1).get(0);
        assertThat(anna).extracting(MieszkaniecDto::imie, MieszkaniecDto::wiek, MieszkaniecDto::poziomPotrzeb,
                MieszkaniecDto::dostawaBezposrednia).containsExactly("Anna", 80, "PODWYZSZONY", true);
    }

    @Test
    void budynekMaNajwyzszyPriorytetIliczbeZywychMieszkancow() {
        mieszkancy.zapisz(List.of(
                new WynikPriorytetu("11111111111", 1, "ZYJE", "B", "PODWYZSZONY", false),
                new WynikPriorytetu("22222222222", 1, "ZYJE", "A", "KRYTYCZNY", true),
                new WynikPriorytetu("33333333333", 2, "NIE_ZYJE", "WYKLUCZONY", "PODSTAWOWY", false)));

        List<BudynekDto> lista = budynki.szukaj(null);

        assertThat(lista).extracting(BudynekDto::id).containsExactly(1, 2, 3);
        assertThat(lista.get(0)).extracting(BudynekDto::priorytet, BudynekDto::liczbaMieszkancow,
                BudynekDto::dzielnica).containsExactly("A", 2, "Centrum");
        // budynek 2: tylko osoba wykluczona → brak priorytetu, 0 żywych
        assertThat(lista.get(1)).extracting(BudynekDto::priorytet, BudynekDto::liczbaMieszkancow)
                .containsExactly(null, 0);
        // budynek 3: nikt nie mieszka i P1 niczego nie policzył
        assertThat(lista.get(2).priorytet()).isNull();
        assertThat(budynki.szukaj(2)).extracting(BudynekDto::id).containsExactly(2);
        assertThat(budynki.znajdz(1)).isPresent();
        assertThat(budynki.znajdz(99)).isEmpty();
    }

    @Test
    void zgloszenieMaCyklZycia() {
        long id = zgloszenia.dodaj("11111111111", null, "SMIERC", "zgłoszone przez sąsiada");

        var nowe = zgloszenia.znajdz(id).orElseThrow();
        assertThat(nowe).extracting("pesel", "budynekId", "status", "losowaKontrola", "decyzja")
                .containsExactly("11111111111", null, "NOWE", false, null);

        zgloszenia.ustawLosowaKontrole(id, true);
        zgloszenia.ustawStatus(id, "PRZEKAZANE");
        zgloszenia.zakoncz(id, "POTWIERDZONE");

        var gotowe = zgloszenia.znajdz(id).orElseThrow();
        assertThat(gotowe).extracting("losowaKontrola", "decyzja", "status")
                .containsExactly(true, "POTWIERDZONE", "ZAKONCZONE");
        assertThat(zgloszenia.wszystkie()).hasSize(1);
    }

    @Test
    void zgloszenieDlaBudynkuNieMaPesel() {
        long id = zgloszenia.dodaj(null, 3, "UTRATA_MIESZKANIA", null);

        assertThat(zgloszenia.znajdz(id).orElseThrow()).extracting("pesel", "budynekId")
                .containsExactly(null, 3);
    }

    @Test
    void audytZapisujeSzczegolyJakoJson() {
        audyt.zapisz("zarzad", "P1b", "ZWERYFIKOWANO", Map.of("decyzja", "POTWIERDZONE"));

        assertThat(jdbc.queryForObject("SELECT szczegoly ->> 'decyzja' FROM audyt WHERE proces = 'P1b'", String.class))
                .isEqualTo("POTWIERDZONE");
    }
}
