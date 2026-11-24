package pl.ans.ipbisr.api.dto;

/** Stan magazynowy jednego zasobu (`GET/PUT /api/magazyn`). */
public record MagazynDto(String zasob, double ilosc, String jednostka) {}
