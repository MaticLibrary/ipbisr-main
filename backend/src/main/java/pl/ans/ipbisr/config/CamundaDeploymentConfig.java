package pl.ans.ipbisr.config;

import io.camunda.client.annotation.Deployment;
import org.springframework.context.annotation.Configuration;

/**
 * Przy starcie backend sam wdraża do Camundy modele BPMN, formularze i tabele DMN.
 * Pliki pochodzą z katalogów bpmn/wykonywalne/ i dmn/ (Maven kopiuje je na classpath).
 * Modele z bpmn/modele-etap2/ to tylko dokumentacja i nie są wdrażane.
 */
@Configuration
@Deployment(resources = {
        "classpath*:/bpmn/**/*.bpmn",
        "classpath*:/bpmn/**/*.form",
        "classpath*:/dmn/**/*.dmn"})
public class CamundaDeploymentConfig {

    /** Te same wzorce w postaci stałej – sprawdza je test. */
    public static final String[] ZASOBY = {
            "classpath*:/bpmn/**/*.bpmn",
            "classpath*:/bpmn/**/*.form",
            "classpath*:/dmn/**/*.dmn"};
}
