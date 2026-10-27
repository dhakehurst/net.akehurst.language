package net.akehurst.datastore.common

import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DsConfigParameterChoice
import net.akehurst.datastore.api.DsConfigParameterDefinition
import net.akehurst.datastore.api.DsConfigParameterGroup
import net.akehurst.datastore.api.DsConfigParameterInfo
import net.akehurst.datastore.api.DsParameterSet
import net.akehurst.language.types.api.TypeDefinition
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.DatastoreProviderRegistry

fun datastoreConfiguration(
    providerIdentity: DatastoreProviderIdentity,
    registry: DatastoreProviderRegistry = DatastoreProviderRegistryDefault.GLOBAL,
    init: DatastoreConfigurationBuilder.() -> Unit
): DatastoreConfiguration {
    val b = DatastoreConfigurationBuilder(registry,providerIdentity, false)
    b.init()
    return b.build()
}

fun accessConfiguration(
    providerIdentity: DatastoreProviderIdentity,
    registry: DatastoreProviderRegistry = DatastoreProviderRegistryDefault.GLOBAL,
    init: DatastoreConfigurationBuilder.() -> Unit
): DatastoreConfiguration {
    val b = DatastoreConfigurationBuilder(registry,providerIdentity, true)
    b.init()
    return b.build()
}

@DslMarker
annotation class DatastoreConfigurationDslMarker

@DatastoreConfigurationDslMarker
class DatastoreConfigurationBuilder(
    private val _registry: DatastoreProviderRegistry,
    private val _providerIdentity: DatastoreProviderIdentity,
    private val _access:Boolean,
) {

    private val provider = _registry.getOrNull(_providerIdentity) ?: error("Provider not found: '${_providerIdentity.value}'")
    val _argument= mutableMapOf<String, Any>()

    fun argument(key: String, value: Any) {
        if (_access) {
            check(provider.configuration.access.getParameterOrNull(key) != null) { "'$key' is not an access parameter of provider '${_providerIdentity.value}'" }
        } else {
            check(provider.configuration.datastore.getParameterOrNull(key) != null) { "'$key' is not a datastore parameter of provider '${_providerIdentity.value}'" }
        }
        _argument[key] = value
    }

    /**
     * Checks the arguments against the provider's schema, then builds the configuration.
     * Errors: a required (non-optional) parameter is missing, a value has the wrong type,
     * or no alternative of a choice has all its required parameters.
     */
    fun build(): DatastoreConfiguration {
        val parameterSet = if (_access) provider.configuration.access else provider.configuration.datastore
        val errors = DatastoreConfigurationValidator.validate(parameterSet, _argument)
        check(errors.isEmpty()) {
            "Invalid ${if (_access) "access" else "datastore"} configuration for provider '${_providerIdentity.value}':\n  ${errors.joinToString("\n  ")}"
        }
        return object : DatastoreConfiguration {
            override val providerIdentity = _providerIdentity
            override val argument = _argument.toMap()
        }
    }
}

object DatastoreConfigurationValidator {

    /** returns a description of each problem with 'argument' with respect to 'parameterSet', empty if there are none */
    fun validate(parameterSet: DsParameterSet, argument: Map<String, Any>): List<String> =
        validateInfos(parameterSet.parameterInfo, argument)

    private fun validateInfos(infos: List<DsConfigParameterInfo>, argument: Map<String, Any>): List<String> =
        infos.flatMap { validateInfo(it, argument) }

    private fun validateInfo(info: DsConfigParameterInfo, argument: Map<String, Any>): List<String> = when (info) {
        is DsConfigParameterDefinition -> {
            val value = argument[info.identity]
            when {
                null == value -> if (info.optional) emptyList() else listOf("Required parameter '${info.identity}' (${info.name}) is missing")
                conformsTo(value, info.type).not() -> listOf("Parameter '${info.identity}' (${info.name}) must be a '${info.type.qualifiedName.value}', got a '${value::class.simpleName}'")
                else -> emptyList()
            }
        }

        is DsConfigParameterGroup -> validateInfos(info.parameterInfo, argument)

        is DsConfigParameterChoice -> {
            val results = info.alternative.mapValues { (_, ps) -> validate(ps, argument) }
            when {
                results.values.any { it.isEmpty() } -> emptyList()
                else -> listOf(
                    "None of the alternatives [${info.alternative.keys.joinToString { "'$it'" }}] is satisfied: " +
                            results.entries.joinToString("; ") { (alt, errs) -> "'$alt': ${errs.joinToString(", ")}" }
                )
            }
        }
    }

    private fun conformsTo(value: Any, type: TypeDefinition): Boolean = when (type.qualifiedName) {
        StdLibDefault.String.qualifiedTypeName -> value is String
        StdLibDefault.Boolean.qualifiedTypeName -> value is Boolean
        StdLibDefault.Integer.qualifiedTypeName -> value is Int || value is Long || value is Short || value is Byte
        StdLibDefault.Real.qualifiedTypeName -> value is Double || value is Float
        else -> type.implementation?.isInstance(value) ?: true
    }
}