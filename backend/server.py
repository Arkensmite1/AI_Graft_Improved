import asyncio
import hashlib
import json
import logging
import os
import re
import uuid
from contextlib import suppress
from datetime import datetime, timezone
from pathlib import Path
from typing import List, Literal, Optional

from dotenv import load_dotenv
from fastapi import APIRouter, FastAPI, HTTPException, Response
from fastapi.responses import FileResponse
from motor.motor_asyncio import AsyncIOMotorClient
from pydantic import BaseModel, Field
from pymongo import ASCENDING, IndexModel
from starlette.middleware.cors import CORSMiddleware

from emergentintegrations.llm.chat import ImageContent, LlmChat, StreamDone, TextDelta, UserMessage
from emergentintegrations.llm.openai import OpenAITextToSpeech

ROOT_DIR = Path(__file__).parent
load_dotenv(ROOT_DIR / ".env")

mongo_url = os.environ["MONGO_URL"]
client = AsyncIOMotorClient(mongo_url)
db = client[os.environ["DB_NAME"]]
LLM_KEY = os.environ["EMERGENT_LLM_KEY"]

VISION_PROVIDER = "gemini"
VISION_MODEL = "gemini-3-flash-preview"

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(name)s - %(levelname)s - %(message)s")
logger = logging.getLogger(__name__)

INDEXES: dict[str, list[IndexModel]] = {
    "tts_cache": [IndexModel([("key", ASCENDING)], name="key", unique=True)],
}


async def ensure_indexes() -> None:
    for collection, models in INDEXES.items():
        for model in models:
            try:
                await db[collection].create_indexes([model])
            except Exception as exc:
                logger.error("ensure_indexes(%s.%s): %s", collection, model.document["name"], exc)


app = FastAPI()


@app.get("/health", include_in_schema=False)
@app.head("/health", include_in_schema=False)
async def health():
    return {"status": "ok"}


api_router = APIRouter(prefix="/api")

# ---------------------------------------------------------------------------
# Models
# ---------------------------------------------------------------------------
Mode = Literal["seedling", "graft", "guide"]
MarkKind = Literal["cut", "clip", "rootstock", "scion", "remove"]
Lang = Literal["hi", "en"]
Verdict = Literal["good", "wait", "bad", "unclear"]
StepIcon = Literal[
    "water", "sun", "shade", "cover", "clock", "cut", "leaf", "warning",
    "check", "camera", "remove", "medicine", "thermometer", "wind",
]


class AnalyzeRequest(BaseModel):
    image_base64: str = Field(min_length=100)
    mode: Mode
    lang: Lang = "hi"


class Step(BaseModel):
    icon: StepIcon = "check"
    text: str
    mark: Optional[int] = None  # index into marks (guide mode)


class Mark(BaseModel):
    """Visual aid on the photo. Coordinates are 0-1 fractions of image width/height.
    cut: line (x1,y1)->(x2,y2); clip/remove: point (x1,y1); rootstock/scion: box (x1,y1)-(x2,y2)."""
    kind: MarkKind
    label: str = ""
    x1: float
    y1: float
    x2: Optional[float] = None
    y2: Optional[float] = None


class Facts(BaseModel):
    leaves: Optional[int] = None
    stem: Optional[Literal["thin", "ok", "thick"]] = None
    days_wait: Optional[int] = None
    healing_day: Optional[int] = None


class AnalyzeResult(BaseModel):
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    mode: Mode
    lang: Lang
    verdict: Verdict
    title: str
    message: str
    steps: List[Step]
    facts: Facts = Field(default_factory=Facts)
    speak: str
    marks: List[Mark] = Field(default_factory=list)
    created_at: str = Field(default_factory=lambda: datetime.now(timezone.utc).isoformat())


class TTSRequest(BaseModel):
    text: str = Field(min_length=1, max_length=1500)
    lang: Lang = "hi"


# ---------------------------------------------------------------------------
# Prompts
# ---------------------------------------------------------------------------
COMMON_RULES = """
You help small Indian farmers who often cannot read well. Be very simple.
Return ONLY one JSON object, no markdown, no extra text. Shape:
{
  "verdict": "good" | "wait" | "bad" | "unclear",
  "title": "max 4 words",
  "message": "one short sentence, max 14 words",
  "steps": [ {"icon": ICON, "text": "max 7 words"} ],   // 1 to 3 steps, most important first
  "facts": {"leaves": int or null, "stem": "thin"|"ok"|"thick"|null, "days_wait": int or null, "healing_day": int or null},
  "speak": "2-3 short spoken sentences: the result and what to do"
}
ICON must be one of: water, sun, shade, cover, clock, cut, leaf, warning, check, camera, remove, medicine, thermometer, wind.
If the photo is blurry, too dark, or not a tomato / vegetable seedling, use verdict "unclear", say how to take a better photo, and use icon "camera".
Never invent precise numbers you cannot see. No scientific words. No percentages.
"""

LANG_RULES = {
    "hi": "Write title, message, steps and speak in very simple spoken Hindi (Devanagari script), village-level words, like talking to a farmer. Avoid English words except very common ones like 'प्लास्टिक'.",
    "en": "Write title, message, steps and speak in very simple English, short common words, like talking to a farmer.",
}

MODE_RULES = {
    "seedling": """
Task: Look at this tomato (or brinjal/chilli) seedling photo. Is it ready for grafting (joining a rootstock and a scion)?
Ready signs: stem about as thick as a pencil lead (1.5-2.5 mm), 2 to 4 true leaves, healthy green colour, straight stem, about 2-3 weeks old.
- "good": ready to graft now.
- "wait": healthy but too small or stem too thin; set facts.days_wait to your best guess (days).
- "bad": sick, yellow, spots, rotten, broken or too old/woody stem. Say what to do (remove plant, separate it, etc.).
facts.stem = thin / ok / thick. facts.leaves = count of true leaves if visible.
Steps examples: when ready -> cut at slant with clean blade, choose same thickness partner, wash hands/blade.
""",
    "graft": """
Task: Look at this grafted tomato plant photo (a top plant joined on a root plant, often with a clip). Is the graft joint healing?
- "good": top plant upright, green, fresh; joint looks joined.
- "wait": top plant slightly drooping or soft; needs care and watching.
- "bad": strong wilting, black/brown rot at the joint, joint broken or top plant dried. Say to remove if it is dead.
Care knowledge: first 3 days keep in dark/shade with high humidity (cover with plastic or dome, spray mist). Days 4-7 slowly open cover and give more light. Remove clip after about 10-14 days. Never give direct strong sun early. Little droop on day 1 is normal.
facts.healing_day = your best guess of days since grafting, or null.
""",
    "guide": """
Task: The farmer is about to graft tomato seedlings now (splice / tube grafting). The photo shows one or two seedlings
(root plant = rootstock, top plant = scion), maybe in a tray or held in hand. Show EXACTLY on the photo where to act.
Add a "marks" array (max 6 items) to the JSON. Each mark: {"kind": KIND, "label": "max 3 words", "pts": [...]}
- "rootstock": box around the root plant, pts [ymin, xmin, ymax, xmax]
- "scion": box around the top plant, pts [ymin, xmin, ymax, xmax]
- "cut": slanted line across the stem where to cut, pts [y1, x1, y2, x2]. Root plant: cut just BELOW the two seed leaves
  (cotyledons), 45 degree slant. Top plant: cut just ABOVE the seed leaves, same slant. Line about 2-3 stem widths long, centred on the stem.
- "clip": point where the clip goes (on the root plant at its cut), pts [y, x]
- "remove": point on a side shoot / damaged leaf to remove (only if needed), pts [y, x]
All coordinates are integers 0-1000 relative to image height (y) and width (x). Put marks exactly on the visible stems.
If only one plant is visible, mark its cut and say in the message if it suits as root or top plant.
steps: 3 to 5 steps in order (this mode allows up to 5). Each step has "mark": index of its related mark in "marks", or null.
verdict: "good" if ready to graft (stem thickness fine, healthy); "wait" if too small; "bad" if sick; "unclear" if stems not visible.
""",
}


def build_prompt(mode: str, lang: str) -> str:
    return f"{COMMON_RULES}\n{MODE_RULES[mode]}\n{LANG_RULES[lang]}"


def extract_json(text: str) -> dict:
    cleaned = re.sub(r"^```(?:json)?|```$", "", text.strip(), flags=re.MULTILINE).strip()
    match = re.search(r"\{.*\}", cleaned, flags=re.DOTALL)
    if not match:
        raise ValueError("no JSON object in model reply")
    return json.loads(match.group(0))


def parse_marks(raw) -> list[Mark]:
    """Convert Gemini's [y, x] 0-1000 coordinates into 0-1 x/y fractions."""
    out: list[Mark] = []
    for m in raw if isinstance(raw, list) else []:
        try:
            kind, pts = m.get("kind"), [max(0.0, min(1.0, float(v) / 1000)) for v in m.get("pts", [])]
            if kind not in MarkKind.__args__:
                continue
            label = str(m.get("label", ""))[:30]
            if kind in ("clip", "remove") and len(pts) >= 2:
                out.append(Mark(kind=kind, label=label, x1=pts[1], y1=pts[0]))
            elif len(pts) >= 4:
                out.append(Mark(kind=kind, label=label, x1=pts[1], y1=pts[0], x2=pts[3], y2=pts[2]))
        except (TypeError, ValueError, AttributeError):
            continue
    return out[:6]


async def run_vision(req: AnalyzeRequest) -> str:
    chat = LlmChat(
        api_key=LLM_KEY,
        session_id=f"scan-{uuid.uuid4()}",
        system_message="You are a kind expert in vegetable grafting who explains things very simply.",
    ).with_model(VISION_PROVIDER, VISION_MODEL)
    b64 = req.image_base64.split(",", 1)[1] if req.image_base64.startswith("data:") else req.image_base64
    message = UserMessage(text=build_prompt(req.mode, req.lang), file_contents=[ImageContent(image_base64=b64)])
    parts: list[str] = []
    async for event in chat.stream_message(message):
        if isinstance(event, TextDelta):
            parts.append(event.content)
        elif isinstance(event, StreamDone):
            break
    return "".join(parts)


@api_router.get("/")
async def root():
    return {"message": "AI-Graft API"}


@api_router.post("/analyze", response_model=AnalyzeResult)
async def analyze(req: AnalyzeRequest):
    last_error: Exception | None = None
    for attempt in range(2):  # one retry on bad JSON or provider hiccup
        try:
            raw = await asyncio.wait_for(run_vision(req), timeout=60)
            data = extract_json(raw)
            marks = parse_marks(data.get("marks")) if req.mode == "guide" else []
            steps = [s for s in data.get("steps", []) if isinstance(s, dict) and s.get("text")][: 5 if req.mode == "guide" else 3]
            for s in steps:
                if s.get("icon") not in StepIcon.__args__:
                    s["icon"] = "check"
                m = s.get("mark")
                s["mark"] = m if isinstance(m, int) and 0 <= m < len(marks) else None
            facts = {k: v for k, v in (data.get("facts") or {}).items() if k in Facts.model_fields}
            if facts.get("stem") not in (None, "thin", "ok", "thick"):
                facts["stem"] = None
            verdict = data.get("verdict") if data.get("verdict") in Verdict.__args__ else "unclear"
            return AnalyzeResult(
                mode=req.mode,
                lang=req.lang,
                verdict=verdict,
                title=str(data.get("title", "")).strip() or "-",
                message=str(data.get("message", "")).strip(),
                steps=[Step(**s) for s in steps],
                facts=Facts(**facts),
                speak=str(data.get("speak", "")).strip() or str(data.get("message", "")),
                marks=marks,
            )
        except Exception as exc:  # noqa: BLE001 - provider / parse errors all handled the same
            last_error = exc
            logger.warning("analyze attempt %s failed: %s", attempt + 1, exc)
    raise HTTPException(status_code=502, detail=f"Analysis failed: {last_error}")


# ---------------------------------------------------------------------------
# Text to speech (cached in Mongo)
# ---------------------------------------------------------------------------
TTS_MODEL = "gpt-4o-mini-tts"
TTS_VOICE = "coral"
TTS_INSTRUCTIONS = {
    "hi": "Speak in clear, simple Hindi. Slow, warm and friendly, like a helpful village farming advisor.",
    "en": "Speak in clear, simple English with an Indian-friendly pace. Slow, warm and friendly, like a helpful farming advisor.",
}


def clean_for_tts(text: str) -> str:
    text = re.sub(r"https?://\S+", "", text)
    text = re.sub(r"[*_#>~|`]", "", text)
    return re.sub(r"\s+", " ", text).strip()


async def generate_audio(text: str, lang: str) -> bytes:
    tts = OpenAITextToSpeech(api_key=LLM_KEY)
    try:
        return await asyncio.wait_for(
            tts.generate_speech(text=text, model=TTS_MODEL, voice=TTS_VOICE,
                                instructions=TTS_INSTRUCTIONS[lang], response_format="mp3"),
            timeout=45,
        )
    except Exception as exc:  # noqa: BLE001
        logger.warning("TTS %s failed, falling back to tts-1: %s", TTS_MODEL, exc)
        return await asyncio.wait_for(
            tts.generate_speech(text=text, model="tts-1", voice="alloy", response_format="mp3"), timeout=45
        )


@api_router.post("/tts")
async def create_tts(req: TTSRequest):
    text = clean_for_tts(req.text)
    if not text:
        raise HTTPException(status_code=400, detail="Empty text")
    key = hashlib.sha256(f"{text}|{TTS_VOICE}|{TTS_MODEL}|mp3|{TTS_INSTRUCTIONS[req.lang]}".encode()).hexdigest()
    if not await db.tts_cache.find_one({"key": key}, {"_id": 1}):
        try:
            audio = await generate_audio(text, req.lang)
        except Exception as exc:  # noqa: BLE001
            logger.error("TTS failed: %s", exc)
            raise HTTPException(status_code=502, detail="Voice failed") from exc
        await db.tts_cache.update_one(
            {"key": key},
            {"$set": {"key": key, "audio": audio, "created_at": datetime.now(timezone.utc).isoformat()}},
            upsert=True,
        )
    return {"url": f"/api/tts/{key}.mp3"}


@api_router.get("/tts/{key}.mp3")
async def get_tts(key: str):
    doc = await db.tts_cache.find_one({"key": key}, {"_id": 0, "audio": 1})
    if not doc:
        raise HTTPException(status_code=404, detail="Not found")
    return Response(content=bytes(doc["audio"]), media_type="audio/mpeg",
                    headers={"Cache-Control": "public, max-age=31536000"})


# Preview-only helper: lets the owner download the packaged source zip if it exists.
CODE_ZIP = ROOT_DIR.parent / "AI_Graft_Expo.zip"


@api_router.get("/download/codebase")
async def download_codebase():
    if not CODE_ZIP.exists():
        raise HTTPException(status_code=404, detail="Not found")
    return FileResponse(CODE_ZIP, media_type="application/zip", filename="AI_Graft_Expo.zip")


app.include_router(api_router)

app.add_middleware(
    CORSMiddleware,
    allow_credentials=True,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
async def start_index_build():
    app.state.index_task = asyncio.create_task(ensure_indexes())


@app.on_event("shutdown")
async def shutdown_db_client():
    app.state.index_task.cancel()
    with suppress(asyncio.CancelledError):
        await app.state.index_task
    client.close()
