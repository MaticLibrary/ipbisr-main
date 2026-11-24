package pl.ans.ipbisr.api;

/** Nie ma żądanego zasobu (np. budynku) – API odpowiada `404`. */
public class BrakZasobuException extends RuntimeException {
    public BrakZasobuException(String komunikat) {
        super(komunikat);
    }
}
