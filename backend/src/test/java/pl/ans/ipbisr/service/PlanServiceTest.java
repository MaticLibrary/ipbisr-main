package pl.ans.ipbisr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.ans.ipbisr.domain.ZlecenieWydania;
import pl.ans.ipbisr.repository.PlanRepository;

/** Krok P3a „Wyślij zlecenia wydania do magazynu” po decyzji analityka – bez bazy i bez Camundy (Mockito). */
@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock PlanRepository plany;
    @Mock KomunikatyService komunikaty;
    @Mock AudytService audyt;

    PlanService service;

    private static final List<ZlecenieWydania> ZLECENIA = List.of(new ZlecenieWydania(1, "WODA", 100.0));

    @BeforeEach
    void setUp() {
        service = new PlanService(plany, komunikaty, audyt);
    }

    @Test
    void zatwierdzonyPlanTworzyZleceniaIWysylaWiadomoscDlaMagazynu() {
        when(plany.zlecenia(2)).thenReturn(ZLECENIA);

        service.zakoncz(2, "ZATWIERDZONY", "ok");

        InOrder kolejnosc = inOrder(plany, komunikaty);
        kolejnosc.verify(plany).ustawStatus(2, "ZATWIERDZONY", "analityk");
        kolejnosc.verify(plany).utworzZlecenia(2);
        // korelacja = id planu; wiadomość niesie zlecenia dla P4 (którego jeszcze nie ma)
        kolejnosc.verify(komunikaty).publikuj("Zlecenie wydania", "2", Map.of("idPlanu", 2L, "zlecenia", ZLECENIA));
        verify(audyt).zapisz(eq("analityk"), eq("P3a"), eq("ZATWIERDZONO_PLAN"), anyMap());
    }

    @Test
    void odrzuconyPlanNieTworzyZlecenIZostajeWAudycie() {
        service.zakoncz(2, "ODRZUCONY", "za mało leków");

        verify(plany).ustawStatus(2, "ODRZUCONY", "analityk");
        verify(plany, never()).utworzZlecenia(2);
        verifyNoInteractions(komunikaty);
        ArgumentCaptor<Map<String, Object>> szczegoly = ArgumentCaptor.captor();
        verify(audyt).zapisz(eq("analityk"), eq("P3a"), eq("ODRZUCONO_PLAN"), szczegoly.capture());
        assertThat(szczegoly.getValue()).containsEntry("planId", 2L).containsEntry("komentarz", "za mało leków");
    }

    @Test
    void brakDecyzjiToZatwierdzenieAutomatyczneZapisaneJakoSystem() {
        when(plany.zlecenia(2)).thenReturn(ZLECENIA);

        // przy automatycznym zatwierdzeniu (po 2 h, M5) nikt nie wypełnił formularza, więc decyzji nie ma
        service.zakoncz(2, null, null);

        verify(plany).ustawStatus(2, "ZATWIERDZONY", "system");
        verify(plany).utworzZlecenia(2);
        verify(audyt).zapisz(eq("system"), eq("P3a"), eq("ZATWIERDZONO_AUTOMATYCZNIE"), anyMap());
        verify(komunikaty).publikuj(eq("Zlecenie wydania"), eq("2"), any());
    }
}
