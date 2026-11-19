package pl.ans.ipbisr.api;

import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.ans.ipbisr.api.dto.PrognozaDto;
import pl.ans.ipbisr.api.dto.ZuzycieDto;
import pl.ans.ipbisr.repository.PrognozaRepository;
import pl.ans.ipbisr.repository.ZuzycieRepository;

/** Prognozy i historia zużycia (ekran „Prognoza” – wykres). */
@RestController
@RequestMapping("/api")
public class PrognozyController {

    private static final Set<String> ZASOBY = Set.of("ZYWNOSC", "WODA", "LEKI", "HIGIENA", "SCHRONIENIE");

    private final PrognozaRepository prognozy;
    private final ZuzycieRepository zuzycie;

    public PrognozyController(PrognozaRepository prognozy, ZuzycieRepository zuzycie) {
        this.prognozy = prognozy;
        this.zuzycie = zuzycie;
    }

    /** Ostatnia prognoza z pozycjami; 404, gdy P2 nie zapisał jeszcze żadnej. */
    @GetMapping("/prognozy/ostatnia")
    public PrognozaDto ostatnia() {
        return prognozy.ostatnia().orElseThrow(() -> new BrakZasobuException("Nie ma jeszcze żadnej prognozy"));
    }

    @GetMapping("/prognozy")
    public List<PrognozaDto> lista() {
        return prognozy.wszystkie();
    }

    /** `zasob` jest wymagany; bez `dzielnica` zwraca sumę dla całego miasta. */
    @GetMapping("/zuzycie/historia")
    public List<ZuzycieDto> historia(@RequestParam(required = false) Integer dzielnica,
                                     @RequestParam(required = false) String zasob) {
        if (zasob == null || !ZASOBY.contains(zasob)) {
            throw new IllegalArgumentException("Podaj zasób: " + String.join(", ", new java.util.TreeSet<>(ZASOBY)));
        }
        return zuzycie.historia(dzielnica, zasob);
    }
}
