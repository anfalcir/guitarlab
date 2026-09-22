package studio.guitarlab.app.backup

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface DriveAuthorizationState {
    data class Authorized(val accessToken: String) : DriveAuthorizationState
    data class NeedsUserConsent(val pendingIntent: PendingIntent) : DriveAuthorizationState
}

class DriveAuthorizationRequiredException : IllegalStateException(MESSAGE) {
    companion object {
        const val MESSAGE = "A autorização do Google Drive precisa ser renovada no aplicativo."
    }
}

/**
 * Thin wrapper around Google Identity Services authorization.
 *
 * This is client-side authorization only: no Firebase/Cloud Run/backend token custody is involved.
 * Once the user grants drive.file, later calls can normally mint a fresh short-lived access token
 * without UI. If Google requires user interaction again, background work fails closed and the UI
 * asks the user to reconnect instead of trying to bypass consent.
 */
class GoogleDriveAuthorization(private val context: Context) {
    private val appContext = context.applicationContext
    private val client get() = Identity.getAuthorizationClient(appContext)

    suspend fun request(): DriveAuthorizationState {
        val result = client.authorize(authorizationRequest()).awaitTask()
        if (result.hasResolution()) {
            return DriveAuthorizationState.NeedsUserConsent(
                requireNotNull(result.pendingIntent) { "O Google não forneceu a tela de autorização esperada." },
            )
        }
        val token = result.accessToken?.takeIf { it.isNotBlank() }
            ?: throw DriveAuthorizationRequiredException()
        return DriveAuthorizationState.Authorized(token)
    }

    fun tokenFromResult(data: Intent?): String {
        val intent = data ?: throw DriveAuthorizationRequiredException()
        val result = client.getAuthorizationResultFromIntent(intent)
        return result.accessToken?.takeIf { it.isNotBlank() }
            ?: throw DriveAuthorizationRequiredException()
    }

    suspend fun clearToken(token: String) {
        if (token.isBlank()) return
        client.clearToken(
            ClearTokenRequest.builder()
                .setToken(token)
                .build(),
        ).awaitTask()
    }

    suspend fun revoke() {
        client.revokeAccess(
            RevokeAccessRequest.builder()
                .setScopes(SCOPES)
                .build(),
        ).awaitTask()
    }

    private fun authorizationRequest(): AuthorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(SCOPES)
        .build()

    companion object {
        const val DRIVE_FILE_SCOPE = Scopes.DRIVE_FILE
        private val SCOPES = listOf(Scope(DRIVE_FILE_SCOPE))
    }
}

interface DriveAccessTokenProvider {
    suspend fun accessToken(): String
    suspend fun invalidateRejectedToken()
}

class GoogleDriveAccessTokenProvider(context: Context) : DriveAccessTokenProvider {
    private val authorization = GoogleDriveAuthorization(context)
    @Volatile private var cachedToken: String? = null

    override suspend fun accessToken(): String {
        cachedToken?.let { return it }
        return when (val state = authorization.request()) {
            is DriveAuthorizationState.Authorized -> state.accessToken.also { cachedToken = it }
            is DriveAuthorizationState.NeedsUserConsent -> throw DriveAuthorizationRequiredException()
        }
    }

    override suspend fun invalidateRejectedToken() {
        val rejected = cachedToken
        cachedToken = null
        if (!rejected.isNullOrBlank()) {
            authorization.clearToken(rejected)
        }
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { value ->
        continuation.resume(value)
    }
    addOnFailureListener { error ->
        continuation.resumeWithException(error)
    }
    addOnCanceledListener {
        continuation.cancel()
    }
}
