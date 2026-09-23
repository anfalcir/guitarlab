package studio.guitarlab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import studio.guitarlab.core.model.GuitarProject
import studio.guitarlab.core.model.PreparationStatus

enum class ProjectWorkspace(val code: String, val label: String) {
    PREPARE("prepare", "Preparar"),
    STUDIO("studio", "Studio"),
    EXPORT("export", "Exportar");

    fun destination(projectId: String): AppScreen = when (this) {
        PREPARE -> AppScreen.Prepare(projectId)
        STUDIO -> AppScreen.Studio(projectId)
        EXPORT -> AppScreen.Export(projectId)
    }

    companion object {
        fun fromCode(code: String?): ProjectWorkspace? = entries.firstOrNull { it.code == code }
    }
}

data class ProjectWorkspaceContext(val projectId: String, val workspace: ProjectWorkspace)

fun AppScreen.projectWorkspaceContextOrNull(): ProjectWorkspaceContext? = when (this) {
    is AppScreen.Prepare -> ProjectWorkspaceContext(projectId, ProjectWorkspace.PREPARE)
    is AppScreen.Studio -> ProjectWorkspaceContext(projectId, ProjectWorkspace.STUDIO)
    is AppScreen.Export -> ProjectWorkspaceContext(projectId, ProjectWorkspace.EXPORT)
    else -> null
}

enum class UnifiedProjectCardStatus(val label: String) {
    STUDIO_READY("Studio pronto"),
    SOURCE_READY("Fonte pronta"),
    SEPARATING("Separação em andamento"),
    PREPARED("Preparado"),
    PREPARATION_ERROR("Preparação requer atenção"),
}

object UnifiedProjectShellPolicy {
    fun initialWorkspace(project: GuitarProject): ProjectWorkspace = when (project.preparation?.status) {
        PreparationStatus.SOURCE_READY,
        PreparationStatus.SEPARATING,
        PreparationStatus.ERROR -> ProjectWorkspace.PREPARE
        PreparationStatus.READY -> if (
            project.preparation?.activeBackingAssetId != null &&
            project.preparation?.activeGuitarAssetId != null
        ) ProjectWorkspace.STUDIO else ProjectWorkspace.PREPARE
        PreparationStatus.NOT_STARTED,
        null -> ProjectWorkspace.STUDIO
    }

    fun initialScreen(project: GuitarProject): AppScreen = initialWorkspace(project).destination(project.id)

    fun cardStatus(project: GuitarProject): UnifiedProjectCardStatus = when (project.preparation?.status) {
        PreparationStatus.SOURCE_READY -> UnifiedProjectCardStatus.SOURCE_READY
        PreparationStatus.SEPARATING -> UnifiedProjectCardStatus.SEPARATING
        PreparationStatus.READY -> UnifiedProjectCardStatus.PREPARED
        PreparationStatus.ERROR -> UnifiedProjectCardStatus.PREPARATION_ERROR
        PreparationStatus.NOT_STARTED, null -> UnifiedProjectCardStatus.STUDIO_READY
    }
}

@Composable
fun ProjectShellScaffold(
    project: GuitarProject?,
    currentWorkspace: ProjectWorkspace,
    onProjects: () -> Unit,
    onPrepare: () -> Unit,
    onStudio: () -> Unit,
    onExport: () -> Unit,
    onSettings: () -> Unit,
    trailingActions: @Composable () -> Unit = {},
    secondaryBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().testTag("project-shell-${currentWorkspace.code}"),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 1.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val compact = maxWidth < 960.dp
                if (compact) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ProjectIdentity(project, Modifier.weight(1f))
                            trailingActions()
                            AppIconButton(
                                icon = Icons.Default.Home,
                                contentDescription = "Início",
                                onClick = onProjects,
                                modifier = Modifier.testTag("project-shell-projects"),
                            )
                            AppIconButton(
                                icon = Icons.Default.Tune,
                                contentDescription = "Opções",
                                onClick = onSettings,
                                modifier = Modifier.testTag("project-shell-settings"),
                            )
                        }
                        ProjectWorkspaceNavigation(
                            currentWorkspace = currentWorkspace,
                            onPrepare = onPrepare,
                            onStudio = onStudio,
                            onExport = onExport,
                        )
                    }
                } else {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    ) {
                        ProjectIdentity(
                            project,
                            Modifier.align(Alignment.CenterStart).widthIn(max = 300.dp),
                        )
                        ProjectWorkspaceNavigation(
                            currentWorkspace = currentWorkspace,
                            onPrepare = onPrepare,
                            onStudio = onStudio,
                            onExport = onExport,
                            modifier = Modifier.align(Alignment.Center).widthIn(max = 520.dp),
                        )
                        Row(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            trailingActions()
                            AppIconButton(
                                icon = Icons.Default.Home,
                                contentDescription = "Início",
                                onClick = onProjects,
                                modifier = Modifier.testTag("project-shell-projects"),
                            )
                            AppIconButton(
                                icon = Icons.Default.Tune,
                                contentDescription = "Opções",
                                onClick = onSettings,
                                modifier = Modifier.testTag("project-shell-settings"),
                            )
                        }
                    }
                }
            }
        }
        secondaryBar?.invoke()
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
                .testTag("project-shell-content"),
        ) { content() }
    }
}

@Composable
private fun ProjectIdentity(project: GuitarProject?, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            project?.name ?: "Carregando projeto…",
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("project-shell-project-name"),
        )
        Text(
            project?.let(UnifiedProjectShellPolicy::cardStatus)?.label ?: "Carregando estado do projeto…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("project-shell-status"),
        )
    }
}

@Composable
private fun ProjectWorkspaceNavigation(
    currentWorkspace: ProjectWorkspace,
    onPrepare: () -> Unit,
    onStudio: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().testTag("project-shell-navigation"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProjectWorkspaceButton(ProjectWorkspace.PREPARE, currentWorkspace, onPrepare, Modifier.weight(1f))
        ProjectWorkspaceButton(ProjectWorkspace.STUDIO, currentWorkspace, onStudio, Modifier.weight(1f))
        ProjectWorkspaceButton(ProjectWorkspace.EXPORT, currentWorkspace, onExport, Modifier.weight(1f))
    }
}

@Composable
private fun ProjectWorkspaceButton(
    workspace: ProjectWorkspace,
    currentWorkspace: ProjectWorkspace,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val selected = workspace == currentWorkspace
    val buttonModifier = modifier
        .heightIn(min = 48.dp)
        .semantics { this.selected = selected }
        .testTag("project-shell-nav-${workspace.code}")
    if (selected) {
        Button(onClick = {}, modifier = buttonModifier) { Text(workspace.label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = buttonModifier) { Text(workspace.label) }
    }
}
