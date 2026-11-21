package pl.ans.ipbisr.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.ans.ipbisr.api.dto.MagazynDto;
import pl.ans.ipbisr.service.MagazynService;

/** Stany magazynowe (ekran „Magazyn”): odczyt i zmiana na demo. */
@RestController
@RequestMapping("/api/magazyn")
public class MagazynController {

    private final MagazynService magazyn;

    public MagazynController(MagazynService magazyn) {
        this.magazyn = magazyn;
    }

    @GetMapping
    public List<MagazynDto> stany() {
        return magazyn.wszystkie();
    }

    /** Wejście i odpowiedź: ta sama lista `{zasob, ilosc, jednostka}`. */
    @PutMapping
    public List<MagazynDto> zapisz(@RequestBody List<MagazynDto> stany) {
        return magazyn.zapisz(stany);
    }
}
