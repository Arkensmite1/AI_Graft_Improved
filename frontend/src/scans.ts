import { useCallback, useEffect, useState } from "react";

import type { AnalyzeResult, Mode } from "@/src/api";
import { storage } from "@/src/utils/storage";

export type Scan = { id: string; mode: Mode; createdAt: string; thumb: string; photo: string; result: AnalyzeResult; batchId?: string; aspect?: number };

const KEY = "scan_history";
const MAX = 30;
const listeners = new Set<() => void>();

export async function loadScans(): Promise<Scan[]> {
  const raw = await storage.getItem(KEY, "[]");
  try {
    return JSON.parse(String(raw ?? "[]")) as Scan[];
  } catch {
    return [];
  }
}

async function saveAll(list: Scan[]) {
  await storage.setItem(KEY, JSON.stringify(list.slice(0, MAX)));
  listeners.forEach((l) => l());
}

export async function addScan(scan: Scan) {
  const list = await loadScans();
  await saveAll([scan, ...list.filter((s) => s.id !== scan.id)]);
}

export async function deleteScan(id: string) {
  await saveAll((await loadScans()).filter((s) => s.id !== id));
}

export const clearScans = () => saveAll([]);

export async function updateScan(id: string, patch: Partial<Scan>) {
  await saveAll((await loadScans()).map((s) => (s.id === id ? { ...s, ...patch } : s)));
}

export function useScans() {
  const [scans, setScans] = useState<Scan[] | null>(null);
  const refresh = useCallback(() => loadScans().then(setScans), []);
  useEffect(() => {
    refresh();
    listeners.add(refresh);
    return () => {
      listeners.delete(refresh);
    };
  }, [refresh]);
  return { scans, refresh };
}

// Photo handed from the camera screen to the result screen (kept in memory only).
export type PendingPhoto = { mode: Mode; uri: string; base64: string; thumb: string; photo: string; aspect: number; batchId?: string };
let pending: PendingPhoto | null = null;
export const setPending = (p: PendingPhoto | null) => {
  pending = p;
};
export const getPending = () => pending;
