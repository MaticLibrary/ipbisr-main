package pl.ans.ipbisr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.EvaluateDecisionResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.ans.ipbisr.domain.IloscZasobu;

/** Odczyt wyniku tabeli DMN „Normy dzienne” (sama tabela jest sprawdzona w NormyDmnTest na prawdziwej Camundzie). */
@ExtendWith(MockitoExtension.class)
class NormyServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS) CamundaClient camunda;

    private void dmnZwraca(String wynikJson) {
        EvaluateDecisionResponse odpowiedz = mock(EvaluateDecisionResponse.class);
        when(odpowiedz.getDecisionOutput()).thenReturn(wynikJson);
        when(camunda.newEvaluateDecisionCommand().decisionId("Normy_dzienne")
                .variables(Map.<String, Object>of("poziomPotrzeb", "PODWYZSZONY", "grupaWiekowa", "SENIOR"))
                .send().join()).thenReturn(odpowiedz);
    }

    @Test
    void zamieniaWyjsciaTabeliNaZasobyIPomijaZerowe() {
        dmnZwraca("{\"wodaL\":4,\"zywnoscKg\":0.6,\"lekiZestaw\":0.15,\"higienaZestaw\":0.15,\"schronienie\":0}");

        var normy = new NormyService(camunda).normy("PODWYZSZONY", "SENIOR");

        // schronienie = 0 nie jest potrzebą; kolejność zasobów jest stała (jak w bazie)
        assertThat(normy).containsExactly(
                new IloscZasobu("ZYWNOSC", 0.6), new IloscZasobu("WODA", 4.0),
                new IloscZasobu("LEKI", 0.15), new IloscZasobu("HIGIENA", 0.15));
    }

    @Test
    void nieoczekiwanyWynikDmnToCzytelnyBlad() {
        dmnZwraca("nie-json");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new NormyService(camunda).normy("PODWYZSZONY", "SENIOR"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PODWYZSZONY").hasMessageContaining("SENIOR");
    }
}
