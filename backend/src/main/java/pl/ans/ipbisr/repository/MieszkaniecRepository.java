package pl.ans.ipbisr.repository;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import pl.ans.ipbisr.api.dto.MieszkaniecDto;
import pl.ans.ipbisr.domain.GrupaPotrzeb;
import pl.ans.ipbisr.domain.WynikPriorytetu;

/** Rejestr potrzeb (tabela `mieszkaniec`, wynik P1) oraz widok mieszkańców dla API. */
@Repository
public class MieszkaniecRepository {

    private final JdbcClient jdbc;

    public MieszkaniecRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Zapisuje lub nadpisuje wyniki analizy (po jednym wierszu na PESEL). */
    public void zapisz(List<WynikPriorytetu> wyniki) {
        for (WynikPriorytetu w : wyniki) {
            jdbc.sql("""
                    INSERT INTO mieszkaniec (pesel, budynek_id, priorytet, poziom_potrzeb, dostawa_bezposrednia, status)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (pesel) DO UPDATE SET budynek_id = EXCLUDED.budynek_id,
                        priorytet = EXCLUDED.priorytet, poziom_potrzeb = EXCLUDED.poziom_potrzeb,
                        dostawa_bezposrednia = EXCLUDED.dostawa_bezposrednia, status = EXCLUDED.status,
                        data_analizy = now()
                    """)
                    .params(w.pesel(), w.budynekId(), w.priorytet(), w.poziomPotrzeb(), w.dostawaBezposrednia(),
                            w.status())
                    .update();
        }
    }

    /**
     * Grupy mieszkańców o takich samych potrzebach (budynek × priorytet × poziom potrzeb × grupa wiekowa) – wejście
     * kroku P3 „Pobierz stany magazynowe, potrzeby i prognozy”. Pomijamy osoby, które nie żyją albo są wykluczone.
     * Grupa wiekowa: poniżej 18 lat DZIECKO, 18–64 DOROSLY, od 65 lat SENIOR (tabela DMN „Normy dzienne” zna te trzy nazwy).
     */
    public List<GrupaPotrzeb> grupyPotrzeb() {
        return jdbc.sql("""
                SELECT m.budynek_id, m.priorytet, m.poziom_potrzeb,
                       CASE WHEN date_part('year', age(e.data_ur)) < 18 THEN 'DZIECKO'
                            WHEN date_part('year', age(e.data_ur)) < 65 THEN 'DOROSLY'
                            ELSE 'SENIOR' END AS grupa_wiekowa,
                       count(*) AS liczba
                FROM mieszkaniec m JOIN ewidencja_ludnosci e ON e.pesel = m.pesel
                WHERE m.status = 'ZYJE' AND m.priorytet <> 'WYKLUCZONY'
                GROUP BY m.budynek_id, m.priorytet, m.poziom_potrzeb, grupa_wiekowa
                ORDER BY m.budynek_id, m.priorytet, m.poziom_potrzeb, grupa_wiekowa
                """)
                .query((rs, i) -> new GrupaPotrzeb(rs.getInt("budynek_id"), rs.getString("priorytet"),
                        rs.getString("poziom_potrzeb"), rs.getString("grupa_wiekowa"), rs.getInt("liczba")))
                .list();
    }

    /** Lista mieszkańców dla API; priorytet i potrzeby są puste, dopóki P1 nie przeliczył danej osoby. */
    public List<MieszkaniecDto> szukaj(String priorytet, String status, Integer budynekId) {
        return jdbc.sql("""
                SELECT e.pesel, e.imie, e.nazwisko,
                       date_part('year', age(e.data_ur))::int AS wiek, e.budynek_id,
                       m.priorytet, m.poziom_potrzeb, COALESCE(m.dostawa_bezposrednia, FALSE) AS dostawa_bezposrednia,
                       e.status
                FROM ewidencja_ludnosci e LEFT JOIN mieszkaniec m ON m.pesel = e.pesel
                WHERE (CAST(:priorytet AS TEXT) IS NULL OR m.priorytet = :priorytet)
                  AND (CAST(:status AS TEXT) IS NULL OR e.status = :status)
                  AND (CAST(:budynek AS INTEGER) IS NULL OR e.budynek_id = :budynek)
                ORDER BY e.nazwisko, e.imie, e.pesel
                """)
                .param("priorytet", priorytet).param("status", status).param("budynek", budynekId)
                .query((rs, i) -> new MieszkaniecDto(rs.getString("pesel").trim(), rs.getString("imie"),
                        rs.getString("nazwisko"), rs.getInt("wiek"), rs.getInt("budynek_id"),
                        rs.getString("priorytet"), rs.getString("poziom_potrzeb"),
                        rs.getBoolean("dostawa_bezposrednia"), rs.getString("status")))
                .list();
    }
}
