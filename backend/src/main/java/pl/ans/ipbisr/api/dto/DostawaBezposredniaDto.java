package pl.ans.ipbisr.api.dto;

/** Dostawa do domu mieszkańca, który nie może odebrać zasobów w punkcie wydawania. */
public record DostawaBezposredniaDto(String pesel, String zasob, double ilosc) {}
