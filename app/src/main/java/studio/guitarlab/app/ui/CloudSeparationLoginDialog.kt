package studio.guitarlab.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import studio.guitarlab.platform.separation.RemoteCloudAuthClient
import studio.guitarlab.platform.separation.RemoteCloudAuthSession

@Composable
fun CloudSeparationLoginDialog(
    visible: Boolean,
    authClient: RemoteCloudAuthClient,
    initialEmail: String,
    onDismiss: () -> Unit,
    onAuthenticated: (RemoteCloudAuthSession) -> Unit,
    testTagPrefix: String,
) {
    if (!visible) return

    val scope = rememberCoroutineScope()
    var email by rememberSaveable(visible, initialEmail) { mutableStateOf(initialEmail) }
    // Password is deliberately not saveable: configuration/process recreation must not persist it.
    var password by remember(visible) { mutableStateOf("") }
    var busy by remember(visible) { mutableStateOf(false) }
    var message by remember(visible) { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = Modifier.widthIn(max = 560.dp).testTag("$testTagPrefix-dialog"),
        onDismissRequest = {
            if (!busy) {
                password = ""
                onDismiss()
            }
        },
        title = { Text("Conta da separação em nuvem") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Entre com a conta pessoal do Firebase usada pelo backend. A senha é utilizada apenas para autenticar e não é armazenada pelo GuitarLab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().testTag("$testTagPrefix-email"),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha") },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("$testTagPrefix-password"),
                )
                message?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("$testTagPrefix-message"),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && email.isNotBlank() && password.isNotEmpty(),
                modifier = Modifier.testTag("$testTagPrefix-submit"),
                onClick = {
                    val secret = password.toCharArray()
                    password = ""
                    busy = true
                    message = "Autenticando e validando autorização no backend…"
                    scope.launch {
                        runCatching { authClient.signInAndValidate(email, secret) }
                            .onSuccess { session ->
                                message = null
                                onAuthenticated(session)
                            }
                            .onFailure { error ->
                                message = error.message ?: "Não foi possível autenticar a conta."
                            }
                        busy = false
                    }
                },
            ) { Text(if (busy) "Validando…" else "Entrar") }
        },
        dismissButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    password = ""
                    onDismiss()
                },
            ) { Text("Cancelar") }
        },
    )
}
