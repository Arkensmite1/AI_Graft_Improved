import { Ionicons } from "@react-native-vector-icons/ionicons";
import * as Haptics from "expo-haptics";
import { useRouter } from "expo-router";
import { ActivityIndicator, FlatList, Pressable, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { BatchCard } from "@/src/components/BatchCard";
import { font } from "@/src/fonts";
import { useBatches } from "@/src/grafts";
import { useLang } from "@/src/i18n";
import { usesNativeTabs } from "@/src/navigation";
import { makeStyles, useTheme } from "@/src/theme";

export default function Grafts() {
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { t } = useLang();
  const { colors } = useTheme();
  const styles = useStyles();
  const { batches } = useBatches();
  const bottomChrome = usesNativeTabs ? insets.bottom : 0;

  return (
    <View style={styles.screen}>
      <View style={[styles.header, { paddingTop: insets.top + 8 }]}>
        <Text style={styles.title}>{t.grafts}</Text>
      </View>
      <FlatList
        testID="grafts-list"
        data={batches ?? []}
        keyExtractor={(b) => b.id}
        contentContainerStyle={[styles.list, { paddingBottom: bottomChrome + 24 }]}
        ListHeaderComponent={
          <Pressable
            testID="grafted-today-button"
            onPress={() => {
              Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
              router.push("/graft/new");
            }}
            style={({ pressed }) => [styles.cta, pressed && styles.pressed]}
          >
            <View style={styles.ctaIcon}>
              <Ionicons name="add" size={36} color={colors.brandPrimary} />
            </View>
            <View style={styles.flex}>
              <Text style={styles.ctaTitle}>{t.graftedToday}</Text>
              <Text style={styles.ctaSub}>{t.graftedTodaySub}</Text>
            </View>
          </Pressable>
        }
        renderItem={({ item }) => (
          <BatchCard batch={item} testID={`graft-card-${item.id}`} onPress={() => router.push({ pathname: "/graft/[id]", params: { id: item.id } })} />
        )}
        ListEmptyComponent={
          batches === null ? (
            <ActivityIndicator size="large" color={colors.brandPrimary} style={styles.loader} />
          ) : (
            <View style={styles.empty} testID="grafts-empty">
              <Ionicons name="calendar-outline" size={56} color={colors.brandPrimary} />
              <Text style={styles.emptyTitle}>{t.noGrafts}</Text>
              <Text style={styles.emptySub}>{t.noGraftsSub}</Text>
            </View>
          )
        }
      />
    </View>
  );
}

const useStyles = makeStyles((c) => ({
  screen: { flex: 1, backgroundColor: c.surface },
  header: { paddingHorizontal: 16, paddingBottom: 12, borderBottomWidth: 1, borderBottomColor: c.divider },
  title: { fontFamily: font.bold, fontSize: 28, lineHeight: 38, color: c.onSurface },
  list: { padding: 16, gap: 12, flexGrow: 1 },
  cta: { flexDirection: "row", alignItems: "center", gap: 16, backgroundColor: c.brandPrimary, borderRadius: 20, padding: 16, minHeight: 96 },
  pressed: { transform: [{ scale: 0.97 }] },
  ctaIcon: { width: 60, height: 60, borderRadius: 30, backgroundColor: c.surface, alignItems: "center", justifyContent: "center" },
  flex: { flex: 1 },
  ctaTitle: { fontFamily: font.bold, fontSize: 22, lineHeight: 30, color: c.onBrandPrimary },
  ctaSub: { fontFamily: font.regular, fontSize: 16, lineHeight: 22, color: c.onBrandPrimary, opacity: 0.9 },
  loader: { marginTop: 32 },
  empty: { alignItems: "center", gap: 8, paddingVertical: 48 },
  emptyTitle: { fontFamily: font.bold, fontSize: 22, lineHeight: 30, color: c.onSurface },
  emptySub: { fontFamily: font.regular, fontSize: 16, color: c.muted, textAlign: "center" },
}));
