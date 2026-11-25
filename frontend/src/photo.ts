import { ImageManipulator, SaveFormat } from "expo-image-manipulator";
import * as ImagePicker from "expo-image-picker";

import type { Mode } from "@/src/api";
import { setPending } from "@/src/scans";

async function shrink(uri: string, width: number, compress: number) {
  const ctx = ImageManipulator.manipulate(uri).resize({ width });
  const img = await ctx.renderAsync();
  return img.saveAsync({ format: SaveFormat.JPEG, compress, base64: true });
}

// Resize the photo and hand it to the result screen.
export async function preparePhoto(uri: string, mode: Mode, batchId?: string) {
  const [big, mid, small] = await Promise.all([shrink(uri, 1024, 0.7), shrink(uri, 640, 0.6), shrink(uri, 240, 0.6)]);
  setPending({
    mode,
    batchId,
    uri: big.uri,
    base64: big.base64 ?? "",
    photo: `data:image/jpeg;base64,${mid.base64}`, // kept in history so visual marks stay sharp
    thumb: `data:image/jpeg;base64,${small.base64}`,
    aspect: big.width / big.height,
  });
}

// Pick a plant photo from the phone (system picker, no permission prompt needed).
export async function pickPhoto(): Promise<string | null> {
  const res = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ["images"], quality: 0.8 });
  return !res.canceled && res.assets[0] ? res.assets[0].uri : null;
}
