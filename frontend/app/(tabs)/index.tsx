import { Ionicons } from "@react-native-vector-icons/ionicons";
import * as Haptics from "expo-haptics";
import { Image } from "expo-image";
import { useRouter } from "expo-router";
import { useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, Text, View } from "react-native";
import Animated, { FadeInDown } from "react-native-reanimated";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { BatchCard } from "@/src/components/BatchCard";
import { LangToggle } from "@/src/components/LangToggle";
import { ScanRow } from "@/src/components/ScanRow";
import type { Mode } from "@/src/api";
import { font } from "@/src/fonts";
import { useLang } from "@/src/i18n";
import { usesNativeTabs } from "@/src/navigation";
import { isActive, useBatches } from "@/src/grafts";
import { pickPhoto, preparePhoto } from "@/src/photo";
import { useScans } from "@/src/scans";
import { makeStyles, useTheme } from "@/src/theme";

const SEEDLING_IMG = "https://images.unsplash.com/photo-1583263840577-b174af9ac796?crop=entropy&cs=srgb&fm=jpg&q=70&w=600";
const GUIDE_IMG = "https://images.unsplash.com/photo-1592150621744-aca64f48394a?crop=entropy&cs=srgb&fm=jpg&q=70&w=600";
const GRAFT_IMG = "https://images.unsplash.com/photo-1596199050105-6d5d32222916?crop=entropy&cs=srgb&fm=jpg&q=70&w=600";

export default function Home() {
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { t } = useLang();
  const { colors } = useTheme();
  const styles = useStyles();
  const { scans } = useScans();
  const { batches } = useBatches();
  const active = (batches ?? []).filter(isActive);
  const [busy, setBusy] = useState(false);
  const bottomChrome = usesNativeTabs ? insets.bottom : 0;

  const uploadPhoto = async (mode: Mode) => {
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    const uri = await pickPhoto();
    if (!uri) return;
    setBusy(true);
    try {
      await preparePhoto(uri, mode);
      router.push("/result");
    } catch (e) {
      console.warn("[home] upload failed", e);
    } finally {
      setBusy(false);
    }
  };

  const openScan = (mode: Mode) => {
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    router.push({ pathname: "/scan", params: { mode } });
  };

  return (
    <View style={styles.screen}>
      <View style={[styles.header, { paddingTop: insets.top + 8 }]}>
        <View style={styles.brandRow}>
          <View style={styles.logo}>
            <Ionicons name="leaf" size={22} color={colors.onBrandPrimary} />
          </View>
          <Text style={styles.brand} testID="app-name">
            {t.appName}
          </Text>
        </View>
        <LangToggle />
      </View>

      <ScrollView contentContainerStyle={[styles.content, { paddingBottom: bottomChrome + 24 }]} showsVerticalScrollIndicator={false}>
        <Animated.View entering={FadeInDown.duration(400)}>
          <Text style={styles.hello}>{t.hello}</Text>
          <Text style={styles.helloSub}>{t.helloSub}</Text>
        </Animated.View>

        {([
          { mode: "seedling", title: t.seedlingTitle, sub: t.seedlingSub, img: SEEDLING_IMG, icon: "leaf" },
          { mode: "guide", title: t.guideTitle, sub: t.guideSub, img: GUIDE_IMG, icon: "cut" },
          { mode: "graft", title: t.graftTitle, sub: t.graftSub, img: GRAFT_IMG, icon: "git-merge" },
        ] as const).map((m, i) => (
          <Animated.View key={m.mode} entering={FadeInDown.delay(100 + i * 100).duration(400)}>
            <View style={styles.modeCard}>
              <Pressable testID={`scan-${m.mode}-button`} onPress={() => openScan(m.mode)} style={({ pressed }) => [styles.modeTop, pressed && styles.pressed]}>
                <Image source={{ uri: m.img }} style={styles.modeImg} contentFit="cover" transition={200} />
                <View style={styles.modeBody}>
                  <Text style={styles.modeTitle}>{m.title}</Text>
                  <Text style={styles.modeSub}>{m.sub}</Text>
                </View>
              </Pressable>
              <View style={styles.modeActions}>
                <Pressable testID={`camera-${m.mode}-button`} onPress={() => openScan(m.mode)} style={({ pressed }) => [styles.modeBtn, styles.modeBtnPrimary, pressed && styles.pressed]}>
                  <Ionicons name="camera" size={26} color={colors.onBrandPrimary} />
                  <Text style={styles.modeBtnTextPrimary}>{t.takePhoto}</Text>
                </Pressable>
                <Pressable testID={`upload-${m.mode}-button`} onPress={() => uploadPhoto(m.mode)} style={({ pressed }) => [styles.modeBtn, styles.modeBtnSecondary, pressed && styles.pressed]}>
                  <Ionicons name="cloud-upload" size={26} color={colors.onBrandSecondary} />
                  <Text style={styles.modeBtnTextSecondary}>{t.upload}</Text>
                </Pressable>
              </View>
            </View>
          </Animated.View>
        ))}

        {active.length > 0 ? (
          <View style={styles.list} testID="home-today-work">
            <Text style={styles.section}>{t.todayWork}</Text>
            {active.slice(0, 2).map((b) => (
              <BatchCard key={b.id} batch={b} testID={`home-graft-${b.id}`} onPress={() => router.push({ pathname: "/graft/[id]", params: { id: b.id } })} />
            ))}
          </View>
        ) : (
          <Pressable testID="home-grafted-today" onPress={() => router.push("/graft/new")} style={({ pressed }) => [styles.tracker, pressed && styles.pressed]}>
            <Ionicons name="calendar" size={32} color={colors.onBrandPrimary} />
            <View style={styles.flex}>
              <Text style={styles.trackerTitle}>{t.graftedToday}</Text>
              <Text style={styles.trackerSub}>{t.graftedTodaySub}</Text>
            </View>
            <Ionicons name="add-circle" size={32} color={colors.onBrandPrimary} />
          </Pressable>
        )}

        <Animated.View entering={FadeInDown.delay(300).duration(400)}>
          <Pressable testID="tips-banner" onPress={() => router.push("/tips")} style={({ pressed }) => [styles.tips, pressed && styles.pressed]}>
            <Ionicons name="bulb" size={32} color={colors.onBrandSecondary} />
            <View style={styles.flex}>
              <Text style={styles.tipsTitle}>{t.tipsBanner}</Text>
              <Text style={styles.tipsSub}>{t.tipsBannerSub}</Text>
            </View>
            <Ionicons name="volume-high" size={28} color={colors.onBrandSecondary} />
          </Pressable>
        </Animated.View>

        <View style={styles.sectionRow}>
          <Text style={styles.section}>{t.recent}</Text>
          {!!scans?.length && (
            <Pressable testID="see-all-history" onPress={() => router.push("/history")} hitSlop={12} style={styles.seeAll}>
              <Text style={styles.seeAllText}>{t.seeAll}</Text>
            </Pressable>
          )}
        </View>

        {scans && scans.length === 0 && (
          <View style={styles.empty} testID="home-empty">
            <Ionicons name="images-outline" size={40} color={colors.muted} />
            <Text style={styles.emptyTitle}>{t.noScans}</Text>
            <Text style={styles.emptySub}>{t.noScansSub}</Text>
          </View>
        )}
        <View style={styles.list}>
          {scans?.slice(0, 3).map((s) => (
            <ScanRow key={s.id} scan={s} testID={`recent-scan-${s.id}`} onPress={() => router.push({ pathname: "/result", params: { id: s.id } })} />
          ))}
        </View>
      </ScrollView>

      {busy && (
        <View style={styles.busy} testID="home-preparing-overlay">
          <ActivityIndicator size="large" color={colors.brandTertiary} />
          <Text style={styles.busyText}>{t.preparing}</Text>
        </View>
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
    backgroundColor: c.surface,
    borderBottomWidth: 1,
    borderBottomColor: c.divider,
  },
  brandRow: { flexDirection: "row", alignItems: "center", gap: 8, flexShrink: 1 },
  logo: { width: 40, height: 40, borderRadius: 12, backgroundColor: c.brandPrimary, alignItems: "center", justifyContent: "center" },
  brand: { fontFamily: font.bold, fontSize: 20, lineHeight: 28, color: c.brand, flexShrink: 1 },
  content: { padding: 16, gap: 16 },
  hello: { fontFamily: font.bold, fontSize: 30, lineHeight: 40, color: c.onSurface },
  helloSub: { fontFamily: font.regular, fontSize: 18, lineHeight: 26, color: c.muted },
  modeCard: { backgroundColor: c.brandSecondary, borderRadius: 20, padding: 12, gap: 12, borderWidth: 2, borderColor: c.brandTertiary },
  modeTop: { flexDirection: "row", alignItems: "center", gap: 12 },
  modeActions: { flexDirection: "row", gap: 8 },
  modeBtn: { flex: 1, height: 56, borderRadius: 999, flexDirection: "row", gap: 6, alignItems: "center", justifyContent: "center" },
  modeBtnPrimary: { backgroundColor: c.brandPrimary },
  modeBtnSecondary: { backgroundColor: c.surface, borderWidth: 2, borderColor: c.brandPrimary },
  modeBtnTextPrimary: { fontFamily: font.bold, fontSize: 17, color: c.onBrandPrimary },
  modeBtnTextSecondary: { fontFamily: font.bold, fontSize: 17, color: c.onBrandSecondary },
  tracker: { flexDirection: "row", alignItems: "center", gap: 12, padding: 16, borderRadius: 20, backgroundColor: c.brand },
  trackerTitle: { fontFamily: font.bold, fontSize: 20, lineHeight: 28, color: c.onBrand },
  trackerSub: { fontFamily: font.regular, fontSize: 15, lineHeight: 20, color: c.onBrand, opacity: 0.9 },
  busy: { position: "absolute", top: 0, bottom: 0, left: 0, right: 0, backgroundColor: "rgba(0,0,0,0.7)", alignItems: "center", justifyContent: "center", gap: 12 },
  busyText: { fontFamily: font.bold, fontSize: 20, color: c.onSurfaceInverse },
  pressed: { transform: [{ scale: 0.97 }] },
  modeImg: { width: 80, height: 80, borderRadius: 16, backgroundColor: c.surfaceTertiary },
  modeBody: { flex: 1 },
  modeTitle: { fontFamily: font.bold, fontSize: 22, lineHeight: 30, color: c.onBrandSecondary },
  modeSub: { fontFamily: font.regular, fontSize: 16, lineHeight: 22, color: c.onSurfaceTertiary },
  tips: {
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    padding: 16,
    borderRadius: 20,
    backgroundColor: c.surfaceSecondary,
    borderWidth: 1,
    borderColor: c.border,
  },
  flex: { flex: 1 },
  tipsTitle: { fontFamily: font.bold, fontSize: 18, lineHeight: 26, color: c.onSurface },
  tipsSub: { fontFamily: font.regular, fontSize: 15, lineHeight: 20, color: c.muted },
  sectionRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "center", marginTop: 8 },
  section: { fontFamily: font.bold, fontSize: 22, lineHeight: 30, color: c.onSurface },
  seeAll: { minHeight: 44, justifyContent: "center" },
  seeAllText: { fontFamily: font.semi, fontSize: 16, color: c.brandPrimary },
  list: { gap: 12 },
  empty: { alignItems: "center", padding: 24, borderRadius: 20, borderWidth: 2, borderStyle: "dashed", borderColor: c.borderStrong, gap: 4 },
  emptyTitle: { fontFamily: font.bold, fontSize: 18, lineHeight: 26, color: c.onSurface },
  emptySub: { fontFamily: font.regular, fontSize: 15, lineHeight: 20, color: c.muted },
}));
