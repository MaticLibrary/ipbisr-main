package pl.ans.ipbisr.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.ans.ipbisr.service.CsvDataImporter;

// Testy importu CSV z katalogu data/ (małe pliki testowe w katalogu tymczasowym)
@Import(CsvDataImporter.class)
class CsvDataImporterTest extends PostgresJdbcTest {

    @Autowired
    private CsvDataImporter importer;

    @Autowired
    private JdbcTemplate jdbc;

    @TempDir
    Path dataDir;

    @BeforeEach
    void czyscBaze() {
        jdbc.execute("TRUNCATE dzielnica, stan_magazynowy RESTART IDENTITY CASCADE");
    }

    private void plik(String nazwa, String tresc) throws IOException {
        Files.writeString(dataDir.resolve(nazwa), tresc);
    }

    @Test
    void importujeDzielniceIPunktyWydawania() throws IOException {
        plik("dzielnice.csv", "id,nazwa\n1,Centrum\n2,Osiedle Północne\n");
        plik("punkty_wydawania.csv", "id,nazwa,dzielnica_id,przepustowosc_kg\n1,Punkt A,1,750.5\n");

        importer.importuj(dataDir);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM dzielnica", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT nazwa FROM dzielnica WHERE id = 2", String.class))
                .isEqualTo("Osiedle Północne");
        assertThat(jdbc.queryForObject("SELECT przepustowosc_kg FROM punkt_wydawania WHERE id = 1", Double.class))
                .isEqualTo(750.5);
    }

    @Test
    void importujeMieszkancowZTypamiLiczbowymiLogicznymiIDatami() throws IOException {
        plik("dzielnice.csv", "id,nazwa\n1,Centrum\n");
        plik("punkty_wydawania.csv", "id,nazwa,dzielnica_id,przepustowosc_kg\n1,Punkt A,1,500\n");
        plik("budynki.csv", "id,adres,dzielnica_id,punkt_id\n1,Rynek 1,1,1\n");
        plik("ewidencja_ludnosci.csv",
                "pesel,imie,nazwisko,plec,data_ur,zawod,zawod_krytyczny,budynek_id,gospodarstwo_id,stan_zdrowia,"
                        + "choroby_przewlekle,niepelnosprawnosc,utrata_mieszkania,osoby_na_utrzymaniu,status\n"
                        + "50010112345,Anna,Nowak,K,1950-01-01,emeryt,false,1,10,CHOROBA,true,BRAK,false,0,ZYJE\n");

        importer.importuj(dataDir);

        assertThat(jdbc.queryForObject(
                "SELECT choroby_przewlekle FROM ewidencja_ludnosci WHERE pesel = '50010112345'", Boolean.class))
                .isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT data_ur::text FROM ewidencja_ludnosci WHERE pesel = '50010112345'", String.class))
                .isEqualTo("1950-01-01");
    }

    @Test
    void pomijaBrakujacePlikiBezBledu() {
        // Generator danych (Robert) może jeszcze nie istnieć – import nie ma się wtedy wywalać
        importer.importuj(dataDir);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM dzielnica", Integer.class)).isZero();
    }

    @Test
    void drugiImportNieDublujeDanych() throws IOException {
        plik("dzielnice.csv", "id,nazwa\n1,Centrum\n");

        importer.importuj(dataDir);
        importer.importuj(dataDir);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM dzielnica", Integer.class)).isEqualTo(1);
    }

    @Test
    void importujeStanyMagazynowe() throws IOException {
        plik("stany_magazynowe.csv", "zasob,ilosc,jednostka\nWODA,12000,l\nZYWNOSC,3500.5,kg\n");

        importer.importuj(dataDir);

        assertThat(jdbc.queryForObject("SELECT ilosc FROM stan_magazynowy WHERE zasob = 'ZYWNOSC'", Double.class))
                .isEqualTo(3500.5);
    }
}
