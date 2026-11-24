package pl.ans.ipbisr.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.ans.ipbisr.api.dto.PrognozaDto;
import pl.ans.ipbisr.api.dto.PrognozaPozycjaDto;
import pl.ans.ipbisr.api.dto.ZuzycieDto;
import pl.ans.ipbisr.config.WebConfig;
import pl.ans.ipbisr.repository.PrognozaRepository;
import pl.ans.ipbisr.repository.ZuzycieRepository;

/** Kontrakt REST dla ekranu „Prognoza” (sekcja 8 planu): ścieżki, format JSON i błędy. */
@WebMvcTest(PrognozyController.class)
@Import({WebConfig.class, BledyApiHandler.class})
class ApiPrognozyTest {

    @Autowired MockMvc mvc;
    @MockitoBean PrognozaRepository prognozy;
    @MockitoBean ZuzycieRepository zuzycie;

    private static final OffsetDateTime CZAS = OffsetDateTime.parse("2026-11-18T12:00:00Z");

    @Test
    void ostatniaPrognozaMaPolaIPozycjeZKontraktu() throws Exception {
        when(prognozy.ostatnia()).thenReturn(Optional.of(new PrognozaDto(5, CZAS, "ML", 12.5, "ZATWIERDZONA",
                List.of(new PrognozaPozycjaDto("DZIELNICA", 1, "WODA", 5400.0),
                        new PrognozaPozycjaDto("MIASTO", null, "WODA", 9000.0)))));

        mvc.perform(get("/api/prognozy/ostatnia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.zrodlo").value("ML"))
                .andExpect(jsonPath("$.roznicaProc").value(12.5))
                .andExpect(jsonPath("$.status").value("ZATWIERDZONA"))
                .andExpect(jsonPath("$.pozycje[0].poziom").value("DZIELNICA"))
                .andExpect(jsonPath("$.pozycje[0].obiektId").value(1))
                .andExpect(jsonPath("$.pozycje[0].ilosc").value(5400.0))
                .andExpect(jsonPath("$.pozycje[1].obiektId").doesNotExist());
    }

    @Test
    void brakPrognozToBlad404ZOpisem() throws Exception {
        when(prognozy.ostatnia()).thenReturn(Optional.empty());

        mvc.perform(get("/api/prognozy/ostatnia"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.blad").exists());
    }

    @Test
    void historiaPrognozToListaBezPozycji() throws Exception {
        when(prognozy.wszystkie()).thenReturn(List.of(new PrognozaDto(5, CZAS, "ML", null, "NOWA", null)));

        mvc.perform(get("/api/prognozy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].status").value("NOWA"))
                .andExpect(jsonPath("$[0].pozycje").doesNotExist());
    }

    @Test
    void historiaZuzyciaPrzekazujeFiltryIZwracaDataIlosc() throws Exception {
        when(zuzycie.historia(1, "WODA")).thenReturn(List.of(new ZuzycieDto(LocalDate.of(2026, 3, 1), 5100.0)));

        mvc.perform(get("/api/zuzycie/historia").param("dzielnica", "1").param("zasob", "WODA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].data").value("2026-03-01"))
                .andExpect(jsonPath("$[0].ilosc").value(5100.0));
    }

    @Test
    void historiaZuzyciaBezDzielnicyToCaleMiasto() throws Exception {
        when(zuzycie.historia(null, "LEKI")).thenReturn(List.of(new ZuzycieDto(LocalDate.of(2026, 3, 1), 7.0)));

        mvc.perform(get("/api/zuzycie/historia").param("zasob", "LEKI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ilosc").value(7.0));
    }

    @Test
    void brakLubZlyZasobToBlad400() throws Exception {
        mvc.perform(get("/api/zuzycie/historia"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.blad").exists());
        mvc.perform(get("/api/zuzycie/historia").param("zasob", "ZLOTO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.blad").exists());
    }
}
