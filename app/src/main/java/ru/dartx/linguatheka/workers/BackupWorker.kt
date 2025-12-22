package ru.dartx.linguatheka.workers

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.dartx.linguatheka.R
import ru.dartx.linguatheka.db.MainDataBase
import ru.dartx.linguatheka.presentation.activities.MainApp
import ru.dartx.linguatheka.utils.AuthorizationClientManager
import ru.dartx.linguatheka.utils.BackupAndRestoreManager
import ru.dartx.linguatheka.utils.BackupAndRestoreManager.isGrantedAllScopes
import java.util.Collections
import java.util.concurrent.TimeUnit

class BackupWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        createBackup()
        return Result.success()
    }

    private fun createBackup() {
        println("Start backup")
        AuthorizationClientManager.authorize(applicationContext) { authorizationResult ->
            if (isGrantedAllScopes(authorizationResult)) {
                val driveService =
                    BackupAndRestoreManager.googleDriveClient(
                        authorizationResult,
                        applicationContext
                    )
                if (BackupAndRestoreManager.isOnline(applicationContext)) backup(driveService)
                else println("There is no Internet connection")
            } else {
                println("Backup account not authorized")
            }
        }
    }

    private fun backup(googleDriveService: Drive) {

        CoroutineScope(Dispatchers.IO).launch {
            val database = MainDataBase.getDataBase(applicationContext as MainApp)
            database.getDao().checkpoint((SimpleSQLiteQuery("pragma wal_checkpoint(full)")))
            database.close()
            MainDataBase.destroyInstance()
            val dbPath = applicationContext.getString(R.string.db_path)
            val storageFile = com.google.api.services.drive.model.File()
            storageFile.parents = Collections.singletonList("appDataFolder")
            storageFile.name = applicationContext.getString(R.string.file_name)
            val filePath = java.io.File(dbPath)
            val mediaContent = FileContent("", filePath)
            try {
                val uploadedFiles = googleDriveService.files().list()
                    .setSpaces("appDataFolder")
                    .setPageSize(10)
                    .execute()
                val file =
                    googleDriveService.files().create(storageFile, mediaContent)
                        .setFields("id")
                        .execute()
                println("Filename: " + file.id)
                if (!file.id.isNullOrEmpty()) {
                    for (uploadedFile in uploadedFiles.files) {
                        if (uploadedFile.id != file.id) {
                            googleDriveService.files().delete(uploadedFile.id).execute()
                            println("File deleted: " + uploadedFile.name + " " + uploadedFile.id)
                        }
                    }
                }
            } catch (e: GoogleJsonResponseException) {
                println("Unable upload: ${e.details}")
                throw e
            } catch (e: Exception) {
                println("Unable upload: ${e.message}")
                throw e
            }
        }
    }

    companion object {
        private const val BACKUP_WORK_NAME = "backup_cards"
        fun startBackupWorker(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresDeviceIdle(true)
                .build()
            val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(
                24, TimeUnit.HOURS, 12, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag(BACKUP_WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                BACKUP_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                backupRequest
            )
        }
    }
}