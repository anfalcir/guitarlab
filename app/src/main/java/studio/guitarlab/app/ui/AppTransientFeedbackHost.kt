package studio.guitarlab.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    var presentationJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(message, kind) {
        val raw = message ?: return@LaunchedEffect
        val safe = AppTransientFeedbackPolicy.userSafe(raw, fallback)
        val shouldShow = AppTransientFeedbackPolicy.shouldShowSnackbar(kind)
        if (shouldShow) {
            presentationJob?.cancel()
            presentationJob = scope.launch {
                hostState.currentSnackbarData?.dismiss()
                val duration = when (kind) {
                    TransientFeedbackKind.ERROR,
                    TransientFeedbackKind.WARNING,
                    -> SnackbarDuration.Long
                    TransientFeedbackKind.ASYNC_COMPLETION,
                    TransientFeedbackKind.OPERATIONAL_STATUS,
                    -> SnackbarDuration.Short
                }
                hostState.showSnackbar(safe, duration = duration)
            }
        }
        // Clear producer state immediately; do not wait for Snackbar duration/navigation.
        onConsumed()
    }

    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(top = 12.dp).widthIn(max = 560.dp),
    )
}
