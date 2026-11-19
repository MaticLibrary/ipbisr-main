package pl.ans.ipbisr.api;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.ans.ipbisr.api.dto.BudynekDto;
import pl.ans.ipbisr.api.dto.BudynekSzczegolyDto;
import pl.ans.ipbisr.api.dto.MieszkaniecDto;
import pl.ans.ipbisr.repository.BudynekRepository;
import pl.ans.ipbisr.repository.MieszkaniecRepository;

/** Budynki i mieszkańcy z priorytetem (ekran „Budynki i mieszkańcy”). */
@RestController
@RequestMapping("/api")
public class BudynkiController {

    private final BudynekRepository budynki;
    private final MieszkaniecRepository mieszkancy;

    public BudynkiController(BudynekRepository budynki, MieszkaniecRepository mieszkancy) {
        this.budynki = budynki;
        this.mieszkancy = mieszkancy;
    }

    @GetMapping("/budynki")
    public List<BudynekDto> lista(@RequestParam(required = false) Integer dzielnica) {
        return budynki.szukaj(dzielnica);
    }

    @GetMapping("/budynki/{id}")
    public BudynekSzczegolyDto szczegoly(@PathVariable int id) {
        BudynekDto b = budynki.znajdz(id).orElseThrow(() -> new BrakZasobuException("Nie ma budynku o ID " + id));
        return new BudynekSzczegolyDto(b.id(), b.adres(), b.dzielnicaId(), b.dzielnica(),
                mieszkancy.szukaj(null, null, id));
    }

    @GetMapping("/mieszkancy")
    public List<MieszkaniecDto> mieszkancy(@RequestParam(required = false) String priorytet,
                                           @RequestParam(required = false) String status) {
        return mieszkancy.szukaj(priorytet, status, null);
    }
}
