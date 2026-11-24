package pl.ans.ipbisr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import pl.ans.ipbisr.api.dto.AudytDto;

/** Dziennik decyzji (tabela `audyt`) – wymaganie „pełna rejestrowalność decyzji”. */
@Service
public class AudytService {

    private final JdbcClient jdbc;
    private final ObjectMapper json = new ObjectMapper();

    public AudytService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Dopisuje wpis: kto (aktor), w którym procesie, co zrobił i szczegóły (JSON). */
    public void zapisz(String aktor, String proces, String akcja, Map<String, Object> szczegoly) {
        try {
            jdbc.sql("INSERT INTO audyt (aktor, proces, akcja, szczegoly) VALUES (?, ?, ?, CAST(? AS JSONB))")
                    .params(aktor, proces, akcja, json.writeValueAsString(szczegoly))
                    .update();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Nie można zapisać szczegółów audytu", e);
        }
    }

    /** Najnowsze wpisy dziennika (najpierw najnowsze). `limit` musi być w zakresie 1–1000. */
    public List<AudytDto> ostatnie(int limit) {
        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("Parametr limit musi być liczbą od 1 do 1000");
        }
        return jdbc.sql("SELECT czas, aktor, proces, akcja, szczegoly::text AS szczegoly FROM audyt "
                        + "ORDER BY czas DESC, id DESC LIMIT ?")
                .param(limit)
                .query((rs, i) -> new AudytDto(rs.getObject("czas", OffsetDateTime.class), rs.getString("aktor"),
                        rs.getString("proces"), rs.getString("akcja"), szczegoly(rs.getString("szczegoly"))))
                .list();
    }

    private Map<String, Object> szczegoly(String tekstJson) {
        if (tekstJson == null) {
            return Map.of();
        }
        try {
            return json.readValue(tekstJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Nieprawidłowe szczegóły audytu w bazie", e);
        }
    }
}
