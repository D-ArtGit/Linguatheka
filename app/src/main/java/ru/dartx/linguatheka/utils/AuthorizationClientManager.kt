package ru.dartx.linguatheka.utils

import android.content.Context
import android.content.IntentSender
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

object AuthorizationClientManager {

    inline fun authorize(
        context: Context,
        crossinline useResult: (AuthorizationResult) -> Unit
    ) {
        val requestedScopes =
            listOf(
                Scope(DriveScopes.DRIVE_FILE),
                Scope(DriveScopes.DRIVE_APPDATA)
            )
        val authorizationRequest = AuthorizationRequest
            .builder()
            .setRequestedScopes(requestedScopes)
            .build()
        Identity.getAuthorizationClient(context)
            .authorize(authorizationRequest)
            .addOnSuccessListener { authorizationResult ->
                if (!authorizationResult.hasResolution()) useResult(authorizationResult)
            }
    }


    inline fun authorize(
        context: Context,
        activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
        crossinline useResult: (AuthorizationResult) -> Unit
    ) {
        val requestedScopes =
            listOf(
                Scope(DriveScopes.DRIVE_FILE),
                Scope(DriveScopes.DRIVE_APPDATA)
            )
        val authorizationRequest = AuthorizationRequest
            .builder()
            .setRequestedScopes(requestedScopes)
            .build()
        Identity.getAuthorizationClient(context)
            .authorize(authorizationRequest)
            .addOnSuccessListener { authorizationResult ->
                if (authorizationResult.hasResolution()) {
                    val pendingIntent = authorizationResult.pendingIntent
                    try {
                        val intentSenderRequest =
                            IntentSenderRequest.Builder(pendingIntent!!).build()
                        activityResultLauncher.launch(intentSenderRequest)
                    } catch (e: IntentSender.SendIntentException) {
                        Log.d("DArtX", e.message.toString())
                    }
                } else {
                    useResult(authorizationResult)
                }
            }
    }
}