package pl.ans.ipbisr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.enums.ProcessInstanceState;
import io.camunda.client.api.search.response.ProcessInstance;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.ans.ipbisr.api.dto.ProcesDto;

/** Lista instancji dla ekranu „Procesy”: mapowanie odpowiedzi Camundy i link do Operate. */
class ProcesyServiceTest {

    @Test
    void mapujeInstancjeNaDtoZLinkiemDoOperate() {
        CamundaClient camunda = mock(CamundaClient.class, RETURNS_DEEP_STUBS);
        ProcessInstance instancja = mock(ProcessInstance.class);
        when(instancja.getProcessInstanceKey()).thenReturn(2251799813685249L);
        when(instancja.getProcessDefinitionId()).thenReturn("Proces_Analiza_sytuacji");
        when(instancja.getState()).thenReturn(ProcessInstanceState.ACTIVE);
        when(instancja.getStartDate()).thenReturn(OffsetDateTime.parse("2026-11-22T10:00:00Z"));
        when(camunda.newProcessInstanceSearchRequest().sort(org.mockito.ArgumentMatchers.<java.util.function.Consumer<io.camunda.client.api.search.sort.ProcessInstanceSort>>any())
                .page(org.mockito.ArgumentMatchers.<java.util.function.Consumer<io.camunda.client.api.search.page.AnyPage>>any()).send().join().items())
                .thenReturn(List.of(instancja));

        // adres z końcowym ukośnikiem – nie może powstać podwójny „//”
        List<ProcesDto> wynik = new ProcesyService(camunda, "https://camunda.uczelnia.pl:8080/").ostatnieInstancje();

        assertThat(wynik).containsExactly(new ProcesDto("2251799813685249", "Proces_Analiza_sytuacji", "ACTIVE",
                "2026-11-22T10:00:00Z", "https://camunda.uczelnia.pl:8080/operate/processes/2251799813685249"));
    }
}
