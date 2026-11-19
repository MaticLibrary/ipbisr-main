package pl.ans.ipbisr.dmn;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.process.test.api.CamundaProcessTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Test tabel DMN z dmn/Priorytet_mieszkancow.dmn na prawdziwej Camundzie (kontener Dockera).
 * Każdy test to jeden mieszkaniec – sprawdzamy, że pierwsza pasująca reguła daje oczekiwany priorytet.
 */
@CamundaProcessTest
@Tag("camunda")
class PriorytetDmnTest {

    private CamundaClient client;

    @BeforeEach
    void wdrozDmn() throws Exception {
        client.newDeployResourceCommand().addResourceFromClasspath("dmn/Priorytet_mieszkancow.dmn").send().join();
    }

    /** Zdrowy dorosły bez żadnych szczególnych cech – baza do modyfikacji w testach. */
    private static Map<String, Object> zdrowyDorosly() {
        Map<String, Object> m = new HashMap<>();
        m.put("status", "ZYJE");
        m.put("wiek", 35);
        m.put("stanZdrowia", "DOBRY");
        m.put("chorobyPrzewlekle", false);
        m.put("niepelnosprawnosc", "BRAK");
        m.put("utrataMieszkania", false);
        m.put("zawodKrytyczny", false);
        m.put("osobyNaUtrzymaniu", 0);
        return m;
    }

    private Map<String, Object> priorytet(Map<String, Object> wejscie) throws Exception {
        var wynik = client.newEvaluateDecisionCommand().decisionId("Priorytet_mieszkancow")
                .variables(wejscie).send().join();
        return com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readValue(wynik.getDecisionOutput(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
    }

    @Test
    void zdrowyDoroslyToC() throws Exception {
        assertThat(priorytet(zdrowyDorosly())).containsEntry("priorytet", "C")
                .containsEntry("poziomPotrzeb", "PODSTAWOWY").containsEntry("dostawaBezposrednia", false);
    }

    @Test
    void osobaZmarlaJestWykluczona() throws Exception {
        var m = zdrowyDorosly();
        m.put("status", "NIE_ZYJE");
        assertThat(priorytet(m)).containsEntry("priorytet", "WYKLUCZONY");
    }

    @Test
    void zaginionyJestWykluczony() throws Exception {
        var m = zdrowyDorosly();
        m.put("status", "ZAGINIONY");
        assertThat(priorytet(m)).containsEntry("priorytet", "WYKLUCZONY");
    }

    @Test
    void ciezkiStanToAKrytyczny() throws Exception {
        var m = zdrowyDorosly();
        m.put("stanZdrowia", "CIEZKI");
        assertThat(priorytet(m)).containsEntry("priorytet", "A")
                .containsEntry("poziomPotrzeb", "KRYTYCZNY").containsEntry("dostawaBezposrednia", true);
    }

    @Test
    void znacznaNiepelnosprawnoscToAKrytyczny() throws Exception {
        var m = zdrowyDorosly();
        m.put("niepelnosprawnosc", "ZNACZNY");
        assertThat(priorytet(m)).containsEntry("priorytet", "A").containsEntry("poziomPotrzeb", "KRYTYCZNY");
    }

    @Test
    void osiemdziesieciolatekToAZDostawaBezposrednia() throws Exception {
        var m = zdrowyDorosly();
        m.put("wiek", 80);
        assertThat(priorytet(m)).containsEntry("priorytet", "A")
                .containsEntry("poziomPotrzeb", "PODWYZSZONY").containsEntry("dostawaBezposrednia", true);
    }

    @Test
    void malyDzieckoToABezDostawyBezposredniej() throws Exception {
        var m = zdrowyDorosly();
        m.put("wiek", 2);
        assertThat(priorytet(m)).containsEntry("priorytet", "A").containsEntry("dostawaBezposrednia", false);
    }

    @Test
    void utrataMieszkaniaToA() throws Exception {
        var m = zdrowyDorosly();
        m.put("utrataMieszkania", true);
        assertThat(priorytet(m)).containsEntry("priorytet", "A").containsEntry("poziomPotrzeb", "PODWYZSZONY");
    }

    @Test
    void chorobaToB() throws Exception {
        var m = zdrowyDorosly();
        m.put("stanZdrowia", "CHOROBA");
        assertThat(priorytet(m)).containsEntry("priorytet", "B").containsEntry("poziomPotrzeb", "PODWYZSZONY");
    }

    @Test
    void chorobyPrzewlekleToB() throws Exception {
        var m = zdrowyDorosly();
        m.put("chorobyPrzewlekle", true);
        assertThat(priorytet(m)).containsEntry("priorytet", "B");
    }

    @Test
    void umiarkowanaNiepelnosprawnoscToB() throws Exception {
        var m = zdrowyDorosly();
        m.put("niepelnosprawnosc", "UMIARKOWANY");
        assertThat(priorytet(m)).containsEntry("priorytet", "B");
    }

    @Test
    void zawodKrytycznyToBPodstawowy() throws Exception {
        var m = zdrowyDorosly();
        m.put("zawodKrytyczny", true);
        assertThat(priorytet(m)).containsEntry("priorytet", "B").containsEntry("poziomPotrzeb", "PODSTAWOWY");
    }

    @Test
    void dwieOsobyNaUtrzymaniuToB() throws Exception {
        var m = zdrowyDorosly();
        m.put("osobyNaUtrzymaniu", 2);
        assertThat(priorytet(m)).containsEntry("priorytet", "B").containsEntry("poziomPotrzeb", "PODSTAWOWY");
    }

    @Test
    void kolejnoscRegulCiezkiStanWygrywaZUtrataMieszkania() throws Exception {
        // hit policy FIRST: reguła 2 (ciężki stan) jest wyżej niż reguła 4 (utrata mieszkania)
        var m = zdrowyDorosly();
        m.put("stanZdrowia", "CIEZKI");
        m.put("utrataMieszkania", true);
        assertThat(priorytet(m)).containsEntry("poziomPotrzeb", "KRYTYCZNY");
    }

    @Test
    void normyDzienneDlaPodstawowegoDoroslego() throws Exception {
        var wynik = client.newEvaluateDecisionCommand().decisionId("Normy_dzienne")
                .variables(Map.of("poziomPotrzeb", "PODSTAWOWY", "grupaWiekowa", "DOROSLY")).send().join();
        assertThat(wynik.getDecisionOutput()).contains("\"wodaL\":3").contains("\"zywnoscKg\":0.6")
                .contains("\"higienaZestaw\":0.1");
    }
}
