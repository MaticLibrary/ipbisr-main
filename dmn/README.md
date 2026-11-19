# dmn/

Tabele decyzyjne DMN (Camunda 8), wdrażane razem z modelami z `bpmn/wykonywalne/` (backend wdraża je przy starcie).

| Plik | Decyzje (id) | Używa |
|---|---|---|
| `Priorytet_mieszkancow.dmn` | `Priorytet_mieszkancow` (hit policy FIRST) – priorytet A/B/C/WYKLUCZONY, poziom potrzeb, dostawa bezpośrednia | P1b – worker `wyznacz-potrzeby-i-priorytet` wywołuje ją dla każdego mieszkańca |
| | `Normy_dzienne` (hit policy UNIQUE) – dobowe normy zasobów na osobę (woda, żywność, leki, higiena, schronienie) | P3 (M4) – wyliczenie potrzeb budynków |

Reguły czytamy od góry – pierwsza pasująca wygrywa (np. ciężki stan zdrowia ma pierwszeństwo przed utratą mieszkania).
Testy: `backend/src/test/java/pl/ans/ipbisr/dmn/PriorytetDmnTest.java` (Camunda w kontenerze, wymaga Dockera).
`Poziom_zagrozenia.dmn` (P5) – dopiero po akceptacji modelu P5.
