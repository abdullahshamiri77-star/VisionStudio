
package com.visionstudio.app

import java.util.UUID

// أنواع المسارات داخل الخط الزمني
enum class TimelineTrackType {
    VIDEO,
    AUDIO,
    TEXT,
    EFFECT
}

// إعدادات المشروع
data class TimelineProjectSettings(
    val width: Int = 1920,
    val height: Int = 1080,
    val frameRate: Int = 30,
    val audioSampleRate: Int = 48000
)

// المسار داخل المشروع
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

// الخصائص المشتركة للمقاطع
interface TimelineItem {
    val id: String
    val trackId: String
    val positionMs: Long
    val durationMs: Long

    val endMs: Long
        get() = positionMs + durationMs
}

// مقطع فيديو
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
) : TimelineItem

// مقطع صوتي
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
) : TimelineItem

// مقطع عنوان أو نص
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
) : TimelineItem

// أنواع الانتقالات
enum class TransitionType {
    DISSOLVE,
    FADE,
    WIPE,
    SLIDE,
    ZOOM
}

// الانتقالات بين المقاطع
data class TimelineTransition(
    val id: String = UUID.randomUUID().toString(),
    val fromClipId: String,
    val toClipId: String,
    val type: TransitionType = TransitionType.DISSOLVE,
    val durationMs: Long = 500L
)

// أنواع خصائص الحركة
enum class KeyframeProperty {
    POSITION_X,
    POSITION_Y,
    SCALE,
    ROTATION,
    OPACITY,
    VOLUME
}

// نقطة تحكم للحركة
data class TimelineKeyframe(
    val id: String = UUID.randomUUID().toString(),
    val clipId: String,
    val timeMs: Long,
    val property: KeyframeProperty,
    val value: Float
)

// حالة الخط الزمني
data class TimelineState(
    val tracks: List<TimelineTrack> = emptyList(),
    val videoClips: List<TimelineVideoClip> = emptyList(),
    val audioClips: List<TimelineAudioClip> = emptyList(),
    val textClips: List<TimelineTextClip> = emptyList(),
    val transitions: List<TimelineTransition> = emptyList(),
    val keyframes: List<TimelineKeyframe> = emptyList(),
    val playheadMs: Long = 0L,
    val settings: TimelineProjectSettings = TimelineProjectSettings()
) {
    val durationMs: Long
        get() = maxOf(
            videoClips.maxOfOrNull { it.endMs } ?: 0L,
            audioClips.maxOfOrNull { it.endMs } ?: 0L,
            textClips.maxOfOrNull { it.endMs } ?: 0L
        )
}
