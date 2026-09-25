package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalGlassColors
import com.example.ui.theme.glass

data class KeypadKey(
    val char: Char,
    val letters: String,
    /** Character typed on long-press, e.g. '+' on the 0 key. */
    val longPressChar: Char? = null
)

val KeypadMatrix = listOf(
    listOf(KeypadKey('1', ""), KeypadKey('2', "ABC"), KeypadKey('3', "DEF")),
    listOf(KeypadKey('4', "GHI"), KeypadKey('5', "JKL"), KeypadKey('6', "MNO")),
    listOf(KeypadKey('7', "PQRS"), KeypadKey('8', "TUV"), KeypadKey('9', "WXYZ")),
    listOf(KeypadKey('*', ""), KeypadKey('0', "+", longPressChar = '+'), KeypadKey('#', ""))
)

/**
 * Phone keypad whose keys stretch to fill the available width, so they are easy to hit on
 * any screen. [keySize] is the key height; long-pressing 0 types '+'.
 */
@Composable
fun DtmfKeypad(
    onKeyPressed: (Char) -> Unit,
    modifier: Modifier = Modifier,
    hapticFeedbackEnabled: Boolean = true,
    keySize: Dp = 72.dp,
    spacing: Dp = 16.dp
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        KeypadMatrix.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowKeys.forEach { key ->
                    KeypadButton(
                        key = key,
                        height = keySize,
                        onClick = {
                            if (hapticFeedbackEnabled) {
                                try {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                } catch (_: Throwable) {}
                            }
                            onKeyPressed(key.char)
                        },
                        onLongClick = key.longPressChar?.let { alt ->
                            {
                                try {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } catch (_: Throwable) {}
                                onKeyPressed(alt)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KeypadButton(
    key: KeypadKey,
    height: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(stiffness = 900f),
        label = "key_press_scale"
    )
    val glassColors = LocalGlassColors.current
    val keyShape = RoundedCornerShape(height * 0.32f)

    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .glass(
                keyShape,
                glassColors,
                fill = if (pressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else glassColors.fill
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(color = MaterialTheme.colorScheme.primary),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics {
                contentDescription = if (key.longPressChar != null) {
                    "${key.char}, hold for ${key.longPressChar}"
                } else {
                    key.char.toString()
                }
            }
            .testTag("dialer_key_${key.char}"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = key.char.toString(),
                fontSize = if (key.char in listOf('*', '#')) 30.sp else 28.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (key.letters.isNotEmpty()) {
                Text(
                    text = key.letters,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
