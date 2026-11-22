package net.akehurst.datastore.gui.compose.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.akehurst.datastore.gui.compose.CompositionEditorState
import net.akehurst.datastore.gui.compose.ConnectionDraft
import net.akehurst.datastore.gui.compose.ConnectionField
import net.akehurst.datastore.gui.compose.EditorCustomisation
import net.akehurst.datastore.gui.compose.EditorIssue
import net.akehurst.datastore.gui.compose.EndDraft
import net.akehurst.datastore.gui.compose.IssueLocation
import net.akehurst.language.types.api.TypesDomain

/** the qualified names of the data types in 'types' */
private fun dataTypeNames(types: TypesDomain?): List<String> =
    types?.namespace?.flatMap { ns -> ns.dataType.map { it.qualifiedName.value } }?.distinct()?.sorted() ?: emptyList()

/** the qualified names of all types in 'types' */
private fun typeNames(types: TypesDomain?): List<String> =
    types?.namespace?.flatMap { ns -> ns.ownedTypes.map { it.qualifiedName.value } }?.distinct()?.sorted() ?: emptyList()

/** Edits one connection: its name, target type and ends; with 'readOnly' only shows them. */
@Composable
internal fun ConnectionDetail(state: CompositionEditorState, connection: ConnectionDraft, customisation: EditorCustomisation, readOnly: Boolean = false) {
    val strings = customisation.strings
    val issues = state.issues.filter { (it.location as? IssueLocation.Connection)?.connection == connection }
    fun issueOf(field: ConnectionField, end: EndDraft? = null): EditorIssue? =
        issues.firstOrNull { (it.location as IssueLocation.Connection).let { l -> l.field == field && l.end == end } }

    val sourceTypes = state.typesOrNull(connection.source)
    val targetTypes = state.typesOrNull(connection.target)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${strings.connection}: ${connection.source.id} → ${connection.target.id}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            customisation.connectionActions(this, connection)
        }
        TextFieldWithError(
            value = connection.name,
            onValueChange = { connection.name = it },
            label = { Text(strings.connectionName) },
            error = issueOf(ConnectionField.NAME)?.message,
            modifier = Modifier.fillMaxWidth(),
            readOnly = readOnly,
        )
        SuggestionTextField(
            value = connection.targetType,
            onValueChange = { state.setTargetType(connection, it) },
            label = { Text(strings.targetType) },
            // the types that the target datastore reads are data types
            suggestions = dataTypeNames(targetTypes),
            // a new target type replaces the pre-filled ends (see CompositionEditorState.setTargetType)
            onChoose = { state.setTargetType(connection, it) },
            error = issueOf(ConnectionField.TARGET_TYPE)?.message,
            chooseLabel = strings.choose,
            modifier = Modifier.fillMaxWidth(),
            readOnly = readOnly,
        )
        Text(strings.ends, style = MaterialTheme.typography.titleSmall)
        val endTypeSuggestions = (typeNames(sourceTypes) + listOf("String", "Integer", "Real", "Boolean")).distinct()
        connection.ends.forEach { end ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        TextFieldWithError(
                            value = end.name,
                            onValueChange = { end.name = it },
                            label = { Text(strings.endName) },
                            error = issueOf(ConnectionField.END_NAME, end)?.message,
                            modifier = Modifier.weight(1f),
                            readOnly = readOnly,
                        )
                        SuggestionTextField(
                            value = end.typeName,
                            onValueChange = { end.typeName = it },
                            label = { Text(strings.endTypeName) },
                            suggestions = endTypeSuggestions,
                            error = issueOf(ConnectionField.END_TYPE_NAME, end)?.message,
                            chooseLabel = strings.choose,
                            modifier = Modifier.weight(1f),
                            readOnly = readOnly,
                        )
                        if (!readOnly) {
                            TextButton(onClick = { connection.removeEnd(end) }) { Text("✕") }
                        }
                    }
                    CompletionTextField(
                        value = end.expression,
                        onValueChange = { end.expression = it },
                        label = { Text(strings.endExpression) },
                        suggestions = state.expressionSuggestions(connection, end),
                        error = issueOf(ConnectionField.END_EXPRESSION, end)?.message,
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = readOnly,
                    )
                }
            }
        }
        if (!readOnly) {
            OutlinedButton(onClick = { connection.addEnd() }) { Text("+ ${strings.addEnd}") }
        }
        // issues not shown next to a field
        issues.filter { (it.location as IssueLocation.Connection).field == null }.forEach { IssueText(it) }
        // a field can have more than one issue, the others are shown here
        issues.filter { i ->
            val l = i.location as IssueLocation.Connection
            null != l.field && issueOf(l.field, l.end) != i
        }.forEach { IssueText(it) }
    }
}
