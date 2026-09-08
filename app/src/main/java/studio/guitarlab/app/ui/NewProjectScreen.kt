package studio.guitarlab.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import studio.guitarlab.core.model.ProjectTemplate

@Composable
fun NewProjectScreen(
    onBack: () -> Unit,
    onCreate: (String, ProjectTemplate) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(ProjectTemplate.GUITAR) }

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Create Project", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Project name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        TemplateCard(
            title = "Guitar Template",
            description = "Backing Track • Guitar L/R • My Guitar L/R, organized in visual groups and ready for study/recording.",
            selected = selected == ProjectTemplate.GUITAR,
            onClick = { selected = ProjectTemplate.GUITAR }
        )
        TemplateCard(
            title = "Blank Project",
            description = "Start with an empty timeline and build the track structure freely.",
            selected = selected == ProjectTemplate.BLANK,
            onClick = { selected = ProjectTemplate.BLANK }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Cancel") }
            Button(
                enabled = name.isNotBlank(),
                onClick = { onCreate(name.trim(), selected) }
            ) { Text("Create") }
        }
    }
}

@Composable
private fun TemplateCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = container
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (selected) "●  $title" else "○  $title", style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
