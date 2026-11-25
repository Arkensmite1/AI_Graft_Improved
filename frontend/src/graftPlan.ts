// Day-by-day care plan after grafting (day 0 = grafting day).
export type PlanIcon = "cover" | "shade" | "camera" | "wind" | "sun" | "water" | "eye" | "clip" | "check";
export type PlanTask = { day: number; icon: PlanIcon; en: string; hi: string; photo?: boolean; milestone?: boolean };

export const PLAN_DAYS = 14;

export const PLAN: PlanTask[] = [
  { day: 0, icon: "cover", en: "Put plants under cover, in shade", hi: "पौधों को ढककर छाया में रखें", milestone: true },
  { day: 1, icon: "shade", en: "Keep covered. Spray a light mist", hi: "ढका रखें। हल्का पानी छिड़कें" },
  { day: 2, icon: "shade", en: "Keep covered and dark", hi: "ढका और अंधेरे में रखें" },
  { day: 3, icon: "camera", en: "Keep covered. Take a photo check", hi: "ढका रखें। फोटो से जाँच करें", photo: true },
  { day: 4, icon: "wind", en: "Open cover a little, morning and evening", hi: "सुबह-शाम ढक्कन थोड़ा खोलें", milestone: true },
  { day: 5, icon: "wind", en: "Open the cover a bit more", hi: "ढक्कन थोड़ा और खोलें" },
  { day: 6, icon: "sun", en: "Give soft light, open half the cover", hi: "हल्की रोशनी दें, आधा ढक्कन खोलें" },
  { day: 7, icon: "camera", en: "Remove cover fully. Keep in shade. Photo check", hi: "ढक्कन पूरा हटाएं। छाया में रखें। फोटो से जाँचें", photo: true, milestone: true },
  { day: 8, icon: "sun", en: "Give more light slowly", hi: "धीरे-धीरे ज़्यादा रोशनी दें" },
  { day: 9, icon: "water", en: "Water gently at the root", hi: "जड़ में हल्का पानी दें" },
  { day: 10, icon: "sun", en: "Give normal sunlight", hi: "सामान्य धूप दें" },
  { day: 11, icon: "eye", en: "Check the joint is strong", hi: "देखें कि जोड़ मज़बूत है" },
  { day: 12, icon: "clip", en: "Remove the clip gently", hi: "क्लिप धीरे से हटाएं", milestone: true },
  { day: 13, icon: "water", en: "Water and keep watching", hi: "पानी दें और ध्यान रखें" },
  { day: 14, icon: "check", en: "Ready to plant in the field!", hi: "खेत में लगाने के लिए तैयार!", photo: true, milestone: true },
];

export const planIcons: Record<PlanIcon, string> = {
  cover: "cube",
  shade: "umbrella",
  camera: "camera",
  wind: "cloudy",
  sun: "sunny",
  water: "water",
  eye: "eye",
  clip: "cut",
  check: "checkmark-done-circle",
};

export const taskFor = (day: number) => PLAN[Math.max(0, Math.min(day, PLAN_DAYS))];
