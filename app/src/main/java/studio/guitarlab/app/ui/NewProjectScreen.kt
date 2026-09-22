package studio.guitarlab.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import studio.guitarlab.core.model.ProjectTemplate

enum class NewProjectSourceIntent { SEARCH, IMPORT }
private enum class NewProjectIntent { SEARCH, IMPORT, STUDIO }

@Composable
fun NewProjectScreen(
    onBack: () -> Unit,
    onCreate: (String, ProjectTemplate) -> Unit,
    onCreateForPrepare: (String, NewProjectSourceIntent) -> Unit = { _, _ -> },
) {
    var name by rememberSaveable { mutableStateOf("") }
    var intentName by rememberSaveable { mutableStateOf<String?>(null) }
    val intent = intentName?.let(NewProjectIntent::valueOf)
    var studioTemplateName by rememberSaveable { mutableStateOf(ProjectTemplate.GUITAR.name) }
    val studioTemplate = ProjectTemplate.valueOf(studioTemplateName)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Novo projeto", style = MaterialTheme.typography.headlineMedium)
                Text("Escolha o que você quer fazer primeiro.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AppIconButton(icon = Icons.Default.ArrowBack, contentDescription = "Voltar", onClick = onBack)
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome do projeto") },
            placeholder = { Text("Ex.: Estudo Hero") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("new-project-name"),
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Como você quer começar?", style = MaterialTheme.typography.titleMedium)
            SafeCreationPath(
                title = "Pesquisar música",
                description = "Cria um projeto de estudo de guitarra e abre Preparar para encontrar uma fonte.",
                selected = intent == NewProjectIntent.SEARCH,
                onClick = { intentName = NewProjectIntent.SEARCH.name },
                modifier = Modifier.testTag("new-project-search"),
            )
            SafeCreationPath(
                title = "Importar áudio",
                description = "Cria um projeto de estudo de guitarra e abre Preparar para validar um arquivo local.",
                selected = intent == NewProjectIntent.IMPORT,
                onClick = { intentName = NewProjectIntent.IMPORT.name },
                modifier = Modifier.testTag("new-project-import"),
            )
            SafeCreationPath(
                title = "Começar no Studio",
                description = "Abre o Studio sem exigir uma fonte preparada.",
                selected = intent == NewProjectIntent.STUDIO,
                onClick = { intentName = NewProjectIntent.STUDIO.name },
                modifier = Modifier.testTag("new-project-studio"),
            )
        }

        if (intent == NewProjectIntent.STUDIO) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("new-project-studio-template-options")) {
                Text("Estrutura inicial do Studio", style = MaterialTheme.typography.titleMedium)
                TemplateOption(
                    title = "Template de guitarra",
                    description = "Cria uma base, guitarras de referência e suas guitarras já organizadas.",
                    selected = studioTemplate == ProjectTemplate.GUITAR,
                    onClick = { studioTemplateName = ProjectTemplate.GUITAR.name },
                )
                TemplateOption(
                    title = "Projeto vazio",
                    description = "Uma área limpa para montar as pistas do jeito que você quiser.",
                    selected = studioTemplate == ProjectTemplate.BLANK,
                    onClick = { studioTemplateName = ProjectTemplate.BLANK.name },
                )
            }
        }

        Button(
            enabled = name.isNotBlank() && intent != null,
            onClick = {
                val normalized = name.trim()
                when (intent) {
                    NewProjectIntent.SEARCH -> onCreateForPrepare(normalized, NewProjectSourceIntent.SEARCH)
                    NewProjectIntent.IMPORT -> onCreateForPrepare(normalized, NewProjectSourceIntent.IMPORT)
                    NewProjectIntent.STUDIO -> onCreate(normalized, studioTemplate)
                    null -> Unit
                }
            },
            modifier = Modifier.testTag("new-project-create"),
        ) {
            Text(
                when (intent) {
                    NewProjectIntent.SEARCH -> "Criar e pesquisar"
                    NewProjectIntent.IMPORT -> "Criar e importar"
                    NewProjectIntent.STUDIO -> "Criar e abrir Studio"
                    null -> "Escolha como começar"
                },
            )
        }
    }
}

@Composable
private fun SafeCreationPath(title: String, description: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = modifier.fillMaxWidth().clip(shape).clickable(onClick = onClick),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = androidx.compose.foundation.BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (selected) "Selecionado" else "Disponível", style = MaterialTheme.typography.labelMedium, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TemplateOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape).clickable(onClick = onClick),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = androidx.compose.foundation.BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (selected) "●" else "○", color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
