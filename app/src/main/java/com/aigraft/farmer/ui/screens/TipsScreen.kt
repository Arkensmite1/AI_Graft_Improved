package com.aigraft.farmer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigraft.farmer.audio.SpeechState
import com.aigraft.farmer.i18n.AppLang
import com.aigraft.farmer.i18n.AppStrings
import com.aigraft.farmer.i18n.GRAFTING_TIPS
import com.aigraft.farmer.ui.components.SpeakButton
import com.aigraft.farmer.ui.components.tipIconVector
import com.aigraft.farmer.ui.theme.AiGraftTokens
import com.aigraft.farmer.ui.theme.Baloo2FontFamily

@Composable
fun TipsScreen(
    lang: AppLang,
    strings: AppStrings,
    speechState: SpeechState,
    onSpeak: (String, String) -> Unit,
    onStopSpeech: () -> Unit,
) {
    val colors = AiGraftTokens.colors

    DisposableEffect(Unit) {
        onDispose { onStopSpeech() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = strings.tipsTitle,
                fontFamily = Baloo2FontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                color = colors.onSurface,
            )
            Text(
                text = strings.tipsSub,
                fontFamily = Baloo2FontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = colors.muted,
            )
        }
        HorizontalDivider(color = colors.divider, thickness = 1.dp)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(GRAFTING_TIPS, key = { _, tip -> tip.id }) { index, tip ->
                val title = tip.title(lang)
                val body = tip.body(lang)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tip_card_${tip.id}"),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 2.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.brandSecondary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = tipIconVector(tip.icon),
                                contentDescription = null,
                                tint = colors.onBrandSecondary,
                                modifier = Modifier.size(30.dp),
                            )
                            Text(
                                text = "${index + 1}",
                                fontFamily = Baloo2FontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = colors.onBrandSecondary,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 6.dp, top = 2.dp),
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontFamily = Baloo2FontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                lineHeight = 25.sp,
                                color = colors.onSurface,
                            )
                            Text(
                                text = body,
                                fontFamily = Baloo2FontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                color = colors.onSurfaceSecondary,
                            )
                        }

                        SpeakButton(
                            id = "tip_${tip.id}_${lang.code}",
                            text = "$title. $body",
                            speechState = speechState,
                            strings = strings,
                            onSpeak = onSpeak,
                            onStop = onStopSpeech,
                            testTagName = "tip_speak_${tip.id}",
                            compact = true,
                        )
                    }
                }
            }
        }
    }
}
