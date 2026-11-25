package com.aigraft.farmer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigraft.farmer.data.ScanMode
import com.aigraft.farmer.data.ScanRecord
import com.aigraft.farmer.i18n.AppLang
import com.aigraft.farmer.i18n.AppStrings
import com.aigraft.farmer.ui.components.ScanRow
import com.aigraft.farmer.ui.theme.AiGraftTokens
import com.aigraft.farmer.ui.theme.Baloo2FontFamily

@Composable
fun HistoryScreen(
    lang: AppLang,
    strings: AppStrings,
    scans: List<ScanRecord>,
    onOpenResult: (String) -> Unit,
    onClearAll: () -> Unit,
    onScanNow: (ScanMode) -> Unit,
) {
    val colors = AiGraftTokens.colors
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = strings.history,
                fontFamily = Baloo2FontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                color = colors.onSurface,
            )

            if (scans.isNotEmpty()) {
                if (confirmDelete) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = strings.sureDelete,
                            fontFamily = Baloo2FontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = colors.onSurface,
                        )
                        Surface(
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("confirm_delete_yes"),
                            shape = RoundedCornerShape(999.dp),
                            color = colors.error,
                            onClick = {
                                onClearAll()
                                confirmDelete = false
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
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
                                .testTag("confirm_delete_no"),
                            shape = RoundedCornerShape(999.dp),
                            color = colors.surfaceSecondary,
                            onClick = { confirmDelete = false },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
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
                    Surface(
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("delete_all_button"),
                        shape = RoundedCornerShape(999.dp),
                        color = colors.surfaceSecondary,
                        onClick = { confirmDelete = true },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = colors.error,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = strings.deleteAll,
                                fontFamily = Baloo2FontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = colors.error,
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = colors.divider, thickness = 1.dp)

        if (scans.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .testTag("history_empty"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Eco,
                    contentDescription = null,
                    tint = colors.brandPrimary,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.noScans,
                    fontFamily = Baloo2FontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = colors.onSurface,
                )
                Text(
                    text = strings.noScansSub,
                    fontFamily = Baloo2FontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = colors.muted,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    modifier = Modifier
                        .height(60.dp)
                        .testTag("history_scan_now"),
                    shape = RoundedCornerShape(999.dp),
                    color = colors.brandPrimary,
                    onClick = { onScanNow(ScanMode.SEEDLING) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = null,
                            tint = colors.onBrandPrimary,
                            modifier = Modifier.size(26.dp),
                        )
                        Text(
                            text = strings.newPhoto,
                            fontFamily = Baloo2FontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = colors.onBrandPrimary,
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("history_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(scans, key = { it.id }) { scan ->
                    ScanRow(
                        scan = scan,
                        lang = lang,
                        strings = strings,
                        onClick = { onOpenResult(scan.id) },
                        testTagName = "history_scan_${scan.id}",
                    )
                }
            }
        }
    }
}
