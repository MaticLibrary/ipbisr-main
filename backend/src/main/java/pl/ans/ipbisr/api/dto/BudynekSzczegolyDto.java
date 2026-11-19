package pl.ans.ipbisr.api.dto;

import java.util.List;

/** `GET /api/budynki/{id}` – budynek z mieszkańcami. */
public record BudynekSzczegolyDto(int id, String adres, int dzielnicaId, String dzielnica,
                                  List<MieszkaniecDto> mieszkancy) {}
