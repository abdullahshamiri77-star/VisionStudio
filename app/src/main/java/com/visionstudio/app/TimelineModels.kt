package com.visionstudio.app

import java.math.BigInteger
import java.util.UUID

// =====================================================
// VISION STUDIO - TIMELINE CORE MODELS
// =====================================================

// أنواع المسارات
enum class TimelineTrackType {
    VIDEO,
    AUDIO,
    TEXT,
    EFFECT
}

// =====================================================
// إعدادات المشروع والتوقيت
// =====================================================

data class TimelineProjectSettings(
    val width: Int = 1920,
    val height: Int = 1080,
    val frameRate: Int = 30,
    val audioSampleRate: Int = 48000
) {
    init {
        require(width > 0 && height > 0)
        require(frameRate > 0)
        require(audioSampleRate > 0)
    }

    // تحويل إطار إلى مللي ثانية
    fun frameToMs(frame: Long): Long {
        return (frame * 1000L) / frameRate
    }

    // تحويل مللي ثانية إلى إطار
    fun msToFrame(timeMs: Long): Long {
        return (timeMs * frameRate) / 1000L
    }

    fun durationMsToFramesCeil(durationMs: Long): Long {
        require(durationMs >= 0L)
        val numerator = BigInteger.valueOf(durationMs)
            .multiply(BigInteger.valueOf(frameRate.toLong()))
        val denominator = BigInteger.valueOf(1000L)
        return numerator.add(denominator).subtract(BigInteger.ONE)
            .divide(denominator)
            .longValueExact()
    }

    // تقريب الوقت إلى أقرب إطار
    fun snapToFrame(timeMs: Long): Long {
        val frame = ((timeMs * frameRate + 500L) / 1000L)
        return frameToMs(frame)
    }
}

// =====================================================
// المسارات
// =====================================================

data class TimelineTrack(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: TimelineTrackType,
    val index: Int,
    val isVisible: Boolean = true,
    val isMuted: Boolean = false,
    val isLocked: Boolean = false,
    val volume: Float = 1f
)

// =====================================================
// الواجهة المشتركة للمقاطع
// =====================================================

interface TimelineItem {
    val id: String
    val trackId: String
    val positionMs: Long
    val durationMs: Long

    val endMs: Long
        get() = positionMs + durationMs
}

// =====================================================
// مقاطع الفيديو
// =====================================================

data class TimelineVideoClip(
    override val id: String = UUID.randomUUID().toString(),
    override val trackId: String,
    val sourceUri: String,
    val name: String,
    override val positionMs: Long,
    override val durationMs: Long,
    val sourceStartMs: Long = 0L,
    val sourceEndMs: Long = durationMs,
    val speed: Float = 1f,
    val volume: Float = 1f,
    val isEnabled: Boolean = true
) : TimelineItem {

    init {
        require(positionMs >= 0L)
        require(durationMs > 0L)
        require(sourceStartMs >= 0L)
        require(sourceEndMs >= sourceStartMs)
        require(speed > 0f)
    }
}

// =====================================================
// مقاطع الصوت
// =====================================================

data class TimelineAudioClip(
    override val id: String = UUID.randomUUID().toString(),
    override val trackId: String,
    val sourceUri: String,
    val name: String,
    override val positionMs: Long,
    override val durationMs: Long,
    val sourceStartMs: Long = 0L,
    val sourceEndMs: Long = durationMs,
    val volume: Float = 1f,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val isEnabled: Boolean = true
) : TimelineItem {

    init {
        require(positionMs >= 0L)
        require(durationMs > 0L)
        require(sourceStartMs >= 0L)
        require(sourceEndMs >= sourceStartMs)
        require(fadeInMs >= 0L)
        require(fadeOutMs >= 0L)
    }
}

// =====================================================
// مقاطع النص والعناوين
// =====================================================

data class TimelineTextClip(
    override val id: String = UUID.randomUUID().toString(),
    override val trackId: String,
    val text: String,
    override val positionMs: Long,
    override val durationMs: Long,
    val fontSize: Float = 48f,
    val color: Long = 0xFFFFFFFF,
    val positionX: Float = 0.5f,
    val positionY: Float = 0.5f
) : TimelineItem {

    init {
        require(positionMs >= 0L)
        require(durationMs > 0L)
        require(fontSize > 0f)
    }
}

// =====================================================
// الانتقالات
// =====================================================

enum class TransitionType {
    DISSOLVE,
    FADE,
    WIPE,
    SLIDE,
    ZOOM
}

data class TimelineTransition(
    val id: String = UUID.randomUUID().toString(),
    val fromClipId: String,
    val toClipId: String,
    val type: TransitionType = TransitionType.DISSOLVE,
    val durationMs: Long = 500L
) {
    init {
        require(durationMs > 0L)
    }
}

// =====================================================
// Keyframes - التحكم بالحركة
// =====================================================

enum class KeyframeProperty {
    POSITION_X,
    POSITION_Y,
    SCALE,
    ROTATION,
    OPACITY,
    VOLUME
}

data class TimelineKeyframe(
    val id: String = UUID.randomUUID().toString(),
    val clipId: String,
    val timeMs: Long,
    val property: KeyframeProperty,
    val value: Float
) {
    init {
        require(timeMs >= 0L)
    }
}

// =====================================================
// حالة الخط الزمني
// =====================================================

data class TimelineState(
    val tracks: List<TimelineTrack> = emptyList(),
    val videoClips: List<TimelineVideoClip> = emptyList(),
    val audioClips: List<TimelineAudioClip> = emptyList(),
    val textClips: List<TimelineTextClip> = emptyList(),
    val transitions: List<TimelineTransition> = emptyList(),
    val keyframes: List<TimelineKeyframe> = emptyList(),
    val playheadMs: Long = 0L,
    val settings: TimelineProjectSettings =
        TimelineProjectSettings()
) {

    val durationMs: Long
        get() = maxOf(
            videoClips.maxOfOrNull { it.endMs } ?: 0L,
            audioClips.maxOfOrNull { it.endMs } ?: 0L,
            textClips.maxOfOrNull { it.endMs } ?: 0L
        )

    val durationFrames: Long
        get() = settings.msToFrame(durationMs)

    val playheadFrame: Long
        get() = settings.msToFrame(playheadMs)
}
