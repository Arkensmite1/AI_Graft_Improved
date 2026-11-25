# AI-Graft — PRD

## Original problem statement
"this is the current codebase for the app, i need you to make the features actually working, implement real time camera access, actual analysis, and also make all the information presented in the app minimal and simple as it is made for indian farmers and often they are not that literate, and suggest any other improvements that can be implemented or added. in the end give me the zip file for the entire updated codebase"

Original upload: Kotlin/Compose + TS research-style tomato grafting assistant with all AI stubbed (fake fixed numbers).

## User choices
- Vision AI: Gemini 3 Flash (`gemini-3-flash-preview`, Emergent LLM key)
- Language: Hindi + English, switchable (default Hindi)
- Read aloud: speaker button (OpenAI `gpt-4o-mini-tts`, voice coral, fallback tts-1)
- History: saved on phone (last 30 scans)

## Architecture
- Expo Router app (SDK 58): tabs Home / History / Tips; full-screen `scan` (expo-camera live view, torch, gallery) and `result` screens.
- FastAPI: `POST /api/analyze` (Gemini vision → simple JSON verdict good/wait/bad/unclear, title, message, ≤3 icon steps, facts, speak text), `POST /api/tts` + `GET /api/tts/{key}.mp3` (Mongo-cached audio).
- Local storage: `scan_history`, `app_lang`.

## Implemented (2026-06)
- Real live camera capture with permission flow (ask → open settings) + gallery fallback
- Real AI analysis for seedling readiness and graft healing, in Hindi/English
- Traffic-light verdict, big icons, 1-3 short steps, auto read-aloud + speaker buttons
- History (view, delete all), 6 spoken grafting tips, Baloo 2 font (Devanagari)

## Backlog / suggestions
- P1: Healing day-by-day tracker per graft (reminders to open cover, remove clip)
- P1: More languages (Marathi, Telugu, Kannada, Tamil, Punjabi)
- P1: Voice question ("ask by speaking") for farmers who can't type
- P2: Offline queue — take photo without internet, analyse later
- P2: Pair matcher (photo of rootstock + scion → same thickness?)
- P2: Share result on WhatsApp to an agri expert / KVK
- P2: Weather-based care alerts (heat / humidity)
