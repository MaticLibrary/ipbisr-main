package pl.ans.ipbisr.worker.p1;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import org.springframework.stereotype.Component;
import pl.ans.ipbisr.service.ZgloszenieService;

/** Worker procesu P1c „Rejestracja zgłoszenia”. */
@Component
public class ZgloszenieWorkers {

    private final ZgloszenieService zgloszenia;

    public ZgloszenieWorkers(ZgloszenieService zgloszenia) {
        this.zgloszenia = zgloszenia;
    }

    /** „Przekaż zgłoszenie do centrum” – wiadomość „Zgłoszenie zdarzenia” uruchamia P1b. */
    @JobWorker(type = "przekaz-zgloszenie")
    public void przekazZgloszenie(ActivatedJob job) {
        long id = ((Number) job.getVariablesAsMap().get("idZgloszenia")).longValue();
        zgloszenia.przekaz(id);
    }
}
