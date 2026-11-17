import Container from "@mui/material/Container";
import Typography from "@mui/material/Typography";
import Stack from "@mui/material/Stack";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Chip from "@mui/material/Chip";

// Strona renderowana przy każdym wejściu – status serwisów jest zawsze aktualny
export const dynamic = "force-dynamic";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8081";
const ML_SERVICE_URL = process.env.ML_SERVICE_URL ?? "http://localhost:8000";

// Sprawdza /health danego serwisu (po stronie serwera Next.js, więc bez CORS)
async function checkHealth(url: string): Promise<boolean> {
  try {
    const res = await fetch(`${url}/health`, { cache: "no-store" });
    return res.ok;
  } catch {
    return false;
  }
}

export default async function HomePage() {
  const [backendUp, mlUp] = await Promise.all([
    checkHealth(BACKEND_URL),
    checkHealth(ML_SERVICE_URL),
  ]);

  const services = [
    { name: "Backend (Spring Boot)", url: BACKEND_URL, up: backendUp },
    { name: "Serwis ML (FastAPI)", url: ML_SERVICE_URL, up: mlUp },
  ];

  return (
    <Container maxWidth="md" sx={{ py: 6 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        System przydzielania zasobów
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 4 }}>
        Centrum zarządzania kryzysowego – panel operatora punktu i analityka kryzysowego.
      </Typography>

      <Typography variant="h6" gutterBottom>
        Stan serwisów
      </Typography>
      <Stack spacing={2}>
        {services.map((s) => (
          <Card key={s.name} variant="outlined">
            <CardContent
              sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 2, flexWrap: "wrap" }}
            >
              <div>
                <Typography sx={{ fontWeight: 600 }}>{s.name}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {s.url}/health
                </Typography>
              </div>
              <Chip label={s.up ? "działa" : "niedostępny"} color={s.up ? "success" : "error"} />
            </CardContent>
          </Card>
        ))}
      </Stack>
    </Container>
  );
}
