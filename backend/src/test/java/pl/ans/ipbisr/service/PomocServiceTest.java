package pl.ans.ipbisr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.ans.ipbisr.domain.IloscZasobu;
import pl.ans.ipbisr.repository.WniosekRepository;

/** Wniosek o pomoc humanitarną (P3a → P3b) i odpowiedź organizacji (P3b → P3a) – bez bazy i bez Camundy (Mockito). */
@ExtendWith(MockitoExtension.class)
class PomocServiceTest {

    @Mock WniosekRepository wnioski;
    @Mock KomunikatyService komunikaty;
    @Mock AudytService audyt;

    PomocService service;

    private static final List<IloscZasobu> NIEDOBORY = List.of(new IloscZasobu("LEKI", 40.0), new IloscZasobu("WODA", 500.0));

    @BeforeEach
    void setUp() {
        service = new PomocService(wnioski, komunikaty, audyt);
    }

    @Test
    void wniosekZapisujeNiedoboryIWysylaWiadomoscZNumeremSprawy() {
        String idWniosku = service.wyslijWniosek(2, NIEDOBORY);

        verify(wnioski).dodaj(2, NIEDOBORY);
        ArgumentCaptor<Map<String, Object>> zmienne = ArgumentCaptor.captor();
        verify(komunikaty).publikuj(eq("Wniosek o pomoc"), eq(idWniosku), zmienne.capture());
        assertThat(zmienne.getValue()).containsEntry("idWniosku", idWniosku).containsEntry("idPlanu", 2L)
                .containsEntry("niedobory", NIEDOBORY);
        // formularz organizacji dostaje listę do uzupełnienia: wnioskowano = ile brakuje, przyznano na start = tyle samo
        assertThat(zmienne.getValue().get("przyznanoIlosci")).isEqualTo(List.of(
                Map.of("zasob", "LEKI", "wnioskowano", 40.0, "przyznano", 40.0),
                Map.of("zasob", "WODA", "wnioskowano", 500.0, "przyznano", 500.0)));
        verify(audyt).zapisz(eq("system"), eq("P3a"), eq("WNIOSEK_WYSLANY"), anyMap());
    }

    @Test
    void kazdyWniosekMaInnyNumerSprawy() {
        assertThat(service.wyslijWniosek(2, NIEDOBORY)).isNotEqualTo(service.wyslijWniosek(2, NIEDOBORY));
    }

    @Test
    void odpowiedzOrganizacjiZapisujePrzyznaneIloscIWysylaWiadomoscDoPlanu() {
        List<Map<String, Object>> przyznano = List.of(
                Map.of("zasob", "LEKI", "wnioskowano", 40, "przyznano", 30),
                Map.of("zasob", "WODA", "wnioskowano", 500.0, "przyznano", 0));

        service.zapiszOdpowiedz("abc", 2, przyznano, "2026-12-01");

        verify(wnioski).zapiszOdpowiedz(2, Map.of("LEKI", 30.0, "WODA", 0.0), LocalDate.of(2026, 12, 1));
        ArgumentCaptor<Map<String, Object>> zmienne = ArgumentCaptor.captor();
        verify(komunikaty).publikuj(eq("Odpowiedź na wniosek"), eq("abc"), zmienne.capture());
        assertThat(zmienne.getValue()).containsEntry("idWniosku", "abc").containsEntry("termin", "2026-12-01")
                .containsEntry("przyznanoIlosci", przyznano);
        // analityk zobaczy to podsumowanie w formularzu „Zatwierdź plan przydziału”
        assertThat((String) zmienne.getValue().get("podsumowaniePomocy"))
                .contains("LEKI 30 z 40").contains("WODA 0 z 500").contains("2026-12-01");
        verify(audyt).zapisz(eq("organizacje"), eq("P3b"), eq("ODPOWIEDZ_NA_WNIOSEK"), anyMap());
    }

    @Test
    void odpowiedzBezTerminuZapisujeNullJakoTermin() {
        service.zapiszOdpowiedz("abc", 2, List.of(Map.of("zasob", "LEKI", "wnioskowano", 40, "przyznano", 40)), null);

        verify(wnioski).zapiszOdpowiedz(2, Map.of("LEKI", 40.0), null);
    }
}
