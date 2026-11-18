package pl.ans.ipbisr.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

// Sprawdza, że Maven skopiował modele P1 na classpath (stamtąd backend wdraża je do Camundy)
class ProcessResourcesTest {

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Test
    void modeleP1SaNaClasspath() throws Exception {
        Resource[] modele = resolver.getResources("classpath*:/bpmn/*.bpmn");

        assertThat(modele).extracting(Resource::getFilename).contains(
                "P1a_Rejestr_ludnosci.bpmn",
                "P1b_Analiza_sytuacji_mieszkancow.bpmn",
                "P1c_Rejestracja_zgloszenia.bpmn");
    }

    @Test
    void modeleEtapu2NieSaWdrazane() throws Exception {
        // modele-etap2/ to tylko dokumentacja – nie wolno ich wdrażać do Camundy
        Resource[] pliki = resolver.getResources("classpath*:/bpmn/**/P?_*.bpmn");

        assertThat(pliki).extracting(Resource::getFilename)
                .noneMatch(nazwa -> nazwa.matches("P[1-5]_.*"));
    }

    @Test
    void konfiguracjaWdrozeniaWskazujeBpmnDmnIFormularze() {
        assertThat(CamundaDeploymentConfig.ZASOBY).containsExactlyInAnyOrder(
                "classpath*:/bpmn/**/*.bpmn", "classpath*:/bpmn/**/*.form", "classpath*:/dmn/**/*.dmn");
    }
}
