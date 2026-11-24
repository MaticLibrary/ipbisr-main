import type { ReactNode } from "react";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import CircularProgress from "@mui/material/CircularProgress";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";

export function PageHeader({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <Stack
      direction={{ xs: "column", sm: "row" }}
      spacing={2}
      sx={{ mb: 3, justifyContent: "space-between", alignItems: { xs: "stretch", sm: "center" } }}
    >
      <Box>
        <Typography component="h1" variant="h4" sx={{ fontSize: { xs: "1.65rem", sm: "2rem" } }}>
          {title}
        </Typography>
        <Typography color="text.secondary" sx={{ mt: 0.75 }}>
          {description}
        </Typography>
      </Box>
      {action}
    </Stack>
  );
}

export function Panel({
  title,
  children,
  action,
}: {
  title?: string;
  children: ReactNode;
  action?: ReactNode;
}) {
  return (
    <Paper
      variant="outlined"
      sx={{ minWidth: 0, p: { xs: 2, sm: 2.5 }, borderColor: "divider" }}
    >
      {(title || action) && (
        <Stack
          direction="row"
          spacing={2}
          sx={{ mb: 2, justifyContent: "space-between", alignItems: "center" }}
        >
          {title && <Typography variant="h6">{title}</Typography>}
          {action}
        </Stack>
      )}
      {children}
    </Paper>
  );
}

export function LoadingState({ label = "Ładowanie danych…" }: { label?: string }) {
  return (
    <Stack direction="row" spacing={1.5} role="status" sx={{ py: 3, alignItems: "center" }}>
      <CircularProgress size={20} aria-hidden="true" />
      <Typography color="text.secondary">{label}</Typography>
    </Stack>
  );
}

export function ErrorState({ message }: { message: string }) {
  return <Alert severity="error">{message}</Alert>;
}

export function EmptyState({ message }: { message: string }) {
  return (
    <Box sx={{ py: 3 }}>
      <Typography color="text.secondary">{message}</Typography>
    </Box>
  );
}

export function formatDate(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat("pl-PL", { dateStyle: "medium", timeStyle: "short" }).format(date);
}
