package pl.ans.ipbisr.worker.p1;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import pl.ans.ipbisr.domain.Osoba;
import pl.ans.ipbisr.repository.EwidencjaRepository;
import pl.ans.ipbisr.service.KomunikatyService;

/** Workery procesu P1a „Rejestr ludności” (system zewnętrzny – symulowany). */
@Component
public class RejestrWorkers {

    private final EwidencjaRepository ewidencja;
    private final KomunikatyService komunikaty;

    public RejestrWorkers(EwidencjaRepository ewidencja, KomunikatyService komunikaty) {
        this.ewidencja = ewidencja;
        this.komunikaty = komunikaty;
    }

    /** „Odczytaj dane mieszkańców z ewidencji” – lista mieszkańców trafia do zmiennej `mieszkancy`. */
    @JobWorker(type = "odczytaj-dane-mieszkancow")
    public Map<String, Object> odczytajDaneMieszkancow() {
        List<Map<String, Object>> mieszkancy = ewidencja.wszyscy().stream().map(Osoba::doZmiennych).toList();
        return Map.of("mieszkancy", mieszkancy);
    }

    /** „Dane wysłane” (message end event) – odsyła listę do P1b wiadomością „Dane mieszkańców”. */
    @JobWorker(type = "wyslij-dane-mieszkancow")
    public void wyslijDaneMieszkancow(ActivatedJob job) {
        Map<String, Object> zmienne = job.getVariablesAsMap();
        // ten sam idZapytania, który przyszedł w „Zapytaniu o dane” → wiadomość trafi do właściwej instancji P1b
        String idZapytania = String.valueOf(zmienne.get("idZapytania"));
        komunikaty.publikuj("Dane mieszkańców", idZapytania,
                Map.of("idZapytania", idZapytania, "mieszkancy", zmienne.get("mieszkancy")));
    }
}
