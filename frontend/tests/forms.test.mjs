import assert from "node:assert/strict";
import test from "node:test";
import { validateReportInput } from "../src/lib/api.ts";

test("requires PESEL or a building for an incident", () => {
  assert.equal(
    validateReportInput({ rodzaj: "CHOROBA" }),
    "Podaj PESEL albo ID budynku.",
  );
});

test("requires PESEL for incident kinds other than housing loss", () => {
  assert.equal(
    validateReportInput({ budynekId: 1, rodzaj: "CHOROBA" }),
    "Dla tego rodzaju zgłoszenia wymagany jest PESEL.",
  );
});

test("allows a building-only housing-loss report", () => {
  assert.equal(
    validateReportInput({ budynekId: 1, rodzaj: "UTRATA_MIESZKANIA" }),
    null,
  );
});
