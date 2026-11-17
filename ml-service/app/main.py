"""Serwis ML: prognoza zapotrzebowania (/forecast) i optymalizacja przydziału (/allocate).

Na razie tylko /health – endpointy ML dojdą w krokach P2 i P3.
"""

from fastapi import FastAPI

app = FastAPI(title="IPBiSR – serwis ML", version="0.1.0")


@app.get("/health")
def health() -> dict:
    # Prosty test, czy serwis działa
    return {"status": "UP", "service": "ml-service"}
