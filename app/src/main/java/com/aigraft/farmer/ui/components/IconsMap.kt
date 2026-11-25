package com.aigraft.farmer.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.aigraft.farmer.data.MarkKind
import com.aigraft.farmer.data.Verdict
import com.aigraft.farmer.i18n.AppStrings
import com.aigraft.farmer.i18n.PlanIcon
import com.aigraft.farmer.i18n.TipIcon
import com.aigraft.farmer.ui.theme.AiGraftColors

data class VerdictVisual(
    val bg: Color,
    val fg: Color,
    val icon: ImageVector,
    val label: String,
)

fun verdictVisual(verdict: Verdict, colors: AiGraftColors, strings: AppStrings): VerdictVisual =
    when (verdict) {
        Verdict.GOOD -> VerdictVisual(colors.success, colors.onSuccess, Icons.Rounded.CheckCircle, strings.vGood)
        Verdict.WAIT -> VerdictVisual(colors.warning, colors.onWarning, Icons.Rounded.Schedule, strings.vWait)
        Verdict.BAD -> VerdictVisual(colors.error, colors.onError, Icons.Rounded.Cancel, strings.vBad)
        Verdict.UNCLEAR -> VerdictVisual(colors.info, colors.onInfo, Icons.Rounded.HelpOutline, strings.vUnclear)
    }

fun stepIconVector(icon: String): ImageVector = when (icon) {
    "water" -> Icons.Rounded.WaterDrop
    "sun" -> Icons.Rounded.WbSunny
    "shade" -> Icons.Rounded.Umbrella
    "cover" -> Icons.Rounded.Inventory2
    "clock" -> Icons.Rounded.Schedule
    "cut" -> Icons.Rounded.ContentCut
    "leaf" -> Icons.Rounded.Eco
    "warning" -> Icons.Rounded.Warning
    "check" -> Icons.Rounded.CheckCircle
    "camera" -> Icons.Rounded.PhotoCamera
    "remove" -> Icons.Rounded.Close
    "medicine" -> Icons.Rounded.Medication
    "thermometer" -> Icons.Rounded.DeviceThermostat
    "wind" -> Icons.Rounded.Air
    else -> Icons.Rounded.CheckCircle
}

fun planIconVector(icon: PlanIcon): ImageVector = when (icon) {
    PlanIcon.COVER -> Icons.Rounded.Inventory2
    PlanIcon.SHADE -> Icons.Rounded.Umbrella
    PlanIcon.CAMERA -> Icons.Rounded.PhotoCamera
    PlanIcon.WIND -> Icons.Rounded.Air
    PlanIcon.SUN -> Icons.Rounded.WbSunny
    PlanIcon.WATER -> Icons.Rounded.WaterDrop
    PlanIcon.EYE -> Icons.Rounded.Visibility
    PlanIcon.CLIP -> Icons.Rounded.ContentCut
    PlanIcon.CHECK -> Icons.Rounded.DoneAll
}

fun tipIconVector(icon: TipIcon): ImageVector = when (icon) {
    TipIcon.LEAF -> Icons.Rounded.Eco
    TipIcon.CUT -> Icons.Rounded.ContentCut
    TipIcon.JOIN -> Icons.Rounded.CallMerge
    TipIcon.COVER -> Icons.Rounded.Inventory2
    TipIcon.SUN -> Icons.Rounded.WbSunny
    TipIcon.EYE -> Icons.Rounded.Visibility
}

fun markColor(kind: MarkKind, colors: AiGraftColors): Color = when (kind) {
    MarkKind.CUT, MarkKind.REMOVE -> colors.error
    MarkKind.CLIP -> colors.warning
    MarkKind.ROOTSTOCK -> colors.info
    MarkKind.SCION -> colors.brandTertiary
}

fun markIconVector(kind: MarkKind): ImageVector = when (kind) {
    MarkKind.CUT -> Icons.Rounded.ContentCut
    MarkKind.CLIP -> Icons.Rounded.AttachFile
    MarkKind.REMOVE -> Icons.Rounded.Close
    MarkKind.ROOTSTOCK -> Icons.Rounded.CallMerge
    MarkKind.SCION -> Icons.Rounded.Eco
}
