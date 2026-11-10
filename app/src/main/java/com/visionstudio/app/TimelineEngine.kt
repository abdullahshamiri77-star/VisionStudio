package com.visionstudio.app

import java.math.BigInteger
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Pure immutable timeline operations.
 * All clip times are milliseconds.
 * Keyframe time is relative to its owning clip.
 */
object TimelineEngine {

    private val supportedSpeeds = setOf(0.5f, 1f, 2f, 4f)

    private fun checkedEnd(positionMs: Long, durationMs: Long): Long {
        require(positionMs >= 0L) { "Position must not be negative" }
        require(durationMs > 0L) { "Duration must be positive" }
        return Math.addExact(positionMs, durationMs)
    }

    private fun sourceDuration(startMs: Long, endMs: Long): Long {
        require(startMs >= 0L && endMs > startMs) {
            "Invalid source range"
        }
        return Math.subtractExact(endMs, startMs)
    }

    private fun allClipIds(state: TimelineState): Set<String> =
        (state.videoClips.map { it.id } +
            state.audioClips.map { it.id } +
            state.textClips.map { it.id }).toSet()

    private fun requireUniqueClipId(state: TimelineState, id: String) {
        require(id.isNotBlank()) { "Clip ID must not be blank" }
        require(id !in allClipIds(state)) { "Duplicate clip ID: $id" }
    }

    private fun requireVideo(c: TimelineVideoClip) {
        checkedEnd(c.positionMs, c.durationMs)

        require(c.id.isNotBlank())
        require(c.trackId.isNotBlank())
        require(c.sourceUri.isNotBlank())
        require(c.name.isNotBlank())

        val actualSourceDuration =
            sourceDuration(c.sourceStartMs, c.sourceEndMs)

        require(c.speed in supportedSpeeds) {
            "Speed must be 0.5x, 1x, 2x, or 4x"
        }

        require(c.volume.isFinite() && c.volume >= 0f)

        val expectedSourceDuration =
            (c.durationMs.toDouble() * c.speed).roundToLong()

        require(
            abs(
                actualSourceDuration.toDouble() -
                    expectedSourceDuration.toDouble()
            ) <= 1.0
        ) {
            "Source range does not match timeline duration and speed"
        }
    }

    private fun requireAudio(c: TimelineAudioClip) {
        checkedEnd(c.positionMs, c.durationMs)

        require(c.id.isNotBlank())
        require(c.trackId.isNotBlank())
        require(c.sourceUri.isNotBlank())
        require(c.name.isNotBlank())

        val actualSourceDuration =
            sourceDuration(c.sourceStartMs, c.sourceEndMs)

        require(
            abs(
                actualSourceDuration.toDouble() -
                    c.durationMs.toDouble()
            ) <= 1.0
        ) {
            "Audio source range must match timeline duration (1x)"
        }

        require(c.volume.isFinite() && c.volume >= 0f)
        require(c.startGain.isFinite() && c.startGain in 0f..1f)
        require(c.fadeInMs in 0L..c.durationMs)
        require(c.fadeOutMs in 0L..c.durationMs)
    }

    private fun requireText(c: TimelineTextClip) {
        checkedEnd(c.positionMs, c.durationMs)

        require(c.id.isNotBlank())
        require(c.trackId.isNotBlank())
        require(c.fontSize.isFinite() && c.fontSize > 0f)
        require(c.positionX.isFinite() && c.positionY.isFinite())
    }

    private fun requireUniqueNonBlank(
        values: List<String>,
        label: String
    ) {
        require(values.all { it.isNotBlank() }) {
            "$label ID must not be blank"
        }

        require(values.toSet().size == values.size) {
            "Duplicate $label ID"
        }
    }

    /**
     * Validates the complete timeline state.
     * Throws if any entity or reference is invalid.
     */
    fun validateState(state: TimelineState) {
        require(state.playheadMs >= 0L) {
            "Playhead must not be negative"
        }

        // Tracks
        val trackIds = state.tracks.map { it.id }
        requireUniqueNonBlank(trackIds, "Track")

        require(state.tracks.all {
            it.name.isNotBlank() &&
                it.index >= 0 &&
                it.volume.isFinite() &&
                it.volume >= 0f
        }) {
            "Invalid track properties"
        }

        require(
            state.tracks.map { it.index }.toSet().size ==
                state.tracks.size
        ) {
            "Duplicate track index"
        }

        // Clip IDs must be unique across all clip types.
        val clipIds =
            state.videoClips.map { it.id } +
                state.audioClips.map { it.id } +
                state.textClips.map { it.id }

        requireUniqueNonBlank(clipIds, "Clip")

        // Video clips and track references.
        state.videoClips.forEach { clip ->
            requireVideo(clip)

            val track = state.tracks.singleOrNull {
                it.id == clip.trackId
            } ?: throw IllegalArgumentException(
                "Video clip references missing or duplicated track"
            )

            require(track.type == TimelineTrackType.VIDEO) {
                "Video clip on incompatible track"
            }
        }

        // Audio clips and track references.
        state.audioClips.forEach { clip ->
            requireAudio(clip)

            val track = state.tracks.singleOrNull {
                it.id == clip.trackId
            } ?: throw IllegalArgumentException(
                "Audio clip references missing or duplicated track"
            )

            require(track.type == TimelineTrackType.AUDIO) {
                "Audio clip on incompatible track"
            }
        }

        // Text clips and track references.
        state.textClips.forEach { clip ->
            requireText(clip)

            val track = state.tracks.singleOrNull {
                it.id == clip.trackId
            } ?: throw IllegalArgumentException(
                "Text clip references missing or duplicated track"
            )

            require(track.type == TimelineTrackType.TEXT) {
                "Text clip on incompatible track"
            }
        }

        // Transitions must reference distinct existing video clips.
        val videoIds = state.videoClips.map { it.id }.toSet()

        requireUniqueNonBlank(
            state.transitions.map { it.id },
            "Transition"
        )

        state.transitions.forEach { transition ->
            require(transition.fromClipId.isNotBlank())
            require(transition.toClipId.isNotBlank())

            require(transition.fromClipId != transition.toClipId) {
                "Transition endpoints must differ"
            }

            require(
                transition.fromClipId in videoIds &&
                    transition.toClipId in videoIds
            ) {
                "Transition endpoints must reference existing video clips"
            }

            require(transition.durationMs > 0L) {
                "Transition duration must be positive"
            }
        }

        // Keyframes must reference existing clips and valid clip-relative times.
        requireUniqueNonBlank(
            state.keyframes.map { it.id },
            "Keyframe"
        )

        val clipDurations = buildMap<String, Long> {
            state.videoClips.forEach {
                put(it.id, it.durationMs)
            }
            state.audioClips.forEach {
                put(it.id, it.durationMs)
            }
            state.textClips.forEach {
                put(it.id, it.durationMs)
            }
        }

        state.keyframes.forEach { keyframe ->
            val duration = clipDurations[keyframe.clipId]
                ?: throw IllegalArgumentException(
                    "Keyframe references missing clip"
                )

            require(
                keyframe.timeMs >= 0L &&
                    keyframe.timeMs < duration
            ) {
                "Keyframe time must be in [0, clip duration)"
            }

            require(keyframe.value.isFinite()) {
                "Keyframe value must be finite"
            }
        }

        // Reading duration also checks clip end-time overflow.
        val duration = state.durationMs

        require(state.playheadMs <= duration) {
            "Playhead exceeds project duration"
        }

        state.settings.msToFrame(state.playheadMs)
        state.settings.durationMsToFramesCeil(duration)
    }

    private fun validated(state: TimelineState): TimelineState {
        validateState(state)
        return state
    }

    private fun track(
        state: TimelineState,
        trackId: String,
        type: TimelineTrackType
    ): TimelineTrack {
        val matches = state.tracks.filter {
            it.id == trackId
        }

        require(matches.size == 1) {
            "Track missing or duplicated: $trackId"
        }

        val found = matches.single()

        require(found.type == type) {
            "Track $trackId has incompatible type"
        }

        require(!found.isLocked) {
            "Track is locked: $trackId"
        }

        return found
    }

    private fun clampPlayheadUnchecked(
        state: TimelineState
    ): TimelineState {
        return state.copy(
            playheadMs = state.playheadMs.coerceIn(
                0L,
                state.durationMs
            )
        )
    }

    fun createDefaultTracks(): List<TimelineTrack> =
        listOf(
            TimelineTrack(
                name = "V1",
                type = TimelineTrackType.VIDEO,
                index = 0
            ),
            TimelineTrack(
                name = "V2",
                type = TimelineTrackType.VIDEO,
                index = 1
            ),
            TimelineTrack(
                name = "A1",
                type = TimelineTrackType.AUDIO,
                index = 2
            ),
            TimelineTrack(
                name = "A2",
                type = TimelineTrackType.AUDIO,
                index = 3
            ),
            TimelineTrack(
                name = "T1",
                type = TimelineTrackType.TEXT,
                index = 4
            )
        )

    fun addTrack(
        state: TimelineState,
        newTrack: TimelineTrack
    ): TimelineState {
        validateState(state)

        require(newTrack.id.isNotBlank())
        require(newTrack.name.isNotBlank())
        require(newTrack.index >= 0)
        require(newTrack.volume.isFinite() && newTrack.volume >= 0f)

        require(state.tracks.none {
            it.id == newTrack.id
        }) {
            "Duplicate track ID"
        }

        require(state.tracks.none {
            it.index == newTrack.index
        }) {
            "Duplicate track index"
        }

        return validated(
            state.copy(tracks = state.tracks + newTrack)
        )
    }

    fun removeTrack(
        state: TimelineState,
        trackId: String
    ): TimelineState {
        validateState(state)

        val matches = state.tracks.filter {
            it.id == trackId
        }

        if (matches.isEmpty()) return state

        require(matches.size == 1) {
            "Duplicate track ID"
        }

        require(!matches.single().isLocked) {
            "Track is locked"
        }

        val removedIds =
            (
                state.videoClips.filter { it.trackId == trackId }.map { it.id } +
                    state.audioClips.filter { it.trackId == trackId }.map { it.id } +
                    state.textClips.filter { it.trackId == trackId }.map { it.id }
                ).toSet()

        val result = state.copy(
            tracks = state.tracks.filterNot {
                it.id == trackId
            },
            videoClips = state.videoClips.filterNot {
                it.trackId == trackId
            },
            audioClips = state.audioClips.filterNot {
                it.trackId == trackId
            },
            textClips = state.textClips.filterNot {
                it.trackId == trackId
            },
            transitions = state.transitions.filterNot {
                it.fromClipId in removedIds ||
                    it.toClipId in removedIds
            },
            keyframes = state.keyframes.filterNot {
                it.clipId in removedIds
            }
        )

        return validated(clampPlayheadUnchecked(result))
    }
