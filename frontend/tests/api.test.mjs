import assert from "node:assert/strict";
import test from "node:test";

process.env.NEXT_PUBLIC_DEMO_DATA = "false";
const {
  ApiError,
  apiRequest,
  filterBuildingsByDistrict,
  getApiErrorMessage,
  getBuildings,
  selectForecastPosition,
  updateInventory,
} =
  await import("../src/lib/api.ts");

test("uses the backend error message when a response contains blad", () => {
  assert.equal(getApiErrorMessage({ blad: "PESEL nie istnieje" }, "Błąd API"), "PESEL nie istnieje");
});

test("uses the fallback when the error payload has no usable message", () => {
  assert.equal(getApiErrorMessage({ message: "internal" }, "Błąd API"), "Błąd API");
  assert.equal(getApiErrorMessage({ blad: "   " }, "Błąd API"), "Błąd API");
});

test("passes a bounded timeout signal to backend requests", async () => {
  const originalFetch = globalThis.fetch;
  const originalTimeout = AbortSignal.timeout;
  const timeoutSignal = new AbortController().signal;
  let requestSignal;

  globalThis.fetch = async (_url, init) => {
    requestSignal = init.signal;
    return new Response("{}", { status: 200 });
  };
  AbortSignal.timeout = (milliseconds) => {
    assert.equal(milliseconds, 10000);
    return timeoutSignal;
  };

  try {
    await apiRequest("/api/test");
    assert.equal(requestSignal, timeoutSignal);
  } finally {
    globalThis.fetch = originalFetch;
    AbortSignal.timeout = originalTimeout;
  }
});

test("preserves backend status and message on API errors", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () =>
    new Response(JSON.stringify({ blad: "Brak prognozy" }), {
      status: 404,
      headers: { "Content-Type": "application/json" },
    });

  try {
    await assert.rejects(
      apiRequest("/api/prognozy/ostatnia"),
      (error) =>
        error instanceof ApiError &&
        error.status === 404 &&
        error.message === "Brak prognozy",
    );
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("surfaces API failures without substituting demo records", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () =>
    new Response(JSON.stringify({ blad: "Backend niedostępny" }), {
      status: 503,
      headers: { "Content-Type": "application/json" },
    });

  try {
    await assert.rejects(
      getBuildings(),
      (error) =>
        error instanceof ApiError &&
        error.status === 503 &&
        error.message === "Backend niedostępny",
    );
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("surfaces fetch failures to the caller", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => {
    throw new TypeError("network offline");
  };

  try {
    await assert.rejects(apiRequest("/api/test"), /network offline/);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("rejects an empty successful API response", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = async () => new Response(null, { status: 200 });

  try {
    await assert.rejects(
      apiRequest("/api/test"),
      (error) =>
        error instanceof ApiError &&
        error.status === 200 &&
        error.message === "Serwer zwrócił pustą odpowiedź.",
    );
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("filters the building list to the selected district", () => {
  const buildings = [
    { id: 1, dzielnicaId: 1, adres: "Rynek 1", dzielnica: "Centrum", liczbaMieszkancow: 4 },
    { id: 2, dzielnicaId: 2, adres: "Polna 2", dzielnica: "Północ", liczbaMieszkancow: 8 },
  ];

  assert.deepEqual(filterBuildingsByDistrict?.(buildings, 2), [buildings[1]]);
});

test("selects the latest forecast position for the chosen resource and district", () => {
  const positions = [
    { poziom: "MIASTO", obiektId: null, zasob: "WODA", ilosc: 1200 },
    { poziom: "DZIELNICA", obiektId: 1, zasob: "WODA", ilosc: 500 },
    { poziom: "DZIELNICA", obiektId: 2, zasob: "WODA", ilosc: 700 },
    { poziom: "DZIELNICA", obiektId: 2, zasob: "LEKI", ilosc: 30 },
  ];

  assert.deepEqual(selectForecastPosition?.(positions, "WODA", 2), positions[2]);
  assert.deepEqual(selectForecastPosition?.(positions, "WODA"), positions[0]);
  assert.equal(selectForecastPosition?.(positions, "SCHRONIENIE", 2), undefined);
});

test("does not submit inventory changes outside explicit demo mode", async () => {
  const originalFetch = globalThis.fetch;
  let fetchCalled = false;
  globalThis.fetch = async () => {
    fetchCalled = true;
    return new Response("[]", { status: 200 });
  };

  try {
    await assert.rejects(
      updateInventory([{ zasob: "WODA", ilosc: 10, jednostka: "l" }]),
      /tylko w trybie demo/i,
    );
    assert.equal(fetchCalled, false);
  } finally {
    globalThis.fetch = originalFetch;
  }
});
