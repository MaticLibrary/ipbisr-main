package pl.ans.ipbisr.api;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.ans.ipbisr.api.dto.MagazynDto;
import pl.ans.ipbisr.config.WebConfig;
import pl.ans.ipbisr.repository.MagazynRepository;
import pl.ans.ipbisr.service.AudytService;
import pl.ans.ipbisr.service.MagazynService;

/** Kontrakt REST dla ekranu „Magazyn”: odczyt stanów i ich zmiana na demo (PUT) wraz z walidacją i wpisem do audytu. */
@WebMvcTest(MagazynController.class)
@Import({WebConfig.class, BledyApiHandler.class, MagazynService.class})
class ApiMagazynTest {

    @Autowired MockMvc mvc;
    @MockitoBean MagazynRepository magazyn;
    @MockitoBean AudytService audyt;

    private static final String WODA_50 = "[{\"zasob\":\"WODA\",\"ilosc\":50.5,\"jednostka\":\"l\"}]";

    @Test
    void getZwracaStanyZJednostkami() throws Exception {
        when(magazyn.wszystkie()).thenReturn(List.of(new MagazynDto("WODA", 12000.0, "l")));

        mvc.perform(get("/api/magazyn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].zasob").value("WODA"))
                .andExpect(jsonPath("$[0].ilosc").value(12000.0))
                .andExpect(jsonPath("$[0].jednostka").value("l"));
    }

    @Test
    void putZapisujeStanyIZwracaZapisanaListeOrazWpisAudytu() throws Exception {
        when(magazyn.wszystkie()).thenReturn(List.of(new MagazynDto("WODA", 50.5, "l")));

        mvc.perform(put("/api/magazyn").contentType(MediaType.APPLICATION_JSON).content(WODA_50))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].zasob").value("WODA"))
                .andExpect(jsonPath("$[0].ilosc").value(50.5));

        verify(magazyn).zapisz(List.of(new MagazynDto("WODA", 50.5, "l")));
        verify(audyt).zapisz(eq("analityk"), eq("P3"), eq("MAGAZYN_ZMIENIONY"), anyMap());
    }

    @Test
    void nieznanyZasobToBlad400ZOpisemIBezZapisu() throws Exception {
        mvc.perform(put("/api/magazyn").contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"zasob\":\"ZLOTO\",\"ilosc\":1,\"jednostka\":\"kg\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.blad").exists());

        verify(magazyn, never()).zapisz(anyList());
    }

    @Test
    void ujemnaIloscToBlad400() throws Exception {
        mvc.perform(put("/api/magazyn").contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"zasob\":\"WODA\",\"ilosc\":-5,\"jednostka\":\"l\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.blad").exists());

        verify(magazyn, never()).zapisz(anyList());
    }

    @Test
    void brakJednostkiToBlad400() throws Exception {
        mvc.perform(put("/api/magazyn").contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"zasob\":\"WODA\",\"ilosc\":5}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.blad").exists());
    }

    @Test
    void pustaListaToBlad400() throws Exception {
        mvc.perform(put("/api/magazyn").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.blad").exists());
    }
}
