
package com.visionstudio.app

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import java.util.Locale

private val StudioBg = Color(0xFF0B0C0F)
private val StudioPanel = Color(0xFF17191E)
private val StudioPanel2 = Color(0xFF20232A)
private val StudioGold = Color(0xFFFFC247)
private val StudioWhite = Color(0xFFF2F3F5)
private val StudioMuted = Color(0xFF9298A4)
private val StudioGreen = Color(0xFF42C995)
private val StudioBlue = Color(0xFF55A8F5)
private val StudioRed = Color(0xFFFF6262)
private val StudioPurple = Color(0xFFB58AFF)

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

enum class StudioPage(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    MEDIA("Media", Icons.Default.VideoLibrary),
    CUT("Cut", Icons.Default.ContentCut),
    EDIT("Edit", Icons.Default.Edit),
    FUSION("Fusion", Icons.Default.AutoAwesome),
    COLOR("Color", Icons.Default.Palette),
    AUDIO("Audio", Icons.Default.GraphicEq)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = StudioBg,
                    surface = StudioPanel,
                    primary = StudioGold
                )
            ) {
                VisionStudioApp()
            }
        }
    }
}

@Composable
fun VisionStudioApp() {
    val context = androidx.compose.ui.platform.LocalContext.current

    var page by remember { mutableStateOf(StudioPage.EDIT) }
    var projectName by remember { mutableStateOf("Untitled Project") }
    var status by remember { mutableStateOf("Ready • Import media to begin") }
    var selectedClipId by remember { mutableIntStateOf(-1) }
    var nextId by remember { mutableIntStateOf(1) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var playerDuration by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf(false) }

    val clips = remember { mutableStateListOf<VideoClip>() }
    val player = remember { ExoPlayer.Builder(context).build() }

    val selectedClip = clips.firstOrNull { it.id == selectedClipId }

    fun rebuildPlaylist(index: Int = 0, position: Long = 0L) {
        if (clips.isEmpty()) {
            player.pause()
            player.clearMediaItems()
            currentPosition = 0L
            playerDuration = 0L
            return
        }

        val items = clips.map { clip ->
            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(clip.startMs)
                .setEndPositionMs(clip.endMs)
                .build()

            MediaItem.Builder()
                .setUri(clip.uri)
                .setMediaId(clip.id.toString())
                .setClippingConfiguration(clipping)
                .build()
        }

        val safeIndex = index.coerceIn(0, items.lastIndex)
        player.pause()
        player.setMediaItems(items, safeIndex, position.coerceAtLeast(0L))
        player.prepare()
    }

    fun importVideo(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }

        try {
            val retriever = MediaMetadataRetriever()
            val length: Long
            try {
                retriever.setDataSource(context, uri)
                length = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L
            } finally {
                retriever.release()
            }

            if (length <= 0L) {
                status = "Could not read video duration"
                return
            }

            val name = uri.lastPathSegment ?: "Video ${nextId}"
            val clip = VideoClip(
                id = nextId++,
                uri = uri,
                name = name,
                startMs = 0L,
                endMs = length
            )

            clips.add(clip)
            selectedClipId = clip.id
            rebuildPlaylist(clips.lastIndex)
            status = "Media added to timeline"
            page = StudioPage.EDIT
        } catch (e: Exception) {
            status = "Import failed: ${e.message}"
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) importVideo(uri)
    }

    fun splitClip() {
        val index = clips.indexOfFirst { it.id == selectedClipId }
        if (index < 0) {
            status = "Select a clip first"
            return
        }

        val clip = clips[index]
        val splitAt = clip.startMs + player.currentPosition

        if (splitAt <= clip.startMs + 500L ||
            splitAt >= clip.endMs - 500L
        ) {
            status = "Move playhead inside the selected clip"
            return
        }

        val first = clip.copy(endMs = splitAt)
        val second = clip.copy(
            id = nextId++,
            name = clip.name + " • Part 2",
            startMs = splitAt
        )

        clips[index] = first
        clips.add(index + 1, second)
        selectedClipId = second.id
        rebuildPlaylist(index + 1)
        status = "Clip split into two parts"
    }

    fun deleteClip() {
        val index = clips.indexOfFirst { it.id == selectedClipId }
        if (index < 0) {
            status = "Select a clip to delete"
            return
        }

        clips.removeAt(index)

        if (clips.isEmpty()) {
            selectedClipId = -1
            rebuildPlaylist()
        } else {
            val next = index.coerceAtMost(clips.lastIndex)
            selectedClipId = clips[next].id
            rebuildPlaylist(next)
        }

        status = "Selected clip deleted"
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int
            ) {
                selectedClipId = mediaItem?.mediaId?.toIntOrNull() ?: -1
            }

            override fun onPlaybackStateChanged(state: Int) {
                playerDuration = player.duration.coerceAtLeast(0L)
            }
        }

        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            currentPosition = player.currentPosition.coerceAtLeast(0L)
            playerDuration = player.duration.coerceAtLeast(0L)
            delay(250)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioBg)
    ) {
        StudioTopBar(
            projectName = projectName,
            onProjectNameChange = { projectName = it }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            PreviewPanel(
                player = player,
                hasVideo = clips.isNotEmpty(),
                playing = playing,
                position = currentPosition,
                duration = playerDuration,
                onPlay = {
                    if (clips.isNotEmpty()) {
                        if (player.isPlaying) player.pause()
                        else player.play()
                    }
                },
                onStart = {
                    if (clips.isNotEmpty()) player.seekTo(0L)
                },
                onEnd = {
                    if (clips.isNotEmpty()) {
                        player.seekTo(player.duration.coerceAtLeast(0L))
                    }
                }
            )

            PageTools(
                page = page,
                clip = selectedClip,
                clipCount = clips.size,
                onImport = {
                    picker.launch(arrayOf("video/*"))
                },
                onSplit = { splitClip() },
                onDelete = { deleteClip() },
                onStatus = { status = it }
            )

            TimelinePanel(
                clips = clips,
                selectedId = selectedClipId,
                position = currentPosition,
                onSelect = { index, clip ->
                    selectedClipId = clip.id
                    player.seekTo(index, 0L)
                    player.pause()
                }
            )

            InspectorPanel(
                page = page,
                clip = selectedClip,
                status = status
            )

            Spacer(Modifier.height(12.dp))
        }

        StudioBottomNavigation(
            selected = page,
            onSelect = { page = it }
        )
    }
}

@Composable
private fun StudioTopBar(
    projectName: String,
    onProjectNameChange: (String) -> Unit
) {
    var editing by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioPanel)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "VISION STUDIO",
                color = StudioGold,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            if (editing) {
                TextField(
                    value = projectName,
                    onValueChange = onProjectNameChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = StudioWhite,
                        fontSize = 12.sp
                    )
                )
            } else {
                Text(
                    projectName,
                    color = StudioMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { editing = true }
                )
            }
        }

        IconButton(onClick = { editing = !editing }) {
            Icon(Icons.Default.Edit, "Rename project", tint = StudioGold)
        }

        IconButton(onClick = { }) {
            Icon(Icons.Default.Settings, "Settings", tint = StudioWhite)
        }
    }
}

@Composable
private fun PreviewPanel(
    player: ExoPlayer,
    hasVideo: Boolean,
    playing: Boolean,
    position: Long,
    duration: Long,
    onPlay: () -> Unit,
    onStart: () -> Unit,
    onEnd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .background(StudioPanel, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("VIEWER", color = StudioGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("16:9 • Preview", color = StudioMuted, fontSize = 10.sp)
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (hasVideo) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { PlayerView(it).apply { useController = false } },
                    update = { it.player = player }
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Movie, null, tint = StudioGold, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(7.dp))
                    Text("No media loaded", color = StudioWhite, fontSize = 12.sp)
                    Text("Import a video to start editing", color = StudioMuted, fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(formatTime(position), color = StudioGold, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onStart, enabled = hasVideo) {
                Icon(Icons.Default.SkipPrevious, null, tint = StudioWhite)
            }
            IconButton(
                onClick = onPlay,
                enabled = hasVideo,
                modifier = Modifier
                    .size(42.dp)
                    .background(StudioGold, RoundedCornerShape(50))
            ) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null,
                    tint = Color.Black
                )
            }
            IconButton(onClick = onEnd, enabled = hasVideo) {
                Icon(Icons.Default.SkipNext, null, tint = StudioWhite)
            }
            Spacer(Modifier.weight(1f))
            Text(formatTime(duration), color = StudioMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PageTools(
    page: StudioPage,
    clip: VideoClip?,
    clipCount: Int,
    onImport: () -> Unit,
    onSplit: () -> Unit,
    onDelete: () -> Unit,
    onStatus: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .background(StudioPanel, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                page.title.uppercase(Locale.getDefault()),
                color = StudioGold,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(Modifier.weight(1f))
            Text("$clipCount media clips", color = StudioMuted, fontSize = 10.sp)
        }

        Spacer(Modifier.height(10.dp))

        when (page) {
            StudioPage.MEDIA -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolButton("Import Video", Icons.Default.Add, Modifier.weight(1f), onImport)
                    ToolButton("Media Library", Icons.Default.Folder, Modifier.weight(1f)) {
                        onStatus("Imported media is listed in the timeline")
                    }
                }
            }

            StudioPage.CUT, StudioPage.EDIT -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolButton("Split", Icons.Default.ContentCut, Modifier.weight(1f), onSplit)
                    ToolButton("Delete", Icons.Default.Delete, Modifier.weight(1f), onDelete, danger = true)
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    clip?.name ?: "Select a clip in the timeline",
                    color = StudioMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            StudioPage.FUSION -> FeaturePlaceholder(
                "Node-based compositing workspace",
                "The Fusion workspace structure is reserved for future node tools."
            )

            StudioPage.COLOR -> FeaturePlaceholder(
                "Color grading workspace",
                "Color wheels, curves and scopes will be implemented in this module."
            )

            StudioPage.AUDIO -> FeaturePlaceholder(
                "Audio workstation",
                "Audio tracks, waveform editing and mixing will be implemented here."
            )
        }
    }
}

@Composable
private fun FeaturePlaceholder(title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Construction, null, tint = StudioGold)
        Spacer(Modifier.width(9.dp))
        Column {
            Text(title, color = StudioWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(description, color = StudioMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TimelinePanel(
    clips: List<VideoClip>,
    selectedId: Int,
    position: Long,
    onSelect: (Int, VideoClip) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .background(StudioPanel, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TIMELINE", color = StudioGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text("MULTI-TRACK EDITOR", color = StudioMuted, fontSize = 9.sp)
        }

        Spacer(Modifier.height(9.dp))

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            repeat(9) { index ->
                Text("${index * 5}s", color = StudioMuted, fontSize = 9.sp, modifier = Modifier.width(75.dp))
            }
        }

        Spacer(Modifier.height(5.dp))

        TimelineTrack("V1", StudioGold, "MAIN VIDEO", clips, selectedId, onSelect)
        TimelineTrack("V2", StudioBlue, "OVERLAY", emptyList(), -1) { _, _ -> }
        TimelineTrack("A1", StudioGreen, "AUDIO", emptyList(), -1) { _, _ -> }
        TimelineTrack("T1", StudioPurple, "TEXT", emptyList(), -1) { _, _ -> }

        Spacer(Modifier.height(7.dp))
        Text("Playhead: ${formatTime(position)}", color = StudioMuted, fontSize = 10.sp)
    }
}

@Composable
private fun TimelineTrack(
    label: String,
    color: Color,
    description: String,
    clips: List<VideoClip>,
    selectedId: Int,
    onSelect: (Int, VideoClip) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .width(48.dp)
                .height(48.dp)
                .background(StudioPanel2, RoundedCornerShape(6.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(description, color = StudioMuted, fontSize = 6.sp)
        }

        Spacer(Modifier.width(6.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .horizontalScroll(rememberScrollState())
                .background(StudioBg, RoundedCornerShape(6.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (clips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(190.dp)
                        .border(1.dp, StudioPanel2, RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Empty track", color = StudioMuted, fontSize = 9.sp)
                }
            } else {
                clips.forEachIndexed { index, clip ->
                    val active = clip.id == selectedId
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width((clip.durationMs / 1000f * 8f).coerceIn(85f, 220f).dp)
                            .background(
                                if (active) Color(0xFF59451F) else Color(0xFF34302A),
                                RoundedCornerShape(5.dp)
                            )
                            .border(
                                if (active) 1.5.dp else 0.dp,
                                if (active) StudioGold else Color.Transparent,
                                RoundedCornerShape(5.dp)
                            )
                            .clickable { onSelect(index, clip) }
                            .padding(5.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "CLIP ${index + 1}",
                            color = if (active) StudioGold else StudioWhite,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(formatTime(clip.durationMs), color = StudioMuted, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorPanel(
    page: StudioPage,
    clip: VideoClip?,
    status: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .background(StudioPanel, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tune, null, tint = StudioGold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text("INSPECTOR", color = StudioGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(page.title, color = StudioMuted, fontSize = 10.sp)
        }

        Spacer(Modifier.height(8.dp))

        if (clip == null) {
            Text("Select an item to inspect its properties", color = StudioMuted, fontSize = 10.sp)
        } else {
            Text(clip.name, color = StudioWhite, fontSize = 11.sp, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(
                "Source range: ${formatTime(clip.startMs)} – ${formatTime(clip.endMs)}",
                color = StudioMuted,
                fontSize = 10.sp
            )
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = StudioPanel2)
        Spacer(Modifier.height(7.dp))
        Text(status, color = StudioGreen, fontSize = 10.sp)
    }
}

@Composable
private fun ToolButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    Row(
        modifier = modifier
            .background(
                if (danger) Color(0xFF382126) else StudioPanel2,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 9.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (danger) StudioRed else StudioGold,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            title,
            color = if (danger) StudioRed else StudioWhite,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StudioBottomNavigation(
    selected: StudioPage,
    onSelect: (StudioPage) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111318))
            .padding(horizontal = 3.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StudioPage.values().forEach { item ->
            val active = selected == item
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(item) }
                    .padding(vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.title,
                    tint = if (active) StudioGold else StudioMuted,
                    modifier = Modifier.size(21.dp)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    item.title,
                    color = if (active) StudioGold else StudioMuted,
                    fontSize = 9.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                )
                if (active) {
                    Spacer(Modifier.height(3.dp))
                    Box(
                        Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(StudioGold, RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val total = milliseconds.coerceAtLeast(0L) / 1000L
    val minutes = total / 60L
    val seconds = total % 60L
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
