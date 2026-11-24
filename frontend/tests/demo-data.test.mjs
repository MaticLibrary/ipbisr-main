import assert from "node:assert/strict";
import test from "node:test";

process.env.NEXT_PUBLIC_DEMO_DATA = "true";
const api = await import("../src/lib/api.ts");

test("demo mode returns synthetic building and resident records", async () => {
  const buildings = await api.getBuildings();
  const building = await api.getBuilding(buildings[0].id);

  assert.ok(buildings.length > 0);
  assert.equal(building.id, buildings[0].id);
  assert.ok(building.mieszkancy.length > 0);
});

test("demo incident submissions appear in the report list", async () => {
  const created = await api.createReport({
    pesel: "50010112345",
    rodzaj: "CHOROBA",
    opis: "test demo",
  });
  const reports = await api.getReports();

  assert.equal(created.status, "NOWE");
  assert.ok(reports.some((report) => report.id === created.id));
});

test("demo usage history responds to resource and district filters", async () => {
  const cityWater = await api.getUsageHistory("WODA");
  const cityFood = await api.getUsageHistory("ZYWNOSC");
  const districtWater = await api.getUsageHistory("WODA", 1);

  assert.notDeepEqual(cityWater, cityFood);
  assert.notDeepEqual(cityWater, districtWater);
});
