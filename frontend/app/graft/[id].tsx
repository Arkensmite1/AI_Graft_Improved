import { Ionicons } from "@react-native-vector-icons/ionicons";
import * as Haptics from "expo-haptics";
import { Image } from "expo-image";
import { Redirect, useFocusEffect, useLocalSearchParams, useRouter } from "expo-router";
import { useCallback, useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { SpeakButton } from "@/src/components/SpeakButton";
import { font } from "@/src/fonts";
import { PLAN, PLAN_DAYS, planIcons, taskFor } from "@/src/graftPlan";
import { dayOf, deleteBatch, enableReminders, toggleDone, useBatches } from "@/src/grafts";
import { useLang } from "@/src/i18n";
import { openSettings, reminderPermission, remindersSupported, requestReminderPermission, type ReminderPermission } from "@/src/reminders";
import { useScans } from "@/src/scans";
import { makeStyles, useTheme } from "@/src/theme";

export default function GraftDetail() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { t, lang } = useLang();
  const { colors } = useTheme();
  const styles = useStyles();
  const { batches } = useBatches();
  const { scans } = useScans();
  const [perm, setPerm] = useState<ReminderPermission>("unsupported");
  const [confirm, setConfirm] = useState(false);

  useFocusEffect(
    useCallback(() => {
      reminderPermission().then(setPerm);
    }, []),
  );

  const batch = batches?.find((b) => b.id === id);
  if (!batches) {
    return (
      <View style={[styles.screen, styles.center]}>
        <ActivityIndicator size="large" color={colors.brandPrimary} />
      </View>
    );
  }
  if (!batch) return <Redirect href="/grafts" />;

  const day = dayOf(batch);
  const finished = day > PLAN_DAYS;
  const task = taskFor(day);
  const doneToday = batch.done.includes(day);
  const photos = (scans ?? []).filter((s) => s.batchId === batch.id);

  const turnOnReminders = async () => {
    const p = perm === "blocked" ? "blocked" : await requestReminderPermission();
    setPerm(p);
    if (p === "granted") await enableReminders(batch, lang);
    else if (p === "blocked") openSettings();
  };

  return (
    <View style={styles.screen}>
      <View style={[styles.header, { paddingTop: insets.top + 8 }]}>
        <Pressable testID="graft-back-button" onPress={() => (router.canGoBack() ? router.back() : router.replace("/grafts"))} style={styles.iconBtn} hitSlop={8}>
          <Ionicons name="arrow-back" size={28} color={colors.onSurface} />
        </Pressable>
        <View style={styles.flex}>
          <Text style={styles.title} numberOfLines={1} testID="graft-detail-name">
            {batch.name}
          </Text>
          <Text style={styles.meta}>
            {batch.plants} {t.plants} · {finished ? t.finished : `${t.dayN} ${day} ${t.dayOf} ${PLAN_DAYS}`}
          </Text>
        </View>
      </View>

      <ScrollView contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 32 }]}>
        {!finished && (
          <View style={styles.today} testID="today-task-card">
            <Text style={styles.todayLabel}>{t.todayWork}</Text>
            <View style={styles.todayRow}>
              <View style={styles.todayIcon}>
                <Ionicons name={planIcons[task.icon] as never} size={40} color={colors.onBrandPrimary} />
              </View>
              <Text style={styles.todayText} testID="today-task-text">
                {task[lang]}
              </Text>
            </View>
            <SpeakButton id={`plan-${batch.id}-${day}-${lang}`} text={`${t.dayN} ${day}. ${task[lang]}`} testID="today-speak-button" />
            <View style={styles.todayActions}>
              <Pressable
                testID="today-done-button"
                onPress={() => {
                  Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
                  toggleDone(batch, day);
                }}
                style={[styles.actionBtn, doneToday ? styles.actionDone : styles.actionOutline]}
              >
                <Ionicons name={doneToday ? "checkmark-circle" : "ellipse-outline"} size={26} color={doneToday ? colors.onSuccess : colors.onSurface} />
                <Text style={[styles.actionText, doneToday && styles.actionTextOn]}>{doneToday ? t.doneToday : t.markDone}</Text>
              </Pressable>
              <Pressable
                testID="today-photo-button"
                onPress={() => router.push({ pathname: "/scan", params: { mode: "graft", batchId: batch.id } })}
                style={[styles.actionBtn, styles.actionOutline]}
              >
                <Ionicons name="camera" size={26} color={colors.onSurface} />
                <Text style={styles.actionText}>{t.takePhoto}</Text>
              </Pressable>
            </View>
          </View>
        )}

        {remindersSupported && !finished && (
          batch.reminders ? (
            <View style={styles.remindOn} testID="reminders-on">
              <Ionicons name="alarm" size={24} color={colors.onBrandSecondary} />
              <Text style={styles.remindOnText}>{t.remindersOn}</Text>
            </View>
          ) : (
            <View style={styles.remindOff} testID="reminders-off">
              <Ionicons name="notifications-off" size={26} color={colors.warning} />
              <Text style={styles.remindOffText}>{t.remindersOff}</Text>
              <Pressable testID="reminders-turn-on" onPress={turnOnReminders} style={styles.turnOn}>
                <Text style={styles.turnOnText}>{perm === "blocked" ? t.openSettings : t.turnOn}</Text>
              </Pressable>
            </View>
          )
        )}

        {photos.length > 0 && (
          <>
            <Text style={styles.section}>{t.photos}</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.photoRow}>
              {photos.map((s) => (
                <Pressable key={s.id} testID={`graft-photo-${s.id}`} onPress={() => router.push({ pathname: "/result", params: { id: s.id } })}>
                  <Image source={{ uri: s.thumb }} style={styles.photo} contentFit="cover" />
                </Pressable>
              ))}
            </ScrollView>
          </>
        )}

        <Text style={styles.section}>{t.allDays}</Text>
        <View style={styles.timeline}>
          {PLAN.map((p) => {
            const isToday = p.day === day;
            const past = p.day < day;
            const done = batch.done.includes(p.day);
            return (
              <Pressable
                key={p.day}
                testID={`plan-day-${p.day}`}
                disabled={p.day > day}
                onPress={() => toggleDone(batch, p.day)}
                style={[styles.dayRow, isToday && styles.dayRowToday, p.day > day && styles.future]}
              >
                <View style={[styles.dayDot, done ? styles.dotDone : isToday ? styles.dotToday : past ? styles.dotMissed : null]}>
                  {done ? (
                    <Ionicons name="checkmark" size={22} color={colors.onSuccess} />
                  ) : (
                    <Text style={[styles.dayNum, isToday && styles.dayNumToday]}>{p.day}</Text>
                  )}
                </View>
                <Ionicons name={planIcons[p.icon] as never} size={24} color={p.milestone ? colors.brandPrimary : colors.muted} />
                <Text style={[styles.dayText, p.milestone && styles.dayTextStrong]}>{p[lang]}</Text>
              </Pressable>
            );
          })}
        </View>

        {confirm ? (
          <View style={styles.confirmRow}>
            <Text style={styles.confirmText}>{t.sureDeleteGraft}</Text>
            <Pressable testID="confirm-delete-graft-yes" style={[styles.smallBtn, styles.danger]} onPress={() => deleteBatch(batch)}>
              <Text style={styles.dangerText}>{t.yes}</Text>
            </Pressable>
            <Pressable testID="confirm-delete-graft-no" style={styles.smallBtn} onPress={() => setConfirm(false)}>
              <Text style={styles.smallText}>{t.no}</Text>
            </Pressable>
          </View>
        ) : (
          <Pressable testID="delete-graft-button" style={styles.deleteBtn} onPress={() => setConfirm(true)}>
            <Ionicons name="trash-outline" size={22} color={colors.error} />
            <Text style={styles.deleteText}>{t.deleteGraft}</Text>
          </Pressable>
        )}
      </ScrollView>
    </View>
  );
}

const useStyles = makeStyles((c) => ({
  screen: { flex: 1, backgroundColor: c.surface },
  center: { alignItems: "center", justifyContent: "center" },
  header: { flexDirection: "row", alignItems: "center", gap: 12, paddingHorizontal: 16, paddingBottom: 12, borderBottomWidth: 1, borderBottomColor: c.divider },
  iconBtn: { width: 48, height: 48, borderRadius: 24, alignItems: "center", justifyContent: "center", backgroundColor: c.surfaceSecondary },
  flex: { flex: 1 },
  title: { fontFamily: font.bold, fontSize: 24, lineHeight: 32, color: c.onSurface },
  meta: { fontFamily: font.semi, fontSize: 15, lineHeight: 20, color: c.muted },
  content: { padding: 16, gap: 12 },
  today: { backgroundColor: c.brandSecondary, borderRadius: 24, padding: 16, gap: 12, borderWidth: 2, borderColor: c.brandTertiary },
  todayLabel: { fontFamily: font.bold, fontSize: 18, lineHeight: 24, color: c.onBrandSecondary },
  todayRow: { flexDirection: "row", alignItems: "center", gap: 12 },
  todayIcon: { width: 72, height: 72, borderRadius: 20, backgroundColor: c.brandPrimary, alignItems: "center", justifyContent: "center" },
  todayText: { flex: 1, fontFamily: font.bold, fontSize: 22, lineHeight: 30, color: c.onSurface },
  todayActions: { flexDirection: "row", gap: 12 },
  actionBtn: { flex: 1, height: 56, borderRadius: 999, flexDirection: "row", gap: 6, alignItems: "center", justifyContent: "center" },
  actionOutline: { backgroundColor: c.surface, borderWidth: 2, borderColor: c.borderStrong },
  actionDone: { backgroundColor: c.success },
  actionText: { fontFamily: font.bold, fontSize: 17, color: c.onSurface },
  actionTextOn: { color: c.onSuccess },
  remindOn: { flexDirection: "row", alignItems: "center", gap: 8, paddingHorizontal: 16, height: 48, borderRadius: 999, backgroundColor: c.brandSecondary, alignSelf: "flex-start" },
  remindOnText: { fontFamily: font.semi, fontSize: 15, color: c.onBrandSecondary },
  remindOff: { flexDirection: "row", alignItems: "center", gap: 8, padding: 12, borderRadius: 16, backgroundColor: c.surfaceSecondary, borderWidth: 1, borderColor: c.warning },
  remindOffText: { flex: 1, fontFamily: font.semi, fontSize: 16, color: c.onSurface },
  turnOn: { minHeight: 44, paddingHorizontal: 16, borderRadius: 999, backgroundColor: c.brandPrimary, justifyContent: "center" },
  turnOnText: { fontFamily: font.bold, fontSize: 15, color: c.onBrandPrimary },
  section: { fontFamily: font.bold, fontSize: 22, lineHeight: 30, color: c.onSurface, marginTop: 8 },
  photoRow: { gap: 8 },
  photo: { width: 88, height: 88, borderRadius: 16, backgroundColor: c.surfaceTertiary },
  timeline: { gap: 6 },
  dayRow: { flexDirection: "row", alignItems: "center", gap: 12, minHeight: 56, paddingHorizontal: 8, borderRadius: 16 },
  dayRowToday: { backgroundColor: c.surfaceSecondary, borderWidth: 2, borderColor: c.brandPrimary },
  future: { opacity: 0.55 },
  dayDot: { width: 40, height: 40, borderRadius: 20, borderWidth: 2, borderColor: c.borderStrong, alignItems: "center", justifyContent: "center", backgroundColor: c.surface },
  dotDone: { backgroundColor: c.success, borderColor: c.success },
  dotToday: { backgroundColor: c.brandPrimary, borderColor: c.brandPrimary },
  dotMissed: { borderColor: c.warning },
  dayNum: { fontFamily: font.bold, fontSize: 16, color: c.onSurface },
  dayNumToday: { color: c.onBrandPrimary },
  dayText: { flex: 1, fontFamily: font.regular, fontSize: 16, lineHeight: 22, color: c.onSurface },
  dayTextStrong: { fontFamily: font.bold },
  deleteBtn: { flexDirection: "row", alignItems: "center", justifyContent: "center", gap: 8, height: 52, marginTop: 16 },
  deleteText: { fontFamily: font.semi, fontSize: 16, color: c.error },
  confirmRow: { flexDirection: "row", alignItems: "center", gap: 8, marginTop: 16, justifyContent: "center" },
  confirmText: { fontFamily: font.semi, fontSize: 16, color: c.onSurface, flexShrink: 1 },
  smallBtn: { minHeight: 44, paddingHorizontal: 16, borderRadius: 999, backgroundColor: c.surfaceSecondary, justifyContent: "center" },
  smallText: { fontFamily: font.semi, fontSize: 15, color: c.onSurface },
  danger: { backgroundColor: c.error },
  dangerText: { fontFamily: font.semi, fontSize: 15, color: c.onError },
}));
