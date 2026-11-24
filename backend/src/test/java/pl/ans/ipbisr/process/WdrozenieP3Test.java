package pl.ans.ipbisr.process;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.DeploymentEvent;
import io.camunda.process.test.api.CamundaProcessTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Smoke test: modele P3a i P3b oraz ich formularze wdrażają się do prawdziwej Camundy (kontener Dockera).
 * Camunda sprawdza przy wdrożeniu składnię BPMN, wyrażeń FEEL (np. warunek bramki) i odwołania do wiadomości.
 * Testy kolejnych ścieżek procesu (z bramką „Potrzeby krytyczne pokryte?”) są w planie jako M6.
 */
@CamundaProcessTest
@Tag("camunda")
class WdrozenieP3Test {

    private CamundaClient client;

    @Test
    void modeleP3aIP3bWdrazajSieRazemZFormularzami() throws Exception {
        DeploymentEvent wdrozenie = client.newDeployResourceCommand()
                .addResourceFromClasspath("bpmn/forms/Zatwierdz_plan_przydzialu.form")
                .addResourceFromClasspath("bpmn/forms/Rozpatrz_wniosek.form")
                .addResourceFromClasspath("bpmn/P3a_Przydzial_zasobow.bpmn")
                .addResourceFromClasspath("bpmn/P3b_Organizacje_humanitarne.bpmn")
                .send().join();

        assertThat(wdrozenie.getProcesses()).extracting(p -> p.getBpmnProcessId())
                .containsExactlyInAnyOrder("Proces_Przydzial", "Proces_Organizacje_humanitarne");
        assertThat(wdrozenie.getForm()).extracting(f -> f.getFormId())
                .containsExactlyInAnyOrder("Zatwierdz_plan_przydzialu", "Rozpatrz_wniosek");
    }
}
