"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError } from "@/lib/api";

type ResourceError = {
  message: string;
  status: number | null;
};

export function useResource<T>(
  load: () => Promise<T>,
  refreshIntervalMs?: number,
) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<ResourceError | null>(null);
  const [loading, setLoading] = useState(true);
  const requestId = useRef(0);

  const reload = useCallback(async () => {
    const currentRequestId = ++requestId.current;
    setLoading(true);
    setError(null);
    try {
      const result = await load();
      if (currentRequestId === requestId.current) setData(result);
    } catch (caught: unknown) {
      if (currentRequestId === requestId.current) {
        setData(null);
        setError({
          message:
            caught instanceof Error
              ? caught.message
              : "Wystąpił nieznany błąd podczas pobierania danych.",
          status: caught instanceof ApiError ? caught.status : null,
        });
      }
    } finally {
      if (currentRequestId === requestId.current) setLoading(false);
    }
  }, [load]);

  useEffect(() => {
    void reload();
    const timer = refreshIntervalMs
      ? window.setInterval(() => void reload(), refreshIntervalMs)
      : null;
    return () => {
      if (timer !== null) window.clearInterval(timer);
      requestId.current += 1;
    };
  }, [reload, refreshIntervalMs]);

  return { data, error, loading, reload };
}
