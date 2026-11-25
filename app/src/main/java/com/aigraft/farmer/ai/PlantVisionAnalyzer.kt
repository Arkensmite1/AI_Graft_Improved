package com.aigraft.farmer.ai

import android.graphics.Bitmap
import android.util.Log
import com.aigraft.farmer.data.AdviceStep
import com.aigraft.farmer.data.AnalyzeResult
import com.aigraft.farmer.data.GuideMark
import com.aigraft.farmer.data.MarkKind
import com.aigraft.farmer.data.PlantFacts
import com.aigraft.farmer.data.ScanMode
import com.aigraft.farmer.data.Verdict
import com.aigraft.farmer.i18n.AppLang
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val TAG = "PlantVisionAnalyzer"
private const val MODEL_NAME = "gemini-3.8-flash"

private val ALLOWED_STEP_ICONS = setOf(
    "water", "sun", "shade", "cover", "clock", "cut", "leaf", "warning",
    "check", "camera", "remove", "medicine", "thermometer", "wind"
)

class PlantVisionAnalyzer {

    private val responseSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "verdict" to Schema.enumeration(
                    values = listOf("good", "wait", "bad", "unclear"),
                    description = "Traffic-light verdict for the farmer"
                ),
                "title" to Schema.string(description = "Max 4 words"),
                "message" to Schema.string(description = "One short sentence, max 14 words"),
                "steps" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "icon" to Schema.enumeration(
                                values = ALLOWED_STEP_ICONS.toList(),
                                description = "Icon key matching the action"
                            ),
                            "text" to Schema.string(description = "Max 7 words"),
                            "mark" to Schema.integer(
                                description = "Optional 0-based index into marks array in guide mode",
                                nullable = true
                            ),
                        ),
                        optionalProperties = listOf("mark")
                    ),
                    description = "1 to 3 steps (or 3 to 5 in guide mode), most important first"
                ),
                "facts" to Schema.obj(
                    properties = mapOf(
                        "leaves" to Schema.integer(nullable = true),
                        "stem" to Schema.string(
                            description = "thin, ok, thick, or null",
                            nullable = true
                        ),
                        "days_wait" to Schema.integer(nullable = true),
                        "healing_day" to Schema.integer(nullable = true),
                    ),
                    optionalProperties = listOf("leaves", "stem", "days_wait", "healing_day")
                ),
                "speak" to Schema.string(description = "2-3 short spoken sentences: the result and what to do"),
                "marks" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "kind" to Schema.enumeration(
                                values = listOf("cut", "clip", "rootstock", "scion", "remove"),
                                description = "Visual aid type"
                            ),
                            "label" to Schema.string(description = "Max 3 words"),
                            "pts" to Schema.array(
                                items = Schema.integer(),
                                description = "Coordinates 0-1000: [y, x] for clip/remove, [y1, x1, y2, x2] for cut/rootstock/scion"
                            )
                        )
                    ),
                    description = "Only populated in guide mode (max 6 items)",
                    nullable = true
                ),
            ),
            optionalProperties = listOf("facts", "marks")
        )
    }

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = this@PlantVisionAnalyzer.responseSchema
            },
            systemInstruction = content {
                text("You are a kind expert in vegetable grafting who explains things very simply.")
            }
        )
    }

    suspend fun analyze(bitmap: Bitmap, mode: ScanMode, lang: AppLang): AnalyzeResult {
        var lastError: Exception? = null
        val prompt = buildPrompt(mode, lang)

        for (attempt in 1..2) {
            try {
                val response = withTimeout(60_000L) {
                    model.generateContent(
                        content {
                            image(bitmap)
                            text(prompt)
                        }
                    )
                }
                val rawJson = response.text
                if (rawJson.isNullOrBlank()) {
                    throw IllegalStateException("Model returned an empty response.")
                }
                return parseResult(rawJson, mode, lang)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Analysis attempt $attempt timed out", e)
                lastError = e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Analysis attempt $attempt failed", e)
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Analysis failed.")
    }

    private fun parseResult(rawJson: String, mode: ScanMode, lang: AppLang): AnalyzeResult {
        val json = JSONObject(rawJson)
        val marks = if (mode == ScanMode.GUIDE) {
            parseMarks(json.optJSONArray("marks"))
        } else {
            emptyList()
        }

        val maxSteps = if (mode == ScanMode.GUIDE) 5 else 3
        val stepsArray = json.optJSONArray("steps")
        val steps = buildList {
            if (stepsArray != null) {
                for (i in 0 until stepsArray.length()) {
                    if (size >= maxSteps) break
                    val obj = stepsArray.optJSONObject(i) ?: continue
                    val stepText = obj.optString("text", "").trim()
                    if (stepText.isEmpty()) continue
                    val iconRaw = obj.optString("icon", "check")
                    val icon = if (iconRaw in ALLOWED_STEP_ICONS) iconRaw else "check"
                    val markIndex = if (!obj.isNull("mark")) {
                        val idx = obj.optInt("mark", -1)
                        if (idx in marks.indices) idx else null
                    } else {
                        null
                    }
                    add(AdviceStep(icon = icon, text = stepText, mark = markIndex))
                }
            }
        }

        val factsObj = json.optJSONObject("facts")
        val rawStem = factsObj?.optString("stem", "")?.takeIf { it in setOf("thin", "ok", "thick") }
        val facts = PlantFacts(
            leaves = factsObj?.takeIf { !it.isNull("leaves") }?.optInt("leaves"),
            stem = rawStem,
            daysWait = factsObj?.takeIf { !it.isNull("days_wait") }?.optInt("days_wait"),
            healingDay = factsObj?.takeIf { !it.isNull("healing_day") }?.optInt("healing_day"),
        )

        val verdict = Verdict.fromWire(json.optString("verdict", "unclear"))
        val title = json.optString("title", "").trim().ifEmpty { "-" }
        val message = json.optString("message", "").trim()
        val speak = json.optString("speak", "").trim().ifEmpty { message.ifEmpty { title } }

        return AnalyzeResult(
            id = UUID.randomUUID().toString(),
            mode = mode,
            lang = lang.code,
            verdict = verdict,
            title = title,
            message = message,
            steps = steps,
            facts = facts,
            speak = speak,
            marks = marks,
            createdAtMillis = System.currentTimeMillis(),
        )
    }

    private fun parseMarks(array: JSONArray?): List<GuideMark> {
        if (array == null) return emptyList()
        val out = mutableListOf<GuideMark>()
        for (i in 0 until array.length()) {
            if (out.size >= 6) break
            val obj = array.optJSONObject(i) ?: continue
            val kind = MarkKind.fromWire(obj.optString("kind", "")) ?: continue
            val ptsArr = obj.optJSONArray("pts") ?: continue
            val pts = List(ptsArr.length()) { idx ->
                (ptsArr.optDouble(idx, 0.0) / 1000.0).toFloat().coerceIn(0f, 1f)
            }
            val label = obj.optString("label", "").trim().take(30)
            if ((kind == MarkKind.CLIP || kind == MarkKind.REMOVE) && pts.size >= 2) {
                out.add(GuideMark(kind = kind, label = label, x1 = pts[1], y1 = pts[0]))
            } else if (pts.size >= 4) {
                out.add(
                    GuideMark(
                        kind = kind,
                        label = label,
                        x1 = pts[1],
                        y1 = pts[0],
                        x2 = pts[3],
                        y2 = pts[2],
                    )
                )
            }
        }
        return out
    }

    private fun buildPrompt(mode: ScanMode, lang: AppLang): String {
        val common = """
You help small Indian farmers who often cannot read well. Be very simple.
Return ONLY one JSON object matching the schema.
ICON must be one of: water, sun, shade, cover, clock, cut, leaf, warning, check, camera, remove, medicine, thermometer, wind.
If the photo is blurry, too dark, or not a tomato / vegetable seedling, use verdict "unclear", say how to take a better photo, and use icon "camera".
Never invent precise numbers you cannot see. No scientific words. No percentages.
""".trimIndent()

        val modeRule = when (mode) {
            ScanMode.SEEDLING -> """
Task: Look at this tomato (or brinjal/chilli) seedling photo. Is it ready for grafting (joining a rootstock and a scion)?
Ready signs: stem about as thick as a pencil lead (1.5-2.5 mm), 2 to 4 true leaves, healthy green colour, straight stem, about 2-3 weeks old.
- "good": ready to graft now.
- "wait": healthy but too small or stem too thin; set facts.days_wait to your best guess (days).
- "bad": sick, yellow, spots, rotten, broken or too old/woody stem. Say what to do (remove plant, separate it, etc.).
facts.stem = thin / ok / thick. facts.leaves = count of true leaves if visible.
Steps examples: when ready -> cut at slant with clean blade, choose same thickness partner, wash hands/blade.
Return 1 to 3 steps. Do not populate marks.
""".trimIndent()

            ScanMode.GRAFT -> """
Task: Look at this grafted tomato plant photo (a top plant joined on a root plant, often with a clip). Is the graft joint healing?
- "good": top plant upright, green, fresh; joint looks joined.
- "wait": top plant slightly drooping or soft; needs care and watching.
- "bad": strong wilting, black/brown rot at the joint, joint broken or top plant dried. Say to remove if it is dead.
Care knowledge: first 3 days keep in dark/shade with high humidity (cover with plastic or dome, spray mist). Days 4-7 slowly open cover and give more light. Remove clip after about 10-14 days. Never give direct strong sun early. Little droop on day 1 is normal.
facts.healing_day = your best guess of days since grafting, or null.
Return 1 to 3 steps. Do not populate marks.
""".trimIndent()

            ScanMode.GUIDE -> """
Task: The farmer is about to graft tomato seedlings now (splice / tube grafting). The photo shows one or two seedlings (root plant = rootstock, top plant = scion), maybe in a tray or held in hand. Show EXACTLY on the photo where to act.
Add a "marks" array (max 6 items). Each mark: {"kind": KIND, "label": "max 3 words", "pts": [...]}
- "rootstock": box around the root plant, pts [ymin, xmin, ymax, xmax]
- "scion": box around the top plant, pts [ymin, xmin, ymax, xmax]
- "cut": slanted line across the stem where to cut, pts [y1, x1, y2, x2]. Root plant: cut just BELOW the two seed leaves (cotyledons), 45 degree slant. Top plant: cut just ABOVE the seed leaves, same slant. Line about 2-3 stem widths long, centred on the stem.
- "clip": point where the clip goes (on the root plant at its cut), pts [y, x]
- "remove": point on a side shoot / damaged leaf to remove (only if needed), pts [y, x]
All coordinates are integers 0-1000 relative to image height (y) and width (x). Put marks exactly on the visible stems.
If only one plant is visible, mark its cut and say in the message if it suits as root or top plant.
steps: 3 to 5 steps in order. Each step has "mark": 0-based index of its related mark in "marks", or null.
verdict: "good" if ready to graft (stem thickness fine, healthy); "wait" if too small; "bad" if sick; "unclear" if stems not visible.
""".trimIndent()
        }

        val langRule = when (lang) {
            AppLang.HI -> "Write title, message, steps, mark labels and speak in very simple spoken Hindi (Devanagari script), village-level words, like talking to a farmer. Avoid English words except very common ones like 'प्लास्टिक'."
            AppLang.EN -> "Write title, message, steps, mark labels and speak in very simple English, short common words, like talking to a farmer."
        }

        return "$common\n\n$modeRule\n\n$langRule"
    }
}
