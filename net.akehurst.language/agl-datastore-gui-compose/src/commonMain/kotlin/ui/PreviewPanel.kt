package net.akehurst.datastore.gui.compose.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.gui.compose.AccessEditorState
import net.akehurst.datastore.gui.compose.EditorCallbacks
import net.akehurst.datastore.gui.compose.EditorCustomisation
import net.akehurst.datastore.gui.compose.PartDraft
import net.akehurst.datastore.gui.compose.PreviewEntry
import net.akehurst.datastore.gui.compose.PreviewReader
import net.akehurst.datastore.gui.compose.PreviewResult
import net.akehurst.datastore.gui.compose.PreviewStatus
import net.akehurst.datastore.gui.compose.PreviewValue
import net.akehurst.kotlinx.issues.api.IssueKind

/**
 * A 'Preview' button that reads through the chain up to 'part' (with the host's preview callback), and the items read.
 * Disabled when there is no callback, or while the composition or access configuration has errors.
 */
@Composable
internal fun PreviewPanel(state: AccessEditorState, part: PartDraft, callbacks: EditorCallbacks, customisation: EditorCustomisation) {
    val strings = customisation.strings
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    val preview = callbacks.preview
    val status = state.previewStatus(part)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = {
                    job?.cancel()
                    job = scope.launch { state.preview(part, preview, customisation.previewLimit) }
                },
                enabled = state.canPreview(part, preview) && status != PreviewStatus.Running,
            ) { Text(strings.preview) }
            if (status == PreviewStatus.Running) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(4.dp))
                Text(strings.previewing, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { job?.cancel(); job = null; state.clearPreview(part) }) { Text(strings.cancel) }
            }
        }
        when (status) {
            PreviewStatus.Idle, PreviewStatus.Running -> Unit
            is PreviewStatus.Done -> when (val r = status.result) {
                is PreviewResult.Failure -> Text("${strings.previewFailed}: ${r.message}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                is PreviewResult.Items -> {
                    Text("${r.items.size} ${strings.items}", style = MaterialTheme.typography.bodySmall)
                    r.items.forEach { PreviewItem(it) }
                    if (r.more) Text(strings.more, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun PreviewItem(item: DataItem) {
    val id = item.qualifiedIdentity.joinToString("/") { it.value }
    val value = remember(item) { PreviewReader.valueOf(item.data) }
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        PreviewEntryRow(PreviewEntry(id, value), depth = 0, bold = true)
        item.issues.forEach { issue ->
            Text(
                issue.message,
                color = if (issue.kind == IssueKind.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

/** One entry; an object or collection is opened by a click, and only then are its properties or elements read. */
@Composable
private fun PreviewEntryRow(entry: PreviewEntry, depth: Int, bold: Boolean = false) {
    var expanded by remember(entry) { mutableStateOf(false) }
    val indent = Modifier.padding(start = (depth * 16).dp)
    val weight = if (bold) FontWeight.Bold else null
    val style = MaterialTheme.typography.bodySmall
    when (val v = entry.value) {
        is PreviewValue.Text -> Text("${entry.name}: ${v.text}", style = style, fontWeight = weight, modifier = indent)
        is PreviewValue.Error -> Text("${entry.name}: ${v.message}", style = style, fontWeight = weight, color = MaterialTheme.colorScheme.error, modifier = indent)
        is PreviewValue.Structure, is PreviewValue.Collection -> {
            val label = when (v) {
                is PreviewValue.Collection -> "${entry.name} : ${v.typeName} [${v.size}]"
                is PreviewValue.Structure -> "${entry.name} : ${v.typeName}"
                else -> entry.name
            }
            Text("${if (expanded) "▾" else "▸"} $label", style = style, fontWeight = weight, modifier = indent.clickable { expanded = !expanded })
            if (expanded) {
                val children by produceState<List<PreviewEntry>?>(null, v) {
                    value = when (v) {
                        is PreviewValue.Structure -> PreviewReader.propertiesOf(v.obj)
                        is PreviewValue.Collection -> PreviewReader.elementsOf(v.obj)
                        else -> emptyList()
                    }
                }
                when (val c = children) {
                    null -> Text("…", style = style, modifier = Modifier.padding(start = ((depth + 1) * 16).dp))
                    else -> c.forEach { PreviewEntryRow(it, depth + 1) }
                }
            }
        }
    }
}
