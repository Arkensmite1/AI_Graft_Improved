import { Ionicons } from "@react-native-vector-icons/ionicons";
import { Image } from "expo-image";
import { Pressable, Text, View } from "react-native";

import { font } from "@/src/fonts";
import { useLang } from "@/src/i18n";
import type { Scan } from "@/src/scans";
import { makeStyles, useTheme } from "@/src/theme";
import { verdictStyle } from "@/src/verdict";

export function useDateLabel() {
  const { t, lang } = useLang();
  return (iso: string) => {
    const d = new Date(iso);
    const days = Math.floor((new Date().setHours(0, 0, 0, 0) - new Date(iso).setHours(0, 0, 0, 0)) / 86400000);
    const time = d.toLocaleTimeString(lang === "hi" ? "hi-IN" : "en-IN", { hour: "numeric", minute: "2-digit" });
    if (days === 0) return `${t.today}, ${time}`;
    if (days === 1) return `${t.yesterday}, ${time}`;
    return d.toLocaleDateString(lang === "hi" ? "hi-IN" : "en-IN", { day: "numeric", month: "short" });
  };
}

export function ScanRow({ scan, onPress, testID }: { scan: Scan; onPress: () => void; testID: string }) {
  const { t } = useLang();
  const { colors } = useTheme();
  const styles = useStyles();
  const v = verdictStyle(scan.result.verdict, colors);
  const dateLabel = useDateLabel();
  return (
    <Pressable testID={testID} onPress={onPress} style={({ pressed }) => [styles.row, pressed && styles.pressed]}>
      <Image source={{ uri: scan.thumb }} style={styles.thumb} contentFit="cover" />
      <View style={styles.mid}>
        <Text style={styles.title} numberOfLines={1}>
          {scan.result.title}
        </Text>
        <Text style={styles.sub} numberOfLines={1}>
          {t[scan.mode]} · {dateLabel(scan.createdAt)}
        </Text>
      </View>
      <View style={[styles.dot, { backgroundColor: v.bg }]}>
        <Ionicons name={v.icon} size={26} color={v.fg} />
      </View>
    </Pressable>
  );
}

const useStyles = makeStyles((c) => ({
  row: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: c.surface,
    borderRadius: 20,
    padding: 12,
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
  thumb: { width: 64, height: 64, borderRadius: 12, backgroundColor: c.surfaceTertiary },
  mid: { flex: 1 },
  title: { fontFamily: font.bold, fontSize: 18, lineHeight: 26, color: c.onSurface },
  sub: { fontFamily: font.regular, fontSize: 14, lineHeight: 20, color: c.muted },
  dot: { width: 48, height: 48, borderRadius: 24, alignItems: "center", justifyContent: "center" },
}));
