import { Ionicons } from "@react-native-vector-icons/ionicons";
import * as Haptics from "expo-haptics";
import { Image } from "expo-image";
import { LinearGradient } from "expo-linear-gradient";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useCallback, useEffect, useRef, useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, Text, View } from "react-native";
import Animated, { FadeInDown, useAnimatedStyle, useSharedValue, withRepeat, withTiming, Easing } from "react-native-reanimated";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { analyzePhoto, type AnalyzeResult } from "@/src/api";
import { GuideOverlay } from "@/src/components/GuideOverlay";
import { SpeakButton } from "@/src/components/SpeakButton";
import { font } from "@/src/fonts";
import { useLang } from "@/src/i18n";
import { addScan, getPending, loadScans, setPending, type Scan } from "@/src/scans";
import { speak } from "@/src/speech";
import { makeStyles, useTheme } from "@/src/theme";
import { stepIcons, verdictStyle } from "@/src/verdict";

type Phase = "loading" | "error" | "done";

export default function Result() {
  const { id } = useLocalSearchParams<{ id?: string }>();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { t, lang } = useLang();
  const styles = useStyles();
  const [scan, setScan] = useState<Scan | null>(null);
  const [phase, setPhase] = useState<Phase>("loading");
  const [active, setActive] = useState<number | null>(null);
  const pending = useRef(getPending()).current;
  const started = useRef(false);

  const runAnalysis = useCallback(async () => {
    if (!pending) return;
    setPhase("loading");
    try {
      const result = await analyzePhoto(pending.mode, pending.base64, lang);
      const s: Scan = { id: result.id, mode: pending.mode, createdAt: result.created_at, thumb: pending.thumb, photo: pending.photo, aspect: pending.aspect, result, batchId: pending.batchId };
      await addScan(s);
      setScan(s);
      setPhase("done");
      setPending(null);
      Haptics.notificationAsync(result.verdict === "bad" ? Haptics.NotificationFeedbackType.Error : Haptics.NotificationFeedbackType.Success);
      speak(`result-${result.id}`, result.speak, result.lang); // auto read-aloud for farmers who can't read
    } catch (e) {
      console.warn("[result] analyze failed", e);
      setPhase("error");
    }
  }, [pending, lang]);

  useEffect(() => {
    if (started.current) return;
    started.current = true;
    if (id) {
      loadScans().then((list) => {
        const found = list.find((s) => s.id === id);
        if (found) {
          setScan(found);
          setPhase("done");
        } else setPhase("error");
      });
    } else if (pending) runAnalysis();
    else router.dismissTo("/");
  }, [id, pending, runAnalysis, router]);

  const photoUri = scan ? scan.photo : pending?.uri;
  const mode = scan?.mode ?? pending?.mode ?? "seedling";
  const batchId = scan?.batchId ?? pending?.batchId;
  const aspect = scan?.aspect ?? pending?.aspect;
  const marks = phase === "done" ? scan?.result.marks ?? [] : [];
  const stepOf = (markIndex: number) => {
    const i = scan?.result.steps.findIndex((st) => st.mark === markIndex) ?? -1;
    return i >= 0 ? i : null;
  };

  return (
    <View style={styles.screen}>
      <ScrollView contentContainerStyle={{ paddingBottom: 120 + insets.bottom }} bounces={false}>
        <View style={[styles.photoWrap, aspect ? { aspectRatio: Math.max(0.6, aspect) } : null]}>
          {photoUri ? <Image source={{ uri: photoUri }} style={styles.photo} contentFit="cover" testID="result-photo" /> : <View style={styles.photo} />}
          {marks.length > 0 && <GuideOverlay marks={marks} stepOf={stepOf} active={active} />}
          {phase === "loading" && <ScanLine />}
          <LinearGradient colors={["rgba(0,0,0,0.55)", "transparent"]} style={styles.topScrim} />
          <Pressable testID="result-back-button" onPress={() => router.dismissTo("/")} style={[styles.back, { top: insets.top + 8 }]} hitSlop={8}>
            <Ionicons name="arrow-back" size={28} color="#FFFFFF" />
          </Pressable>
        </View>

        <View style={styles.body}>
          {phase === "loading" && <Loading />}
          {phase === "error" && <ErrorBox onRetry={pending ? runAnalysis : undefined} />}
          {phase === "done" && scan && <ResultBody result={scan.result} mode={mode} active={active} onStep={setActive} />}
          {phase === "done" && scan && mode === "graft" && !scan.batchId && scan.result.verdict !== "unclear" && (
            <Pressable
              testID="track-graft-button"
              style={({ pressed }) => [styles.track, pressed && styles.pressed]}
              onPress={() =>
                router.push({ pathname: "/graft/new", params: { scanId: scan.id, days: String(scan.result.facts.healing_day ?? 0) } })
              }
            >
              <Ionicons name="calendar" size={26} style={styles.trackIcon} />
              <Text style={styles.trackText}>{t.trackGraft}</Text>
            </Pressable>
          )}
        </View>
      </ScrollView>

      {phase !== "loading" && (
        <View style={[styles.footer, { paddingBottom: insets.bottom + 12 }]}>
          <Pressable
            testID="new-photo-button"
            style={({ pressed }) => [styles.footerBtn, styles.footerSecondary, pressed && styles.pressed]}
            onPress={() => router.replace({ pathname: "/scan", params: { mode, ...(batchId ? { batchId } : {}) } })}
          >
            <Ionicons name="camera" size={26} style={styles.secondaryIcon} />
            <Text style={styles.footerSecondaryText}>{t.newPhoto}</Text>
          </Pressable>
          <Pressable testID="done-button" style={({ pressed }) => [styles.footerBtn, styles.footerPrimary, pressed && styles.pressed]} onPress={() => router.dismissTo("/")}>
            <Ionicons name="checkmark" size={26} style={styles.primaryIcon} />
            <Text style={styles.footerPrimaryText}>{t.done}</Text>
          </Pressable>
        </View>
      )}
    </View>
  );
}

function ScanLine() {
  const styles = useStyles();
  const y = useSharedValue(0);
  useEffect(() => {
    y.value = withRepeat(withTiming(1, { duration: 1600, easing: Easing.inOut(Easing.quad) }), -1, true);
  }, [y]);
  const anim = useAnimatedStyle(() => ({ top: `${y.value * 92 + 4}%` }));
  return <Animated.View style={[styles.scanLine, anim]} />;
}

function Loading() {
  const { t } = useLang();
  const { colors } = useTheme();
  const styles = useStyles();
  return (
    <View style={styles.loading} testID="analyzing-indicator">
      <ActivityIndicator size="large" color={colors.brandPrimary} />
      <Text style={styles.loadingText}>{t.checking}</Text>
    </View>
  );
}

function ErrorBox({ onRetry }: { onRetry?: () => void }) {
  const { t } = useLang();
  const { colors } = useTheme();
  const styles = useStyles();
  return (
    <View style={styles.errorBox} testID="analysis-error">
      <Ionicons name="cloud-offline" size={56} color={colors.error} />
      <Text style={styles.errorTitle}>{t.failTitle}</Text>
      <Text style={styles.errorText}>{t.failText}</Text>
      {onRetry && (
        <Pressable testID="retry-button" style={styles.retry} onPress={onRetry}>
          <Ionicons name="refresh" size={26} color={colors.onBrandPrimary} />
          <Text style={styles.retryText}>{t.tryAgain}</Text>
        </Pressable>
      )}
    </View>
  );
}

type BodyProps = { result: AnalyzeResult; mode: string; active: number | null; onStep: (i: number | null) => void };

function ResultBody({ result, mode, active, onStep }: BodyProps) {
  const hasMarks = (result.marks?.length ?? 0) > 0;
  const { t } = useLang();
  const { colors } = useTheme();
  const styles = useStyles();
  const v = verdictStyle(result.verdict, colors);
  const f = result.facts;
  const stemLabel = f.stem === "thin" ? t.stemThin : f.stem === "thick" ? t.stemThick : f.stem === "ok" ? t.stemOk : null;
  const chips = [
    f.leaves != null && f.leaves > 0 && { icon: "leaf", text: `${t.leaves}: ${f.leaves}` },
    stemLabel && { icon: "resize", text: `${t.stem}: ${stemLabel}` },
    f.days_wait != null && f.days_wait > 0 && { icon: "time", text: `${t.waitFor} ${f.days_wait} ${t.days}` },
    mode === "graft" && f.healing_day != null && { icon: "calendar", text: `${t.dayN} ${f.healing_day}` },
  ].filter(Boolean) as { icon: string; text: string }[];

  return (
    <View style={styles.resultGap}>
      <Animated.View entering={FadeInDown.duration(400)} style={[styles.verdict, { backgroundColor: v.bg }]} testID={`verdict-card-${result.verdict}`}>
        <View style={styles.verdictTop}>
          <Ionicons name={v.icon} size={44} color={v.fg} />
          <Text style={[styles.verdictLabel, { color: v.fg }]} testID="verdict-label">
            {t[v.key]}
          </Text>
        </View>
        <Text style={[styles.verdictTitle, { color: v.fg }]} testID="result-title">
          {result.title}
        </Text>
        {!!result.message && <Text style={[styles.verdictMsg, { color: v.fg }]} testID="result-message">{result.message}</Text>}
      </Animated.View>

      <Animated.View entering={FadeInDown.delay(100).duration(400)}>
        <SpeakButton id={`result-${result.id}`} text={result.speak} testID="result-speak-button" />
      </Animated.View>

      {chips.length > 0 && (
        <View style={styles.chips}>
          {chips.map((c, i) => (
            <View key={i} style={styles.chip} testID={`fact-chip-${i}`}>
              <Ionicons name={c.icon as never} size={20} color={colors.onBrandSecondary} />
              <Text style={styles.chipText}>{c.text}</Text>
            </View>
          ))}
        </View>
      )}

      {result.steps.length > 0 && (
        <View style={styles.sectionRow}>
          <Text style={styles.section}>{t.whatToDo}</Text>
          {hasMarks && active !== null && (
            <Pressable testID="show-all-marks" onPress={() => onStep(null)} style={styles.showAll} hitSlop={8}>
              <Text style={styles.showAllText}>{t.showAll}</Text>
            </Pressable>
          )}
        </View>
      )}
      {hasMarks && <Text style={styles.hintText}>{t.tapStep}</Text>}
      {result.steps.map((s, i) => (
        <Animated.View key={i} entering={FadeInDown.delay(200 + i * 80).duration(400)}>
          <Pressable
            testID={`step-card-${i}`}
            disabled={!hasMarks}
            onPress={() => {
              Haptics.selectionAsync();
              onStep(active === i ? null : i);
            }}
            style={[styles.step, active === i && styles.stepActive]}
          >
            <View style={styles.stepIcon}>
              <Ionicons name={stepIcons[s.icon] as never} size={32} color={colors.onBrandSecondary} />
              {mode === "guide" && (
                <View style={styles.stepNum}>
                  <Text style={styles.stepNumText}>{i + 1}</Text>
                </View>
              )}
            </View>
            <Text style={styles.stepText}>{s.text}</Text>
            {hasMarks && s.mark != null && <Ionicons name="eye" size={24} color={active === i ? colors.brandPrimary : colors.muted} />}
          </Pressable>
        </Animated.View>
      ))}
    </View>
  );
}

const useStyles = makeStyles((c) => ({
  screen: { flex: 1, backgroundColor: c.surface },
  photoWrap: { width: "100%", aspectRatio: 4 / 3.4, backgroundColor: c.surfaceInverse, overflow: "hidden" },
  photo: { width: "100%", height: "100%" },
  topScrim: { position: "absolute", top: 0, left: 0, right: 0, height: 120 },
  back: { position: "absolute", left: 16, width: 52, height: 52, borderRadius: 26, backgroundColor: "rgba(0,0,0,0.5)", alignItems: "center", justifyContent: "center" },
  scanLine: { position: "absolute", left: 0, right: 0, height: 4, backgroundColor: c.brandTertiary, shadowColor: c.brandTertiary, shadowOpacity: 1, shadowRadius: 12, elevation: 8 },
  body: { padding: 16, marginTop: -24, backgroundColor: c.surface, borderTopLeftRadius: 24, borderTopRightRadius: 24 },
  loading: { alignItems: "center", paddingVertical: 32, gap: 12 },
  loadingText: { fontFamily: font.bold, fontSize: 22, lineHeight: 30, color: c.onSurface, textAlign: "center" },
  errorBox: { alignItems: "center", paddingVertical: 24, gap: 8 },
  errorTitle: { fontFamily: font.bold, fontSize: 24, lineHeight: 32, color: c.onSurface },
  errorText: { fontFamily: font.regular, fontSize: 17, lineHeight: 24, color: c.muted, textAlign: "center" },
  retry: { marginTop: 12, height: 60, paddingHorizontal: 32, borderRadius: 999, backgroundColor: c.brandPrimary, flexDirection: "row", gap: 8, alignItems: "center" },
  retryText: { fontFamily: font.bold, fontSize: 20, color: c.onBrandPrimary },
  resultGap: { gap: 16 },
  verdict: { borderRadius: 20, padding: 20, gap: 4 },
  verdictTop: { flexDirection: "row", alignItems: "center", gap: 8 },
  verdictLabel: { fontFamily: font.bold, fontSize: 30, lineHeight: 40, flexShrink: 1 },
  verdictTitle: { fontFamily: font.bold, fontSize: 22, lineHeight: 30 },
  verdictMsg: { fontFamily: font.regular, fontSize: 18, lineHeight: 26 },
  chips: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
  chip: { flexDirection: "row", alignItems: "center", gap: 6, backgroundColor: c.brandSecondary, paddingHorizontal: 14, height: 40, borderRadius: 999 },
  chipText: { fontFamily: font.semi, fontSize: 16, color: c.onBrandSecondary },
  section: { fontFamily: font.bold, fontSize: 22, lineHeight: 30, color: c.onSurface, marginTop: 4 },
  sectionRow: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  showAll: { minHeight: 44, justifyContent: "center" },
  showAllText: { fontFamily: font.semi, fontSize: 16, color: c.brandPrimary },
  hintText: { fontFamily: font.regular, fontSize: 15, lineHeight: 20, color: c.muted, marginTop: -12 },
  stepActive: { borderColor: c.brandPrimary, borderWidth: 3, backgroundColor: c.brandSecondary },
  stepNum: { position: "absolute", top: -6, left: -6, width: 24, height: 24, borderRadius: 12, backgroundColor: c.brandPrimary, alignItems: "center", justifyContent: "center" },
  stepNumText: { fontFamily: font.bold, fontSize: 13, lineHeight: 18, color: c.onBrandPrimary },
  step: { flexDirection: "row", alignItems: "center", gap: 12, padding: 12, borderRadius: 20, backgroundColor: c.surfaceSecondary, borderWidth: 1, borderColor: c.border, minHeight: 76 },
  stepIcon: { width: 56, height: 56, borderRadius: 16, backgroundColor: c.brandSecondary, alignItems: "center", justifyContent: "center" },
  stepText: { flex: 1, fontFamily: font.semi, fontSize: 19, lineHeight: 26, color: c.onSurface },
  footer: {
    position: "absolute",
    left: 0,
    right: 0,
    bottom: 0,
    flexDirection: "row",
    gap: 12,
    paddingHorizontal: 16,
    paddingTop: 12,
    backgroundColor: c.surface,
    borderTopWidth: 1,
    borderTopColor: c.border,
  },
  footerBtn: { flex: 1, height: 60, borderRadius: 999, flexDirection: "row", gap: 8, alignItems: "center", justifyContent: "center" },
  footerSecondary: { backgroundColor: c.surfaceSecondary, borderWidth: 2, borderColor: c.borderStrong },
  footerPrimary: { backgroundColor: c.brandPrimary },
  secondaryIcon: { color: c.onSurface },
  primaryIcon: { color: c.onBrandPrimary },
  footerSecondaryText: { fontFamily: font.bold, fontSize: 19, color: c.onSurface },
  footerPrimaryText: { fontFamily: font.bold, fontSize: 19, color: c.onBrandPrimary },
  pressed: { transform: [{ scale: 0.97 }] },
  track: { marginTop: 16, height: 60, borderRadius: 999, borderWidth: 2, borderColor: c.brandPrimary, flexDirection: "row", gap: 8, alignItems: "center", justifyContent: "center", backgroundColor: c.brandSecondary },
  trackIcon: { color: c.onBrandSecondary },
  trackText: { fontFamily: font.bold, fontSize: 19, color: c.onBrandSecondary },
}));
