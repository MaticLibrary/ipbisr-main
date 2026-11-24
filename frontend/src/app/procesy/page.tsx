"use client";

import { FormEvent, useCallback, useState } from "react";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { getProcesses, simulateProcess } from "@/lib/api";
import type { ProcessInstance } from "@/lib/types";
import { EmptyState, ErrorState, formatDate, LoadingState, PageHeader, Panel } from "@/components/ui";
import { useResource } from "@/components/use-resource";

function processStatusColor(status: ProcessInstance["stan"]): "success" | "warning" | "error" {
  if (status === "ACTIVE") return "warning";
  if (status === "COMPLETED") return "success";
  return "error";
}

export default function ProcessesPage() {
  const loadProcesses = useCallback(() => getProcesses(), []);
  const processes = useResource(loadProcesses, 10000);
  const [threatLevel, setThreatLevel] = useState(0);
  const [sending, setSending] = useState<"situation" | "replan" | null>(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  async function sendSimulation(kind: "situation" | "replan") {
    setSending(kind);
    setMessage("");
    setError("");
    try {
      const result = await simulateProcess(kind, threatLevel);
      setMessage(`Wysłano zdarzenie. ID: ${result.idZdarzenia}`);
      await processes.reload();
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : "Nie udało się wysłać symulacji.");
    } finally {
      setSending(null);
    }
  }

  function submitSimulation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void sendSimulation("situation");
  }

  return (
    <Box>
      <PageHeader
        title="Procesy"
        description="Monitoruj instancje Camunda i uruchamiaj zdarzenia demonstracyjne."
      />
      <Panel title="Symulacja zdarzenia">
        <Stack
          component="form"
          direction={{ xs: "column", md: "row" }}
          spacing={1.5}
          sx={{ alignItems: { xs: "stretch", md: "center" } }}
          onSubmit={submitSimulation}
        >
          <TextField
            select
            label="Poziom zagrożenia"
            value={threatLevel}
            onChange={(event) => setThreatLevel(Number(event.target.value))}
            sx={{ minWidth: 200 }}
          >
            {[0, 1, 2, 3].map((level) => (
              <MenuItem key={level} value={level}>{level}</MenuItem>
            ))}
          </TextField>
          <Button type="submit" variant="contained" disabled={sending !== null}>
            {sending === "situation" ? "Wysyłanie…" : "Symuluj zmianę sytuacji"}
          </Button>
          <Button
            type="button"
            variant="outlined"
            onClick={() => void sendSimulation("replan")}
            disabled={sending !== null}
          >
            {sending === "replan" ? "Wysyłanie…" : "Symuluj ponowne planowanie"}
          </Button>
        </Stack>
        {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
        {message && <Alert severity="success" sx={{ mt: 2 }}>{message}</Alert>}
      </Panel>

      <Panel
        title="Ostatnie instancje"
        action={<Chip size="small" variant="outlined" label="Odświeżanie co 10 s" />}
      >
        {processes.loading && !processes.data ? (
          <LoadingState />
        ) : processes.error ? (
          <ErrorState message={processes.error.message} />
        ) : processes.data?.length ? (
          <TableContainer>
            <Table aria-label="Instancje procesów">
              <TableHead>
                <TableRow>
                  <TableCell>Proces</TableCell>
                  <TableCell>Stan</TableCell>
                  <TableCell>Rozpoczęto</TableCell>
                  <TableCell>Instancja</TableCell>
                  <TableCell>Operate</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {processes.data.map((process) => (
                  <TableRow key={process.instancjaId}>
                    <TableCell>{process.proces}</TableCell>
                    <TableCell>
                      <Chip size="small" color={processStatusColor(process.stan)} label={process.stan} />
                    </TableCell>
                    <TableCell>{formatDate(process.start)}</TableCell>
                    <TableCell sx={{ fontFamily: "monospace", wordBreak: "break-all" }}>{process.instancjaId}</TableCell>
                    <TableCell>
                      <Button
                        component="a"
                        href={process.operateUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        disabled={!process.operateUrl}
                        aria-label={`Otwórz instancję ${process.instancjaId} w Operate`}
                      >
                        Otwórz w Operate
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        ) : (
          <EmptyState message="Nie znaleziono uruchomionych procesów." />
        )}
        <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 1.5 }}>
          Zadania wymagające decyzji człowieka są dostępne w Camunda Tasklist.
        </Typography>
      </Panel>
    </Box>
  );
}
