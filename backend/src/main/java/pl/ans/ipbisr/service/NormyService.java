package pl.ans.ipbisr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.EvaluateDecisionResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import pl.ans.ipbisr.domain.IloscZasobu;

/**
 * Wywołuje wdrożoną w Camundzie tabelę DMN „Normy dzienne” (dmn/Priorytet_mieszkancow.dmn): ile zasobów na dobę
 * potrzebuje jedna osoba o danym poziomie potrzeb i w danej grupie wiekowej. Zmiana norm = zmiana tabeli DMN.
 */
@Service
public class NormyService {

    static final String DECYZJA = "Normy_dzienne";

    /** Kolumna wyjściowa tabeli DMN → nazwa zasobu w bazie; kolejność jak w stanach magazynowych. */
    private static final Map<String, String> ZASOBY = new LinkedHashMap<>();

    static {
        ZASOBY.put("zywnoscKg", "ZYWNOSC");
        ZASOBY.put("wodaL", "WODA");
        ZASOBY.put("lekiZestaw", "LEKI");
        ZASOBY.put("higienaZestaw", "HIGIENA");
        ZASOBY.put("schronienie", "SCHRONIENIE");
    }

    private final CamundaClient camunda;
    private final ObjectMapper json = new ObjectMapper();

    public NormyService(CamundaClient camunda) {
        this.camunda = camunda;
    }

    /** Dobowe normy jednej osoby; zasoby z normą 0 (np. schronienie dla osoby w podstawowej potrzebie) pomijamy. */
    public List<IloscZasobu> normy(String poziomPotrzeb, String grupaWiekowa) {
        try {
            EvaluateDecisionResponse odpowiedz = camunda.newEvaluateDecisionCommand()
                    .decisionId(DECYZJA)
                    .variables(Map.<String, Object>of("poziomPotrzeb", poziomPotrzeb, "grupaWiekowa", grupaWiekowa))
                    .send().join();
            Map<String, Object> wynik = json.readValue(odpowiedz.getDecisionOutput(), new TypeReference<>() {});
            List<IloscZasobu> normy = new ArrayList<>();
            ZASOBY.forEach((kolumna, zasob) -> {
                double ilosc = ((Number) wynik.get(kolumna)).doubleValue();
                if (ilosc > 0) {
                    normy.add(new IloscZasobu(zasob, ilosc));
                }
            });
            return normy;
        } catch (Exception e) {
            throw new IllegalStateException("Nieoczekiwany wynik DMN „Normy dzienne” dla poziomu potrzeb "
                    + poziomPotrzeb + " i grupy wiekowej " + grupaWiekowa, e);
        }
    }
}
