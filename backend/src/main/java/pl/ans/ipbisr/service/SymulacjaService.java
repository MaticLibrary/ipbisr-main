package pl.ans.ipbisr.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Symulacja procesu P5 (nie ma go jeszcze): zamiast ostrzeżenia z Systemu ostrzegania OC przycisk we frontendzie
 * publikuje wiadomość do Camundy. Numer sprawy (correlation key) to nowy `idZdarzenia`, więc każde kliknięcie
 * startuje nową instancję procesu.
 */
@Service
public class SymulacjaService {

    private final KomunikatyService komunikaty;
    private final AudytService audyt;

    public SymulacjaService(KomunikatyService komunikaty, AudytService audyt) {
        this.komunikaty = komunikaty;
        this.audyt = audyt;
    }

    /** Wiadomość „Zmiana sytuacji kryzysowej” (odbiera P2b). */
    public String zmianaSytuacji(Integer poziomZagrozenia) {
        return wyslij("Zmiana sytuacji kryzysowej", "SYMULACJA_ZMIANY_SYTUACJI", poziomZagrozenia);
    }

    /** Wiadomość „Ponowne planowanie” (odbiera P3a). */
    public String ponownePlanowanie(Integer poziomZagrozenia) {
        return wyslij("Ponowne planowanie", "SYMULACJA_PONOWNEGO_PLANOWANIA", poziomZagrozenia);
    }

    private String wyslij(String wiadomosc, String akcja, Integer poziomZagrozenia) {
        if (poziomZagrozenia != null && (poziomZagrozenia < 0 || poziomZagrozenia > 3)) {
            throw new IllegalArgumentException("Poziom zagrożenia musi być liczbą od 0 do 3");
        }
        String idZdarzenia = UUID.randomUUID().toString();
        Map<String, Object> zmienne = new HashMap<>();
        zmienne.put("idZdarzenia", idZdarzenia);
        if (poziomZagrozenia != null) {
            zmienne.put("poziomZagrozenia", poziomZagrozenia);
        }
        komunikaty.publikuj(wiadomosc, idZdarzenia, zmienne);

        Map<String, Object> szczegoly = new HashMap<>();
        szczegoly.put("idZdarzenia", idZdarzenia);
        szczegoly.put("poziomZagrozenia", poziomZagrozenia);
        audyt.zapisz("analityk", "P5", akcja, szczegoly);
        return idZdarzenia;
    }
}
