package pl.ans.ipbisr.worker.p3;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import pl.ans.ipbisr.service.PomocService;

/** Worker procesu P3b „Organizacje humanitarne” (system zewnętrzny – symulowany, zadanie klikamy w Tasklist). */
@Component
public class OrganizacjeWorkers {

    private final PomocService pomoc;

    public OrganizacjeWorkers(PomocService pomoc) {
        this.pomoc = pomoc;
    }

    /** „Odpowiedź wysłana” (message end event) – odsyła do P3a wiadomość „Odpowiedź na wniosek” z tym samym `idWniosku`. */
    @JobWorker(type = "wyslij-odpowiedz-na-wniosek")
    public void wyslijOdpowiedzNaWniosek(ActivatedJob job) {
        Map<String, Object> zmienne = job.getVariablesAsMap();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> przyznanoIlosci = (List<Map<String, Object>>) zmienne.get("przyznanoIlosci");
        pomoc.zapiszOdpowiedz((String) zmienne.get("idWniosku"), ((Number) zmienne.get("idPlanu")).longValue(),
                przyznanoIlosci, (String) zmienne.get("termin"));
    }
}
