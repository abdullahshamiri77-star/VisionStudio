
package com.visionstudio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF101114)
private val Panel = Color(0xFF1B1A1F)
private val Gold = Color(0xFFFFC247)
private val Muted = Color(0xFF92909A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VisionApp() }
    }
}

@Composable
fun VisionApp() {
    var selected by remember { mutableStateOf("Edit") }
    var playhead by remember { mutableFloatStateOf(0.22f) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            background = Bg,
            surface = Panel
        )
    ) {
        Column(
            Modifier.fillMaxSize().background(Bg).padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp)
                        .background(Gold, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "V",
                        color = Bg,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column {
                    Text(
                        "VISION STUDIO",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        "PRO VIDEO EDITOR • 0.1",
                        color = Muted,
                        fontSize = 10.sp
                    )
                }

                Spacer(Modifier.weight(1f))

                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Muted
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Media", "Edit", "Text", "Audio", "FX", "Color")
                    .forEach { item ->
                        FilterChip(
                            selected = selected == item,
                            onClick = { selected = item },
                            label = { Text(item) }
                        )
                    }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                Modifier.fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Movie,
                        tint = Color(0xFF45434A),
                        modifier = Modifier.size(54.dp),
                        contentDescription = null
                    )
                    Text(
                        "VIDEO PREVIEW",
                        color = Color(0xFF55525B),
                        fontSize = 12.sp,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "استورد مقطعًا لبدء التحرير",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }

                Row(
                    Modifier.align(Alignment.BottomCenter).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AspectRatio,
                        tint = Muted,
                        modifier = Modifier.size(16.dp),
                        contentDescription = null
                    )
                    Text(
                        "  16:9     •     1080p",
                        color = Muted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White
                    )
                }

                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Gold,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    "00:00:12:08",
                    color = Gold,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    " / 00:00:54:00",
                    color = Muted,
                    fontSize = 12.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "TIMELINE",
                    color = Gold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )

                Spacer(Modifier.weight(1f))

                Icon(
                    Icons.Default.ZoomOut,
                    contentDescription = "Zoom out",
                    tint = Muted
                )
                Icon(
                    Icons.Default.ZoomIn,
                    contentDescription = "Zoom in",
                    tint = Muted
                )
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Timeline settings",
                    tint = Muted
                )
            }

            Slider(
                value = playhead,
                onValueChange = { playhead = it },
                colors = SliderDefaults.colors(
                    thumbColor = Gold,
                    activeTrackColor = Gold
                )
            )

            Column(
                Modifier.fillMaxWidth()
                    .background(Panel, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Track("T", "نص / عناوين", Color(0xFF9B6BCE))
                Track("V", "فيديو / صور", Color(0xFF4C91D8))
                Track("A", "صوت / موسيقى", Color(0xFF38A78B))
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomTool("إضافة", Icons.Default.Add)
                BottomTool("قص", Icons.Default.ContentCut)
                BottomTool("طبقات", Icons.Default.Layers)
                BottomTool("تصدير", Icons.Default.Outbox)
            }
        }
    }
}

@Composable
fun Track(tag: String, title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.width(28.dp).height(28.dp)
                .background(color.copy(alpha = .18f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(tag, color = color, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.width(8.dp))

        Text(
            title,
            color = Color.White,
            fontSize = 11.sp,
            modifier = Modifier.width(85.dp)
        )

        Box(
            Modifier.weight(1f).height(28.dp)
                .background(color.copy(alpha = .22f), RoundedCornerShape(6.dp))
        ) {
            Box(
                Modifier.fillMaxWidth(.58f).fillMaxHeight()
                    .background(color.copy(alpha = .7f), RoundedCornerShape(6.dp))
            )
        }
    }
}

@Composable
fun BottomTool(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { }.padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Gold
        )
        Text(label, color = Muted, fontSize = 10.sp)
    }
}
