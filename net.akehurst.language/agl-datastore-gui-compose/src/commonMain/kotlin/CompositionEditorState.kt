package net.akehurst.datastore.gui.compose

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.akehurst.datastore.api.DatastoreCompositionConfiguration
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreConnection
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProvider
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.DatastoreProviderRegistry
import net.akehurst.datastore.common.DatastoreProviderRegistryDefault
import net.akehurst.datastore.common.DatastoreCompositionConfigurationDefault
import net.akehurst.datastore.common.DatastoreConnectionDefault
import net.akehurst.datastore.common.DatastoreConnectionEndDefault
import net.akehurst.language.types.api.TypesDomain

/** One part (datastore) of the composition being edited. */
class PartDraft(
    id: String,
    val provider: DatastoreProvider,
) {
    var id by mutableStateOf(id)
    val datastore = ConfigurationDraft(provider.identity, provider.schema.datastore)
    val providerIdentity: DatastoreProviderIdentity get() = provider.identity
}

/** One end of a connection being edited: `end(name, typeName, expression)`. */
class EndDraft(name: String = "", typeName: String = "", expression: String = "") {
    var name by mutableStateOf(name)
    var typeName by mutableStateOf(typeName)
    var expression by mutableStateOf(expression)
}

/** A connection being edited, from the part 'source' to the next part 'target'. */
class ConnectionDraft(
    source: PartDraft,
    target: PartDraft,
    targetType: String = "",
    name: String = "",
) {
    var source by mutableStateOf(source)
    var target by mutableStateOf(target)
    var targetType by mutableStateOf(targetType)

    /** empty for no name */
    var name by mutableStateOf(name)
    val ends = mutableStateListOf<EndDraft>()

    /** as DatastoreConnection.identity */
    val identity: String
        get() = name.ifBlank { targetType.substringAfterLast('.').replaceFirstChar { it.lowercaseChar() } }

    fun addEnd(name: String = "", typeName: String = "", expression: String = ""): EndDraft =
        EndDraft(name, typeName, expression).also { ends.add(it) }

    fun removeEnd(end: EndDraft) {
        ends.remove(end)
    }
}

/**
 * The state of a DatastoreCompositionEditor: the parts and connections being edited, and their issues.
 * The host application creates it, keeps it as long as it likes, and reads the result with [build].
 *
 * @param registry the providers that parts can be made from are those registered in it
 * @param secretSourceIds the identities of the secret sources a sensitive parameter can refer to
 * @param initial a composition configuration to edit; its providers must be registered in 'registry'
 */
class CompositionEditorState(
    val registry: DatastoreProviderRegistry = DatastoreProviderRegistryDefault.GLOBAL,
    secretSourceIds: List<String> = emptyList(),
    initial: DatastoreCompositionConfiguration? = null,
) {
    /** uses a registry of just 'providers' */
    constructor(
        providers: List<DatastoreProvider>,
        secretSourceIds: List<String> = emptyList(),
        initial: DatastoreCompositionConfiguration? = null,
    ) : this(DatastoreProviderRegistryDefault().also { r -> providers.forEach { r.register(it) } }, secretSourceIds, initial)

    /** the providers registered in 'registry' */
    val providers: List<DatastoreProvider> get() = registry.providers
    val secretSourceIds: List<String> = secretSourceIds.toList()

    /** the parts, in chain order */
    val parts = mutableStateListOf<PartDraft>()

    val connections = mutableStateListOf<ConnectionDraft>()

    private val _types = mutableStateMapOf<PartDraft, TypesDomain>()

    /** issues found when loading 'initial' (e.g. a provider that is not available) */
    private val _loadIssues = mutableStateListOf<EditorIssue>()

    private val _validator = CompositionValidator(this)

    /** all issues, recomputed when anything they depend on is edited */
    val issues: List<EditorIssue> by derivedStateOf { _loadIssues + _validator.validate() }

    val hasErrors: Boolean get() = issues.any { it.severity == IssueSeverity.ERROR }

    init {
        initial?.let { load(it) }
    }

    fun providerOrNull(identity: DatastoreProviderIdentity): DatastoreProvider? = registry.getOrNull(identity)

    fun partOrNull(id: String): PartDraft? = parts.firstOrNull { it.id == id }

    /** adds a part at the end of the chain */
    fun addPart(providerIdentity: DatastoreProviderIdentity, id: String): PartDraft {
        val provider = providerOrNull(providerIdentity) ?: error("Provider '${providerIdentity.value}' is not available")
        return PartDraft(id, provider).also { parts.add(it) }
    }

    /** removes a part and every connection to or from it */
    fun removePart(part: PartDraft) {
        connections.removeAll { it.source == part || it.target == part }
        _types.remove(part)
        parts.remove(part)
    }

    /** moves a part in the chain; connections that no longer join neighbouring parts are reported as errors */
    fun movePart(from: Int, to: Int) {
        if (from == to || from !in parts.indices || to !in parts.indices) return
        val p = parts.removeAt(from)
        parts.add(to, p)
    }

    /** the part after 'part' in the chain, if any */
    fun nextPart(part: PartDraft): PartDraft? = parts.indexOf(part).let { i -> if (i >= 0 && i + 1 < parts.size) parts[i + 1] else null }

    fun connectionsFrom(part: PartDraft): List<ConnectionDraft> = connections.filter { it.source == part }

    fun addConnection(source: PartDraft, target: PartDraft, targetType: String = "", name: String = ""): ConnectionDraft =
        ConnectionDraft(source, target, targetType, name).also { connections.add(it) }

    fun removeConnection(connection: ConnectionDraft) {
        connections.remove(connection)
    }

    /** the part's datastore configuration, or null while it has errors */
    fun partConfiguration(part: PartDraft): DatastoreConfiguration? = part.datastore.build()

    /** sets the types of a part (from the host's typesFor callback), or clears them with null */
    fun setTypes(part: PartDraft, types: TypesDomain?) {
        if (null == types) _types.remove(part) else _types[part] = types
    }

    fun typesOrNull(part: PartDraft): TypesDomain? = _types[part]

    /** the composition configuration, or null while there are errors */
    fun build(): DatastoreCompositionConfiguration? {
        if (hasErrors) return null
        val part = linkedMapOf<Pair<DatastoreProviderIdentity, DatastoreIdentity>, DatastoreConfiguration>()
        parts.forEach { p -> part[Pair(p.providerIdentity, DatastoreIdentity(p.id))] = partConfiguration(p) ?: return null }
        // connections in chain order, as the composition builder would have them
        val conns: List<DatastoreConnection> = parts.flatMap { connectionsFrom(it) }.map { c ->
            DatastoreConnectionDefault(
                source = DatastoreIdentity(c.source.id),
                target = DatastoreIdentity(c.target.id),
                targetType = c.targetType,
                end = c.ends.map { DatastoreConnectionEndDefault(it.name, it.typeName, it.expression) },
                name = c.name.ifBlank { null }
            )
        }
        return DatastoreCompositionConfigurationDefault(part, conns)
    }

    /** replaces what is being edited with 'configuration' */
    fun load(configuration: DatastoreCompositionConfiguration) {
        connections.clear()
        parts.clear()
        _types.clear()
        _loadIssues.clear()
        configuration.part.forEach { (key, cfg) ->
            val (providerId, dsId) = key
            val provider = providerOrNull(providerId)
            if (null == provider) {
                _loadIssues.add(EditorIssue(IssueSeverity.ERROR, IssueLocation.Composition, "Part '${dsId.value}': provider '${providerId.value}' is not available, the part was left out"))
            } else {
                val p = PartDraft(dsId.value, provider)
                p.datastore.load(cfg)
                parts.add(p)
            }
        }
        configuration.connection.forEach { c ->
            val s = partOrNull(c.source.value)
            val t = partOrNull(c.target.value)
            if (null == s || null == t) {
                _loadIssues.add(EditorIssue(IssueSeverity.ERROR, IssueLocation.Composition, "Connection '${c.source.value}' -> '${c.target.value}' (${c.identity}) refers to a part that was left out, so it was left out"))
            } else {
                val cd = addConnection(s, t, c.targetType, c.name ?: "")
                c.end.forEach { e -> cd.addEnd(e.name, e.typeName, e.expression) }
            }
        }
    }
}
