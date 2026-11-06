
package com.visionstudio.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import java.io.File

private val Bg = Color(0xFF101114)
private val Panel = Color(0xFF1B1A1F)
private val Gold = Color(0xFFFFC247)
private val Muted = Color(0xFF92909A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VisionApp()
        }
    }
}

@Composable
fun VisionApp() {
    val context = LocalContext.current

    var selected by remember { mutableStateOf("Edit") }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var playhead by remember { mutableFloatStateOf(0f) }

    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var savedRecording by remember { mutableStateOf<File?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var audioMessage by remember { mutableStateOf("جاهز للتسجيل") }

    var audioPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val player = remember(context) {
        ExoPlayer.Builder(context).build()
    }

    fun startRecording() {
        try {
            val musicFolder = context.getExternalFilesDir(
                Environment.DIRECTORY_MUSIC
            ) ?: context.cacheDir

            if (!musicFolder.exists()) {
                musicFolder.mkdirs()
            }

            val file = File(
                musicFolder,
                "VisionAudio_${System.currentTimeMillis()}.m4a"
            )

            val newRecorder = MediaRecorder()

            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(
                MediaRecorder.OutputFormat.MPEG_4
            )
            newRecorder.setAudioEncoder(
                MediaRecorder.AudioEncoder.AAC
            )
            newRecorder.setAudioEncodingBitRate(128000)
            newRecorder.setAudioSamplingRate(44100)
            newRecorder.setOutputFile(file.absolutePath)

            newRecorder.prepare()
            newRecorder.start()

            recorder = newRecorder
            recordingFile = file
            isRecording = true
            isPaused = false
            audioMessage = "جاري التسجيل..."
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            isRecording = false
            isPaused = false
            audioMessage = "تعذر بدء التسجيل: ${e.message}"
        }
    }

    val microphonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecording()
        } else {
            audioMessage = "يجب السماح باستخدام الميكروفون"
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            mediaUri = uri
            currentPosition = 0L
            playhead = 0f
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
            try {
                recorder?.release()
            } catch (_: Exception) {
            }
            try {
                audioPlayer?.release()
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(mediaUri) {
        if (mediaUri != null) {
            player.setMediaItem(MediaItem.fromUri(mediaUri!!))
            player.prepare()
            player.pause()
            isPlaying = false
            currentPosition = 0L
            playhead = 0f
        } else {
            player.stop()
            isPlaying = false
        }
    }

    LaunchedEffect(player, mediaUri) {
        while (true) {
            if (mediaUri != null) {
                currentPosition = player.currentPosition.coerceAtLeast(0L)
                duration = player.duration.coerceAtLeast(0L)
                isPlaying = player.isPlaying

                playhead = if (duration > 0L) {
                    currentPosition.toFloat() / duration.toFloat()
                } else {
                    0f
                }
            }
            delay(250)
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            background = Bg,
            surface = Panel
        )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Bg)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
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
                        "PRO VIDEO EDITOR • 0.2",
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
                            onClick = {
                                selected = item
                                if (item == "Media") {
                                    videoPicker.launch(arrayOf("video/*"))
                                }
                            },
                            label = { Text(item) }
                        )
                    }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(if (selected == "Audio") 0.75f else 1f)
                    .background(Color.Black, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (mediaUri != null) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { viewContext ->
                            PlayerView(viewContext)
                        },
                        update = { view ->
                            view.player = player
                        }
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            videoPicker.launch(arrayOf("video/*"))
                        }
                    ) {
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
                }

                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
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

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (mediaUri != null) player.seekTo(0L)
                    }
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        if (mediaUri != null) {
                            if (player.isPlaying) {
                                player.pause()
                            } else {
                                player.play()
                            }
                            isPlaying = player.isPlaying
                        } else {
                            videoPicker.launch(arrayOf("video/*"))
                        }
                    }
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause
                        else Icons.Default.PlayArrow,
                        contentDescription = "Play or Pause",
                        tint = Gold,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (mediaUri != null && duration > 0L) {
                            player.seekTo(duration)
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    formatTime(currentPosition),
                    color = Gold,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    " / ${formatTime(duration)}",
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
                value = playhead.coerceIn(0f, 1f),
                onValueChange = { value ->
                    playhead = value
                    if (duration > 0L) {
                        player.seekTo((duration * value).toLong())
                    }
                },
                enabled = mediaUri != null && duration > 0L,
                colors = SliderDefaults.colors(
                    thumbColor = Gold,
                    activeTrackColor = Gold,
                    inactiveTrackColor = Panel
                )
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Panel, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Track("T", "نص / عناوين", Color(0xFF9B6BCE))
                Track("V", "فيديو / صور", Color(0xFF4C91D8))
                Track("A", "صوت / موسيقى", Color(0xFF38A78B))
            }

            if (selected == "Audio") {
                Spacer(Modifier.height(8.dp))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Panel, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Mic,
                            tint = Gold,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "استوديو التسجيل الصوتي",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        audioMessage,
                        color = if (isRecording) Gold else Muted,
                        fontSize = 12.sp
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            enabled = !isRecording,
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (granted) {
                                    startRecording()
                                } else {
                                    microphonePermission.launch(
                                        Manifest.permission.RECORD_AUDIO
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold,
                                contentColor = Bg
                            )
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null
                            )
                            Text("تسجيل")
                        }

                        Button(
                            enabled = isRecording,
                            onClick = {
                                try {
                                    if (isPaused) {
                                        recorder?.resume()
                                        isPaused = false
                                        audioMessage = "جاري التسجيل..."
                                    } else {
                                        recorder?.pause()
                                        isPaused = true
                                        audioMessage = "التسجيل متوقف مؤقتاً"
                                    }
                                } catch (e: Exception) {
                                    audioMessage = "تعذر تغيير حالة التسجيل"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF38343D)
                            )
                        ) {
                            Icon(
                                if (isPaused) Icons.Default.PlayArrow
                                else Icons.Default.Pause,
                                contentDescription = null
                            )
                            Text(if (isPaused) "استئناف" else "إيقاف مؤقت")
                        }

                        Button(
                            enabled = isRecording,
                            onClick = {
                                try {
                                    recorder?.stop()
                                    savedRecording = recordingFile
                                    audioMessage = "تم حفظ التسجيل بنجاح"
                                } catch (e: Exception) {
                                    audioMessage = "لم يكتمل التسجيل"
                                    recordingFile?.delete()
                                } finally {
                                    recorder?.release()
                                    recorder = null
                                    recordingFile = null
                                    isRecording = false
                                    isPaused = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B3030)
                            )
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = null
                            )
                            Text("إنهاء")
                        }
                    }

                    if (savedRecording != null) {
                        Text(
                            "آخر تسجيل: ${savedRecording!!.name}",
                            color = Muted,
                            fontSize = 10.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                enabled = !isAudioPlaying,
                                onClick = {
                                    try {
                                        audioPlayer?.release()
                                        audioPlayer = MediaPlayer().apply {
                                            setDataSource(
                                                savedRecording!!.absolutePath
                                            )
                                            prepare()
                                            setOnCompletionListener {
                                                it.release()
                                                audioPlayer = null
                                                isAudioPlaying = false
                                            }
                                            start()
                                        }
                                        isAudioPlaying = true
                                        audioMessage = "يتم تشغيل التسجيل"
                                    } catch (e: Exception) {
                                        audioMessage = "تعذر تشغيل التسجيل"
                                        isAudioPlaying = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF38343D)
                                )
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null
                                )
                                Text("استماع")
                            }

                            OutlinedButton(
                                enabled = isAudioPlaying,
                                onClick = {
                                    try {
                                        audioPlayer?.stop()
                                        audioPlayer?.release()
                                    } catch (_: Exception) {
                                    }
                                    audioPlayer = null
                                    isAudioPlaying = false
                                    audioMessage = "تم إيقاف التشغيل"
                                }
                            ) {
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = null
                                )
                                Text("إيقاف")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomTool("إضافة", Icons.Default.Add) {
                    videoPicker.launch(arrayOf("video/*"))
                }
                BottomTool("قص", Icons.Default.ContentCut) {
                    if (mediaUri != null) {
                        player.pause()
                        isPlaying = false
                    }
                }
                BottomTool("طبقات", Icons.Default.Layers) {
                    selected = "Media"
                }
                BottomTool("تصدير", Icons.Default.Outbox) {
                    selected = "Export"
                }
            }
        }
    }
}

@Composable
fun Track(tag: String, title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(28.dp)
                .height(28.dp)
                .background(
                    color.copy(alpha = .18f),
                    RoundedCornerShape(6.dp)
                ),
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
            Modifier
                .weight(1f)
                .height(28.dp)
                .background(
                    color.copy(alpha = .22f),
                    RoundedCornerShape(6.dp)
                )
        ) {
            Box(
                Modifier
                    .fillMaxWidth(.58f)
                    .fillMaxHeight()
                    .background(
                        color.copy(alpha = .7f),
                        RoundedCornerShape(6.dp)
                    )
            )
        }
    }
}

@Composable
fun BottomTool(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Gold
        )
        Text(label, color = Muted, fontSize = 10.sp)
    }
}

fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}
