import { Ionicons } from "@react-native-vector-icons/ionicons";
import * as Haptics from "expo-haptics";
import { useFocusEffect } from "expo-router";
import { useCallback } from "react";
import { ActivityIndicator, Pressable, Text } from "react-native";

import { font } from "@/src/fonts";
import { useLang } from "@/src/i18n";
import { speak, stopSpeech, useSpeech } from "@/src/speech";
import { makeStyles, useTheme } from "@/src/theme";

type Props = { id: string; text: string; size?: "big" | "small"; testID: string };

export function SpeakButton({ id, text, size = "big", testID }: Props) {
  const { t, lang } = useLang();
  const { colors } = useTheme();
  const styles = useStyles();
  const speech = useSpeech();
  const mine = speech.id === id;
  const status = mine ? speech.status : "idle";

  // Stop narration when the screen loses focus.
  useFocusEffect(useCallback(() => () => stopSpeech(), []));

  const onPress = () => {
    Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    if (status === "playing" || status === "loading") stopSpeech();
    else speak(id, text, lang);
  };

  const icon = status === "playing" ? "stop" : status === "error" ? "refresh" : "volume-high";
  const label =
    status === "playing" ? t.stop : status === "loading" ? t.loadingVoice : status === "error" ? t.voiceError : t.listen;

  if (size === "small") {
    return (
      <Pressable testID={testID} onPress={onPress} style={({ pressed }) => [styles.small, pressed && styles.pressed]} hitSlop={8}>
        {status === "loading" ? (
          <ActivityIndicator color={colors.onBrandPrimary} />
        ) : (
          <Ionicons name={icon} size={24} color={colors.onBrandPrimary} />
        )}
      </Pressable>
    );
  }

  return (
    <Pressable testID={testID} onPress={onPress} style={({ pressed }) => [styles.big, status === "error" && styles.bigError, pressed && styles.pressed]}>
      <Text style={styles.bigLabel} numberOfLines={1}>
        {label}
      </Text>
      {status === "loading" ? (
        <ActivityIndicator color={colors.onBrandPrimary} size="large" />
      ) : (
        <Ionicons name={icon} size={36} color={colors.onBrandPrimary} />
      )}
    </Pressable>
  );
}

const useStyles = makeStyles((c) => ({
  big: {
    height: 72,
    borderRadius: 999,
    backgroundColor: c.brandPrimary,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingLeft: 28,
    paddingRight: 20,
    shadowColor: c.brand,
    shadowOpacity: 0.3,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 6 },
    elevation: 6,
  },
  bigError: { backgroundColor: c.error },
  bigLabel: { fontFamily: font.bold, fontSize: 24, lineHeight: 32, color: c.onBrandPrimary, flexShrink: 1 },
  small: {
    width: 52,
    height: 52,
    borderRadius: 26,
    backgroundColor: c.brandPrimary,
    alignItems: "center",
    justifyContent: "center",
  },
  pressed: { transform: [{ scale: 0.96 }], opacity: 0.9 },
}));
