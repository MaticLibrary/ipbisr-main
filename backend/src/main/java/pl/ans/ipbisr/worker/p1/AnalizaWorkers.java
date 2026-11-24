package pl.ans.ipbisr.worker.p1;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pl.ans.ipbisr.service.AnalizaService;
import pl.ans.ipbisr.service.KomunikatyService;

/** Workery procesu P1b „Analiza sytuacji mieszkańców”. */
@Component
public class AnalizaWorkers {

    private final AnalizaService analiza;
    private final KomunikatyService komunikaty;

    public AnalizaWorkers(AnalizaService analiza, KomunikatyService komunikaty) {
        this.analiza = analiza;
        this.komunikaty = komunikaty;
    }

    /**
     * „Wyślij zapytanie o dane mieszkańców” – tworzy numer sprawy `idZapytania` i wysyła „Zapytanie o dane” do P1a.
     * Zmienna `zgloszenie` ustawiamy zawsze (null, gdy proces ruszył od timera), bo bramka na nią patrzy.
     */
    @JobWorker(type = "wyslij-zapytanie-o-dane")
    public Map<String, Object> wyslijZapytanieODane(ActivatedJob job) {
        String idZapytania = UUID.randomUUID().toString();
        komunikaty.publikuj("Zapytanie o dane", idZapytania, Map.of("idZapytania", idZapytania));

        Map<String, Object> wynik = new HashMap<>();
        wynik.put("idZapytania", idZapytania);
        wynik.put("zgloszenie", job.getVariablesAsMap().get("zgloszenie"));
        return wynik;
    }

    /** „Wyznacz potrzeby i priorytet” – tabela DMN dla każdego mieszkańca (patrz AnalizaService). */
    @JobWorker(type = "wyznacz-potrzeby-i-priorytet")
    public Map<String, Object> wyznaczPotrzebyIPriorytet(ActivatedJob job) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mieszkancy = (List<Map<String, Object>>) job.getVariablesAsMap().get("mieszkancy");
        return Map.of("wyniki", analiza.wyznacz(mieszkancy));
    }

    /** „Zapisz wyniki w rejestrze potrzeb”. */
    @JobWorker(type = "zapisz-wyniki")
    public void zapiszWyniki(ActivatedJob job) {
        Map<String, Object> zmienne = job.getVariablesAsMap();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> wyniki = (List<Map<String, Object>>) zmienne.get("wyniki");
        @SuppressWarnings("unchecked")
        Map<String, Object> zgloszenie = (Map<String, Object>) zmienne.get("zgloszenie");
        analiza.zapiszWyniki(wyniki, zgloszenie, (String) zmienne.get("decyzja"), (String) zmienne.get("komentarz"));
    }
}
