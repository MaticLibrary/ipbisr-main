package pl.ans.ipbisr.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pl.ans.ipbisr.service.CsvDataImporter;

/** Przy starcie z profilem „dev” wczytuje dane syntetyczne z katalogu data/ do bazy. */
@Component
@Profile("dev")
public class DevDataLoader implements ApplicationRunner {

    private final CsvDataImporter importer;
    private final Path katalogDanych;

    public DevDataLoader(CsvDataImporter importer, @Value("${ipbisr.data-dir}") Path katalogDanych) {
        this.importer = importer;
        this.katalogDanych = katalogDanych;
    }

    @Override
    public void run(ApplicationArguments args) {
        importer.importuj(katalogDanych);
    }
}
