package com.example.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.AppGraph
import com.example.data.model.AppThemeMode
import com.example.data.model.CallState
import com.example.data.model.CallType
import com.example.data.repository.CallLogRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SipAccountRepository
import com.example.desktop.sip.DesktopSipManager
import com.example.desktop.update.DesktopUpdateDialog
import com.example.desktop.update.DesktopUpdater
import com.example.platform.formatMediumDate
import com.example.sip.CallEvent
import com.example.ui.platform.DesktopToast
import com.example.ui.screens.SoftphoneMainScreen
import com.example.ui.theme.SoftphoneTheme
import com.example.ui.viewmodel.SoftphoneViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** packageVersion from desktopApp/build.gradle.kts, passed in by the installer's launcher */
private val DESKTOP_VERSION: String = System.getProperty("dialer.version") ?: "1.0.0"

/** How often the app, usually left running in the tray, looks for a newer release */
private const val UPDATE_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L

/** Events the window reacts to (tray notifications, bringing the window up for a call). */
private object DesktopEvents {
    var notification by mutableStateOf<Notification?>(null)
}

fun main() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Services (the Windows counterpart of SoftphoneApp.onCreate on Android)
    val database = openDesktopDatabase()
    val accountRepository = SipAccountRepository(database.sipAccountDao())
    val callLogRepository = CallLogRepository(database.callLogDao())
    val settingsRepository = SettingsRepository(PreferencesStore("settings"))
    val sipManager = DesktopSipManager()
    val contacts = NoContacts()
    val chat = ChatRepository(database.chatDao(), sipManager, contacts, { _, title, text ->
        DesktopEvents.notification = Notification(title, text, Notification.Type.Info)
    }, appScope)

    AppGraph.sipManager = sipManager
    AppGraph.accountRepository = accountRepository
    AppGraph.callLogRepository = callLogRepository
    AppGraph.settingsRepository = settingsRepository
    AppGraph.contacts = contacts
    AppGraph.push = NoPush()
    AppGraph.chatRepository = chat
    AppGraph.appVersion = DESKTOP_VERSION

    val updater = DesktopUpdater(DESKTOP_VERSION)
    AppGraph.checkForUpdates = {
        appScope.launch {
            val update = updater.check()
            DesktopToast.show(
                when {
                    update != null -> "PBX Pro Lab ${update.version} is available"
                    updater.lastError != null -> "Could not check for updates (${updater.lastError})"
                    else -> "PBX Pro Lab is up to date ($DESKTOP_VERSION)"
                }
            )
        }
    }
    appScope.launch {
        delay(10_000)
        var announced = ""
        while (true) {
            val update = updater.check()
            if (update != null && update.version != announced) {
                announced = update.version
                DesktopEvents.notification =
                    Notification("PBX Pro Lab update available", "Version ${update.version} is ready to install", Notification.Type.Info)
            }
            delay(UPDATE_CHECK_INTERVAL_MS)
        }
    }

    sipManager.applySettings(settingsRepository.settings.value)
    sipManager.initializeSdk()
    chat.start()

    appScope.launch {
        accountRepository.activeAccount.distinctUntilChanged().collectLatest { account ->
            if (account != null) sipManager.registerAccount(account) else sipManager.unregisterCurrentAccount()
        }
    }
    appScope.launch {
        sipManager.callEvents.collect { event ->
            if (event is CallEvent.CallEnded && settingsRepository.settings.value.recordCallHistory) {
                callLogRepository.addLog(
                    remoteUri = event.remoteUri,
                    displayName = event.displayName.takeIf { it.isNotBlank() && !it.startsWith("sip:") } ?: "",
                    callType = when {
                        event.wasMissed -> CallType.MISSED
                        event.isIncoming -> CallType.INCOMING
                        else -> CallType.OUTGOING
                    },
                    durationSeconds = event.durationSeconds
                )
            }
        }
    }

    application(exitProcessOnExit = true) {
        var windowVisible by remember { mutableStateOf(true) }
        val trayState = rememberTrayState()
        val windowState = rememberWindowState(size = DpSize(1100.dp, 760.dp), position = WindowPosition(Alignment.Center))
        val callState by sipManager.callState.collectAsState()
        val appIcon = remember { BitmapPainter(useResource("dialer_icon.png", ::loadImageBitmap)) }

        fun quit() {
            sipManager.shutdown()
            exitApplication()
        }

        // Closing the window keeps the app in the tray, so incoming calls still ring
        Tray(
            icon = appIcon,
            state = trayState,
            tooltip = "PBX Pro Lab",
            onAction = { windowVisible = true },
            menu = {
                Item("Open Dialer", onClick = { windowVisible = true })
                Item("Quit", onClick = { quit() })
            }
        )

        LaunchedEffect(DesktopEvents.notification) {
            DesktopEvents.notification?.let {
                trayState.sendNotification(it)
                DesktopEvents.notification = null
            }
        }

        // An incoming call brings the window to the front
        LaunchedEffect(callState is CallState.Incoming) {
            if (callState is CallState.Incoming) {
                windowVisible = true
                windowState.isMinimized = false
                val incoming = callState as CallState.Incoming
                trayState.sendNotification(
                    Notification("Incoming call", incoming.displayName.ifBlank { incoming.remoteUri }, Notification.Type.Info)
                )
            }
        }

        Window(
            onCloseRequest = { windowVisible = false },
            visible = windowVisible,
            state = windowState,
            title = "PBX Pro Lab",
            icon = appIcon,
            alwaysOnTop = callState is CallState.Incoming
        ) {
            DesktopApp(updater, onInstallerStarted = { quit() })
        }
    }
}

@Composable
private fun DesktopApp(updater: DesktopUpdater, onInstallerStarted: () -> Unit) {
    val viewModel: SoftphoneViewModel = viewModel(factory = SoftphoneViewModel.Factory)
    val themeMode by viewModel.themeMode.collectAsState()
    val dark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    SoftphoneTheme(darkTheme = dark) {
        Box(Modifier.fillMaxSize()) {
            SoftphoneMainScreen(viewModel = viewModel)
            DesktopToastHost(Modifier.align(Alignment.BottomCenter))
        }

        // Not in the middle of a call: the installer closes Dialer
        val updateState by updater.state.collectAsState()
        val callState by AppGraph.sipManager.callState.collectAsState()
        val scope = rememberCoroutineScope()
        if (callState is CallState.Idle) {
            DesktopUpdateDialog(
                state = updateState,
                onUpdate = { update -> scope.launch { if (updater.downloadAndInstall(update)) onInstallerStarted() } },
                onLater = updater::dismiss
            )
        }
    }
}

/** Shows [DesktopToast] messages for a few seconds, like a Toast on Android. */
@Composable
private fun DesktopToastHost(modifier: Modifier = Modifier) {
    val message = DesktopToast.message
    LaunchedEffect(message) {
        if (message != null) {
            delay(3500)
            DesktopToast.dismiss()
        }
    }
    AnimatedVisibility(visible = message != null, modifier = modifier.padding(24.dp)) {
        Snackbar { Text(message.orEmpty()) }
    }
}
