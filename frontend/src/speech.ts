// One app-wide audio player for read-aloud. Only one voice plays at a time.
import { createAudioPlayer, setAudioModeAsync, type AudioPlayer } from "expo-audio";
import { useEffect, useState } from "react";
import { AppState } from "react-native";

import { getSpeechUrl } from "@/src/api";
import type { Lang } from "@/src/i18n";

export type SpeechState = { id: string | null; status: "idle" | "loading" | "playing" | "error" };

let state: SpeechState = { id: null, status: "idle" };
let player: AudioPlayer | null = null;
let seq = 0;
const listeners = new Set<(s: SpeechState) => void>();

function set(next: SpeechState) {
  state = next;
  listeners.forEach((l) => l(state));
}

function getPlayer() {
  if (!player) {
    player = createAudioPlayer(null);
    player.addListener("playbackStatusUpdate", (s) => {
      if (s.didJustFinish && state.status === "playing") set({ id: null, status: "idle" });
    });
  }
  return player;
}

export function stopSpeech() {
  seq++;
  player?.pause();
  if (state.status !== "idle") set({ id: null, status: "idle" });
}

export async function speak(id: string, text: string, lang: Lang) {
  stopSpeech();
  const mySeq = seq;
  set({ id, status: "loading" });
  try {
    await setAudioModeAsync({ playsInSilentMode: true, allowsRecording: false });
    const url = await getSpeechUrl(text, lang);
    if (mySeq !== seq) return;
    const p = getPlayer();
    p.replace({ uri: url });
    p.play();
    set({ id, status: "playing" });
  } catch (e) {
    console.warn("[speech] failed", e);
    if (mySeq === seq) set({ id, status: "error" });
  }
}

AppState.addEventListener("change", (s) => {
  if (s !== "active") stopSpeech();
});

export function useSpeech() {
  const [s, setS] = useState(state);
  useEffect(() => {
    listeners.add(setS);
    return () => {
      listeners.delete(setS);
    };
  }, []);
  return s;
}
