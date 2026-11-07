
package com.visionstudio.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun VisionStudioTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = StudioGold,
        secondary = StudioBlue,
        background = StudioBg,
        surface = StudioPanel,
        onPrimary = StudioBg,
        onBackground = StudioWhite,
        onSurface = StudioWhite
    )

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
