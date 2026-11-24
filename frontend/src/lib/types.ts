export type Priority = "A" | "B" | "C" | "WYKLUCZONY";

export type Resident = {
  pesel: string;
  imie: string;
  nazwisko: string;
  wiek: number;
  budynekId: number;
  priorytet?: Priority | null;
  poziomPotrzeb?: string | null;
  dostawaBezposrednia: boolean;
  status: string;
};

export type Building = {
  id: number;
  adres: string;
  dzielnicaId: number;
  dzielnica: string;
  priorytet?: Priority | null;
  liczbaMieszkancow: number;
};

export type BuildingDetails = {
  id: number;
  adres: string;
  dzielnicaId: number;
  dzielnica: string;
  mieszkancy: Resident[];
};

export type IncidentType =
  | "CHOROBA"
  | "NIEPELNOSPRAWNOSC"
  | "UTRATA_MIESZKANIA"
  | "SMIERC"
  | "ZAGINIECIE";

export type ReportStatus =
  | "NOWE"
  | "PRZEKAZANE"
  | "ZAKONCZONE"
  | "BLAD_PROCESU";

export type Report = {
  id: number;
  pesel?: string | null;
  budynekId?: number | null;
  rodzaj: IncidentType;
  opis?: string | null;
  losowaKontrola?: boolean;
  decyzja?: "POTWIERDZONE" | "ODRZUCONE" | null;
  status: ReportStatus;
  utworzono: string;
};

export type ForecastPosition = {
  poziom: "MIASTO" | "DZIELNICA" | "BUDYNEK";
  obiektId?: number | null;
  zasob: string;
  ilosc: number;
};

export type Forecast = {
  id: number;
  utworzono: string;
  zrodlo: string;
  roznicaProc?: number | null;
  status: string;
  pozycje: ForecastPosition[];
};

export type ForecastHistoryItem = Omit<Forecast, "pozycje">;

export type UsageHistoryItem = {
  data: string;
  ilosc: number;
};

export type PlanPosition = {
  budynekId: number;
  zasob: string;
  priorytet: Exclude<Priority, "WYKLUCZONY">;
  przydzielono: number;
  potrzeba: number;
};

export type Plan = {
  id: number;
  prognozaId: number;
  krytycznePokryte: boolean;
  status: string;
  zatwierdzil?: string | null;
  pozycje: PlanPosition[];
  niedobory: { zasob: string; ilosc: number }[];
  dostawyBezposrednie: { pesel: string; zasob: string; ilosc: number }[];
  harmonogram: { punktId: number; okno: string; budynekId: number }[];
};

export type InventoryItem = {
  zasob: string;
  ilosc: number;
  jednostka: string;
};

export type ProcessInstance = {
  instancjaId: string;
  proces: string;
  stan: "ACTIVE" | "COMPLETED" | "TERMINATED";
  start: string;
  operateUrl: string;
};

export type AuditEntry = {
  czas: string;
  aktor: string;
  proces: string;
  akcja: string;
  szczegoly: Record<string, unknown>;
};

export type SimulationResult = {
  idZdarzenia: string;
};
