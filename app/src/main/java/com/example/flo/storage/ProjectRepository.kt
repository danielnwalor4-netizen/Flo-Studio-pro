package com.example.flo.storage

import android.content.Context
import android.util.Log
import com.example.flo.audio.MotifGenerator
import com.example.flo.model.Genre
import com.example.flo.model.InstrumentType
import com.example.flo.model.KeyRoot
import com.example.flo.model.Mood
import com.example.flo.model.Project
import com.example.flo.model.ScaleMode
import java.io.File

class ProjectRepository(private val context: Context) {
    private val TAG = "FloProjectRepository"
    private val projectsDir: File
        get() = File(context.filesDir, "flo_projects").apply { if (!exists()) mkdirs() }

    private val currentProjectFile: File
        get() = File(context.filesDir, "current_session.json")

    fun saveCurrentProject(project: Project): Boolean {
        return try {
            val json = project.toJson()
            // 1. Save as current active session
            currentProjectFile.writeText(json)
            // 2. Save in projects directory
            val file = File(projectsDir, "${project.id}.json")
            file.writeText(json)
            Log.d(TAG, "Saved project '${project.name}' (${project.id}) successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save project", e)
            false
        }
    }

    fun loadLastSessionProject(): Project {
        try {
            if (currentProjectFile.exists()) {
                val json = currentProjectFile.readText()
                if (json.isNotBlank()) {
                    return Project.fromJson(json)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load last session, fallback to starter template", e)
        }
        return createDefaultProject()
    }

    fun getAllSavedProjects(): List<Project> {
        val list = mutableListOf<Project>()
        try {
            val files = projectsDir.listFiles { _, name -> name.endsWith(".json") } ?: emptyArray()
            for (file in files) {
                runCatching {
                    val json = file.readText()
                    list.add(Project.fromJson(json))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list projects", e)
        }
        return list.sortedByDescending { it.lastModified }
    }

    fun deleteProject(projectId: String): Boolean {
        return try {
            val file = File(projectsDir, "$projectId.json")
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete project", e)
            false
        }
    }

    fun createDefaultProject(): Project {
        val initial = Project(
            name = "Untitled Composition",
            bpm = 75,
            mood = Mood.DARK,
            genre = Genre.CINEMATIC,
            instrument = InstrumentType.PIANO_ACOUSTIC,
            keyRoot = KeyRoot.A,
            scaleMode = ScaleMode.NATURAL_MINOR,
            bars = 16
        )
        // Pre-populate with starter arrangement
        val starterNotes = com.example.flo.audio.CompositionEngine.generateComposition(initial)
        val projectWithNotes = initial.copy(notes = starterNotes)
        saveCurrentProject(projectWithNotes)
        return projectWithNotes
    }
}
