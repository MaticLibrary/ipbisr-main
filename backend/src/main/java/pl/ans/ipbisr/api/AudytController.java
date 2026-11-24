package pl.ans.ipbisr.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.ans.ipbisr.api.dto.AudytDto;
import pl.ans.ipbisr.service.AudytService;

/** Dziennik decyzji (ekran „Audyt”). */
@RestController
@RequestMapping("/api/audyt")
public class AudytController {

    private final AudytService audyt;

    public AudytController(AudytService audyt) {
        this.audyt = audyt;
    }

    /** Najnowsze wpisy; `limit` 1–1000 (domyślnie 100), inaczej HTTP 400. */
    @GetMapping
    public List<AudytDto> ostatnie(@RequestParam(defaultValue = "100") int limit) {
        return audyt.ostatnie(limit);
    }
}
