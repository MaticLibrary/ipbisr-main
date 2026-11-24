"use client";

import { useEffect, useState, type ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Chip from "@mui/material/Chip";
import Divider from "@mui/material/Divider";
import Drawer from "@mui/material/Drawer";
import FormControl from "@mui/material/FormControl";
import IconButton from "@mui/material/IconButton";
import List from "@mui/material/List";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemText from "@mui/material/ListItemText";
import MenuItem from "@mui/material/MenuItem";
import Select from "@mui/material/Select";
import Stack from "@mui/material/Stack";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import { createTheme, ThemeProvider } from "@mui/material/styles";
import { getNavigationForRole, type UserRole } from "@/lib/navigation";
import { isDemoDataEnabled } from "@/lib/api";
import { UserRoleContext } from "@/components/role-context";

const drawerWidth = 248;
const tasklistUrl =
  process.env.NEXT_PUBLIC_CAMUNDA_TASKLIST_URL ?? "http://localhost:8080/tasklist";
const theme = createTheme({
  palette: {
    mode: "light",
    primary: { main: "#1d4ed8", dark: "#1e40af" },
    background: { default: "#f4f7fb", paper: "#ffffff" },
    text: { primary: "#172033", secondary: "#526078" },
    success: { main: "#15803d" },
    warning: { main: "#b45309" },
    error: { main: "#b91c1c" },
    divider: "#dce3ed",
  },
  shape: { borderRadius: 10 },
  typography: {
    fontFamily: 'Arial, "Helvetica Neue", sans-serif',
    button: { textTransform: "none", fontWeight: 600 },
    h4: { fontWeight: 700, letterSpacing: "-0.025em" },
    h5: { fontWeight: 700, letterSpacing: "-0.02em" },
  },
  components: {
    MuiButton: { styleOverrides: { root: { minHeight: 44 } } },
    MuiTableCell: { styleOverrides: { head: { fontWeight: 700 } } },
  },
});

type ServiceState = {
  backendUp: boolean;
  mlUp: boolean;
};

const roleLabels: Record<UserRole, string> = {
  operator: "Operator",
  analyst: "Analityk",
  management: "Zarząd",
};

function NavigationContent({
  role,
  pathname,
  onNavigate,
}: {
  role: UserRole;
  pathname: string;
  onNavigate?: () => void;
}) {
  return (
    <Box sx={{ px: 1.5, py: 2 }}>
      <Typography variant="overline" color="text.secondary" sx={{ px: 1.5 }}>
        MENU · {roleLabels[role].toLocaleUpperCase("pl-PL")}
      </Typography>
      <List aria-label={`Menu roli: ${roleLabels[role]}`} sx={{ mt: 1 }}>
        {getNavigationForRole(role).map((item) => (
          <ListItemButton
            key={item.href}
            component={Link}
            href={item.href}
            selected={pathname === item.href}
            onClick={onNavigate}
            sx={{
              borderRadius: 1.5,
              mb: 0.5,
              minHeight: 44,
              "&.Mui-selected": {
                bgcolor: "#eaf0ff",
                color: "primary.dark",
                "&:hover": { bgcolor: "#e1eaff" },
              },
            }}
          >
            <ListItemText primary={item.label} />
          </ListItemButton>
        ))}
      </List>
      <Divider sx={{ my: 1.5 }} />
      <ListItemButton component={Link} href="/" sx={{ borderRadius: 1.5, minHeight: 44 }}>
        <ListItemText primary="Przegląd" />
      </ListItemButton>
      {role !== "operator" && (
        <ListItemButton
          component="a"
          href={tasklistUrl}
          target="_blank"
          rel="noopener noreferrer"
          sx={{ borderRadius: 1.5, minHeight: 44 }}
        >
          <ListItemText primary="Camunda Tasklist" secondary="Zadania do decyzji" />
        </ListItemButton>
      )}
    </Box>
  );
}

export default function AppShell({
  children,
  services,
}: {
  children: ReactNode;
  services: ServiceState;
}) {
  const pathname = usePathname();
  const [role, setRole] = useState<UserRole>("analyst");
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    const savedRole = window.localStorage.getItem("ipbisr-role");
    if (savedRole === "operator" || savedRole === "analyst" || savedRole === "management") {
      setRole(savedRole);
    }
  }, []);

  function changeRole(nextRole: UserRole) {
    setRole(nextRole);
    window.localStorage.setItem("ipbisr-role", nextRole);
  }

  const navigation = (
    <NavigationContent
      role={role}
      pathname={pathname}
      onNavigate={() => setMobileMenuOpen(false)}
    />
  );

  return (
    <ThemeProvider theme={theme}>
      <UserRoleContext.Provider value={role}>
        <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }}>
          <AppBar
            position="sticky"
            color="inherit"
            elevation={0}
            sx={{
              borderBottom: 1,
              borderColor: "divider",
              zIndex: (currentTheme) => currentTheme.zIndex.drawer + 1,
            }}
          >
            <Toolbar sx={{ gap: { xs: 1, sm: 2 }, minHeight: "68px !important" }}>
              <IconButton
                aria-label="Otwórz menu"
                aria-controls="mobile-navigation"
                aria-expanded={mobileMenuOpen}
                onClick={() => setMobileMenuOpen(true)}
                sx={{ display: { xs: "inline-flex", md: "none" } }}
              >
                <Box aria-hidden="true" sx={{ width: 18, display: "grid", gap: 0.5 }}>
                  {[0, 1, 2].map((line) => (
                    <Box key={line} sx={{ height: 2, borderRadius: 1, bgcolor: "text.primary" }} />
                  ))}
                </Box>
              </IconButton>
              <Typography
                component={Link}
                href="/"
                variant="h6"
                sx={{ color: "text.primary", textDecoration: "none", fontWeight: 800, mr: "auto" }}
              >
                IPBiSR
              </Typography>
              <Stack direction="row" spacing={0.75} sx={{ display: { xs: "none", sm: "flex" } }}>
                <Chip
                  size="small"
                  color={services.backendUp ? "success" : "error"}
                  label={`Backend ${services.backendUp ? "działa" : "niedostępny"}`}
                />
                <Chip
                  size="small"
                  color={services.mlUp ? "success" : "error"}
                  label={`ML ${services.mlUp ? "działa" : "niedostępny"}`}
                />
                {isDemoDataEnabled() && <Chip size="small" color="warning" label="Dane demo" />}
              </Stack>
              <FormControl size="small" sx={{ minWidth: { xs: 112, sm: 142 } }}>
                <Select<UserRole>
                  aria-label="Wybierz rolę"
                  value={role}
                  onChange={(event) => changeRole(event.target.value)}
                  sx={{ minHeight: 44 }}
                >
                  <MenuItem value="operator">Operator</MenuItem>
                  <MenuItem value="analyst">Analityk</MenuItem>
                  <MenuItem value="management">Zarząd</MenuItem>
                </Select>
              </FormControl>
            </Toolbar>
          </AppBar>

          <Box sx={{ display: "flex" }}>
            <Drawer
              variant="permanent"
              sx={{
                display: { xs: "none", md: "block" },
                width: drawerWidth,
                flexShrink: 0,
                "& .MuiDrawer-paper": {
                  width: drawerWidth,
                  boxSizing: "border-box",
                  top: 68,
                  height: "calc(100% - 68px)",
                  borderRight: 1,
                  borderColor: "divider",
                  bgcolor: "background.paper",
                },
              }}
            >
              {navigation}
            </Drawer>
            <Drawer
              id="mobile-navigation"
              variant="temporary"
              open={mobileMenuOpen}
              onClose={() => setMobileMenuOpen(false)}
              ModalProps={{ keepMounted: true }}
              sx={{
                display: { xs: "block", md: "none" },
                "& .MuiDrawer-paper": { width: drawerWidth, boxSizing: "border-box" },
              }}
            >
              {navigation}
            </Drawer>
            <Box component="main" sx={{ flexGrow: 1, minWidth: 0 }}>
              <Box sx={{ maxWidth: 1440, mx: "auto", px: { xs: 2, sm: 3, lg: 4 }, py: { xs: 3, sm: 4 } }}>
                <Stack
                  direction="row"
                  spacing={0.75}
                  sx={{ display: { xs: "flex", sm: "none" }, mb: 2, flexWrap: "wrap" }}
                >
                  <Chip
                    size="small"
                    color={services.backendUp ? "success" : "error"}
                    label={`Backend ${services.backendUp ? "działa" : "niedostępny"}`}
                  />
                  <Chip
                    size="small"
                    color={services.mlUp ? "success" : "error"}
                    label={`ML ${services.mlUp ? "działa" : "niedostępny"}`}
                  />
                  {isDemoDataEnabled() && <Chip size="small" color="warning" label="Dane demo" />}
                </Stack>
                {children}
              </Box>
            </Box>
          </Box>
        </Box>
      </UserRoleContext.Provider>
    </ThemeProvider>
  );
}
