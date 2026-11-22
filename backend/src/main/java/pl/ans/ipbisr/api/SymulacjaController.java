package pl.ans.ipbisr.api;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.ans.ipbisr.api.dto.SymulacjaDto;
import pl.ans.ipbisr.service.SymulacjaService;

/** Przyciski „Symulacja” – zastępują proces P5 (ostrzeżenie z Systemu ostrzegania OC), którego jeszcze nie ma. */
@RestController
@RequestMapping("/api/symulacja")
public class SymulacjaController {

    private final SymulacjaService symulacja;

    public SymulacjaController(SymulacjaService symulacja) {
        this.symulacja = symulacja;
    }

    /** Publikuje „Zmianę sytuacji kryzysowej” (start P2). */
    @PostMapping("/zmiana-sytuacji")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> zmianaSytuacji(@RequestBody(required = false) SymulacjaDto wejscie) {
        return Map.of("idZdarzenia", symulacja.zmianaSytuacji(poziom(wejscie)));
    }

    /** Publikuje „Ponowne planowanie” (start P3a). */
    @PostMapping("/ponowne-planowanie")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> ponownePlanowanie(@RequestBody(required = false) SymulacjaDto wejscie) {
        return Map.of("idZdarzenia", symulacja.ponownePlanowanie(poziom(wejscie)));
    }

    private static Integer poziom(SymulacjaDto wejscie) {
        return wejscie == null ? null : wejscie.poziomZagrozenia();
    }
}
