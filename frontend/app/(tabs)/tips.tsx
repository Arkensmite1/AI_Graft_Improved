import { Ionicons } from "@react-native-vector-icons/ionicons";
import { ScrollView, Text, View } from "react-native";
import Animated, { FadeInDown } from "react-native-reanimated";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import { SpeakButton } from "@/src/components/SpeakButton";
import { font } from "@/src/fonts";
import { useLang } from "@/src/i18n";
import { usesNativeTabs } from "@/src/navigation";
import { makeStyles, useTheme } from "@/src/theme";

type Tip = { id: string; icon: string; en: [string, string]; hi: [string, string] };

const TIPS: Tip[] = [
  {
    id: "pick",
    icon: "leaf",
    en: ["Pick the right plants", "Both plants same thickness, like a pencil lead. 2 to 4 leaves."],
    hi: ["सही पौधे चुनें", "दोनों पौधे एक जैसे मोटे हों, पेंसिल की नोक जितने। 2 से 4 पत्ते हों।"],
  },
  {
    id: "cut",
    icon: "cut",
    en: ["Make a clean slant cut", "Use a sharp, clean blade. Cut at a slant. Wash hands first."],
    hi: ["साफ़ तिरछा कट लगाएं", "तेज़ और साफ़ ब्लेड लें। तिरछा काटें। पहले हाथ धोएं।"],
  },
  {
    id: "join",
    icon: "git-merge",
    en: ["Join and clip at once", "Press both cut ends together. Put the clip right away."],
    hi: ["तुरंत जोड़ें और क्लिप लगाएं", "दोनों कटे हिस्से मिलाएं। तुरंत क्लिप लगाएं।"],
  },
  {
    id: "cover",
    icon: "cube",
    en: ["Keep covered and dark", "First 3 days keep under plastic cover, in shade. Keep air moist."],
    hi: ["ढककर अंधेरे में रखें", "पहले 3 दिन प्लास्टिक से ढककर छाया में रखें। नमी बनाए रखें।"],
  },
  {
    id: "open",
    icon: "sunny",
    en: ["Give light slowly", "From day 4, open the cover a little more each day for one week."],
    hi: ["धीरे-धीरे रोशनी दें", "चौथे दिन से हर दिन ढक्कन थोड़ा ज़्यादा खोलें, एक हफ्ते तक।"],
  },
  {
    id: "watch",
    icon: "eye",
    en: ["Watch the plant daily", "Small droop on day 1 is normal. Black joint means remove the plant."],
    hi: ["रोज़ पौधा देखें", "पहले दिन थोड़ा मुरझाना ठीक है। जोड़ काला हो तो पौधा हटा दें।"],
  },
];

export default function Tips() {
  const insets = useSafeAreaInsets();
  const { t, lang } = useLang();
  const { colors } = useTheme();
  const styles = useStyles();
  const bottomChrome = usesNativeTabs ? insets.bottom : 0;

  return (
    <View style={styles.screen}>
      <View style={[styles.header, { paddingTop: insets.top + 8 }]}>
        <Text style={styles.title}>{t.tipsTitle}</Text>
        <Text style={styles.sub}>{t.tipsSub}</Text>
      </View>
      <ScrollView contentContainerStyle={[styles.list, { paddingBottom: bottomChrome + 24 }]}>
        {TIPS.map((tip, i) => {
          const [title, body] = tip[lang];
          return (
            <Animated.View key={tip.id} entering={FadeInDown.delay(i * 60).duration(350)} style={styles.card} testID={`tip-card-${tip.id}`}>
              <View style={styles.num}>
                <Ionicons name={tip.icon as never} size={30} color={colors.onBrandSecondary} />
                <Text style={styles.numText}>{i + 1}</Text>
              </View>
              <View style={styles.body}>
                <Text style={styles.cardTitle}>{title}</Text>
                <Text style={styles.cardText}>{body}</Text>
              </View>
              <SpeakButton id={`tip-${tip.id}-${lang}`} text={`${title}. ${body}`} size="small" testID={`tip-speak-${tip.id}`} />
            </Animated.View>
          );
        })}
      </ScrollView>
    </View>
  );
}

const useStyles = makeStyles((c) => ({
  screen: { flex: 1, backgroundColor: c.surface },
  header: { paddingHorizontal: 16, paddingBottom: 12, borderBottomWidth: 1, borderBottomColor: c.divider },
  title: { fontFamily: font.bold, fontSize: 28, lineHeight: 38, color: c.onSurface },
  sub: { fontFamily: font.regular, fontSize: 16, lineHeight: 22, color: c.muted },
  list: { padding: 16, gap: 12 },
  card: {
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    padding: 16,
    borderRadius: 20,
    backgroundColor: c.surface,
    borderWidth: 1,
    borderColor: c.border,
    shadowColor: c.onSurface,
    shadowOpacity: 0.06,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 3 },
    elevation: 2,
  },
  num: { width: 60, height: 60, borderRadius: 16, backgroundColor: c.brandSecondary, alignItems: "center", justifyContent: "center" },
  numText: { position: "absolute", top: 2, left: 6, fontFamily: font.bold, fontSize: 13, color: c.onBrandSecondary },
  body: { flex: 1 },
  cardTitle: { fontFamily: font.bold, fontSize: 18, lineHeight: 26, color: c.onSurface },
  cardText: { fontFamily: font.regular, fontSize: 16, lineHeight: 22, color: c.onSurfaceSecondary },
}));
