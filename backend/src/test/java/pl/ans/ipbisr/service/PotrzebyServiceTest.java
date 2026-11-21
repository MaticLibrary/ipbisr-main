package pl.ans.ipbisr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.ans.ipbisr.api.dto.MagazynDto;
import pl.ans.ipbisr.domain.GrupaPotrzeb;
import pl.ans.ipbisr.domain.IloscZasobu;
import pl.ans.ipbisr.domain.Potrzeba;
import pl.ans.ipbisr.domain.PunktWydawania;
import pl.ans.ipbisr.repository.MagazynRepository;
import pl.ans.ipbisr.repository.MieszkaniecRepository;
import pl.ans.ipbisr.repository.PrognozaRepository;
import pl.ans.ipbisr.repository.PunktRepository;

/** Krok P3a „Pobierz stany magazynowe, potrzeby i prognozy” – logika bez bazy i bez Camundy (Mockito). */
@ExtendWith(MockitoExtension.class)
class PotrzebyServiceTest {

    @Mock MieszkaniecRepository mieszkancy;
    @Mock MagazynRepository magazyn;
    @Mock PrognozaRepository prognozy;
    @Mock PunktRepository punkty;
    @Mock NormyService normy;

    PotrzebyService service;

    @BeforeEach
    void setUp() {
        service = new PotrzebyService(mieszkancy, magazyn, prognozy, punkty, normy);
        // lenient: test „pusty rejestr” kończy się błędem, zanim serwis w ogóle zajrzy do magazynu i punktów
        lenient().when(magazyn.wszystkie())
                .thenReturn(List.of(new MagazynDto("WODA", 1000.0, "l"), new MagazynDto("LEKI", 50.0, "zestaw")));
        lenient().when(punkty.wszystkie()).thenReturn(List.of(new PunktWydawania(1, 800.0, List.of(1, 2))));
    }

    @Test
    void potrzebyBudynkuToLiczbaOsobPomnozonaPrzezNormeDobowa() {
        when(mieszkancy.grupyPotrzeb()).thenReturn(List.of(
                new GrupaPotrzeb(1, "A", "PODWYZSZONY", "SENIOR", 2),
                new GrupaPotrzeb(1, "C", "PODSTAWOWY", "DZIECKO", 3)));
        when(normy.normy("PODWYZSZONY", "SENIOR"))
                .thenReturn(List.of(new IloscZasobu("WODA", 4.0), new IloscZasobu("LEKI", 0.15)));
        when(normy.normy("PODSTAWOWY", "DZIECKO")).thenReturn(List.of(new IloscZasobu("WODA", 2.0)));
        when(prognozy.miasto(null)).thenReturn(List.of());

        var dane = service.zbierz(null);

        // 2 seniorów × 4 l = 8 l; 2 × 0,15 zestawu leków = 0,3 (bez błędów zaokrągleń); 3 dzieci × 2 l = 6 l
        assertThat(dane.potrzeby()).containsExactlyInAnyOrder(
                new Potrzeba(1, "WODA", "A", 8.0), new Potrzeba(1, "LEKI", "A", 0.3), new Potrzeba(1, "WODA", "C", 6.0));
    }

    @Test
    void tabeleDmnWywolujeJedenRazNaKombinacjeAJedenRazNaBudynek() {
        when(mieszkancy.grupyPotrzeb()).thenReturn(List.of(
                new GrupaPotrzeb(1, "A", "PODWYZSZONY", "SENIOR", 2),
                new GrupaPotrzeb(2, "A", "PODWYZSZONY", "SENIOR", 5)));
        when(normy.normy("PODWYZSZONY", "SENIOR")).thenReturn(List.of(new IloscZasobu("WODA", 4.0)));
        when(prognozy.miasto(null)).thenReturn(List.of());

        var dane = service.zbierz(null);

        verify(normy, times(1)).normy("PODWYZSZONY", "SENIOR");
        assertThat(dane.potrzeby()).containsExactlyInAnyOrder(
                new Potrzeba(1, "WODA", "A", 8.0), new Potrzeba(2, "WODA", "A", 20.0));
    }

    @Test
    void stanyIPunktyPochodzaZMagazynuIZBazy() {
        when(mieszkancy.grupyPotrzeb()).thenReturn(List.of(new GrupaPotrzeb(1, "C", "PODSTAWOWY", "DOROSLY", 1)));
        when(normy.normy("PODSTAWOWY", "DOROSLY")).thenReturn(List.of(new IloscZasobu("WODA", 3.0)));
        when(prognozy.miasto(null)).thenReturn(List.of());

        var dane = service.zbierz(null);

        assertThat(dane.stany()).containsExactly(new IloscZasobu("WODA", 1000.0), new IloscZasobu("LEKI", 50.0));
        assertThat(dane.punkty()).containsExactly(new PunktWydawania(1, 800.0, List.of(1, 2)));
    }

    @Test
    void prognozaDlaGrupyAToPrognozaMiastaPomnozonaPrzezUdzialPotrzebA() {
        when(mieszkancy.grupyPotrzeb()).thenReturn(List.of(
                new GrupaPotrzeb(1, "A", "PODWYZSZONY", "SENIOR", 2),
                new GrupaPotrzeb(1, "C", "PODSTAWOWY", "DOROSLY", 1)));
        when(normy.normy("PODWYZSZONY", "SENIOR"))
                .thenReturn(List.of(new IloscZasobu("WODA", 4.0), new IloscZasobu("LEKI", 0.2)));
        when(normy.normy("PODSTAWOWY", "DOROSLY")).thenReturn(List.of(new IloscZasobu("WODA", 2.0)));
        // WODA: A = 8, wszyscy = 10 → udział 80%; LEKI: potrzebują tylko osoby z A → 100%
        when(prognozy.miasto(7L)).thenReturn(List.of(new IloscZasobu("WODA", 1000.0), new IloscZasobu("LEKI", 30.0),
                new IloscZasobu("SCHRONIENIE", 9.0)));

        var dane = service.zbierz(7L);

        // zasób, którego nikt w rejestrze nie potrzebuje (SCHRONIENIE), nie trafia do prognozy dla grupy A
        assertThat(dane.prognozaA()).containsExactlyInAnyOrder(new IloscZasobu("WODA", 800.0), new IloscZasobu("LEKI", 30.0));
    }

    @Test
    void bezPrognozyPrognozaDlaGrupyAJestPusta() {
        when(mieszkancy.grupyPotrzeb()).thenReturn(List.of(new GrupaPotrzeb(1, "C", "PODSTAWOWY", "DOROSLY", 1)));
        when(normy.normy("PODSTAWOWY", "DOROSLY")).thenReturn(List.of(new IloscZasobu("WODA", 3.0)));
        when(prognozy.miasto(null)).thenReturn(List.of());

        assertThat(service.zbierz(null).prognozaA()).isEmpty();
    }

    @Test
    void pustyRejestrPotrzebToCzytelnyBladZamiastPustegoPlanu() {
        when(mieszkancy.grupyPotrzeb()).thenReturn(List.of());

        assertThatThrownBy(() -> service.zbierz(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("analiza P1");
    }
}
