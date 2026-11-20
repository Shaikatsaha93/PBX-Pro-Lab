package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.example.update.UpdateDialog
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppThemeMode
import com.example.data.model.CallState
import com.example.service.BackgroundReliability
import com.example.service.SipForegroundService
import com.example.ui.screens.SoftphoneMainScreen
import com.example.ui.theme.SoftphoneTheme
import com.example.ui.viewmodel.SoftphoneViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SoftphoneViewModel by viewModels { SoftphoneViewModel.Factory }

    /** Conversation to open, when started from a chat notification. */
    private var openChatRequest by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        configureLockScreenFlags()
        openChatRequest = intent?.getStringExtra(EXTRA_OPEN_CHAT)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val callState by viewModel.callState.collectAsStateWithLifecycle()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            // Keep screen on during active or incoming call if configured
            LaunchedEffect(callState is CallState.Connected || callState is CallState.Incoming || callState is CallState.Outgoing) {
                val isActive = callState is CallState.Connected || callState is CallState.Incoming || callState is CallState.Outgoing
                if (isActive) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // Start background standby service if keep-alive is active
            LaunchedEffect(settings.backgroundKeepAlive) {
                if (settings.backgroundKeepAlive) {
                    SipForegroundService.startStandby(this@MainActivity)
                    askBatteryExemptionOnce()
                }
            }

            val isDarkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            // Status/navigation bar icons follow the app theme, not the system one
            LaunchedEffect(isDarkTheme) {
                val style = if (isDarkTheme) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            SoftphoneTheme(darkTheme = isDarkTheme) {
                // A newer version from GitHub releases (see AppUpdater)
                val updater = SoftphoneApp.instance.appUpdater
                val updateState by updater.state.collectAsStateWithLifecycle()
                val updateScope = rememberCoroutineScope()
                UpdateDialog(
                    state = updateState,
                    onUpdate = { update -> updateScope.launch { updater.downloadAndInstall(update) } },
                    onLater = updater::dismiss
                )

                SoftphoneMainScreen(
                    viewModel = viewModel,
                    openChatRequest = openChatRequest,
                    onChatOpened = { openChatRequest = null }
                )
            }
        }
    }

    /**
     * Without the battery-optimization exemption Doze cuts the network and ignores our wake
     * lock, so incoming calls are missed in power saving. Asked once; Settings shows the status.
     */
    private fun askBatteryExemptionOnce() {
        if (BackgroundReliability.isIgnoringBatteryOptimizations(this)) return
        val prefs = getSharedPreferences("background_reliability", MODE_PRIVATE)
        if (prefs.getBoolean("battery_exemption_asked", false)) return
        prefs.edit().putBoolean("battery_exemption_asked", true).apply()
        try {
            startActivity(BackgroundReliability.batteryOptimizationIntent(this))
        } catch (_: Exception) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_OPEN_CHAT)?.let { openChatRequest = it }
    }

    override fun onStart() {
        super.onStart()
        // Opening the app checks the SIP registration right away (e.g. after the network changed
        // while the phone was asleep) instead of waiting for the next refresh
        SoftphoneApp.instance.sipManager.onPushWakeup()
        // New version published on GitHub? (at most once an hour)
        lifecycleScope.launch { SoftphoneApp.instance.appUpdater.check() }
    }

    private fun configureLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    companion object {
        const val EXTRA_OPEN_CHAT = "open_chat"
    }
}
