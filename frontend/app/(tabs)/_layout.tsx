import { Ionicons } from "@react-native-vector-icons/ionicons";
import * as Haptics from "expo-haptics";
import { Tabs } from "expo-router";
import { NativeTabs } from "expo-router/native-tabs";
import { Platform } from "react-native";

import { font } from "@/src/fonts";
import { useLang } from "@/src/i18n";
import { usesNativeTabs } from "@/src/navigation";
import { useTheme } from "@/src/theme";

export default function TabsLayout() {
  const { t } = useLang();
  const { colors } = useTheme();

  if (usesNativeTabs) {
    return (
      <NativeTabs tintColor={colors.brandPrimary}>
        <NativeTabs.Trigger name="index">
          <NativeTabs.Trigger.Icon sf="house.fill" />
          <NativeTabs.Trigger.Label>{t.tabHome}</NativeTabs.Trigger.Label>
        </NativeTabs.Trigger>
        <NativeTabs.Trigger name="grafts">
          <NativeTabs.Trigger.Icon sf="leaf.fill" />
          <NativeTabs.Trigger.Label>{t.tabGrafts}</NativeTabs.Trigger.Label>
        </NativeTabs.Trigger>
        <NativeTabs.Trigger name="history">
          <NativeTabs.Trigger.Icon sf="clock.fill" />
          <NativeTabs.Trigger.Label>{t.tabHistory}</NativeTabs.Trigger.Label>
        </NativeTabs.Trigger>
        <NativeTabs.Trigger name="tips">
          <NativeTabs.Trigger.Icon sf="lightbulb.fill" />
          <NativeTabs.Trigger.Label>{t.tabTips}</NativeTabs.Trigger.Label>
        </NativeTabs.Trigger>
      </NativeTabs>
    );
  }

  return (
    <Tabs
      screenListeners={{ tabPress: () => Haptics.selectionAsync() }}
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.brandPrimary,
        tabBarInactiveTintColor: colors.muted,
        tabBarStyle: {
          backgroundColor: colors.surface,
          borderTopColor: colors.border,
          ...(Platform.OS === "web" ? { height: 64 } : {}),
        },
        tabBarItemStyle: { alignSelf: "center" },
        tabBarLabelStyle: { fontFamily: font.semi, fontSize: 13 },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: t.tabHome,
          tabBarButtonTestID: "tab-home",
          tabBarIcon: ({ color, focused }) => <Ionicons name={focused ? "home" : "home-outline"} size={26} color={color} />,
        }}
      />
      <Tabs.Screen
        name="grafts"
        options={{
          title: t.tabGrafts,
          tabBarButtonTestID: "tab-grafts",
          tabBarIcon: ({ color, focused }) => <Ionicons name={focused ? "leaf" : "leaf-outline"} size={26} color={color} />,
        }}
      />
      <Tabs.Screen
        name="history"
        options={{
          title: t.tabHistory,
          tabBarButtonTestID: "tab-history",
          tabBarIcon: ({ color, focused }) => <Ionicons name={focused ? "time" : "time-outline"} size={26} color={color} />,
        }}
      />
      <Tabs.Screen
        name="tips"
        options={{
          title: t.tabTips,
          tabBarButtonTestID: "tab-tips",
          tabBarIcon: ({ color, focused }) => <Ionicons name={focused ? "bulb" : "bulb-outline"} size={26} color={color} />,
        }}
      />
    </Tabs>
  );
}
