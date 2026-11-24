package pl.ans.ipbisr.dmn;

import static org.assertj.core.api.Assertions.assertThat;

import io.camunda.client.CamundaClient;
import io.camunda.process.test.api.CamundaProcessTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import pl.ans.ipbisr.domain.IloscZasobu;
import pl.ans.ipbisr.service.NormyService;

/**
 * Tabela DMN „Normy dzienne” z dmn/Priorytet_mieszkancow.dmn na prawdziwej Camundzie (kontener Dockera),
 * wywoływana przez ten sam NormyService, którego używa krok P3a „Pobierz stany magazynowe, potrzeby i prognozy”.
 */
@CamundaProcessTest
@Tag("camunda")
class NormyDmnTest {

    private CamundaClient client;

    @BeforeEach
    void wdrozDmn() throws Exception {
        client.newDeployResourceCommand().addResourceFromClasspath("dmn/Priorytet_mieszkancow.dmn").send().join();
    }

    @Test
    void doroslyWPodstawowejPotrzebieDostajeWodeZywnoscIHigiene() {
        // przykład z planu etapu III: woda 3 l, żywność 0,6 kg, higiena 0,1 zestawu; leków i schronienia brak
        assertThat(new NormyService(client).normy("PODSTAWOWY", "DOROSLY")).containsExactly(
                new IloscZasobu("ZYWNOSC", 0.6), new IloscZasobu("WODA", 3.0), new IloscZasobu("HIGIENA", 0.1));
    }

    @Test
    void seniorWKrytycznejPotrzebieDostajeWszystkieZasobyWlacznieZSchronieniem() {
        assertThat(new NormyService(client).normy("KRYTYCZNY", "SENIOR")).containsExactly(
                new IloscZasobu("ZYWNOSC", 0.7), new IloscZasobu("WODA", 5.0), new IloscZasobu("LEKI", 0.3),
                new IloscZasobu("HIGIENA", 0.2), new IloscZasobu("SCHRONIENIE", 1.0));
    }

    @Test
    void dzieckoMaMniejszaNormeNizDoroslyPrzyTymSamymPoziomiePotrzeb() {
        var dziecko = new NormyService(client).normy("PODWYZSZONY", "DZIECKO");
        var doroslyNorma = new NormyService(client).normy("PODWYZSZONY", "DOROSLY");

        assertThat(dziecko).contains(new IloscZasobu("WODA", 3.0));
        assertThat(doroslyNorma).contains(new IloscZasobu("WODA", 4.0));
    }

    @Test
    void kazdaKombinacjaPoziomuIGrupyWiekowejMaNorme() {
        // hit policy UNIQUE: dla każdej z 9 kombinacji musi pasować dokładnie jedna reguła (inaczej Camunda zgłasza błąd)
        for (String poziom : List.of("PODSTAWOWY", "PODWYZSZONY", "KRYTYCZNY")) {
            for (String grupa : List.of("DZIECKO", "DOROSLY", "SENIOR")) {
                assertThat(new NormyService(client).normy(poziom, grupa))
                        .as("%s / %s", poziom, grupa).isNotEmpty();
            }
        }
    }
}
