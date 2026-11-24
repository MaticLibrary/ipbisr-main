"use client";

import Link from "next/link";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Paper from "@mui/material/Paper";
import Typography from "@mui/material/Typography";
import { getNavigationForRole } from "@/lib/navigation";
import { useCurrentRole } from "@/components/role-context";
import { PageHeader } from "@/components/ui";

const descriptions: Record<string, string> = {
  "/operator": "Rejestruj zdarzenia i śledź status obsługi.",
  "/analityk/budynki": "Przeglądaj mieszkańców, potrzeby i priorytety.",
  "/analityk/prognoza": "Analizuj prognozę i historię zużycia zasobów.",
  "/analityk/plan": "Sprawdź przydziały, niedobory i harmonogram dostaw.",
  "/magazyn": "Przeglądaj i aktualizuj stany zasobów.",
  "/procesy": "Monitoruj instancje i uruchamiaj symulacje.",
  "/audyt": "Przeglądaj zapisane decyzje i zdarzenia.",
};

export default function HomePage() {
  const role = useCurrentRole();
  const shortcuts = getNavigationForRole(role);

  return (
    <Box>
      <PageHeader
        title="Centrum operacyjne"
        description="Panel do obsługi zgłoszeń i planowania przydziału zasobów."
      />
      <Paper
        variant="outlined"
        sx={{
          p: { xs: 2.5, sm: 3.5 },
          mb: 3,
          borderColor: "divider",
          background: "linear-gradient(120deg, #ffffff 0%, #eff5ff 100%)",
        }}
      >
        <Typography variant="h5" component="h2">
          Witaj w systemie IPBiSR
        </Typography>
        <Typography color="text.secondary" sx={{ mt: 1, maxWidth: 760 }}>
          Korzystaj z menu dopasowanego do wybranej roli. Decyzje procesowe, które
          wymagają udziału człowieka, są obsługiwane w Camunda Tasklist.
        </Typography>
      </Paper>
      <Box
        sx={{
          display: "grid",
          gridTemplateColumns: { xs: "1fr", md: "repeat(3, minmax(0, 1fr))" },
          gap: 2,
        }}
      >
        {shortcuts.map((shortcut) => (
          <Paper key={shortcut.href} variant="outlined" sx={{ p: 2.5, display: "flex", flexDirection: "column" }}>
            <Typography variant="h6" component="h2">
              {shortcut.label}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 1, mb: 2, flexGrow: 1 }}>
              {descriptions[shortcut.href]}
            </Typography>
            <Button component={Link} href={shortcut.href} sx={{ alignSelf: "flex-start", px: 0 }}>
              Otwórz ekran
            </Button>
          </Paper>
        ))}
      </Box>
    </Box>
  );
}
