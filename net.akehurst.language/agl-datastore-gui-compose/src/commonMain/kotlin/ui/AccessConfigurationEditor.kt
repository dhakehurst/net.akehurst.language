package net.akehurst.datastore.gui.compose.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.gui.compose.AccessEditorState
import net.akehurst.datastore.gui.compose.EditorCallbacks
import net.akehurst.datastore.gui.compose.EditorCustomisation
import net.akehurst.datastore.gui.compose.IssueLocation

/**
 * Edits the access configuration of each part of a composition, with a 'Test access' button per part.
 * The result is read from 'state' (state.build()).
 */
@Composable
fun AccessConfigurationEditor(
    state: AccessEditorState,
    modifier: Modifier = Modifier,
    callbacks: EditorCallbacks = EditorCallbacks(),
    customisation: EditorCustomisation = EditorCustomisation.Default,
) {
    val strings = customisation.strings
    val composition = state.composition
    val issues = state.issues
    Column(modifier.verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(strings.accessConfiguration, style = MaterialTheme.typography.titleMedium)
        composition.parts.forEach { part ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val presentation = customisation.providerPresentation(part.providerIdentity)
                    Text("${part.id.ifBlank { "?" }} (${presentation.displayName})", style = MaterialTheme.typography.titleSmall)
                    val draft = state.draftFor(part)
                    val partIssues = issues.filter { (it.location as? IssueLocation.Access)?.part == part }
                    val errors = partIssues.mapNotNull { i -> (i.location as IssueLocation.Access).parameterIdentity?.let { Pair(it, i.message) } }.toMap()
                    if (draft.parameter.isNotEmpty()) {
                        ParameterForm(draft, errors, composition.secretSourceIds, customisation)
                    }
                    val datastore = composition.partConfiguration(part)
                    val access = state.accessConfiguration(part)
                    val testAccess = callbacks.testAccess
                    TestButton(
                        label = strings.testAccess,
                        test = if (null == testAccess || null == datastore || null == access) null else ({ testAccess(DatastoreIdentity(part.id), part.providerIdentity, datastore, access) }),
                        enabled = null != datastore && null != access,
                        strings = strings,
                        resetKey = Pair(datastore?.argument, access?.argument),
                    )
                    partIssues.filter { (it.location as IssueLocation.Access).parameterIdentity == null }.forEach { IssueText(it) }
                }
            }
        }
    }
}
