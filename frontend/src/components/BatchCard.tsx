import { Ionicons } from "@react-native-vector-icons/ionicons";
import * as Haptics from "expo-haptics";
import { Pressable, Text, View } from "react-native";

import { font } from "@/src/fonts";
import { PLAN_DAYS, planIcons, taskFor } from "@/src/graftPlan";
import { type Batch, dayOf, toggleDone } from "@/src/grafts";
import { useLang } from "@/src/i18n";
import { makeStyles, useTheme } from "@/src/theme";

export function BatchCard({ batch, onPress, testID }: { batch: Batch; onPress: () => void; testID: string }) {
  const { t, lang } = useLang();
  const { colors } = useTheme();
  const styles = useStyles();
  const day = dayOf(batch);
  const finished = day > PLAN_DAYS;
  const task = taskFor(day);
  const done = batch.done.includes(day);
  const progress = Math.min(1, (day + 1) / (PLAN_DAYS + 1));

  return (
    <Pressable testID={testID} onPress={onPress} style={({ pressed }) => [styles.card, pressed && styles.pressed]}>
      <View style={styles.top}>
        <View style={styles.flex}>
          <Text style={styles.name} numberOfLines={1}>
            {batch.name}
          </Text>
          <Text style={styles.meta}>
            {batch.plants} {t.plants} · {finished ? t.finished : `${t.dayN} ${day} ${t.dayOf} ${PLAN_DAYS}`}
          </Text>
        </View>
        <Ionicons name="chevron-forward" size={26} color={colors.muted} />
      </View>
      <View style={styles.bar}>
        <View style={[styles.barFill, { width: `${progress * 100}%` }]} />
      </View>
      {!finished && (
        <View style={styles.task}>
          <View style={[styles.taskIcon, task.milestone && styles.taskIconStrong]}>
            <Ionicons name={planIcons[task.icon] as never} size={28} color={task.milestone ? colors.onBrandPrimary : colors.onBrandSecondary} />
          </View>
          <Text style={styles.taskText}>{task[lang]}</Text>
          <Pressable
            testID={`${testID}-done-toggle`}
            hitSlop={8}
            onPress={() => {
              Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
              toggleDone(batch, day);
            }}
            style={[styles.check, done && styles.checkOn]}
          >
            <Ionicons name="checkmark" size={28} color={done ? colors.onSuccess : colors.borderStrong} />
          </Pressable>
        </View>
      )}
    </Pressable>
  );
}

const useStyles = makeStyles((c) => ({
  card: {
    backgroundColor: c.surface,
    borderRadius: 20,
    padding: 16,
    gap: 12,
    borderWidth: 1,
    borderColor: c.border,
    shadowColor: c.onSurface,
    shadowOpacity: 0.06,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 3 },
    elevation: 2,
  },
  pressed: { transform: [{ scale: 0.98 }] },
  top: { flexDirection: "row", alignItems: "center", gap: 8 },
  flex: { flex: 1 },
  name: { fontFamily: font.bold, fontSize: 20, lineHeight: 28, color: c.onSurface },
  meta: { fontFamily: font.semi, fontSize: 15, lineHeight: 20, color: c.muted },
  bar: { height: 10, borderRadius: 5, backgroundColor: c.surfaceTertiary, overflow: "hidden" },
  barFill: { height: "100%", backgroundColor: c.brandPrimary, borderRadius: 5 },
  task: { flexDirection: "row", alignItems: "center", gap: 12, backgroundColor: c.surfaceSecondary, borderRadius: 16, padding: 8 },
  taskIcon: { width: 52, height: 52, borderRadius: 14, backgroundColor: c.brandSecondary, alignItems: "center", justifyContent: "center" },
  taskIconStrong: { backgroundColor: c.brandPrimary },
  taskText: { flex: 1, fontFamily: font.semi, fontSize: 17, lineHeight: 24, color: c.onSurface },
  check: { width: 52, height: 52, borderRadius: 26, borderWidth: 3, borderColor: c.borderStrong, alignItems: "center", justifyContent: "center" },
  checkOn: { backgroundColor: c.success, borderColor: c.success },
}));
