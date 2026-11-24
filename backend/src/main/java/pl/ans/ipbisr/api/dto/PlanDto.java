package pl.ans.ipbisr.api.dto;

import java.util.List;
import pl.ans.ipbisr.domain.IloscZasobu;

/** Plan przydziału zasobów (`GET /api/plany/ostatni`, `/api/plany/{id}`) – kształt z sekcji 8 planu etapu III. */
public record PlanDto(long id, Long prognozaId, boolean krytycznePokryte, String status, String zatwierdzil,
                      List<PlanPozycjaDto> pozycje, List<IloscZasobu> niedobory,
                      List<DostawaBezposredniaDto> dostawyBezposrednie, List<HarmonogramDto> harmonogram) {}
