package pl.ans.ipbisr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.ans.ipbisr.domain.Osoba;
import pl.ans.ipbisr.domain.WynikPriorytetu;
import pl.ans.ipbisr.domain.Zgloszenie;
import pl.ans.ipbisr.repository.EwidencjaRepository;
import pl.ans.ipbisr.repository.MieszkaniecRepository;
import pl.ans.ipbisr.repository.ZgloszenieRepository;

/** Logika kroków P1b „Wyznacz potrzeby i priorytet” oraz „Zapisz wyniki” – bez Camundy i bez bazy (Mockito). */
@ExtendWith(MockitoExtension.class)
class AnalizaServiceTest {

    @Mock EwidencjaRepository ewidencja;
    @Mock MieszkaniecRepository mieszkancy;
    @Mock ZgloszenieRepository zgloszenia;
    @Mock PriorytetService priorytety;
    @Mock AudytService audyt;

    AnalizaService service;

    @BeforeEach
    void setUp() {
        service = new AnalizaService(ewidencja, mieszkancy, zgloszenia, priorytety, audyt);
    }

    private static Osoba osoba(String pesel, String status) {
        return new Osoba(pesel, 1, status, 40, "DOBRY", false, "BRAK", false, false, 0);
    }

    private static WynikPriorytetu wynik(String pesel, String priorytet, String poziom) {
        return new WynikPriorytetu(pesel, 1, "ZYJE", priorytet, poziom, false);
    }

    private static Zgloszenie zgloszenie(String rodzaj, String pesel, Integer budynek) {
        return new Zgloszenie(7, pesel, budynek, rodzaj, null, false, null, "PRZEKAZANE", OffsetDateTime.now());
    }

    @Test
    void wyznaczWywolujeDmnDlaKazdegoMieszkanca() {
        Osoba a = osoba("11111111111", "ZYJE");
        Osoba b = osoba("22222222222", "NIE_ZYJE");
        when(priorytety.oblicz(a)).thenReturn(wynik("11111111111", "C", "PODSTAWOWY"));
        when(priorytety.oblicz(b)).thenReturn(wynik("22222222222", "WYKLUCZONY", "PODSTAWOWY"));

        List<Map<String, Object>> wyniki = service.wyznacz(List.of(a.doZmiennych(), b.doZmiennych()));

        assertThat(wyniki).hasSize(2);
        assertThat(wyniki.get(0)).containsEntry("pesel", "11111111111").containsEntry("priorytet", "C");
        assertThat(wyniki.get(1)).containsEntry("priorytet", "WYKLUCZONY");
    }

    @Test
    void zapisujeWynikiBezZgloszenia() {
        List<Map<String, Object>> wyniki = List.of(wynik("11111111111", "C", "PODSTAWOWY").doZmiennych());

        service.zapiszWyniki(wyniki, null, null, null);

        verify(mieszkancy).zapisz(List.of(wynik("11111111111", "C", "PODSTAWOWY")));
        verify(zgloszenia, never()).zakoncz(any(Long.class), any());
    }

    @Test
    void potwierdzonaSmiercZmieniaStatusIPrzeliczaPriorytet() {
        Osoba przed = osoba("11111111111", "ZYJE");
        Osoba po = osoba("11111111111", "NIE_ZYJE");
        when(zgloszenia.znajdz(7)).thenReturn(Optional.of(zgloszenie("SMIERC", "11111111111", null)));
        when(ewidencja.znajdz("11111111111")).thenReturn(Optional.of(po));
        when(priorytety.oblicz(po)).thenReturn(
                new WynikPriorytetu("11111111111", 1, "NIE_ZYJE", "WYKLUCZONY", "PODSTAWOWY", false));
        List<Map<String, Object>> wyniki = List.of(wynik("11111111111", "C", "PODSTAWOWY").doZmiennych());

        service.zapiszWyniki(wyniki, Map.of("id", 7), "POTWIERDZONE", "zgon potwierdzony");

        verify(ewidencja).ustawStatus("11111111111", "NIE_ZYJE");
        ArgumentCaptor<List<WynikPriorytetu>> zapis = ArgumentCaptor.forClass(List.class);
        verify(mieszkancy, org.mockito.Mockito.times(2)).zapisz(zapis.capture());
        // drugi zapis = przeliczony mieszkaniec po zmianie
        assertThat(zapis.getAllValues().get(1)).extracting(WynikPriorytetu::priorytet).containsExactly("WYKLUCZONY");
        verify(zgloszenia).zakoncz(7, "POTWIERDZONE");
        verify(audyt).zapisz(eq("zarzad"), eq("P1b"), eq("ZWERYFIKOWANO"), any());
    }

    @Test
    void odrzuconeZgloszenieNieZmieniaEwidencji() {
        when(zgloszenia.znajdz(7)).thenReturn(Optional.of(zgloszenie("SMIERC", "11111111111", null)));

        service.zapiszWyniki(List.of(wynik("11111111111", "C", "PODSTAWOWY").doZmiennych()),
                Map.of("id", 7), "ODRZUCONE", "pomyłka");

        verify(ewidencja, never()).ustawStatus(any(), any());
        verify(zgloszenia).zakoncz(7, "ODRZUCONE");
    }

    @Test
    void niezweryfikowanaChorobaJestPrzyjmowanaAutomatycznie() {
        Osoba po = new Osoba("11111111111", 1, "ZYJE", 40, "CHOROBA", false, "BRAK", false, false, 0);
        when(zgloszenia.znajdz(7)).thenReturn(Optional.of(zgloszenie("CHOROBA", "11111111111", null)));
        when(ewidencja.znajdz("11111111111")).thenReturn(Optional.of(osoba("11111111111", "ZYJE")), Optional.of(po));
        when(priorytety.oblicz(po)).thenReturn(wynik("11111111111", "B", "PODWYZSZONY"));

        service.zapiszWyniki(List.of(wynik("11111111111", "C", "PODSTAWOWY").doZmiennych()),
                Map.of("id", 7), null, null);

        verify(ewidencja).ustawStanZdrowia("11111111111", "CHOROBA");
        verify(zgloszenia).zakoncz(7, null);
    }

    @Test
    void chorobaNieObnizaCiezkiegoStanu() {
        Osoba ciezki = new Osoba("11111111111", 1, "ZYJE", 40, "CIEZKI", false, "BRAK", false, false, 0);
        when(zgloszenia.znajdz(7)).thenReturn(Optional.of(zgloszenie("CHOROBA", "11111111111", null)));
        when(ewidencja.znajdz("11111111111")).thenReturn(Optional.of(ciezki));
        when(priorytety.oblicz(ciezki)).thenReturn(wynik("11111111111", "A", "KRYTYCZNY"));

        service.zapiszWyniki(List.of(), Map.of("id", 7), "POTWIERDZONE", null);

        verify(ewidencja, never()).ustawStanZdrowia(any(), any());
    }

    @Test
    void utrataMieszkaniaDlaBudynkuDotyczyWszystkichMieszkancow() {
        when(zgloszenia.znajdz(7)).thenReturn(Optional.of(zgloszenie("UTRATA_MIESZKANIA", null, 5)));
        when(ewidencja.peseleWBudynku(5)).thenReturn(List.of("11111111111", "22222222222"));
        Osoba a = osoba("11111111111", "ZYJE");
        Osoba b = osoba("22222222222", "ZYJE");
        when(ewidencja.znajdz("11111111111")).thenReturn(Optional.of(a));
        when(ewidencja.znajdz("22222222222")).thenReturn(Optional.of(b));
        when(priorytety.oblicz(any(Osoba.class))).thenReturn(wynik("x", "A", "PODWYZSZONY"));

        service.zapiszWyniki(List.of(), Map.of("id", 7), "POTWIERDZONE", null);

        verify(ewidencja).ustawUtrateMieszkania("11111111111");
        verify(ewidencja).ustawUtrateMieszkania("22222222222");
    }

    @Test
    void brakZgloszeniaWBazieToBlad() {
        when(zgloszenia.znajdz(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.zapiszWyniki(List.of(), Map.of("id", 99), null, null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("99");
    }
}
