package pl.ans.ipbisr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Symulacja P5: przycisk publikuje wiadomość z nowym `idZdarzenia` jako numerem sprawy i zapisuje wpis do audytu. */
@ExtendWith(MockitoExtension.class)
class SymulacjaServiceTest {

    @Mock KomunikatyService komunikaty;
    @Mock AudytService audyt;
    @Captor ArgumentCaptor<Map<String, Object>> zmienne;

    SymulacjaService service;

    @BeforeEach
    void setUp() {
        service = new SymulacjaService(komunikaty, audyt);
    }

    @Test
    void zmianaSytuacjiPublikujeWiadomoscZKluczemRownymIdZdarzeniaIPoziomem() {
        String id = service.zmianaSytuacji(2);

        verify(komunikaty).publikuj(eq("Zmiana sytuacji kryzysowej"), eq(id), zmienne.capture());
        assertThat(zmienne.getValue()).containsEntry("idZdarzenia", id).containsEntry("poziomZagrozenia", 2);
        verify(audyt).zapisz(eq("analityk"), eq("P5"), eq("SYMULACJA_ZMIANY_SYTUACJI"), any());
    }

    @Test
    void ponowneplanowanieBezPoziomuNieWysylaZmiennejPoziomu() {
        String id = service.ponownePlanowanie(null);

        verify(komunikaty).publikuj(eq("Ponowne planowanie"), eq(id), zmienne.capture());
        assertThat(zmienne.getValue()).containsOnlyKeys("idZdarzenia");
        verify(audyt).zapisz(eq("analityk"), eq("P5"), eq("SYMULACJA_PONOWNEGO_PLANOWANIA"), any());
    }

    @Test
    void kazdeWywolanieDajeInneIdZdarzenia() {
        assertThat(service.ponownePlanowanie(null)).isNotEqualTo(service.ponownePlanowanie(null));
    }

    @Test
    void poziomSpozaZakresuToBladIBrakWiadomosci() {
        assertThatThrownBy(() -> service.zmianaSytuacji(4)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.zmianaSytuacji(-1)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(komunikaty, audyt);
    }
}
