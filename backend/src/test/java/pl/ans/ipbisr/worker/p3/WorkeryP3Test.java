package pl.ans.ipbisr.worker.p3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.camunda.client.api.response.ActivatedJob;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.ans.ipbisr.domain.IloscZasobu;
import pl.ans.ipbisr.domain.Potrzeba;
import pl.ans.ipbisr.domain.PunktWydawania;
import pl.ans.ipbisr.service.PlanService;
import pl.ans.ipbisr.service.PomocService;
import pl.ans.ipbisr.service.PotrzebyService;
import pl.ans.ipbisr.service.PotrzebyService.DanePrzydzialu;

/** Workery P3: czy biorą właściwe zmienne z joba, wywołują logikę i zwracają to, czego oczekuje proces. */
@ExtendWith(MockitoExtension.class)
class WorkeryP3Test {

    @Mock ActivatedJob job;
    @Mock PotrzebyService potrzeby;
    @Mock PomocService pomoc;
    @Mock PlanService plan;

    private final DanePrzydzialu dane = new DanePrzydzialu(
            List.of(new IloscZasobu("WODA", 1000.0)), List.of(new Potrzeba(1, "WODA", "A", 8.0)),
            List.of(new IloscZasobu("WODA", 800.0)), List.of(new PunktWydawania(1, 500.0, List.of(1))));

    // ----- P3a: pobierz-dane-do-przydzialu -----

    @Test
    void p3aPobieraDaneDlaPrognozyZWiadomosciIZwracaJeJakoZmienneProcesu() {
        when(job.getVariablesAsMap()).thenReturn(Map.of("idPrognozy", 5));
        when(potrzeby.zbierz(5L)).thenReturn(dane);

        Map<String, Object> wynik = new PrzydzialWorkers(potrzeby, pomoc, plan).pobierzDaneDoPrzydzialu(job);

        assertThat(wynik).containsEntry("stany", dane.stany()).containsEntry("potrzeby", dane.potrzeby())
                .containsEntry("prognozaA", dane.prognozaA()).containsEntry("punkty", dane.punkty())
                .containsEntry("idPrognozy", 5L);
    }

    @Test
    void p3aAkceptujeIdPrognozyJakoTekst() {
        when(job.getVariablesAsMap()).thenReturn(Map.of("idPrognozy", "7"));
        when(potrzeby.zbierz(7L)).thenReturn(dane);

        assertThat(new PrzydzialWorkers(potrzeby, pomoc, plan).pobierzDaneDoPrzydzialu(job)).containsEntry("idPrognozy", 7L);
    }

    @Test
    void p3aPrzyPonownymPlanowaniuNieMaIdPrognozyAleZmiennaIstnieje() {
        when(job.getVariablesAsMap()).thenReturn(new HashMap<>());
        when(potrzeby.zbierz(null)).thenReturn(dane);

        Map<String, Object> wynik = new PrzydzialWorkers(potrzeby, pomoc, plan).pobierzDaneDoPrzydzialu(job);

        assertThat(wynik).containsKey("idPrognozy");
        assertThat(wynik.get("idPrognozy")).isNull();
    }

    // ----- P3a: wyslij-wniosek-o-pomoc -----

    @Test
    void p3aWysylaWniosekONiedoboryIZwracaIdWnioskuDoKorelacji() {
        List<Map<String, Object>> niedobory = List.of(Map.of("zasob", "LEKI", "ilosc", 40.0));
        when(job.getVariablesAsMap()).thenReturn(Map.of("idPlanu", 2, "niedobory", niedobory));
        when(pomoc.wyslijWniosek(2L, List.of(new IloscZasobu("LEKI", 40.0)))).thenReturn("abc");

        Map<String, Object> wynik = new PrzydzialWorkers(potrzeby, pomoc, plan).wyslijWniosekOPomoc(job);

        // receive task „Odbierz odpowiedź na wniosek” czeka na wiadomość z kluczem `idWniosku`
        assertThat(wynik).containsOnly(Map.entry("idWniosku", "abc"));
    }

    // ----- P3a: wyslij-zlecenia-wydania -----

    @Test
    void p3aKonczyPlanZDecyzjaAnalityka() {
        when(job.getVariablesAsMap()).thenReturn(Map.of("idPlanu", 2, "decyzja", "ODRZUCONY", "komentarz", "za mało"));

        new PrzydzialWorkers(potrzeby, pomoc, plan).wyslijZleceniaWydania(job);

        verify(plan).zakoncz(2L, "ODRZUCONY", "za mało");
    }

    @Test
    void p3aBezDecyzjiPrzekazujeNulle() {
        when(job.getVariablesAsMap()).thenReturn(Map.of("idPlanu", 2));

        new PrzydzialWorkers(potrzeby, pomoc, plan).wyslijZleceniaWydania(job);

        verify(plan).zakoncz(2L, null, null);
    }

    // ----- P3b: wyslij-odpowiedz-na-wniosek -----

    @Test
    void p3bOdsylaOdpowiedzZIdWnioskuIDanymiZFormularza() {
        List<Map<String, Object>> przyznano = List.of(Map.of("zasob", "LEKI", "wnioskowano", 40, "przyznano", 30));
        when(job.getVariablesAsMap()).thenReturn(
                Map.of("idWniosku", "abc", "idPlanu", 2, "przyznanoIlosci", przyznano, "termin", "2026-12-01"));

        new OrganizacjeWorkers(pomoc).wyslijOdpowiedzNaWniosek(job);

        verify(pomoc).zapiszOdpowiedz("abc", 2L, przyznano, "2026-12-01");
    }
}
