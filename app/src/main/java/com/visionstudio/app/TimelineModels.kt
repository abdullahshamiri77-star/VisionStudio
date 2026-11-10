
package com.visionstudio.app

import java.math.BigInteger
import java.util.UUID

enum class TimelineTrackType {
    VIDEO,
    AUDIO,
    TEXT,
    EFFECT
}

data class TimelineProjectSettings(
    val width: Int = 1920,
    val height: Int = 1080,
    val frameRate: Int = 30,
    val audioSampleRate: Int = 48000
) {
    init {
        require(width > 0 && height > 0) {
            "Project dimensions must be positive"
        }
        require(frameRate in SUPPORTED_FPS) {
            "FPS must be 24, 30, or 60"
        }
        require(audioSampleRate > 0) {
            "Audio sample rate must be positive"
        }
    }

    /**
     * Converts a frame index to the nearest millisecond.
     * Rounding mode: half-up.
     */
    fun frameToMs(frame: Long): Long {
        require(frame >= 0L) {
            "Frame must not be negative"
        }

        val numerator = BigInteger.valueOf(frame)
            .multiply(THOUSAND)

        return numerator
            .add(BigInteger.valueOf(frameRate / 2L))
            .divide(BigInteger.valueOf(frameRate.toLong()))
            .longValueExact()
    }

    /**
     * Converts milliseconds to the nearest frame.
     * Rounding mode: half-up.
     */
    fun msToFrame(timeMs: Long): Long {
        require(timeMs >= 0L) {
            "Time must not be negative"
        }

        val numerator = BigInteger.valueOf(timeMs)
            .multiply(BigInteger.valueOf(frameRate.toLong()))

        return numerator
            .add(BigInteger.valueOf(500L))
            .divide(THOUSAND)
            .longValueExact()
    }

    /**
     * Calculates the number of frames required
     * to cover the complete duration.
     *
     * Uses ceiling instead of nearest-frame rounding.
     */
    fun durationMsToFramesCeil(durationMs: Long): Long {
        require(durationMs >= 0L) {
            "Duration must not be negative"
        }

        val numerator = BigInteger.valueOf(durationMs)
            .multiply(BigInteger.valueOf(frameRate.toLong()))

        return numerator
            .add(THOUSAND)
            .subtract(BigInteger.ONE)
            .divide(THOUSAND)
            .longValueExact()
    }

    /**
     * Snaps a millisecond position to the nearest frame boundary.
     */
    fun snapToFrame(timeMs: Long): Long {
        return frameToMs(msToFrame(timeMs))
    }

    companion object {
        val SUPPORTED_FPS = setOf(24, 30, 60)

        private val THOUSAND = BigInteger.valueOf(1000L)
    }
}

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

interface TimelineItem {
    val id: String
    val trackId: String
    val positionMs: Long
    val durationMs: Long

    /**
     * Uses checked arithmetic to prevent silent overflow.
     */
    val endMs: Long
        get() = Math.addExact(positionMs, durationMs)
}

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
    val isEnabled: Boolean = true,

    /**
     * Retained for compatibility with existing code.
     * Audible behavior depends on the audio renderer.
     */
    val startGain: Float = 0f
) : TimelineItem

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
)

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
)

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

    /**
     * Project duration is determined by the latest clip endpoint.
     */
    val durationMs: Long
        get() = maxOf(
            videoClips.maxOfOrNull { it.endMs } ?: 0L,
            audioClips.maxOfOrNull { it.endMs } ?: 0L,
            textClips.maxOfOrNull { it.endMs } ?: 0L
        )

    /**
     * Number of frames required to cover the entire project.
     */
    val durationFrames: Long
        get() = settings.durationMsToFramesCeil(durationMs)

    /**
     * Playhead position converted to the nearest frame.
     */
    val playheadFrame: Long
        get() = settings.msToFrame(playheadMs)
}
