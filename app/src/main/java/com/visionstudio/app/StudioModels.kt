
package com.visionstudio.app

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

// Shared studio colors
val StudioBg = Color(0xFF0B0C0F)
val StudioPanel = Color(0xFF17191E)
val StudioPanel2 = Color(0xFF20232A)
val StudioGold = Color(0xFFFFC247)
val StudioWhite = Color(0xFFF2F3F5)
val StudioMuted = Color(0xFF9298A4)
val StudioGreen = Color(0xFF42C995)
val StudioBlue = Color(0xFF55A8F5)
val StudioRed = Color(0xFFFF6262)
val StudioPurple = Color(0xFFB58AFF)

// Video clip data
data class VideoClip(
    val id: Int,
    val uri: Uri,
    val name: String,
    val startMs: Long,
    val endMs: Long
) {
    val durationMs: Long
        get() = (endMs - startMs).coerceAtLeast(0L)
}

// Main editing workspaces
enum class StudioPage(
    val title: String,
    val icon: ImageVector
) {
    MEDIA("Media", Icons.Default.VideoLibrary),
    CUT("Cut", Icons.Default.ContentCut),
    EDIT("Edit", Icons.Default.Edit),
    FUSION("Fusion", Icons.Default.AutoAwesome),
    COLOR("Color", Icons.Default.Palette),
    AUDIO("Audio", Icons.Default.GraphicEq)
}
