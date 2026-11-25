package com.aigraft.farmer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigraft.farmer.data.GraftBatch
import com.aigraft.farmer.i18n.AppLang
import com.aigraft.farmer.i18n.AppStrings
import com.aigraft.farmer.ui.components.BatchCard
import com.aigraft.farmer.ui.theme.AiGraftTokens
import com.aigraft.farmer.ui.theme.Baloo2FontFamily

@Composable
fun GraftsScreen(
    lang: AppLang,
    strings: AppStrings,
    batches: List<GraftBatch>,
    onNewGraft: () -> Unit,
    onOpenGraftDetail: (String) -> Unit,
    onToggleBatchDayDone: (String, Int) -> Unit,
) {
    val colors = AiGraftTokens.colors
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = strings.grafts,
                fontFamily = Baloo2FontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                color = colors.onSurface,
            )
        }
        HorizontalDivider(color = colors.divider, thickness = 1.dp)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("grafts_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .testTag("grafted_today_button"),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.brandPrimary,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNewGraft()
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(colors.surface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = strings.graftedToday,
                                tint = colors.brandPrimary,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.graftedToday,
                                fontFamily = Baloo2FontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                lineHeight = 28.sp,
                                color = colors.onBrandPrimary,
                            )
                            Text(
                                text = strings.graftedTodaySub,
                                fontFamily = Baloo2FontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp,
                                color = colors.onBrandPrimary.copy(alpha = 0.9f),
                            )
                        }
                    }
                }
            }

            if (batches.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp)
                            .testTag("grafts_empty"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = colors.brandPrimary,
                            modifier = Modifier.size(56.dp),
                        )
                        Text(
                            text = strings.noGrafts,
                            fontFamily = Baloo2FontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = colors.onSurface,
                        )
                        Text(
                            text = strings.noGraftsSub,
                            fontFamily = Baloo2FontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            color = colors.muted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                items(batches, key = { it.id }) { batch ->
                    BatchCard(
                        batch = batch,
                        lang = lang,
                        strings = strings,
                        onClick = { onOpenGraftDetail(batch.id) },
                        onToggleTodayDone = { day -> onToggleBatchDayDone(batch.id, day) },
                        testTagName = "graft_card_${batch.id}",
                    )
                }
            }
        }
    }
}
