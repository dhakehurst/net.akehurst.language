package net.akehurst.datastore.gui.compose

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import net.akehurst.datastore.api.AccessCompositionConfiguration
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.common.AccessCompositionConfigurationDefault

/**
 * The state of an AccessConfigurationEditor: the access configuration of each part of a composition
 * (the provider's `schema.access` parameters), for the parts being edited in 'composition'.
 *
 * @param initial access configurations to edit, by part id
 */
class AccessEditorState(
    val composition: CompositionEditorState,
    initial: AccessCompositionConfiguration? = null,
) {
    // drafts are made when first asked for, so that parts added later get one; the map itself is not observed
    private val _drafts = mutableMapOf<PartDraft, ConfigurationDraft>()
    private val _initial: Map<String, DatastoreConfiguration> = initial?.part?.mapKeys { it.key.value } ?: emptyMap()

    fun draftFor(part: PartDraft): ConfigurationDraft = _drafts.getOrPut(part) {
        ConfigurationDraft(part.providerIdentity, part.provider.schema.access).also { d ->
            _initial[part.id]?.let { d.load(it) }
        }
    }

    /** the part's access configuration, or null while it has errors */
    fun accessConfiguration(part: PartDraft): DatastoreConfiguration? = draftFor(part).build()

    val issues: List<EditorIssue> by derivedStateOf {
        val issues = mutableListOf<EditorIssue>()
        composition.parts.forEach { p -> configurationIssues(draftFor(p), issues) { pid -> IssueLocation.Access(p, pid) } }
        issues
    }

    val hasErrors: Boolean get() = issues.any { it.severity == IssueSeverity.ERROR }

    /** the access configuration of the composition, or null while it, or the composition, has errors */
    fun build(): AccessCompositionConfiguration? {
        val cfg = composition.build() ?: return null
        if (hasErrors) return null
        val part = composition.parts.associate { p -> Pair(DatastoreIdentity(p.id), accessConfiguration(p) ?: return null) }
        return AccessCompositionConfigurationDefault(cfg, part)
    }
}
