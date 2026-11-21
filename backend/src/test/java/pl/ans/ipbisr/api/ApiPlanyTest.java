package pl.ans.ipbisr.api;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.ans.ipbisr.api.dto.DostawaBezposredniaDto;
import pl.ans.ipbisr.api.dto.HarmonogramDto;
import pl.ans.ipbisr.api.dto.PlanDto;
import pl.ans.ipbisr.api.dto.PlanPozycjaDto;
import pl.ans.ipbisr.config.WebConfig;
import pl.ans.ipbisr.domain.IloscZasobu;
import pl.ans.ipbisr.repository.PlanRepository;

/** Kontrakt REST dla ekranu „Plan przydziału” (sekcja 8 planu): ścieżki, format JSON i błędy. */
@WebMvcTest(PlanyController.class)
@Import({WebConfig.class, BledyApiHandler.class})
class ApiPlanyTest {

    @Autowired MockMvc mvc;
    @MockitoBean PlanRepository plany;

    private static PlanDto plan() {
        return new PlanDto(2, 5L, false, "DO_ZATWIERDZENIA", null,
                List.of(new PlanPozycjaDto(3, "WODA", "A", 60.0, 60.0)),
                List.of(new IloscZasobu("LEKI", 40.0)),
                List.of(new DostawaBezposredniaDto("50010112345", "LEKI", 1.0)),
                List.of(new HarmonogramDto(1, "08:00-10:00", 3)));
    }

    @Test
    void ostatniPlanMaWszystkiePolaZKontraktu() throws Exception {
        when(plany.ostatni()).thenReturn(Optional.of(plan()));

        mvc.perform(get("/api/plany/ostatni"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.prognozaId").value(5))
                .andExpect(jsonPath("$.krytycznePokryte").value(false))
                .andExpect(jsonPath("$.status").value("DO_ZATWIERDZENIA"))
                .andExpect(jsonPath("$.pozycje[0].budynekId").value(3))
                .andExpect(jsonPath("$.pozycje[0].zasob").value("WODA"))
                .andExpect(jsonPath("$.pozycje[0].priorytet").value("A"))
                .andExpect(jsonPath("$.pozycje[0].przydzielono").value(60.0))
                .andExpect(jsonPath("$.pozycje[0].potrzeba").value(60.0))
                .andExpect(jsonPath("$.niedobory[0].zasob").value("LEKI"))
                .andExpect(jsonPath("$.niedobory[0].ilosc").value(40.0))
                .andExpect(jsonPath("$.dostawyBezposrednie[0].pesel").value("50010112345"))
                .andExpect(jsonPath("$.harmonogram[0].punktId").value(1))
                .andExpect(jsonPath("$.harmonogram[0].okno").value("08:00-10:00"))
                .andExpect(jsonPath("$.harmonogram[0].budynekId").value(3));
    }

    @Test
    void niezatwierdzonyPlanMaZatwierdzilJakoJawnyNull() throws Exception {
        when(plany.ostatni()).thenReturn(Optional.of(plan()));

        // kontrakt: pole `zatwierdzil` jest zawsze w JSON-ie, a null znaczy „jeszcze nikt nie zatwierdził”
        // (exists() nie nadaje się do null-a, dlatego sprawdzamy klucz osobno)
        mvc.perform(get("/api/plany/ostatni"))
                .andExpect(jsonPath("$", hasKey("zatwierdzil")))
                .andExpect(jsonPath("$.zatwierdzil", nullValue()));
    }

    @Test
    void planBezPrognozyMaPrognozaIdNull() throws Exception {
        when(plany.znajdz(2)).thenReturn(Optional.of(new PlanDto(2, null, true, "ZATWIERDZONY", "analityk",
                List.of(), List.of(), List.of(), List.of())));

        mvc.perform(get("/api/plany/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prognozaId").isEmpty())
                .andExpect(jsonPath("$.zatwierdzil").value("analityk"))
                .andExpect(jsonPath("$.pozycje").isEmpty());
    }

    @Test
    void brakPlanowToBlad404ZOpisem() throws Exception {
        when(plany.ostatni()).thenReturn(Optional.empty());

        mvc.perform(get("/api/plany/ostatni"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.blad").exists());
    }

    @Test
    void nieznanyPlanToBlad404() throws Exception {
        when(plany.znajdz(99)).thenReturn(Optional.empty());

        mvc.perform(get("/api/plany/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.blad").exists());
    }

    @Test
    void idPlanuKtoreNieJestLiczbaToBlad400NieSerwera() throws Exception {
        mvc.perform(get("/api/plany/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.blad").exists());
    }
}
