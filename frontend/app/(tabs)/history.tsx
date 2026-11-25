import { Ionicons } from "@react-native-vector-icons/ionicons";
import { useRouter } from "expo-router";
import { useState } from "react";
import { ActivityIndicator, FlatList, Pressable, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { ScanRow } from "@/src/components/ScanRow";
import { font } from "@/src/fonts";
import { useLang } from "@/src/i18n";
import { usesNativeTabs } from "@/src/navigation";
import { clearScans, useScans } from "@/src/scans";
import { makeStyles, useTheme } from "@/src/theme";

export default function History() {
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { t } = useLang();
  const { colors } = useTheme();
  const styles = useStyles();
  const { scans, refresh } = useScans();
  const [confirm, setConfirm] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const bottomChrome = usesNativeTabs ? insets.bottom : 0;

  return (
    <View style={styles.screen}>
      <View style={[styles.header, { paddingTop: insets.top + 8 }]}>
        <Text style={styles.title}>{t.history}</Text>
        {!!scans?.length &&
          (confirm ? (
            <View style={styles.confirmRow}>
              <Text style={styles.confirmText}>{t.sureDelete}</Text>
              <Pressable testID="confirm-delete-yes" style={[styles.smallBtn, styles.danger]} onPress={() => { clearScans(); setConfirm(false); }}>
                <Text style={styles.dangerText}>{t.yes}</Text>
              </Pressable>
              <Pressable testID="confirm-delete-no" style={styles.smallBtn} onPress={() => setConfirm(false)}>
                <Text style={styles.smallText}>{t.no}</Text>
              </Pressable>
            </View>
          ) : (
            <Pressable testID="delete-all-button" style={styles.smallBtn} onPress={() => setConfirm(true)}>
              <Ionicons name="trash-outline" size={20} color={colors.error} />
              <Text style={[styles.smallText, { color: colors.error }]}>{t.deleteAll}</Text>
            </Pressable>
          ))}
      </View>

      {scans === null ? (
        <ActivityIndicator style={styles.loader} size="large" color={colors.brandPrimary} />
      ) : (
        <FlatList
          testID="history-list"
          data={scans}
          keyExtractor={(s) => s.id}
          contentContainerStyle={[styles.list, { paddingBottom: bottomChrome + 24 }]}
          refreshing={refreshing}
          onRefresh={async () => {
            setRefreshing(true);
            await refresh();
            setRefreshing(false);
          }}
          renderItem={({ item }) => (
            <ScanRow scan={item} testID={`history-scan-${item.id}`} onPress={() => router.push({ pathname: "/result", params: { id: item.id } })} />
          )}
          ListEmptyComponent={
            <View style={styles.empty} testID="history-empty">
              <Ionicons name="leaf-outline" size={56} color={colors.brandPrimary} />
              <Text style={styles.emptyTitle}>{t.noScans}</Text>
              <Text style={styles.emptySub}>{t.noScansSub}</Text>
              <Pressable testID="history-scan-now" style={styles.cta} onPress={() => router.push({ pathname: "/scan", params: { mode: "seedling" } })}>
                <Ionicons name="camera" size={26} color={colors.onBrandPrimary} />
                <Text style={styles.ctaText}>{t.newPhoto}</Text>
              </Pressable>
            </View>
          }
        />
      )}
    </View>
  );
}

const useStyles = makeStyles((c) => ({
  screen: { flex: 1, backgroundColor: c.surface },
  header: {
    paddingHorizontal: 16,
    paddingBottom: 12,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    borderBottomWidth: 1,
    borderBottomColor: c.divider,
    minHeight: 56,
    gap: 8,
  },
  title: { fontFamily: font.bold, fontSize: 28, lineHeight: 38, color: c.onSurface },
  confirmRow: { flexDirection: "row", alignItems: "center", gap: 8, flexShrink: 1 },
  confirmText: { fontFamily: font.semi, fontSize: 14, color: c.onSurface, flexShrink: 1 },
  smallBtn: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    minHeight: 44,
    paddingHorizontal: 12,
    borderRadius: 999,
    backgroundColor: c.surfaceSecondary,
  },
  smallText: { fontFamily: font.semi, fontSize: 15, color: c.onSurface },
  danger: { backgroundColor: c.error },
  dangerText: { fontFamily: font.semi, fontSize: 15, color: c.onError },
  loader: { marginTop: 48 },
  list: { padding: 16, gap: 12, flexGrow: 1 },
  empty: { alignItems: "center", justifyContent: "center", flex: 1, gap: 8, paddingVertical: 48 },
  emptyTitle: { fontFamily: font.bold, fontSize: 22, lineHeight: 30, color: c.onSurface },
  emptySub: { fontFamily: font.regular, fontSize: 16, color: c.muted },
  cta: {
    marginTop: 16,
    flexDirection: "row",
    gap: 8,
    alignItems: "center",
    height: 60,
    paddingHorizontal: 28,
    borderRadius: 999,
    backgroundColor: c.brandPrimary,
  },
  ctaText: { fontFamily: font.bold, fontSize: 20, color: c.onBrandPrimary },
}));
