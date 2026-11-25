import type { Lang } from "@/src/i18n";

const BASE = process.env.EXPO_PUBLIC_BACKEND_URL;

export type Mode = "seedling" | "graft" | "guide";
export type Verdict = "good" | "wait" | "bad" | "unclear";
export type StepIcon =
  | "water" | "sun" | "shade" | "cover" | "clock" | "cut" | "leaf"
  | "warning" | "check" | "camera" | "remove" | "medicine" | "thermometer" | "wind";

export type Mark = {
  kind: "cut" | "clip" | "rootstock" | "scion" | "remove";
  label: string;
  x1: number;
  y1: number;
  x2: number | null;
  y2: number | null;
};

export type AnalyzeResult = {
  id: string;
  mode: Mode;
  lang: Lang;
  verdict: Verdict;
  title: string;
  message: string;
  steps: { icon: StepIcon; text: string; mark?: number | null }[];
  marks?: Mark[];
  facts: { leaves: number | null; stem: "thin" | "ok" | "thick" | null; days_wait: number | null; healing_day: number | null };
  speak: string;
  created_at: string;
};

async function post<T>(path: string, body: object, timeoutMs: number): Promise<T> {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), timeoutMs);
  try {
    const res = await fetch(`${BASE}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
      signal: ctrl.signal,
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return (await res.json()) as T;
  } finally {
    clearTimeout(timer);
  }
}

export const analyzePhoto = (mode: Mode, imageBase64: string, lang: Lang) =>
  post<AnalyzeResult>("/api/analyze", { mode, image_base64: imageBase64, lang }, 150000);

export async function getSpeechUrl(text: string, lang: Lang): Promise<string> {
  const { url } = await post<{ url: string }>("/api/tts", { text, lang }, 60000);
  return `${BASE}${url}`;
}
