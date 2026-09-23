package studio.guitarlab.platform.separation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteCloudAuthPolicyTest {
    @Test fun stableSessionRequiresNonAnonymousUserWithEmail() {
        assertTrue(RemoteCloudAuthPolicy.isStableSession(false, "owner@example.com"))
        assertFalse(RemoteCloudAuthPolicy.isStableSession(true, "owner@example.com"))
        assertFalse(RemoteCloudAuthPolicy.isStableSession(false, null))
        assertFalse(RemoteCloudAuthPolicy.isStableSession(false, ""))
    }

    @Test fun invalidCredentialsHaveActionableMessageWithoutCredentialLeak() {
        val (code, message) = RemoteCloudAuthPolicy.authMessage("ERROR_INVALID_LOGIN_CREDENTIALS")
        assertEquals("INVALID_CREDENTIAL", code)
        assertTrue(message.contains("E-mail ou senha"))
        assertFalse(message.contains("ERROR_"))
    }

    @Test fun disabledEmailProviderIsTerminalConfigurationError() {
        val (code, message) = RemoteCloudAuthPolicy.authMessage("ERROR_OPERATION_NOT_ALLOWED")
        assertEquals("EMAIL_AUTH_DISABLED", code)
        assertTrue(message.contains("desativado"))
    }

    @Test fun networkFailureRemainsClearlyTransientToTheUser() {
        val (code, message) = RemoteCloudAuthPolicy.authMessage("ERROR_NETWORK_REQUEST_FAILED")
        assertEquals("NETWORK", code)
        assertTrue(message.contains("conexão"))
    }
}
