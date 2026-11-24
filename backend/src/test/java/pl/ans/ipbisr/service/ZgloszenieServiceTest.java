package pl.ans.ipbisr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.ans.ipbisr.domain.NoweZgloszenie;
import pl.ans.ipbisr.domain.Osoba;
import pl.ans.ipbisr.domain.Zgloszenie;
import pl.ans.ipbisr.repository.EwidencjaRepository;
import pl.ans.ipbisr.repository.ZgloszenieRepository;

/** Rejestracja zgłoszenia (API) i krok „Przekaż zgłoszenie do centrum” (P1c). */
@ExtendWith(MockitoExtension.class)
class ZgloszenieServiceTest {

    @Mock ZgloszenieRepository zgloszenia;
    @Mock EwidencjaRepository ewidencja;
    @Mock KomunikatyService komunikaty;
    @Mock ProcesyService procesy;
    @Mock AudytService audyt;

    ZgloszenieService service;
    double los = 0.5; // „losowanie” sterowane z testu

    @BeforeEach
    void setUp() {
        service = new ZgloszenieService(zgloszenia, ewidencja, komunikaty, procesy, audyt, () -> los);
    }

    private static Osoba osoba() {
        return new Osoba("50010112345", 1, "ZYJE", 40, "DOBRY", false, "BRAK", false, false, 0);
    }

    private static Zgloszenie zapisane(String rodzaj, boolean kontrola) {
        return new Zgloszenie(12, "50010112345", null, rodzaj, "opis", kontrola, null, "NOWE", OffsetDateTime.now());
    }

    // ----- rejestracja (POST /api/zgloszenia) -----

    @Test
    void rejestrujeZgloszenieStartujeProcesIKonczyZadanieOperatora() {
        when(ewidencja.znajdz("50010112345")).thenReturn(Optional.of(osoba()));
        when(zgloszenia.dodaj("50010112345", null, "SMIERC", "opis")).thenReturn(12L);
        when(zgloszenia.znajdz(12)).thenReturn(Optional.of(zapisane("SMIERC", false)));

        Zgloszenie wynik = service.zarejestruj(new NoweZgloszenie("50010112345", null, "SMIERC", "opis"));

        assertThat(wynik.id()).isEqualTo(12);
        verify(procesy).uruchomRejestracjeIZakonczZadanie(12);
        verify(audyt).zapisz(eq("operator"), eq("P1c"), eq("ZAREJESTROWANO"), any());
    }

    @Test
    void wymagaPeselAlboBudynek() {
        assertThatThrownBy(() -> service.zarejestruj(new NoweZgloszenie(null, null, "SMIERC", null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("PESEL");
        verify(zgloszenia, never()).dodaj(any(), any(), any(), any());
    }

    @Test
    void odrzucaNieznanyRodzaj() {
        assertThatThrownBy(() -> service.zarejestruj(new NoweZgloszenie("50010112345", null, "KRADZIEZ", null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("rodzaj");
    }

    @Test
    void odrzucaNieznanyPesel() {
        when(ewidencja.znajdz("99999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.zarejestruj(new NoweZgloszenie("99999999999", null, "SMIERC", null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("PESEL");
    }

    @Test
    void odrzucaNieznanyBudynek() {
        when(ewidencja.budynekIstnieje(999)).thenReturn(false);

        assertThatThrownBy(() -> service.zarejestruj(new NoweZgloszenie(null, 999, "UTRATA_MIESZKANIA", null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("udynek");
    }

    @Test
    void zgloszenieDlaSamegoBudynkuTylkoDlaUtratyMieszkania() {
        assertThatThrownBy(() -> service.zarejestruj(new NoweZgloszenie(null, 3, "SMIERC", null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("PESEL");
    }

    @Test
    void przyBleduProceduOznaczaZgloszenieJakoBlad() {
        when(ewidencja.znajdz("50010112345")).thenReturn(Optional.of(osoba()));
        when(zgloszenia.dodaj(any(), any(), any(), any())).thenReturn(12L);
        org.mockito.Mockito.doThrow(new IllegalStateException("Camunda niedostępna"))
                .when(procesy).uruchomRejestracjeIZakonczZadanie(12);

        assertThatThrownBy(() -> service.zarejestruj(new NoweZgloszenie("50010112345", null, "SMIERC", null)))
                .isInstanceOf(IllegalStateException.class);
        verify(zgloszenia).ustawStatus(12, "BLAD_PROCESU");
    }

    // ----- krok P1c „Przekaż zgłoszenie do centrum” -----

    @Test
    void przekazujeZgloszenieWiadomosciaZLosowaKontrola() {
        los = 0.1; // < 20% → wylosowana kontrola
        when(zgloszenia.znajdz(12)).thenReturn(Optional.of(zapisane("CHOROBA", false)),
                Optional.of(zapisane("CHOROBA", true)));

        service.przekaz(12);

        verify(zgloszenia).ustawLosowaKontrole(12, true);
        verify(zgloszenia).ustawStatus(12, "PRZEKAZANE");
        ArgumentCaptor<Map<String, Object>> zmienne = ArgumentCaptor.forClass(Map.class);
        verify(komunikaty).publikuj(eq("Zgłoszenie zdarzenia"), eq("12"), zmienne.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> zgloszenie = (Map<String, Object>) zmienne.getValue().get("zgloszenie");
        assertThat(zgloszenie).containsEntry("rodzaj", "CHOROBA").containsEntry("losowaKontrola", true);
    }

    @Test
    void bezLosowejKontroliPowyzej20Procent() {
        los = 0.2; // granica: dokładnie 20% już NIE jest kontrolą
        when(zgloszenia.znajdz(12)).thenReturn(Optional.of(zapisane("CHOROBA", false)));

        service.przekaz(12);

        verify(zgloszenia).ustawLosowaKontrole(12, false);
    }

    @Test
    void przekazanieNieistniejacegoZgloszeniaToBlad() {
        when(zgloszenia.znajdz(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.przekaz(5)).isInstanceOf(IllegalStateException.class);
    }
}
