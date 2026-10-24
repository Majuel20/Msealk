package com.msealk.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.ui.platform.testTag
import androidx.lifecycle.repeatOnLifecycle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.msealk.R
import com.msealk.data.AppSealPrefs
import com.msealk.data.AppSealRepository
import com.msealk.data.AppSettings
import com.msealk.data.Corte
import com.msealk.notifications.AppSealNotifications
import com.msealk.scheduler.AppSealScheduler
import com.msealk.util.TextMatch
import com.msealk.util.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime


internal const val SOCIAL_URL = "https://majuel20.s.gy/majuel-link"

private enum class MainTab(val title: String, val iconRes: Int, val showInBottom: Boolean = true) {
    HOME("Inicio", R.drawable.ic_ui_home),
    ZONES("Zonas", R.drawable.ic_ui_zones),
    HISTORY("Cortes", R.drawable.ic_ui_cuts),
    SETTINGS("Ajustes", R.drawable.ic_ui_settings),
    NOTIFICATIONS("Alertas", R.drawable.ic_ui_bell, false),
    DIAGNOSTIC("Diagnóstico", R.drawable.ic_ui_settings, false)
}

internal enum class HomeFilter(val title: String) {
    MATCHES("Mis zonas"),
    TODAY("Hoy"),
    WEEK("7 días"),
    ALL("Todos")
}

internal enum class HistoryView(val title: String) {
    UPCOMING("Próximos"),
    ALL("Todos"),
    EVENTS("Historial")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSealScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val screenState = rememberSaveableStateHolder()

    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var settings by remember { mutableStateOf(AppSealPrefs.getSettings(context)) }
    var zones by remember { mutableStateOf(AppSealPrefs.getZones(context)) }
    var cortes by remember { mutableStateOf(AppSealRepository.loadLocal(context)) }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Listo") }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var permissionOk by remember { mutableStateOf(AppSealNotifications.canNotify(context)) }
    var historyVersion by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionOk = AppSealNotifications.canNotify(context)
                now = LocalDateTime.now()
                scope.launch {
                    val cached = withContext(Dispatchers.IO) { AppSealRepository.loadLocal(context) }
                    if (!loading) {
                        cortes = cached
                        settings = AppSealPrefs.getSettings(context)
                        zones = AppSealPrefs.getZones(context)
                        historyVersion++
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = LocalDateTime.now()
                delay(60_000)
            }
        }
    }
    BackHandler(enabled = tab != MainTab.HOME) { tab = MainTab.HOME }

    val visibleCortes by remember(cortes, settings, now) {
        derivedStateOf { cortes.filterNot { it.shouldHideInUi(settings, now) } }
    }
    val hiddenOldCount by remember(cortes, settings, now) {
        derivedStateOf { cortes.size - visibleCortes.size }
    }
    val matches by remember(visibleCortes, zones) {
        derivedStateOf { TextMatch.findMatches(visibleCortes, zones) }
    }
    val activeZonesCount by remember(zones) {
        derivedStateOf { zones.count { it.enabled && it.cleanKeywords().isNotEmpty() } }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionOk = AppSealNotifications.canNotify(context)
        status = if (granted) "Permiso de notificaciones concedido" else "Permiso denegado. Revísalo desde 🔔 Alertas"
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !AppSealNotifications.canNotify(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            AppSealNotifications.openNotificationSettings(context)
        }
    }

    fun refreshNow() {
        if (loading) return
        loading = true
        status = "Consultando cortes programados…"
        scope.launch {
            try {
                val remote = withContext(Dispatchers.IO) { AppSealRepository.updateFromRemote(context) }
                withContext(Dispatchers.IO) {
                    AppSealScheduler.processAfterDataUpdate(
                        context = context,
                        cortes = remote,
                        settings = AppSealPrefs.getSettings(context),
                        manual = true
                    )
                    AppSealPrefs.addHistory(context, "Actualización manual correcta. Cortes descargados: ${remote.size}.", "sync")
                }
                cortes = remote
                settings = AppSealPrefs.getSettings(context)
                zones = AppSealPrefs.getZones(context)
                permissionOk = AppSealNotifications.canNotify(context)
                historyVersion++
                status = "Actualizado ${AppSealPrefs.lastSyncText(context)}"
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                val msg = e.message ?: "error desconocido"
                AppSealPrefs.setLastSyncError(context, msg)
                AppSealPrefs.addHistory(context, "Error en actualización manual: $msg", "error")
                historyVersion++
                status = "Error: $msg"
            } finally {
                loading = false
            }
        }
    }

    fun pruneOldCuts() {
        scope.launch(Dispatchers.IO) {
            val removed = AppSealRepository.pruneLocal(context, settings.keepPastDays)
            val updated = AppSealRepository.loadLocal(context)
            AppSealPrefs.addHistory(context, "Limpieza manual: $removed cortes antiguos eliminados.", "cleanup")
            withContext(Dispatchers.Main) {
                cortes = updated
                historyVersion++
                status = if (removed > 0) "Se eliminaron $removed cortes antiguos" else "No había cortes antiguos para eliminar"
            }
        }
    }

    LaunchedEffect(Unit) {
        AppSealNotifications.createChannel(context)
        if (settings.autoSyncEnabled) AppSealScheduler.scheduleAutomaticSync(context, settings)
        permissionOk = AppSealNotifications.canNotify(context)
    }

    Scaffold(
        containerColor = NeonColor.Dark,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        NeonTitle("MSealk")
                        Text(
                            text = "by Majuel20",
                            color = NeonColor.Cyan,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    if (tab == MainTab.NOTIFICATIONS || tab == MainTab.DIAGNOSTIC) {
                        TopAction(R.drawable.ic_ui_back, "Volver a Inicio") { tab = MainTab.HOME }
                    } else LogoMark(Modifier.padding(start = 16.dp), compact = true)
                },
                actions = {
                    TopAction(R.drawable.ic_ui_share, "Compartir app") { ShareUtils.shareApp(context) }
                    TopAction(R.drawable.ic_ui_bell, "Abrir alertas") { tab = MainTab.NOTIFICATIONS }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = NeonColor.Dark,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = NeonColor.Dark2.copy(alpha = 0.96f)) {
                MainTab.values().filter { it.showInBottom }.forEach { item ->
                    NavigationBarItem(
                        modifier = Modifier.testTag("nav_${item.name}"),
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            Icon(painterResource(item.iconRes), contentDescription = null, modifier = Modifier.size(24.dp))
                        },
                        label = { Text(item.title, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonColor.Cyan,
                            selectedTextColor = Color.White,
                            unselectedIconColor = NeonColor.Muted,
                            unselectedTextColor = NeonColor.Muted,
                            indicatorColor = NeonColor.Cyan.copy(alpha = 0.16f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppSealBackground)
                .padding(padding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(Modifier.widthIn(max = 760.dp).fillMaxSize()) {
                screenState.SaveableStateProvider(tab.name) {
                    when (tab) {
                        MainTab.HOME -> HomeScreen(
                            settings = settings,
                            cortes = visibleCortes,
                            matches = matches,
                            zones = zones,
                            activeZonesCount = activeZonesCount,
                            hiddenOldCount = hiddenOldCount,
                            status = status,
                            loading = loading,
                            permissionOk = permissionOk,
                            now = now,
                            onRefresh = { refreshNow() },
                            onOpenZones = { tab = MainTab.ZONES },
                            onOpenDiagnostic = { tab = MainTab.DIAGNOSTIC },
                            onOpenNotifications = { tab = MainTab.NOTIFICATIONS }
                        )
                        MainTab.ZONES -> ZonesScreen(
                            zones = zones,
                            onZonesChanged = { newZones ->
                                zones = newZones
                                AppSealPrefs.saveZones(context, newZones)
                                AppSealScheduler.processAfterDataUpdate(
                                    context = context,
                                    cortes = cortes,
                                    settings = AppSealPrefs.getSettings(context),
                                    manual = true
                                )
                                historyVersion++
                                status = "Zonas guardadas · ${TextMatch.findMatches(visibleCortes, newZones).size} coincidencias"
                            }
                        )
                        MainTab.HISTORY -> HistoryScreen(
                            cortes = visibleCortes,
                            matches = matches,
                            history = AppSealPrefs.getHistory(context),
                            version = historyVersion,
                            hiddenOldCount = hiddenOldCount,
                            onClear = {
                                AppSealPrefs.clearHistory(context)
                                historyVersion++
                            }
                        )
                        MainTab.SETTINGS -> SettingsScreen(
                            settings = settings,
                            cortes = visibleCortes,
                            zones = zones,
                            matchesCount = matches.size,
                            hiddenOldCount = hiddenOldCount,
                            onSettingsChanged = { newSettings ->
                                val normalized = newSettings.normalized()
                                settings = normalized
                                AppSealScheduler.applyConfiguration(context, normalized, cortes)
                                permissionOk = AppSealNotifications.canNotify(context)
                                historyVersion++
                                status = if (normalized.alertsEnabled && !permissionOk) "Ajustes guardados · activa permisos desde 🔔" else "Ajustes guardados"
                            },
                            onPruneOldCuts = { pruneOldCuts() },
                            onOpenNotifications = { tab = MainTab.NOTIFICATIONS },
                            onOpenDiagnostic = { tab = MainTab.DIAGNOSTIC }
                        )
                        MainTab.NOTIFICATIONS -> NotificationsScreen(
                            settings = settings,
                            permissionOk = permissionOk,
                            zonesCount = activeZonesCount,
                            matchesCount = matches.size,
                            onRequestPermission = { requestNotificationPermission() },
                            onPermissionStateChanged = { permissionOk = AppSealNotifications.canNotify(context) }
                        )
                        MainTab.DIAGNOSTIC -> DiagnosticScreen(
                            settings = settings,
                            cortes = visibleCortes,
                            zones = zones,
                            matchesCount = matches.size,
                            hiddenOldCount = hiddenOldCount,
                            status = status,
                            onRequestPermission = { requestNotificationPermission() },
                            onOpenNotifications = { tab = MainTab.NOTIFICATIONS },
                            onPruneOldCuts = { pruneOldCuts() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopAction(
    iconRes: Int,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(painterResource(iconRes), contentDescription, tint = NeonColor.Muted, modifier = Modifier.size(24.dp))
    }
}

private fun Corte.shouldHideInUi(settings: AppSettings, now: LocalDateTime): Boolean {
    val end = finDateTime() ?: inicioDateTime() ?: return false
    if (!end.isBefore(now)) return false
    if (!settings.showPastGhosts) return true
    val limit = now.toLocalDate().minusDays(settings.keepPastDays.coerceIn(0, 30).toLong())
    return end.toLocalDate().isBefore(limit)
}
