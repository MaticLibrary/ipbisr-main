# Projekt IPBiSR – instrukcja dla Claude Code

## O projekcie
Projekt semestralny z przedmiotu **Inżynieria procesów biznesowych i systemów regułowych** (Akademia Nauk Stosowanych w Tarnowie, prowadzący: Tomasz Potempa).
Autorzy: Ilia Smakovskyi, Robert Kłusek.

Temat: **System inteligentnego przydzielania zasobów niezbędnych do życia mieszkańcom miasta podczas wojny.**
Pełny opis (cel, aktorzy, wymagania, opis procesów, modele BPMN): `docs/Projekt_IPBiSR.pdf`.
Etapy 1 (projekt koncepcyjny) i 2 (modele BPMN) są zrobione. Teraz robimy **etap 3: implementacja procesów, reguł i algorytmów AI/ML** (60 pkt), a potem etap 4: testy i optymalizacja (10 pkt).

Wymagania prowadzącego:
- procesy muszą działać na serwerze **Camunda 8** (serwer uczelniany lub lokalnie Camunda 8 Run),
- implementacja algorytmów: uczenie maszynowe / reguły (DMN) / big data, plus testy,
- **każdy pool to osobny proces i osobny diagram**, procesy komunikują się wiadomościami,
- system ma być mniejszy, ale gęstszy – NIE rozbudowuj logiki ponad modele,
- obaj autorzy muszą rozumieć i umieć wyjaśnić każdy element – kod ma być prosty i czytelny, z krótkimi komentarzami po polsku.

Wzór od prowadzącego (warto się wzorować na strukturze): https://github.com/tpotempa/University-Recruitment-2.0.0

## Procesy (zatwierdzone modele w `bpmn/modele-etap2/`)
1. **P1 – Analiza sytuacji mieszkańców**: timer co 12 h lub wiadomość „Zgłoszenie zdarzenia” (od operatora punktu) → zapytanie do Rejestru ludności (send) → odbiór danych (receive) → DMN: potrzeby i priorytet (A/B/C) → bramka „Są zgłoszenia do weryfikacji?” → (tak) Zarząd weryfikuje (user task) → zapis wyników.
   Już rozdzielony na 3 procesy w `bpmn/wykonywalne/` (P1a Rejestr, P1b Analiza, P1c Rejestracja zgłoszenia).
2. **P2 – Prognozowanie zapotrzebowania**: timer co 12 h lub wiadomość „Zmiana sytuacji kryzysowej” (z P5) → zapytanie/odbiór danych pogodowych → **model ML** (prognoza) → jeśli różnica > 30% → Analityk weryfikuje → wiadomość „Prognoza gotowa” do P3.
3. **P3 – Przydział zasobów**: wiadomość „Prognoza gotowa” → pobranie stanów, potrzeb, prognoz → **algorytm optymalizacyjny** (plan przydziału) → jeśli potrzeby krytyczne niepokryte → wniosek do Organizacji humanitarnych i odbiór odpowiedzi → Analityk zatwierdza plan → wiadomość „Zlecenie wydania” do P4.
4. **P4 – Dystrybucja zasobów** (Magazyn + Operator punktu): zlecenie → skompletuj → przewieź → wydaj (ID budynku, PESEL) → jeśli brak odbioru → wiadomość do P1 → zwrot i raport. *(model czeka na akceptację prowadzącego)*
5. **P5 – Obsługa sytuacji nadzwyczajnych**: ostrzeżenie z Systemu ostrzegania OC → DMN: poziom zagrożenia → jeśli plany do zmiany → Analityk zatwierdza → wiadomość „Zmiana sytuacji kryzysowej” do P2 → komunikaty dla mieszkańców. *(model czeka na akceptację prowadzącego)*
6. **Na razie sa sprawdzine 3 modele, to 3 są do realisacji, 4,-5 dopiki nie sprawdzony

Systemy zewnętrzne (Rejestr ludności, System pogodowy, Organizacje humanitarne, System ostrzegania OC) **symulujemy** – jako proste procesy BPMN z workerami albo proste endpointy z danymi testowymi. Nie łączymy się z prawdziwymi systemami.

## Stack (uproszczony względem „wstępnego” z dokumentacji)
- **Camunda 8** (Zeebe) – BPMN, DMN, Tasklist dla user tasków, Operate do podglądu
- **Java 21 + Spring Boot 3 + Camunda Spring Boot SDK** – job workery dla service/send tasków, REST do startu procesów i wysyłania wiadomości
- **Python 3 + FastAPI + scikit-learn + pandas** – serwis ML (prognoza zapotrzebowania) i optymalizacja przydziału (np. `scipy.optimize.linprog` lub prosty algorytm priorytetowy)
- **PostgreSQL** – mieszkańcy, budynki, stany magazynowe, prognozy, plany, dziennik audytu
- **Docker Compose** – uruchomienie Postgresa, backendu i serwisu ML
- **Next.js + TypeScript + Material UI** – prosty frontend (panel), szczegóły niżej
- Testy: **JUnit 5 + Mockito**, testy procesów (camunda-process-test), **pytest** dla ML
- BEZ Kafki – komunikację między procesami robi Camunda przez wiadomości.

### Zakres frontendu (prosty, bez przesady)
- **Panel operatora punktu**: formularz „Zgłoszenie zdarzenia” (PESEL / ID budynku, rodzaj zdarzenia) → backend uruchamia proces P1c / wysyła wiadomość do Camundy.
- **Panel analityka**: lista budynków i mieszkańców z priorytetem (A/B/C), ostatnia prognoza zapotrzebowania (wykres), plan przydziału zasobów.
- **Podgląd stanu procesów**: lista uruchomionych instancji (dane z backendu).
- User taski (weryfikacja przez zarząd, zatwierdzenie prognozy/planu) **domyślnie obsługujemy w Camunda Tasklist**; własne formularze tylko jeśli zostanie czas.
- Logowanie: prosta rola (operator / analityk / zarząd) – bez rozbudowanego Spring Security na start.

## Proponowana struktura repozytorium
```
bpmn/modele-etap2/   – zatwierdzone modele z etapu 2 (dokumentacja, nie zmieniać bez zgody)
bpmn/wykonywalne/    – modele rozdzielone na osobne procesy, z ustawieniami Camunda 8
dmn/                 – tabele decyzyjne (priorytet mieszkańca, poziom zagrożenia)
backend/             – Spring Boot: job workery, REST, JPA
ml-service/          – FastAPI: /forecast (ML), /allocate (optymalizacja)
frontend/            – Next.js + TypeScript + MUI
data/                – dane syntetyczne do trenowania i testów
docs/                – dokumentacja projektu (PDF)
docker-compose.yml
```

## Plan pracy (kolejność)
1. **Szkielet repo** – utwórz wszystkie foldery ze struktury powyżej, `docker-compose.yml` (Postgres), minimalne działające projekty: backend (Spring Boot, endpoint `/health`), ml-service (FastAPI, `/health`), frontend (Next.js, strona startowa), `.env.example`, README z instrukcją uruchomienia.
2. P1: modele wykonywalne (job type dla każdego service/send taska, nazwy wiadomości + correlation key, warunki na bramkach), DMN „Priorytet mieszkańca”, workery, dane testowe mieszkańców. Deploy i uruchomienie end-to-end.
3. P2: rozdzielenie na osobne procesy, serwis ML (wygenerować dane syntetyczne zużycia, wytrenować model regresji, endpoint /forecast), worker wywołujący ML.
4. P3: algorytm optymalizacyjny (/allocate), worker, user task zatwierdzenia planu.
5. P4 i P5 – dopiero po akceptacji modeli przez prowadzącego.
6. Frontend: panel operatora (zgłoszenie) i panel analityka (budynki, prognoza, plan).
7. Testy (jednostkowe + testy procesów) i dane do rozdziału 4 dokumentacji (wykresy, wnioski).
8. Opis architektury do rozdziału 3 dokumentacji (diagram komponentów, lista serwisów i endpointów API).

## Plan etapu III
Szczegółowy plan (job type, wiadomości, DMN, ML, algorytm, API, ekrany, kamienie milowe): **`docs/PLAN_ETAP3.md`**. Status modeli: `bpmn/README.md`.

## Podział pracy
- **Ilia Smakovskyi** – backend (Spring Boot), Camunda, baza danych, procesy P1 i P3.
- **Robert Kłusek** – serwis ML (Python), proces P2, frontend.
- Razem – kontrakty API, DMN, testy, dokumentacja.
Pracujemy na osobnych gałęziach i robimy Pull Requesty do `main`; drugi autor przegląda PR.

## Camunda – serwer uczelniany
- Adres od prowadzącego (Operate na porcie 8080), logowanie basic: login `2026_nazwisko_imie` (bez polskich znaków), hasło = numer albumu.
- Adres, login i hasło trzymamy **tylko w `.env`** (szablon: `.env.example`).
- Wersja Camundy na serwerze – **jeszcze nieznana** (czekamy na prowadzącego); od niej zależy wersja `camunda-spring-boot-starter`.
- Do czasu odpowiedzi pracujemy lokalnie na Camunda 8 Run.

## Skille Claude Code (`.claude/skills/`)
- `git-commit` – commity w stylu Conventional Commits (typ po angielsku, np. `feat(p1):`, opis po polsku – reguła repozytorium ma pierwszeństwo),
- `ui-ux-pro-max` – wskazówki UI/UX przy frontendzie,
- `requesting-code-review` – przegląd kodu przed PR do `main`.
Instalacja/odtworzenie: `npx skills experimental_install` (wersje w `skills-lock.json`).

## Zasady pracy
- Przed większą zmianą najpierw pokaż plan i poczekaj na akceptację.
- **Nie zmieniaj modeli** w `bpmn/modele-etap2/` – P1–P3 są zatwierdzone przez prowadzącego. W `bpmn/wykonywalne/` wolno dodać tylko ustawienia techniczne (job type, wiadomości, correlation key, warunki bramek, formularze) – bez nowych elementów, timerów brzegowych ani multi-instance. To, czego brakuje na diagramie, a jest w opisie procesu, robimy w kodzie (patrz `docs/PLAN_ETAP3.md`, sekcja 2).
- Nazwy elementów BPMN, wiadomości i zadań – po polsku, tak jak w modelach. Identyfikatory w kodzie – po angielsku.
- **Nigdy nie commituj haseł ani danych dostępowych** (np. Client ID / Secret do serwera Camunda). Trzymaj je w `.env`, który jest w `.gitignore`; w repo tylko `.env.example`.
- Po każdym kroku krótko wyjaśnij, co zostało zrobione i jak to uruchomić – autorzy muszą to rozumieć na obronie.
- Małe commity z opisem po polsku.
