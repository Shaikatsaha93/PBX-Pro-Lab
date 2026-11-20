package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Colors for the tinted panel look, one set per light/dark theme. */
@Immutable
data class GlassColors(
    val fill: Color,
    val sheen: Color,
    val edgeTop: Color,
    val edgeBottom: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val orbs: List<Color>,
    val grid: Color
)

val LightGlassColors = GlassColors(
    fill = Color.White.copy(alpha = 0.82f),
    sheen = Color(0x145B3FE0),
    edgeTop = Color(0x405B3FE0),
    edgeBottom = Color(0x1400A58E),
    backgroundTop = Color(0xFFF4F3FA),
    backgroundBottom = Color(0xFFEDEBF8),
    // Violet and teal aurora glows
    orbs = listOf(Color(0x806C4DFF), Color(0x6600C2A8), Color(0x4D9D86FF)),
    grid = Color(0x125B3FE0)
)

val DarkGlassColors = GlassColors(
    fill = Color(0xCC15152A),
    sheen = Color(0x1A9D86FF),
    edgeTop = Color(0x599D86FF),
    edgeBottom = Color(0x143DE0C4),
    backgroundTop = Color(0xFF0A0A18),
    backgroundBottom = Color(0xFF0E0D22),
    orbs = listOf(Color(0x996C4DFF), Color(0x6600C2A8), Color(0x664B2FD0)),
    grid = Color(0x0FFFFFFF)
)

val LocalGlassColors = staticCompositionLocalOf { LightGlassColors }

/**
 * Tinted panel: near-opaque fill, a faint brand-tinted wash from the top and a
 * violet-to-teal hairline edge.
 */
fun Modifier.glass(
    shape: Shape,
    colors: GlassColors,
    fill: Color = colors.fill,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(fill, shape)
    .background(Brush.verticalGradient(listOf(colors.sheen, Color.Transparent)), shape)
    .border(
        borderWidth,
        Brush.linearGradient(
            colors = listOf(colors.edgeTop, colors.edgeBottom),
            start = Offset.Zero,
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        ),
        shape
    )

/** Gradient edge for components that take a BorderStroke (Card, Surface). */
fun glassEdgeBrush(colors: GlassColors): Brush = Brush.linearGradient(listOf(colors.edgeTop, colors.edgeBottom))

/** Violet-to-teal brand gradient for the logo mark and primary actions. */
val BrandGradient: Brush
    get() = Brush.linearGradient(listOf(BrandGradientStart, BrandGradientEnd))

/** App-wide backdrop: a dotted "lab" grid with violet and teal aurora glows behind the panels. */
@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LocalGlassColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val step = 22.dp.toPx()
            val dot = 1.dp.toPx()
            var y = step / 2
            while (y < h) {
                var x = step / 2
                while (x < w) {
                    drawCircle(colors.grid, radius = dot, center = Offset(x, y))
                    x += step
                }
                y += step
            }

            val spots = listOf(
                Triple(Offset(w * 0.95f, h * 0.02f), w * 0.85f, 0.60f),
                Triple(Offset(w * 0.00f, h * 0.55f), w * 0.70f, 0.40f),
                Triple(Offset(w * 0.85f, h * 1.00f), w * 0.80f, 0.45f)
            )
            spots.forEachIndexed { i, (center, radius, alpha) ->
                val color = colors.orbs[i % colors.orbs.size]
                drawCircle(
                    brush = Brush.radialGradient(
                        // Orb colors carry their own intensity in alpha
                        colors = listOf(color.copy(alpha = alpha * color.alpha), color.copy(alpha = 0f)),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )
            }
        }
        content()
    }
}
