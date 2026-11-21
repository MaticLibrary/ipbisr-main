package pl.ans.ipbisr.worker.p3;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import pl.ans.ipbisr.domain.IloscZasobu;
import pl.ans.ipbisr.service.PlanService;
import pl.ans.ipbisr.service.PomocService;
import pl.ans.ipbisr.service.PotrzebyService;
import pl.ans.ipbisr.service.PotrzebyService.DanePrzydzialu;

/**
 * Workery procesu P3a „Przydział zasobów”.
 * Krok „Wygeneruj plan przydziału” (`wygeneruj-plan-przydzialu`, algorytm LP z serwisu ML `/allocate`) dojdzie,
 * gdy Robert skończy `/allocate` – patrz docs/PLAN_ETAP3.md, sekcja M4.
 */
@Component
public class PrzydzialWorkers {

    private final PotrzebyService potrzeby;
    private final PomocService pomoc;
    private final PlanService plan;

    public PrzydzialWorkers(PotrzebyService potrzeby, PomocService pomoc, PlanService plan) {
        this.potrzeby = potrzeby;
        this.pomoc = pomoc;
        this.plan = plan;
    }

    /**
     * „Pobierz stany magazynowe, potrzeby i prognozy” – wynik (stany, potrzeby, prognozaA, punkty) trafia do zmiennych
     * procesu i jest wejściem algorytmu przydziału. `idPrognozy` przychodzi z „Prognozy gotowej”; po „Ponownym
     * planowaniu” go nie ma, więc ustawiamy je zawsze (null) – żeby zmienna istniała.
     */
    @JobWorker(type = "pobierz-dane-do-przydzialu")
    public Map<String, Object> pobierzDaneDoPrzydzialu(ActivatedJob job) {
        Long idPrognozy = liczbaLubNull(job.getVariablesAsMap().get("idPrognozy"));
        DanePrzydzialu dane = potrzeby.zbierz(idPrognozy);

        Map<String, Object> wynik = new HashMap<>();
        wynik.put("stany", dane.stany());
        wynik.put("potrzeby", dane.potrzeby());
        wynik.put("prognozaA", dane.prognozaA());
        wynik.put("punkty", dane.punkty());
        wynik.put("idPrognozy", idPrognozy);
        return wynik;
    }

    /** „Wyślij wniosek o pomoc humanitarną” – zwraca `idWniosku`, po którym P3a odbierze odpowiedź organizacji. */
    @JobWorker(type = "wyslij-wniosek-o-pomoc")
    public Map<String, Object> wyslijWniosekOPomoc(ActivatedJob job) {
        Map<String, Object> zmienne = job.getVariablesAsMap();
        long idPlanu = ((Number) zmienne.get("idPlanu")).longValue();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> niedobory = (List<Map<String, Object>>) zmienne.get("niedobory");
        List<IloscZasobu> braki = niedobory.stream()
                .map(n -> new IloscZasobu((String) n.get("zasob"), ((Number) n.get("ilosc")).doubleValue()))
                .toList();
        return Map.of("idWniosku", pomoc.wyslijWniosek(idPlanu, braki));
    }

    /** „Wyślij zlecenia wydania do magazynu” – po decyzji analityka z formularza (patrz PlanService). */
    @JobWorker(type = "wyslij-zlecenia-wydania")
    public void wyslijZleceniaWydania(ActivatedJob job) {
        Map<String, Object> zmienne = job.getVariablesAsMap();
        plan.zakoncz(((Number) zmienne.get("idPlanu")).longValue(), (String) zmienne.get("decyzja"),
                (String) zmienne.get("komentarz"));
    }

    /** Numer z wiadomości może przyjść jako liczba albo tekst; brak = null. */
    private static Long liczbaLubNull(Object wartosc) {
        if (wartosc == null) {
            return null;
        }
        return wartosc instanceof Number n ? n.longValue() : Long.parseLong(wartosc.toString());
    }
}
