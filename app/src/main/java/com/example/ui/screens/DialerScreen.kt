package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AccountBalance
import com.example.data.model.AppThemeMode
import com.example.data.model.CallState
import com.example.data.model.RegistrationStatus
import com.example.data.model.SipAccount
import com.example.ui.components.DtmfKeypad
import com.example.ui.theme.BrandGradient
import com.example.ui.theme.BrandGradientEnd
import com.example.ui.theme.CallActionAmber
import com.example.ui.theme.CallActionGreen
import com.example.ui.theme.CallActionRed
import com.example.ui.theme.LocalGlassColors
import com.example.ui.theme.glass
import com.example.ui.viewmodel.SoftphoneViewModel

@Composable
fun DialerScreen(
    viewModel: SoftphoneViewModel,
    onNavigateToActiveCall: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dialerInput by viewModel.dialerInput.collectAsStateWithLifecycle()
    val activeAccount by viewModel.activeAccount.collectAsStateWithLifecycle()
    val registrationState by viewModel.registrationState.collectAsStateWithLifecycle()
    val accountBalance by viewModel.accountBalance.collectAsStateWithLifecycle()
    val callState by viewModel.callState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val lastDialedNumber by viewModel.lastDialedNumber.collectAsStateWithLifecycle()

    val isCallActive = callState !is CallState.Idle && callState !is CallState.Disconnected
    val matchedContactName = if (dialerInput.isNotBlank()) viewModel.getContactNameForUri(dialerInput) else null

    val infiniteTransition = rememberInfiniteTransition(label = "PulseAnimation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Call button: no account -> open setup; number typed -> call; empty -> fill in the last number
    val onCallPressed: () -> Unit = {
        when {
            activeAccount == null -> onNavigateToAccounts()
            dialerInput.isNotBlank() -> {
                viewModel.initiateCall()
                onNavigateToActiveCall()
            }
            lastDialedNumber != null -> viewModel.setDialerInput(lastDialedNumber!!)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 600.dp
        val isCompactHeight = maxHeight < 680.dp

        if (isWideLayout) {
            // Foldables & Tablets: status and number on the left, keypad on the right
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DialerTopBar(
                        themeMode = themeMode,
                        onThemeSelect = { viewModel.setThemeMode(it) }
                    )
                    AccountStatusBar(
                        activeAccount = activeAccount,
                        registrationState = registrationState,
                        accountBalance = accountBalance,
                        pulseScale = pulseScale,
                        onClick = onNavigateToAccounts
                    )
                    OngoingCallBanner(isCallActive = isCallActive, onClick = onNavigateToActiveCall)
                    NumberDisplayField(
                        dialerInput = dialerInput,
                        matchedContactName = matchedContactName,
                        lastDialedNumber = lastDialedNumber,
                        hasAccount = activeAccount != null,
                        isCompactHeight = false
                    )
                    CallActionsRow(
                        dialerInput = dialerInput,
                        hasAccount = activeAccount != null,
                        canRedial = lastDialedNumber != null,
                        isCompactHeight = false,
                        onCall = onCallPressed,
                        onBackspace = { viewModel.onBackspace() },
                        onClearAll = { viewModel.setDialerInput("") }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    ) {
                        Box(
                            modifier = Modifier.padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DtmfKeypad(
                                onKeyPressed = { char -> viewModel.onDialerChar(char) },
                                hapticFeedbackEnabled = settings.dtmfHapticFeedback,
                                keySize = 76.dp,
                                spacing = 14.dp
                            )
                        }
                    }
                }
            }
        } else {
            // Phones and foldable cover screens
            val keyHeight = if (isCompactHeight) 56.dp else 66.dp
            val keySpacing = if (isCompactHeight) 8.dp else 12.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = if (isCompactHeight) 6.dp else 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DialerTopBar(
                    themeMode = themeMode,
                    onThemeSelect = { viewModel.setThemeMode(it) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                AccountStatusBar(
                    activeAccount = activeAccount,
                    registrationState = registrationState,
                    accountBalance = accountBalance,
                    pulseScale = pulseScale,
                    onClick = onNavigateToAccounts
                )

                OngoingCallBanner(isCallActive = isCallActive, onClick = onNavigateToActiveCall)

                Spacer(modifier = Modifier.weight(1f))

                NumberDisplayField(
                    dialerInput = dialerInput,
                    matchedContactName = matchedContactName,
                    lastDialedNumber = lastDialedNumber,
                    hasAccount = activeAccount != null,
                    isCompactHeight = isCompactHeight
                )

                Spacer(modifier = Modifier.weight(1f))

                DtmfKeypad(
                    onKeyPressed = { char -> viewModel.onDialerChar(char) },
                    hapticFeedbackEnabled = settings.dtmfHapticFeedback,
                    keySize = keyHeight,
                    spacing = keySpacing
                )

                Spacer(modifier = Modifier.height(if (isCompactHeight) 12.dp else 18.dp))

                CallActionsRow(
                    dialerInput = dialerInput,
                    hasAccount = activeAccount != null,
                    canRedial = lastDialedNumber != null,
                    isCompactHeight = isCompactHeight,
                    onCall = onCallPressed,
                    onBackspace = { viewModel.onBackspace() },
                    onClearAll = { viewModel.setDialerInput("") }
                )

                Spacer(modifier = Modifier.height(if (isCompactHeight) 6.dp else 10.dp))
            }
        }
    }
}

@Composable
private fun DialerTopBar(
    themeMode: AppThemeMode,
    onThemeSelect: (AppThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Gradient monogram tile
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(BrandGradient),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PBX",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.semantics { contentDescription = "PBX Pro Lab logo" }
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "PBX Pro Lab",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "SIP SOFTPHONE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        ThemeCycleButton(themeMode = themeMode, onThemeSelect = onThemeSelect)
    }
}

/** One compact button that cycles System -> Light -> Dark; the full picker lives in Settings. */
@Composable
private fun ThemeCycleButton(
    themeMode: AppThemeMode,
    onThemeSelect: (AppThemeMode) -> Unit
) {
    val (icon, next) = when (themeMode) {
        AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto to AppThemeMode.LIGHT
        AppThemeMode.LIGHT -> Icons.Default.LightMode to AppThemeMode.DARK
        AppThemeMode.DARK -> Icons.Default.DarkMode to AppThemeMode.SYSTEM
    }
    Surface(
        onClick = { onThemeSelect(next) },
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        modifier = Modifier
            .size(44.dp)
            .glass(RoundedCornerShape(14.dp), LocalGlassColors.current)
            .testTag("theme_cycle_button")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = "Theme: ${themeMode.label}. Tap to change",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun AccountStatusBar(
    activeAccount: SipAccount?,
    registrationState: RegistrationStatus,
    accountBalance: AccountBalance?,
    pulseScale: Float,
    onClick: () -> Unit
) {
    val statusDotColor = when {
        activeAccount == null -> CallActionAmber
        registrationState == RegistrationStatus.REGISTERED -> CallActionGreen
        registrationState == RegistrationStatus.REGISTERING -> CallActionAmber
        registrationState == RegistrationStatus.FAILED -> CallActionRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .glass(RoundedCornerShape(20.dp), LocalGlassColors.current)
            .clickable(onClick = onClick)
            .testTag("dialer_account_status_bar"),
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .scale(if (registrationState == RegistrationStatus.REGISTERING) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(statusDotColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (activeAccount != null) {
                        // Just the number; the server address is not useful to the agent
                        activeAccount.username.trim().removePrefix("sip:").substringBefore("@")
                    } else {
                        "No SIP account yet"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        activeAccount == null -> "Tap here to add your account"
                        registrationState == RegistrationStatus.REGISTERED -> "Online • Ready to call"
                        registrationState == RegistrationStatus.REGISTERING -> "Connecting…"
                        registrationState == RegistrationStatus.FAILED -> "Can't connect • Tap to check settings"
                        else -> "Offline • Tap to reconnect"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = statusDotColor
                )
            }

            if (accountBalance != null && registrationState == RegistrationStatus.REGISTERED) {
                val low = accountBalance.amount < 5.0
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("dialer_balance")
                ) {
                    Text(
                        text = "Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = accountBalance.formatted,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (low) CallActionRed else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Icon(
                imageVector = if (activeAccount == null) Icons.Default.ChevronRight else Icons.Default.Settings,
                contentDescription = "Account settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun OngoingCallBanner(
    isCallActive: Boolean,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = isCallActive,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clickable(onClick = onClick)
                .testTag("ongoing_call_banner"),
            colors = CardDefaults.cardColors(containerColor = CallActionGreen.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CallActionGreen.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneInTalk,
                    contentDescription = null,
                    tint = CallActionGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Call in progress • Tap to return",
                    style = MaterialTheme.typography.labelLarge,
                    color = CallActionGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NumberDisplayField(
    dialerInput: String,
    matchedContactName: String?,
    lastDialedNumber: String?,
    hasAccount: Boolean,
    isCompactHeight: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isCompactHeight) 64.dp else 84.dp)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (dialerInput.isEmpty()) {
            Text(
                text = "Enter a number",
                fontSize = if (isCompactHeight) 24.sp else 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.testTag("dialer_display_text")
            )
            Text(
                text = when {
                    !hasAccount -> "Add a SIP account to start calling"
                    lastDialedNumber != null -> "Tap the call button to redial $lastDialedNumber"
                    else -> "Hold 0 to type +"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            Text(
                text = dialerInput,
                fontSize = when {
                    dialerInput.length > 16 -> 24.sp
                    dialerInput.length > 11 -> 30.sp
                    else -> if (isCompactHeight) 32.sp else 38.sp
                },
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("dialer_display_text")
            )
            if (!matchedContactName.isNullOrBlank()) {
                Text(
                    text = matchedContactName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CallActionsRow(
    dialerInput: String,
    hasAccount: Boolean,
    canRedial: Boolean,
    isCompactHeight: Boolean,
    onCall: () -> Unit,
    onBackspace: () -> Unit,
    onClearAll: () -> Unit
) {
    val sideSize = if (isCompactHeight) 52.dp else 58.dp
    val hasInput = dialerInput.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Keeps the call button centred
        Spacer(modifier = Modifier.size(sideSize))

        val (label, icon) = when {
            !hasAccount -> "Set up" to Icons.Default.Settings
            hasInput -> "Call" to Icons.Default.Call
            canRedial -> "Redial" to Icons.Default.Replay
            else -> "Call" to Icons.Default.Call
        }
        val enabled = !hasAccount || hasInput || canRedial

        Row(
            modifier = Modifier
                .height(if (isCompactHeight) 60.dp else 66.dp)
                .widthIn(min = 150.dp)
                .shadow(if (enabled) 10.dp else 0.dp, RoundedCornerShape(33.dp), spotColor = CallActionGreen)
                .clip(RoundedCornerShape(33.dp))
                .background(
                    if (enabled) {
                        Brush.linearGradient(listOf(CallActionGreen, BrandGradientEnd))
                    } else {
                        Brush.linearGradient(listOf(CallActionGreen.copy(alpha = 0.35f), BrandGradientEnd.copy(alpha = 0.35f)))
                    }
                )
                .clickable(enabled = enabled, onClick = onCall)
                .padding(horizontal = 28.dp)
                .testTag("dialer_call_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(if (isCompactHeight) 26.dp else 28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Backspace: only shown when there is something to delete; hold to clear everything
        Box(modifier = Modifier.size(sideSize), contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(visible = hasInput, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                Box(
                    modifier = Modifier
                        .size(sideSize)
                        .glass(RoundedCornerShape(18.dp), LocalGlassColors.current)
                        .combinedClickable(onClick = onBackspace, onLongClick = onClearAll)
                        .testTag("dialer_backspace_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Delete. Hold to clear",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
