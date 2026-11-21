package pl.ans.ipbisr.domain;

/** Ilość jednego zasobu (ZYWNOSC, WODA, LEKI, HIGIENA, SCHRONIENIE) – stan magazynu, prognoza albo niedobór. */
public record IloscZasobu(String zasob, double ilosc) {}
