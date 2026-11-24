package pl.ans.ipbisr.domain;

/** Dobowa potrzeba budynku: ile zasobu potrzebują mieszkańcy danej grupy priorytetu (A/B/C). Wejście algorytmu przydziału. */
public record Potrzeba(int budynekId, String zasob, String priorytet, double ilosc) {}
