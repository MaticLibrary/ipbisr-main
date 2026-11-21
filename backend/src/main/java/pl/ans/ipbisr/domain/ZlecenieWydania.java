package pl.ans.ipbisr.domain;

/** Zlecenie dla magazynu: ile zasobu wydać do punktu wydawania (wiadomość „Zlecenie wydania” dla przyszłego P4). */
public record ZlecenieWydania(int punktId, String zasob, double ilosc) {}
