"use client";

import { FormEvent, useCallback, useState } from "react";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TextField from "@mui/material/TextField";
import Chip from "@mui/material/Chip";
import Typography from "@mui/material/Typography";
import { createReport, getBuildings, getReports, validateReportInput } from "@/lib/api";
import type { CreateReportInput } from "@/lib/api";
import type { IncidentType, ReportStatus } from "@/lib/types";
import { ErrorState, LoadingState, PageHeader, Panel, EmptyState } from "@/components/ui";
import { useResource } from "@/components/use-resource";

const incidentTypes: { value: IncidentType; label: string }[] = [
  { value: "CHOROBA", label: "Choroba" },
  { value: "NIEPELNOSPRAWNOSC", label: "Niepełnosprawność" },
  { value: "UTRATA_MIESZKANIA", label: "Utrata mieszkania" },
  { value: "SMIERC", label: "Śmierć" },
  { value: "ZAGINIECIE", label: "Zaginięcie" },
];

function statusColor(status: ReportStatus): "success" | "warning" | "error" | "default" {
  if (status === "ZAKONCZONE") return "success";
  if (status === "PRZEKAZANE") return "warning";
  if (status === "BLAD_PROCESU") return "error";
  return "default";
}

function maskPesel(pesel?: string | null): string {
  return pesel ? `••••••••${pesel.slice(-3)}` : "—";
}

export default function ReportsPage() {
  const loadReports = useCallback(() => getReports(), []);
  const loadBuildings = useCallback(() => getBuildings(), []);
  const reports = useResource(loadReports, 5000);
  const buildings = useResource(loadBuildings);
  const [pesel, setPesel] = useState("");
  const [buildingId, setBuildingId] = useState("");
  const [kind, setKind] = useState<IncidentType>("CHOROBA");
  const [description, setDescription] = useState("");
  const [formError, setFormError] = useState("");
  const [success, setSuccess] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError("");
    setSuccess("");

    const input: CreateReportInput = {
      pesel: pesel.trim() || undefined,
      budynekId: buildingId ? Number(buildingId) : undefined,
      rodzaj: kind,
      opis: description.trim() || undefined,
    };
    const validationError = validateReportInput(input);
    if (validationError) {
      setFormError(validationError);
      return;
    }

    setSubmitting(true);
    try {
      const created = await createReport(input);
      setSuccess(`Zgłoszenie ${created.id} zostało przyjęte.`);
      setPesel("");
      setBuildingId("");
      setDescription("");
      await reports.reload();
    } catch (error: unknown) {
      setFormError(
        error instanceof Error
          ? error.message
          : "Nie udało się wysłać zgłoszenia.",
      );
    } finally {
      setSubmitting(false);
    }
  }

  const listedBuildings = buildings.data ?? [];
  const needsPesel = kind !== "UTRATA_MIESZKANIA";

  return (
    <Box>
      <PageHeader
        title="Zgłoszenia"
        description="Zarejestruj zdarzenie zgłoszone przez operatora punktu i śledź jego status."
      />
      <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", lg: "minmax(320px, 0.8fr) minmax(0, 1.2fr)" }, gap: 2.5 }}>
        <Panel title="Nowe zgłoszenie">
          <Stack component="form" spacing={2} onSubmit={submit}>
            <TextField
              select
              label="Rodzaj zdarzenia"
              value={kind}
              onChange={(event) => setKind(event.target.value as IncidentType)}
              fullWidth
            >
              {incidentTypes.map((option) => (
                <MenuItem key={option.value} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label={needsPesel ? "PESEL" : "PESEL (opcjonalnie)"}
              value={pesel}
              onChange={(event) => setPesel(event.target.value)}
              slotProps={{ htmlInput: { inputMode: "numeric", maxLength: 11 } }}
              helperText={needsPesel ? "Wymagany dla wybranego rodzaju zdarzenia." : "Możesz wskazać osobę lub cały budynek."}
            />
            <TextField
              select
              label="Budynek"
              value={buildingId}
              onChange={(event) => setBuildingId(event.target.value)}
              fullWidth
            >
              <MenuItem value="">Nie wskazano</MenuItem>
              {listedBuildings.map((building) => (
                <MenuItem key={building.id} value={building.id}>
                  {building.adres} · {building.dzielnica}
                </MenuItem>
              ))}
            </TextField>
            {buildings.error && <ErrorState message={buildings.error.message} />}
            <TextField
              label="Opis (opcjonalnie)"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              multiline
              minRows={3}
              fullWidth
            />
            {formError && <Alert severity="error">{formError}</Alert>}
            {success && <Alert severity="success">{success}</Alert>}
            <Button type="submit" variant="contained" disabled={submitting}>
              {submitting ? "Wysyłanie…" : "Wyślij zgłoszenie"}
            </Button>
            <Typography variant="caption" color="text.secondary">
              Śmierć i zaginięcie mogą wymagać weryfikacji zarządu w Camunda Tasklist.
            </Typography>
          </Stack>
        </Panel>

        <Panel title="Ostatnie zgłoszenia" action={<Chip size="small" label="Odświeżanie co 5 s" variant="outlined" />}>
          {reports.loading && !reports.data ? (
            <LoadingState />
          ) : reports.error ? (
            <ErrorState message={reports.error.message} />
          ) : reports.data?.length ? (
            <TableContainer component={Paper} variant="outlined">
              <Table size="small" aria-label="Lista ostatnich zgłoszeń">
                <TableHead>
                  <TableRow>
                    <TableCell>ID</TableCell>
                    <TableCell>Rodzaj</TableCell>
                    <TableCell>Identyfikator</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Decyzja</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {reports.data.map((report) => (
                    <TableRow key={report.id}>
                      <TableCell>{report.id}</TableCell>
                      <TableCell>{incidentTypes.find((item) => item.value === report.rodzaj)?.label ?? report.rodzaj}</TableCell>
                      <TableCell>
                        {report.pesel ? maskPesel(report.pesel) : `Budynek ${report.budynekId ?? "—"}`}
                      </TableCell>
                      <TableCell>
                        <Chip size="small" color={statusColor(report.status)} label={report.status} />
                      </TableCell>
                      <TableCell>{report.decyzja ?? "—"}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <EmptyState message="Nie ma jeszcze zgłoszeń." />
          )}
        </Panel>
      </Box>
    </Box>
  );
}
