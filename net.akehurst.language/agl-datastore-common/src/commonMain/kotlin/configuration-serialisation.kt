package net.akehurst.datastore.common

import kotlinx.serialization.Contextual
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import net.akehurst.datastore.api.AccessCompositionConfiguration
import net.akehurst.datastore.api.DatastoreCompositionConfiguration
import net.akehurst.datastore.api.DatastoreCompositionIdentity
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreConnection
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.DatastoreProviderRegistry
import net.akehurst.datastore.api.DsParameterSet
import net.akehurst.datastore.api.SecretReference
import net.akehurst.kotlinx.utils.AnySerializer
import net.akehurst.language.types.asm.StdLibDefault

/**
 * Saves and loads datastore composition configurations, and access configurations, as JSON.
 *
 * One document holds a composition and, optionally, the access configuration of each of its parts:
 * ```
 * { "composition": { "id": "doors-reqs", "name": "DOORS requirements", "description": "...",
 *                    "part": [ {provider, id, configuration}, ... ], "connection": [ ... ] },
 *   "access": [ {id, configuration}, ... ] }
 * ```
 * The composition's `id` is required and must be valid (DatastoreCompositionIdentity.isValid); `name` and `description` default to "".
 * Parts are saved in chain order. The provider schemas are not saved; on decode each configuration is
 * re-attached to its provider's schema from the registry, and validated against it.
 *
 * Secrets are never saved: a sensitive parameter must have a SecretReference (`secret(source, key)`) as its value,
 * otherwise encode fails. Move plain sensitive values into secure storage before encoding.
 */
object DatastoreConfigurationJson {

    private const val SECRET_REFERENCE_CLASS = "SecretReference"

    private val anySerializer = AnySerializer(
        customSerialize = { value ->
            when (value) {
                is SecretReference -> Pair(
                    SECRET_REFERENCE_CLASS,
                    JsonObject(mapOf("source" to JsonPrimitive(value.source), "key" to JsonPrimitive(value.key)))
                )

                else -> error("Cannot save an argument of type '${value::class.simpleName}': only String, Integer, Real, Boolean and secret references are supported")
            }
        },
        customDeserialize = { className, obj ->
            when (className) {
                SECRET_REFERENCE_CLASS -> SecretReference(
                    source = obj["source"]?.jsonPrimitive?.content ?: error("Secret reference without a 'source'"),
                    key = obj["key"]?.jsonPrimitive?.content ?: error("Secret reference without a 'key'")
                )

                else -> error("Unknown argument class '$className'")
            }
        }
    )

    private val json = Json {
        prettyPrint = true
        serializersModule = SerializersModule { contextual(Any::class, anySerializer) }
    }

    /** JSON for 'configuration'. Fails if a sensitive parameter has a value that is not a secret reference. */
    fun encode(configuration: DatastoreCompositionConfiguration): String =
        json.encodeToString(DocumentDto.serializer(), DocumentDto(toDto(configuration), null))

    /**
     * JSON for 'configuration': its composition and the access configuration of each part, in the same document.
     * Fails if a sensitive parameter has a value that is not a secret reference.
     */
    fun encode(configuration: AccessCompositionConfiguration): String {
        val composition = configuration.datastoreConfiguration
        val access = configuration.part.map { (id, cfg) ->
            check(composition.part.keys.any { it.second == id }) { "Access configuration for '${id.value}', which is not a part of the composition" }
            AccessPartDto(id.value, toDto("access configuration of '${id.value}'", cfg))
        }
        return json.encodeToString(DocumentDto.serializer(), DocumentDto(toDto(composition), access))
    }

    /** The composition configuration in 'text' (an access section, if present, is ignored). */
    fun decode(text: String, registry: DatastoreProviderRegistry): DatastoreCompositionConfiguration {
        val doc = json.decodeFromString(DocumentDto.serializer(), text)
        return fromDto(doc.composition, registry)
    }

    /** The access configuration in 'text', with the composition configuration saved in the same document. */
    fun decodeAccess(text: String, registry: DatastoreProviderRegistry): AccessCompositionConfiguration {
        val doc = json.decodeFromString(DocumentDto.serializer(), text)
        val composition = fromDto(doc.composition, registry)
        val access = (doc.access ?: error("The document has no access configuration")).associate { ap ->
            val providerId = composition.part.keys.firstOrNull { it.second.value == ap.id }?.first
                ?: error("Access configuration for '${ap.id}', which is not a part of the composition")
            val cfg = fromDto("access configuration of '${ap.id}'", providerId, ap.configuration, registry, true)
            Pair(DatastoreIdentity(ap.id), cfg)
        }
        return AccessCompositionConfigurationDefault(composition, access)
    }

    // --- encode ---

    private fun toDto(cfg: DatastoreCompositionConfiguration) = CompositionDto(
        id = cfg.identity.value,
        name = cfg.name,
        description = cfg.description,
        part = cfg.part.map { (key, dsCfg) ->
            val (providerId, dsId) = key
            check(dsCfg.providerIdentity == providerId) {
                "Part '${dsId.value}': configuration is for provider '${dsCfg.providerIdentity.value}', not '${providerId.value}'"
            }
            PartDto(providerId.value, dsId.value, toDto("datastore '${dsId.value}'", dsCfg))
        },
        connection = cfg.connection.map { toDto(it) }
    )

    private fun toDto(what: String, cfg: DatastoreConfiguration): Map<String, Any> {
        cfg.argument.forEach { (id, value) ->
            check(!cfg.isSensitive(id) || value is SecretReference) {
                "Cannot save $what: sensitive parameter '$id' has a plain value; store it in a secret source and give a secret(source, key) reference"
            }
        }
        return cfg.argument
    }

    private fun toDto(c: DatastoreConnection) = ConnectionDto(
        source = c.source.value,
        target = c.target.value,
        targetType = c.targetType,
        name = c.name,
        end = c.end.map { ConnectionEndDto(it.name, it.typeName, it.expression) }
    )

    // --- decode ---

    private fun fromDto(dto: CompositionDto, registry: DatastoreProviderRegistry): DatastoreCompositionConfiguration {
        check(DatastoreCompositionIdentity.isValid(dto.id)) { "Composition id '${dto.id}' is not valid: ${DatastoreCompositionIdentity.RULE}" }
        // LinkedHashMap keeps the chain order
        val part = linkedMapOf<Pair<DatastoreProviderIdentity, DatastoreIdentity>, DatastoreConfiguration>()
        dto.part.forEach { p ->
            check(part.keys.none { it.second.value == p.id }) { "Datastore '${p.id}' is defined more than once" }
            val providerId = DatastoreProviderIdentity(p.provider)
            part[Pair(providerId, DatastoreIdentity(p.id))] = fromDto("datastore '${p.id}'", providerId, p.configuration, registry, false)
        }
        val ids = part.keys.map { it.second.value }.toSet()
        val connection = dto.connection.map { c ->
            check(c.source in ids) { "Connection source '${c.source}' is not a datastore of the composition" }
            check(c.target in ids) { "Connection target '${c.target}' is not a datastore of the composition" }
            DatastoreConnectionDefault(
                source = DatastoreIdentity(c.source),
                target = DatastoreIdentity(c.target),
                targetType = c.targetType,
                end = c.end.map { DatastoreConnectionEndDefault(it.name, it.typeName, it.expression) },
                name = c.name
            )
        }
        return DatastoreCompositionConfigurationDefault(DatastoreCompositionIdentity(dto.id), part, connection, dto.name, dto.description)
    }

    private fun fromDto(
        what: String,
        providerId: DatastoreProviderIdentity,
        argument: Map<String, Any>,
        registry: DatastoreProviderRegistry,
        access: Boolean
    ): DatastoreConfiguration {
        val provider = registry.getOrNull(providerId)
            ?: error("Cannot load $what: provider '${providerId.value}' is not registered")
        val parameterSet = if (access) provider.schema.access else provider.schema.datastore
        val converted = argument.mapValues { (id, value) -> convert(what, providerId, parameterSet, access, id, value) }
        val errors = DatastoreConfigurationValidator.validate(parameterSet, converted)
        check(errors.isEmpty()) { "Cannot load $what (provider '${providerId.value}'):\n  ${errors.joinToString("\n  ")}" }
        return DatastoreConfigurationDefault(providerId, converted, parameterSet)
    }

    /** JSON numbers do not say Integer or Real, so the parameter's type in the schema decides */
    private fun convert(what: String, providerId: DatastoreProviderIdentity, parameterSet: DsParameterSet, access: Boolean, id: String, value: Any): Any {
        val param = parameterSet.getParameterOrNull(id)
            ?: error("Cannot load $what: '$id' is not ${if (access) "an access" else "a datastore"} parameter of provider '${providerId.value}'")
        return when {
            value !is Number -> value
            param.type.qualifiedName == StdLibDefault.Integer.qualifiedTypeName -> when (value) {
                is Long -> value
                is Int -> value.toLong()
                else -> error("Cannot load $what: parameter '$id' must be an Integer, got '$value'")
            }

            param.type.qualifiedName == StdLibDefault.Real.qualifiedTypeName -> value.toDouble()
            else -> value
        }
    }
}

@Serializable
internal data class DocumentDto(
    val composition: CompositionDto,
    val access: List<AccessPartDto>? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class CompositionDto(
    val id: String,
    // always written, so a saved document shows that they can be given
    @EncodeDefault val name: String = "",
    @EncodeDefault val description: String = "",
    val part: List<PartDto>,
    val connection: List<ConnectionDto> = emptyList()
)

@Serializable
internal data class PartDto(
    val provider: String,
    val id: String,
    val configuration: Map<String, @Contextual Any>
)

@Serializable
internal data class AccessPartDto(
    val id: String,
    val configuration: Map<String, @Contextual Any>
)

@Serializable
internal data class ConnectionDto(
    val source: String,
    val target: String,
    val targetType: String,
    val name: String? = null,
    val end: List<ConnectionEndDto> = emptyList()
)

@Serializable
internal data class ConnectionEndDto(
    val name: String,
    val typeName: String,
    val expression: String
)
