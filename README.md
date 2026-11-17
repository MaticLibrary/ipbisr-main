# System inteligentnego przydzielania zasobów niezbędnych do życia mieszkańcom miasta podczas wojny

Projekt semestralny – Inżynieria procesów biznesowych i systemów regułowych (ANS w Tarnowie).
Autorzy: Ilia Smakovskyi, Robert Kłusek. Prowadzący: Tomasz Potempa.

## Struktura repozytorium

| Folder | Zawartość |
|---|---|
| `bpmn/modele-etap2/` | zatwierdzone modele BPMN z etapu 2 (dokumentacja – nie zmieniamy) |
| `bpmn/wykonywalne/` | modele rozdzielone na osobne procesy, z ustawieniami Camunda 8 |
| `dmn/` | tabele decyzyjne DMN |
| `backend/` | Java 21 + Spring Boot 3 – job workery Camunda, REST API (port **8081**) |
| `ml-service/` | Python + FastAPI – prognoza ML i optymalizacja przydziału (port **8000**) |
| `frontend/` | Next.js + TypeScript + Material UI – panel (port **3000**) |
| `data/` | dane syntetyczne do trenowania i testów |
| `docs/` | dokumentacja projektu (PDF) |
| `docker-compose.yml` | PostgreSQL + backend + serwis ML |

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
npm install
npm run dev               # http://localhost:3000
```

Strona startowa pokazuje, czy backend i serwis ML odpowiadają na `/health`.

### 4. Camunda 8

Lokalnie: pobierz **Camunda 8 Run** (https://docs.camunda.io/docs/self-managed/quickstart/developer-quickstart/c8run/) i uruchom `./start.sh` (Linux/macOS) lub `.\c8run.exe start` (Windows).
Operate i Tasklist: http://localhost:8080 (login `demo` / `demo`), gRPC Zeebe: `localhost:26500`.
Dlatego backend działa na porcie **8081**.

Serwer uczelniany: wpisz Client ID / Secret do `.env` (nigdy do repozytorium).

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
