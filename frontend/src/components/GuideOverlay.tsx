import { Ionicons } from "@react-native-vector-icons/ionicons";
import { useState } from "react";
import { Text, View } from "react-native";
import Svg, { Circle, G, Line, Rect } from "react-native-svg";

import type { Mark } from "@/src/api";
import { font } from "@/src/fonts";
import { makeStyles, useTheme, type ThemeColors } from "@/src/theme";

type Props = { marks: Mark[]; stepOf: (markIndex: number) => number | null; active: number | null };

const markColor = (k: Mark["kind"], c: ThemeColors) =>
  k === "cut" || k === "remove" ? c.error : k === "clip" ? c.warning : k === "rootstock" ? c.info : c.brandTertiary;

const markIcon = (k: Mark["kind"]) =>
  k === "cut" ? "cut" : k === "clip" ? "attach" : k === "remove" ? "close" : k === "rootstock" ? "git-commit" : "leaf";

// Draws cut lines, clip points and plant boxes on top of the photo (coords are 0-1 fractions).
export function GuideOverlay({ marks, stepOf, active }: Props) {
  const { colors } = useTheme();
  const styles = useStyles();
  const [size, setSize] = useState({ w: 0, h: 0 });
  const { w, h } = size;

  return (
    <View style={styles.fill} pointerEvents="none" onLayout={(e) => setSize({ w: e.nativeEvent.layout.width, h: e.nativeEvent.layout.height })} testID="guide-overlay">
      {w > 0 && (
        <Svg width={w} height={h} style={styles.fill}>
          {marks.map((m, i) => {
            const col = markColor(m.kind, colors);
            const dim = active !== null && stepOf(i) !== active;
            const op = dim ? 0.25 : 1;
            const strong = active !== null && !dim;
            if (m.kind === "rootstock" || m.kind === "scion") {
              const x = Math.min(m.x1, m.x2 ?? m.x1) * w;
              const y = Math.min(m.y1, m.y2 ?? m.y1) * h;
              return (
                <Rect key={i} x={x} y={y} width={Math.abs((m.x2 ?? m.x1) - m.x1) * w} height={Math.abs((m.y2 ?? m.y1) - m.y1) * h}
                  rx={12} stroke={col} strokeWidth={strong ? 5 : 3} strokeDasharray="10 6" fill="transparent" opacity={op} />
              );
            }
            if (m.kind === "cut") {
              const p = { x1: m.x1 * w, y1: m.y1 * h, x2: (m.x2 ?? m.x1) * w, y2: (m.y2 ?? m.y1) * h };
              return (
                <G key={i}>
                  <Line {...p} stroke={colors.surface} strokeWidth={strong ? 12 : 10} strokeLinecap="round" opacity={op} />
                  <Line {...p} stroke={col} strokeWidth={strong ? 7 : 5} strokeLinecap="round" strokeDasharray="12 6" opacity={op} />
                </G>
              );
            }
            return (
              <G key={i}>
                <Circle cx={m.x1 * w} cy={m.y1 * h} r={strong ? 22 : 18} stroke={colors.surface} strokeWidth={7} fill="transparent" opacity={op} />
                <Circle cx={m.x1 * w} cy={m.y1 * h} r={strong ? 22 : 18} stroke={col} strokeWidth={4} fill="transparent" opacity={op} />
              </G>
            );
          })}
        </Svg>
      )}

      {w > 0 &&
        marks.map((m, i) => {
          const step = stepOf(i);
          const dim = active !== null && step !== active;
          const isBox = m.kind === "rootstock" || m.kind === "scion";
          const cx = isBox ? Math.min(m.x1, m.x2 ?? m.x1) * w : ((m.x1 + (m.x2 ?? m.x1)) / 2) * w;
          const cy = isBox ? Math.min(m.y1, m.y2 ?? m.y1) * h : ((m.y1 + (m.y2 ?? m.y1)) / 2) * h;
          const left = Math.max(4, Math.min(w - 150, isBox ? cx + 4 : cx + 22));
          const top = Math.max(4, Math.min(h - 36, isBox ? cy + 4 : cy - 16));
          return (
            <View key={`tag-${i}`} style={[styles.tag, { left, top, borderColor: markColor(m.kind, colors), opacity: dim ? 0.3 : 1 }]} testID={`guide-mark-${i}`}>
              {step !== null ? (
                <View style={[styles.num, { backgroundColor: markColor(m.kind, colors) }]}>
                  <Text style={styles.numText}>{step + 1}</Text>
                </View>
              ) : (
                <Ionicons name={markIcon(m.kind) as never} size={16} color={colors.onSurface} />
              )}
              {!!m.label && (
                <Text style={styles.tagText} numberOfLines={1}>
                  {m.label}
                </Text>
              )}
            </View>
          );
        })}
    </View>
  );
}

const useStyles = makeStyles((c) => ({
  fill: { position: "absolute", top: 0, left: 0, right: 0, bottom: 0 },
  tag: {
    position: "absolute",
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    maxWidth: 146,
    paddingLeft: 3,
    paddingRight: 8,
    height: 30,
    borderRadius: 999,
    backgroundColor: c.surface,
    borderWidth: 2,
  },
  num: { width: 22, height: 22, borderRadius: 11, alignItems: "center", justifyContent: "center" },
  numText: { fontFamily: font.bold, fontSize: 13, lineHeight: 18, color: c.onSurface },
  tagText: { fontFamily: font.semi, fontSize: 13, lineHeight: 18, color: c.onSurface, flexShrink: 1 },
}));
