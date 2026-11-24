# data/

Dane syntetyczne do trenowania modelu ML i do testów (mieszkańcy, budynki, historia zużycia, stany magazynowe).
Generowane skryptem w `ml-service/` (`scripts/generate_data.py`, ziarno `seed=42`) – nie zawierają prawdziwych danych osobowych.

## Kontrakt plików CSV (dla generatora – Robert)

Backend (profil `dev`) wczytuje te pliki do bazy – klasa `CsvDataImporter`. Zasady:
kodowanie **UTF-8**, separator **przecinek** (bez przecinków wewnątrz pól), pierwszy wiersz = **nagłówek** z poniższymi nazwami
(kolejność kolumn dowolna, dodatkowe kolumny są ignorowane), puste pole = NULL, wartości logiczne `true`/`false`, daty `RRRR-MM-DD`.
Pliki wczytujemy w tej kolejności (klucze obce!). Jeśli pliku nie ma, backend go pomija.

| Plik | Kolumny |
|---|---|
| `dzielnice.csv` | `id`, `nazwa` |
| `punkty_wydawania.csv` | `id`, `nazwa`, `dzielnica_id`, `przepustowosc_kg` |
| `budynki.csv` | `id`, `adres`, `dzielnica_id`, `punkt_id` |
| `ewidencja_ludnosci.csv` | `pesel`, `imie`, `nazwisko`, `plec` (K/M), `data_ur`, `zawod`, `zawod_krytyczny`, `budynek_id`, `gospodarstwo_id`, `stan_zdrowia` (DOBRY/CHOROBA/CIEZKI), `choroby_przewlekle`, `niepelnosprawnosc` (BRAK/LEKKI/UMIARKOWANY/ZNACZNY), `utrata_mieszkania`, `osoby_na_utrzymaniu`, `status` (ZYJE/NIE_ZYJE/ZAGINIONY) |
| `stany_magazynowe.csv` | `zasob` (ZYWNOSC/WODA/LEKI/HIGIENA/SCHRONIENIE), `ilosc`, `jednostka` |
| `historia_zuzycia.csv` | `data`, `dzielnica_id`, `zasob`, `ilosc`, `dzien_tygodnia`, `miesiac`, `temperatura`, `opady`, `liczba_mieszkancow`, `procent_seniorow`, `procent_chorych`, `poziom_zagrozenia` (0–3), `liczba_alarmow_24h`, `liczba_przesiedlonych` |

Uwagi:
- Kolumny `zawod_krytyczny`, `utrata_mieszkania`, `osoby_na_utrzymaniu` **dodałem względem planu** – potrzebuje ich tabela DMN
  `Priorytet` (sekcja 5 planu). Jeśli generator ich nie ma, napisz – zmienimy schemat.
- `pesel` musi mieć 11 znaków; `plec` to `K` lub `M`; wartości słownikowe pilnuje baza (ograniczenia `CHECK`).
- Import jest „wszystko albo nic” (błąd w jednym pliku cofa cały import i wskazuje plik oraz linię) i pomija tabele,
  które mają już dane. Po ponownym wygenerowaniu CSV trzeba wyczyścić bazę: `docker compose down -v` (usuwa wolumen `pgdata`).
- Pliki z BOM (np. Python `utf-8-sig`) i końcami linii CRLF są obsługiwane; wartości logiczne tylko `true`/`false`.
- Tabela `mieszkaniec` (rejestr potrzeb) **nie** pochodzi z CSV – wypełnia ją proces P1.
