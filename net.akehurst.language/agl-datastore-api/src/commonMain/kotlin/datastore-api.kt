package net.akehurst.datastore.api

import kotlinx.coroutines.flow.Flow
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.TypeDefinition
import net.akehurst.language.types.api.TypesDomain
import kotlin.jvm.JvmInline

@JvmInline
value class DatastoreProviderIdentity(val value:String)

@JvmInline
value class DatastoreIdentity(val value:String)

interface DsConfigParameterInfo

interface DsConfigParameterDefinition : DsConfigParameterInfo{
    val identity: String
    val name: String
    val optional: Boolean
    val description:String
    val type: TypeDefinition
}

interface DsConfigParameterGroup : DsConfigParameterInfo {
    val name:String
    val parameterInfo: List<DsConfigParameterInfo>
}

interface DsConfigParameterChoice : DsConfigParameterInfo {
    val alternative: Map<String, DsParameterSet>
}


interface DsParameterSet {
    val parameterInfo: List<DsConfigParameterInfo>
}

interface DatastoreProviderConfiguration {
    val datastore: DsParameterSet
    val access:DsParameterSet
}

interface DatastoreProvider {
    val identity:DatastoreProviderIdentity
    val configuration: DatastoreProviderConfiguration

    fun provide(datastoreIdentity: DatastoreIdentity, configuration: DatastoreConfiguration):Datastore
}

interface DatastoreConfiguration {

    val argument: Map<String, Any>

    fun getAsString(parameterIdentity:String): String = argument[parameterIdentity] as String
    fun getAsStringOrNull(parameterIdentity:String): String? = argument[parameterIdentity] as String?

}

interface Datastore {
    val identity:DatastoreIdentity

    val configuration:DatastoreConfiguration

    suspend fun types() : TypesDomain

    suspend fun accessorMutator() : ObjectGraphAccessorMutator

    suspend fun read(accessConfiguration:DatastoreConfiguration, input:Flow<TypedObject>): Flow<TypedObject>

    suspend fun write(accessConfiguration:DatastoreConfiguration, input:Flow<TypedObject>): Flow<TypedObject>

}