package pl.ans.ipbisr.api.dto;

/** Pozycja planu: ile zasobu przydzielono budynkowi dla grupy priorytetu i ile potrzebował. */
public record PlanPozycjaDto(int budynekId, String zasob, String priorytet, double przydzielono, double potrzeba) {}
