
package com.visionstudio.app

import java.util.UUID
import kotlin.math.roundToLong

/**
 * VISION STUDIO
 * Timeline Engine - Version 2
 *
 * محرك العمليات الأساسية للخط الزمني.
 * يدعم الفيديو والصوت والنصوص والمسارات ومؤشر التشغيل.
 */
object TimelineEngine {

    // إنشاء المسارات الافتراضية
    fun createDefaultTracks(): List<TimelineTrack> {
        return listOf(
            TimelineTrack("V1 - Main Video", TimelineTrackType.VIDEO, 0),
            TimelineTrack("V2 - Overlay Video", TimelineTrackType.VIDEO, 1),
            TimelineTrack("A1 - Main Audio", TimelineTrackType.AUDIO, 2),
            TimelineTrack("A2 - Music", TimelineTrackType.AUDIO, 3),
            TimelineTrack("T1 - Titles", TimelineTrackType.TEXT, 4)
        )
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

        return state.copy(tracks = state.tracks + track)
    }

    // حذف مسار وجميع محتوياته
    fun removeTrack(
        state: TimelineState,
        trackId: String
    ): TimelineState {
        val removedClipIds =
            state.videoClips.filter { it.trackId == trackId }.map { it.id } +
            state.audioClips.filter { it.trackId == trackId }.map { it.id } +
            state.textClips.filter { it.trackId == trackId }.map { it.id }

        return state.copy(
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
        ).let { clampPlayhead(it) }
    }

    // التحقق من وجود مسار مناسب
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

    // إضافة فيديو
    fun addVideoClip(
        state: TimelineState,
        clip: TimelineVideoClip
    ): TimelineState {
        requireTrack(state, clip.trackId, TimelineTrackType.VIDEO)
        require(state.videoClips.none { it.id == clip.id }) {
            "Video clip ID already exists"
        }
        require(clip.positionMs >= 0L && clip.durationMs > 0L) {
            "Invalid video timing"
        }

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
        require(state.audioClips.none { it.id == clip.id }) {
            "Audio clip ID already exists"
        }
        require(clip.positionMs >= 0L && clip.durationMs > 0L) {
            "Invalid audio timing"
        }

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
        require(state.textClips.none { it.id == clip.id }) {
            "Text clip ID already exists"
        }
        require(clip.positionMs >= 0L && clip.durationMs > 0L) {
            "Invalid text timing"
        }

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
        require(newPositionMs >= 0L) {
            "Position cannot be negative"
        }
        require(state.videoClips.any { it.id == clipId }) {
            "Video clip not found"
        }

        return state.copy(
            videoClips = state.videoClips.map {
                if (it.id == clipId) it.copy(positionMs = newPositionMs)
                else it
            }
        )
    }

    // نقل صوت
    fun moveAudioClip(
        state: TimelineState,
        clipId: String,
        newPositionMs: Long
    ): TimelineState {
        require(newPositionMs >= 0L) {
            "Position cannot be negative"
        }
        require(state.audioClips.any { it.id == clipId }) {
            "Audio clip not found"
        }

        return state.copy(
            audioClips = state.audioClips.map {
                if (it.id == clipId) it.copy(positionMs = newPositionMs)
                else it
            }
        )
    }

    // نقل نص
    fun moveTextClip(
        state: TimelineState,
        clipId: String,
        newPositionMs: Long
    ): TimelineState {
        require(newPositionMs >= 0L) {
            "Position cannot be negative"
        }
        require(state.textClips.any { it.id == clipId }) {
            "Text clip not found"
        }

        return state.copy(
            textClips = state.textClips.map {
                if (it.id == clipId) it.copy(positionMs = newPositionMs)
                else it
            }
        )
    }

    // حذف فيديو وتنظيف الانتقالات والإطارات المفتاحية
    fun removeVideoClip(
        state: TimelineState,
        clipId: String
    ): TimelineState {
        require(state.videoClips.any { it.id == clipId }) {
            "Video clip not found"
        }

        return state.copy(
            videoClips = state.videoClips.filterNot { it.id == clipId },
            transitions = state.transitions.filterNot {
                it.fromClipId == clipId || it.toClipId == clipId
            },
            keyframes = state.keyframes.filterNot {
                it.clipId == clipId
            }
        ).let { clampPlayhead(it) }
    }

    // حذف صوت
    fun removeAudioClip(
        state: TimelineState,
        clipId: String
    ): TimelineState {
        require(state.audioClips.any { it.id == clipId }) {
            "Audio clip not found"
        }

        return state.copy(
            audioClips = state.audioClips.filterNot { it.id == clipId },
            keyframes = state.keyframes.filterNot {
                it.clipId == clipId
            }
        ).let { clampPlayhead(it) }
    }

    // حذف نص
    fun removeTextClip(
        state: TimelineState,
        clipId: String
    ): TimelineState {
        require(state.textClips.any { it.id == clipId }) {
            "Text clip not found"
        }

        return state.copy(
            textClips = state.textClips.filterNot { it.id == clipId },
            keyframes = state.keyframes.filterNot {
                it.clipId == clipId
            }
        ).let { clampPlayhead(it) }
    }

    // تقسيم فيديو مع مراعاة سرعة التشغيل
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

        val leftClip = clip.copy(
            durationMs = leftDuration,
            sourceEndMs = sourceSplit
        )

        val rightClip = clip.copy(
            id = UUID.randomUUID().toString(),
            positionMs = splitAtMs,
            durationMs = rightDuration,
            sourceStartMs = sourceSplit
        )

        return state.copy(
            videoClips = state.videoClips
                .filterNot { it.id == clipId } + leftClip + rightClip
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
        val sourceSplit = (
            clip.sourceStartMs + leftDuration
        ).coerceIn(clip.sourceStartMs, clip.sourceEndMs)

        val leftClip = clip.copy(
            durationMs = leftDuration,
            sourceEndMs = sourceSplit,
            fadeOutMs = 0L
        )

        val rightClip = clip.copy(
            id = UUID.randomUUID().toString(),
            positionMs = splitAtMs,
            durationMs = clip.durationMs - leftDuration,
            sourceStartMs = sourceSplit,
            fadeInMs = 0L
        )

        return state.copy(
            audioClips = state.audioClips
                .filterNot { it.id == clipId } + leftClip + rightClip
        )
    }

    // تحويل زمن إلى أقرب إطار
    fun snapToFrame(
        timeMs: Long,
        frameRate: Int
    ): Long {
        require(frameRate > 0) {
            "Frame rate must be greater than zero"
        }

        val frame = (
            timeMs.coerceAtLeast(0L).toDouble() *
                frameRate / 1000.0
            ).roundToLong()

        return (frame.toDouble() * 1000.0 / frameRate)
            .roundToLong()
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
                state.settings.frameRate
            ).roundToLong()

        return movePlayhead(state, timeMs)
    }

    // إعادة مؤشر التشغيل للبداية
    fun resetPlayhead(
        state: TimelineState
    ): TimelineState {
        return state.copy(playheadMs = 0L)
    }

    // ضبط المؤشر بعد تغيّر مدة المشروع
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
