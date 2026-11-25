// Local morning reminders for the graft care plan (no server, no push service).
import * as Notifications from "expo-notifications";
import { Linking, Platform } from "react-native";

import { PLAN, PLAN_DAYS } from "@/src/graftPlan";
import type { Lang } from "@/src/i18n";

export const remindersSupported = Platform.OS !== "web";
const CHANNEL = "graft-reminders";
const HOUR = 8;

if (remindersSupported) {
  Notifications.setNotificationHandler({
    handleNotification: async () => ({ shouldShowBanner: true, shouldShowList: true, shouldPlaySound: true, shouldSetBadge: false }),
  });
}

export type ReminderPermission = "granted" | "ask" | "blocked" | "unsupported";

export async function reminderPermission(): Promise<ReminderPermission> {
  if (!remindersSupported) return "unsupported";
  const p = await Notifications.getPermissionsAsync();
  if (p.granted) return "granted";
  return p.canAskAgain ? "ask" : "blocked";
}

// Ask only after the farmer chose reminders; returns final state.
export async function requestReminderPermission(): Promise<ReminderPermission> {
  const current = await reminderPermission();
  if (current !== "ask") return current;
  const p = await Notifications.requestPermissionsAsync();
  if (p.granted) return "granted";
  return p.canAskAgain ? "ask" : "blocked";
}

export const openSettings = () => Linking.openSettings();

export async function scheduleReminders(batchId: string, name: string, startMs: number, lang: Lang): Promise<string[]> {
  if (!remindersSupported) return [];
  if (Platform.OS === "android") {
    await Notifications.setNotificationChannelAsync(CHANNEL, {
      name: lang === "hi" ? "कलम की याद" : "Graft reminders",
      importance: Notifications.AndroidImportance.HIGH,
    });
  }
  const ids: string[] = [];
  const now = Date.now();
  for (let day = 0; day <= PLAN_DAYS; day++) {
    const at = new Date(startMs);
    at.setDate(at.getDate() + day);
    at.setHours(HOUR, 0, 0, 0);
    if (at.getTime() <= now) continue;
    const task = PLAN[day];
    const id = await Notifications.scheduleNotificationAsync({
      content: {
        title: `${name} · ${lang === "hi" ? "दिन" : "Day"} ${day}`,
        body: task[lang],
        data: { batchId },
      },
      trigger: { type: Notifications.SchedulableTriggerInputTypes.DATE, date: at, channelId: CHANNEL },
    });
    ids.push(id);
  }
  return ids;
}

export async function cancelReminders(ids: string[]) {
  if (!remindersSupported) return;
  await Promise.all(ids.map((id) => Notifications.cancelScheduledNotificationAsync(id).catch(() => {})));
}
