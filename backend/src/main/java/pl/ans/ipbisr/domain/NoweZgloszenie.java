package pl.ans.ipbisr.domain;

/** Wejście `POST /api/zgloszenia`. */
public record NoweZgloszenie(String pesel, Integer budynekId, String rodzaj, String opis) {}
