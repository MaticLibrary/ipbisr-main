package pl.ans.ipbisr.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import pl.ans.ipbisr.api.dto.MagazynDto;
import pl.ans.ipbisr.repository.MagazynRepository;

/** Stany magazynowe: odczyt i zmiana na demo (ekran „Magazyn”) z walidacją i wpisem do audytu. */
@Service
public class MagazynService {

    static final Set<String> ZASOBY = Set.of("ZYWNOSC", "WODA", "LEKI", "HIGIENA", "SCHRONIENIE");

    private final MagazynRepository magazyn;
    private final AudytService audyt;

    public MagazynService(MagazynRepository magazyn, AudytService audyt) {
        this.magazyn = magazyn;
        this.audyt = audyt;
    }

    public List<MagazynDto> wszystkie() {
        return magazyn.wszystkie();
    }

    /** `PUT /api/magazyn`: zapisuje podane stany (pozostałe zasoby bez zmian) i zwraca aktualną listę. */
    public List<MagazynDto> zapisz(List<MagazynDto> stany) {
        waliduj(stany);
        magazyn.zapisz(stany);
        audyt.zapisz("analityk", "P3", "MAGAZYN_ZMIENIONY", Map.of("stany", stany));
        return magazyn.wszystkie();
    }

    private static void waliduj(List<MagazynDto> stany) {
        if (stany == null || stany.isEmpty()) {
            throw new IllegalArgumentException("Podaj co najmniej jeden stan magazynowy");
        }
        for (MagazynDto s : stany) {
            if (s.zasob() == null || !ZASOBY.contains(s.zasob())) {
                throw new IllegalArgumentException(
                        "Nieznany zasób „" + s.zasob() + "”, dozwolone: " + String.join(", ", new TreeSet<>(ZASOBY)));
            }
            if (s.ilosc() < 0) {
                throw new IllegalArgumentException("Ilość zasobu " + s.zasob() + " nie może być ujemna");
            }
            if (s.jednostka() == null || s.jednostka().isBlank()) {
                throw new IllegalArgumentException("Podaj jednostkę dla zasobu " + s.zasob());
            }
        }
    }
}
