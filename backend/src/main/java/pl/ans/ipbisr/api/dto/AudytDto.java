package pl.ans.ipbisr.api.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/** Wpis dziennika decyzji (`GET /api/audyt`); `szczegoly` to dowolny obiekt JSON. */
public record AudytDto(OffsetDateTime czas, String aktor, String proces, String akcja, Map<String, Object> szczegoly) {}
