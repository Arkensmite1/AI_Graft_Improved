package com.aigraft.farmer.data

import android.graphics.Bitmap
import com.aigraft.farmer.i18n.AppLang
import com.aigraft.farmer.i18n.AppStrings
import com.aigraft.farmer.i18n.PLAN_DAYS
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Serializable
enum class ScanMode(val wire: String) {
    SEEDLING("seedling"),
    GUIDE("guide"),
    GRAFT("graft");

    companion object {
        fun fromWire(value: String?): ScanMode = entries.firstOrNull { it.wire == value } ?: SEEDLING
    }
}

@Serializable
enum class Verdict(val wire: String) {
    GOOD("good"),
    WAIT("wait"),
    BAD("bad"),
    UNCLEAR("unclear");

    companion object {
        fun fromWire(value: String?): Verdict = entries.firstOrNull { it.wire == value } ?: UNCLEAR
    }
}

@Serializable
enum class MarkKind(val wire: String) {
    CUT("cut"),
    CLIP("clip"),
    ROOTSTOCK("rootstock"),
    SCION("scion"),
    REMOVE("remove");

    companion object {
        fun fromWire(value: String?): MarkKind? = entries.firstOrNull { it.wire == value }
    }
}

@Serializable
data class GuideMark(
    val kind: MarkKind,
    val label: String = "",
    val x1: Float,
    val y1: Float,
    val x2: Float? = null,
    val y2: Float? = null,
)

@Serializable
data class AdviceStep(
    val icon: String = "check",
    val text: String,
    val mark: Int? = null,
)

@Serializable
data class PlantFacts(
    val leaves: Int? = null,
    val stem: String? = null, // "thin" | "ok" | "thick"
    val daysWait: Int? = null,
    val healingDay: Int? = null,
)

@Serializable
data class AnalyzeResult(
    val id: String,
    val mode: ScanMode,
    val lang: String,
    val verdict: Verdict,
    val title: String,
    val message: String,
    val steps: List<AdviceStep> = emptyList(),
    val facts: PlantFacts = PlantFacts(),
    val speak: String,
    val marks: List<GuideMark> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis(),
)

@Serializable
data class ScanRecord(
    val id: String,
    val mode: ScanMode,
    val createdAtMillis: Long,
    val thumbPath: String,
    val photoPath: String,
    val aspectRatio: Float = 4f / 3.4f,
    val result: AnalyzeResult,
    val batchId: String? = null,
)

@Serializable
data class GraftBatch(
    val id: String,
    val name: String,
    val plants: Int,
    val graftedAtMillis: Long,
    val doneDays: List<Int> = emptyList(),
    val reminders: Boolean = false,
)

data class PendingScan(
    val id: String,
    val mode: ScanMode,
    val photoPath: String,
    val thumbPath: String,
    val aspectRatio: Float,
    val analysisBitmap: Bitmap,
    val batchId: String? = null,
)

fun startOfDayMillis(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

fun dayOfBatch(batch: GraftBatch, nowMillis: Long = System.currentTimeMillis()): Int {
    val diff = startOfDayMillis(nowMillis) - startOfDayMillis(batch.graftedAtMillis)
    return (diff / 86_400_000L).toInt().coerceAtLeast(0)
}

fun isBatchActive(batch: GraftBatch, nowMillis: Long = System.currentTimeMillis()): Boolean =
    dayOfBatch(batch, nowMillis) <= PLAN_DAYS

fun formatScanDate(millis: Long, lang: AppLang, strings: AppStrings): String {
    val locale = if (lang == AppLang.HI) Locale("hi", "IN") else Locale("en", "IN")
    val days = ((startOfDayMillis(System.currentTimeMillis()) - startOfDayMillis(millis)) / 86_400_000L).toInt()
    val timeFormat = SimpleDateFormat("h:mm a", locale)
    val dateFormat = SimpleDateFormat("d MMM", locale)
    val d = Date(millis)
    return when (days) {
        0 -> "${strings.today}, ${timeFormat.format(d)}"
        1 -> "${strings.yesterday}, ${timeFormat.format(d)}"
        else -> dateFormat.format(d)
    }
}

fun modeLabel(mode: ScanMode, strings: AppStrings): String = when (mode) {
    ScanMode.SEEDLING -> strings.seedling
    ScanMode.GUIDE -> strings.guide
    ScanMode.GRAFT -> strings.graft
}
