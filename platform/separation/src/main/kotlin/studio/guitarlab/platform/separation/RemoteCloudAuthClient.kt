package studio.guitarlab.platform.separation

import android.content.Context
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.io.IOException
import kotlinx.coroutines.tasks.await
import studio.guitarlab.core.separation.RemoteJobState

data class RemoteCloudAuthSession(
    val uid: String,
    val email: String,
)

class RemoteCloudAuthException(
    val code: String,
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

internal class RemoteAuthenticationRequiredException :
    IllegalStateException("AUTH_REQUIRED")

internal object RemoteCloudAuthPolicy {
    fun isStableSession(isAnonymous: Boolean, email: String?): Boolean =
        !isAnonymous && !email.isNullOrBlank()

    fun authMessage(code: String): Pair<String, String> {
        val normalized = code.removePrefix("ERROR_").uppercase()
        return when (normalized) {
            "INVALID_EMAIL" -> "INVALID_EMAIL" to "O e-mail informado não é válido."
            "INVALID_CREDENTIAL", "INVALID_LOGIN_CREDENTIALS", "WRONG_PASSWORD", "USER_NOT_FOUND" ->
                "INVALID_CREDENTIAL" to "E-mail ou senha inválidos."
            "USER_DISABLED" -> "USER_DISABLED" to "Esta conta Firebase está desativada."
            "TOO_MANY_REQUESTS" -> "TOO_MANY_REQUESTS" to "Muitas tentativas de login. Aguarde e tente novamente."
            "NETWORK_REQUEST_FAILED" -> "NETWORK" to "Não foi possível acessar o Firebase. Verifique a conexão."
            "OPERATION_NOT_ALLOWED" -> "EMAIL_AUTH_DISABLED" to "O login por e-mail e senha está desativado no Firebase."
            else -> "AUTH_$normalized" to "Não foi possível autenticar a conta de processamento em nuvem."
        }
    }
}

class RemoteCloudAuthClient(context: Context) {
    private val appContext = context.applicationContext
    private val app get() = GuitarLabFirebase.app(appContext)
    private val auth get() = FirebaseAuth.getInstance(app)
    private val functions get() = FirebaseFunctions.getInstance(app, REGION)

    fun currentSession(): RemoteCloudAuthSession? {
        val user = auth.currentUser ?: return null
        if (!RemoteCloudAuthPolicy.isStableSession(user.isAnonymous, user.email)) return null
        return RemoteCloudAuthSession(user.uid, requireNotNull(user.email))
    }

    suspend fun signInAndValidate(email: String, password: CharArray): RemoteCloudAuthSession {
        require(email.isNotBlank()) { "E-mail obrigatório." }
        require(password.isNotEmpty()) { "Senha obrigatória." }
        val passwordString = password.concatToString()
        try {
            auth.signOut()
            val result = auth.signInWithEmailAndPassword(email.trim(), passwordString).await()
            val user = requireNotNull(result.user) { "AUTH_REQUIRED" }
            if (!RemoteCloudAuthPolicy.isStableSession(user.isAnonymous, user.email)) {
                auth.signOut()
                throw RemoteCloudAuthException("UNSTABLE_SESSION", "A conta autenticada não possui identidade estável.")
            }
            validateBackendAuthorization()
            return RemoteCloudAuthSession(user.uid, requireNotNull(user.email))
        } catch (error: RemoteCloudAuthException) {
            auth.signOut()
            throw error
        } catch (error: FirebaseAuthException) {
            auth.signOut()
            val (code, message) = RemoteCloudAuthPolicy.authMessage(error.errorCode)
            throw RemoteCloudAuthException(code, message, error)
        } catch (error: FirebaseFunctionsException) {
            auth.signOut()
            if (error.code == FirebaseFunctionsException.Code.PERMISSION_DENIED) {
                throw RemoteCloudAuthException(
                    "ACCOUNT_NOT_AUTHORIZED",
                    "Esta conta não está autorizada para a separação em nuvem.",
                    error,
                )
            }
            throw RemoteCloudAuthException(
                "BACKEND_${error.code.name}",
                "A conta foi autenticada, mas o backend de separação não pôde ser validado.",
                error,
            )
        } catch (error: FirebaseNetworkException) {
            auth.signOut()
            throw RemoteCloudAuthException("NETWORK", "Não foi possível acessar o Firebase. Verifique a conexão.", error)
        } catch (error: IOException) {
            auth.signOut()
            throw RemoteCloudAuthException("NETWORK", "Não foi possível validar a conta na nuvem.", error)
        } finally {
            password.fill('\u0000')
        }
    }

    suspend fun validateCurrentSession(): RemoteCloudAuthSession {
        val session = currentSession()
            ?: throw RemoteCloudAuthException("AUTH_REQUIRED", "Entre na conta de processamento em nuvem.")
        try {
            validateBackendAuthorization()
            return session
        } catch (error: FirebaseFunctionsException) {
            if (error.code == FirebaseFunctionsException.Code.PERMISSION_DENIED) {
                throw RemoteCloudAuthException(
                    "ACCOUNT_NOT_AUTHORIZED",
                    "Esta conta não está autorizada para a separação em nuvem.",
                    error,
                )
            }
            throw RemoteCloudAuthException(
                "BACKEND_${error.code.name}",
                "Não foi possível validar o backend de separação.",
                error,
            )
        }
    }

    fun signOut() {
        val hasActive = FileRemoteJobStore(appContext).active().any {
            it.state !in setOf(
                RemoteJobState.IMPORT_FAILED,
                RemoteJobState.IMPORTED,
                RemoteJobState.CANCELLED,
                RemoteJobState.FAILED,
                RemoteJobState.EXPIRED,
            )
        }
        if (hasActive) {
            throw RemoteCloudAuthException(
                "ACTIVE_REMOTE_JOB",
                "Conclua ou cancele a separação em andamento antes de sair da conta.",
            )
        }
        auth.signOut()
    }

    private suspend fun validateBackendAuthorization() {
        functions.getHttpsCallable("remoteBackendStatus").call(emptyMap<String, Any>()).await()
    }

    private companion object {
        const val REGION = "us-central1"
    }
}

internal fun requireStableRemoteUid(auth: FirebaseAuth): String {
    val user = auth.currentUser ?: throw RemoteAuthenticationRequiredException()
    if (!RemoteCloudAuthPolicy.isStableSession(user.isAnonymous, user.email)) {
        throw RemoteAuthenticationRequiredException()
    }
    return user.uid
}
