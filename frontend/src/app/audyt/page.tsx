"use client";

import { useCallback, useState } from "react";
import Accordion from "@mui/material/Accordion";
import AccordionDetails from "@mui/material/AccordionDetails";
import AccordionSummary from "@mui/material/AccordionSummary";
import Box from "@mui/material/Box";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { getAudit } from "@/lib/api";
import { useResource } from "@/components/use-resource";
import { EmptyState, ErrorState, formatDate, LoadingState, PageHeader, Panel } from "@/components/ui";

export default function AuditPage() {
  const [limit, setLimit] = useState(100);
  const loadAudit = useCallback(() => getAudit(limit), [limit]);
  const audit = useResource(loadAudit);

  return (
    <Box>
      <PageHeader
        title="Dziennik audytu"
        description="Historia decyzji i zdarzeń zapisanych przez procesy."
        action={
          <TextField
            select
            size="small"
            label="Liczba wpisów"
            value={limit}
            onChange={(event) => setLimit(Number(event.target.value))}
            sx={{ minWidth: 150 }}
          >
            {[50, 100, 250, 500, 1000].map((value) => (
              <MenuItem key={value} value={value}>{value}</MenuItem>
            ))}
          </TextField>
        }
      />
      <Panel title="Wpisy audytowe">
        {audit.loading && !audit.data ? (
          <LoadingState />
        ) : audit.error ? (
          <ErrorState message={audit.error.message} />
        ) : audit.data?.length ? (
          <Stack spacing={1}>
            {audit.data.map((entry, index) => (
              <Accordion key={`${entry.czas}-${entry.akcja}-${index}`} disableGutters variant="outlined">
                <AccordionSummary
                  expandIcon={<span aria-hidden="true">⌄</span>}
                  aria-controls={`audit-${index}-details`}
                  id={`audit-${index}-header`}
                >
                  <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", md: "220px 1fr 1fr 1fr" }, gap: { xs: 0.5, md: 2 }, width: "100%", pr: 1 }}>
                    <Typography variant="body2" color="text.secondary">{formatDate(entry.czas)}</Typography>
                    <Typography sx={{ fontWeight: 700 }}>{entry.akcja}</Typography>
                    <Typography variant="body2">{entry.proces}</Typography>
                    <Typography variant="body2" color="text.secondary">{entry.aktor}</Typography>
                  </Box>
                </AccordionSummary>
                <AccordionDetails id={`audit-${index}-details`}>
                  <Typography component="pre" variant="body2" sx={{ m: 0, whiteSpace: "pre-wrap", overflowWrap: "anywhere", fontFamily: "monospace" }}>
                    {JSON.stringify(entry.szczegoly ?? {}, null, 2)}
                  </Typography>
                </AccordionDetails>
              </Accordion>
            ))}
          </Stack>
        ) : (
          <EmptyState message="Dziennik audytu jest pusty." />
        )}
      </Panel>
    </Box>
  );
}
