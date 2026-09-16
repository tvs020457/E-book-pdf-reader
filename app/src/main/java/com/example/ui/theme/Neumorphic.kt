package com.example.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class NeuColors(
    val background: Color,
    val lightShadow: Color,
    val darkShadow: Color,
    val text: Color,
    val accent: Color,
    val red: Color
)

val LightNeuColors = NeuColors(
    background = Color(0xFFE0E5EC),
    lightShadow = Color(0xFFFFFFFF),
    darkShadow = Color(0xFFA3B1C6),
    text = Color(0xFF4A5568),
    accent = Color(0xFF4299E1),
    red = Color(0xFFE53E3E)
)

val DarkNeuColors = NeuColors(
    background = Color(0xFF2D3748),
    lightShadow = Color(0xFF4A5568),
    darkShadow = Color(0xFF1A202C),
    text = Color(0xFFE2E8F0),
    accent = Color(0xFF63B3ED),
    red = Color(0xFFFC8181)
)

val SepiaNeuColors = NeuColors(
    background = Color(0xFFF4ECD8),
    lightShadow = Color(0xFFFFFFFF),
    darkShadow = Color(0xFFD3C5A3),
    text = Color(0xFF5B4636),
    accent = Color(0xFFD69E2E),
    red = Color(0xFFE53E3E)
)

val LocalNeuColors = compositionLocalOf { LightNeuColors }

fun Modifier.neumorphic(
    isPressed: Boolean = false,
    isCircle: Boolean = false,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 6.dp
) = this.composed {
    val neuColors = LocalNeuColors.current
    val shape = if (isCircle) CircleShape else RoundedCornerShape(cornerRadius)
    
    this.drawBehind {
        drawIntoCanvas { canvas ->
            val w = size.width
            val h = size.height
            val r = if (isCircle) w / 2f else cornerRadius.toPx()
            val offset = elevation.toPx()
            val blur = elevation.toPx() * 1.2f

            val paintDark = Paint().asFrameworkPaint().apply {
                color = neuColors.darkShadow.toArgb()
                maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
            }
            val paintLight = Paint().asFrameworkPaint().apply {
                color = neuColors.lightShadow.toArgb()
                maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
            }

            if (!isPressed) {
                canvas.nativeCanvas.drawRoundRect(offset, offset, w + offset, h + offset, r, r, paintDark)
                canvas.nativeCanvas.drawRoundRect(-offset, -offset, w - offset, h - offset, r, r, paintLight)
            } else {
                // Simple pressed state: inverted shadows and smaller offset
                canvas.nativeCanvas.drawRoundRect(offset/2, offset/2, w + offset/2, h + offset/2, r, r, paintLight)
                canvas.nativeCanvas.drawRoundRect(-offset/2, -offset/2, w - offset/2, h - offset/2, r, r, paintDark)
            }
        }
    }
    .clip(shape)
    .background(neuColors.background)
}

@Composable
fun NeuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCircle: Boolean = false,
    cornerRadius: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .neumorphic(isPressed = isPressed, isCircle = isCircle, cornerRadius = cornerRadius)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .neumorphic(isPressed = false, isCircle = false, cornerRadius = cornerRadius)
            .padding(contentPadding)
    ) {
        content()
    }
}
