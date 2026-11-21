package pl.ans.ipbisr.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.ans.ipbisr.api.dto.PlanDto;
import pl.ans.ipbisr.repository.PlanRepository;

/** Plany przydziału zasobów (ekran „Plan przydziału”). */
@RestController
@RequestMapping("/api/plany")
public class PlanyController {

    private final PlanRepository plany;

    public PlanyController(PlanRepository plany) {
        this.plany = plany;
    }

    /** Najnowszy plan z pozycjami, niedoborami, dostawami bezpośrednimi i harmonogramem; 404, gdy nie powstał żaden. */
    @GetMapping("/ostatni")
    public PlanDto ostatni() {
        return plany.ostatni().orElseThrow(() -> new BrakZasobuException("Nie ma jeszcze żadnego planu przydziału"));
    }

    @GetMapping("/{id}")
    public PlanDto plan(@PathVariable long id) {
        return plany.znajdz(id).orElseThrow(() -> new BrakZasobuException("Nie ma planu o id " + id));
    }
}
