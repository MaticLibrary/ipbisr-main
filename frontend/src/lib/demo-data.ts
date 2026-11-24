import type {
  AuditEntry,
  Building,
  BuildingDetails,
  Forecast,
  InventoryItem,
  Plan,
  ProcessInstance,
  Report,
  Resident,
  UsageHistoryItem,
} from "./types";

const now = new Date().toISOString();
const operateUrl =
  process.env.NEXT_PUBLIC_CAMUNDA_OPERATE_URL ?? "http://localhost:8080";

type DemoBuilding = Building & { mieszkancy: Resident[] };

export const demoBuildings: DemoBuilding[] = [
  {
    id: 1,
    adres: "ul. Słoneczna 12",
    dzielnicaId: 1,
    dzielnica: "Centrum",
    priorytet: "A",
    liczbaMieszkancow: 2,
    mieszkancy: [
      {
        pesel: "50010112345",
        imie: "Anna",
        nazwisko: "Nowak",
        wiek: 76,
        budynekId: 1,
        priorytet: "A",
        poziomPotrzeb: "PODWYZSZONY",
        dostawaBezposrednia: true,
        status: "ZYJE",
      },
      {
        pesel: "82030456789",
        imie: "Jan",
        nazwisko: "Nowak",
        wiek: 44,
        budynekId: 1,
        priorytet: "B",
        poziomPotrzeb: "STANDARDOWY",
        dostawaBezposrednia: false,
        status: "ZYJE",
      },
    ],
  },
  {
    id: 2,
    adres: "ul. Leśna 4",
    dzielnicaId: 2,
    dzielnica: "Północ",
    priorytet: "B",
    liczbaMieszkancow: 1,
    mieszkancy: [
      {
        pesel: "69081234567",
        imie: "Maria",
        nazwisko: "Kowalska",
        wiek: 57,
        budynekId: 2,
        priorytet: "B",
        poziomPotrzeb: "STANDARDOWY",
        dostawaBezposrednia: false,
        status: "ZYJE",
      },
    ],
  },
  {
    id: 3,
    adres: "ul. Polna 31",
    dzielnicaId: 1,
    dzielnica: "Centrum",
    priorytet: "C",
    liczbaMieszkancow: 0,
    mieszkancy: [],
  },
];

export const demoReports: Report[] = [
  {
    id: 1,
    pesel: "50010112345",
    rodzaj: "CHOROBA",
    opis: "Wymaga pilnego dostarczenia leków.",
    status: "ZAKONCZONE",
    decyzja: "POTWIERDZONE",
    utworzono: now,
  },
];

export const demoForecast: Forecast = {
  id: 3,
  utworzono: now,
  zrodlo: "ML (dane demo)",
  roznicaProc: 12.5,
  status: "ZATWIERDZONA",
  pozycje: [
    { poziom: "MIASTO", zasob: "WODA", ilosc: 5400 },
    { poziom: "MIASTO", zasob: "ZYWNOSC", ilosc: 3200 },
    { poziom: "MIASTO", zasob: "LEKI", ilosc: 450 },
  ],
};

export const demoForecastHistory = [
  { id: 3, utworzono: now, zrodlo: "ML", roznicaProc: 12.5, status: "ZATWIERDZONA" },
  { id: 2, utworzono: "2026-11-23T12:00:00Z", zrodlo: "ML", roznicaProc: 8.2, status: "ZATWIERDZONA" },
  { id: 1, utworzono: "2026-11-22T12:00:00Z", zrodlo: "ML", roznicaProc: null, status: "ZATWIERDZONA" },
];

export const demoUsageHistory: UsageHistoryItem[] = [
  { data: "2026-06-01", ilosc: 4600 },
  { data: "2026-07-01", ilosc: 4750 },
  { data: "2026-08-01", ilosc: 4920 },
  { data: "2026-09-01", ilosc: 4810 },
  { data: "2026-10-01", ilosc: 5100 },
  { data: "2026-11-01", ilosc: 5250 },
];

export const demoPlan: Plan = {
  id: 2,
  prognozaId: 3,
  krytycznePokryte: false,
  status: "DO_ZATWIERDZENIA",
  zatwierdzil: null,
  pozycje: [
    { budynekId: 1, zasob: "WODA", priorytet: "A", przydzielono: 60, potrzeba: 60 },
    { budynekId: 2, zasob: "LEKI", priorytet: "B", przydzielono: 3, potrzeba: 5 },
  ],
  niedobory: [{ zasob: "LEKI", ilosc: 2 }],
  dostawyBezposrednie: [{ pesel: "50010112345", zasob: "LEKI", ilosc: 1 }],
  harmonogram: [{ punktId: 1, okno: "08:00-10:00", budynekId: 1 }],
};

export let demoInventory: InventoryItem[] = [
  { zasob: "WODA", ilosc: 12000, jednostka: "l" },
  { zasob: "ZYWNOSC", ilosc: 4500, jednostka: "kg" },
  { zasob: "LEKI", ilosc: 320, jednostka: "opak." },
  { zasob: "HIGIENA", ilosc: 800, jednostka: "zest." },
  { zasob: "SCHRONIENIE", ilosc: 60, jednostka: "miejsc" },
];

export const demoProcesses: ProcessInstance[] = [
  {
    instancjaId: "demo-p1-001",
    proces: "Proces_Analiza_sytuacji",
    stan: "ACTIVE",
    start: now,
    operateUrl: `${operateUrl}/operate/processes/demo-p1-001`,
  },
  {
    instancjaId: "demo-p3-001",
    proces: "Proces_Przydzial_zasobow",
    stan: "COMPLETED",
    start: "2026-11-23T10:00:00Z",
    operateUrl: `${operateUrl}/operate/processes/demo-p3-001`,
  },
];

export const demoAudit: AuditEntry[] = [
  {
    czas: now,
    aktor: "operator",
    proces: "P1c",
    akcja: "ZAREJESTROWANO",
    szczegoly: { budynekId: 1, rodzaj: "CHOROBA" },
  },
  {
    czas: "2026-11-23T10:05:00Z",
    aktor: "analityk",
    proces: "P3a",
    akcja: "ZATWIERDZONO_PLAN",
    szczegoly: { idPlanu: 2 },
  },
];

export function getDemoBuilding(id: number): BuildingDetails | undefined {
  const building = demoBuildings.find((item) => item.id === id);
  if (!building) return undefined;
  return {
    id: building.id,
    adres: building.adres,
    dzielnicaId: building.dzielnicaId,
    dzielnica: building.dzielnica,
    mieszkancy: building.mieszkancy,
  };
}

export function listDemoBuildings(): Building[] {
  return demoBuildings.map(({ mieszkancy: _mieszkancy, ...building }) => building);
}

export function saveDemoInventory(items: InventoryItem[]): InventoryItem[] {
  const updated = new Map(demoInventory.map((item) => [item.zasob, item]));
  for (const item of items) {
    if (!Number.isFinite(item.ilosc) || item.ilosc < 0 || !item.jednostka.trim()) {
      throw new Error("Ilość nie może być ujemna, a jednostka nie może być pusta.");
    }
    updated.set(item.zasob, item);
  }
  demoInventory = Array.from(updated.values());
  return demoInventory;
}
