package com.aigraft.farmer.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aigraft.farmer.audio.SpeechState
import com.aigraft.farmer.data.AnalyzeResult
import com.aigraft.farmer.data.PendingScan
import com.aigraft.farmer.data.ScanMode
import com.aigraft.farmer.data.Verdict
import com.aigraft.farmer.i18n.AppStrings
import com.aigraft.farmer.ui.components.GuideOverlay
import com.aigraft.farmer.ui.components.SpeakButton
import com.aigraft.farmer.ui.components.stepIconVector
import com.aigraft.farmer.ui.components.verdictVisual
import com.aigraft.farmer.ui.theme.AiGraftTokens
import com.aigraft.farmer.ui.theme.Baloo2FontFamily
import com.aigraft.farmer.viewmodel.AnalysisUiState
import java.io.File

@Composable
fun ResultScreen(
    scanId: String?,
    strings: AppStrings,
    pendingScan: PendingScan?,
    analysisState: AnalysisUiState,
    speechState: SpeechState,
    onLoadExisting: (String) -> Unit,
    onAnalyzePending: () -> Unit,
    onSpeak: (String, String) -> Unit,
    onStopSpeech: () -> Unit,
    onNewPhoto: (ScanMode, String?) -> Unit,
    onTrackGraft: (String, Int) -> Unit,
    onDone: () -> Unit,
) {
    val colors = AiGraftTokens.colors
    var activeStep by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(scanId) {
        activeStep = null
        if (!scanId.isNullOrBlank()) {
            onLoadExisting(scanId)
        } else {
            onAnalyzePending()
        }
    }

    DisposableEffect(Unit) {
        onDispose { onStopSpeech() }
    }

    val successScan = (analysisState as? AnalysisUiState.Success)?.scan
    val photoPath = successScan?.photoPath ?: pendingScan?.photoPath
    val mode = successScan?.mode ?: pendingScan?.mode ?: ScanMode.SEEDLING
    val batchId = successScan?.batchId ?: pendingScan?.batchId
    val aspect = (successScan?.aspectRatio ?: pendingScan?.aspectRatio ?: (4f / 3.4f)).coerceIn(0.65f, 1.6f)
    val marks = successScan?.result?.marks.orEmpty()
    val stepOf: (Int) -> Int? = { markIdx ->
        val idx = successScan?.result?.steps?.indexOfFirst { it.mark == markIdx } ?: -1
        if (idx >= 0) idx else null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 112.dp)
        ) {
            // Photo Header + GuideOverlay + Scanning Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect)
                    .background(colors.surfaceInverse)
            ) {
                if (!photoPath.isNullOrBlank()) {
                    AsyncImage(
                        model = File(photoPath),
                        contentDescription = successScan?.result?.title ?: strings.checking,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("result_photo"),
                    )
                }

                if (marks.isNotEmpty() && analysisState is AnalysisUiState.Success) {
                    GuideOverlay(
                        marks = marks,
                        stepOf = stepOf,
                        activeStep = activeStep,
                    )
                }

                if (analysisState is AnalysisUiState.Loading) {
                    ScanningLine()
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 16.dp, top = 8.dp)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(onClick = onDone)
                        .testTag("result_back_button"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = strings.done,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            // Overlapping White Sheet Body
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-24).dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = colors.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (analysisState) {
                        is AnalysisUiState.Idle,
                        is AnalysisUiState.Loading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                                    .testTag("analyzing_indicator"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(color = colors.brandPrimary)
                                Text(
                                    text = strings.checking,
                                    fontFamily = Baloo2FontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp,
                                    color = colors.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }

                        is AnalysisUiState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                                    .testTag("analysis_error"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudOff,
                                    contentDescription = null,
                                    tint = colors.error,
                                    modifier = Modifier.size(56.dp),
                                )
                                Text(
                                    text = strings.failTitle,
                                    fontFamily = Baloo2FontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp,
                                    color = colors.onSurface,
                                )
                                Text(
                                    text = strings.failText,
                                    fontFamily = Baloo2FontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 17.sp,
                                    color = colors.muted,
                                    textAlign = TextAlign.Center,
                                )
                                if (analysisState.message.isNotBlank()) {
                                    Text(
                                        text = analysisState.message,
                                        fontFamily = Baloo2FontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = colors.muted,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                if (analysisState.canRetry) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        modifier = Modifier
                                            .height(60.dp)
                                            .testTag("retry_button"),
                                        shape = RoundedCornerShape(999.dp),
                                        color = colors.brandPrimary,
                                        onClick = onAnalyzePending,
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 32.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Refresh,
                                                contentDescription = null,
                                                tint = colors.onBrandPrimary,
                                                modifier = Modifier.size(26.dp),
                                            )
                                            Text(
                                                text = strings.tryAgain,
                                                fontFamily = Baloo2FontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 20.sp,
                                                color = colors.onBrandPrimary,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is AnalysisUiState.Success -> {
                            val scan = analysisState.scan
                            ResultContent(
                                result = scan.result,
                                mode = scan.mode,
                                strings = strings,
                                speechState = speechState,
                                activeStep = activeStep,
                                onStepSelect = { activeStep = it },
                                onSpeak = onSpeak,
                                onStopSpeech = onStopSpeech,
                            )

                            if (scan.mode == ScanMode.GRAFT &&
                                scan.batchId == null &&
                                scan.result.verdict != Verdict.UNCLEAR
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp)
                                        .testTag("track_graft_button"),
                                    shape = RoundedCornerShape(999.dp),
                                    color = colors.brandSecondary,
                                    border = BorderStroke(2.dp, colors.brandPrimary),
                                    onClick = {
                                        onTrackGraft(scan.id, scan.result.facts.healingDay ?: 0)
                                    },
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CalendarMonth,
                                            contentDescription = null,
                                            tint = colors.onBrandSecondary,
                                            modifier = Modifier.size(26.dp),
                                        )
                                        Text(
                                            text = strings.trackGraft,
                                            fontFamily = Baloo2FontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 19.sp,
                                            color = colors.onBrandSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sticky Footer
        if (analysisState !is AnalysisUiState.Loading && analysisState !is AnalysisUiState.Idle) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(colors.surface)
            ) {
                HorizontalDivider(color = colors.border, thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("new_photo_button"),
                        shape = RoundedCornerShape(999.dp),
                        color = colors.surfaceSecondary,
                        border = BorderStroke(2.dp, colors.borderStrong),
                        onClick = { onNewPhoto(mode, batchId) },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoCamera,
                                contentDescription = null,
                                tint = colors.onSurface,
                                modifier = Modifier.size(26.dp),
                            )
                            Text(
                                text = strings.newPhoto,
                                fontFamily = Baloo2FontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = colors.onSurface,
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .testTag("done_button"),
                        shape = RoundedCornerShape(999.dp),
                        color = colors.brandPrimary,
                        onClick = onDone,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = colors.onBrandPrimary,
                                modifier = Modifier.size(26.dp),
                            )
                            Text(
                                text = strings.done,
                                fontFamily = Baloo2FontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = colors.onBrandPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanningLine() {
    val colors = AiGraftTokens.colors
    val infinite = rememberInfiniteTransition(label = "scan_line")
    val fraction by infinite.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.94f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan_y",
    )
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = maxHeight * fraction)
                .height(4.dp)
                .background(colors.brandTertiary)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultContent(
    result: AnalyzeResult,
    mode: ScanMode,
    strings: AppStrings,
    speechState: SpeechState,
    activeStep: Int?,
    onStepSelect: (Int?) -> Unit,
    onSpeak: (String, String) -> Unit,
    onStopSpeech: () -> Unit,
) {
    val colors = AiGraftTokens.colors
    val haptics = LocalHapticFeedback.current
    val v = verdictVisual(result.verdict, colors, strings)
    val hasMarks = result.marks.isNotEmpty()

    val f = result.facts
    val stemLabel = when (f.stem) {
        "thin" -> strings.stemThin
        "thick" -> strings.stemThick
        "ok" -> strings.stemOk
        else -> null
    }
    val chips = buildList<Pair<ImageVector, String>> {
        if ((f.leaves ?: 0) > 0) add(Icons.Rounded.Eco to "${strings.leaves}: ${f.leaves}")
        if (stemLabel != null) add(Icons.Rounded.Straighten to "${strings.stem}: $stemLabel")
        if ((f.daysWait ?: 0) > 0) add(Icons.Rounded.Schedule to "${strings.waitFor} ${f.daysWait} ${strings.days}")
        if (mode == ScanMode.GRAFT && f.healingDay != null) {
            add(Icons.Rounded.CalendarMonth to "${strings.dayN} ${f.healingDay}")
        }
    }

    // Traffic-light Verdict Card
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("verdict_card_${result.verdict.wire}"),
        shape = RoundedCornerShape(20.dp),
        color = v.bg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = v.icon,
                    contentDescription = v.label,
                    tint = v.fg,
                    modifier = Modifier.size(44.dp),
                )
                Text(
                    text = v.label,
                    fontFamily = Baloo2FontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp,
                    lineHeight = 38.sp,
                    color = v.fg,
                    modifier = Modifier.testTag("verdict_label"),
                )
            }
            Text(
                text = result.title,
                fontFamily = Baloo2FontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                lineHeight = 29.sp,
                color = v.fg,
                modifier = Modifier.testTag("result_title"),
            )
            if (result.message.isNotBlank()) {
                Text(
                    text = result.message,
                    fontFamily = Baloo2FontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp,
                    lineHeight = 25.sp,
                    color = v.fg,
                    modifier = Modifier.testTag("result_message"),
                )
            }
        }
    }

    // Big Listen Button
    SpeakButton(
        id = "result-${result.id}",
        text = result.speak,
        speechState = speechState,
        strings = strings,
        onSpeak = onSpeak,
        onStop = onStopSpeech,
        testTagName = "result_speak_button",
    )

    // Fact Chips
    if (chips.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chips.forEachIndexed { idx, (icon, text) ->
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.brandSecondary)
                        .padding(horizontal = 14.dp)
                        .testTag("fact_chip_$idx"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.onBrandSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = text,
                        fontFamily = Baloo2FontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = colors.onBrandSecondary,
                    )
                }
            }
        }
    }

    // What to do steps
    if (result.steps.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = strings.whatToDo,
                fontFamily = Baloo2FontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = colors.onSurface,
            )
            if (hasMarks && activeStep != null) {
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onStepSelect(null) }
                        .padding(horizontal = 8.dp)
                        .testTag("show_all_marks"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = strings.showAll,
                        fontFamily = Baloo2FontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = colors.brandPrimary,
                    )
                }
            }
        }
        if (hasMarks) {
            Text(
                text = strings.tapStep,
                fontFamily = Baloo2FontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = colors.muted,
            )
        }

        result.steps.forEachIndexed { index, step ->
            val selected = activeStep == index
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 76.dp)
                    .testTag("step_card_$index"),
                shape = RoundedCornerShape(20.dp),
                color = if (selected) colors.brandSecondary else colors.surfaceSecondary,
                border = BorderStroke(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) colors.brandPrimary else colors.border,
                ),
                onClick = {
                    if (hasMarks) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onStepSelect(if (selected) null else index)
                    }
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(modifier = Modifier.size(56.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.brandSecondary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = stepIconVector(step.icon),
                                contentDescription = null,
                                tint = colors.onBrandSecondary,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                        if (mode == ScanMode.GUIDE) {
                            Box(
                                modifier = Modifier
                                    .offset(x = (-4).dp, y = (-4).dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(colors.brandPrimary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontFamily = Baloo2FontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = colors.onBrandPrimary,
                                )
                            }
                        }
                    }

                    Text(
                        text = step.text,
                        fontFamily = Baloo2FontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 19.sp,
                        lineHeight = 25.sp,
                        color = colors.onSurface,
                        modifier = Modifier.weight(1f),
                    )

                    if (hasMarks && step.mark != null) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = if (selected) colors.brandPrimary else colors.muted,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}
