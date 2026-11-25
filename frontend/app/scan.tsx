import { Ionicons } from "@react-native-vector-icons/ionicons";
import { CameraView, useCameraPermissions } from "expo-camera";
import * as Haptics from "expo-haptics";
import { useLocalSearchParams, useRouter } from "expo-router";
import { useRef, useState } from "react";
import { ActivityIndicator, Linking, Pressable, Text, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import type { Mode } from "@/src/api";
import { font } from "@/src/fonts";
import { useLang } from "@/src/i18n";
import { pickPhoto, preparePhoto } from "@/src/photo";
import { makeStyles, useTheme } from "@/src/theme";

export default function Scan() {
  const { mode = "seedling", batchId } = useLocalSearchParams<{ mode: Mode; batchId?: string }>();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { t } = useLang();
  const { colors } = useTheme();
  const styles = useStyles();
  const camRef = useRef<CameraView>(null);
  const [permission, requestPermission] = useCameraPermissions();
  const [ready, setReady] = useState(false);
  const [torch, setTorch] = useState(false);
  const [busy, setBusy] = useState(false);

  const handlePhoto = async (uri: string) => {
    setBusy(true);
    try {
      await preparePhoto(uri, mode, batchId);
      router.replace("/result");
    } catch (e) {
      console.warn("[scan] prepare failed", e);
      setBusy(false);
    }
  };

  const capture = async () => {
    if (!camRef.current || busy) return;
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy);
    try {
      const photo = await camRef.current.takePictureAsync({ quality: 0.8 });
      if (photo?.uri) await handlePhoto(photo.uri);
    } catch (e) {
      console.warn("[scan] capture failed", e);
      Haptics.notificationAsync(Haptics.NotificationFeedbackType.Error);
    }
  };

  const pickFromGallery = async () => {
    const uri = await pickPhoto();
    if (uri) await handlePhoto(uri);
  };

  const closeBtn = (
    <Pressable testID="scan-close-button" onPress={() => router.back()} style={styles.roundBtn} hitSlop={8}>
      <Ionicons name="close" size={30} color={colors.onSurfaceInverse} />
    </Pressable>
  );

  // Permission states: loading, not granted (ask / settings), granted.
  if (!permission) {
    return (
      <View style={[styles.dark, styles.center]}>
        <ActivityIndicator size="large" color={colors.brandTertiary} />
      </View>
    );
  }

  if (!permission.granted) {
    const blocked = !permission.canAskAgain;
    return (
      <View style={[styles.dark, { paddingTop: insets.top + 8, paddingBottom: insets.bottom + 16 }]}>
        <View style={styles.topBar}>{closeBtn}</View>
        <View style={[styles.center, styles.flex, styles.permBox]} testID="camera-permission-view">
          <View style={styles.permIcon}>
            <Ionicons name="camera" size={56} color={colors.onBrandPrimary} />
          </View>
          <Text style={styles.permTitle}>{t.camNeedTitle}</Text>
          <Text style={styles.permText}>{t.camNeedText}</Text>
          <Pressable
            testID={blocked ? "open-settings-button" : "allow-camera-button"}
            style={styles.primary}
            onPress={() => (blocked ? Linking.openSettings() : requestPermission())}
          >
            <Ionicons name={blocked ? "settings" : "checkmark"} size={26} color={colors.onBrandPrimary} />
            <Text style={styles.primaryText}>{blocked ? t.openSettings : t.allowCamera}</Text>
          </Pressable>
          <Pressable testID="pick-gallery-button" style={styles.secondary} onPress={pickFromGallery}>
            <Ionicons name="images" size={24} color={colors.onSurfaceInverse} />
            <Text style={styles.secondaryText}>{t.useGallery}</Text>
          </Pressable>
        </View>
      </View>
    );
  }

  return (
    <View style={styles.dark}>
      <CameraView
        ref={camRef}
        style={styles.flex}
        facing="back"
        enableTorch={torch}
        onCameraReady={() => setReady(true)}
        testID="camera-view"
      />

      {/* Overlays are siblings of the camera, absolutely positioned. */}
      <View style={[styles.topOverlay, { paddingTop: insets.top + 8 }]} pointerEvents="box-none">
        {closeBtn}
        <View style={styles.modePill}>
          <Ionicons name={mode === "seedling" ? "leaf" : mode === "guide" ? "cut" : "git-merge"} size={18} color={colors.onBrandPrimary} />
          <Text style={styles.modeText}>{t[mode]}</Text>
        </View>
        <Pressable testID="torch-toggle" onPress={() => setTorch((v) => !v)} style={[styles.roundBtn, torch && styles.roundBtnOn]} hitSlop={8}>
          <Ionicons name={torch ? "flash" : "flash-off"} size={26} color={torch ? colors.onWarning : colors.onSurfaceInverse} />
        </Pressable>
      </View>

      <View style={styles.frameWrap} pointerEvents="none">
        <View style={styles.frame} />
        <Text style={styles.hint}>{mode === "guide" ? t.guideHint : t.keepInBox}</Text>
      </View>

      <View style={[styles.bottomBar, { paddingBottom: insets.bottom + 16 }]}>
        <Pressable testID="gallery-button" onPress={pickFromGallery} style={styles.sideBtn} disabled={busy}>
          <Ionicons name="images" size={28} color={colors.onSurfaceInverse} />
          <Text style={styles.sideText}>{t.gallery}</Text>
        </Pressable>
        <Pressable
          testID="capture-button"
          onPress={capture}
          disabled={!ready || busy}
          style={({ pressed }) => [styles.shutter, pressed && styles.shutterPressed, (!ready || busy) && styles.disabled]}
        >
          {busy ? <ActivityIndicator size="large" color={colors.brandPrimary} /> : <View style={styles.shutterInner} />}
        </Pressable>
        <View style={styles.sideBtn} />
      </View>

      {busy && (
        <View style={styles.busy} testID="preparing-overlay">
          <ActivityIndicator size="large" color={colors.brandTertiary} />
          <Text style={styles.busyText}>{t.preparing}</Text>
        </View>
      )}
    </View>
  );
}

const useStyles = makeStyles((c) => ({
  dark: { flex: 1, backgroundColor: c.surfaceInverse },
  flex: { flex: 1 },
  center: { alignItems: "center", justifyContent: "center" },
  topBar: { paddingHorizontal: 16 },
  topOverlay: {
    position: "absolute",
    top: 0,
    left: 0,
    right: 0,
    paddingHorizontal: 16,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  roundBtn: { width: 52, height: 52, borderRadius: 26, backgroundColor: "rgba(0,0,0,0.55)", alignItems: "center", justifyContent: "center" },
  roundBtnOn: { backgroundColor: c.warning },
  modePill: { flexDirection: "row", alignItems: "center", gap: 6, backgroundColor: c.brandPrimary, paddingHorizontal: 16, height: 40, borderRadius: 999 },
  modeText: { fontFamily: font.semi, fontSize: 16, color: c.onBrandPrimary },
  frameWrap: { position: "absolute", top: 0, bottom: 0, left: 0, right: 0, alignItems: "center", justifyContent: "center", gap: 12 },
  frame: { width: "78%", aspectRatio: 0.8, borderWidth: 4, borderColor: c.brandTertiary, borderRadius: 24, borderStyle: "dashed" },
  hint: {
    fontFamily: font.bold,
    fontSize: 18,
    color: c.onSurfaceInverse,
    backgroundColor: "rgba(0,0,0,0.55)",
    paddingHorizontal: 16,
    paddingVertical: 6,
    borderRadius: 999,
    overflow: "hidden",
  },
  bottomBar: {
    position: "absolute",
    bottom: 0,
    left: 0,
    right: 0,
    paddingTop: 20,
    paddingHorizontal: 24,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    backgroundColor: "rgba(0,0,0,0.6)",
  },
  sideBtn: { width: 72, alignItems: "center", gap: 2 },
  sideText: { fontFamily: font.semi, fontSize: 14, color: c.onSurfaceInverse },
  shutter: { width: 88, height: 88, borderRadius: 44, backgroundColor: c.surface, alignItems: "center", justifyContent: "center", borderWidth: 6, borderColor: c.brandTertiary },
  shutterInner: { width: 62, height: 62, borderRadius: 31, backgroundColor: c.brandPrimary },
  shutterPressed: { transform: [{ scale: 0.92 }] },
  disabled: { opacity: 0.5 },
  busy: { position: "absolute", top: 0, bottom: 0, left: 0, right: 0, backgroundColor: "rgba(0,0,0,0.7)", alignItems: "center", justifyContent: "center", gap: 12 },
  busyText: { fontFamily: font.bold, fontSize: 20, color: c.onSurfaceInverse },
  permBox: { paddingHorizontal: 24, gap: 12 },
  permIcon: { width: 112, height: 112, borderRadius: 56, backgroundColor: c.brandPrimary, alignItems: "center", justifyContent: "center", marginBottom: 8 },
  permTitle: { fontFamily: font.bold, fontSize: 28, lineHeight: 38, color: c.onSurfaceInverse, textAlign: "center" },
  permText: { fontFamily: font.regular, fontSize: 18, lineHeight: 26, color: c.onSurfaceInverse, textAlign: "center", opacity: 0.85 },
  primary: { marginTop: 16, height: 64, alignSelf: "stretch", borderRadius: 999, backgroundColor: c.brandPrimary, flexDirection: "row", gap: 8, alignItems: "center", justifyContent: "center" },
  primaryText: { fontFamily: font.bold, fontSize: 20, color: c.onBrandPrimary },
  secondary: { height: 56, alignSelf: "stretch", borderRadius: 999, borderWidth: 2, borderColor: c.onSurfaceInverse, flexDirection: "row", gap: 8, alignItems: "center", justifyContent: "center" },
  secondaryText: { fontFamily: font.semi, fontSize: 18, color: c.onSurfaceInverse },
}));
