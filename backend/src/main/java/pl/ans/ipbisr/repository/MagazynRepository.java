package pl.ans.ipbisr.repository;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.ans.ipbisr.api.dto.MagazynDto;

/** Stany magazynowe (tabela `stan_magazynowy`) – wejście algorytmu przydziału i ekran „Magazyn”. */
@Repository
public class MagazynRepository {

    private final JdbcClient jdbc;

    public MagazynRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<MagazynDto> wszystkie() {
        return jdbc.sql("SELECT zasob, ilosc, jednostka FROM stan_magazynowy ORDER BY zasob")
                .query((rs, i) -> new MagazynDto(rs.getString("zasob"), rs.getDouble("ilosc"), rs.getString("jednostka")))
                .list();
    }

    /** Nadpisuje stan podanych zasobów (nowy zasób zostaje dodany); pozostałe zasoby się nie zmieniają. */
    public void zapisz(List<MagazynDto> stany) {
        for (MagazynDto s : stany) {
            jdbc.sql("""
                    INSERT INTO stan_magazynowy (zasob, ilosc, jednostka) VALUES (?, ?, ?)
                    ON CONFLICT (zasob) DO UPDATE SET ilosc = EXCLUDED.ilosc, jednostka = EXCLUDED.jednostka
                    """)
                    .params(s.zasob(), s.ilosc(), s.jednostka()).update();
        }
    }
}
