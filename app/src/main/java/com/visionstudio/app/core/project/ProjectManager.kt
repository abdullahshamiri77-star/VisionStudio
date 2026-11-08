

package com.visionstudio.app.core.project

import android.content.Context

class ProjectManager(context: Context) {

    private val repository = ProjectRepository(context)

    private val sessionPreferences = context.applicationContext
        .getSharedPreferences("vision_studio_session", Context.MODE_PRIVATE)

    fun createProject(
        name: String,
        settings: ProjectSettings = ProjectSettings()
    ): Project? {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return null

        val duplicate = repository.getAll().any {
            it.name.equals(cleanName, ignoreCase = true)
        }
        if (duplicate) return null

        val project = Project(
            name = cleanName,
            settings = settings
        )

        if (!repository.save(project)) return null

        sessionPreferences.edit()
            .putString(KEY_ACTIVE_PROJECT, project.id)
            .apply()

        return project
    }

    fun getProjects(): List<Project> {
        return repository.getAll()
    }

    fun openProject(projectId: String): Project? {
        val project = repository.getById(projectId) ?: return null

        sessionPreferences.edit()
            .putString(KEY_ACTIVE_PROJECT, project.id)
            .apply()

        return project
    }

    fun getActiveProject(): Project? {
        val activeId = sessionPreferences
            .getString(KEY_ACTIVE_PROJECT, null)
            ?: return null

        return repository.getById(activeId)
    }

    fun renameProject(
        projectId: String,
        newName: String
    ): Project? {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) return null

        val project = repository.getById(projectId) ?: return null

        val duplicate = repository.getAll().any {
            it.id != projectId &&
                it.name.equals(cleanName, ignoreCase = true)
        }
        if (duplicate) return null

        val updated = project.copy(
            name = cleanName,
            updatedAt = System.currentTimeMillis()
        )

        return if (repository.save(updated)) updated else null
    }

    fun deleteProject(projectId: String): Boolean {
        val deleted = repository.delete(projectId)

        if (deleted &&
            sessionPreferences.getString(KEY_ACTIVE_PROJECT, null) == projectId
        ) {
            sessionPreferences.edit()
                .remove(KEY_ACTIVE_PROJECT)
                .apply()
        }

        return deleted
    }

    private companion object {
        const val KEY_ACTIVE_PROJECT = "active_project_id"
    }
}
