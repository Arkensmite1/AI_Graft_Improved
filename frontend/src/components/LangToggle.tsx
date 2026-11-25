import * as Haptics from "expo-haptics";
import { Pressable, Text, View } from "react-native";

import { font } from "@/src/fonts";
import { Lang, useLang } from "@/src/i18n";
import { makeStyles } from "@/src/theme";

const OPTIONS: { value: Lang; label: string }[] = [
  { value: "hi", label: "हिंदी" },
  { value: "en", label: "English" },
];

export function LangToggle() {
  const { lang, setLang } = useLang();
  const styles = useStyles();
  return (
    <View style={styles.wrap} testID="lang-toggle">
      {OPTIONS.map((o) => {
        const active = o.value === lang;
        return (
          <Pressable
            key={o.value}
            testID={`lang-option-${o.value}`}
            onPress={() => {
              Haptics.selectionAsync();
              setLang(o.value);
            }}
            style={[styles.opt, active && styles.optActive]}
          >
            <Text style={[styles.text, active && styles.textActive]}>{o.label}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const useStyles = makeStyles((c) => ({
  wrap: { flexDirection: "row", backgroundColor: c.surfaceSecondary, borderRadius: 999, padding: 4, borderWidth: 1, borderColor: c.border },
  opt: { minHeight: 44, paddingHorizontal: 16, borderRadius: 999, alignItems: "center", justifyContent: "center" },
  optActive: { backgroundColor: c.brandPrimary },
  text: { fontFamily: font.semi, fontSize: 16, lineHeight: 22, color: c.onSurfaceSecondary },
  textActive: { color: c.onBrandPrimary },
}));
