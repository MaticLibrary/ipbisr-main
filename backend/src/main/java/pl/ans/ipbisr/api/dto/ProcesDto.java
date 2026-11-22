package pl.ans.ipbisr.api.dto;

/** Instancja procesu z Camundy (`GET /api/procesy`) – do ekranu „Procesy” z linkiem do Operate. */
public record ProcesDto(String instancjaId, String proces, String stan, String start, String operateUrl) {}
