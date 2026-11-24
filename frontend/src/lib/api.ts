import {
  demoAudit,
  demoBuildings,
  demoForecast,
  demoForecastHistory,
  demoInventory,
  demoPlan,
  demoProcesses,
  demoReports,
  demoUsageHistory,
  getDemoBuilding,
  listDemoBuildings,
  saveDemoInventory,
} from "./demo-data.ts";
import type {
  AuditEntry,
  Building,
  BuildingDetails,
  Forecast,
  ForecastHistoryItem,
  ForecastPosition,
  IncidentType,
  InventoryItem,
  Plan,
  ProcessInstance,
  Report,
  SimulationResult,
  UsageHistoryItem,
} from "./types";

export class ApiError extends Error {
  readonly status: number;

  constructor(
    message: string,
    status: number,
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

export function getApiErrorMessage(payload: unknown, fallback: string): string {
  if (
    typeof payload === "object" &&
    payload !== null &&
    "blad" in payload &&
    typeof payload.blad === "string" &&
    payload.blad.trim().length > 0
  ) {
    return payload.blad;
  }

  return fallback;
}

const backendUrl = process.env.NEXT_PUBLIC_BACKEND_URL ?? "http://localhost:8081";
const operateUrl =
  process.env.NEXT_PUBLIC_CAMUNDA_OPERATE_URL ?? "http://localhost:8080";
const demoDataEnabled = process.env.NEXT_PUBLIC_DEMO_DATA === "true";

export function isDemoDataEnabled(): boolean {
  return demoDataEnabled;
}

export async function apiRequest<T>(
  path: string,
  init?: RequestInit,
): Promise<T> {
  const headers = new Headers(init?.headers);
  if (init?.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${backendUrl}${path}`, {
    ...init,
    headers,
    signal: init?.signal
      ? AbortSignal.any([init.signal, AbortSignal.timeout(10_000)])
      : AbortSignal.timeout(10_000),
  });

  const responseText = await response.text();
  if (response.ok && !responseText) {
    throw new ApiError("Serwer zwrócił pustą odpowiedź.", response.status);
  }

  let payload: unknown = responseText;
  if (responseText) {
    try {
      payload = JSON.parse(responseText);
    } catch {
      if (response.ok) {
        throw new ApiError("Serwer zwrócił nieprawidłową odpowiedź.", response.status);
      }
    }
  }

  if (!response.ok) {
    throw new ApiError(
      getApiErrorMessage(payload, `Błąd serwera (${response.status})`),
      response.status,
    );
  }

  return payload as T;
}

function demoRequest<T>(value: T): T {
  if (!demoDataEnabled) {
    throw new Error("Tryb demo nie jest włączony.");
  }
  return value;
}

export async function getReports(): Promise<Report[]> {
  if (demoDataEnabled) return demoRequest([...demoReports].reverse());
  return apiRequest<Report[]>("/api/zgloszenia");
}

export type CreateReportInput = {
  pesel?: string;
  budynekId?: number;
  rodzaj: IncidentType;
  opis?: string;
};

export function validateReportInput(input: CreateReportInput): string | null {
  const hasPesel = Boolean(input.pesel?.trim());
  const hasBuilding = input.budynekId !== undefined;

  if (!hasPesel && !hasBuilding) {
    return "Podaj PESEL albo ID budynku.";
  }
  if (!hasPesel && input.rodzaj !== "UTRATA_MIESZKANIA") {
    return "Dla tego rodzaju zgłoszenia wymagany jest PESEL.";
  }
  return null;
}

export async function createReport(input: CreateReportInput): Promise<Report> {
  const validationError = validateReportInput(input);
  if (validationError) throw new ApiError(validationError, 400);

  if (demoDataEnabled) {
    const report: Report = {
      id: Math.max(0, ...demoReports.map((item) => item.id)) + 1,
      ...input,
      status: "NOWE",
      utworzono: new Date().toISOString(),
    };
    demoReports.push(report);
    return demoRequest(report);
  }
  return apiRequest<Report>("/api/zgloszenia", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export async function getBuildings(districtId?: number): Promise<Building[]> {
  if (demoDataEnabled) {
    const buildings = listDemoBuildings();
    return demoRequest(
      districtId === undefined
        ? buildings
        : buildings.filter((building) => building.dzielnicaId === districtId),
    );
  }
  const query = districtId === undefined ? "" : `?dzielnica=${districtId}`;
  return apiRequest<Building[]>(`/api/budynki${query}`);
}

export function filterBuildingsByDistrict(
  buildings: Building[],
  districtId?: number,
): Building[] {
  return districtId === undefined
    ? buildings
    : buildings.filter((building) => building.dzielnicaId === districtId);
}

export async function getBuilding(id: number): Promise<BuildingDetails> {
  if (demoDataEnabled) {
    const building = getDemoBuilding(id);
    if (!building) throw new ApiError("Nie znaleziono budynku.", 404);
    return demoRequest(building);
  }
  return apiRequest<BuildingDetails>(`/api/budynki/${id}`);
}

export async function getLastForecast(): Promise<Forecast> {
  if (demoDataEnabled) return demoRequest(demoForecast);
  return apiRequest<Forecast>("/api/prognozy/ostatnia");
}

export function selectForecastPosition(
  positions: ForecastPosition[],
  resource: string,
  districtId?: number,
): ForecastPosition | undefined {
  const level = districtId === undefined ? "MIASTO" : "DZIELNICA";
  return positions.find(
    (position) =>
      position.poziom === level &&
      position.zasob === resource &&
      (districtId === undefined
        ? position.obiektId == null
        : position.obiektId === districtId),
  );
}

export async function getForecastHistory(): Promise<ForecastHistoryItem[]> {
  if (demoDataEnabled) return demoRequest(demoForecastHistory);
  return apiRequest<ForecastHistoryItem[]>("/api/prognozy");
}

export async function getUsageHistory(
  resource: string,
  districtId?: number,
): Promise<UsageHistoryItem[]> {
  if (demoDataEnabled) {
    const resourceScale: Record<string, number> = {
      WODA: 1,
      ZYWNOSC: 0.62,
      LEKI: 0.18,
      HIGIENA: 0.36,
      SCHRONIENIE: 0.08,
    };
    const scale = resourceScale[resource];
    if (scale === undefined) {
      throw new ApiError("Nieznany rodzaj zasobu.", 400);
    }

    const districtScale =
      districtId === undefined ? 1 : districtId === 1 ? 0.58 : districtId === 2 ? 0.42 : null;
    if (districtScale === null) return demoRequest([]);

    return demoRequest(
      demoUsageHistory.map((item) => ({
        ...item,
        ilosc: Math.round(item.ilosc * scale * districtScale),
      })),
    );
  }
  const query = new URLSearchParams({ zasob: resource });
  if (districtId !== undefined) query.set("dzielnica", String(districtId));
  return apiRequest<UsageHistoryItem[]>(`/api/zuzycie/historia?${query}`);
}

export async function getLastPlan(): Promise<Plan> {
  if (demoDataEnabled) return demoRequest(demoPlan);
  return apiRequest<Plan>("/api/plany/ostatni");
}

export async function getInventory(): Promise<InventoryItem[]> {
  if (demoDataEnabled) return demoRequest(demoInventory);
  return apiRequest<InventoryItem[]>("/api/magazyn");
}

export async function updateInventory(
  items: InventoryItem[],
): Promise<InventoryItem[]> {
  if (!demoDataEnabled) {
    throw new ApiError("Edycja magazynu jest dostępna tylko w trybie demo.", 403);
  }
  return demoRequest(saveDemoInventory(items));
}

export async function getProcesses(): Promise<ProcessInstance[]> {
  if (demoDataEnabled) return demoRequest([...demoProcesses]);
  return apiRequest<ProcessInstance[]>("/api/procesy");
}

export async function simulateProcess(
  kind: "situation" | "replan",
  threatLevel: number,
): Promise<SimulationResult> {
  if (demoDataEnabled) {
    const idZdarzenia = crypto.randomUUID();
    const processName =
      kind === "situation" ? "Proces_Prognozowanie_zapotrzebowania" : "Proces_Przydzial_zasobow";
    demoProcesses.unshift({
      instancjaId: `demo-${idZdarzenia}`,
      proces: processName,
      stan: "ACTIVE",
      start: new Date().toISOString(),
      operateUrl: `${operateUrl}/operate/processes/demo-${idZdarzenia}`,
    });
    demoAudit.unshift({
      czas: new Date().toISOString(),
      aktor: "analityk",
      proces: kind === "situation" ? "P2" : "P3",
      akcja:
        kind === "situation"
          ? "SYMULACJA_ZMIANY_SYTUACJI"
          : "SYMULACJA_PONOWNEGO_PLANOWANIA",
      szczegoly: { poziomZagrozenia: threatLevel, idZdarzenia },
    });
    return demoRequest({ idZdarzenia });
  }

  const endpoint =
    kind === "situation"
      ? "/api/symulacja/zmiana-sytuacji"
      : "/api/symulacja/ponowne-planowanie";
  return apiRequest<SimulationResult>(endpoint, {
    method: "POST",
    body: JSON.stringify({ poziomZagrozenia: threatLevel }),
  });
}

export async function getAudit(limit = 100): Promise<AuditEntry[]> {
  if (demoDataEnabled) return demoRequest(demoAudit.slice(0, limit));
  return apiRequest<AuditEntry[]>(`/api/audyt?limit=${limit}`);
}
