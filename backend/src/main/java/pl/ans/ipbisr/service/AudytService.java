package pl.ans.ipbisr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

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
}
