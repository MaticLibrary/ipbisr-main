-- Schemat bazy systemu (sekcja 9 planu etapu III).
-- Nazwy tabel i kolumn po polsku, tak jak w dokumentacji; wartości słownikowe pilnują ograniczenia CHECK.

-- ===== Dane referencyjne (generowane do data/*.csv) =====

CREATE TABLE dzielnica (
    id    INTEGER PRIMARY KEY,
    nazwa TEXT NOT NULL
);

CREATE TABLE punkt_wydawania (
    id               INTEGER PRIMARY KEY,
    nazwa            TEXT NOT NULL,
    dzielnica_id     INTEGER NOT NULL REFERENCES dzielnica (id),
    przepustowosc_kg NUMERIC NOT NULL CHECK (przepustowosc_kg >= 0)  -- ile kg można przewieźć do punktu
);

CREATE TABLE budynek (
    id           INTEGER PRIMARY KEY,
    adres        TEXT NOT NULL,
    dzielnica_id INTEGER NOT NULL REFERENCES dzielnica (id),
    punkt_id     INTEGER NOT NULL REFERENCES punkt_wydawania (id)
);

-- Rejestr ludności – system zewnętrzny, w projekcie tylko symulowany (czyta go proces P1a)
CREATE TABLE ewidencja_ludnosci (
    pesel               CHAR(11) PRIMARY KEY,
    imie                TEXT NOT NULL,
    nazwisko            TEXT NOT NULL,
    plec                CHAR(1) NOT NULL CHECK (plec IN ('K', 'M')),
    data_ur             DATE NOT NULL,
    zawod               TEXT,
    zawod_krytyczny     BOOLEAN NOT NULL DEFAULT FALSE,   -- lekarz, ratownik, strażak, energetyk…
    budynek_id          INTEGER NOT NULL REFERENCES budynek (id),
    gospodarstwo_id     INTEGER,
    stan_zdrowia        TEXT NOT NULL CHECK (stan_zdrowia IN ('DOBRY', 'CHOROBA', 'CIEZKI')),
    choroby_przewlekle  BOOLEAN NOT NULL DEFAULT FALSE,
    niepelnosprawnosc   TEXT NOT NULL DEFAULT 'BRAK'
                        CHECK (niepelnosprawnosc IN ('BRAK', 'LEKKI', 'UMIARKOWANY', 'ZNACZNY')),
    utrata_mieszkania   BOOLEAN NOT NULL DEFAULT FALSE,
    osoby_na_utrzymaniu INTEGER NOT NULL DEFAULT 0 CHECK (osoby_na_utrzymaniu >= 0),
    status              TEXT NOT NULL CHECK (status IN ('ZYJE', 'NIE_ZYJE', 'ZAGINIONY'))
);
CREATE INDEX idx_ewidencja_budynek ON ewidencja_ludnosci (budynek_id);

CREATE TABLE stan_magazynowy (
    zasob     TEXT PRIMARY KEY CHECK (zasob IN ('ZYWNOSC', 'WODA', 'LEKI', 'HIGIENA', 'SCHRONIENIE')),
    ilosc     NUMERIC NOT NULL CHECK (ilosc >= 0),
    jednostka TEXT NOT NULL
);

-- Historia zużycia = dane treningowe modelu ML (cechy + zużycie)
CREATE TABLE zuzycie_historia (
    id                  BIGSERIAL PRIMARY KEY,
    data                DATE NOT NULL,
    dzielnica_id        INTEGER NOT NULL REFERENCES dzielnica (id),
    zasob               TEXT NOT NULL CHECK (zasob IN ('ZYWNOSC', 'WODA', 'LEKI', 'HIGIENA', 'SCHRONIENIE')),
    ilosc               NUMERIC NOT NULL,
    dzien_tygodnia      INTEGER,
    miesiac             INTEGER,
    temperatura         NUMERIC,
    opady               NUMERIC,
    liczba_mieszkancow  INTEGER,
    procent_seniorow    NUMERIC,
    procent_chorych     NUMERIC,
    poziom_zagrozenia   INTEGER CHECK (poziom_zagrozenia BETWEEN 0 AND 3),
    liczba_alarmow_24h  INTEGER,
    liczba_przesiedlonych INTEGER
);
CREATE INDEX idx_zuzycie_dzielnica_zasob ON zuzycie_historia (dzielnica_id, zasob, data);

-- ===== P1 – analiza sytuacji mieszkańców =====

-- Rejestr potrzeb: wynik procesu P1 (priorytet i potrzeby każdego mieszkańca)
CREATE TABLE mieszkaniec (
    pesel                 CHAR(11) PRIMARY KEY REFERENCES ewidencja_ludnosci (pesel),
    budynek_id            INTEGER NOT NULL REFERENCES budynek (id),
    priorytet             TEXT NOT NULL CHECK (priorytet IN ('A', 'B', 'C', 'WYKLUCZONY')),
    poziom_potrzeb        TEXT NOT NULL CHECK (poziom_potrzeb IN ('PODSTAWOWY', 'PODWYZSZONY', 'KRYTYCZNY')),
    dostawa_bezposrednia  BOOLEAN NOT NULL DEFAULT FALSE,
    status                TEXT NOT NULL CHECK (status IN ('ZYJE', 'NIE_ZYJE', 'ZAGINIONY')),
    data_analizy          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_mieszkaniec_budynek ON mieszkaniec (budynek_id);

CREATE TABLE zgloszenie (
    id               BIGSERIAL PRIMARY KEY,
    pesel            CHAR(11) REFERENCES ewidencja_ludnosci (pesel),
    budynek_id       INTEGER REFERENCES budynek (id),
    rodzaj           TEXT NOT NULL CHECK (rodzaj IN
                     ('CHOROBA', 'NIEPELNOSPRAWNOSC', 'UTRATA_MIESZKANIA', 'SMIERC', 'ZAGINIECIE')),
    opis             TEXT,
    losowa_kontrola  BOOLEAN NOT NULL DEFAULT FALSE,
    decyzja          TEXT CHECK (decyzja IN ('POTWIERDZONE', 'ODRZUCONE')),
    status           TEXT NOT NULL DEFAULT 'NOWE',
    utworzono        TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- zgłoszenie musi wskazywać osobę (PESEL) albo budynek
    CONSTRAINT zgloszenie_adresat CHECK (pesel IS NOT NULL OR budynek_id IS NOT NULL)
);

-- ===== P2 – prognozy =====

CREATE TABLE prognoza (
    id           BIGSERIAL PRIMARY KEY,
    utworzono    TIMESTAMPTZ NOT NULL DEFAULT now(),
    zrodlo       TEXT NOT NULL CHECK (zrodlo IN ('ML', 'OSTATNIA_ZATWIERDZONA')),
    roznica_proc NUMERIC,
    status       TEXT NOT NULL DEFAULT 'NOWA'
);

CREATE TABLE prognoza_pozycja (
    id          BIGSERIAL PRIMARY KEY,
    prognoza_id BIGINT NOT NULL REFERENCES prognoza (id) ON DELETE CASCADE,
    poziom      TEXT NOT NULL CHECK (poziom IN ('MIASTO', 'DZIELNICA', 'BUDYNEK')),
    obiekt_id   INTEGER,                       -- id dzielnicy lub budynku; dla miasta puste
    zasob       TEXT NOT NULL CHECK (zasob IN ('ZYWNOSC', 'WODA', 'LEKI', 'HIGIENA', 'SCHRONIENIE')),
    ilosc       NUMERIC NOT NULL
);
CREATE INDEX idx_prognoza_pozycja_prognoza ON prognoza_pozycja (prognoza_id);

-- ===== P3 – plan przydziału =====

CREATE TABLE plan_przydzialu (
    id                 BIGSERIAL PRIMARY KEY,
    prognoza_id        BIGINT REFERENCES prognoza (id),
    krytyczne_pokryte  BOOLEAN NOT NULL DEFAULT FALSE,
    status             TEXT NOT NULL DEFAULT 'NOWY',
    zatwierdzil        TEXT,
    utworzono          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE plan_pozycja (
    id           BIGSERIAL PRIMARY KEY,
    plan_id      BIGINT NOT NULL REFERENCES plan_przydzialu (id) ON DELETE CASCADE,
    budynek_id   INTEGER NOT NULL REFERENCES budynek (id),
    zasob        TEXT NOT NULL CHECK (zasob IN ('ZYWNOSC', 'WODA', 'LEKI', 'HIGIENA', 'SCHRONIENIE')),
    priorytet    TEXT NOT NULL CHECK (priorytet IN ('A', 'B', 'C')),
    przydzielono NUMERIC NOT NULL CHECK (przydzielono >= 0),
    potrzeba     NUMERIC NOT NULL CHECK (potrzeba >= 0)
);
CREATE INDEX idx_plan_pozycja_plan ON plan_pozycja (plan_id);

CREATE TABLE dostawa_bezposrednia (
    id      BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES plan_przydzialu (id) ON DELETE CASCADE,
    pesel   CHAR(11) NOT NULL REFERENCES ewidencja_ludnosci (pesel),
    zasob   TEXT NOT NULL CHECK (zasob IN ('ZYWNOSC', 'WODA', 'LEKI', 'HIGIENA', 'SCHRONIENIE')),
    ilosc   NUMERIC NOT NULL CHECK (ilosc >= 0)
);

CREATE TABLE wniosek_pomoc (
    id        BIGSERIAL PRIMARY KEY,
    plan_id   BIGINT NOT NULL REFERENCES plan_przydzialu (id) ON DELETE CASCADE,
    zasob     TEXT NOT NULL CHECK (zasob IN ('ZYWNOSC', 'WODA', 'LEKI', 'HIGIENA', 'SCHRONIENIE')),
    ilosc     NUMERIC NOT NULL CHECK (ilosc >= 0),   -- ile wnioskujemy
    przyznano NUMERIC CHECK (przyznano >= 0),        -- ile przyznała organizacja
    termin    DATE,
    status    TEXT NOT NULL DEFAULT 'WYSLANY'
);

CREATE TABLE zlecenie_wydania (
    id       BIGSERIAL PRIMARY KEY,
    plan_id  BIGINT NOT NULL REFERENCES plan_przydzialu (id) ON DELETE CASCADE,
    punkt_id INTEGER NOT NULL REFERENCES punkt_wydawania (id),
    zasob    TEXT NOT NULL CHECK (zasob IN ('ZYWNOSC', 'WODA', 'LEKI', 'HIGIENA', 'SCHRONIENIE')),
    ilosc    NUMERIC NOT NULL CHECK (ilosc >= 0)
);

-- ===== Audyt – „pełna rejestrowalność decyzji” =====

CREATE TABLE audyt (
    id        BIGSERIAL PRIMARY KEY,
    czas      TIMESTAMPTZ NOT NULL DEFAULT now(),
    aktor     TEXT NOT NULL,        -- np. operator, zarzad, analityk, system
    proces    TEXT NOT NULL,        -- np. P1b
    akcja     TEXT NOT NULL,
    szczegoly JSONB
);
CREATE INDEX idx_audyt_czas ON audyt (czas DESC);
