package com.visionstudio.app

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BackgroundColor = Color(0xFF101114)
private val PanelColor = Color(0xFF1B1C21)
private val GoldColor = Color(0xFFFFC247)
private val WhiteColor = Color(0xFFF5F5F5)
private val MutedColor = Color(0xFF9999A3)
private val GreenColor = Color(0xFF48C78E)
private val RedColor = Color(0xFFFF6262)

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
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoTitle by remember { mutableStateOf("لم يتم اختيار فيديو") }
    var isPlaying by remember { mutableStateOf(false) }
    var duration by remember { mutableLongStateOf(0L) }
    var currentPosition by remember { mutableLongStateOf(0L) }

    var recording by remember { mutableStateOf(false) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var audioPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var recordingMessage by remember { mutableStateOf("جاهز لتسجيل الصوت") }

    val mediaList = remember { mutableStateListOf<String>() }

    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            videoUri = uri
            videoTitle = uri.lastPathSegment ?: "Video"
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.pause()
            isPlaying = false

            if (!mediaList.contains(videoTitle)) {
                mediaList.add(videoTitle)
            }
        }
    }

    val audioPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                recordingMessage = "تم السماح باستخدام الميكروفون"
            } else {
                recordingMessage = "يجب السماح باستخدام الميكروفون للتسجيل"
            }
        }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                duration = player.duration.coerceAtLeast(0L)
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
            try {
                audioPlayer?.release()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(isPlaying, videoUri) {
        while (true) {
            currentPosition = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.coerceAtLeast(0L)
            kotlinx.coroutines.delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 18.dp)
    ) {

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PanelColor)
                .padding(horizontal = 16.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "VISION STUDIO",
                    color = GoldColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Professional Video Editor",
                    color = MutedColor,
                    fontSize = 11.sp
                )
            }

            Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = GoldColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preview title
        SectionTitle("معاينة الفيديو")

        // Video preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(220.dp)
                .background(Color.Black, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (videoUri != null) {
                AndroidViewAndroidView(
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
    update = { view ->
        view.player = player
    }
)
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = GoldColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "اختر فيديو لبدء العمل",
                        color = WhiteColor,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "مساحة معاينة المشروع",
                        color = MutedColor,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = videoTitle,
            color = MutedColor,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Playback controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    player.seekTo(0)
                }
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "البداية",
                    tint = WhiteColor
                )
            }

            IconButton(
                onClick = {
                    if (videoUri != null) {
                        if (player.isPlaying) {
                            player.pause()
                        } else {
                            player.play()
                        }
                    }
                },
                modifier = Modifier
                    .background(GoldColor, RoundedCornerShape(50))
                    .size(52.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) {
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
                    player.seekTo(duration)
                }
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "النهاية",
                    tint = WhiteColor
                )
            }
        }

        Text(
            text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
            color = MutedColor,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main tabs
        SectionTitle("أدوات التحرير")

        val tabs = listOf(
            "الميديا",
            "تحرير",
            "النص",
            "الصوت",
            "التأثيرات",
            "الألوان"
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            tabs.forEach { tab ->
                val active = selectedTab == tab

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (active) GoldColor else PanelColor,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            selectedTab = tab
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab,
                        color = if (active) Color.Black else WhiteColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        // Selected panel
        when (selectedTab) {

            "الميديا" -> {
                MediaPanel(
                    mediaList = mediaList,
                    onImport = {
                        videoPicker.launch(arrayOf("video/*"))
                    }
                )
            }

            "تحرير" -> {
                EditingPanel(
                    onImport = {
                        videoPicker.launch(arrayOf("video/*"))
                    }
                )
            }

            "النص" -> {
                ComingSoonPanel(
                    title = "أدوات النصوص",
                    description = "سيتم إضافة النصوص والعناوين والتحكم بالخط والحجم واللون."
                )
            }

            "الصوت" -> {
                AudioPanel(
                    recording = recording,
                    message = recordingMessage,
                    isAudioPlaying = isAudioPlaying,
                    hasRecording = recordedFile != null,
                    onStartRecording = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (granted) {
                            try {
                                val folder = context.getExternalFilesDir("Recordings")
                                    ?: context.filesDir

                                if (!folder.exists()) {
                                    folder.mkdirs()
                                }

                                val file = File(
                                    folder,
                                    "VisionAudio_${System.currentTimeMillis()}.m4a"
                                )

                                val recorder = MediaRecorder()

                                recorder.setAudioSource(
                                    MediaRecorder.AudioSource.MIC
                                )
                                recorder.setOutputFormat(
                                    MediaRecorder.OutputFormat.MPEG_4
                                )
                                recorder.setAudioEncoder(
                                    MediaRecorder.AudioEncoder.AAC
                                )
                                recorder.setAudioSamplingRate(44100)
                                recorder.setAudioEncodingBitRate(128000)
                                recorder.setOutputFile(file.absolutePath)
                                recorder.prepare()
                                recorder.start()

                                audioRecorder = recorder
                                recordedFile = file
                                recording = true
                                isAudioPlaying = false
                                recordingMessage = "جاري تسجيل الصوت..."
                            } catch (e: Exception) {
                                recordingMessage =
                                    "تعذر بدء التسجيل: ${e.message}"
                            }
                        } else {
                            audioPermissionLauncher.launch(
                                Manifest.permission.RECORD_AUDIO
                            )
                        }
                    },
                    onStopRecording = {
                        try {
                            audioRecorder?.stop()
                            audioRecorder?.release()
                            audioRecorder = null
                            recording = false
                            recordingMessage = "تم حفظ التسجيل بنجاح"
                        } catch (e: Exception) {
                            recording = false
                            recordingMessage = "تعذر حفظ التسجيل"
                        }
                    },
                    onPlayRecording = {
                        val file = recordedFile

                        if (file != null && file.exists()) {
                            try {
                                audioPlayer?.release()

                                audioPlayer = MediaPlayer().apply {
                                    setDataSource(file.absolutePath)
                                    prepare()
                                    start()
                                    setOnCompletionListener {
                                        isAudioPlaying = false
                                    }
                                }

                                isAudioPlaying = true
                                recordingMessage = "جاري تشغيل التسجيل"
                            } catch (e: Exception) {
                                recordingMessage = "تعذر تشغيل التسجيل"
                                isAudioPlaying = false
                            }
                        }
                    },
                    onStopPlayback = {
                        try {
                            audioPlayer?.stop()
                            audioPlayer?.release()
                            audioPlayer = null
                        } catch (_: Exception) {
                        }

                        isAudioPlaying = false
                        recordingMessage = "تم إيقاف تشغيل التسجيل"
                    }
                )
            }

            "التأثيرات" -> {
                ComingSoonPanel(
                    title = "المؤثرات البصرية",
                    description = "سيتم إضافة المؤثرات والانتقالات والتحكم بطبقات الفيديو."
                )
            }

            "الألوان" -> {
                ComingSoonPanel(
                    title = "تصحيح الألوان",
                    description = "سيتم إضافة أدوات السطوع والتباين والتشبع ودرجات الألوان."
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Timeline
        SectionTitle("الخط الزمني للمشروع")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .background(PanelColor, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            TrackRow("V1", "مسار الفيديو", GoldColor)
            Spacer(modifier = Modifier.height(8.dp))
            TrackRow("A1", "مسار الصوت", GreenColor)
            Spacer(modifier = Modifier.height(8.dp))
            TrackRow("T1", "مسار النصوص", Color(0xFF69A9FF))

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "الخط الزمني الحالي للعرض والتنظيم الأولي. " +
                        "تحرير المقاطع الفعلي قيد التطوير.",
                color = MutedColor,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Bottom actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionButton(
                title = "إضافة فيديو",
                icon = Icons.Default.Add,
                modifier = Modifier.weight(1f),
                onClick = {
                    videoPicker.launch(arrayOf("video/*"))
                }
            )

            ActionButton(
                title = "الصوت",
                icon = Icons.Default.Mic,
                modifier = Modifier.weight(1f),
                onClick = {
                    selectedTab = "الصوت"
                }
            )

            ActionButton(
                title = "الميديا",
                icon = Icons.Default.Folder,
                modifier = Modifier.weight(1f),
                onClick = {
                    selectedTab = "الميديا"
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "VISION STUDIO  •  VERSION 0.2",
            color = MutedColor,
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

// Recorder reference
private var audioRecorder: MediaRecorder? = null

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
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
fun MediaPanel(
    mediaList: List<String>,
    onImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .background(PanelColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "مكتبة الوسائط",
            color = WhiteColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onImport,
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldColor,
                contentColor = Color.Black
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.VideoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("استيراد فيديو من الهاتف")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (mediaList.isEmpty()) {
            Text(
                text = "لا توجد وسائط مستوردة حتى الآن",
                color = MutedColor,
                fontSize = 12.sp
            )
        } else {
            mediaList.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Movie,
                        contentDescription = null,
                        tint = GoldColor
                    )

                    Spacer(modifier = Modifier.width(9.dp))

                    Text(
                        text = item,
                        color = WhiteColor,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EditingPanel(
    onImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .background(PanelColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "أدوات تحرير الفيديو",
            color = WhiteColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onImport,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldColor,
                contentColor = Color.Black
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("إضافة فيديو للمشروع")
        }

        Spacer(modifier = Modifier.height(10.dp))

        ToolInfoRow(
            icon = Icons.Default.ContentCut,
            title = "قص وتقسيم المقاطع",
            status = "قيد التطوير"
        )

        ToolInfoRow(
            icon = Icons.Default.Layers,
            title = "تحريك وترتيب الطبقات",
            status = "قيد التطوير"
        )

        ToolInfoRow(
            icon = Icons.Default.Tune,
            title = "سرعة الفيديو",
            status = "قيد التطوير"
        )
    }
}

@Composable
fun AudioPanel(
    recording: Boolean,
    message: String,
    isAudioPlaying: Boolean,
    hasRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onPlayRecording: () -> Unit,
    onStopPlayback: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .background(PanelColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "استوديو الصوت",
            color = WhiteColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = message,
            color = if (recording) RedColor else MutedColor,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (recording) onStopRecording() else onStartRecording()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (recording) RedColor else GoldColor,
                contentColor = if (recording) WhiteColor else Color.Black
            )
        ) {
            Icon(
                imageVector = if (recording) {
                    Icons.Default.Stop
                } else {
                    Icons.Default.Mic
                },
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                if (recording) "إيقاف التسجيل" else "بدء تسجيل الصوت"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onPlayRecording,
            enabled = hasRecording && !recording,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = GoldColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("تشغيل التسجيل")
        }

        OutlinedButton(
            onClick = onStopPlayback,
            enabled = isAudioPlaying,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Stop,
                contentDescription = null,
                tint = GoldColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("إيقاف تشغيل التسجيل")
        }
    }
}

@Composable
fun ComingSoonPanel(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .background(PanelColor, RoundedCornerShape(12.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Construction,
            contentDescription = null,
            tint = GoldColor,
            modifier = Modifier.size(35.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            color = WhiteColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            color = MutedColor,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "سيتم تنفيذ هذه الأداة في مرحلة التطوير القادمة",
            color = GoldColor,
            fontSize = 11.sp
        )
    }
}

@Composable
fun TrackRow(
    code: String,
    title: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundColor, RoundedCornerShape(7.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = code,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            color = WhiteColor,
            fontSize = 12.sp
        )
    }
}

@Composable
fun ToolInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    status: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GoldColor
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = title,
            color = WhiteColor,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = status,
            color = MutedColor,
            fontSize = 10.sp
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
            .background(PanelColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GoldColor
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = title,
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
