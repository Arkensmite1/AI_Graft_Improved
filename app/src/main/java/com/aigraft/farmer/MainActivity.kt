package com.aigraft.farmer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.aigraft.farmer.data.ScanMode
import com.aigraft.farmer.i18n.stringsFor
import com.aigraft.farmer.ui.screens.GraftDetailScreen
import com.aigraft.farmer.ui.screens.GraftsScreen
import com.aigraft.farmer.ui.screens.HistoryScreen
import com.aigraft.farmer.ui.screens.HomeScreen
import com.aigraft.farmer.ui.screens.NewGraftScreen
import com.aigraft.farmer.ui.screens.ResultScreen
import com.aigraft.farmer.ui.screens.ScanScreen
import com.aigraft.farmer.ui.screens.TipsScreen
import com.aigraft.farmer.ui.theme.AiGraftTheme
import com.aigraft.farmer.ui.theme.AiGraftTokens
import com.aigraft.farmer.ui.theme.Baloo2FontFamily
import com.aigraft.farmer.viewmodel.AiGraftViewModel
import kotlinx.serialization.Serializable

@Serializable data object RouteHome
@Serializable data object RouteGrafts
@Serializable data object RouteHistory
@Serializable data object RouteTips
@Serializable data class RouteScan(val mode: String = "seedling", val batchId: String? = null)
@Serializable data class RouteResult(val scanId: String? = null)
@Serializable data class RouteNewGraft(val daysAgo: Int = 0, val scanId: String? = null)
@Serializable data class RouteGraftDetail(val batchId: String)

private data class TopTab(
    val route: Any,
    val label: String,
    val icon: ImageVector,
    val tag: String,
)

class MainActivity : ComponentActivity() {
    private val viewModel: AiGraftViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent BEFORE UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb(),
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.White.toArgb(),
                Color.White.toArgb(),
            ),
        )

        setContent {
            AiGraftTheme {
                AiGraftApp(viewModel = viewModel)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopSpeech()
    }
}

@Composable
private fun AiGraftApp(viewModel: AiGraftViewModel) {
    val navController = rememberNavController()
    val colors = AiGraftTokens.colors

    val lang by viewModel.lang.collectAsStateWithLifecycle()
    val scans by viewModel.scans.collectAsStateWithLifecycle()
    val batches by viewModel.batches.collectAsStateWithLifecycle()
    val pendingScan by viewModel.pendingScan.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val isPreparingPhoto by viewModel.isPreparingPhoto.collectAsStateWithLifecycle()
    val speechState by viewModel.speechState.collectAsStateWithLifecycle()

    val strings = stringsFor(lang)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    val tabs = listOf(
        TopTab(RouteHome, strings.tabHome, Icons.Rounded.Home, "tab_home"),
        TopTab(RouteGrafts, strings.tabGrafts, Icons.Rounded.CalendarMonth, "tab_grafts"),
        TopTab(RouteHistory, strings.tabHistory, Icons.Rounded.History, "tab_history"),
        TopTab(RouteTips, strings.tabTips, Icons.Rounded.Lightbulb, "tab_tips"),
    )

    val showBottomBar = currentDest != null && (
        currentDest.hasRoute<RouteHome>() ||
            currentDest.hasRoute<RouteGrafts>() ||
            currentDest.hasRoute<RouteHistory>() ||
            currentDest.hasRoute<RouteTips>()
        )

    fun switchTab(route: Any) {
        viewModel.stopSpeech()
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = colors.surface,
        bottomBar = {
            if (showBottomBar) {
                Box {
                    NavigationBar(
                        containerColor = colors.surface,
                        tonalElevation = 0.dp,
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentDest?.hasRoute(tab.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { switchTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(28.dp),
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        fontFamily = Baloo2FontFamily,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.brandPrimary,
                                    selectedTextColor = colors.brandPrimary,
                                    indicatorColor = colors.brandSecondary,
                                    unselectedIconColor = colors.muted,
                                    unselectedTextColor = colors.muted,
                                ),
                                modifier = Modifier.testTag(tab.tag),
                            )
                        }
                    }
                    HorizontalDivider(color = colors.border, thickness = 1.dp)
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = RouteHome,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp),
        ) {
            composable<RouteHome> {
                HomeScreen(
                    lang = lang,
                    strings = strings,
                    scans = scans,
                    batches = batches,
                    isPreparingPhoto = isPreparingPhoto,
                    onSelectLang = viewModel::setLang,
                    onOpenScan = { mode -> navController.navigate(RouteScan(mode = mode.wire)) },
                    onPickGalleryPhoto = { uri, mode -> viewModel.preparePhotoFromUri(uri, mode, null) },
                    onOpenResultNew = { navController.navigate(RouteResult(scanId = null)) },
                    onOpenResultScan = { id -> navController.navigate(RouteResult(scanId = id)) },
                    onOpenNewGraft = { navController.navigate(RouteNewGraft()) },
                    onOpenGraftDetail = { id -> navController.navigate(RouteGraftDetail(batchId = id)) },
                    onToggleBatchDayDone = viewModel::toggleBatchDayDone,
                    onOpenTips = { switchTab(RouteTips) },
                    onOpenHistory = { switchTab(RouteHistory) },
                )
            }

            composable<RouteGrafts> {
                GraftsScreen(
                    lang = lang,
                    strings = strings,
                    batches = batches,
                    onNewGraft = { navController.navigate(RouteNewGraft()) },
                    onOpenGraftDetail = { id -> navController.navigate(RouteGraftDetail(batchId = id)) },
                    onToggleBatchDayDone = viewModel::toggleBatchDayDone,
                )
            }

            composable<RouteHistory> {
                HistoryScreen(
                    lang = lang,
                    strings = strings,
                    scans = scans,
                    onOpenResult = { id -> navController.navigate(RouteResult(scanId = id)) },
                    onClearAll = viewModel::clearAllScans,
                    onScanNow = { mode -> navController.navigate(RouteScan(mode = mode.wire)) },
                )
            }

            composable<RouteTips> {
                TipsScreen(
                    lang = lang,
                    strings = strings,
                    speechState = speechState,
                    onSpeak = { id, text -> viewModel.speak(id, text) },
                    onStopSpeech = viewModel::stopSpeech,
                )
            }

            composable<RouteScan> { entry ->
                val args = entry.toRoute<RouteScan>()
                ScanScreen(
                    mode = ScanMode.fromWire(args.mode),
                    batchId = args.batchId,
                    strings = strings,
                    isPreparingPhoto = isPreparingPhoto,
                    onPrepareFile = viewModel::preparePhotoFromFile,
                    onPrepareUri = viewModel::preparePhotoFromUri,
                    onPhotoReady = {
                        navController.navigate(RouteResult(scanId = null)) {
                            popUpTo<RouteScan> { inclusive = true }
                        }
                    },
                    onClose = { navController.popBackStack() },
                )
            }

            composable<RouteResult> { entry ->
                val args = entry.toRoute<RouteResult>()
                ResultScreen(
                    scanId = args.scanId,
                    strings = strings,
                    pendingScan = pendingScan,
                    analysisState = analysisState,
                    speechState = speechState,
                    onLoadExisting = viewModel::loadExistingScan,
                    onAnalyzePending = viewModel::analyzePendingScan,
                    onSpeak = { id, text -> viewModel.speak(id, text) },
                    onStopSpeech = viewModel::stopSpeech,
                    onNewPhoto = { mode, batchId ->
                        navController.navigate(RouteScan(mode = mode.wire, batchId = batchId)) {
                            popUpTo<RouteResult> { inclusive = true }
                        }
                    },
                    onTrackGraft = { scanId, days ->
                        navController.navigate(RouteNewGraft(daysAgo = days, scanId = scanId))
                    },
                    onDone = {
                        viewModel.stopSpeech()
                        navController.popBackStack(RouteHome, inclusive = false)
                    },
                )
            }

            composable<RouteNewGraft> { entry ->
                val args = entry.toRoute<RouteNewGraft>()
                NewGraftScreen(
                    initialDaysAgo = args.daysAgo,
                    linkedScanId = args.scanId,
                    existingBatchCount = batches.size,
                    strings = strings,
                    onSave = { name, plants, daysAgo, remindersEnabled, linkedId ->
                        val created = viewModel.createBatch(
                            name = name,
                            plants = plants,
                            daysAgo = daysAgo,
                            remindersEnabled = remindersEnabled,
                            linkedScanId = linkedId,
                        )
                        navController.navigate(RouteGraftDetail(batchId = created.id)) {
                            popUpTo<RouteNewGraft> { inclusive = true }
                        }
                    },
                    onClose = { navController.popBackStack() },
                )
            }

            composable<RouteGraftDetail> { entry ->
                val args = entry.toRoute<RouteGraftDetail>()
                val batch = batches.firstOrNull { it.id == args.batchId }
                val attachedScans = scans.filter { it.batchId == args.batchId }
                GraftDetailScreen(
                    batch = batch,
                    attachedScans = attachedScans,
                    lang = lang,
                    strings = strings,
                    speechState = speechState,
                    onSpeak = { id, text -> viewModel.speak(id, text) },
                    onStopSpeech = viewModel::stopSpeech,
                    onToggleDayDone = { day -> viewModel.toggleBatchDayDone(args.batchId, day) },
                    onEnableReminders = { viewModel.enableReminders(args.batchId) },
                    onTakePhoto = { bId ->
                        navController.navigate(RouteScan(mode = ScanMode.GRAFT.wire, batchId = bId))
                    },
                    onOpenScanResult = { scanId ->
                        navController.navigate(RouteResult(scanId = scanId))
                    },
                    onDeleteBatch = {
                        viewModel.deleteBatch(args.batchId)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
