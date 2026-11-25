package com.aigraft.farmer.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigraft.farmer.i18n.AppStrings
import com.aigraft.farmer.notifications.ReminderScheduler
import com.aigraft.farmer.ui.theme.AiGraftTokens
import com.aigraft.farmer.ui.theme.Baloo2FontFamily

private val DAY_OPTIONS = (0..10).toList()

@Composable
fun NewGraftScreen(
    initialDaysAgo: Int,
    linkedScanId: String?,
    existingBatchCount: Int,
    strings: AppStrings,
    onSave: (name: String, plants: Int, daysAgo: Int, remindersEnabled: Boolean, linkedScanId: String?) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val colors = AiGraftTokens.colors
    val haptics = LocalHapticFeedback.current

    var name by remember { mutableStateOf("") }
    var plants by remember { mutableIntStateOf(10) }
    var daysAgo by remember { mutableIntStateOf(initialDaysAgo.coerceIn(0, 10)) }
    var remind by remember { mutableStateOf(true) }

    val fallbackName = "${strings.batch} ${existingBatchCount + 1}"

    fun dayLabel(d: Int): String = when (d) {
        0 -> strings.today
        1 -> strings.yesterday
        else -> "$d ${strings.daysAgo}"
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onSave(
            name.trim().ifEmpty { fallbackName },
            plants,
            daysAgo,
            granted,
            linkedScanId,
        )
    }

    val handleSave = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (remind && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ReminderScheduler.hasPermission(context)) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onSave(
                name.trim().ifEmpty { fallbackName },
                plants,
                daysAgo,
                remind,
                linkedScanId,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .imePadding()
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
                    .clickable(onClick = onClose)
                    .testTag("new_graft_close"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = strings.no,
                    tint = colors.onSurface,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = strings.newGraft,
                fontFamily = Baloo2FontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                color = colors.onSurface,
            )
        }
        HorizontalDivider(color = colors.divider, thickness = 1.dp)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = strings.whenGrafted,
                fontFamily = Baloo2FontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = colors.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(DAY_OPTIONS) { d ->
                    val selected = daysAgo == d
                    Surface(
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("days_ago_chip_$d"),
                        shape = RoundedCornerShape(999.dp),
                        color = if (selected) colors.brandPrimary else colors.surface,
                        border = BorderStroke(2.dp, if (selected) colors.brandPrimary else colors.borderStrong),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            daysAgo = d
                        },
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = dayLabel(d),
                                fontFamily = Baloo2FontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = if (selected) colors.onBrandPrimary else colors.onSurface,
                            )
                        }
                    }
                }
            }

            Text(
                text = strings.plants,
                fontFamily = Baloo2FontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = colors.onSurface,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
            )

            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .testTag("plants_minus"),
                    shape = CircleShape,
                    color = colors.brandSecondary,
                    onClick = {
                        plants = (plants - if (plants > 10) 5 else 1).coerceAtLeast(1)
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = "Decrease",
                            tint = colors.onBrandSecondary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Text(
                    text = "$plants",
                    fontFamily = Baloo2FontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(min = 64.dp)
                        .testTag("plants_count"),
                )

                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .testTag("plants_plus"),
                    shape = CircleShape,
                    color = colors.brandSecondary,
                    onClick = {
                        plants += if (plants >= 10) 5 else 1
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Increase",
                            tint = colors.onBrandSecondary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
            }

            Text(
                text = strings.graftName,
                fontFamily = Baloo2FontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = colors.onSurface,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
            )

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 30) name = it },
                placeholder = {
                    Text(
                        text = fallbackName,
                        fontFamily = Baloo2FontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = colors.muted,
                    )
                },
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = Baloo2FontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = colors.onSurface,
                ),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.brandPrimary,
                    unfocusedBorderColor = colors.borderStrong,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("graft_name_input"),
            )

            // Morning Reminder Switch Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceSecondary)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Alarm,
                    contentDescription = null,
                    tint = colors.brandPrimary,
                    modifier = Modifier.size(32.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.remindMe,
                        fontFamily = Baloo2FontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = colors.onSurface,
                    )
                    Text(
                        text = strings.remindMeSub,
                        fontFamily = Baloo2FontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        color = colors.muted,
                    )
                }
                Switch(
                    checked = remind,
                    onCheckedChange = { remind = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = colors.brandPrimary,
                        checkedThumbColor = colors.surface,
                        uncheckedTrackColor = colors.borderStrong,
                        uncheckedThumbColor = colors.surface,
                    ),
                    modifier = Modifier.testTag("reminder_switch"),
                )
            }
        }

        // Sticky Save Footer
        HorizontalDivider(color = colors.border, thickness = 1.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("save_graft_button"),
                shape = RoundedCornerShape(999.dp),
                color = colors.brandPrimary,
                onClick = handleSave,
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
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = strings.save,
                        fontFamily = Baloo2FontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = colors.onBrandPrimary,
                    )
                }
            }
        }
    }
}
