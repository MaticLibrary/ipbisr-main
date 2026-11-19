package pl.ans.ipbisr.service;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import io.camunda.client.api.search.response.UserTask;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Uruchamianie procesów i obsługa user tasków, które w naszym systemie klika frontend, a nie Tasklist. */
@Service
public class ProcesyService {

    private static final Logger log = LoggerFactory.getLogger(ProcesyService.class);

    static final String PROCES_REJESTRACJI = "Proces_Rejestracja_zgloszenia";
    private static final int PROBY = 40;
    private static final long PAUZA_MS = 500;

    private final CamundaClient camunda;

    public ProcesyService(CamundaClient camunda) {
        this.camunda = camunda;
    }

    /**
     * P1c: startuje „Rejestrację zgłoszenia” i od razu kończy user task „Zarejestruj zgłoszenie”
     * (operator wypełnił już formularz we frontendzie, więc nie musi go klikać drugi raz w Tasklist).
     */
    public void uruchomRejestracjeIZakonczZadanie(long idZgloszenia) {
        ProcessInstanceEvent proces = camunda.newCreateInstanceCommand()
                .bpmnProcessId(PROCES_REJESTRACJI).latestVersion()
                .variables(Map.of("idZgloszenia", idZgloszenia))
                .send().join();
        long zadanie = czekajNaZadanie(proces.getProcessInstanceKey());
        camunda.newCompleteUserTaskCommand(zadanie).send().join();
        log.info("Zgłoszenie {}: zadanie operatora {} zakończone (instancja {})", idZgloszenia, zadanie, proces.getProcessInstanceKey());
    }

    /**
     * User task pojawia się w wyszukiwarce Camundy z niewielkim opóźnieniem (dane trafiają tam asynchronicznie),
     * dlatego próbujemy kilka razy.
     */
    private long czekajNaZadanie(long kluczInstancji) {
        for (int i = 0; i < PROBY; i++) {
            List<UserTask> zadania = camunda.newUserTaskSearchRequest()
                    .filter(f -> f.processInstanceKey(kluczInstancji))
                    .send().join().items();
            if (!zadania.isEmpty()) {
                return zadania.get(0).getUserTaskKey();
            }
            try {
                Thread.sleep(PAUZA_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Przerwano oczekiwanie na zadanie operatora", e);
            }
        }
        throw new IllegalStateException("Camunda nie utworzyła zadania „Zarejestruj zgłoszenie” (instancja "
                + kluczInstancji + ")");
    }
}
