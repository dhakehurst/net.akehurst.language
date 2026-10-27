package net.akehurst.datastore.api

import kotlinx.coroutines.flow.Flow
import net.akehurst.kotlinx.issues.api.IssueCollection
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.TypeDefinition
import net.akehurst.language.types.api.TypesDomain
import kotlin.jvm.JvmInline

@JvmInline
value class DatastoreProviderIdentity(val value:String)

@JvmInline
value class DatastoreIdentity(val value:String)

@JvmInline
value class DataItemIdentity(val value:String)

/**
 * Where a DataItem came from.
 * Each Datastore implementation defines its own provenance by implementing this interface.
 */
interface DataItemProvenance

/**
 * Where, within a DataItem, an issue was found (e.g. a position in parsed text, or a spreadsheet cell).
 * Each Datastore implementation defines its own issue location by implementing this interface.
 */
interface DataItemIssueLocation

/**
 * A unit of data passed to and from Datastore read/write.
 */
interface DataItem {
    /** the item this item was derived from, if any */
    val parent: DataItem?
    val identity: DataItemIdentity

    /** the identities of the parents (outermost first) followed by this item's identity */
    val qualifiedIdentity: List<DataItemIdentity> get() = (parent?.qualifiedIdentity ?: emptyList()) + identity

    val data: TypedObject
    val provenance: DataItemProvenance

    /** issues found when this item was read or written, e.g. parse errors. If there are errors, 'data' may be nothing. */
    val issues: IssueCollection<DataItemIssueLocation>
}

interface DatastoreProviderRegistry {
    fun register(provider: DatastoreProvider)
    fun getOrNull(providerIdentity: DatastoreProviderIdentity): DatastoreProvider?
    fun provide(providerIdentity: DatastoreProviderIdentity, datastoreIdentity: DatastoreIdentity, datastoreConfiguration: DatastoreConfiguration): Datastore
}

sealed interface DsConfigParameterInfo {
    fun getParameterOrNull(parameterIdentity: String): DsConfigParameterDefinition?
}

interface DsConfigParameterDefinition : DsConfigParameterInfo{
    val identity: String
    val name: String
    val optional: Boolean
    val description:String
    val type: TypeDefinition

    override fun getParameterOrNull(parameterIdentity: String): DsConfigParameterDefinition? = when (parameterIdentity) {
        identity -> this
        else -> null
    }

}

interface DsConfigParameterGroup : DsConfigParameterInfo {
    val name:String
    val parameterInfo: List<DsConfigParameterInfo>

    override fun getParameterOrNull(parameterIdentity: String): DsConfigParameterDefinition? =
        parameterInfo.firstNotNullOfOrNull { pi ->
            pi.getParameterOrNull(parameterIdentity)
        }
}

interface DsConfigParameterChoice : DsConfigParameterInfo {
    val alternative: Map<String, DsParameterSet>

    override fun getParameterOrNull(parameterIdentity: String): DsConfigParameterDefinition? =
        alternative.firstNotNullOfOrNull { alt ->
            alt.value.getParameterOrNull(parameterIdentity)
        }
}

interface DsParameterSet {
    val parameterInfo: List<DsConfigParameterInfo>

    fun getParameterOrNull(parameterIdentity: String): DsConfigParameterDefinition?
}

interface DatastoreProviderSchema {
    val datastore: DsParameterSet
    val access:DsParameterSet

}

interface DatastoreProvider {
    val identity:DatastoreProviderIdentity
    val configuration: DatastoreProviderSchema

    fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration):Datastore
}

interface DatastoreConfiguration {

    val providerIdentity:DatastoreProviderIdentity

    val argument: Map<String, Any>

    fun getAsString(parameterIdentity:String): String = argument[parameterIdentity] as String
    fun getAsStringOrNull(parameterIdentity:String): String? = argument[parameterIdentity] as String?

    fun getAsInt(parameterIdentity: String): Int = (argument[parameterIdentity] as Number).toInt()
    fun getAsIntOrNull(parameterIdentity: String): Int? = (argument[parameterIdentity] as Number?)?.toInt()

    fun getAsLong(parameterIdentity: String): Long = (argument[parameterIdentity] as Number).toLong()
    fun getAsLongOrNull(parameterIdentity: String): Long? = (argument[parameterIdentity] as Number?)?.toLong()

    fun getAsDouble(parameterIdentity: String): Double = (argument[parameterIdentity] as Number).toDouble()
    fun getAsDoubleOrNull(parameterIdentity: String): Double? = (argument[parameterIdentity] as Number?)?.toDouble()

    fun getAsBoolean(parameterIdentity: String): Boolean = argument[parameterIdentity] as Boolean
    fun getAsBooleanOrNull(parameterIdentity: String): Boolean? = argument[parameterIdentity] as Boolean?

}

interface Datastore {
    val identity:DatastoreIdentity

    val configuration:DatastoreConfiguration

    suspend fun types() : TypesDomain

    suspend fun accessorMutator() : ObjectGraphAccessorMutator

    suspend fun read(accessConfiguration:DatastoreConfiguration, input:Flow<DataItem>): Flow<DataItem>

    suspend fun write(accessConfiguration:DatastoreConfiguration, input:Flow<DataItem>): Flow<DataItem>

}

/**
 * One end of a DatastoreConnection: a constructor argument of the connection's targetType,
 * derived by evaluating 'expression' with `$self` = the source item's data.
 */
interface DatastoreConnectionEnd {
    val name: String

    /** (possibly qualified) name of the type of the expression result */
    val typeName: String

    val expression: String
}

/**
 * Connects the output of one datastore (source) to the input of another (target).
 * For each source item, an instance of targetType is created from the connection's ends.
 */
interface DatastoreConnection {
    val source: DatastoreIdentity
    val target: DatastoreIdentity

    /** optional name, needed to tell apart several connections from the same source to the same target (fan-out) */
    val name: String?

    /**
     * identifies the connection among the connections from the same source to the same target:
     * 'name' if given, otherwise the simple name of targetType with a lower-case first letter (e.g. 'parsableUnit').
     * It is the identity of the DataItems the connection creates, and the name of the derived association end on the source type.
     */
    val identity: String get() = name ?: targetType.substringAfterLast('.').replaceFirstChar { it.lowercaseChar() }

    /** (possibly qualified) name of a type, known to the target datastore, that the target reads as input */
    val targetType: String

    val end: List<DatastoreConnectionEnd>
}

interface DatastoreCompositionConfiguration {
    val part: Map<Pair<DatastoreProviderIdentity,DatastoreIdentity>, DatastoreConfiguration>
    val connection: List<DatastoreConnection>
}

interface AccessCompositionConfiguration {

    val datastoreConfiguration: DatastoreCompositionConfiguration
    val part: Map<DatastoreIdentity, DatastoreConfiguration>
}

interface DatastoreComposition {

    val configuration: DatastoreCompositionConfiguration

    fun create(registry: DatastoreProviderRegistry)

    suspend fun types(): TypesDomain

    suspend fun accessorMutator(): ObjectGraphAccessorMutator
    suspend fun read(configuration: AccessCompositionConfiguration): Flow<DataItem>
    suspend fun write(configuration: AccessCompositionConfiguration, input: Flow<DataItem>)
}