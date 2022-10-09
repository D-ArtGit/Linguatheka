package ru.dartx.wordcards.utils

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import ru.dartx.wordcards.R

object GoogleDriveService {
    fun googleDriveClient(account: GoogleSignInAccount, context: Context): Drive? {
        Log.d("DArtX", "GoogleDriveService")
        val credentials =
            GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA))
        credentials.selectedAccount = account.account
        Log.d("DArtX", "Account: ${account.account}")
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credentials
        )
            .setApplicationName(context.getString(R.string.app_name))
            .build()
    }
}