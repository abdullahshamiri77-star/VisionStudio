
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.util.Locale

private val BackgroundColor = Color(0xFF101114)
private val PanelColor = Color(0xFF1B1C21)
private val GoldColor = Color(0xFFFFC247)
private val WhiteColor = Color(0xFFF5F5F5)
private val MutedColor = Color(0xFF9999A3)
private val RedColor = Color(0xFFFF6262)
private val GreenColor = Color(0xFF48C78E)

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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                VisionStudioApp()
            }
        }
    }
}

@Composable
fun VisionStudioApp() {
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf("الميديا") }
    var videoTitle by remember {
        mutableStateOf("لم يتم اختيار فيديو")
    }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }
    var statusMessage by remember {
        mutableStateOf("استورد فيديو لبدء مشروعك")
    }

    val clips = remember {
        mutableStateListOf<VideoClip>()
    }

    var selectedClipId by remember {
        mutableIntStateOf(-1)
    }

    var nextClipId by remember {
        mutableIntStateOf(1)
    }

    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    val selectedClip = clips.firstOrNull {
        it.id == selectedClipId
    }

    fun rebuildPlaylist(
        startIndex: Int = 0,
        positionMs: Long = 0L
    ) {
        if (clips.isEmpty()) {
            player.pause()
            player.clearMediaItems()
            currentPosition = 0L
            duration = 0L
            return
        }

        val items = clips.map { clip ->
            val clipping =
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.startMs)
                    .setEndPositionMs(clip.endMs)
                    .build()

            MediaItem.Builder()
                .setUri(clip.uri)
                .setMediaId(clip.id.toString())
                .setClippingConfiguration(clipping)
                .build()
        }

        val safeIndex =
            startIndex.coerceIn(0, items.lastIndex)

        player.pause()
        player.setMediaItems(
            items,
            safeIndex,
            positionMs.coerceAtLeast(0L)
        )
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

            val videoDuration: Long

            try {
                retriever.setDataSource(context, uri)

                videoDuration = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull() ?: 0L
            } finally {
                retriever.release()
            }

            if (videoDuration <= 0L) {
                statusMessage = "تعذر قراءة مدة الفيديو"
                return
            }

            val name = uri.lastPathSegment
                ?: "Video ${nextClipId}"

            val newClip = VideoClip(
                id = nextClipId++,
                uri = uri,
                name = name,
                startMs = 0L,
                endMs = videoDuration
            )

            clips.add(newClip)
            selectedClipId = newClip.id
            videoTitle = name

            rebuildPlaylist(clips.lastIndex, 0L)

            statusMessage = "تمت إضافة الفيديو إلى Timeline"

        } catch (e: Exception) {
            statusMessage =
                "تعذر استيراد الفيديو: ${e.message}"
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importVideo(uri)
        }
    }

    fun splitSelectedClip() {
        val index = clips.indexOfFirst {
            it.id == selectedClipId
        }

        if (index < 0) {
            statusMessage = "حدد مقطعًا أولًا"
            return
        }

        val clip = clips[index]

        val splitPoint =
            clip.startMs + player.currentPosition

        if (
            splitPoint <= clip.startMs + 500L ||
            splitPoint >= clip.endMs - 500L
        ) {
            statusMessage =
                "اختر موضعًا داخل المقطع بعيدًا عن البداية والنهاية"
            return
        }

        val firstPart = clip.copy(
            endMs = splitPoint
        )

        val secondPart = clip.copy(
            id = nextClipId++,
            name = clip.name + " - الجزء الثاني",
            startMs = splitPoint
        )

        clips[index] = firstPart
        clips.add(index + 1, secondPart)

        selectedClipId = secondPart.id

        rebuildPlaylist(index + 1, 0L)

        statusMessage = "تم تقسيم المقطع إلى جزأين"
    }

    fun deleteSelectedClip() {
        val index = clips.indexOfFirst {
            it.id == selectedClipId
        }

        if (index < 0) {
            statusMessage = "حدد مقطعًا للحذف"
            return
        }

        clips.removeAt(index)

        if (clips.isEmpty()) {
            selectedClipId = -1
            videoTitle = "لم يتم اختيار فيديو"
            rebuildPlaylist()
            statusMessage = "تم حذف آخر مقطع"
        } else {
            val newIndex =
                index.coerceAtMost(clips.lastIndex)

            selectedClipId = clips[newIndex].id

            rebuildPlaylist(newIndex, 0L)

            statusMessage = "تم حذف المقطع المحدد"
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {

            override fun onIsPlayingChanged(
                playing: Boolean
            ) {
                isPlaying = playing
            }

            override fun onMediaItemTransition(
                mediaItem: MediaItem?,
                reason: Int
            ) {
                val id = mediaItem?.mediaId?.toIntOrNull()

                if (id != null) {
                    selectedClipId = id
                }
            }

            override fun onPlaybackStateChanged(
                state: Int
            ) {
                duration =
                    player.duration.coerceAtLeast(0L)
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
            currentPosition =
                player.currentPosition.coerceAtLeast(0L)

            duration =
                player.duration.coerceAtLeast(0L)

            kotlinx.coroutines.delay(300)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {

        // HEADER

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelColor)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "VISION STUDIO",
                    color = GoldColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    "Professional Video Editor",
                    color = MutedColor,
                    fontSize = 11.sp
                )
            }

            Icon(
                Icons.Default.Movie,
                contentDescription = null,
                tint = GoldColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(Modifier.height(14.dp))

        SectionTitle("معاينة الفيديو")

        // VIDEO PREVIEW

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(220.dp)
                .background(
                    Color.Black,
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (clips.isNotEmpty()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            useController = true
                        }
                    },
                    update = { view ->
                        view.player = player
                    }
                )
            } else {
                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = GoldColor,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "استورد فيديو لبدء التحرير",
                        color = WhiteColor
                    )
                }
            }
        }

        Text(
            videoTitle,
            color = MutedColor,
            fontSize = 12.sp,
            modifier = Modifier.padding(16.dp)
        )

        // PLAYBACK CONTROLS

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (clips.isNotEmpty()) {
                        player.seekTo(0L)
                    }
                },
                enabled = clips.isNotEmpty()
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "البداية",
                    tint = WhiteColor
                )
            }

            IconButton(
                onClick = {
                    if (clips.isNotEmpty()) {
                        if (player.isPlaying) {
                            player.pause()
                        } else {
                            player.play()
                        }
                    }
                },
                enabled = clips.isNotEmpty(),
                modifier = Modifier
                    .background(
                        GoldColor,
                        RoundedCornerShape(50)
                    )
                    .size(52.dp)
            ) {
                Icon(
                    if (isPlaying) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = "تشغيل",
                    tint = Color.Black,
                    modifier = Modifier.size(30.dp)
                )
            }

            IconButton(
                onClick = {
                    if (clips.isNotEmpty()) {
                        player.seekTo(player.duration)
                    }
                },
                enabled = clips.isNotEmpty()
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "النهاية",
                    tint = WhiteColor
                )
            }
        }

        Text(
            "${formatTime(currentPosition)} / ${formatTime(duration)}",
            color = MutedColor,
            fontSize = 12.sp,
            modifier = Modifier.align(
                Alignment.CenterHorizontally
            )
        )

        Spacer(Modifier.height(18.dp))

        SectionTitle("أدوات التحرير")

        val tabs = listOf(
            "الميديا",
            "تحرير",
            "الصوت",
            "النص",
            "الألوان"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {
            tabs.forEach { tab ->
                val active = selectedTab == tab

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (active) GoldColor
                            else PanelColor,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            selectedTab = tab
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        tab,
                        color = if (active) {
                            Color.Black
                        } else {
                            WhiteColor
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        when (selectedTab) {

            "الميديا" -> {
                Panel {
                    Text(
                        "مكتبة الوسائط",
                        color = WhiteColor,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    GoldButton(
                        "استيراد فيديو من الهاتف"
                    ) {
                        videoPicker.launch(
                            arrayOf("video/*")
                        )
                    }

                    Text(
                        "عدد المقاطع: ${clips.size}",
                        color = MutedColor,
                        fontSize = 12.sp
                    )
                }
            }

            "تحرير" -> {
                Panel {
                    Text(
                        "أدوات تحرير المقطع",
                        color = WhiteColor,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        selectedClip?.name
                            ?: "لم يتم تحديد مقطع",
                        color = GoldColor,
                        fontSize = 12.sp
                    )

                    if (selectedClip != null) {
                        Text(
                            "المدة: ${
                                formatTime(
                                    selectedClip.durationMs
                                )
                            }",
                            color = MutedColor,
                            fontSize = 11.sp
                        )
                    }

                    GoldButton(
                        "تقسيم عند موضع التشغيل"
                    ) {
                        splitSelectedClip()
                    }

                    OutlinedButton(
                        onClick = {
                            deleteSelectedClip()
                        },
                        enabled = selectedClip != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = RedColor
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text("حذف المقطع المحدد")
                    }
                }
            }

            "الصوت" -> {
                Panel {
                    Text(
                        "استوديو الصوت",
                        color = WhiteColor,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "تسجيل الصوت ومزج المسارات قيد التطوير.",
                        color = MutedColor,
                        fontSize = 12.sp
                    )
                }
            }

            "النص" -> {
                Panel {
                    Text(
                        "النصوص والعناوين",
                        color = WhiteColor
                    )

                    Text(
                        "أدوات النصوص قيد التطوير.",
                        color = MutedColor,
                        fontSize = 12.sp
                    )
                }
            }

            "الألوان" -> {
                Panel {
                    Text(
                        "تصحيح الألوان",
                        color = WhiteColor
                    )

                    Text(
                        "أدوات الألوان قيد التطوير.",
                        color = MutedColor,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // TIMELINE

        SectionTitle("Timeline • الخط الزمني")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .background(
                    PanelColor,
                    RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "V1",
                    color = GoldColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    "مسار الفيديو",
                    color = WhiteColor,
                    fontSize = 12.sp
                )

                Spacer(Modifier.weight(1f))

                Text(
                    "${clips.size} مقطع",
                    color = MutedColor,
                    fontSize = 10.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            if (clips.isEmpty()) {
                Text(
                    "لا توجد مقاطع. استورد فيديو لإضافته هنا.",
                    color = MutedColor,
                    fontSize = 11.sp
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState()
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(7.dp)
                ) {
                    clips.forEachIndexed { index, clip ->
                        val active =
                            clip.id == selectedClipId

                        Column(
                            modifier = Modifier
                                .width(145.dp)
                                .background(
                                    if (active) {
                                        Color(0xFF59451F)
                                    } else {
                                        BackgroundColor
                                    },
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedClipId = clip.id
                                    player.seekTo(index, 0L)
                                    player.pause()
                                }
                                .padding(9.dp)
                        ) {
                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = if (active) {
                                        GoldColor
                                    } else {
                                        MutedColor
                                    },
                                    modifier = Modifier.size(17.dp)
                                )

                                Spacer(Modifier.width(5.dp))

                                Text(
                                    "Clip ${index + 1}",
                                    color = if (active) {
                                        GoldColor
                                    } else {
                                        WhiteColor
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                formatTime(clip.durationMs),
                                color = WhiteColor,
                                fontSize = 12.sp
                            )

                            Text(
                                clip.name,
                                color = MutedColor,
                                fontSize = 9.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "A1",
                    color = GreenColor,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    "مسار الصوت",
                    color = MutedColor,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "T1",
                    color = Color(0xFF69A9FF),
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    "مسار النصوص",
                    color = MutedColor,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                statusMessage,
                color = GreenColor,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                "إضافة فيديو",
                Icons.Default.Add,
                Modifier.weight(1f)
            ) {
                videoPicker.launch(arrayOf("video/*"))
            }

            ActionButton(
                "تحرير",
                Icons.Default.ContentCut,
                Modifier.weight(1f)
            ) {
                selectedTab = "تحرير"
            }

            ActionButton(
                "الميديا",
                Icons.Default.Folder,
                Modifier.weight(1f)
            ) {
                selectedTab = "الميديا"
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            "VISION STUDIO • TIMELINE V1",
            color = MutedColor,
            fontSize = 10.sp,
            modifier = Modifier.align(
                Alignment.CenterHorizontally
            )
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        title,
        color = GoldColor,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(
            start = 16.dp,
            end = 16.dp,
            bottom = 10.dp
        )
    )
}

@Composable
fun Panel(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .background(
                PanelColor,
                RoundedCornerShape(12.dp)
            )
            .padding(14.dp),
        content = content
    )
}

@Composable
fun GoldButton(
    title: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GoldColor,
            contentColor = Color.Black
        )
    ) {
        Text(
            title,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(
                PanelColor,
                RoundedCornerShape(10.dp)
            )
            .clickable {
                onClick()
            }
            .padding(vertical = 12.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = GoldColor
        )

        Spacer(Modifier.height(5.dp))

        Text(
            title,
            color = WhiteColor,
            fontSize = 10.sp
        )
    }
}

fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return String.format(
        Locale.getDefault(),
        "%02d:%02d",
        minutes,
        seconds
    )
}
