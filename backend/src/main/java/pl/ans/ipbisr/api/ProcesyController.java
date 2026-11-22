package pl.ans.ipbisr.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.ans.ipbisr.api.dto.ProcesDto;
import pl.ans.ipbisr.service.ProcesyService;

/** Podgląd uruchomionych procesów (ekran „Procesy”). */
@RestController
@RequestMapping("/api/procesy")
public class ProcesyController {

    private final ProcesyService procesy;

    public ProcesyController(ProcesyService procesy) {
        this.procesy = procesy;
    }

    /** Ostatnie instancje procesów z Camundy, najnowsze pierwsze. */
    @GetMapping
    public List<ProcesDto> lista() {
        return procesy.ostatnieInstancje();
    }
}
