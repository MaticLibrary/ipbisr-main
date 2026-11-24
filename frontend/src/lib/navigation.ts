export type UserRole = "operator" | "analyst" | "management";

export type NavigationItem = {
  label: string;
  href: string;
};

const navigationByRole: Record<UserRole, NavigationItem[]> = {
  operator: [
    { label: "Zgłoszenia", href: "/operator" },
    { label: "Procesy", href: "/procesy" },
  ],
  analyst: [
    { label: "Budynki", href: "/analityk/budynki" },
    { label: "Prognoza", href: "/analityk/prognoza" },
    { label: "Plan przydziału", href: "/analityk/plan" },
    { label: "Magazyn", href: "/magazyn" },
    { label: "Procesy", href: "/procesy" },
  ],
  management: [
    { label: "Budynki", href: "/analityk/budynki" },
    { label: "Procesy", href: "/procesy" },
    { label: "Audyt", href: "/audyt" },
  ],
};

export function getNavigationForRole(role: UserRole): NavigationItem[] {
  return navigationByRole[role];
}
