import { useCallback, useEffect, useState } from "react";

import { PLAN_DAYS } from "@/src/graftPlan";
import type { Lang } from "@/src/i18n";
import { cancelReminders, scheduleReminders } from "@/src/reminders";
import { storage } from "@/src/utils/storage";

export type Batch = {
  id: string;
  name: string;
  plants: number;
  startMs: number; // local midnight of grafting day
  done: number[]; // days marked done
  reminders: boolean;
  notifIds: string[];
  createdAt: string;
};

const KEY = "graft_batches";
const listeners = new Set<() => void>();

export const midnight = (ms: number) => new Date(ms).setHours(0, 0, 0, 0);
export const dayOf = (b: Batch) => Math.floor((midnight(Date.now()) - b.startMs) / 86400000 + 0.5);
export const isActive = (b: Batch) => dayOf(b) <= PLAN_DAYS;

export async function loadBatches(): Promise<Batch[]> {
  const raw = await storage.getItem(KEY, "[]");
  try {
    return JSON.parse(String(raw ?? "[]")) as Batch[];
  } catch {
    return [];
  }
}

async function saveAll(list: Batch[]) {
  await storage.setItem(KEY, JSON.stringify(list));
  listeners.forEach((l) => l());
}

export async function createBatch(input: { name: string; plants: number; daysAgo: number; reminders: boolean; lang: Lang }) {
  const startMs = midnight(Date.now() - input.daysAgo * 86400000);
  const id = `g-${Date.now()}`;
  const notifIds = input.reminders ? await scheduleReminders(id, input.name, startMs, input.lang).catch(() => []) : [];
  const batch: Batch = {
    id,
    name: input.name,
    plants: input.plants,
    startMs,
    done: [],
    reminders: input.reminders && notifIds.length > 0,
    notifIds,
    createdAt: new Date().toISOString(),
  };
  await saveAll([batch, ...(await loadBatches())]);
  return batch;
}

export async function updateBatch(id: string, patch: Partial<Batch>) {
  await saveAll((await loadBatches()).map((b) => (b.id === id ? { ...b, ...patch } : b)));
}

export async function toggleDone(b: Batch, day: number) {
  const done = b.done.includes(day) ? b.done.filter((d) => d !== day) : [...b.done, day];
  await updateBatch(b.id, { done });
}

export async function enableReminders(b: Batch, lang: Lang) {
  await cancelReminders(b.notifIds);
  const notifIds = await scheduleReminders(b.id, b.name, b.startMs, lang);
  await updateBatch(b.id, { notifIds, reminders: notifIds.length > 0 });
}

export async function deleteBatch(b: Batch) {
  await cancelReminders(b.notifIds);
  await saveAll((await loadBatches()).filter((x) => x.id !== b.id));
}

export function useBatches() {
  const [batches, setBatches] = useState<Batch[] | null>(null);
  const refresh = useCallback(() => loadBatches().then(setBatches), []);
  useEffect(() => {
    refresh();
    listeners.add(refresh);
    return () => {
      listeners.delete(refresh);
    };
  }, [refresh]);
  return { batches, refresh };
}
