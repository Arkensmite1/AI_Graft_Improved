package com.aigraft.farmer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigraft.farmer.data.GuideMark
import com.aigraft.farmer.data.MarkKind
import com.aigraft.farmer.ui.theme.AiGraftTokens
import com.aigraft.farmer.ui.theme.Baloo2FontFamily
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun GuideOverlay(
    marks: List<GuideMark>,
    stepOf: (Int) -> Int?,
    activeStep: Int?,
    modifier: Modifier = Modifier,
) {
    val colors = AiGraftTokens.colors
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("guide_overlay")
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            marks.forEachIndexed { index, m ->
                val col = markColor(m.kind, colors)
                val dimmed = activeStep != null && stepOf(index) != activeStep
                val alpha = if (dimmed) 0.25f else 1f
                val strong = activeStep != null && !dimmed

                when (m.kind) {
                    MarkKind.ROOTSTOCK, MarkKind.SCION -> {
                        val x2 = m.x2 ?: m.x1
                        val y2 = m.y2 ?: m.y1
                        val left = min(m.x1, x2) * w
                        val top = min(m.y1, y2) * h
                        val rectW = abs(x2 - m.x1) * w
                        val rectH = abs(y2 - m.y1) * h
                        drawRoundRect(
                            color = col.copy(alpha = alpha),
                            topLeft = Offset(left, top),
                            size = Size(rectW, rectH),
                            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                            style = Stroke(
                                width = (if (strong) 5.dp else 3.dp).toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 14f), 0f)
                            )
                        )
                    }

                    MarkKind.CUT -> {
                        val start = Offset(m.x1 * w, m.y1 * h)
                        val end = Offset((m.x2 ?: m.x1) * w, (m.y2 ?: m.y1) * h)
                        // White halo for sun-glare readability
                        drawLine(
                            color = colors.surface.copy(alpha = alpha),
                            start = start,
                            end = end,
                            strokeWidth = (if (strong) 12.dp else 10.dp).toPx(),
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = col.copy(alpha = alpha),
                            start = start,
                            end = end,
                            strokeWidth = (if (strong) 7.dp else 5.dp).toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(26f, 14f), 0f),
                        )
                    }

                    MarkKind.CLIP, MarkKind.REMOVE -> {
                        val center = Offset(m.x1 * w, m.y1 * h)
                        val radius = (if (strong) 22.dp else 18.dp).toPx()
                        drawCircle(
                            color = colors.surface.copy(alpha = alpha),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 7.dp.toPx()),
                        )
                        drawCircle(
                            color = col.copy(alpha = alpha),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 4.dp.toPx()),
                        )
                    }
                }
            }
        }

        if (widthPx > 0f && heightPx > 0f) {
            val maxTagWidthPx = with(density) { 150.dp.toPx() }
            val tagHeightPx = with(density) { 36.dp.toPx() }
            val padPx = with(density) { 4.dp.toPx() }

            marks.forEachIndexed { index, m ->
                val step = stepOf(index)
                val dimmed = activeStep != null && step != activeStep
                val isBox = m.kind == MarkKind.ROOTSTOCK || m.kind == MarkKind.SCION
                val col = markColor(m.kind, colors)

                val cx = if (isBox) {
                    min(m.x1, m.x2 ?: m.x1) * widthPx
                } else {
                    ((m.x1 + (m.x2 ?: m.x1)) / 2f) * widthPx
                }
                val cy = if (isBox) {
                    min(m.y1, m.y2 ?: m.y1) * heightPx
                } else {
                    ((m.y1 + (m.y2 ?: m.y1)) / 2f) * heightPx
                }

                val leftPx = (if (isBox) cx + padPx else cx + with(density) { 20.dp.toPx() })
                    .coerceIn(padPx, (widthPx - maxTagWidthPx).coerceAtLeast(padPx))
                val topPx = (if (isBox) cy + padPx else cy - with(density) { 16.dp.toPx() })
                    .coerceIn(padPx, (heightPx - tagHeightPx).coerceAtLeast(padPx))

                Row(
                    modifier = Modifier
                        .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                        .alpha(if (dimmed) 0.3f else 1f)
                        .height(30.dp)
                        .widthIn(max = 146.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.surface)
                        .border(2.dp, col, RoundedCornerShape(999.dp))
                        .padding(start = 3.dp, end = 8.dp)
                        .testTag("guide_mark_$index"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (step != null) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(col),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${step + 1}",
                                fontFamily = Baloo2FontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = if (m.kind == MarkKind.CUT || m.kind == MarkKind.REMOVE || m.kind == MarkKind.ROOTSTOCK) {
                                    colors.surface
                                } else {
                                    colors.onSurface
                                },
                            )
                        }
                    } else {
                        Icon(
                            imageVector = markIconVector(m.kind),
                            contentDescription = m.label.ifBlank { m.kind.wire },
                            tint = colors.onSurface,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    if (m.label.isNotBlank()) {
                        Text(
                            text = m.label,
                            fontFamily = Baloo2FontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = colors.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
