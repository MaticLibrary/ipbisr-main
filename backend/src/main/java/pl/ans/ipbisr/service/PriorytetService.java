package pl.ans.ipbisr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.EvaluateDecisionResponse;
import java.util.Map;
import org.springframework.stereotype.Service;
import pl.ans.ipbisr.domain.Osoba;
import pl.ans.ipbisr.domain.WynikPriorytetu;

/**
 * Wywołuje wdrożoną w Camundzie tabelę DMN „Priorytet” (dmn/Priorytet_mieszkancow.dmn) dla jednego mieszkańca.
 * Zmiana reguły = zmiana tabeli DMN, bez zmiany tego kodu.
 */
@Service
public class PriorytetService {

    static final String DECYZJA = "Priorytet_mieszkancow";

    private final CamundaClient camunda;
    private final ObjectMapper json = new ObjectMapper();

    public PriorytetService(CamundaClient camunda) {
        this.camunda = camunda;
    }

    public WynikPriorytetu oblicz(Osoba osoba) {
        EvaluateDecisionResponse odpowiedz = camunda.newEvaluateDecisionCommand()
                .decisionId(DECYZJA)
                .variables(osoba.doDmn())
                .send().join();
        try {
            Map<String, Object> wynik = json.readValue(odpowiedz.getDecisionOutput(), new TypeReference<>() {});
            return new WynikPriorytetu(osoba.pesel(), osoba.budynekId(), osoba.status(),
                    (String) wynik.get("priorytet"), (String) wynik.get("poziomPotrzeb"),
                    (Boolean) wynik.get("dostawaBezposrednia"));
        } catch (Exception e) {
            throw new IllegalStateException("Nieoczekiwany wynik DMN dla PESEL " + osoba.pesel(), e);
        }
    }
}
