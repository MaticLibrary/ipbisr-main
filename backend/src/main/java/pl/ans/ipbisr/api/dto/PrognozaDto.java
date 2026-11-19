package pl.ans.ipbisr.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Prognoza zapotrzebowania. `pozycje` jest wypełnione tylko w `GET /api/prognozy/ostatnia`
 * (na liście `GET /api/prognozy` = null i pole znika z JSON-a). `roznicaProc` może być null.
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record PrognozaDto(long id, OffsetDateTime utworzono, String zrodlo,
                          @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
                          Double roznicaProc,
                          String status, List<PrognozaPozycjaDto> pozycje) {}
