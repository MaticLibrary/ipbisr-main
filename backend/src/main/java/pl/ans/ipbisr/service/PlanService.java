package pl.ans.ipbisr.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import pl.ans.ipbisr.domain.ZlecenieWydania;
import pl.ans.ipbisr.repository.PlanRepository;

/**
 * Logika planu przydziału (P3a) po decyzji analityka. Model nie ma bramki po zadaniu „Zatwierdź plan przydziału”
 * (przepływ idzie zawsze do „Wyślij zlecenia wydania”), więc odrzucenie planu obsługujemy tutaj, w kodzie:
 * plan dostaje status ODRZUCONY, a zlecenia dla magazynu nie powstają.
 */
@Service
public class PlanService {

    private final PlanRepository plany;
    private final KomunikatyService komunikaty;
    private final AudytService audyt;

    public PlanService(PlanRepository plany, KomunikatyService komunikaty, AudytService audyt) {
        this.plany = plany;
        this.komunikaty = komunikaty;
        this.audyt = audyt;
    }

    /**
     * Krok „Wyślij zlecenia wydania do magazynu”.
     *
     * @param decyzja ZATWIERDZONY / ODRZUCONY z formularza analityka; null, gdy nikt nie decydował
     *                (automatyczne zatwierdzenie po 2 h – M5) – plan traktujemy wtedy jak zatwierdzony
     */
    public void zakoncz(long planId, String decyzja, String komentarz) {
        Map<String, Object> szczegoly = new HashMap<>();
        szczegoly.put("planId", planId);
        szczegoly.put("komentarz", komentarz);

        if ("ODRZUCONY".equals(decyzja)) {
            plany.ustawStatus(planId, "ODRZUCONY", "analityk");
            audyt.zapisz("analityk", "P3a", "ODRZUCONO_PLAN", szczegoly);
            return;
        }

        String aktor = decyzja == null ? "system" : "analityk";
        plany.ustawStatus(planId, "ZATWIERDZONY", aktor);
        plany.utworzZlecenia(planId);
        List<ZlecenieWydania> zlecenia = plany.zlecenia(planId);
        // numer sprawy = id planu; wiadomość czeka na P4 (jeszcze go nie ma, więc Camunda ją po prostu przechowa)
        komunikaty.publikuj("Zlecenie wydania", String.valueOf(planId), Map.of("idPlanu", planId, "zlecenia", zlecenia));
        szczegoly.put("liczbaZlecen", zlecenia.size());
        audyt.zapisz(aktor, "P3a", decyzja == null ? "ZATWIERDZONO_AUTOMATYCZNIE" : "ZATWIERDZONO_PLAN", szczegoly);
    }
}
