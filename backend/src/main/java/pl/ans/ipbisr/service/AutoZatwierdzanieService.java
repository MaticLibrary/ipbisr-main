package pl.ans.ipbisr.service;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.enums.UserTaskState;
import io.camunda.client.api.search.response.UserTask;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Z opisu procesu P3: „plan niezatwierdzony w 2 h → zatwierdzony automatycznie”. Na diagramie nie ma timera
 * brzegowego (nie wolno zmieniać modeli), więc robi to backend: co minutę szuka otwartych zadań
 * „Zatwierdź plan przydziału” starszych niż 2 h i kończy je bez decyzji. Worker `wyslij-zlecenia-wydania`
 * traktuje brak decyzji jak zatwierdzenie i zapisuje w audycie `ZATWIERDZONO_AUTOMATYCZNIE` (patrz PlanService).
 * Analogiczna reguła dla prognozy P2 (1 h) dojdzie razem z procesem P2 Roberta.
 */
@Service
public class AutoZatwierdzanieService {

    private static final Logger log = LoggerFactory.getLogger(AutoZatwierdzanieService.class);

    static final String PROCES_PRZYDZIALU = "Proces_Przydzial";
    static final String ZADANIE_ZATWIERDZ_PLAN = "T5";
    static final Duration LIMIT_PLANU = Duration.ofHours(2);

    private final CamundaClient camunda;

    public AutoZatwierdzanieService(CamundaClient camunda) {
        this.camunda = camunda;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void sprawdzCoMinute() {
        try {
            zatwierdzPrzeterminowane(OffsetDateTime.now());
        } catch (RuntimeException e) {
            // brak połączenia z Camundą nie może zatrzymać harmonogramu – spróbujemy za minutę
            log.warn("Nie udało się sprawdzić zadań do automatycznego zatwierdzenia: {}", e.getMessage());
        }
    }

    /** Kończy otwarte zadania „Zatwierdź plan przydziału” starsze niż limit. Zwraca liczbę zakończonych. */
    int zatwierdzPrzeterminowane(OffsetDateTime teraz) {
        int zakonczone = 0;
        for (UserTask zadanie : camunda.newUserTaskSearchRequest()
                .filter(f -> f.state(UserTaskState.CREATED).elementId(ZADANIE_ZATWIERDZ_PLAN))
                .send().join().items()) {
            if (!PROCES_PRZYDZIALU.equals(zadanie.getBpmnProcessId()) || !przeterminowane(zadanie, teraz)) {
                continue;
            }
            // bez zmiennej `decyzja` – PlanService uzna to za zatwierdzenie automatyczne
            camunda.newCompleteUserTaskCommand(zadanie.getUserTaskKey())
                    .variables(Map.of("autoZatwierdzono", true))
                    .send().join();
            log.info("Plan (zadanie {}) zatwierdzony automatycznie po {}", zadanie.getUserTaskKey(), LIMIT_PLANU);
            zakonczone++;
        }
        return zakonczone;
    }

    private static boolean przeterminowane(UserTask zadanie, OffsetDateTime teraz) {
        return zadanie.getCreationDate() != null && zadanie.getCreationDate().plus(LIMIT_PLANU).isBefore(teraz);
    }
}
