package pl.ans.ipbisr.api;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Błędy API w jednym formacie z kontraktu: HTTP 4xx/5xx i `{"blad": "opis po polsku"}`. */
@RestControllerAdvice
public class BledyApiHandler {

    private static final Logger log = LoggerFactory.getLogger(BledyApiHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> zleDane(IllegalArgumentException e) {
        return odpowiedz(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> zlyJson(HttpMessageNotReadableException e) {
        return odpowiedz(HttpStatus.BAD_REQUEST, "Nieprawidłowy format JSON");
    }

    @ExceptionHandler(BrakZasobuException.class)
    public ResponseEntity<Map<String, String>> brak(BrakZasobuException e) {
        return odpowiedz(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> inne(Exception e) {
        log.error("Nieoczekiwany błąd API", e);
        return odpowiedz(HttpStatus.INTERNAL_SERVER_ERROR, "Błąd serwera: " + e.getMessage());
    }

    private static ResponseEntity<Map<String, String>> odpowiedz(HttpStatus status, String opis) {
        return ResponseEntity.status(status).body(Map.of("blad", opis));
    }
}
