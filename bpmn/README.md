# Modele BPMN – status

| Proces | Model (etap 2) | Akceptacja prowadzącego | Wersja wykonywalna (`wykonywalne/`) |
|---|---|---|---|
| **P1 – Analiza sytuacji mieszkańców** | `modele-etap2/P1_…` | ✅ zatwierdzony | ✅ `P1a_Rejestr_ludnosci`, `P1b_Analiza_sytuacji_mieszkancow`, `P1c_Rejestracja_zgloszenia` |
| **P2 – Prognozowanie zapotrzebowania** | `modele-etap2/P2_…` | ✅ zatwierdzony | 🔧 w realizacji: `P2a_System_pogodowy`, `P2b_Prognozowanie_zapotrzebowania` |
| **P3 – Przydział zasobów** | `modele-etap2/P3_…` | ✅ zatwierdzony | 🔧 w realizacji: `P3a_Przydzial_zasobow`, `P3b_Organizacje_humanitarne` |
| P4 – Dystrybucja zasobów | `modele-etap2/P4_…` | ⏳ czeka na akceptację | — (nie implementujemy) |
| P5 – Obsługa sytuacji nadzwyczajnych | `modele-etap2/P5_…` | ⏳ czeka na akceptację | — (nie implementujemy) |

## Zasady

- `modele-etap2/` – modele zatwierdzone przez prowadzącego. **Nie zmieniamy ich logiki ani wyglądu.**
- `wykonywalne/` – każdy pool to **osobny proces i osobny plik** (wymóg prowadzącego). Względem modelu dodajemy wyłącznie
  ustawienia techniczne Camunda 8: job type, nazwy wiadomości, correlation key, warunki na bramkach, formularze user tasków.
- Procesy komunikują się **wiadomościami** (message), nie wywołaniami bezpośrednimi.
- Do Camundy wdrażamy tylko pliki z `wykonywalne/` (+ `dmn/`). Połączone modele z `modele-etap2/` są tylko dokumentacją.
