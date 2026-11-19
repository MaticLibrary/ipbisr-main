package pl.ans.ipbisr.worker.p1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.camunda.client.api.response.ActivatedJob;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.ans.ipbisr.domain.Osoba;
import pl.ans.ipbisr.repository.EwidencjaRepository;
import pl.ans.ipbisr.service.AnalizaService;
import pl.ans.ipbisr.service.KomunikatyService;
import pl.ans.ipbisr.service.ZgloszenieService;

/** Workery P1: czy biorą właściwe zmienne z joba, wywołują logikę i zwracają/wysyłają to, czego oczekuje proces. */
@ExtendWith(MockitoExtension.class)
class WorkeryP1Test {

    @Mock ActivatedJob job;
    @Mock EwidencjaRepository ewidencja;
    @Mock KomunikatyService komunikaty;
    @Mock AnalizaService analiza;
    @Mock ZgloszenieService zgloszenia;

    // ----- P1a -----

    @Test
    void p1aOdczytujeMieszkancowZEwidencji() {
        Osoba o = new Osoba("11111111111", 1, "ZYJE", 40, "DOBRY", false, "BRAK", false, false, 0);
        when(ewidencja.wszyscy()).thenReturn(List.of(o));

        Map<String, Object> wynik = new RejestrWorkers(ewidencja, komunikaty).odczytajDaneMieszkancow();

        assertThat(wynik).containsOnlyKeys("mieszkancy");
        assertThat(wynik.get("mieszkancy")).isEqualTo(List.of(o.doZmiennych()));
    }

    @Test
    void p1aOdsylaDaneWiadomosciZTymSamymIdZapytania() {
        List<Map<String, Object>> lista = List.of(Map.of("pesel", "1"));
        when(job.getVariablesAsMap()).thenReturn(Map.of("idZapytania", "abc", "mieszkancy", lista));

        new RejestrWorkers(ewidencja, komunikaty).wyslijDaneMieszkancow(job);

        verify(komunikaty).publikuj("Dane mieszkańców", "abc", Map.of("idZapytania", "abc", "mieszkancy", lista));
    }

    // ----- P1b -----

    @Test
    void p1bWysylaZapytanieIZwracaIdZapytaniaOrazZgloszenie() {
        Map<String, Object> zgloszenie = Map.of("id", 12, "rodzaj", "SMIERC");
        when(job.getVariablesAsMap()).thenReturn(Map.of("zgloszenie", zgloszenie));

        Map<String, Object> wynik = new AnalizaWorkers(analiza, komunikaty).wyslijZapytanieODane(job);

        ArgumentCaptor<String> klucz = ArgumentCaptor.forClass(String.class);
        verify(komunikaty).publikuj(eq("Zapytanie o dane"), klucz.capture(), any());
        assertThat(wynik).containsEntry("idZapytania", klucz.getValue()).containsEntry("zgloszenie", zgloszenie);
    }

    @Test
    void p1bPrzyStarcieOdTimeraUstawiaZgloszenieNaNull() {
        when(job.getVariablesAsMap()).thenReturn(new HashMap<>());

        Map<String, Object> wynik = new AnalizaWorkers(analiza, komunikaty).wyslijZapytanieODane(job);

        // bramka „Są zgłoszenia do weryfikacji?” czyta tę zmienną – musi istnieć, nawet pusta
        assertThat(wynik).containsKey("zgloszenie");
        assertThat(wynik.get("zgloszenie")).isNull();
    }

    @Test
    void p1bWyznaczaPriorytetyPrzezSerwis() {
        List<Map<String, Object>> mieszkancy = List.of(Map.of("pesel", "1"));
        List<Map<String, Object>> wyniki = List.of(Map.of("pesel", "1", "priorytet", "C"));
        when(job.getVariablesAsMap()).thenReturn(Map.of("mieszkancy", mieszkancy));
        when(analiza.wyznacz(mieszkancy)).thenReturn(wyniki);

        assertThat(new AnalizaWorkers(analiza, komunikaty).wyznaczPotrzebyIPriorytet(job))
                .containsEntry("wyniki", wyniki);
    }

    @Test
    void p1bZapisujeWynikiZDecyzjaZarzadu() {
        List<Map<String, Object>> wyniki = List.of();
        Map<String, Object> zgloszenie = Map.of("id", 12);
        when(job.getVariablesAsMap()).thenReturn(Map.of("wyniki", wyniki, "zgloszenie", zgloszenie,
                "decyzja", "POTWIERDZONE", "komentarz", "ok"));

        new AnalizaWorkers(analiza, komunikaty).zapiszWyniki(job);

        verify(analiza).zapiszWyniki(wyniki, zgloszenie, "POTWIERDZONE", "ok");
    }

    @Test
    void p1bZapisujeWynikiBezDecyzjiGdyNieByloWeryfikacji() {
        List<Map<String, Object>> wyniki = List.of();
        Map<String, Object> zmienne = new HashMap<>();
        zmienne.put("wyniki", wyniki);
        zmienne.put("zgloszenie", null);
        when(job.getVariablesAsMap()).thenReturn(zmienne);

        new AnalizaWorkers(analiza, komunikaty).zapiszWyniki(job);

        verify(analiza).zapiszWyniki(wyniki, null, null, null);
    }

    // ----- P1c -----

    @Test
    void p1cPrzekazujeZgloszenieOPodanymId() {
        when(job.getVariablesAsMap()).thenReturn(Map.of("idZgloszenia", 12));

        new ZgloszenieWorkers(zgloszenia).przekazZgloszenie(job);

        verify(zgloszenia).przekaz(12L);
    }
}
