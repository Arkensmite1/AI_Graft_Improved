package com.aigraft.farmer.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aigraft.farmer.audio.SpeechState
import com.aigraft.farmer.data.GraftBatch
import com.aigraft.farmer.data.ScanRecord
import com.aigraft.farmer.data.dayOfBatch
import com.aigraft.farmer.i18n.AppLang
import com.aigraft.farmer.i18n.AppStrings
import com.aigraft.farmer.i18n.GRAFT_PLAN
import com.aigraft.farmer.i18n.PLAN_DAYS
import com.aigraft.farmer.i18n.taskForDay
import com.aigraft.farmer.notifications.ReminderScheduler
import com.aigraft.farmer.ui.components.SpeakButton
import com.aigraft.farmer.ui.components.planIconVector
import com.aigraft.farmer.ui.theme.AiGraftTokens
import com.aigraft.farmer.ui.theme.Baloo2FontFamily
import java.io.File

@Composable
fun GraftDetailScreen(
    batch: GraftBatch?,
    attachedScans: List<ScanRecord>,
    lang: AppLang,
    strings: AppStrings,
    speechState: SpeechState,
    onSpeak: (String, String) -> Unit,
    onStopSpeech: () -> Unit,
    onToggleDayDone: (Int) -> Unit,
    onEnableReminders: () -> Unit,
    onTakePhoto: (String) -> Unit,
    onOpenScanResult: (String) -> Unit,
    onDeleteBatch: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val colors = AiGraftTokens.colors
    val haptics = LocalHapticFeedback.current
    var confirmDelete by remember { mutableStateOf(false) }
    var askedNotifOnce by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { onStopSpeech() }
    }

    LaunchedEffect(batch) {
        if (batch == null) onBack()
    }
    if (batch == null) return

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        askedNotifOnce = true
        if (granted) onEnableReminders()
    }

    val day = dayOfBatch(batch)
    val finished = day > PLAN_DAYS
    val task = taskForDay(day)
    val doneToday = day in batch.doneDays

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceSecondary)
                    .clickable(onClick = onBack)
                    .testTag("graft_back_button"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = strings.done,
                    tint = colors.onSurface,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = batch.name,
                    fontFamily = Baloo2FontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("graft_detail_name"),
                )
                Text(
                    text = "${batch.plants} ${strings.plants} · ${if (finished) strings.finished else "${strings.dayN} $day ${strings.dayOf} $PLAN_DAYS"}",
                    fontFamily = Baloo2FontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = colors.muted,
                )
            }
        }
        HorizontalDivider(color = colors.divider, thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Today's Work Hero Card
            if (!finished) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("today_task_card"),
                    shape = RoundedCornerShape(24.dp),
                    color = colors.brandSecondary,
                    border = BorderStroke(2.dp, colors.brandTertiary),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = strings.todayWork,
                            fontFamily = Baloo2FontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = colors.onBrandSecondary,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(colors.brandPrimary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = planIconVector(task.icon),
                                    contentDescription = null,
                                    tint = colors.onBrandPrimary,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                            Text(
                                text = task.text(lang),
                                fontFamily = Baloo2FontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                lineHeight = 29.sp,
                                color = colors.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("today_task_text"),
                            )
                        }

                        SpeakButton(
                            id = "plan_${batch.id}_${day}_${lang.code}",
                            text = "${strings.dayN} $day. ${task.text(lang)}",
                            speechState = speechState,
                            strings = strings,
                            onSpeak = onSpeak,
                            onStop = onStopSpeech,
                            testTagName = "today_speak_button",
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("today_done_button"),
                                shape = RoundedCornerShape(999.dp),
                                color = if (doneToday) colors.success else colors.surface,
                                border = if (doneToday) null else BorderStroke(2.dp, colors.borderStrong),
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleDayDone(day)
                                },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                ) {
                                    Icon(
                                        imageVector = if (doneToday) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (doneToday) colors.onSuccess else colors.onSurface,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Text(
                                        text = if (doneToday) strings.doneToday else strings.markDone,
                                        fontFamily = Baloo2FontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = if (doneToday) colors.onSuccess else colors.onSurface,
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("today_photo_button"),
                                shape = RoundedCornerShape(999.dp),
                                color = colors.surface,
                                border = BorderStroke(2.dp, colors.borderStrong),
                                onClick = { onTakePhoto(batch.id) },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PhotoCamera,
                                        contentDescription = null,
                                        tint = colors.onSurface,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Text(
                                        text = strings.takePhoto,
                                        fontFamily = Baloo2FontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp,
                                        color = colors.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }

                // Reminder status badge or turn-on banner
                if (batch.reminders) {
                    Row(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.brandSecondary)
                            .padding(horizontal = 16.dp)
                            .testTag("reminders_on"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Alarm,
                            contentDescription = null,
                            tint = colors.onBrandSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = strings.remindersOn,
                            fontFamily = Baloo2FontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = colors.onBrandSecondary,
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reminders_off"),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceSecondary,
                        border = BorderStroke(1.dp, colors.warning),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsOff,
                                contentDescription = null,
                                tint = colors.warning,
                                modifier = Modifier.size(26.dp),
                            )
                            Text(
                                text = strings.remindersOff,
                                fontFamily = Baloo2FontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = colors.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Surface(
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("reminders_turn_on"),
                                shape = RoundedCornerShape(999.dp),
                                color = colors.brandPrimary,
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderScheduler.hasPermission(context)) {
                                        if (askedNotifOnce) {
                                            val intent = Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", context.packageName, null)
                                            )
                                            context.startActivity(intent)
                                        } else {
                                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    } else {
                                        onEnableReminders()
                                    }
                                },
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = if (askedNotifOnce && !ReminderScheduler.hasPermission(context)) {
                                            strings.openSettings
                                        } else {
                                            strings.turnOn
                                        },
                                        fontFamily = Baloo2FontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = colors.onBrandPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Attached Photos Strip
            if (attachedScans.isNotEmpty()) {
                Text(
                    text = strings.photos,
                    fontFamily = Baloo2FontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = colors.onSurface,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                ) {
                    items(attachedScans, key = { it.id }) { scan ->
                        AsyncImage(
                            model = File(scan.thumbPath),
                            contentDescription = scan.result.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.surfaceTertiary)
                                .clickable { onOpenScanResult(scan.id) }
                                .testTag("graft_photo_${scan.id}"),
                        )
                    }
                }
            }

            // 15-Day Care Timeline
            Text(
                text = strings.allDays,
                fontFamily = Baloo2FontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = colors.onSurface,
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GRAFT_PLAN.forEach { p ->
                    val isToday = p.day == day
                    val isPast = p.day < day
                    val isFuture = p.day > day
                    val isDone = p.day in batch.doneDays

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .alpha(if (isFuture) 0.55f else 1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isToday) colors.surfaceSecondary else colors.surface)
                            .then(
                                if (isToday) {
                                    Modifier.border(2.dp, colors.brandPrimary, RoundedCornerShape(16.dp))
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(enabled = !isFuture) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleDayDone(p.day)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("plan_day_${p.day}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val dotBg = when {
                            isDone -> colors.success
                            isToday -> colors.brandPrimary
                            else -> colors.surface
                        }
                        val dotBorder = when {
                            isDone -> colors.success
                            isToday -> colors.brandPrimary
                            isPast -> colors.warning
                            else -> colors.borderStrong
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(dotBg)
                                .border(2.dp, dotBorder, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = strings.doneToday,
                                    tint = colors.onSuccess,
                                    modifier = Modifier.size(22.dp),
                                )
                            } else {
                                Text(
                                    text = "${p.day}",
                                    fontFamily = Baloo2FontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = if (isToday) colors.onBrandPrimary else colors.onSurface,
                                )
                            }
                        }

                        Icon(
                            imageVector = planIconVector(p.icon),
                            contentDescription = null,
                            tint = if (p.milestone) colors.brandPrimary else colors.muted,
                            modifier = Modifier.size(24.dp),
                        )

                        Text(
                            text = p.text(lang),
                            fontFamily = Baloo2FontFamily,
                            fontWeight = if (p.milestone) FontWeight.ExtraBold else FontWeight.Normal,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            color = colors.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Inline Delete Confirmation
            Spacer(modifier = Modifier.height(8.dp))
            if (confirmDelete) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    Text(
                        text = strings.sureDeleteGraft,
                        fontFamily = Baloo2FontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = colors.onSurface,
                    )
                    Surface(
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("confirm_delete_graft_yes"),
                        shape = RoundedCornerShape(999.dp),
                        color = colors.error,
                        onClick = onDeleteBatch,
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = strings.yes,
                                fontFamily = Baloo2FontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = colors.onError,
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("confirm_delete_graft_no"),
                        shape = RoundedCornerShape(999.dp),
                        color = colors.surfaceSecondary,
                        onClick = { confirmDelete = false },
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = strings.no,
                                fontFamily = Baloo2FontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = colors.onSurface,
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { confirmDelete = true }
                        .testTag("delete_graft_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = strings.deleteGraft,
                        fontFamily = Baloo2FontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = colors.error,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
