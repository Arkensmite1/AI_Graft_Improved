# AI-Graft backend API tests: analyze (Gemini vision) + TTS (OpenAI) endpoints
import base64
import os
from pathlib import Path

import pytest
import requests


def _base_url():
    url = os.environ.get("EXPO_PUBLIC_BACKEND_URL") or os.environ.get("EXPO_BACKEND_URL")
    if not url:
        for line in Path("/app/frontend/.env").read_text().splitlines():
            if line.startswith("EXPO_PUBLIC_BACKEND_URL=") or line.startswith("EXPO_BACKEND_URL="):
                url = line.split("=", 1)[1].strip().strip('"')
    return url.rstrip("/")


BASE_URL = _base_url()
IMG_URL = "https://images.unsplash.com/photo-1583263840577-b174af9ac796?fm=jpg&q=70&w=800"
VERDICTS = {"good", "wait", "bad", "unclear"}
ICONS = {"water", "sun", "shade", "cover", "clock", "cut", "leaf", "warning", "check", "camera",
         "remove", "medicine", "thermometer", "wind"}


@pytest.fixture(scope="session")
def img_b64():
    r = requests.get(IMG_URL, timeout=30)
    assert r.status_code == 200 and r.content[:2] == b"\xff\xd8"
    return base64.b64encode(r.content).decode()


def _check_result(d, mode, lang):
    assert d["mode"] == mode and d["lang"] == lang
    assert d["verdict"] in VERDICTS
    assert isinstance(d["title"], str) and d["title"]
    assert isinstance(d["message"], str)
    assert isinstance(d["speak"], str) and d["speak"]
    assert isinstance(d["steps"], list) and 0 < len(d["steps"]) <= 3
    for s in d["steps"]:
        assert s["icon"] in ICONS and s["text"]
    assert set(d["facts"].keys()) == {"leaves", "stem", "days_wait", "healing_day"}
    assert "_id" not in d and d["id"] and d["created_at"]


def _has_devanagari(s):
    return any("\u0900" <= ch <= "\u097F" for ch in s)


# Health
def test_root():
    r = requests.get(f"{BASE_URL}/api/", timeout=15)
    assert r.status_code == 200 and r.json()["message"] == "AI-Graft API"


# Analyze - seedling Hindi
def test_analyze_seedling_hi(img_b64):
    r = requests.post(f"{BASE_URL}/api/analyze", json={"image_base64": img_b64, "mode": "seedling", "lang": "hi"}, timeout=120)
    assert r.status_code == 200, r.text
    d = r.json()
    print(d)
    _check_result(d, "seedling", "hi")
    assert _has_devanagari(d["title"] + d["message"] + d["speak"])


# Analyze - default lang is hi, data URI prefix accepted
def test_analyze_default_lang_data_uri(img_b64):
    r = requests.post(f"{BASE_URL}/api/analyze", json={"image_base64": "data:image/jpeg;base64," + img_b64, "mode": "seedling"}, timeout=120)
    assert r.status_code == 200, r.text
    assert r.json()["lang"] == "hi"


# Analyze - graft English
def test_analyze_graft_en(img_b64):
    r = requests.post(f"{BASE_URL}/api/analyze", json={"image_base64": img_b64, "mode": "graft", "lang": "en"}, timeout=120)
    assert r.status_code == 200, r.text
    d = r.json()
    print(d)
    _check_result(d, "graft", "en")
    assert not _has_devanagari(d["title"] + d["message"] + d["speak"])


# Analyze - validation
@pytest.mark.parametrize("payload", [
    {"image_base64": "x" * 200, "mode": "flower", "lang": "hi"},
    {"image_base64": "x" * 200, "mode": "seedling", "lang": "fr"},
    {"image_base64": "short", "mode": "seedling", "lang": "hi"},
    {"mode": "seedling"},
])
def test_analyze_validation_422(payload):
    r = requests.post(f"{BASE_URL}/api/analyze", json=payload, timeout=30)
    assert r.status_code == 422


# Analyze - garbage base64 should fail gracefully (502, not 500)
def test_analyze_garbage_image():
    r = requests.post(f"{BASE_URL}/api/analyze", json={"image_base64": "A" * 400, "mode": "seedling", "lang": "en"}, timeout=150)
    print(r.status_code, r.text[:300])
    assert r.status_code in (200, 502)


# TTS - create + fetch audio, cache returns same url
@pytest.mark.parametrize("lang,text", [("hi", "पौधा तैयार है। अब ग्राफ्टिंग करें।"), ("en", "TEST plant is ready. Graft now.")])
def test_tts_roundtrip(lang, text):
    r = requests.post(f"{BASE_URL}/api/tts", json={"text": text, "lang": lang}, timeout=120)
    assert r.status_code == 200, r.text
    url = r.json()["url"]
    assert url.startswith("/api/tts/") and url.endswith(".mp3")
    a = requests.get(f"{BASE_URL}{url}", timeout=30)
    assert a.status_code == 200
    assert a.headers["content-type"].startswith("audio/mpeg")
    assert len(a.content) > 1000
    r2 = requests.post(f"{BASE_URL}/api/tts", json={"text": text, "lang": lang}, timeout=30)
    assert r2.json()["url"] == url


# TTS - validation / not found
def test_tts_empty_text():
    assert requests.post(f"{BASE_URL}/api/tts", json={"text": "", "lang": "hi"}, timeout=15).status_code == 422
    assert requests.post(f"{BASE_URL}/api/tts", json={"text": "***", "lang": "hi"}, timeout=15).status_code == 400
    assert requests.post(f"{BASE_URL}/api/tts", json={"text": "x" * 1501}, timeout=15).status_code == 422


def test_tts_missing_key_404():
    assert requests.get(f"{BASE_URL}/api/tts/doesnotexist.mp3", timeout=15).status_code == 404
