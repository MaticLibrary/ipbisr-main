package pl.ans.ipbisr.api.dto;

/** Jedna pozycja prognozy. `poziom` ∈ MIASTO, DZIELNICA, BUDYNEK; `obiektId` = id dzielnicy/budynku (dla miasta null). */
public record PrognozaPozycjaDto(String poziom, Integer obiektId, String zasob, double ilosc) {}
