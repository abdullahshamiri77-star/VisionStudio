
package com.visionstudio.app

import java.util.UUID
import kotlin.math.roundToLong

/**
 * VISION STUDIO
 * Timeline Engine - Version 3
 *
 * محرك العمليات الأساسية للخط الزمني.
 */
object TimelineEngine {

    // المسارات الافتراضية
    fun createDefaultTracks(): List<TimelineTrack> {
        return listOf(
            TimelineTrack(
                name = "V1 - Main Video",
                type = TimelineTrackType.VIDEO,
                index = 0
            ),
            TimelineTrack(
                name = "V2 - Overlay Video",
                type = TimelineTrackType.VIDEO,
                index = 1
            ),
            TimelineTrack(
                name = "A1 - Main Audio",
                type = TimelineTrackType.AUDIO,
                index = 2
            ),
            TimelineTrack(
                name = "A2 - Music",
                type = TimelineTrackType.AUDIO,
                index = 3
            ),
            TimelineTrack(
                name = "T1 - Titles",
                type = TimelineTrackType.TEXT,
                index = 4
            )
        )
    }

    // التحقق من المعرفات
    private fun requireUniqueClipId(
        state: TimelineState,
        clipId: String
    ) {
        val exists =
            state.videoClips.any { it.id == clipId } ||
            state.audioClips.any { it.id == clipId } ||
            state.textClips.any { it.id == clipId }

        require(!exists) {
            "Clip ID already exists"
        }
    }

    private fun requireTrack(
        state: TimelineState,
        trackId: String,
        type: TimelineTrackType
    ) {
        require(state.tracks.any {
            it.id == trackId && it.type == type
        }) {
            "Compatible track not found"
        }
    }

    // إضافة مسار
    fun addTrack(
        state: TimelineState,
        track: TimelineTrack
    ): TimelineState {
        require(state.tracks.none { it.id == track.id }) {
            "Track ID already exists"
        }
        require(state.tracks.none { it.index == track.index }) {
            "Track index already exists"
        }
        require(track.index >= 0) {
            "Track index cannot be negative"
        }

        return state.copy(
            tracks = state.tracks + track
        )
    }

    // حذف مسار ومحتوياته
    fun removeTrack(
        state: TimelineState,
        trackId: String
    ): TimelineState {
        require(state.tracks.any { it.id == trackId }) {
            "Track not found"
        }

        val removedClipIds =
            state.videoClips.filter { it.trackId == trackId }.map { it.id } +
            state.audioClips.filter { it.trackId == trackId }.map { it.id } +
            state.textClips.filter { it.trackId == trackId }.map { it.id }

        val updated = state.copy(
            tracks = state.tracks.filterNot { it.id == trackId },
            videoClips = state.videoClips.filterNot { it.trackId == trackId },
            audioClips = state.audioClips.filterNot { it.trackId == trackId },
            textClips = state.textClips.filterNot { it.trackId == trackId },
            transitions = state.transitions.filterNot {
                it.fromClipId in removedClipIds ||
                it.toClipId in removedClipIds
            },
            keyframes = state.keyframes.filterNot {
                it.clipId in removedClipIds
            }
        )

        return clampPlayhead(updated)
    }

    // إضافة فيديو
    fun addVideoClip(
        state: TimelineState,
        clip: TimelineVideoClip
    ): TimelineState {
        requireTrack(state, clip.trackId, TimelineTrackType.VIDEO)
        requireUniqueClipId(state, clip.id)

        require(clip.positionMs >= 0L)
        require(clip.durationMs > 0L)
        require(clip.sourceEndMs > clip.sourceStartMs)
        require(clip.speed.isFinite() && clip.speed > 0f)
        require(clip.volume.isFinite() && clip.volume >= 0f)

        return state.copy(
            videoClips = state.videoClips + clip
        )
    }

    // إضافة صوت
    fun addAudioClip(
        state: TimelineState,
        clip: TimelineAudioClip
    ): TimelineState {
        requireTrack(state, clip.trackId, TimelineTrackType.AUDIO)
        requireUniqueClipId(state, clip.id)

        require(clip.positionMs >= 0L)
        require(clip.durationMs > 0L)
        require(clip.sourceEndMs > clip.sourceStartMs)
        require(clip.fadeInMs <= clip.durationMs)
        require(clip.fadeOutMs <= clip.durationMs)
        require(clip.volume.isFinite() && clip.volume >= 0f)

        return state.copy(
            audioClips = state.audioClips + clip
        )
    }

    // إضافة نص
    fun addTextClip(
        state: TimelineState,
        clip: TimelineTextClip
    ): TimelineState {
        requireTrack(state, clip.trackId, TimelineTrackType.TEXT)
        requireUniqueClipId(state, clip.id)

        require(clip.positionMs >= 0L)
        require(clip.durationMs > 0L)
        require(clip.fontSize.isFinite() && clip.fontSize > 0f)
        require(clip.positionX.isFinite())
        require(clip.positionY.isFinite())

        return state.copy(
            textClips = state.textClips + clip
        )
    }

    // نقل فيديو
    fun moveVideoClip(
        state: TimelineState,
        clipId: String,
        newPositionMs: Long
    ): TimelineState {
        require(newPositionMs >= 0L)
        require(state.videoClips.any { it.id == clipId }) {
            "Video clip not found"
        }

        val updated = state.copy(
            videoClips = state.videoClips.map {
                if (it.id == clipId) it.copy(positionMs = newPositionMs)
                else it
            }
        )

        return clampPlayhead(updated)
    }

    // نقل صوت
    fun moveAudioClip(
        state: TimelineState,
        clipId: String,
        newPositionMs: Long
    ): TimelineState {
        require(newPositionMs >= 0L)
        require(state.audioClips.any { it.id == clipId }) {
            "Audio clip not found"
        }

        val updated = state.copy(
            audioClips = state.audioClips.map {
                if (it.id == clipId) it.copy(positionMs = newPositionMs)
                else it
            }
        )

        return clampPlayhead(updated)
    }

    // نقل نص
    fun moveTextClip(
        state: TimelineState,
        clipId: String,
        newPositionMs: Long
    ): TimelineState {
        require(newPositionMs >= 0L)
        require(state.textClips.any { it.id == clipId }) {
            "Text clip not found"
        }

        val updated = state.copy(
            textClips = state.textClips.map {
                if (it.id == clipId) it.copy(positionMs = newPositionMs)
                else it
            }
        )

        return clampPlayhead(updated)
    }

    // حذف فيديو
    fun removeVideoClip(
        state: TimelineState,
        clipId: String
    ): TimelineState {
        require(state.videoClips.any { it.id == clipId }) {
            "Video clip not found"
        }

        val updated = state.copy(
            videoClips = state.videoClips.filterNot { it.id == clipId },
            transitions = state.transitions.filterNot {
                it.fromClipId == clipId || it.toClipId == clipId
            },
            keyframes = state.keyframes.filterNot {
                it.clipId == clipId
            }
        )

        return clampPlayhead(updated)
    }

    // حذف صوت
    fun removeAudioClip(
        state: TimelineState,
        clipId: String
    ): TimelineState {
        require(state.audioClips.any { it.id == clipId }) {
            "Audio clip not found"
        }

        val updated = state.copy(
            audioClips = state.audioClips.filterNot { it.id == clipId },
            keyframes = state.keyframes.filterNot {
                it.clipId == clipId
            }
        )

        return clampPlayhead(updated)
    }

    // حذف نص
    fun removeTextClip(
        state: TimelineState,
        clipId: String
    ): TimelineState {
        require(state.textClips.any { it.id == clipId }) {
            "Text clip not found"
        }

        val updated = state.copy(
            textClips = state.textClips.filterNot { it.id == clipId },
            keyframes = state.keyframes.filterNot {
                it.clipId == clipId
            }
        )

        return clampPlayhead(updated)
    }

    // تقسيم فيديو
    fun splitVideoClip(
        state: TimelineState,
        clipId: String,
        splitAtMs: Long
    ): TimelineState {
        val clip = state.videoClips.firstOrNull {
            it.id == clipId
        } ?: return state

        require(splitAtMs > clip.positionMs && splitAtMs < clip.endMs) {
            "Split point must be inside the clip"
        }

        val leftDuration = splitAtMs - clip.positionMs
        val rightDuration = clip.durationMs - leftDuration

        val sourceSplit = (
            clip.sourceStartMs +
                (leftDuration.toDouble() * clip.speed).roundToLong()
        ).coerceIn(clip.sourceStartMs, clip.sourceEndMs)

        require(
            sourceSplit > clip.sourceStartMs &&
            sourceSplit < clip.sourceEndMs
        ) {
            "Invalid source split point"
        }

        val rightClipId = UUID.randomUUID().toString()

        val leftClip = clip.copy(
            durationMs = leftDuration,
            sourceEndMs = sourceSplit
        )

        val rightClip = clip.copy(
            id = rightClipId,
            positionMs = splitAtMs,
            durationMs = rightDuration,
            sourceStartMs = sourceSplit
        )

        // زمن الإطار المفتاحي نسبي لبداية المقطع
        val leftKeyframes = state.keyframes.filter {
            it.clipId == clipId && it.timeMs < leftDuration
        }

        val rightKeyframes = state.keyframes
            .filter {
                it.clipId == clipId && it.timeMs >= leftDuration
            }
            .map {
                it.copy(
                    id = UUID.randomUUID().toString(),
                    clipId = rightClipId,
                    timeMs = it.timeMs - leftDuration
                )
            }

        val updatedKeyframes =
            state.keyframes.filterNot {
                it.clipId == clipId
            } + leftKeyframes + rightKeyframes

        return state.copy(
            videoClips = state.videoClips.filterNot {
                it.id == clipId
            } + leftClip + rightClip,
            keyframes = updatedKeyframes
        )
    }

    // تقسيم صوت
    fun splitAudioClip(
        state: TimelineState,
        clipId: String,
        splitAtMs: Long
    ): TimelineState {
        val clip = state.audioClips.firstOrNull {
            it.id == clipId
        } ?: return state

        require(splitAtMs > clip.positionMs && splitAtMs < clip.endMs) {
            "Split point must be inside the clip"
        }

        val leftDuration = splitAtMs - clip.positionMs
        val rightDuration = clip.durationMs - leftDuration
        val sourceDuration = clip.sourceEndMs - clip.sourceStartMs

        val sourceSplit = clip.sourceStartMs + (
            sourceDuration.toDouble() *
                leftDuration.toDouble() /
                clip.durationMs.toDouble()
        ).roundToLong()

        require(
            sourceSplit > clip.sourceStartMs &&
            sourceSplit < clip.sourceEndMs
        ) {
            "Invalid audio source split point"
        }

        val leftClip = clip.copy(
            durationMs = leftDuration,
            sourceEndMs = sourceSplit,
            fadeOutMs = 0L
        )

        val rightClip = clip.copy(
            id = UUID.randomUUID().toString(),
            positionMs = splitAtMs,
            durationMs = rightDuration,
            sourceStartMs = sourceSplit,
            fadeInMs = 0L
        )

        return state.copy(
            audioClips = state.audioClips.filterNot {
                it.id == clipId
            } + leftClip + rightClip
        )
    }

    // تقريب الزمن إلى أقرب إطار
    fun snapToFrame(
        timeMs: Long,
        frameRate: Int
    ): Long {
        require(frameRate > 0) {
            "Frame rate must be greater than zero"
        }

        val safeTime = timeMs.coerceAtLeast(0L)

        val frame = (
            safeTime.toDouble() * frameRate.toDouble() / 1000.0
        ).roundToLong()

        return (
            frame.toDouble() * 1000.0 / frameRate.toDouble()
        ).roundToLong()
    }

    // تحريك مؤشر التشغيل
    fun movePlayhead(
        state: TimelineState,
        positionMs: Long
    ): TimelineState {
        val snappedPosition = snapToFrame(
            positionMs,
            state.settings.frameRate
        )

        return state.copy(
            playheadMs = snappedPosition.coerceIn(
                0L,
                state.durationMs
            )
        )
    }

    // الانتقال إلى إطار محدد
    fun movePlayheadToFrame(
        state: TimelineState,
        frame: Long
    ): TimelineState {
        val safeFrame = frame.coerceAtLeast(0L)

        val timeMs = (
            safeFrame.toDouble() * 1000.0 /
                state.settings.frameRate.toDouble()
        ).roundToLong()

        return movePlayhead(state, timeMs)
    }

    // إعادة مؤشر التشغيل للبداية
    fun resetPlayhead(
        state: TimelineState
    ): TimelineState {
        return state.copy(
            playheadMs = 0L
        )
    }

    // ضبط مؤشر التشغيل
    private fun clampPlayhead(
        state: TimelineState
    ): TimelineState {
        return state.copy(
            playheadMs = state.playheadMs.coerceIn(
                0L,
                state.durationMs
            )
        )
    }
}
