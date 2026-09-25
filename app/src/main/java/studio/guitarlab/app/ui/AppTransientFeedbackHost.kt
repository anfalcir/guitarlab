package studio.guitarlab.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * One-shot transient feedback boundary.
 *
 * The producer state is consumed before presentation is suspended on Snackbar duration. This
 * prevents a completed operation from being replayed minutes later after navigation. Presentation
 * itself lives in the composable scope and is intentionally disposable when its owning surface
 * leaves the composition.
 */
@Composable
fun AppTransientFeedbackHost(
    message: String?,
    kind: TransientFeedbackKind,
    onConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    fallback: String = "Não foi possível concluir a operação.",
) {
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var presentationJob = remember { null as Job? }

    LaunchedEffect(message, kind) {
        val raw = message ?: return@LaunchedEffect
        val safe = AppTransientFeedbackPolicy.userSafe(raw, fallback)
        val shouldShow = AppTransientFeedbackPolicy.shouldShowSnackbar(kind)
        onConsumed()
        if (shouldShow) {
            presentationJob?.cancel()
            presentationJob = scope.launch {
                hostState.currentSnackbarData?.dismiss()
                hostState.showSnackbar(safe, duration = SnackbarDuration.Short)
            }
        }
    }

    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(top = 12.dp).widthIn(max = 560.dp),
    )
}
