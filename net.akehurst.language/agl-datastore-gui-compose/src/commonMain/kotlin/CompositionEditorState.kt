package net.akehurst.datastore.gui.compose

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.akehurst.datastore.api.DatastoreCompositionConfiguration
import net.akehurst.datastore.api.DatastoreCompositionIdentity
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
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.types.api.DataType
import net.akehurst.language.types.api.TypeDefinition
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import kotlin.coroutines.cancellation.CancellationException

/** One part (datastore) of the composition being edited. */
class PartDraft(
    id: String,
    val provider: DatastoreProvider,
) {
    var id by mutableStateOf(id)
    val datastore = ConfigurationDraft(provider.identity, provider.schema.datastore)
    val providerIdentity: DatastoreProviderIdentity get() = provider.identity
}

/**
 * One end of a connection being edited: `end(name, typeName, expression)`.
 * @param prefilled true for an end made by pre-fill; changing its name, type or expression clears it
 */
class EndDraft(name: String = "", typeName: String = "", expression: String = "", prefilled: Boolean = false) {
    private var _name by mutableStateOf(name)
    private var _typeName by mutableStateOf(typeName)
    private var _expression by mutableStateOf(expression)

    /** true while the end is as pre-fill made it; such an end is replaced when the target type changes */
    var prefilled by mutableStateOf(prefilled)
        private set

    var name: String
        get() = _name
        set(value) {
            if (value != _name) { _name = value; prefilled = false }
        }
    var typeName: String
        get() = _typeName
        set(value) {
            if (value != _typeName) { _typeName = value; prefilled = false }
        }
    var expression: String
        get() = _expression
        set(value) {
            if (value != _expression) { _expression = value; prefilled = false }
        }
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

    fun addEnd(name: String = "", typeName: String = "", expression: String = "", prefilled: Boolean = false): EndDraft =
        EndDraft(name, typeName, expression, prefilled).also { ends.add(it) }

    fun removeEnd(end: EndDraft) {
        ends.remove(end)
    }
}

/**
 * A move of a part, as done by [CompositionEditorState.movePart].
 * @param removedConnections the connections that no longer joined neighbouring parts, with their index in the connections list
 */
class PartMove(
    val from: Int,
    val to: Int,
    val removedConnections: List<Pair<Int, ConnectionDraft>>,
)

/**
 * The state of a DatastoreCompositionEditor: the parts and connections being edited, and their issues.
 * The host application creates it, keeps it as long as it likes, and reads the result with [build].
 *
 * @param registry the providers that parts can be made from are those registered in it
 * @param secretSourceIds the identities of the secret sources a sensitive parameter can refer to
 * @param initial a composition configuration to edit; its providers must be registered in 'registry'
 * @param suggestedIdentity the starting identity of a new composition (no 'initial'), e.g. suggested by the host; ignored with 'initial'
 */
class CompositionEditorState(
    val registry: DatastoreProviderRegistry = DatastoreProviderRegistryDefault.GLOBAL,
    secretSourceIds: List<String> = emptyList(),
    initial: DatastoreCompositionConfiguration? = null,
    suggestedIdentity: String = "",
) {
    /** uses a registry of just 'providers' */
    constructor(
        providers: List<DatastoreProvider>,
        secretSourceIds: List<String> = emptyList(),
        initial: DatastoreCompositionConfiguration? = null,
        suggestedIdentity: String = "",
    ) : this(DatastoreProviderRegistryDefault().also { r -> providers.forEach { r.register(it) } }, secretSourceIds, initial, suggestedIdentity)

    private var _identity by mutableStateOf(suggestedIdentity)

    /**
     * true only for a new composition (no 'initial', nothing loaded): its identity is chosen once, when it is created.
     * The identity of a loaded composition is read-only.
     */
    var identityEditable: Boolean by mutableStateOf(true)
        private set

    /**
     * the identity of the composition (see DatastoreCompositionIdentity.isValid); it can be set only while [identityEditable].
     * Uniqueness among the owner's compositions is checked by the host.
     */
    var identity: String
        get() = _identity
        set(value) {
            check(identityEditable) { "The identity of a loaded composition cannot be changed" }
            _identity = value
        }

    /** a readable name; blank means none */
    var name: String by mutableStateOf("")

    /** what the composition is for */
    var description: String by mutableStateOf("")

    /** the providers registered in 'registry' */
    val providers: List<DatastoreProvider> get() = registry.providers
    val secretSourceIds: List<String> = secretSourceIds.toList()

    /** the parts, in chain order */
    val parts = mutableStateListOf<PartDraft>()

    val connections = mutableStateListOf<ConnectionDraft>()

    private val _types = mutableStateMapOf<PartDraft, TypesStatus>()

    /** bumped by [retryTypes], so the editor asks for the types again */
    private val _typesRequest = mutableStateMapOf<PartDraft, Int>()

    /** issues found when loading 'initial' (e.g. a provider that is not available) */
    private val _loadIssues = mutableStateListOf<EditorIssue>()

    private val _validator = CompositionValidator(this)

    /** all issues, recomputed when anything they depend on is edited */
    val issues: List<EditorIssue> by derivedStateOf { _loadIssues + _validator.validate() }

    val hasErrors: Boolean get() = issues.any { it.severity == IssueSeverity.ERROR }

    init {
        initial?.let { load(it) }
    }

    private companion object {
        const val SELF = "\$self"
        const val OWNER_END = "owner"
        const val COMPOSITION_NAMESPACE = "composition"
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
        _typesRequest.remove(part)
        parts.remove(part)
    }

    /**
     * moves a part in the chain, and removes the connections that then no longer join neighbouring parts.
     * Returns the move (with the removed connections) for [undoMove], or null if nothing was moved.
     */
    fun movePart(from: Int, to: Int): PartMove? {
        if (from == to || from !in parts.indices || to !in parts.indices) return null
        val p = parts.removeAt(from)
        parts.add(to, p)
        val removed = connections.withIndex().filter { (_, c) -> nextPart(c.source) != c.target }.map { Pair(it.index, it.value) }
        removed.asReversed().forEach { (i, _) -> connections.removeAt(i) }
        return PartMove(from, to, removed)
    }

    /** undoes 'move': restores the order of the parts and the removed connections */
    fun undoMove(move: PartMove) {
        if (move.to !in parts.indices || move.from !in parts.indices) return
        val p = parts.removeAt(move.to)
        parts.add(move.from, p)
        move.removedConnections.forEach { (i, c) ->
            if (c.source in parts && c.target in parts) connections.add(i.coerceAtMost(connections.size), c)
        }
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

    /** sets the types of a part, or clears them with null */
    fun setTypes(part: PartDraft, types: TypesDomain?) {
        if (null == types) _types.remove(part) else _types[part] = TypesResult.Found(types)
    }

    fun setTypesStatus(part: PartDraft, status: TypesStatus) {
        _types[part] = status
    }

    /** the state of the types of 'part'; [TypesStatus.Loading] until they have been asked for */
    fun typesStatus(part: PartDraft): TypesStatus = _types[part] ?: TypesStatus.Loading

    fun typesOrNull(part: PartDraft): TypesDomain? = (_types[part] as? TypesResult.Found)?.types

    /** changes each time [retryTypes] is called for 'part' */
    fun typesRequestKey(part: PartDraft): Int = _typesRequest[part] ?: 0

    /** asks for the types of 'part' again */
    fun retryTypes(part: PartDraft) {
        _typesRequest[part] = typesRequestKey(part) + 1
    }

    /**
     * asks 'typesFor' for the types of 'part' and sets its types status: loading, then the result.
     * Without a callback, or while the configuration has errors, the types are not available.
     */
    suspend fun requestTypes(
        part: PartDraft,
        typesFor: (suspend (partId: DatastoreIdentity, provider: DatastoreProviderIdentity, datastore: DatastoreConfiguration) -> TypesResult)?,
    ) {
        val cfg = partConfiguration(part)
        val result = when {
            null == typesFor -> TypesResult.NotAvailable("no typesFor callback")
            null == cfg -> TypesResult.NotAvailable("the configuration has errors")
            else -> {
                setTypesStatus(part, TypesStatus.Loading)
                try {
                    typesFor(DatastoreIdentity(part.id), part.providerIdentity, cfg)
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: Throwable) {
                    TypesResult.NotAvailable(ex.message ?: ex::class.simpleName ?: "error")
                }
            }
        }
        // the part may have been removed while waiting
        if (part in parts) setTypesStatus(part, result)
    }

    /** the number of type definitions of 'types', not counting those of the std namespace */
    fun typesCount(types: TypesDomain): Int = types.namespace.filter { it.qualifiedName != StdLibDefault.qualifiedName }.sumOf { it.ownedTypes.size }

    /** the data types of 'part': those of its types, except the std and composition namespaces */
    fun dataTypesOf(part: PartDraft): List<DataType> =
        typesOrNull(part)?.namespace
            ?.filter { it.qualifiedName.value != StdLibDefault.qualifiedName.value && it.qualifiedName.value != COMPOSITION_NAMESPACE }
            ?.flatMap { it.dataType }
            ?.sortedBy { it.qualifiedName.value }
            ?: emptyList()

    /**
     * sets the target type of 'connection'. When it changes and the target part's types are known,
     * the ends still marked pre-filled (except `owner`) are removed and the ends are pre-filled again for the new type ([prefillEnds]).
     * While the target part's types are not known, the ends are kept as they are.
     */
    fun setTargetType(connection: ConnectionDraft, targetType: String) {
        if (targetType == connection.targetType) return
        connection.targetType = targetType
        if (null == typesOrNull(connection.target)) return
        connection.ends.removeAll { it.prefilled && it.name != OWNER_END }
        prefillEnds(connection)
    }

    /**
     * pre-fills the ends of 'connection' from the types; the ends made are marked [EndDraft.prefilled]. Ends that are kept are never changed.
     * - an end `owner = $self` whose type is the data type of the source part's items, if there is exactly one, otherwise left empty to be chosen.
     *   It is not added if there is already an end named `owner`; a still pre-filled `owner` whose type differs from the one now found is replaced.
     * - an end per constructor parameter of the target type, with an empty expression, unless an end with that name is already there.
     * Returns true if an end was added or replaced.
     */
    fun prefillEnds(connection: ConnectionDraft): Boolean {
        val target = typesOrNull(connection.target)?.let { findType(it, connection.targetType) } as? DataType ?: return false
        var changed = false
        val itemTypes = (typesStatus(connection.source) as? TypesResult.Found)?.itemTypes ?: emptyList()
        val ownerCandidates = itemTypes.ifEmpty { dataTypesOf(connection.source).map { it.qualifiedName.value } }.distinct()
        val ownerType = ownerCandidates.singleOrNull() ?: ""
        val owner = connection.ends.firstOrNull { it.name == OWNER_END }
        when {
            null == owner -> {
                connection.ends.add(0, EndDraft(OWNER_END, ownerType, SELF, prefilled = true))
                changed = true
            }
            // the source's types may not be known; then a pre-filled owner is kept
            owner.prefilled && owner.typeName != ownerType && null != typesOrNull(connection.source) -> {
                connection.ends[connection.ends.indexOf(owner)] = EndDraft(OWNER_END, ownerType, SELF, prefilled = true)
                changed = true
            }
        }
        target.constructors.firstOrNull()?.parameters?.forEach { p ->
            if (connection.ends.none { it.name == p.name.value }) {
                connection.addEnd(p.name.value, typeNameOf(p.typeInstance), "", prefilled = true)
                changed = true
            }
        }
        return changed
    }

    /**
     * suggestions for the expression of 'end': the property paths of the type of the connection's `$self` end,
     * one segment at a time (the part before the last '.' of what is typed is followed, then that type's properties),
     * those that start with what is typed. Paths whose type conforms to the end's type come first.
     */
    fun expressionSuggestions(connection: ConnectionDraft, end: EndDraft): List<String> {
        val sourceTypes = typesOrNull(connection.source) ?: return emptyList()
        val selfEnd = connection.ends.firstOrNull { it.expression.trim() == SELF }
        if (null == selfEnd || selfEnd == end) return emptyList()
        val selfType = findType(sourceTypes, selfEnd.typeName) ?: return emptyList()
        val typed = end.expression.trim()
        val selfPrefix = if (typed.startsWith("$SELF.")) "$SELF." else ""
        val path = typed.removePrefix(selfPrefix)
        val lastDot = path.lastIndexOf('.')
        val head = if (lastDot < 0) "" else path.substring(0, lastDot)
        val last = path.substring(lastDot + 1)
        var t: TypeInstance = selfType.type()
        if (head.isNotEmpty()) {
            for (seg in head.split('.')) {
                t = t.allResolvedProperty.entries.firstOrNull { it.key.value == seg }?.value?.typeInstance ?: return emptyList()
            }
        }
        val endType = findType(sourceTypes, end.typeName) ?: typesOrNull(connection.target)?.let { findType(it, end.typeName) }
        fun conforms(ti: TypeInstance): Boolean {
            val n = end.typeName.trim()
            if (n == "Any" || n == "std.Any") return true
            val d = ti.resolvedDefinitionOrNull ?: return false
            return when (endType) {
                null -> n.isNotEmpty() && (d.qualifiedName.value == n || d.name.value == n)
                else -> d.conformsTo(endType)
            }
        }
        val prefix = selfPrefix + (if (head.isEmpty()) "" else "$head.")
        return t.allResolvedProperty.values
            .filter { it.name.value.startsWith(last) }
            .map { Pair(prefix + it.name.value, conforms(it.typeInstance)) }
            .sortedWith(compareBy<Pair<String, Boolean>> { !it.second }.thenBy { it.first })
            .map { it.first }
    }

    private fun findType(types: TypesDomain, typeName: String): TypeDefinition? =
        if (typeName.isBlank()) null else try {
            types.findFirstDefinitionByPossiblyQualifiedNameOrNull(typeName.trim().asPossiblyQualifiedName)
        } catch (_: Exception) {
            null
        }

    /** the name of a type as given in an end: simple for std types (e.g. String), otherwise qualified */
    private fun typeNameOf(ti: TypeInstance): String {
        val d = try { ti.resolvedDefinitionOrNull } catch (_: Exception) { null } ?: return ti.typeName.value
        return if (d.namespace.qualifiedName == StdLibDefault.qualifiedName) d.name.value else d.qualifiedName.value
    }

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
        return DatastoreCompositionConfigurationDefault(DatastoreCompositionIdentity(identity), part, conns, name, description)
    }

    /** replaces what is being edited with 'configuration'; its identity is then read-only ([identityEditable] is false) */
    fun load(configuration: DatastoreCompositionConfiguration) {
        _identity = configuration.identity.value
        identityEditable = false
        name = configuration.name
        description = configuration.description
        connections.clear()
        parts.clear()
        _types.clear()
        _typesRequest.clear()
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
