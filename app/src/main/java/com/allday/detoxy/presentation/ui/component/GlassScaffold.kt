package com.allday.detoxy.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.allday.detoxy.presentation.ui.theme.LocalHazeState
import com.allday.detoxy.presentation.ui.theme.MeshGradientBackgroundDark
import com.allday.detoxy.presentation.ui.theme.MeshGradientBackgroundLight
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

/**
 * A Scaffold-like container that provides the Glassmorphism environment.
 * It sets up the [HazeState] and the Mesh Gradient background.
 *
 * All child components that use [Modifier.liquidGlass] must be placed within a [GlassScaffold]
 * (or manually provided with a HazeState).
 */
@Composable
fun GlassScaffold(
    modifier: Modifier = Modifier,
    hazeState: HazeState = remember { HazeState() },
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val backgroundBrush = if (isDark) MeshGradientBackgroundDark else MeshGradientBackgroundLight

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundBrush) // 1. Draw Background
                .haze(state = hazeState)     // 2. Set this Box as the Haze Source
        ) {
            content() // 3. Draw Foreground Content (which can use hazeChild)
        }
    }
}
