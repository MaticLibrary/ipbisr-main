package pl.ans.ipbisr.api.dto;

/** Okno wydawania: budynek odbiera zasoby w punkcie w podanym czasie (np. „08:00-10:00”). */
public record HarmonogramDto(int punktId, String okno, int budynekId) {}
