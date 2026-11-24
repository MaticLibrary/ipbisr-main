"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import MenuItem from "@mui/material/MenuItem";
import Paper from "@mui/material/Paper";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { filterBuildingsByDistrict, getBuilding, getBuildings } from "@/lib/api";
import type { Priority } from "@/lib/types";
import { EmptyState, ErrorState, LoadingState, PageHeader, Panel } from "@/components/ui";
import { useResource } from "@/components/use-resource";

function PriorityChip({ priority }: { priority?: Priority | null }) {
  const color =
    priority === "A" ? "error" : priority === "B" ? "warning" : priority === "C" ? "success" : "default";
  return <Chip size="small" color={color} label={priority ?? "Nieustalony"} />;
}

function maskPesel(pesel: string): string {
  return `••••••••${pesel.slice(-3)}`;
}

export default function BuildingsPage() {
  const loadBuildings = useCallback(() => getBuildings(), []);
  const buildings = useResource(loadBuildings);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [districtId, setDistrictId] = useState(0);

  const districts = useMemo(() => {
    const unique = new Map<number, string>();
    for (const building of buildings.data ?? []) {
      unique.set(building.dzielnicaId, building.dzielnica);
    }
    return Array.from(unique, ([id, name]) => ({ id, name }));
  }, [buildings.data]);
  const visibleBuildings = useMemo(
    () => filterBuildingsByDistrict(buildings.data ?? [], districtId || undefined),
    [buildings.data, districtId],
  );

  useEffect(() => {
    if (!visibleBuildings.some((building) => building.id === selectedId)) {
      setSelectedId(visibleBuildings[0]?.id ?? null);
    }
  }, [visibleBuildings, selectedId]);

  const loadSelected = useCallback(
    () => (selectedId === null ? Promise.resolve(null) : getBuilding(selectedId)),
    [selectedId],
  );
  const selected = useResource(loadSelected);

  return (
    <Box>
      <PageHeader
        title="Budynki i mieszkańcy"
        description="Przeglądaj priorytety budynków oraz potrzeby mieszkańców."
      />
      <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", lg: "minmax(0, 1fr) minmax(0, 1.15fr)" }, gap: 2.5 }}>
        <Panel
          title="Budynki"
          action={
            <TextField
              select
              size="small"
              label="Dzielnica"
              value={districtId}
              onChange={(event) => setDistrictId(Number(event.target.value))}
              sx={{ minWidth: 160 }}
            >
              <MenuItem value={0}>Wszystkie dzielnice</MenuItem>
              {districts.map((district) => (
                <MenuItem key={district.id} value={district.id}>
                  {district.name}
                </MenuItem>
              ))}
            </TextField>
          }
        >
          {buildings.loading && !buildings.data ? (
            <LoadingState />
          ) : buildings.error ? (
            <ErrorState message={buildings.error.message} />
          ) : visibleBuildings.length ? (
            <TableContainer component={Paper} variant="outlined">
              <Table size="small" aria-label="Budynki">
                <TableHead>
                  <TableRow>
                    <TableCell>Adres</TableCell>
                    <TableCell>Dzielnica</TableCell>
                    <TableCell>Priorytet</TableCell>
                    <TableCell align="right">Osoby</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {visibleBuildings.map((building) => (
                    <TableRow
                      key={building.id}
                      hover
                      selected={selectedId === building.id}
                    >
                      <TableCell>
                        <Button
                          onClick={() => setSelectedId(building.id)}
                          aria-label={`Pokaż szczegóły budynku ${building.adres}`}
                          aria-current={selectedId === building.id ? "true" : undefined}
                          sx={{ justifyContent: "flex-start", px: 0 }}
                        >
                          {building.adres}
                        </Button>
                      </TableCell>
                      <TableCell>{building.dzielnica}</TableCell>
                      <TableCell><PriorityChip priority={building.priorytet} /></TableCell>
                      <TableCell align="right">{building.liczbaMieszkancow}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <EmptyState
              message={
                districtId === 0
                  ? "Nie znaleziono budynków."
                  : "Nie znaleziono budynków w wybranej dzielnicy."
              }
            />
          )}
        </Panel>

        <Panel title={selected.data ? `${selected.data.adres} · mieszkańcy` : "Szczegóły budynku"}>
          {selectedId === null ? (
            <EmptyState message="Wybierz budynek, aby zobaczyć jego mieszkańców." />
          ) : selected.loading && !selected.data ? (
            <LoadingState />
          ) : selected.error ? (
            <ErrorState message={selected.error.message} />
          ) : selected.data?.mieszkancy.length ? (
            <TableContainer component={Paper} variant="outlined">
              <Table size="small" aria-label="Mieszkańcy wybranego budynku">
                <TableHead>
                  <TableRow>
                    <TableCell>Mieszkaniec</TableCell>
                    <TableCell>Wiek</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Priorytet</TableCell>
                    <TableCell>Potrzeby</TableCell>
                    <TableCell>Dostawa</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {selected.data.mieszkancy.map((resident) => (
                    <TableRow key={resident.pesel}>
                      <TableCell>
                        <Typography variant="body2">
                          {resident.imie} {resident.nazwisko}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {maskPesel(resident.pesel)}
                        </Typography>
                      </TableCell>
                      <TableCell>{resident.wiek}</TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          color={resident.status === "ZYJE" ? "success" : "default"}
                          label={resident.status}
                        />
                      </TableCell>
                      <TableCell><PriorityChip priority={resident.priorytet} /></TableCell>
                      <TableCell>{resident.poziomPotrzeb ?? "—"}</TableCell>
                      <TableCell>{resident.dostawaBezposrednia ? "Bezpośrednia" : "Punkt"}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : selected.data ? (
            <EmptyState message="W tym budynku nie ma mieszkańców do wyświetlenia." />
          ) : (
            <EmptyState message="Wybierz budynek, aby zobaczyć szczegóły." />
          )}
        </Panel>
      </Box>
    </Box>
  );
}
