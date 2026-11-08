package com.visionstudio.app.core.project

import java.util.UUID

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val settings: ProjectSettings = ProjectSettings(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ProjectSettings(
    val width: Int = 1920,
    val height: Int = 1080,
    val frameRate: Int = 30,
    val audioSampleRate: Int = 48000
) {
    init {
        require(width > 0) { "Project width must be positive" }
        require(height > 0) { "Project height must be positive" }
        require(frameRate > 0) { "Frame rate must be positive" }
        require(audioSampleRate > 0) {
            "Audio sample rate must be positive"
        }
    }
}
