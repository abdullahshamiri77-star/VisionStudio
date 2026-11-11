
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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.visionstudio.app.core.project.Project
import com.visionstudio.app.core.project.ProjectHomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VisionStudioTheme {
                VisionStudioApp()
            }
        }
    }
}

@Composable
fun VisionStudioApp() {
    val context = androidx.compose.ui.platform.LocalContext.current

    var showProjectHome by remember { mutableStateOf(true) }
    var activeProject by remember { mutableStateOf<Project?>(null) }

    var page by remember { mutableStateOf(StudioPage.EDIT) }
    var projectName by remember { mutableStateOf("Untitled Project") }
    var status by remember { mutableStateOf("Ready • Import media to begin") }
    var selectedClipId by remember { mutableIntStateOf(-1) }
    var nextId by remember { mutableIntStateOf(1) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var playerDuration by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf(false) }

    var timelineState by remember { mutableStateOf(
        TimelineState(tracks = TimelineEngine.createDefaultTracks())
    ) }
    val clips = timelineState.videoClips.mapNotNull { timelineClip ->
        val id = timelineClip.id.toIntOrNull() ?: return@mapNotNull null
        VideoClip(
            id = id,
            uri = Uri.parse(timelineClip.sourceUri),
            name = timelineClip.name,
            startMs = timelineClip.sourceStartMs,
            endMs = timelineClip.sourceEndMs
        )
    }
    val player = remember { ExoPlayer.Builder(context).build() }

    val selectedClip = clips.firstOrNull {
        it.id == selectedClipId
    }

    fun currentVideoClips(): List<VideoClip> = timelineState.videoClips.mapNotNull { item ->
        val id = item.id.toIntOrNull() ?: return@mapNotNull null
        VideoClip(id, Uri.parse(item.sourceUri), item.name, item.sourceStartMs, item.sourceEndMs)
    }

    fun rebuildPlaylist(index: Int = 0, position: Long = 0L) {
        val playlistClips = currentVideoClips()
        if (playlistClips.isEmpty()) {
            player.pause()
            player.clearMediaItems()
            currentPosition = 0L
            playerDuration = 0L
            return
        }

        val items = playlistClips.map { clip ->
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
            // بعض مصادر الملفات لا تمنح صلاحية دائمة.
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

            val name = uri.lastPathSegment ?: "Video $nextId"

            val clipId = nextId++
            val videoTrack = timelineState.tracks.firstOrNull {
                it.type == TimelineTrackType.VIDEO && !it.isLocked
            } ?: throw IllegalStateException("No available video track")

            val timelineClip = TimelineVideoClip(
                id = clipId.toString(),
                trackId = videoTrack.id,
                sourceUri = uri.toString(),
                name = name,
                positionMs = timelineState.durationMs,
                durationMs = length,
                sourceStartMs = 0L,
                sourceEndMs = length
            )
            timelineState = TimelineEngine.addVideoClip(timelineState, timelineClip)
            selectedClipId = clipId

            rebuildPlaylist(currentVideoClips().lastIndex)
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
        val index = clips.indexOfFirst {
            it.id == selectedClipId
        }

        if (index < 0) {
            status = "Select a clip first"
            return
        }

        val clip = timelineState.videoClips.firstOrNull {
            it.id == selectedClipId.toString()
        } ?: run {
            status = "Selected clip is missing from timeline state"
            return
        }

        // ExoPlayer reports the local position inside the selected source clip.
        val splitAtTimelineMs = clip.positionMs + player.currentPosition
        val secondId = nextId++.toString()
        try {
            timelineState = TimelineEngine.splitVideoClip(
                state = timelineState,
                clipId = clip.id,
                splitAtMs = splitAtTimelineMs,
                secondClipId = secondId
            )
        } catch (e: IllegalArgumentException) {
            status = e.message ?: "Unable to split clip at this position"
            return
        }

        selectedClipId = secondId.toInt()
        rebuildPlaylist(index + 1)

        status = "Clip split into two parts"
    }

    fun deleteClip() {
        val index = clips.indexOfFirst {
            it.id == selectedClipId
        }

        if (index < 0) {
            status = "Select a clip to delete"
            return
        }

        timelineState = TimelineEngine.removeVideoClip(
            timelineState,
            selectedClipId.toString()
        )
        val remainingClips = currentVideoClips()

        if (remainingClips.isEmpty()) {
            selectedClipId = -1
            rebuildPlaylist()
        } else {
            val next = index.coerceAtMost(remainingClips.lastIndex)
            selectedClipId = remainingClips[next].id
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
                selectedClipId =
                    mediaItem?.mediaId?.toIntOrNull() ?: -1
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
            val activeIndex = player.currentMediaItemIndex.coerceAtLeast(0)
            val timelinePosition = currentVideoClips().take(activeIndex)
                .sumOf { it.durationMs } + currentPosition
            val safePlayhead = timelinePosition.coerceIn(0L, timelineState.durationMs)
            if (timelineState.playheadMs != safePlayhead) {
                timelineState = TimelineEngine.setPlayhead(timelineState, safePlayhead)
            }
            delay(250)
        }
    }

    if (showProjectHome) {
        ProjectHomeScreen(
            onOpenProject = { project ->
                activeProject = project
                projectName = project.name
                timelineState = timelineState.copy(
                    settings = TimelineProjectSettings(
                        width = project.settings.width,
                        height = project.settings.height,
                        frameRate = project.settings.frameRate,
                        audioSampleRate = project.settings.audioSampleRate
                    )
                )
                showProjectHome = false
                status = "Project opened: ${project.name}"
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(StudioBg)
        ) {
            StudioTopBar(
                projectName = projectName,
                onProjectNameChange = { projectName = it },
                onHome = {
                    player.pause()
                    showProjectHome = true
                }
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
                        timelineState = TimelineEngine.setPlayhead(
                            timelineState,
                            timelineState.videoClips.getOrNull(index)?.positionMs ?: 0L
                        )
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
}

@Composable
private fun StudioTopBar(
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    onHome: () -> Unit
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
                    modifier = Modifier.clickable {
                        editing = true
                    }
                )
            }
        }

        IconButton(onClick = { editing = !editing }) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Rename project",
                tint = StudioGold
            )
        }

        IconButton(onClick = onHome) {
            Icon(
                Icons.Default.Folder,
                contentDescription = "Projects",
                tint = StudioGold
            )
        }

        IconButton(onClick = { }) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = StudioWhite
            )
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
            Text(
                "VIEWER",
                color = StudioGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

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
                    factory = {
                        PlayerView(it).apply {
                            useController = false
                        }
                    },
                    update = {
                        it.player = player
                    }
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Movie,
                        null,
                        tint = StudioGold,
                        modifier = Modifier.size(42.dp)
                    )

                    Spacer(Modifier.height(7.dp))

                    Text("No media loaded", color = StudioWhite, fontSize = 12.sp)

                    Text(
                        "Import a video to start editing",
                        color = StudioMuted,
                        fontSize = 10.sp
                    )
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
                    if (playing) Icons.Default.Pause
                    else Icons.Default.PlayArrow,
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
                    ToolButton(
                        "Import Video",
                        Icons.Default.Add,
                        Modifier.weight(1f),
                        onImport
                    )

                    ToolButton(
                        "Media Library",
                        Icons.Default.Folder,
                        Modifier.weight(1f),
                        onClick = {
                            onStatus("Imported media is listed in the timeline")
                        }
                    )
                }
            }

            StudioPage.CUT, StudioPage.EDIT -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ToolButton(
                        "Split",
                        Icons.Default.ContentCut,
                        Modifier.weight(1f),
                        onSplit
                    )

                    ToolButton(
                        "Delete",
                        Icons.Default.Delete,
                        Modifier.weight(1f),
                        onDelete,
                        danger = true
                    )
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
private fun FeaturePlaceholder(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = StudioGold,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = description,
            color = StudioMuted,
            style = MaterialTheme.typography.bodyMedium
        )
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
            .background(StudioPanel)
            .padding(12.dp)
    ) {
        Text(
            text = "Timeline",
            color = StudioWhite,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Playhead: ${formatTime(position)}",
            color = StudioMuted,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(10.dp))

        if (clips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(StudioPanel2, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Import media to start editing",
                    color = StudioMuted
                )
            }
        } else {
            clips.forEachIndexed { index, clip ->
                TimelineTrackRow(
                    clip = clip,
                    index = index,
                    selected = clip.id == selectedId,
                    onClick = { onSelect(index, clip) }
                )

                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun TimelineTrackRow(
    clip: VideoClip,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) StudioGold.copy(alpha = 0.16f)
                else StudioPanel2
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "V${index + 1}",
            color = StudioGold,
            style = MaterialTheme.typography.labelMedium
        )

        Spacer(Modifier.width(10.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = clip.name,
                color = StudioWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = formatTime(clip.durationMs),
                color = StudioMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (selected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = StudioGold
            )
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
            .background(StudioPanel)
            .padding(12.dp)
    ) {
        Text(
            text = "Inspector",
            color = StudioGold,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Workspace: ${page.title}",
            color = StudioWhite,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = clip?.name ?: "No clip selected",
            color = StudioMuted,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = status,
            color = StudioMuted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ToolButton(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(StudioPanel2)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (danger) StudioRed else StudioGold
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = title,
            color = if (danger) StudioRed else StudioWhite,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun StudioBottomNavigation(
    selected: StudioPage,
    onSelect: (StudioPage) -> Unit
) {
    NavigationBar(
        containerColor = StudioPanel,
        contentColor = StudioWhite
    ) {
        StudioPage.values().forEach { page ->
            NavigationBarItem(
                selected = selected == page,
                onClick = { onSelect(page) },
                icon = {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = page.title
                    )
                },
                label = {
                    Text(page.title)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = StudioGold,
                    selectedTextColor = StudioGold,
                    indicatorColor = StudioGold.copy(alpha = 0.15f),
                    unselectedIconColor = StudioMuted,
                    unselectedTextColor = StudioMuted
                )
            )
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val safeTime = milliseconds.coerceAtLeast(0L)
    val totalSeconds = safeTime / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (safeTime % 1000) / 10

    return "%02d:%02d.%02d".format(
        minutes,
        seconds,
        millis
    )
}
