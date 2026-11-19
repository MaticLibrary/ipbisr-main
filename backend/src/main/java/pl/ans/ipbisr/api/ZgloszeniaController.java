package pl.ans.ipbisr.api;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.ans.ipbisr.domain.NoweZgloszenie;
import pl.ans.ipbisr.domain.Zgloszenie;
import pl.ans.ipbisr.repository.ZgloszenieRepository;
import pl.ans.ipbisr.service.ZgloszenieService;

/** Zgłoszenia zdarzeń od operatora punktu (ekran „Operator”). */
@RestController
@RequestMapping("/api/zgloszenia")
public class ZgloszeniaController {

    private final ZgloszenieService service;
    private final ZgloszenieRepository repozytorium;

    public ZgloszeniaController(ZgloszenieService service, ZgloszenieRepository repozytorium) {
        this.service = service;
        this.repozytorium = repozytorium;
    }

    /** Odpowiedź `201`: `{ id, status, utworzono }`. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> dodaj(@RequestBody NoweZgloszenie nowe) {
        Zgloszenie z = service.zarejestruj(nowe);
        return Map.of("id", z.id(), "status", z.status(), "utworzono", z.utworzono());
    }

    @GetMapping
    public List<Zgloszenie> lista() {
        return repozytorium.wszystkie();
    }
}
