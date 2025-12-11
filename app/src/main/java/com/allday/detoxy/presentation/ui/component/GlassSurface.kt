package com.allday.detoxy.presentation.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.allday.detoxy.presentation.ui.theme.GlassWhite

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    blurRadius: Dp = 20.dp,
    alpha: Float = 0.4f,
    tint: Color = GlassWhite.copy(alpha = alpha),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlass(
            shape = shape,
            blurRadius = blurRadius,
            alpha = alpha,
            glassTint = tint
        ),
        content = content
    )
}
