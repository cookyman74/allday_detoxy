package com.allday.detoxy.presentation.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import dev.chrisbanes.haze.HazeState

/**
 * CompositionLocal to provide a shared [HazeState] down the tree.
 * This allows [liquidGlass] modifier to access the state without explicitly passing it.
 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }
