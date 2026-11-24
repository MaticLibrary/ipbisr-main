# System inteligentnego przydzielania zasobów niezbędnych do życia mieszkańcom miasta podczas wojny

Projekt semestralny – Inżynieria procesów biznesowych i systemów regułowych (ANS w Tarnowie).
Autorzy: Ilia Smakovskyi, Robert Kłusek. Prowadzący: Tomasz Potempa.

## Struktura repozytorium

| Folder | Zawartość |
|---|---|
| `bpmn/modele-etap2/` | zatwierdzone modele BPMN z etapu 2 (dokumentacja – nie zmieniamy) |
| `bpmn/wykonywalne/` | modele rozdzielone na osobne procesy, z ustawieniami Camunda 8 |
| `dmn/` | tabele decyzyjne DMN |
| `backend/` | Java 21 + Spring Boot 4 – job workery Camunda, REST API (port **8081**) |
| `ml-service/` | Python + FastAPI – prognoza ML i optymalizacja przydziału (port **8000**) |
| `frontend/` | Next.js + TypeScript + Material UI – panel (port **3000**) |
| `data/` | dane syntetyczne do trenowania i testów |
| `docs/` | dokumentacja projektu (PDF) |
| `docker-compose.yml` | PostgreSQL + backend + serwis ML (+ lokalna Camunda 8.9 w profilu `camunda`) |

## Wymagania

- Docker Desktop (z `docker compose`)
- Node.js 20+ (frontend)
- Opcjonalnie do pracy bez Dockera: Java 21, Python 3.12+
- Camunda 8 Run (lokalnie) albo dostęp do serwera uczelnianego Camunda 8

## Uruchomienie

### 1. Konfiguracja

```bash
cp .env.example .env      # uzupełnij hasła; .env nie trafia do gita
```

### 2. Postgres + backend + serwis ML (Docker)

```bash
docker compose up -d --build
curl http://localhost:8081/health   # {"status":"UP","service":"backend"}
curl http://localhost:8000/health   # {"status":"UP","service":"ml-service"}
```

Zatrzymanie: `docker compose down` (dane bazy zostają w wolumenie `pgdata`; `down -v` je usuwa).

### 3. Frontend

```bash
cd frontend
cp .env.example .env.local
npm ci
npm run dev               # http://localhost:3000
```

Panel pokazuje stan backendu i serwisu ML oraz ekrany zgłoszeń, budynków, prognozy,
planu, magazynu, procesów i audytu. Wybór roli zmienia menu demonstracyjne; nie
zastępuje logowania ani kontroli dostępu. Zadania użytkowników nadal obsługuje
Camunda Tasklist. Opis ekranów, integracji API, konfiguracji i ograniczeń do
wykorzystania w rozdziale 3 znajduje się w [`docs/FRONTEND.md`](docs/FRONTEND.md).

Do pracy bez backendu można jawnie włączyć syntetyczne dane demonstracyjne:
ustaw `NEXT_PUBLIC_DEMO_DATA=true` w `frontend/.env.local` przed uruchomieniem frontendu. Panel oznaczy
wtedy dane etykietą „Dane demo”. Adresy frontendowe można zmienić w `frontend/.env.local`:
`NEXT_PUBLIC_BACKEND_URL`, `NEXT_PUBLIC_CAMUNDA_OPERATE_URL` i
`NEXT_PUBLIC_CAMUNDA_TASKLIST_URL`.

Testy logiki frontendowej:

```bash
cd frontend
npm test
```

### 4. Camunda 8.9

**Lokalnie (Docker, ta sama linia wersji co serwer uczelniany):**

```bash
docker compose --profile camunda up -d camunda
```

Operate i Tasklist: http://localhost:8080 (login `demo` / `demo`), gRPC Zeebe: `localhost:26500`.
Dlatego backend działa na porcie **8081**. Dane Operate są w pamięci (H2) – po restarcie kontenera historia znika,
a backend wdraża modele ponownie przy starcie.

**Camunda musi działać przed startem backendu** (lokalna albo uczelniana) – backend wdraża do niej modele przy starcie.
Dlatego `docker compose up` bez profilu `camunda` wymaga, żeby Camunda była już dostępna pod adresem z `.env`.

Backend przy starcie **sam wdraża** wszystko z `bpmn/wykonywalne/` (`*.bpmn`, `forms/*.form`) i `dmn/` (`*.dmn`).
Po uruchomieniu backendu procesy P1a, P1b, P1c są widoczne w Operate (zakładka *Processes*).

**Serwer uczelniany:** używaj połączenia REST; port gRPC `26500` nie jest potrzebny poza siecią uczelni/VPN. Adres i dane logowania wpisz do `.env` (nigdy do repozytorium):

```
CAMUNDA_CLIENT_MODE=self-managed
CAMUNDA_CLIENT_PREFER_REST_OVER_GRPC=true
CAMUNDA_CLIENT_REST_ADDRESS=http://<adres-serwera>:8080
CAMUNDA_CLIENT_AUTH_METHOD=basic
CAMUNDA_CLIENT_AUTH_USERNAME=2026_nazwisko_imie
CAMUNDA_CLIENT_AUTH_PASSWORD=<numer albumu>
```

### 5. Backend lokalnie z danymi z CSV (profil `dev`)

```bash
docker compose up -d postgres
cd backend
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run   # PowerShell: $env:SPRING_PROFILES_ACTIVE="dev"
```

Profil `dev` wczytuje pliki CSV z `data/` do bazy (kontrakt kolumn: `data/README.md`). Brakujące pliki są pomijane,
tabele, które mają już dane, nie są wczytywane drugi raz. Schemat bazy tworzy Flyway (`backend/src/main/resources/db/migration`).

### Testy backendu

```bash
cd backend
./mvnw test    # testy bazy używają Testcontainers – Docker musi działać
```

## Uruchomienie bez Dockera (dla deweloperów)

```bash
# backend (Java 21)
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
./mvnw test

# serwis ML (Python)
cd ml-service
python -m venv .venv
.venv/Scripts/activate          # Linux/macOS: source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
pytest
```
