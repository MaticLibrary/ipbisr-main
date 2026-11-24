import type { Metadata } from "next";
import type { ReactNode } from "react";
import { AppRouterCacheProvider } from "@mui/material-nextjs/v16-appRouter";
import CssBaseline from "@mui/material/CssBaseline";
import AppShell from "@/components/app-shell";

export const metadata: Metadata = {
  title: "IPBiSR – Przydział zasobów",
  description: "System inteligentnego przydzielania zasobów niezbędnych do życia",
};

export const dynamic = "force-dynamic";

async function checkHealth(url: string): Promise<boolean> {
  try {
    const response = await fetch(`${url}/health`, {
      cache: "no-store",
      signal: AbortSignal.timeout(2500),
    });
    return response.ok;
  } catch {
    return false;
  }
}

export default async function RootLayout({ children }: { children: ReactNode }) {
  const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8081";
  const mlServiceUrl = process.env.ML_SERVICE_URL ?? "http://localhost:8000";
  const [backendUp, mlUp] = await Promise.all([
    checkHealth(backendUrl),
    checkHealth(mlServiceUrl),
  ]);

  return (
    <html lang="pl">
      <body>
        <AppRouterCacheProvider>
          <CssBaseline />
          <AppShell services={{ backendUp, mlUp }}>{children}</AppShell>
        </AppRouterCacheProvider>
      </body>
    </html>
  );
}
