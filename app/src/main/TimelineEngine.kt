
package com.visionstudio.app

/**
 * VISION STUDIO
 * Timeline Engine - Version 1
 *
 * مسؤول عن العمليات الأساسية على الخط الزمني.
 */
object TimelineEngine {

    // إنشاء المسارات الافتراضية للمشروع
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

    // إضافة مسار جديد
    fun addTrack(
        state: TimelineState,
        track: TimelineTrack
    ): TimelineState {
        require(state.tracks.none { it.id == track.id }) {
            "Track ID already exists"
        }

        return state.copy(
            tracks = state.tracks + track
        )
    }

    // حذف مسار وجميع عناصره
    fun removeTrack(
        state: TimelineState,
        trackId: String
    ): TimelineState {
        return state.copy(
            tracks = state.tracks.filterNot { it.id == trackId },
            videoClips = state.videoClips.filterNot {
                it.trackId == trackId
            },
            audioClips = state.audioClips.filterNot {
                it.trackId == trackId
            },
            textClips = state.textClips.filterNot {
                it.trackId == trackId
            }
        )
    }

    // إضافة مقطع فيديو
    fun addVideoClip(
        state: TimelineState,
        clip: TimelineVideoClip
    ): TimelineState {
        require(state.tracks.any {
            it.id == clip.trackId &&
                it.type == TimelineTrackType.VIDEO
        }) {
            "Video track not found"
        }

        require(state.videoClips.none { it.id == clip.id }) {
            "Clip ID already exists"
        }

        require(clip.positionMs >= 0L && clip.durationMs > 0L) {
            "Invalid clip timing"
        }

        return state.copy(
            videoClips = state.videoClips + clip
        )
    }

    // تحريك مقطع فيديو على الخط الزمني
    fun moveVideoClip(
        state: TimelineState,
        clipId: String,
        newPositionMs: Long
    ): TimelineState {
        require(newPositionMs >= 0L) {
            "Position cannot be negative"
        }

        return state.copy(
            videoClips = state.videoClips.map { clip ->
                if (clip.id == clipId) {
                    clip.copy(positionMs = newPositionMs)
                } else {
                    clip
                }
            }
        )
    }

    // حذف مقطع فيديو
    fun removeVideoClip(
        state: TimelineState,
        clipId: String
    ): TimelineState {
        return state.copy(
            videoClips = state.videoClips.filterNot {
                it.id == clipId
            },
            transitions = state.transitions.filterNot {
                it.fromClipId == clipId ||
                    it.toClipId == clipId
            },
            keyframes = state.keyframes.filterNot {
                it.clipId == clipId
            }
        )
    }

    // تقسيم مقطع فيديو إلى مقطعين
    fun splitVideoClip(
        state: TimelineState,
        clipId: String,
        splitAtMs: Long
    ): TimelineState {
        val clip = state.videoClips.firstOrNull {
            it.id == clipId
        } ?: return state

        require(splitAtMs > clip.positionMs &&
                splitAtMs < clip.endMs) {
            "Split point must be inside the clip"
        }

        val leftDuration = splitAtMs - clip.positionMs
        val rightDuration = clip.durationMs - leftDuration

        val sourceSplit = (
            clip.sourceStartMs +
                (leftDuration * clip.speed).toLong()
            ).coerceIn(
                clip.sourceStartMs,
                clip.sourceEndMs
            )

        val leftClip = clip.copy(
            durationMs = leftDuration,
            sourceEndMs = sourceSplit
        )

        val rightClip = clip.copy(
            id = java.util.UUID.randomUUID().toString(),
            positionMs = splitAtMs,
            durationMs = rightDuration,
            sourceStartMs = sourceSplit
        )

        return state.copy(
            videoClips = state.videoClips
                .filterNot { it.id == clipId } +
                listOf(leftClip, rightClip)
        )
    }

    // تحريك مؤشر التشغيل
    fun movePlayhead(
        state: TimelineState,
        positionMs: Long
    ): TimelineState {
        return state.copy(
            playheadMs = positionMs.coerceIn(
                0L,
                state.durationMs
            )
        )
    }

    // إعادة مؤشر التشغيل إلى البداية
    fun resetPlayhead(
        state: TimelineState
    ): TimelineState {
        return state.copy(playheadMs = 0L)
    }
}
