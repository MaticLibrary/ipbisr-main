package pl.ans.ipbisr.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import pl.ans.ipbisr.domain.GrupaPotrzeb;
import pl.ans.ipbisr.domain.IloscZasobu;
import pl.ans.ipbisr.domain.Potrzeba;
import pl.ans.ipbisr.domain.PunktWydawania;
import pl.ans.ipbisr.repository.MagazynRepository;
import pl.ans.ipbisr.repository.MieszkaniecRepository;
import pl.ans.ipbisr.repository.PrognozaRepository;
import pl.ans.ipbisr.repository.PunktRepository;

/**
 * Krok P3a „Pobierz stany magazynowe, potrzeby i prognozy”: zbiera wszystko, czego potrzebuje algorytm przydziału.
 * Potrzeby budynku = liczba mieszkańców w grupie × dobowa norma z tabeli DMN „Normy dzienne”.
 */
@Service
public class PotrzebyService {

    /** Wejście algorytmu przydziału (to samo trafia do zmiennych procesu i do serwisu ML `/allocate`). */
    public record DanePrzydzialu(List<IloscZasobu> stany, List<Potrzeba> potrzeby, List<IloscZasobu> prognozaA,
                                 List<PunktWydawania> punkty) {}

    private record Klucz(int budynekId, String zasob, String priorytet) {}

    private final MieszkaniecRepository mieszkancy;
    private final MagazynRepository magazyn;
    private final PrognozaRepository prognozy;
    private final PunktRepository punkty;
    private final NormyService normy;

    public PotrzebyService(MieszkaniecRepository mieszkancy, MagazynRepository magazyn, PrognozaRepository prognozy,
                           PunktRepository punkty, NormyService normy) {
        this.mieszkancy = mieszkancy;
        this.magazyn = magazyn;
        this.prognozy = prognozy;
        this.punkty = punkty;
        this.normy = normy;
    }

    /**
     * @param idPrognozy prognoza z wiadomości „Prognoza gotowa”; null, gdy proces ruszył od „Ponownego planowania”
     *                   (wtedy bierzemy najnowszą prognozę z bazy, a gdy jej nie ma – planujemy bez rezerwy)
     */
    public DanePrzydzialu zbierz(Long idPrognozy) {
        List<GrupaPotrzeb> grupy = mieszkancy.grupyPotrzeb();
        if (grupy.isEmpty()) {
            throw new IllegalStateException(
                    "Rejestr potrzeb jest pusty – najpierw musi zadziałać analiza P1 (P1b), która przypisuje mieszkańcom priorytety");
        }
        List<Potrzeba> potrzeby = potrzeby(grupy);
        List<IloscZasobu> stany = magazyn.wszystkie().stream().map(s -> new IloscZasobu(s.zasob(), s.ilosc())).toList();
        return new DanePrzydzialu(stany, potrzeby, prognozaDlaGrupyA(prognozy.miasto(idPrognozy), potrzeby),
                punkty.wszystkie());
    }

    private List<Potrzeba> potrzeby(List<GrupaPotrzeb> grupy) {
        // tabelę DMN wywołujemy raz na kombinację (poziom potrzeb × grupa wiekowa), a nie dla każdego mieszkańca
        Map<String, List<IloscZasobu>> normyGrup = new HashMap<>();
        Map<Klucz, Double> suma = new LinkedHashMap<>();
        for (GrupaPotrzeb g : grupy) {
            List<IloscZasobu> norma = normyGrup.computeIfAbsent(g.poziomPotrzeb() + "/" + g.grupaWiekowa(),
                    k -> normy.normy(g.poziomPotrzeb(), g.grupaWiekowa()));
            for (IloscZasobu n : norma) {
                suma.merge(new Klucz(g.budynekId(), n.zasob(), g.priorytet()), g.liczba() * n.ilosc(), Double::sum);
            }
        }
        List<Potrzeba> wynik = new ArrayList<>();
        suma.forEach((k, ilosc) -> wynik.add(new Potrzeba(k.budynekId(), k.zasob(), k.priorytet(), zaokraglij(ilosc))));
        return wynik;
    }

    /**
     * Prognoza dla grupy A = prognoza miasta × udział grupy A w potrzebach danego zasobu. Algorytm zostawia z niej
     * rezerwę na następny cykl. Zasób, którego nikt nie potrzebuje, pomijamy.
     */
    private List<IloscZasobu> prognozaDlaGrupyA(List<IloscZasobu> prognozaMiasta, List<Potrzeba> potrzeby) {
        Map<String, Double> wszystkie = new HashMap<>();
        Map<String, Double> grupaA = new HashMap<>();
        for (Potrzeba p : potrzeby) {
            wszystkie.merge(p.zasob(), p.ilosc(), Double::sum);
            if ("A".equals(p.priorytet())) {
                grupaA.merge(p.zasob(), p.ilosc(), Double::sum);
            }
        }
        List<IloscZasobu> wynik = new ArrayList<>();
        for (IloscZasobu p : prognozaMiasta) {
            double razem = wszystkie.getOrDefault(p.zasob(), 0.0);
            if (razem > 0) {
                wynik.add(new IloscZasobu(p.zasob(), zaokraglij(p.ilosc() * grupaA.getOrDefault(p.zasob(), 0.0) / razem)));
            }
        }
        return wynik;
    }

    /** Do 2 miejsc po przecinku – żeby 2 × 0,15 nie dawało 0,30000000000000004 w planie i na ekranie. */
    private static double zaokraglij(double x) {
        return Math.round(x * 100) / 100.0;
    }
}
