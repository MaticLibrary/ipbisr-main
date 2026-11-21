package pl.ans.ipbisr.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.ans.ipbisr.domain.PunktWydawania;

/** Punkty wydawania zasobów z budynkami, które do nich należą – wejście algorytmu przydziału (ograniczenie transportu). */
@Repository
public class PunktRepository {

    private final JdbcClient jdbc;

    public PunktRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<PunktWydawania> wszystkie() {
        Map<Integer, Double> przepustowosc = new LinkedHashMap<>();
        Map<Integer, List<Integer>> budynki = new LinkedHashMap<>();
        jdbc.sql("""
                SELECT p.id AS punkt_id, p.przepustowosc_kg, b.id AS budynek_id
                FROM punkt_wydawania p LEFT JOIN budynek b ON b.punkt_id = p.id
                ORDER BY p.id, b.id""")
                .query(rs -> {
                    int punkt = rs.getInt("punkt_id");
                    przepustowosc.put(punkt, rs.getDouble("przepustowosc_kg"));
                    List<Integer> lista = budynki.computeIfAbsent(punkt, k -> new ArrayList<>());
                    int budynek = rs.getInt("budynek_id");
                    if (!rs.wasNull()) { // punkt bez budynków ma pustą listę
                        lista.add(budynek);
                    }
                });
        List<PunktWydawania> wynik = new ArrayList<>();
        przepustowosc.forEach((id, kg) -> wynik.add(new PunktWydawania(id, kg, budynki.get(id))));
        return wynik;
    }
}
