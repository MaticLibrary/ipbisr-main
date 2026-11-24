import assert from "node:assert/strict";
import test from "node:test";
import { getNavigationForRole } from "../src/lib/navigation.ts";

test("operator navigation includes incident reporting and process monitoring", () => {
  assert.deepEqual(
    getNavigationForRole("operator").map((item) => item.href),
    ["/operator", "/procesy"],
  );
});

test("management navigation includes buildings, processes, and audit", () => {
  assert.deepEqual(
    getNavigationForRole("management").map((item) => item.href),
    ["/analityk/budynki", "/procesy", "/audyt"],
  );
});

test("analyst navigation includes buildings, forecasting, planning, inventory, and processes", () => {
  assert.deepEqual(
    getNavigationForRole("analyst").map((item) => item.href),
    ["/analityk/budynki", "/analityk/prognoza", "/analityk/plan", "/magazyn", "/procesy"],
  );
});
