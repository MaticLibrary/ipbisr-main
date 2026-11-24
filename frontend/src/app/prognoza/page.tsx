"use client";

import { useCallback, useMemo, useState } from "react";
import Box from "@mui/material/Box";
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
import {
  getBuildings,
  getForecastHistory,
  getLastForecast,
  getUsageHistory,
  selectForecastPosition,
} from "@/lib/api";
import type { UsageHistoryItem } from "@/lib/types";
import { useResource } from "@/components/use-resource";
import { EmptyState, ErrorState, formatDate, LoadingState, PageHeader, Panel } from "@/components/ui";

const resources = [
  { value: "WODA", label: "Woda" },
  { value: "ZYWNOSC", label: "Żywność" },
  { value: "LEKI", label: "Leki" },
  { value: "HIGIENA", label: "Higiena" },
  { value: "SCHRONIENIE", label: "Schronienie" },
];

function HistoryChart({
  values,
  resourceLabel,
  forecastValue,
}: {
  values: UsageHistoryItem[];
  resourceLabel: string;
  forecastValue?: number;
}) {
  if (!values.length) {
    return <EmptyState message="Brak danych historycznych dla wybranych filtrów." />;
  }

  const width = 720;
  const height = 220;
  const padding = 32;
  const maxValue = Math.max(...values.map((item) => item.ilosc));
  const minValue = Math.min(...values.map((item) => item.ilosc));
  const scaleMax = Math.max(maxValue, forecastValue ?? 0, 1);
  const forecastX = width - padding;
  const historyEndX = forecastValue === undefined ? forecastX : forecastX - 72;
  const points = values.map((item, index) => {
    const x =
      values.length === 1
        ? forecastValue === undefined
          ? width / 2
          : historyEndX
        : padding + (index * (historyEndX - padding)) / (values.length - 1);
    const y = height - padding - (item.ilosc / scaleMax) * (height - padding * 2);
    return { ...item, x, y };
  });
  const forecastY =
    forecastValue === undefined
      ? undefined
      : height - padding - (forecastValue / scaleMax) * (height - padding * 2);

  return (
    <Box>
      <svg
        viewBox={`0 0 ${width} ${height}`}
        role="img"
        aria-label={`Historia zużycia i prognoza: ${resourceLabel}`}
        style={{ display: "block", width: "100%", height: "auto", maxHeight: 260 }}
      >
        <line x1={padding} y1={height - padding} x2={width - padding} y2={height - padding} stroke="#cbd5e1" />
        <line x1={padding} y1={padding} x2={padding} y2={height - padding} stroke="#cbd5e1" />
        <text x={padding - 7} y={padding - 8} textAnchor="end" fill="#526078" fontSize="12">
          {scaleMax.toLocaleString("pl-PL")}
        </text>
        <text x={padding - 7} y={height - padding} textAnchor="end" fill="#526078" fontSize="12">
          0
        </text>
        <polyline
          fill="none"
          stroke="#1d4ed8"
          strokeWidth="3"
          strokeLinejoin="round"
          strokeLinecap="round"
          points={points.map((point) => `${point.x},${point.y}`).join(" ")}
        />
        {forecastValue !== undefined && forecastY !== undefined && points.length > 0 && (
          <>
            <line
              x1={points[points.length - 1].x}
              y1={points[points.length - 1].y}
              x2={forecastX}
              y2={forecastY}
              stroke="#b45309"
              strokeWidth="2"
              strokeDasharray="5 5"
            />
            <circle cx={forecastX} cy={forecastY} r="6" fill="#b45309">
              <title>{`Prognoza: ${forecastValue.toLocaleString("pl-PL")}`}</title>
            </circle>
            <text
              x={forecastX}
              y={height - padding + 22}
              textAnchor="middle"
              fill="#526078"
              fontSize="12"
            >
              Prognoza
            </text>
          </>
        )}
        {points.map((point) => (
          <g key={point.data}>
            <circle cx={point.x} cy={point.y} r="5" fill="#ffffff" stroke="#1d4ed8" strokeWidth="3" />
            <title>{`${point.data}: ${point.ilosc}`}</title>
          </g>
        ))}
      </svg>
      <Box sx={{ display: "flex", justifyContent: "space-between", gap: 1, mt: 1 }}>
        <Typography variant="caption" color="text.secondary">
          Zakres osi: 0–{scaleMax.toLocaleString("pl-PL")}
        </Typography>
        <Typography variant="caption" color="text.secondary">
          Najniższa wartość: {minValue.toLocaleString("pl-PL")}
        </Typography>
      </Box>
      <Box sx={{ display: "flex", gap: 2, mt: 1, flexWrap: "wrap" }}>
        <Typography variant="caption" color="text.secondary">
          <Box component="span" sx={{ color: "#1d4ed8", fontWeight: 700 }}>━</Box> Historia
        </Typography>
        {forecastValue !== undefined && (
          <Typography variant="caption" color="text.secondary">
            <Box component="span" sx={{ color: "#b45309", fontWeight: 700 }}>●</Box> Prognoza
          </Typography>
        )}
      </Box>
      <TableContainer sx={{ mt: 1, maxHeight: 224 }}>
        <Table size="small" aria-label={`Dane historii zużycia: ${resourceLabel}`}>
          <TableHead>
            <TableRow>
              <TableCell>Data</TableCell>
              <TableCell align="right">Ilość</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {values.map((item) => (
              <TableRow key={item.data}>
                <TableCell>{item.data}</TableCell>
                <TableCell align="right">{item.ilosc.toLocaleString("pl-PL")}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}

export default function ForecastPage() {
  const [resource, setResource] = useState("WODA");
  const [districtId, setDistrictId] = useState(0);
  const loadForecast = useCallback(() => getLastForecast(), []);
  const loadForecastHistory = useCallback(() => getForecastHistory(), []);
  const loadBuildings = useCallback(() => getBuildings(), []);
  const loadUsage = useCallback(
    () => getUsageHistory(resource, districtId === 0 ? undefined : districtId),
    [districtId, resource],
  );
  const forecast = useResource(loadForecast);
  const forecastHistory = useResource(loadForecastHistory);
  const buildings = useResource(loadBuildings);
  const usage = useResource(loadUsage);

  const districts = useMemo(() => {
    const unique = new Map<number, string>();
    for (const building of buildings.data ?? []) {
      unique.set(building.dzielnicaId, building.dzielnica);
    }
    return Array.from(unique, ([id, name]) => ({ id, name }));
  }, [buildings.data]);

  const resourceLabel = resources.find((item) => item.value === resource)?.label ?? resource;
  const selectedForecast = forecast.data
    ? selectForecastPosition(
        forecast.data.pozycje,
        resource,
        districtId === 0 ? undefined : districtId,
      )
    : undefined;
  const forecastScope =
    districtId === 0
      ? "Całe miasto"
      : districts.find((district) => district.id === districtId)?.name ?? "Wybrana dzielnica";

  return (
    <Box>
      <PageHeader
        title="Prognoza zapotrzebowania"
        description="Porównaj ostatnią prognozę z historią zużycia zasobów."
      />
      <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", md: "repeat(3, minmax(0, 1fr))" }, gap: 2, mb: 2.5 }}>
        {[
          { label: "Prognoza", value: forecast.data?.id ? `#${forecast.data.id}` : "—" },
          { label: "Źródło", value: forecast.data?.zrodlo ?? "—" },
          {
            label: "Różnica",
            value: forecast.data?.roznicaProc == null ? "—" : `${forecast.data.roznicaProc.toLocaleString("pl-PL")}%`,
          },
        ].map((item) => (
          <Paper key={item.label} variant="outlined" sx={{ p: 2.25 }}>
            <Typography variant="body2" color="text.secondary">{item.label}</Typography>
            <Typography variant="h5" sx={{ mt: 0.75 }}>{item.value}</Typography>
          </Paper>
        ))}
      </Box>

      <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", xl: "minmax(0, 1.25fr) minmax(320px, 0.75fr)" }, gap: 2.5 }}>
        <Panel
          title="Historia zużycia"
          action={
            <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
              <TextField
                select
                size="small"
                label="Zasób"
                value={resource}
                onChange={(event) => setResource(event.target.value)}
                sx={{ minWidth: 140 }}
              >
                {resources.map((item) => (
                  <MenuItem key={item.value} value={item.value}>{item.label}</MenuItem>
                ))}
              </TextField>
              <TextField
                select
                size="small"
                label="Dzielnica"
                value={districtId}
                onChange={(event) => setDistrictId(Number(event.target.value))}
                sx={{ minWidth: 140 }}
              >
                <MenuItem value={0}>Całe miasto</MenuItem>
                {districts.map((district) => (
                  <MenuItem key={district.id} value={district.id}>{district.name}</MenuItem>
                ))}
              </TextField>
            </Box>
          }
        >
          {usage.loading && !usage.data ? (
            <LoadingState />
          ) : usage.error ? (
            <ErrorState message={usage.error.message} />
          ) : (
            <HistoryChart
              values={usage.data ?? []}
              resourceLabel={resourceLabel}
              forecastValue={selectedForecast?.ilosc}
            />
          )}
        </Panel>

        <Panel title="Prognoza dla wybranych filtrów">
          {forecast.loading && !forecast.data ? (
            <LoadingState />
          ) : forecast.error?.status === 404 ? (
            <EmptyState message="Brak prognozy. Dane pojawią się po uruchomieniu procesu P2." />
          ) : forecast.error ? (
            <ErrorState message={forecast.error.message} />
          ) : forecast.data ? (
            <Box>
              <Box sx={{ display: "flex", justifyContent: "space-between", gap: 1, mb: 2 }}>
                <Chip size="small" color={forecast.data.status === "ZATWIERDZONA" ? "success" : "warning"} label={forecast.data.status} />
                <Typography variant="body2" color="text.secondary">{formatDate(forecast.data.utworzono)}</Typography>
              </Box>
              <TableContainer>
                <Table size="small" aria-label="Prognoza dla wybranego zasobu i dzielnicy">
                  <TableHead>
                    <TableRow>
                      <TableCell>Zakres</TableCell>
                      <TableCell>Zasób</TableCell>
                      <TableCell align="right">Ilość</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {selectedForecast ? (
                      <TableRow>
                        <TableCell>{forecastScope}</TableCell>
                        <TableCell>{resourceLabel}</TableCell>
                        <TableCell align="right">
                          {selectedForecast.ilosc.toLocaleString("pl-PL")}
                        </TableCell>
                      </TableRow>
                    ) : (
                      <TableRow>
                        <TableCell colSpan={3}>
                          Brak prognozy dla wybranego zasobu i zakresu.
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          ) : (
            <EmptyState message="Brak danych prognozy." />
          )}
        </Panel>
      </Box>

      <Panel title="Historia prognoz">
        {forecastHistory.loading && !forecastHistory.data ? (
          <LoadingState />
        ) : forecastHistory.error ? (
          <ErrorState message={forecastHistory.error.message} />
        ) : forecastHistory.data?.length ? (
          <TableContainer>
            <Table size="small" aria-label="Historia prognoz">
              <TableHead>
                <TableRow>
                  <TableCell>Utworzono</TableCell>
                  <TableCell>Źródło</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right">Różnica</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {forecastHistory.data.map((item) => (
                  <TableRow key={item.id}>
                    <TableCell>{formatDate(item.utworzono)}</TableCell>
                    <TableCell>{item.zrodlo}</TableCell>
                    <TableCell>{item.status}</TableCell>
                    <TableCell align="right">{item.roznicaProc == null ? "—" : `${item.roznicaProc}%`}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        ) : (
          <EmptyState message="Brak historii prognoz." />
        )}
      </Panel>
    </Box>
  );
}
