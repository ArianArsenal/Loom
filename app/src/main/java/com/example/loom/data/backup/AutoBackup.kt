package com.example.loom.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Weekly ZIP backups into a folder the user picked once (Storage Access Framework tree). */
object AutoBackup {
    private const val PREFS = "loom_auto_backup"
    private const val KEY_TREE = "treeUri"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LAST = "lastBackupMillis"
    private const val KEY_ERROR = "lastError"
    private const val WORK_PERIODIC = "auto_backup_weekly"
    private const val WORK_NOW = "auto_backup_now"
    private const val FILE_PREFIX = "Loom_AutoBackup_"
    const val KEEP_COPIES = 5

    data class State(val enabled: Boolean, val folder: Uri?, val lastBackupMillis: Long, val lastError: String?)

    fun state(context: Context): State {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return State(
            enabled = p.getBoolean(KEY_ENABLED, false),
            folder = p.getString(KEY_TREE, null)?.let(Uri::parse),
            lastBackupMillis = p.getLong(KEY_LAST, 0L),
            lastError = p.getString(KEY_ERROR, null)
        )
    }

    /** Remembers the chosen folder (with persistable access) and starts the weekly schedule. */
    fun enable(context: Context, treeUri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            treeUri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_TREE, treeUri.toString())
            putBoolean(KEY_ENABLED, true)
        }
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
        backUpNow(context)
    }

    fun disable(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_ENABLED, false) }
        WorkManager.getInstance(context).cancelUniqueWork(WORK_PERIODIC)
    }

    fun backUpNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NOW, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<AutoBackupWorker>().build())
    }

    internal suspend fun run(context: Context, backupManager: BackupManager) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val tree = prefs.getString(KEY_TREE, null)?.let(Uri::parse) ?: return
        try {
            val resolver = context.contentResolver
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val name = FILE_PREFIX + SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date()) + ".zip"
            val doc = DocumentsContract.createDocument(resolver, parent, "application/zip", name)
                ?: error("Could not create a file in the backup folder")
            resolver.openOutputStream(doc)?.use { backupManager.writeZip(it) } ?: error("Could not write to the backup folder")
            pruneOldCopies(context, tree)
            prefs.edit {
                putLong(KEY_LAST, System.currentTimeMillis())
                remove(KEY_ERROR)
            }
        } catch (e: Exception) {
            android.util.Log.e("AutoBackup", "Automatic backup failed", e)
            prefs.edit { putString(KEY_ERROR, e.message ?: e.javaClass.simpleName) }
            throw e
        }
    }

    /** Keeps only the newest [KEEP_COPIES] automatic backups (never touches other files). */
    private fun pruneOldCopies(context: Context, tree: Uri) {
        val resolver = context.contentResolver
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val backups = mutableListOf<Pair<String, String>>()
        resolver.query(children, projection, null, null, null)?.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0)
                val name = c.getString(1) ?: continue
                if (name.startsWith(FILE_PREFIX) && name.endsWith(".zip")) backups += id to name
            }
        }
        // File names embed a sortable timestamp
        backups.sortedByDescending { it.second }.drop(KEEP_COPIES).forEach { (id, _) ->
            try { DocumentsContract.deleteDocument(resolver, DocumentsContract.buildDocumentUriUsingTree(tree, id)) } catch (_: Exception) {}
        }
    }
}

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BackupEntryPoint {
        fun backupManager(): BackupManager
    }

    override suspend fun doWork(): Result {
        val backupManager = EntryPointAccessors.fromApplication(applicationContext, BackupEntryPoint::class.java).backupManager()
        return try {
            AutoBackup.run(applicationContext, backupManager)
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }
}
