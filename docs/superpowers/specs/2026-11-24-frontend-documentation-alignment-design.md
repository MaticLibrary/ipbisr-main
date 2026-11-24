# Dopasowanie frontendu do planu i dokumentacja — projekt

**Status: do akceptacji**

## Cel

Przygotować frontend do opisania w rozdziale 3 pracy: dopasować widoczne ekrany i menu do sekcji 10 `docs/PLAN_ETAP3.md`, a następnie opisać faktycznie działające rozwiązanie bez przypisywania mu funkcji, których nie ma.

## Rozważone podejścia

1. **Opisać obecny stan bez zmian w kodzie** — najszybciej, ale pozostawia rozbieżności względem planu; użytkownik wybrał inne podejście.
2. **Najpierw dopasować frontend do planu, potem przygotować opis** — wybrane; ogranicza różnice i daje materiał zgodny z rzeczywistym kodem.
3. **Przepisać panel od nowa** — odrzucone; obecne widoki działają, więc wystarczą punktowe zmiany.

## Zakres

- Zachować istniejące implementacje ekranów, dodać planowane ścieżki (`/operator`, `/analityk/budynki`, `/analityk/prognoza`, `/analityk/plan`) i utrzymać dotychczasowe adresy jako kompatybilne aliasy.
- Ustawić menu według tabeli z planu: operator — zgłoszenia; analityk — budynki i mieszkańcy, prognoza, plan, magazyn; zarząd — budynki i mieszkańcy, audyt; procesy — wszyscy. Ekran startowy i link do Tasklist pozostają dostępne zgodnie z rolą.
- Dodać filtr dzielnicy do tabeli budynków oraz status mieszkańca do szczegółów budynku.
- Na ekranie prognozy filtrować pozycję prognozy dla wybranego zasobu i poziomu (miasto albo dzielnica) oraz przedstawić ją razem z historią. Kontrakt API nie podaje daty, na którą prognoza obowiązuje, więc wykres pokaże ją jako osobno oznaczoną wartość, bez dopisywania fikcyjnej daty.
- Ograniczyć edycję magazynu do jawnego trybu demo; przy zwykłym połączeniu z backendem ekran będzie tylko do odczytu.
- Pozostawić formularz i listę zgłoszeń, monitoring procesów, symulacje, audyt, stany ładowania/błędów oraz jawne dane demo.
- Przygotować `docs/FRONTEND.md` jako tekst gotowy do wykorzystania w rozdziale 3: architektura, ekrany i role, integracja API, tryb demo, uruchomienie, testy oraz ograniczenia.
- Uaktualnić tylko checklisty frontendowe i ich statusy w `docs/PLAN_ETAP3.md`, a z `README.md` dodać odnośnik do opisu. Nie oznaczać jako ukończonych zadań ML ani backendu.

## Ograniczenia i decyzje

- Nie zmieniamy API, procesów BPMN/DMN, backendu, serwisu ML ani P4/P5.
- Wybór roli nadal jest demonstracyjny, zapisany w przeglądarce; nie jest uwierzytelnianiem ani autoryzacją.
- Przy braku tożsamości użytkownika lista zgłoszeń nie będzie udawać listy „moich” zgłoszeń; dokumentacja nazwie ją listą ostatnich zgłoszeń.
- Zadania człowieka pozostają w Camunda Tasklist.
- Zostaje obecny lekki wykres SVG zamiast wprowadzania nowej biblioteki: to świadoma decyzja implementacyjna, choć plan wymienia `@mui/x-charts`; zostanie to jawnie zaznaczone w dokumentacji.

## Weryfikacja

Po zmianach uruchomić testy frontendu i build produkcyjny. Dodać/zmienić testy logiki menu i filtrowania danych, a następnie sprawdzić kluczowe ścieżki oraz układ mobilny.
