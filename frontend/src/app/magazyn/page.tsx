"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TextField from "@mui/material/TextField";
import { getInventory, isDemoDataEnabled, updateInventory } from "@/lib/api";
import type { InventoryItem } from "@/lib/types";
import { EmptyState, ErrorState, LoadingState, PageHeader, Panel } from "@/components/ui";
import { useResource } from "@/components/use-resource";

type InventoryDraft = Omit<InventoryItem, "ilosc"> & { ilosc: string };

export default function InventoryPage() {
  const demoMode = isDemoDataEnabled();
  const loadInventory = useCallback(() => getInventory(), []);
  const inventory = useResource(loadInventory);
  const [draft, setDraft] = useState<InventoryDraft[]>([]);
  const [formError, setFormError] = useState("");
  const [success, setSuccess] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (inventory.data) {
      setDraft(inventory.data.map((item) => ({ ...item, ilosc: String(item.ilosc) })));
    }
  }, [inventory.data]);

  function updateDraft(index: number, field: "ilosc" | "jednostka", value: string) {
    setDraft((current) =>
      current.map((item, currentIndex) =>
        currentIndex === index
          ? { ...item, [field]: value }
          : item,
      ),
    );
  }

  const inventoryTable = (
    <TableContainer>
      <Table aria-label="Stany magazynowe">
        <TableHead>
          <TableRow>
            <TableCell>Zasób</TableCell>
            <TableCell>Ilość</TableCell>
            <TableCell>Jednostka</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {draft.map((item, index) => (
            <TableRow key={item.zasob}>
              <TableCell sx={{ fontWeight: 600 }}>{item.zasob}</TableCell>
              <TableCell sx={{ maxWidth: 240 }}>
                {demoMode ? (
                  <TextField
                    label={`Ilość: ${item.zasob}`}
                    type="number"
                    size="small"
                    value={item.ilosc}
                    onChange={(event) => updateDraft(index, "ilosc", event.target.value)}
                    slotProps={{ htmlInput: { min: 0, step: "any" } }}
                    fullWidth
                  />
                ) : (
                  Number(item.ilosc).toLocaleString("pl-PL")
                )}
              </TableCell>
              <TableCell sx={{ maxWidth: 200 }}>
                {demoMode ? (
                  <TextField
                    label={`Jednostka: ${item.zasob}`}
                    size="small"
                    value={item.jednostka}
                    onChange={(event) => updateDraft(index, "jednostka", event.target.value)}
                    fullWidth
                  />
                ) : (
                  item.jednostka
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError("");
    setSuccess("");

    if (
      draft.some(
        (item) =>
          !item.ilosc.trim() ||
          !Number.isFinite(Number(item.ilosc)) ||
          Number(item.ilosc) < 0 ||
          !item.jednostka.trim(),
      )
    ) {
      setFormError("Ilość nie może być ujemna, a jednostka nie może być pusta.");
      return;
    }

    setSaving(true);
    try {
      const saved = await updateInventory(
        draft.map((item) => ({ ...item, ilosc: Number(item.ilosc) })),
      );
      setDraft(saved.map((item) => ({ ...item, ilosc: String(item.ilosc) })));
      setSuccess("Stany magazynowe zostały zapisane.");
      await inventory.reload();
    } catch (error: unknown) {
      setFormError(error instanceof Error ? error.message : "Nie udało się zapisać magazynu.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <Box>
      <PageHeader
        title="Magazyn"
        description={
          demoMode
            ? "Edytuj syntetyczne stany magazynowe na potrzeby demonstracji; zmiany są tymczasowe."
            : "Przeglądaj stany magazynowe z backendu. Edycja z poziomu panelu jest wyłączona."
        }
      />
      <Panel title="Aktualne stany" action={demoMode ? "Edycja demo" : "Tylko odczyt"}>
        {inventory.loading && !inventory.data ? (
          <LoadingState />
        ) : inventory.error ? (
          <ErrorState message={inventory.error.message} />
        ) : inventory.data?.length ? (
          demoMode ? (
            <Stack component="form" spacing={2} onSubmit={save}>
              {inventoryTable}
              {formError && <Alert severity="error">{formError}</Alert>}
              {success && <Alert severity="success">{success}</Alert>}
              <Button type="submit" variant="contained" disabled={saving} sx={{ alignSelf: "flex-start" }}>
                {saving ? "Zapisywanie…" : "Zapisz stany"}
              </Button>
            </Stack>
          ) : (
            <Box>
              {inventoryTable}
              <Alert severity="info" sx={{ mt: 2 }}>
                Edycja stanów jest dostępna wyłącznie w jawnym trybie demo.
              </Alert>
            </Box>
          )
        ) : (
          <EmptyState message="Brak pozycji magazynowych." />
        )}
      </Panel>
    </Box>
  );
}
