# Projekt ekranów panelu IPBiSR

## Cel

Zbudować w obecnym Next.js + MUI prosty, responsywny panel operacyjny do demonstracji procesów IPBiSR. Ma obsłużyć role operatora punktu i analityka oraz pokazać dane mieszkańców, prognozę, plan przydziału i przebieg procesów. Użytkownik wybrał realizację wszystkich zaplanowanych ekranów w jednym etapie.

## Założenia i ograniczenia

- Frontend korzysta z istniejącego kontraktu REST backendu Spring Boot opisanego w `docs/PLAN_ETAP3.md`; nie zmienia kontraktu ani logiki BPMN.
- Stos pozostaje bez zmian: Next.js, TypeScript, React i MUI.
- Prawdziwe zadania użytkownika P1/P2/P3 pozostają w Camunda Tasklist. Frontend nie zastępuje Tasklist ani nie zatwierdza tych zadań.
- Wybór roli służy wyłącznie do filtrowania nawigacji/demonstracji, nie jest uwierzytelnianiem ani kontrolą dostępu; bez logowania każda ścieżka pozostaje dostępna.
- Brak danych, HTTP 404, błąd API i niedostępność backendu mają jawne, odrębne komunikaty. Dane przykładowe są dostępne wyłącznie w jawnym trybie demo (konfiguracja developerska), nigdy jako cichy fallback po błędzie.
- Nie dodajemy P4/P5, nowych procesów ani nowych endpointów.
- Bieżąca strona sprawdza zdrowie backendu i serwisu ML; zostaje ona zastąpiona panelem, a informacja o stanie serwisów będzie widoczna w układzie wspólnym.
- Sprawdzenie `/health` backendu i ML pozostaje po stronie serwera Next.js, zgodnie z obecnym wzorcem. Adresy serwisów są konfigurowalne przez `BACKEND_URL` i `ML_SERVICE_URL`; frontendowy klient REST używa `NEXT_PUBLIC_BACKEND_URL`. Lokalnie obowiązują porty 8081, 8000 i 8080 dla Operate.

## Podejścia

1. **P1 najpierw, potem kolejne ekrany** — szybciej daje działający formularz zgłoszeń i widok budynków, ale odkłada pełny panel; nie wybrano.
2. **Wszystkie zaplanowane ekrany w jednym etapie** — spójna nawigacja i gotowy przepływ demonstracyjny; ekrany bez danych pokazują jawne stany puste/błędy, a dane demo są odrębnie oznaczone. To podejście wybrał użytkownik.
3. **Jedna strona z linkami do narzędzi zewnętrznych** — najmniej pracy, ale nie realizuje zakresu ekranów operatora i analityka; odrzucono.

## Zakres ekranów

- **Przegląd (`/`)**: wejście do panelu, stan backendu i ML oraz skróty do modułów. Nie tworzy nowych wskaźników wymagających nieistniejącego endpointu.
- **Zgłoszenia (`/zgloszenia`)**: formularz operatora (PESEL albo ID budynku, rodzaj i opis) oraz lista zgłoszeń ze statusem. Sam budynek jest dozwolony wyłącznie dla `UTRATA_MIESZKANIA`; błędy walidacji API pokazujemy użytkownikowi. Lista odświeża statusy okresowo.
- **Budynki (`/budynki`)**: lista budynków z dzielnicą, liczbą mieszkańców i priorytetem; szczegóły budynku pokazują mieszkańców, poziom potrzeb i status. PESEL nie jest potrzebny w tabeli zbiorczej; nie eksponujemy go tam.
- **Prognoza (`/prognoza`)**: ostatnia prognoza, status/źródło, historia i wykres zużycia dla wybranego zasobu oraz opcjonalnej dzielnicy. Brak prognozy (404) to stan pusty.
- **Plan (`/plan`)**: ostatni plan, status, pokrycie potrzeb krytycznych, pozycje, niedobory, dostawy bezpośrednie i harmonogram. Brak planu (404) to stan pusty; nie ma tu akcji zatwierdzania.
- **Magazyn (`/magazyn`)**: aktualne stany i edycja ilości/jednostek przez `GET/PUT /api/magazyn`, z potwierdzeniem zapisu.
- **Procesy (`/procesy`)**: odświeżana lista instancji i link „Otwórz w Operate”; przyciski symulacji zmiany sytuacji i ponownego planowania, z poziomem zagrożenia 0–3.
- **Audyt (`/audyt`)**: chronologiczna lista wpisów, limit wyników i rozwijane szczegóły `szczegoly`.

### Nawigacja według roli

- **Operator**: Zgłoszenia, Budynki (potrzebne do wskazania budynku w formularzu).
- **Analityk**: Budynki, Prognoza, Plan, Magazyn, Procesy, Audyt.
- **Zarząd**: Procesy, Audyt. Weryfikacja zgłoszeń pozostaje w Tasklist.

Role wpływają na widoczne pozycje menu i stronę startową; nie ograniczają bezpośredniego wejścia na URL ani dostępu do danych.

## Układ i wygląd

Wspólny, jasny panel administracyjny: nagłówek ze statusem serwisów i wyborem roli; boczna nawigacja na dużych ekranach, dostępna na wąskich ekranach w formie menu; główna część ekranu zawiera tytuł, akcje i tabelę/formularz. Kolory statusów mają też etykiety tekstowe, a tabele i formularze obsługują klawiaturę oraz widoczny fokus. Zachowujemy prosty styl MUI, bez dodatkowych bibliotek UI. Wykres ma pozostać lekki i dostępny (tekstowe podsumowanie wartości obok wizualizacji).

## Poza zakresem

Logowanie i autoryzacja, własna obsługa zadań Camunda, mapy GIS, P4/P5, zmiany backendu/ML, nowe KPI oraz trwały tryb demonstracyjny z fikcyjnymi danymi.

## Do decyzji implementacyjnej

- Wspólny klient REST w `frontend/src/lib/api.ts`; adres backendu konfigurowany przez zmienną środowiskową publiczną dla przeglądarki.
- Widoki będą obsługiwać ładowanie, puste wyniki, błąd sieci/serwera i błędy formularzy osobno.
- Role w selektorze: operator, analityk i zarząd. Domyślna rola to analityk; menu stosuje macierz powyżej.
- Symulacje mają wspólny selektor poziomu zagrożenia 0–3 (wartość początkowa 0); po odpowiedzi pokazują potwierdzenie i `idZdarzenia`.
- Użyjemy minimalnej zależności wykresowej; jeśli istniejący zestaw nie wystarczy do dostępnego wykresu, dobór biblioteki będzie ograniczony do tej jednej potrzeby.
