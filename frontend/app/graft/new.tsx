import { Ionicons } from "@react-native-vector-icons/ionicons";
import * as Haptics from "expo-haptics";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, Switch, Text, TextInput, View } from "react-native";
import { KeyboardAwareScrollView, KeyboardStickyView } from "react-native-keyboard-controller";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { font } from "@/src/fonts";
import { createBatch, useBatches } from "@/src/grafts";
import { useLang } from "@/src/i18n";
import { remindersSupported, requestReminderPermission } from "@/src/reminders";
import { updateScan } from "@/src/scans";
import { makeStyles, useTheme } from "@/src/theme";

const DAY_OPTIONS = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

export default function NewGraft() {
  const params = useLocalSearchParams<{ days?: string; scanId?: string }>();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { t, lang } = useLang();
  const { colors } = useTheme();
  const styles = useStyles();
  const { batches } = useBatches();
  const [name, setName] = useState("");
  const [plants, setPlants] = useState(10);
  const [daysAgo, setDaysAgo] = useState(Math.min(10, Math.max(0, Number(params.days ?? 0) || 0)));
  const [remind, setRemind] = useState(remindersSupported);
  const [saving, setSaving] = useState(false);

  const fallbackName = `${t.batch} ${(batches?.length ?? 0) + 1}`;
  const dayLabel = (d: number) => (d === 0 ? t.today : d === 1 ? t.yesterday : `${d} ${t.daysAgo}`);

  const save = async () => {
    if (saving) return;
    setSaving(true);
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    const allowed = remind ? (await requestReminderPermission()) === "granted" : false;
    const batch = await createBatch({ name: name.trim() || fallbackName, plants, daysAgo, reminders: allowed, lang });
    if (params.scanId) await updateScan(params.scanId, { batchId: batch.id });
    router.replace({ pathname: "/graft/[id]", params: { id: batch.id } });
  };

  return (
    <View style={styles.screen}>
      <View style={[styles.header, { paddingTop: insets.top + 8 }]}>
        <Pressable testID="new-graft-close" onPress={() => router.back()} style={styles.iconBtn} hitSlop={8}>
          <Ionicons name="close" size={28} color={colors.onSurface} />
        </Pressable>
        <Text style={styles.title}>{t.newGraft}</Text>
      </View>

      <KeyboardAwareScrollView contentContainerStyle={styles.content} bottomOffset={100} keyboardShouldPersistTaps="handled">
        <Text style={styles.label}>{t.whenGrafted}</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chipRow} style={styles.chipScroll}>
          {DAY_OPTIONS.map((d) => (
            <Pressable
              key={d}
              testID={`days-ago-chip-${d}`}
              onPress={() => {
                Haptics.selectionAsync();
                setDaysAgo(d);
              }}
              style={[styles.chip, daysAgo === d && styles.chipOn]}
            >
              <Text style={[styles.chipText, daysAgo === d && styles.chipTextOn]}>{dayLabel(d)}</Text>
            </Pressable>
          ))}
        </ScrollView>

        <Text style={styles.label}>{t.plants}</Text>
        <View style={styles.stepper}>
          <Pressable testID="plants-minus" style={styles.stepBtn} onPress={() => setPlants((p) => Math.max(1, p - (p > 10 ? 5 : 1)))}>
            <Ionicons name="remove" size={32} color={colors.onBrandSecondary} />
          </Pressable>
          <Text style={styles.stepValue} testID="plants-count">
            {plants}
          </Text>
          <Pressable testID="plants-plus" style={styles.stepBtn} onPress={() => setPlants((p) => p + (p >= 10 ? 5 : 1))}>
            <Ionicons name="add" size={32} color={colors.onBrandSecondary} />
          </Pressable>
        </View>

        <Text style={styles.label}>{t.graftName}</Text>
        <TextInput
          testID="graft-name-input"
          value={name}
          onChangeText={setName}
          placeholder={fallbackName}
          placeholderTextColor={colors.muted}
          style={styles.input}
          maxLength={30}
          returnKeyType="done"
        />

        <View style={styles.remind}>
          <Ionicons name="alarm" size={32} color={colors.brandPrimary} />
          <View style={styles.flex}>
            <Text style={styles.remindTitle}>{t.remindMe}</Text>
            <Text style={styles.remindSub}>{remindersSupported ? t.remindMeSub : t.notOnWeb}</Text>
          </View>
          <Switch
            testID="reminder-switch"
            value={remind}
            onValueChange={setRemind}
            disabled={!remindersSupported}
            trackColor={{ true: colors.brandPrimary, false: colors.borderStrong }}
            thumbColor={colors.surface}
          />
        </View>
      </KeyboardAwareScrollView>

      <KeyboardStickyView offset={{ closed: 0, opened: insets.bottom }}>
        <View style={[styles.footer, { paddingBottom: insets.bottom + 12 }]}>
          <Pressable testID="save-graft-button" onPress={save} disabled={saving} style={({ pressed }) => [styles.saveBtn, pressed && styles.pressed]}>
            {saving ? <ActivityIndicator color={colors.onBrandPrimary} /> : <Ionicons name="checkmark" size={28} color={colors.onBrandPrimary} />}
            <Text style={styles.saveText}>{t.save}</Text>
          </Pressable>
        </View>
      </KeyboardStickyView>
    </View>
  );
}

const useStyles = makeStyles((c) => ({
  screen: { flex: 1, backgroundColor: c.surface },
  header: { flexDirection: "row", alignItems: "center", gap: 8, paddingHorizontal: 16, paddingBottom: 12, borderBottomWidth: 1, borderBottomColor: c.divider },
  iconBtn: { width: 48, height: 48, borderRadius: 24, alignItems: "center", justifyContent: "center", backgroundColor: c.surfaceSecondary },
  title: { fontFamily: font.bold, fontSize: 26, lineHeight: 36, color: c.onSurface },
  content: { padding: 16, gap: 12, paddingBottom: 32 },
  label: { fontFamily: font.bold, fontSize: 20, lineHeight: 28, color: c.onSurface, marginTop: 8 },
  chipScroll: { flexGrow: 0, height: 56, marginHorizontal: -16 },
  chipRow: { gap: 8, paddingHorizontal: 16, alignItems: "center" },
  chip: { flexShrink: 0, height: 44, paddingHorizontal: 16, borderRadius: 999, borderWidth: 2, borderColor: c.borderStrong, backgroundColor: c.surface, justifyContent: "center" },
  chipOn: { backgroundColor: c.brandPrimary, borderColor: c.brandPrimary },
  chipText: { fontFamily: font.semi, fontSize: 16, color: c.onSurface },
  chipTextOn: { color: c.onBrandPrimary },
  stepper: { flexDirection: "row", alignItems: "center", gap: 16 },
  stepBtn: { width: 64, height: 64, borderRadius: 32, backgroundColor: c.brandSecondary, alignItems: "center", justifyContent: "center" },
  stepValue: { fontFamily: font.bold, fontSize: 32, lineHeight: 42, color: c.onSurface, minWidth: 64, textAlign: "center" },
  input: {
    height: 56,
    borderRadius: 16,
    borderWidth: 2,
    borderColor: c.borderStrong,
    paddingHorizontal: 16,
    fontFamily: font.semi,
    fontSize: 18,
    color: c.onSurface,
    backgroundColor: c.surface,
  },
  remind: { flexDirection: "row", alignItems: "center", gap: 12, padding: 16, borderRadius: 20, backgroundColor: c.surfaceSecondary, marginTop: 8 },
  flex: { flex: 1 },
  remindTitle: { fontFamily: font.bold, fontSize: 18, lineHeight: 26, color: c.onSurface },
  remindSub: { fontFamily: font.regular, fontSize: 15, lineHeight: 20, color: c.muted },
  footer: { paddingHorizontal: 16, paddingTop: 12, backgroundColor: c.surface, borderTopWidth: 1, borderTopColor: c.border },
  saveBtn: { height: 64, borderRadius: 999, backgroundColor: c.brandPrimary, flexDirection: "row", gap: 8, alignItems: "center", justifyContent: "center" },
  saveText: { fontFamily: font.bold, fontSize: 22, color: c.onBrandPrimary },
  pressed: { transform: [{ scale: 0.97 }] },
}));
