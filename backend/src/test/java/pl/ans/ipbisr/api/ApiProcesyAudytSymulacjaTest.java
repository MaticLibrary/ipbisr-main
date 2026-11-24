package pl.ans.ipbisr.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.ans.ipbisr.api.dto.AudytDto;
import pl.ans.ipbisr.api.dto.ProcesDto;
import pl.ans.ipbisr.config.WebConfig;
import pl.ans.ipbisr.service.AudytService;
import pl.ans.ipbisr.service.KomunikatyService;
import pl.ans.ipbisr.service.ProcesyService;
import pl.ans.ipbisr.service.SymulacjaService;

/** Kontrakt REST ekranów „Procesy” (lista instancji + symulacja P5) i „Audyt” (dziennik decyzji). */
@WebMvcTest({ProcesyController.class, AudytController.class, SymulacjaController.class})
@Import({WebConfig.class, BledyApiHandler.class, SymulacjaService.class})
class ApiProcesyAudytSymulacjaTest {

    @Autowired MockMvc mvc;
    @MockitoBean ProcesyService procesy;
    @MockitoBean AudytService audyt;
    @MockitoBean KomunikatyService komunikaty;

    // ----- GET /api/procesy -----

    @Test
    void procesyZwracajaInstancjeZLinkiemDoOperate() throws Exception {
        when(procesy.ostatnieInstancje()).thenReturn(List.of(new ProcesDto("2251799813685249",
                "Proces_Analiza_sytuacji", "ACTIVE", "2026-11-22T10:00:00Z",
                "http://localhost:8080/operate/processes/2251799813685249")));

        mvc.perform(get("/api/procesy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].instancjaId").value("2251799813685249"))
                .andExpect(jsonPath("$[0].proces").value("Proces_Analiza_sytuacji"))
                .andExpect(jsonPath("$[0].stan").value("ACTIVE"))
                .andExpect(jsonPath("$[0].operateUrl").value("http://localhost:8080/operate/processes/2251799813685249"));
    }

    // ----- GET /api/audyt -----

    @Test
    void audytDomyslnieZwraca100WpisowZSzczegolamiJakoObiekt() throws Exception {
        when(audyt.ostatnie(100)).thenReturn(List.of(new AudytDto(OffsetDateTime.parse("2026-11-22T10:00:00Z"),
                "zarzad", "P1b", "ZWERYFIKOWANO", Map.of("decyzja", "POTWIERDZONE"))));

        mvc.perform(get("/api/audyt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].aktor").value("zarzad"))
                .andExpect(jsonPath("$[0].proces").value("P1b"))
                .andExpect(jsonPath("$[0].akcja").value("ZWERYFIKOWANO"))
                .andExpect(jsonPath("$[0].szczegoly.decyzja").value("POTWIERDZONE"))
                .andExpect(jsonPath("$[0].czas").exists());
    }

    @Test
    void audytPrzekazujeLimitZZapytania() throws Exception {
        mvc.perform(get("/api/audyt?limit=5")).andExpect(status().isOk());

        verify(audyt).ostatnie(5);
    }

    @Test
    void audytZLimitemSpozaZakresuToBlad400() throws Exception {
        when(audyt.ostatnie(0)).thenThrow(new IllegalArgumentException("Parametr limit musi być liczbą od 1 do 1000"));

        mvc.perform(get("/api/audyt?limit=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.blad").value("Parametr limit musi być liczbą od 1 do 1000"));
    }

    @Test
    void audytZLimitemNiebedacymLiczbaToBlad400() throws Exception {
        mvc.perform(get("/api/audyt?limit=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.blad").exists());
    }

    // ----- POST /api/symulacja/... -----

    @Test
    void zmianaSytuacjiZwraca202IdZdarzeniaIPublikujeWiadomosc() throws Exception {
        mvc.perform(post("/api/symulacja/zmiana-sytuacji").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poziomZagrozenia\":2}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.idZdarzenia").isNotEmpty());

        verify(komunikaty).publikuj(eq("Zmiana sytuacji kryzysowej"), any(), any());
        verify(audyt).zapisz(eq("analityk"), eq("P5"), eq("SYMULACJA_ZMIANY_SYTUACJI"), any());
    }

    @Test
    void ponowneplanowanieDzialaBezCialaZadania() throws Exception {
        mvc.perform(post("/api/symulacja/ponowne-planowanie"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.idZdarzenia").isNotEmpty());

        verify(komunikaty).publikuj(eq("Ponowne planowanie"), any(), any());
    }

    @Test
    void poziomZagrozeniaSpozaZakresuToBlad400BezWiadomosci() throws Exception {
        mvc.perform(post("/api/symulacja/zmiana-sytuacji").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"poziomZagrozenia\":7}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.blad").exists());

        verify(komunikaty, never()).publikuj(any(), any(), any());
    }
}
