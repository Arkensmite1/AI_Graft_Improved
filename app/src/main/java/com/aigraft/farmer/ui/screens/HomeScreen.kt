package com.aigraft.farmer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aigraft.farmer.data.GraftBatch
import com.aigraft.farmer.data.ScanMode
import com.aigraft.farmer.data.ScanRecord
import com.aigraft.farmer.data.isBatchActive
import com.aigraft.farmer.i18n.AppLang
import com.aigraft.farmer.i18n.AppStrings
import com.aigraft.farmer.ui.components.BatchCard
import com.aigraft.farmer.ui.components.LangToggle
import com.aigraft.farmer.ui.components.ScanRow
import com.aigraft.farmer.ui.theme.AiGraftTokens
import com.aigraft.farmer.ui.theme.Baloo2FontFamily
import kotlinx.coroutines.launch

private const val SEEDLING_IMG =
    "https://images.unsplash.com/photo-1583263840577-b174af9ac796?crop=entropy&cs=srgb&fm=jpg&q=70&w=600"
private const val GUIDE_IMG =
    "https://images.unsplash.com/photo-1592150621744-aca64f48394a?crop=entropy&cs=srgb&fm=jpg&q=70&w=600"
private const val GRAFT_IMG =
    "https://images.unsplash.com/photo-1596199050105-6d5d32222916?crop=entropy&cs=srgb&fm=jpg&q=70&w=600"

private data class HomeModeItem(
    val mode: ScanMode,
    val title: String,
    val sub: String,
    val imageUrl: String,
    val icon: ImageVector,
)

@Composable
fun HomeScreen(
    lang: AppLang,
    strings: AppStrings,
    scans: List<ScanRecord>,
    batches: List<GraftBatch>,
    isPreparingPhoto: Boolean,
    onSelectLang: (AppLang) -> Unit,
    onOpenScan: (ScanMode) -> Unit,
    onPickGalleryPhoto: suspend (android.net.Uri, ScanMode) -> Boolean,
    onOpenResultNew: () -> Unit,
    onOpenResultScan: (String) -> Unit,
    onOpenNewGraft: () -> Unit,
    onOpenGraftDetail: (String) -> Unit,
    onToggleBatchDayDone: (String, Int) -> Unit,
    onOpenTips: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val colors = AiGraftTokens.colors
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var pendingUploadMode by remember { mutableStateOf(ScanMode.SEEDLING) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = onPickGalleryPhoto(uri, pendingUploadMode)
                if (ok) onOpenResultNew()
            }
        }
    }

    val modes = remember(strings) {
        listOf(
            HomeModeItem(ScanMode.SEEDLING, strings.seedlingTitle, strings.seedlingSub, SEEDLING_IMG, Icons.Rounded.Eco),
            HomeModeItem(ScanMode.GUIDE, strings.guideTitle, strings.guideSub, GUIDE_IMG, Icons.Rounded.ContentCut),
            HomeModeItem(ScanMode.GRAFT, strings.graftTitle, strings.graftSub, GRAFT_IMG, Icons.Rounded.CallMerge),
        )
    }

    val activeBatches = remember(batches) { batches.filter { isBatchActive(it) } }
    val recentScans = remember(scans) { scans.take(3) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Brand Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.brandPrimary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Eco,
                            contentDescription = strings.appName,
                            tint = colors.onBrandPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Text(
                        text = strings.appName,
                        fontFamily = Baloo2FontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = colors.brand,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("app_name"),
                    )
                }
                LangToggle(lang = lang, onSelect = onSelectLang)
            }
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Column {
                        Text(
                            text = strings.hello,
                            fontFamily = Baloo2FontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
                            lineHeight = 38.sp,
                            color = colors.onSurface,
                        )
                        Text(
                            text = strings.helloSub,
                            fontFamily = Baloo2FontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 18.sp,
                            lineHeight = 25.sp,
                            color = colors.muted,
                        )
                    }
                }

                // 3 Primary Action Mode Cards
                items(modes, key = { it.mode.wire }) { item ->
                    ModeActionCard(
                        item = item,
                        strings = strings,
                        onTakePhoto = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenScan(item.mode)
                        },
                        onUpload = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            pendingUploadMode = item.mode
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    )
                }

                // Today's Work or "I grafted today" CTA
                if (activeBatches.isNotEmpty()) {
                    item {
                        Text(
                            text = strings.todayWork,
                            fontFamily = Baloo2FontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = colors.onSurface,
                            modifier = Modifier.testTag("home_today_work"),
                        )
                    }
                    items(activeBatches.take(2), key = { "active_${it.id}" }) { batch ->
                        BatchCard(
                            batch = batch,
                            lang = lang,
                            strings = strings,
                            onClick = { onOpenGraftDetail(batch.id) },
                            onToggleTodayDone = { day -> onToggleBatchDayDone(batch.id, day) },
                            testTagName = "home_graft_${batch.id}",
                        )
                    }
                } else {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_grafted_today"),
                            shape = RoundedCornerShape(20.dp),
                            color = colors.brand,
                            onClick = onOpenNewGraft,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = colors.onBrand,
                                    modifier = Modifier.size(32.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.graftedToday,
                                        fontFamily = Baloo2FontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        lineHeight = 26.sp,
                                        color = colors.onBrand,
                                    )
                                    Text(
                                        text = strings.graftedTodaySub,
                                        fontFamily = Baloo2FontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 15.sp,
                                        color = colors.onBrand.copy(alpha = 0.9f),
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.AddCircle,
                                    contentDescription = strings.graftedToday,
                                    tint = colors.onBrand,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                    }
                }

                // Tips audio banner
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tips_banner"),
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surfaceSecondary,
                        border = BorderStroke(1.dp, colors.border),
                        onClick = onOpenTips,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                tint = colors.onBrandSecondary,
                                modifier = Modifier.size(32.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.tipsBanner,
                                    fontFamily = Baloo2FontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = colors.onSurface,
                                )
                                Text(
                                    text = strings.tipsBannerSub,
                                    fontFamily = Baloo2FontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 15.sp,
                                    color = colors.muted,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = strings.listen,
                                tint = colors.onBrandSecondary,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }

                // Recent checks header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = strings.recent,
                            fontFamily = Baloo2FontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = colors.onSurface,
                        )
                        if (scans.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("see_all_history")
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onOpenHistory)
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = strings.seeAll,
                                    fontFamily = Baloo2FontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = colors.brandPrimary,
                                )
                            }
                        }
                    }
                }

                if (recentScans.isEmpty()) {
                    item {
                        DashedEmptyCard(
                            title = strings.noScans,
                            subtitle = strings.noScansSub,
                        )
                    }
                } else {
                    items(recentScans, key = { it.id }) { scan ->
                        ScanRow(
                            scan = scan,
                            lang = lang,
                            strings = strings,
                            onClick = { onOpenResultScan(scan.id) },
                            testTagName = "recent_scan_${scan.id}",
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }

        if (isPreparingPhoto) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .testTag("home_preparing_overlay"),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = colors.brandTertiary)
                    Text(
                        text = strings.preparing,
                        fontFamily = Baloo2FontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = colors.onSurfaceInverse,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeActionCard(
    item: HomeModeItem,
    strings: AppStrings,
    onTakePhoto: () -> Unit,
    onUpload: () -> Unit,
) {
    val colors = AiGraftTokens.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.brandSecondary,
        border = BorderStroke(2.dp, colors.brandTertiary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scan_${item.mode.wire}_button")
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onTakePhoto),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.brandTertiary.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = colors.onBrandSecondary,
                        modifier = Modifier.size(36.dp),
                    )
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontFamily = Baloo2FontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        color = colors.onBrandSecondary,
                    )
                    Text(
                        text = item.sub,
                        fontFamily = Baloo2FontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = colors.onSurfaceTertiary,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("camera_${item.mode.wire}_button"),
                    shape = RoundedCornerShape(999.dp),
                    color = colors.brandPrimary,
                    onClick = onTakePhoto,
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = null,
                            tint = colors.onBrandPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = strings.takePhoto,
                            fontFamily = Baloo2FontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = colors.onBrandPrimary,
                            maxLines = 1,
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("upload_${item.mode.wire}_button"),
                    shape = RoundedCornerShape(999.dp),
                    color = colors.surface,
                    border = BorderStroke(2.dp, colors.brandPrimary),
                    onClick = onUpload,
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudUpload,
                            contentDescription = null,
                            tint = colors.onBrandSecondary,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = strings.upload,
                            fontFamily = Baloo2FontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = colors.onBrandSecondary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashedEmptyCard(title: String, subtitle: String) {
    val colors = AiGraftTokens.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_empty"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                color = colors.borderStrong,
                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f),
                ),
            )
        }
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.PhotoLibrary,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = title,
                fontFamily = Baloo2FontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = colors.onSurface,
            )
            Text(
                text = subtitle,
                fontFamily = Baloo2FontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = colors.muted,
            )
        }
    }
}
