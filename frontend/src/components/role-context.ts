"use client";

import { createContext, useContext } from "react";
import type { UserRole } from "@/lib/navigation";

export const UserRoleContext = createContext<UserRole>("analyst");

export function useCurrentRole(): UserRole {
  return useContext(UserRoleContext);
}
