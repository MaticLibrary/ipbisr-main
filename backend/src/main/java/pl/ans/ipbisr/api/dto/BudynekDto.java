package pl.ans.ipbisr.api.dto;

/** Element listy `GET /api/budynki`. `priorytet` = najwyższy priorytet mieszkańca; null, dopóki P1 nie przeliczył. */
public record BudynekDto(int id, String adres, int dzielnicaId, String dzielnica, String priorytet,
                         int liczbaMieszkancow) {}
