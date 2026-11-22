package pl.ans.ipbisr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.filter.UserTaskFilter;
import io.camunda.client.api.search.response.UserTask;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

/** Automatyczne zatwierdzenie planu po 2 h (opis P3) – logika wyboru zadań, bez prawdziwej Camundy. */
class AutoZatwierdzanieServiceTest {

    private static final OffsetDateTime TERAZ = OffsetDateTime.parse("2026-11-22T12:00:00Z");

    CamundaClient camunda = mock(CamundaClient.class, RETURNS_DEEP_STUBS);
    AutoZatwierdzanieService service;

    @BeforeEach
    void setUp() {
        service = new AutoZatwierdzanieService(camunda);
    }

    private static UserTask zadanie(long klucz, String proces, OffsetDateTime utworzone) {
        UserTask z = mock(UserTask.class);
        when(z.getUserTaskKey()).thenReturn(klucz);
        when(z.getBpmnProcessId()).thenReturn(proces);
        when(z.getCreationDate()).thenReturn(utworzone);
        return z;
    }

    private void otwarteZadania(UserTask... zadania) {
        when(camunda.newUserTaskSearchRequest()
                .filter(ArgumentMatchers.<Consumer<UserTaskFilter>>any()).send().join().items())
                .thenReturn(List.of(zadania));
    }

    @Test
    void konczyTylkoPlanyStarszeNizDwieGodziny() {
        otwarteZadania(
                zadanie(1, "Proces_Przydzial", TERAZ.minusHours(3)),
                zadanie(2, "Proces_Przydzial", TERAZ.minusMinutes(119)));

        assertThat(service.zatwierdzPrzeterminowane(TERAZ)).isEqualTo(1);

        verify(camunda.newCompleteUserTaskCommand(1L)).variables(Map.of("autoZatwierdzono", true));
        verify(camunda, never()).newCompleteUserTaskCommand(2L);
    }

    @Test
    void pomijaZadaniaInnychProcesowOTejSamejNazwieElementu() {
        otwarteZadania(zadanie(3, "Proces_Analiza_sytuacji", TERAZ.minusHours(10)));

        assertThat(service.zatwierdzPrzeterminowane(TERAZ)).isZero();

        verify(camunda, never()).newCompleteUserTaskCommand(anyLong());
    }

    @Test
    void brakZadanDoZatwierdzeniaToZero() {
        otwarteZadania();

        assertThat(service.zatwierdzPrzeterminowane(TERAZ)).isZero();
    }
}
