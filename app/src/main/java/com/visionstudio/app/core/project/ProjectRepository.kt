

package com.visionstudio.app.core.project

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ProjectRepository(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences("vision_studio_projects", Context.MODE_PRIVATE)

    fun save(project: Project): Boolean {
        return try {
            val projects = getAll().toMutableList()
            val index = projects.indexOfFirst { it.id == project.id }

            if (index >= 0) {
                projects[index] = project.copy(
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                projects.add(project)
            }

            val jsonArray = JSONArray()

            projects.forEach { item ->
                jsonArray.put(item.toJson())
            }

            preferences.edit()
                .putString(KEY_PROJECTS, jsonArray.toString())
                .commit()
        } catch (exception: Exception) {
            false
        }
    }

    fun getAll(): List<Project> {
        return try {
            val stored = preferences.getString(KEY_PROJECTS, "[]")
                ?: "[]"

            val jsonArray = JSONArray(stored)
            val projects = mutableListOf<Project>()

            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(index)
                projects.add(item.toProject())
            }

            projects.sortedByDescending { it.updatedAt }
        } catch (exception: Exception) {
            emptyList()
        }
    }

    fun getById(projectId: String): Project? {
        return getAll().firstOrNull { it.id == projectId }
    }

    fun delete(projectId: String): Boolean {
        return try {
            val projects = getAll()
                .filterNot { it.id == projectId }

            val jsonArray = JSONArray()
            projects.forEach { jsonArray.put(it.toJson()) }

            preferences.edit()
                .putString(KEY_PROJECTS, jsonArray.toString())
                .commit()
        } catch (exception: Exception) {
            false
        }
    }

    private fun Project.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("createdAt", createdAt)
            put("updatedAt", updatedAt)

            put("settings", JSONObject().apply {
                put("width", settings.width)
                put("height", settings.height)
                put("frameRate", settings.frameRate)
                put("audioSampleRate", settings.audioSampleRate)
            })
        }
    }

    private fun JSONObject.toProject(): Project {
        val settingsJson = getJSONObject("settings")

        return Project(
            id = getString("id"),
            name = getString("name"),
            createdAt = getLong("createdAt"),
            updatedAt = getLong("updatedAt"),
            settings = ProjectSettings(
                width = settingsJson.getInt("width"),
                height = settingsJson.getInt("height"),
                frameRate = settingsJson.getInt("frameRate"),
                audioSampleRate = settingsJson.getInt("audioSampleRate")
            )
        )
    }

    private companion object {
        const val KEY_PROJECTS = "saved_projects"
    }
}
