package pl.ans.ipbisr.api.dto;

import java.time.LocalDate;

/** Jeden punkt historii zużycia do wykresu: dzień i ilość. */
public record ZuzycieDto(LocalDate data, double ilosc) {}
