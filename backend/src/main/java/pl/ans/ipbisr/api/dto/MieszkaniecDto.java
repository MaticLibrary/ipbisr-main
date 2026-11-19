package pl.ans.ipbisr.api.dto;

/** `<mieszkaniec>` z kontraktu API (sekcja 8 planu). */
public record MieszkaniecDto(String pesel, String imie, String nazwisko, int wiek, int budynekId,
                             String priorytet, String poziomPotrzeb, boolean dostawaBezposrednia, String status) {}
