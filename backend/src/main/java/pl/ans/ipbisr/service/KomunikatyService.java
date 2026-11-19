package pl.ans.ipbisr.service;

import io.camunda.client.CamundaClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Publikowanie wiadomości do Camundy – tak procesy „rozmawiają” ze sobą.
 * Correlation key to „numer sprawy”: Camunda dostarcza wiadomość do tej instancji procesu, która ma ten sam klucz.
 * TTL = 1 h: jeśli odbiorca jeszcze nie czeka (np. P1a odpowie szybciej, niż P1b dojdzie do „Odbierz dane”),
 * Camunda przechowa wiadomość i dostarczy ją później.
 */
@Service
public class KomunikatyService {

    static final Duration TTL = Duration.ofHours(1);

    private final CamundaClient camunda;

    public KomunikatyService(CamundaClient camunda) {
        this.camunda = camunda;
    }

    public void publikuj(String nazwa, String korelacja, Map<String, Object> zmienne) {
        camunda.newPublishMessageCommand()
                .messageName(nazwa)
                .correlationKey(korelacja)
                .timeToLive(TTL)
                .variables(zmienne)
                .send().join();
    }
}
