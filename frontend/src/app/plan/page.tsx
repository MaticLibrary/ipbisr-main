"use client";

import { useCallback } from "react";
import Box from "@mui/material/Box";
import Chip from "@mui/material/Chip";
import Paper from "@mui/material/Paper";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import Typography from "@mui/material/Typography";
import { getLastPlan } from "@/lib/api";
import { useResource } from "@/components/use-resource";
import { EmptyState, ErrorState, LoadingState, PageHeader, Panel } from "@/components/ui";

export default function PlanPage() {
  const loadPlan = useCallback(() => getLastPlan(), []);
  const plan = useResource(loadPlan);

  return (
    <Box>
      <PageHeader
        title="Plan przydziału"
        description="Zobacz zaplanowane dostawy, pokrycie potrzeb i niedobory zasobów."
      />
      {plan.loading && !plan.data ? (
        <Panel><LoadingState /></Panel>
      ) : plan.error?.status === 404 ? (
        <Panel><EmptyState message="Brak planu przydziału. Uruchom proces P3, aby wygenerować plan." /></Panel>
      ) : plan.error ? (
        <ErrorState message={plan.error.message} />
      ) : plan.data ? (
        <Box>
          <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", md: "repeat(3, minmax(0, 1fr))" }, gap: 2, mb: 2.5 }}>
            <Panel title="Status">
              <Chip
                color={plan.data.status === "ZATWIERDZONY" ? "success" : plan.data.status === "ODRZUCONY" ? "error" : "warning"}
                label={plan.data.status}
              />
            </Panel>
            <Panel title="Potrzeby krytyczne">
              <Typography variant="h6">
                {plan.data.krytycznePokryte ? "Pokryte" : "Wymagają uwagi"}
              </Typography>
            </Panel>
            <Panel title="Plan / prognoza">
              <Typography variant="h6">#{plan.data.id} / #{plan.data.prognozaId}</Typography>
              <Typography variant="body2" color="text.secondary">
                Zatwierdził: {plan.data.zatwierdzil ?? "oczekuje na decyzję"}
              </Typography>
            </Panel>
          </Box>

          <Panel title="Pozycje planu">
            {plan.data.pozycje.length ? (
              <TableContainer component={Paper} variant="outlined">
                <Table size="small" aria-label="Pozycje planu przydziału">
                  <TableHead>
                    <TableRow>
                      <TableCell>Budynek</TableCell>
                      <TableCell>Zasób</TableCell>
                      <TableCell>Priorytet</TableCell>
                      <TableCell align="right">Potrzeba</TableCell>
                      <TableCell align="right">Przydzielono</TableCell>
                      <TableCell align="right">Pokrycie</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {plan.data.pozycje.map((position, index) => (
                      <TableRow key={`${position.budynekId}-${position.zasob}-${index}`}>
                        <TableCell>{position.budynekId}</TableCell>
                        <TableCell>{position.zasob}</TableCell>
                        <TableCell><Chip size="small" label={position.priorytet} /></TableCell>
                        <TableCell align="right">{position.potrzeba.toLocaleString("pl-PL")}</TableCell>
                        <TableCell align="right">{position.przydzielono.toLocaleString("pl-PL")}</TableCell>
                        <TableCell align="right">
                          {position.potrzeba === 0
                            ? "—"
                            : `${Math.min(100, Math.round((position.przydzielono / position.potrzeba) * 100))}%`}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : (
              <EmptyState message="Plan nie zawiera pozycji." />
            )}
          </Panel>

          <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", lg: "1fr 1fr" }, gap: 2.5, mt: 2.5 }}>
            <Panel title="Niedobory">
              {plan.data.niedobory.length ? (
                <Table size="small" aria-label="Niedobory zasobów">
                  <TableHead>
                    <TableRow><TableCell>Zasób</TableCell><TableCell align="right">Brakująca ilość</TableCell></TableRow>
                  </TableHead>
                  <TableBody>
                    {plan.data.niedobory.map((item) => (
                      <TableRow key={item.zasob}>
                        <TableCell>{item.zasob}</TableCell>
                        <TableCell align="right">{item.ilosc.toLocaleString("pl-PL")}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              ) : (
                <EmptyState message="Plan nie wykazuje niedoborów." />
              )}
            </Panel>

            <Panel title="Dostawy bezpośrednie">
              {plan.data.dostawyBezposrednie.length ? (
                <Table size="small" aria-label="Dostawy bezpośrednie">
                  <TableHead>
                    <TableRow><TableCell>PESEL</TableCell><TableCell>Zasób</TableCell><TableCell align="right">Ilość</TableCell></TableRow>
                  </TableHead>
                  <TableBody>
                    {plan.data.dostawyBezposrednie.map((item, index) => (
                      <TableRow key={`${item.pesel}-${item.zasob}-${index}`}>
                        <TableCell>{`••••••••${item.pesel.slice(-3)}`}</TableCell>
                        <TableCell>{item.zasob}</TableCell>
                        <TableCell align="right">{item.ilosc.toLocaleString("pl-PL")}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              ) : (
                <EmptyState message="Brak dostaw bezpośrednich." />
              )}
            </Panel>
          </Box>

          <Panel title="Harmonogram" >
            {plan.data.harmonogram.length ? (
              <Table size="small" aria-label="Harmonogram dostaw">
                <TableHead>
                  <TableRow>
                    <TableCell>Punkt dystrybucji</TableCell>
                    <TableCell>Okno czasowe</TableCell>
                    <TableCell>Budynek</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {plan.data.harmonogram.map((item, index) => (
                    <TableRow key={`${item.punktId}-${item.budynekId}-${index}`}>
                      <TableCell>{item.punktId}</TableCell>
                      <TableCell>{item.okno}</TableCell>
                      <TableCell>{item.budynekId}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            ) : (
              <EmptyState message="Brak wpisów w harmonogramie." />
            )}
          </Panel>
          <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 1.5 }}>
            Zatwierdzanie planu odbywa się w Camunda Tasklist.
          </Typography>
        </Box>
      ) : (
        <EmptyState message="Brak danych planu." />
      )}
    </Box>
  );
}
