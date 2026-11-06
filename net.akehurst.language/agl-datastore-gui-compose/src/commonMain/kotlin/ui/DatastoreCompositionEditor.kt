package net.akehurst.datastore.gui.compose.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.gui.compose.CompositionEditorState
import net.akehurst.datastore.gui.compose.ConnectionDraft
import net.akehurst.datastore.gui.compose.EditorCallbacks
import net.akehurst.datastore.gui.compose.EditorCustomisation
import net.akehurst.datastore.gui.compose.EditorIssue
import net.akehurst.datastore.gui.compose.IssueLocation
import net.akehurst.datastore.gui.compose.IssueSeverity
import net.akehurst.datastore.gui.compose.PartDraft

/** What is shown in the detail pane. */
internal sealed interface Selection {
    data class Part(val part: PartDraft) : Selection
    data class Connection(val connection: ConnectionDraft) : Selection
}

/** windows narrower than this show the chain and the selected item's form in one column */
private val TWO_PANE_MIN_WIDTH = 600.dp

/**
 * Edits a datastore composition: its parts, their datastore configurations, and the connections between them.
 * The result is read from 'state' (state.build()). The editor is UI only; 'callbacks' supply its behaviour.
 */
@Composable
fun DatastoreCompositionEditor(
    state: CompositionEditorState,
    modifier: Modifier = Modifier,
    callbacks: EditorCallbacks = EditorCallbacks(),
    customisation: EditorCustomisation = EditorCustomisation.Default,
) {
    val strings = customisation.strings
    var selection by remember(state) { mutableStateOf<Selection?>(null) }
    // a selection whose part or connection was removed is cleared
    val current = selection?.takeIf {
        when (it) {
            is Selection.Part -> it.part in state.parts
            is Selection.Connection -> it.connection in state.connections
        }
    }

    // ask the host for the types of each part whose configuration is valid
    state.parts.forEach { part ->
        val cfg = state.partConfiguration(part)
        LaunchedEffect(part, part.id, cfg?.argument, callbacks.typesFor) {
            val typesFor = callbacks.typesFor
            state.setTypes(part, if (null == cfg || null == typesFor) null else runCatching { typesFor(DatastoreIdentity(part.id), part.providerIdentity, cfg) }.getOrNull())
        }
    }

    val select: (Selection) -> Unit = { selection = it }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(strings.composition, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            customisation.toolbarActions(this)
        }
        HorizontalDivider()
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            if (maxWidth >= TWO_PANE_MIN_WIDTH) {
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.width(280.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(8.dp)) {
                        ChainList(state, current, select, customisation)
                    }
                    VerticalDivider()
                    Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(12.dp)) {
                        Detail(state, current, callbacks, customisation)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
                    ChainList(state, current, select, customisation)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Detail(state, current, callbacks, customisation)
                }
            }
        }
        HorizontalDivider()
        IssueList(state.issues, customisation, Modifier.padding(8.dp)) { loc ->
            when (loc) {
                is IssueLocation.Part -> selection = Selection.Part(loc.part)
                is IssueLocation.PartId -> selection = Selection.Part(loc.part)
                is IssueLocation.Connection -> selection = Selection.Connection(loc.connection)
                is IssueLocation.Access -> selection = Selection.Part(loc.part)
                IssueLocation.Composition -> Unit
            }
        }
    }
}

@Composable
private fun Detail(state: CompositionEditorState, selection: Selection?, callbacks: EditorCallbacks, customisation: EditorCustomisation) {
    when (selection) {
        null -> Text(customisation.strings.nothingSelected, style = MaterialTheme.typography.bodyMedium)
        is Selection.Part -> PartDetail(state, selection.part, callbacks, customisation)
        is Selection.Connection -> ConnectionDetail(state, selection.connection, customisation)
    }
}

/** The parts in chain order, with the connections between neighbouring parts, and a way to add a part. */
@Composable
private fun ChainList(state: CompositionEditorState, selection: Selection?, select: (Selection) -> Unit, customisation: EditorCustomisation) {
    val strings = customisation.strings
    val issues = state.issues
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(strings.chain, style = MaterialTheme.typography.titleSmall)
        state.parts.forEachIndexed { index, part ->
            val hasError = issues.any { it.severity == IssueSeverity.ERROR && it.location.partOrNull() == part }
            val presentation = customisation.providerPresentation(part.providerIdentity)
            ChainRow(
                selected = (selection as? Selection.Part)?.part == part,
                hasError = hasError,
                onClick = { select(Selection.Part(part)) },
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${index + 1}. ${part.id.ifBlank { "?" }}", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        presentation.icon?.let { it(); Spacer(Modifier.width(4.dp)) }
                        Text(presentation.displayName, style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(onClick = { state.movePart(index, index - 1) }, enabled = index > 0) { Text("↑") }
                TextButton(onClick = { state.movePart(index, index + 1) }, enabled = index < state.parts.size - 1) { Text("↓") }
                TextButton(onClick = { state.removePart(part) }) { Text("✕") }
            }
            // connections from this part (to the next part, or wrongly to another part)
            state.connectionsFrom(part).forEach { c ->
                val cHasError = issues.any { it.severity == IssueSeverity.ERROR && (it.location as? IssueLocation.Connection)?.connection == c }
                ChainRow(
                    selected = (selection as? Selection.Connection)?.connection == c,
                    hasError = cHasError,
                    onClick = { select(Selection.Connection(c)) },
                    indent = true,
                ) {
                    Text("↓ ${c.identity.ifBlank { "?" }} : ${c.targetType.ifBlank { "?" }}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { state.removeConnection(c) }) { Text("✕") }
                }
            }
            state.nextPart(part)?.let { next ->
                TextButton(onClick = { select(Selection.Connection(state.addConnection(part, next))) }, modifier = Modifier.padding(start = 16.dp)) {
                    Text("+ ${strings.addConnection}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        AddPart(state, customisation) { select(Selection.Part(it)) }
    }
}

@Composable
private fun ChainRow(
    selected: Boolean,
    hasError: Boolean,
    onClick: () -> Unit,
    indent: Boolean = false,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val colors = when {
        selected -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        else -> CardDefaults.cardColors()
    }
    Card(colors = colors, modifier = Modifier.fillMaxWidth().padding(start = if (indent) 16.dp else 0.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (hasError) {
                Text("!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
            }
            content()
        }
    }
}

/** Choose a provider and give an id, then add the part at the end of the chain. */
@Composable
private fun AddPart(state: CompositionEditorState, customisation: EditorCustomisation, onAdded: (PartDraft) -> Unit) {
    val strings = customisation.strings
    var open by remember { mutableStateOf(false) }
    var provider by remember { mutableStateOf<DatastoreProvider?>(null) }
    var id by remember { mutableStateOf("") }
    if (!open) {
        OutlinedButton(onClick = { open = true }, enabled = state.providers.isNotEmpty()) { Text("+ ${strings.addPart}") }
    } else {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.provider, style = MaterialTheme.typography.bodySmall)
                    val p = provider
                    ChoiceButton(
                        current = p?.let { customisation.providerPresentation(it.identity).displayName } ?: strings.choose,
                        options = state.providers,
                        optionLabel = { customisation.providerPresentation(it.identity).displayName },
                        onChoose = { provider = it },
                    )
                    p?.let { HelpIcon(customisation.providerPresentation(it.identity).description, strings.help) }
                }
                val idError = when {
                    id.isNotEmpty() && state.parts.any { it.id == id } -> "'$id' is already used"
                    else -> null
                }
                TextFieldWithError(value = id, onValueChange = { id = it }, label = { Text(strings.partId) }, error = idError, modifier = Modifier.fillMaxWidth())
                Row {
                    TextButton(onClick = { open = false; provider = null; id = "" }) { Text(strings.cancel) }
                    TextButton(
                        onClick = {
                            val p = provider ?: return@TextButton
                            onAdded(state.addPart(p.identity, id))
                            open = false; provider = null; id = ""
                        },
                        enabled = null != provider && id.isNotBlank() && null == idError,
                    ) { Text(strings.add) }
                }
            }
        }
    }
}

@Composable
private fun PartDetail(state: CompositionEditorState, part: PartDraft, callbacks: EditorCallbacks, customisation: EditorCustomisation) {
    val strings = customisation.strings
    val issues = state.issues
    val presentation = customisation.providerPresentation(part.providerIdentity)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${strings.provider}: ${presentation.displayName}", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(4.dp))
            HelpIcon(presentation.description, strings.help)
        }
        val idIssue = issues.firstOrNull { (it.location as? IssueLocation.PartId)?.part == part }
        TextFieldWithError(
            value = part.id,
            onValueChange = { part.id = it },
            label = { Text(strings.partId) },
            error = idIssue?.message,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(strings.datastoreConfiguration, style = MaterialTheme.typography.titleSmall)
        val errors = issues.mapNotNull { i -> (i.location as? IssueLocation.Part)?.takeIf { it.part == part }?.parameterIdentity?.let { Pair(it, i.message) } }.toMap()
        ParameterForm(part.datastore, errors, state.secretSourceIds, customisation)
        val cfg = state.partConfiguration(part)
        val testConnection = callbacks.testConnection
        TestButton(
            label = strings.testConnection,
            test = if (null == testConnection || null == cfg) null else ({ testConnection(DatastoreIdentity(part.id), part.providerIdentity, cfg) }),
            enabled = null != cfg,
            strings = strings,
            resetKey = cfg?.argument,
        )
        val partIssues = issues.filter { (it.location as? IssueLocation.Part)?.let { l -> l.part == part && null == l.parameterIdentity } == true }
        partIssues.forEach { IssueText(it) }
    }
}

@Composable
internal fun IssueText(issue: EditorIssue) {
    Text(
        issue.message,
        color = if (issue.severity == IssueSeverity.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
        style = MaterialTheme.typography.bodySmall,
    )
}

/** All issues, each of which can be clicked to select where it is. */
@Composable
internal fun IssueList(issues: List<EditorIssue>, customisation: EditorCustomisation, modifier: Modifier = Modifier, onSelect: (IssueLocation) -> Unit) {
    val strings = customisation.strings
    Column(modifier) {
        val errors = issues.count { it.severity == IssueSeverity.ERROR }
        val warnings = issues.size - errors
        Text(
            if (issues.isEmpty()) strings.noIssues else "${strings.issues}: $errors error(s), $warnings warning(s)",
            style = MaterialTheme.typography.titleSmall
        )
        issues.forEach { issue ->
            Row(Modifier.fillMaxWidth().clickable { onSelect(issue.location) }.padding(vertical = 2.dp)) {
                Text("${where(issue.location)}: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                IssueText(issue)
            }
        }
    }
}

private fun where(location: IssueLocation): String = when (location) {
    IssueLocation.Composition -> "composition"
    is IssueLocation.Part -> listOfNotNull(location.part.id.ifBlank { "?" }, location.parameterIdentity).joinToString(".")
    is IssueLocation.PartId -> location.part.id.ifBlank { "?" }
    is IssueLocation.Connection -> "${location.connection.source.id} → ${location.connection.target.id} (${location.connection.identity.ifBlank { "?" }})"
    is IssueLocation.Access -> listOfNotNull(location.part.id.ifBlank { "?" }, "access", location.parameterIdentity).joinToString(".")
}

private fun IssueLocation.partOrNull(): PartDraft? = when (this) {
    is IssueLocation.Part -> part
    is IssueLocation.PartId -> part
    else -> null
}
