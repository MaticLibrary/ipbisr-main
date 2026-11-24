package pl.ans.ipbisr.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.ans.ipbisr.api.dto.BudynekDto;
import pl.ans.ipbisr.api.dto.MieszkaniecDto;
import pl.ans.ipbisr.config.WebConfig;
import pl.ans.ipbisr.domain.Zgloszenie;
import pl.ans.ipbisr.repository.BudynekRepository;
import pl.ans.ipbisr.repository.MieszkaniecRepository;
import pl.ans.ipbisr.repository.ZgloszenieRepository;
import pl.ans.ipbisr.service.ZgloszenieService;

/** Kontrakt REST dla P1 (sekcja 8 planu): ścieżki, format JSON i błędy. */
@WebMvcTest({ZgloszeniaController.class, BudynkiController.class})
@org.springframework.context.annotation.Import({WebConfig.class, BledyApiHandler.class})
class ApiP1Test {

    @Autowired MockMvc mvc;
    @MockitoBean ZgloszenieService zgloszenieService;
    @MockitoBean ZgloszenieRepository zgloszeniaRepo;
    @MockitoBean BudynekRepository budynki;
    @MockitoBean MieszkaniecRepository mieszkancy;

    private static Zgloszenie zgloszenie() {
        return new Zgloszenie(12, "50010112345", null, "SMIERC", "opis", false, null, "NOWE",
                OffsetDateTime.parse("2026-11-18T12:00:00Z"));
    }

    @Test
    void postZgloszeniaZwraca201IdStatusUtworzono() throws Exception {
        when(zgloszenieService.zarejestruj(any())).thenReturn(zgloszenie());

        mvc.perform(post("/api/zgloszenia").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesel\":\"50010112345\",\"budynekId\":null,\"rodzaj\":\"SMIERC\",\"opis\":\"opis\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.status").value("NOWE"))
                .andExpect(jsonPath("$.utworzono").exists());
    }

    @Test
    void bladWalidacjiToBlad400ZOpisemPoPolsku() throws Exception {
        when(zgloszenieService.zarejestruj(any())).thenThrow(new IllegalArgumentException("Podaj numer PESEL albo ID budynku"));

        mvc.perform(post("/api/zgloszenia").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rodzaj\":\"SMIERC\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.blad").value("Podaj numer PESEL albo ID budynku"));
    }

    @Test
    void zlyJsonToBlad400() throws Exception {
        mvc.perform(post("/api/zgloszenia").contentType(MediaType.APPLICATION_JSON).content("{nie json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.blad").exists());
    }

    @Test
    void getZgloszeniaZwracaListeZPolamiZKontraktu() throws Exception {
        when(zgloszeniaRepo.wszystkie()).thenReturn(List.of(zgloszenie()));

        mvc.perform(get("/api/zgloszenia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[0].pesel").value("50010112345"))
                .andExpect(jsonPath("$[0].rodzaj").value("SMIERC"))
                .andExpect(jsonPath("$[0].losowaKontrola").value(false))
                .andExpect(jsonPath("$[0].status").value("NOWE"))
                .andExpect(jsonPath("$[0].utworzono").exists());
    }

    @Test
    void getBudynkiZFiltremDzielnicy() throws Exception {
        when(budynki.szukaj(1)).thenReturn(List.of(new BudynekDto(3, "Rynek 1", 1, "Centrum", "A", 24)));

        mvc.perform(get("/api/budynki").param("dzielnica", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adres").value("Rynek 1"))
                .andExpect(jsonPath("$[0].dzielnicaId").value(1))
                .andExpect(jsonPath("$[0].priorytet").value("A"))
                .andExpect(jsonPath("$[0].liczbaMieszkancow").value(24));
    }

    @Test
    void getBudynekZMieszkancami() throws Exception {
        when(budynki.znajdz(3)).thenReturn(Optional.of(new BudynekDto(3, "Rynek 1", 1, "Centrum", "A", 1)));
        when(mieszkancy.szukaj(null, null, 3)).thenReturn(List.of(
                new MieszkaniecDto("11111111111", "Anna", "Nowak", 76, 3, "A", "PODWYZSZONY", true, "ZYJE")));

        mvc.perform(get("/api/budynki/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.mieszkancy[0].imie").value("Anna"))
                .andExpect(jsonPath("$.mieszkancy[0].dostawaBezposrednia").value(true));
    }

    @Test
    void nieznanyBudynekToBlad404() throws Exception {
        when(budynki.znajdz(99)).thenReturn(Optional.empty());

        mvc.perform(get("/api/budynki/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.blad").exists());
    }

    @Test
    void getMieszkancyPrzekazujeFiltry() throws Exception {
        when(mieszkancy.szukaj("A", "ZYJE", null)).thenReturn(List.of(
                new MieszkaniecDto("11111111111", "Anna", "Nowak", 76, 3, "A", "PODWYZSZONY", true, "ZYJE")));

        mvc.perform(get("/api/mieszkancy").param("priorytet", "A").param("status", "ZYJE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pesel").value("11111111111"))
                .andExpect(jsonPath("$[0].poziomPotrzeb").value("PODWYZSZONY"));
    }

    @Test
    void corsPozwalaFrontendowiZLocalhost3000() throws Exception {
        mvc.perform(options("/api/zgloszenia")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }
}
