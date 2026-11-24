# Frontend panelu operacyjnego

*Tekst przygotowany jako materiał do rozdziału 3 pracy. Opisuje stan implementacji frontendowej; nie zastępuje opisu backendu, Camundy ani serwisu ML. Kontrakty i zakres ekranów pochodzą z [planu etapu III](./PLAN_ETAP3.md).*

## Cel i miejsce w architekturze

Frontend systemu IPBiSR jest panelem operacyjnym dla operatora punktu, analityka i zarządu. Umożliwia rejestrowanie zgłoszeń, przeglądanie informacji o budynkach i mieszkańcach, analizę prognoz, kontrolę planu przydziału, podgląd magazynu, monitorowanie instancji procesów oraz przegląd dziennika audytu.

Aplikację zbudowano w Next.js z App Router, React, TypeScript i Material UI. Frontend odpowiada za prezentację danych i obsługę interakcji. Dane domenowe i wykonanie procesów pozostają po stronie backendu Spring Boot oraz Camunda 8. Przeglądarka komunikuje się z backendem przez REST. Zadania wymagające decyzji człowieka są wykonywane w Camunda Tasklist; panel nie zatwierdza ich samodzielnie.

```text
Użytkownik
   │
   ▼
Frontend Next.js ── REST /api/... ──► Backend Spring Boot
   │                                      │
   ├── link do Camunda Tasklist            ├── Camunda 8
   └── link do Operate                     ├── PostgreSQL
                                          └── serwis ML
```

Wspólny układ aplikacji zawiera nawigację, wybór roli demonstracyjnej, odnośnik do Tasklist oraz informacje o dostępności backendu i serwisu ML. Stan usług jest sprawdzany po stronie serwera Next.js przez ich endpointy `/health`; brak odpowiedzi jest pokazywany jako niedostępność, a nie zastępowany statusem pozytywnym.

## Ekrany i nawigacja

| Ścieżka | Rola w menu | Funkcje |
|---|---|---|
| `/` | wszystkie | Strona startowa z odnośnikami do ekranów dostępnych dla wybranej roli oraz stanem usług. |
| `/operator` | operator | Formularz zgłoszenia zdarzenia i lista ostatnich zgłoszeń. Dla utraty mieszkania można podać sam budynek; pozostałe rodzaje wymagają PESEL. |
| `/analityk/budynki` | analityk, zarząd | Tabela budynków z dzielnicą, priorytetem i liczbą mieszkańców; filtr dzielnicy; szczegóły budynku z priorytetem, poziomem potrzeb i statusem mieszkańców. PESEL jest maskowany. |
| `/analityk/prognoza` | analityk | Wykres historii zużycia i oznaczona osobno wartość ostatniej prognozy. Dane można filtrować według zasobu i miasta lub dzielnicy. Widok pokazuje także źródło, status, różnicę procentową oraz historię prognoz. |
| `/analityk/plan` | analityk | Status planu, poziom pokrycia potrzeb, pozycje przydziału, niedobory, dostawy bezpośrednie i harmonogram. Decyzja o planie pozostaje w Tasklist. |
| `/magazyn` | analityk | Stany magazynowe. Edycja jest dostępna wyłącznie w jawnym trybie demo; przy połączeniu z backendem ekran jest tylko do odczytu. |
| `/procesy` | wszystkie | Lista instancji Camunda z linkami do Operate oraz symulacje zmiany sytuacji i ponownego planowania z poziomem zagrożenia 0–3. |
| `/audyt` | zarząd | Lista wpisów audytowych z wyborem limitu i rozwijanymi szczegółami. |

Dotychczasowe adresy `/zgloszenia`, `/budynki`, `/prognoza` i `/plan` przekierowują do nowych ścieżek, aby zachować zgodność ze starszymi odnośnikami.

Rola jest domyślnie ustawiona na analityka i można ją zmienić w nagłówku panelu. Wybór jest zapisywany w `localStorage` przeglądarki. Służy wyłącznie do prezentacji odpowiedniego menu i skrótów. Nie jest logowaniem ani autoryzacją: bezpośrednie wejście na ścieżkę nie jest blokowane. Lista zgłoszeń zawiera ostatnie rekordy zwrócone przez API, a nie zgłoszenia przypisane do konkretnego użytkownika, ponieważ aplikacja nie posiada jego tożsamości.

## Integracja z API

Wspólny klient REST znajduje się w [`frontend/src/lib/api.ts`](../frontend/src/lib/api.ts). Nazwy pól i wartości słownikowe odpowiadają kontraktowi z [planu etapu III](./PLAN_ETAP3.md); błędy backendu w formacie `{"blad":"..."}` są pokazywane użytkownikowi.

| Ekran | Żądania backendu |
|---|---|
| Zgłoszenia | `GET /api/budynki`, `GET /api/zgloszenia`, `POST /api/zgloszenia` |
| Budynki | `GET /api/budynki`, `GET /api/budynki/{id}` |
| Prognoza | `GET /api/prognozy/ostatnia`, `GET /api/prognozy`, `GET /api/zuzycie/historia?zasob=...&dzielnica=...` |
| Plan | `GET /api/plany/ostatni` |
| Magazyn | `GET /api/magazyn` |
| Procesy | `GET /api/procesy`, `POST /api/symulacja/zmiana-sytuacji`, `POST /api/symulacja/ponowne-planowanie` |
| Audyt | `GET /api/audyt?limit=...` |

Żądania klienta mają limit czasu 10 sekund. Odpowiedź `404` dla nieistniejącej prognozy lub planu jest przedstawiana jako stan pusty. Inne błędy HTTP, błędy sieciowe oraz niepoprawne odpowiedzi są wyświetlane jako błędy. Błąd API nie uruchamia automatycznie danych demo.

Na wykresie prognoza jest dobierana dla wybranego zasobu i poziomu: miasta albo wskazanej dzielnicy. Kontrakt prognozy nie zawiera daty jej obowiązywania, dlatego frontend pokazuje wartość jako osobny, podpisany punkt, bez przypisywania jej fikcyjnej daty. Wykres jest responsywnym SVG z legendą i tabelą danych historycznych; nie używa `@mui/x-charts`, mimo że biblioteka ta została wymieniona w planie jako preferowana.

## Tryb demonstracyjny i konfiguracja

Tryb demo włącza się jawnie przez `NEXT_PUBLIC_DEMO_DATA=true` w `frontend/.env.local`. Panel oznacza go etykietą „Dane demo” i używa syntetycznych rekordów z [`frontend/src/lib/demo-data.ts`](../frontend/src/lib/demo-data.ts). Zmiany stanów magazynowych w tym trybie są przechowywane wyłącznie w pamięci aplikacji demonstracyjnej i nie zapisują się w bazie.

Adresy usług konfiguruje się w `frontend/.env.local`:

- `BACKEND_URL` i `ML_SERVICE_URL` — adresy używane przez serwer Next.js do sprawdzania `/health`;
- `NEXT_PUBLIC_BACKEND_URL` — adres API backendu dostępny dla klienta przeglądarkowego;
- `NEXT_PUBLIC_CAMUNDA_OPERATE_URL` i `NEXT_PUBLIC_CAMUNDA_TASKLIST_URL` — adresy linków do interfejsów Camunda;
- `NEXT_PUBLIC_DEMO_DATA` — jawne włączenie albo wyłączenie danych syntetycznych.

Plik [`frontend/.env.example`](../frontend/.env.example) zawiera wartości lokalne bez danych uwierzytelniających. Hasła i sekrety nie powinny trafiać do zmiennych `NEXT_PUBLIC_*`.

## Obsługa stanów i prywatność

Widoki rozróżniają ładowanie danych, pusty wynik, błąd API oraz brak prognozy lub planu. Statusy procesów i zgłoszeń są prezentowane tekstowo, nie tylko kolorem. Lista zgłoszeń odświeża się co 5 sekund, a lista procesów co 10 sekund. PESEL jest maskowany na tabelach mieszkańców i dostaw; pełna wartość jest potrzebna wyłącznie w formularzu zgłoszenia i żądaniu do backendu.

## Uruchomienie i weryfikacja

Z katalogu `frontend`:

```powershell
npm ci
npm run dev
npm test
npm run build
```

Panel developerski jest dostępny pod `http://localhost:3000`. Dla pracy bez backendu należy jawnie włączyć `NEXT_PUBLIC_DEMO_DATA=true` w `.env.local`.

Testy logiki klienta sprawdzają m.in. walidację zgłoszeń, obsługę odpowiedzi API, filtrowanie budynków i prognoz, ograniczenie edycji magazynu do demo oraz widoczność menu według roli. Build produkcyjny sprawdza kompilację i typy TypeScript.

## Ograniczenia

- Wybór roli nie zabezpiecza dostępu do danych ani operacji.
- Lista zgłoszeń nie jest filtrowana według operatora.
- Z poziomu frontendu nie zatwierdza się zadań Camunda.
- Edycja magazynu nie zapisuje danych backendu; działa tylko w pamięci trybu demo.
- Wartość prognozy nie ma w API daty obowiązywania, więc wykres nie przypisuje jej daty.
- Wykres wykonano jako SVG zamiast wskazanej w planie biblioteki `@mui/x-charts`.
- Dostępność działania P2/P3 zależy od backendu, procesów Camunda, danych oraz serwisu ML; frontend nie zastępuje tych komponentów.
