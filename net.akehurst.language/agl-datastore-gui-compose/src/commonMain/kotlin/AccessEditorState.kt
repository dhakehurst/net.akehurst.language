package net.akehurst.datastore.gui.compose

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import net.akehurst.datastore.api.DatastoreCompositionConfiguration
import kotlin.coroutines.cancellation.CancellationException
import net.akehurst.datastore.api.AccessCompositionConfiguration
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.common.AccessCompositionConfigurationDefault

/** the type of [EditorCallbacks.preview] */
typealias PreviewCallback = suspend (composition: DatastoreCompositionConfiguration, access: AccessCompositionConfiguration, upTo: DatastoreIdentity, limit: Int) -> PreviewResult

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

    private val _preview = mutableStateMapOf<PartDraft, PreviewStatus>()

    /** the configurations each preview was run with, so that a result is dropped when they change */
    private val _previewKey = mutableMapOf<PartDraft, Any?>()

    /** what a preview depends on: the composition and access configurations, or null while they have errors */
    private fun configurationKey(): Any? = build()?.let { a ->
        listOf(
            a.datastoreConfiguration.part.map { (k, v) -> Pair(k, v.argument) },
            a.datastoreConfiguration.connection,
            a.part.map { (k, v) -> Pair(k, v.argument) },
        )
    }

    /** the state of the preview up to 'part'; a result is dropped (Idle) once a configuration has changed since it was run */
    fun previewStatus(part: PartDraft): PreviewStatus {
        val s = _preview[part] ?: return PreviewStatus.Idle
        return if (s is PreviewStatus.Done && _previewKey[part] != configurationKey()) PreviewStatus.Idle else s
    }

    /** forgets the preview up to 'part' (e.g. when it is cancelled, or a configuration changes) */
    fun clearPreview(part: PartDraft) {
        _preview.remove(part)
        _previewKey.remove(part)
    }

    /** a preview up to 'part' can be run: there is a callback, and the composition and access configurations have no errors */
    fun canPreview(part: PartDraft, preview: PreviewCallback?): Boolean = null != preview && part in composition.parts && null != build()

    /**
     * runs 'preview' through the chain up to 'part' for at most 'limit' items, and sets the preview state: running, then the result.
     * An exception thrown by 'preview' gives a [PreviewResult.Failure].
     */
    suspend fun preview(part: PartDraft, preview: PreviewCallback?, limit: Int) {
        val access = build()
        if (null == preview || null == access || part !in composition.parts) return
        _preview[part] = PreviewStatus.Running
        val key = configurationKey()
        val result = try {
            preview(access.datastoreConfiguration, access, DatastoreIdentity(part.id), limit)
        } catch (ex: CancellationException) {
            _preview.remove(part)
            throw ex
        } catch (ex: Throwable) {
            PreviewResult.Failure(ex.message ?: ex::class.simpleName ?: "error", ex)
        }
        if (part in composition.parts) {
            _previewKey[part] = key
            _preview[part] = PreviewStatus.Done(result)
        }
    }

    /** the access configuration of the composition, or null while it, or the composition, has errors */
    fun build(): AccessCompositionConfiguration? {
        val cfg = composition.build() ?: return null
        if (hasErrors) return null
        val part = composition.parts.associate { p -> Pair(DatastoreIdentity(p.id), accessConfiguration(p) ?: return null) }
        return AccessCompositionConfigurationDefault(cfg, part)
    }
}
