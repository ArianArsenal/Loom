package com.example.loom.data.backup

import android.content.Context
import com.example.loom.data.local.dao.SandboxDao
import com.example.loom.data.local.dao.TrackerDao
import kotlinx.coroutines.flow.first

/**
 * Deletes copied media (filesDir/media_*) that no block or note references any more, e.g. after a
 * gallery item, block or whole space was deleted. Files younger than a day are kept so a picture
 * that was just picked but not saved yet is never removed.
 */
object MediaCleaner {
    private const val MIN_AGE_MILLIS = 24 * 60 * 60 * 1000L

    suspend fun collectGarbage(context: Context, sandboxDao: SandboxDao, trackerDao: TrackerDao): Int {
        val files = context.filesDir.listFiles { f -> f.isFile && f.name.startsWith("media_") }.orEmpty()
        if (files.isEmpty()) return 0

        // Every text column that can hold a media url, flattened into one searchable string
        val referenced = buildString {
            sandboxDao.getAllSpacesOnce().forEach { space ->
                append(space.space.bannerUrl).append('\n')
                space.blocks.forEach { append(it.toString()).append('\n') }
            }
            trackerDao.getAllDiaryNotes().first().forEach { append(it.toString()).append('\n') }
        }

        val cutoff = System.currentTimeMillis() - MIN_AGE_MILLIS
        var deleted = 0
        files.forEach { file ->
            if (file.lastModified() < cutoff && !referenced.contains(file.name) && file.delete()) deleted++
        }
        return deleted
    }
}
