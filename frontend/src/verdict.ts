import type { StepIcon, Verdict } from "@/src/api";
import type { ThemeColors } from "@/src/theme";

type Ion = "checkmark-circle" | "time" | "alert-circle" | "camera";

export function verdictStyle(v: Verdict, c: ThemeColors) {
  switch (v) {
    case "good":
      return { bg: c.success, fg: c.onSuccess, icon: "checkmark-circle" as Ion, key: "vGood" as const };
    case "wait":
      return { bg: c.warning, fg: c.onWarning, icon: "time" as Ion, key: "vWait" as const };
    case "bad":
      return { bg: c.error, fg: c.onError, icon: "alert-circle" as Ion, key: "vBad" as const };
    default:
      return { bg: c.info, fg: c.onInfo, icon: "camera" as Ion, key: "vUnclear" as const };
  }
}

export const stepIcons: Record<StepIcon, string> = {
  water: "water",
  sun: "sunny",
  shade: "umbrella",
  cover: "cube",
  clock: "time",
  cut: "cut",
  leaf: "leaf",
  warning: "warning",
  check: "checkmark-circle",
  camera: "camera",
  remove: "trash",
  medicine: "medkit",
  thermometer: "thermometer",
  wind: "cloudy",
};
