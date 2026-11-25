package com.aigraft.farmer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aigraft.farmer.audio.SpeechState
import com.aigraft.farmer.audio.SpeechStatus
import com.aigraft.farmer.data.GraftBatch
import com.aigraft.farmer.data.ScanRecord
import com.aigraft.farmer.data.dayOfBatch
import com.aigraft.farmer.data.formatScanDate
import com.aigraft.farmer.data.modeLabel
import com.aigraft.farmer.i18n.AppLang
import com.aigraft.farmer.i18n.AppStrings
import com.aigraft.farmer.i18n.PLAN_DAYS
import com.aigraft.farmer.i18n.taskForDay
import com.aigraft.farmer.ui.theme.AiGraftTokens
import com.aigraft.farmer.ui.theme.Baloo2FontFamily
import java.io.File

@Composable
fun LangToggle(
    lang: AppLang,
    onSelect: (AppLang) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AiGraftTokens.colors
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.surfaceSecondary)
            .border(1.dp, colors.border, RoundedCornerShape(999.dp))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            AppLang.HI to "हिन्दी",
            AppLang.EN to "English",
        ).forEach { (option, label) ->
            val selected = lang == option
            Box(
                modifier = Modifier
                    .testTag("lang_toggle_${option.code}")
                    .height(40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) colors.brandPrimary else colors.surfaceSecondary)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(option)
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontFamily = Baloo2FontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (selected) colors.onBrandPrimary else colors.onSurfaceSecondary,
                )
            }
        }
    }
}

@Composable
fun SpeakButton(
    id: String,
    text: String,
    speechState: SpeechState,
    strings: AppStrings,
    onSpeak: (String, String) -> Unit,
    onStop: () -> Unit,
    testTagName: String,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = AiGraftTokens.colors
    val haptics = LocalHapticFeedback.current
    val isMine = speechState.activeId == id
    val status = if (isMine) speechState.status else SpeechStatus.IDLE

    val icon = when (status) {
        SpeechStatus.PLAYING -> Icons.Rounded.Stop
        SpeechStatus.ERROR -> Icons.Rounded.Refresh
        else -> Icons.AutoMirrored.Rounded.VolumeUp
    }
    val label = when (status) {
        SpeechStatus.PLAYING -> strings.stop
        SpeechStatus.LOADING -> strings.loadingVoice
        SpeechStatus.ERROR -> strings.voiceError
        SpeechStatus.IDLE -> strings.listen
    }

    val onClick = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (status == SpeechStatus.PLAYING || status == SpeechStatus.LOADING) {
            onStop()
        } else {
            onSpeak(id, text)
        }
    }

    if (compact) {
        Surface(
            modifier = modifier
                .size(52.dp)
                .testTag(testTagName),
            shape = CircleShape,
            color = if (status == SpeechStatus.ERROR) colors.error else colors.brandPrimary,
            onClick = onClick,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (status == SpeechStatus.LOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = colors.onBrandPrimary,
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = colors.onBrandPrimary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
        return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .testTag(testTagName),
        shape = RoundedCornerShape(999.dp),
        color = if (status == SpeechStatus.ERROR) colors.error else colors.brandPrimary,
        shadowElevation = 4.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                fontFamily = Baloo2FontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = colors.onBrandPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (status == SpeechStatus.LOADING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(30.dp),
                    color = colors.onBrandPrimary,
                    strokeWidth = 3.dp,
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = colors.onBrandPrimary,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}

@Composable
fun ScanRow(
    scan: ScanRecord,
    lang: AppLang,
    strings: AppStrings,
    onClick: () -> Unit,
    testTagName: String,
    modifier: Modifier = Modifier,
) {
    val colors = AiGraftTokens.colors
    val v = verdictVisual(scan.result.verdict, colors, strings)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTagName),
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 2.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = File(scan.thumbPath),
                contentDescription = scan.result.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceTertiary),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scan.result.title,
                    fontFamily = Baloo2FontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${modeLabel(scan.mode, strings)} · ${formatScanDate(scan.createdAtMillis, lang, strings)}",
                    fontFamily = Baloo2FontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(v.bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = v.icon,
                    contentDescription = v.label,
                    tint = v.fg,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
fun BatchCard(
    batch: GraftBatch,
    lang: AppLang,
    strings: AppStrings,
    onClick: () -> Unit,
    onToggleTodayDone: (Int) -> Unit,
    testTagName: String,
    modifier: Modifier = Modifier,
) {
    val colors = AiGraftTokens.colors
    val haptics = LocalHapticFeedback.current
    val day = dayOfBatch(batch)
    val finished = day > PLAN_DAYS
    val task = taskForDay(day)
    val done = day in batch.doneDays
    val progress = ((day + 1).toFloat() / (PLAN_DAYS + 1).toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTagName),
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 2.dp,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = batch.name,
                        fontFamily = Baloo2FontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${batch.plants} ${strings.plants} · ${if (finished) strings.finished else "${strings.dayN} $day ${strings.dayOf} $PLAN_DAYS"}",
                        fontFamily = Baloo2FontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        color = colors.muted,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.size(26.dp),
                )
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.surfaceTertiary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(colors.brandPrimary)
                )
            }

            if (!finished) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceSecondary)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (task.milestone) colors.brandPrimary else colors.brandSecondary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = planIconVector(task.icon),
                            contentDescription = null,
                            tint = if (task.milestone) colors.onBrandPrimary else colors.onBrandSecondary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Text(
                        text = task.text(lang),
                        fontFamily = Baloo2FontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        lineHeight = 23.sp,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("${testTagName}_done_toggle")
                            .clip(CircleShape)
                            .background(if (done) colors.success else colors.surface)
                            .border(
                                width = 3.dp,
                                color = if (done) colors.success else colors.borderStrong,
                                shape = CircleShape,
                            )
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleTodayDone(day)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = if (done) strings.doneToday else strings.markDone,
                            tint = if (done) colors.onSuccess else colors.borderStrong,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
    }
}
