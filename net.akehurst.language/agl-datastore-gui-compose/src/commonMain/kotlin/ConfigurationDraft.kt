package net.akehurst.datastore.gui.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreProviderIdentity
import net.akehurst.datastore.api.DsConfigParameterChoice
import net.akehurst.datastore.api.DsConfigParameterDefinition
import net.akehurst.datastore.api.DsConfigParameterGroup
import net.akehurst.datastore.api.DsConfigParameterInfo
import net.akehurst.datastore.api.DsParameterSet
import net.akehurst.datastore.api.SecretReference
import net.akehurst.datastore.common.DatastoreConfigurationDefault
import net.akehurst.datastore.common.DatastoreConfigurationValidator
import net.akehurst.language.types.asm.StdLibDefault

/** How the value of a sensitive parameter is given. */
enum class ValueMode { VALUE, SECRET_REFERENCE }

/** The kind of field used for a parameter, from its type. */
enum class ParameterKind { STRING, INTEGER, REAL, BOOLEAN, LIST, OTHER }

val DsConfigParameterDefinition.kind: ParameterKind
    get() = when (type.qualifiedName) {
        StdLibDefault.String.qualifiedTypeName -> ParameterKind.STRING
        StdLibDefault.Integer.qualifiedTypeName -> ParameterKind.INTEGER
        StdLibDefault.Real.qualifiedTypeName -> ParameterKind.REAL
        StdLibDefault.Boolean.qualifiedTypeName -> ParameterKind.BOOLEAN
        StdLibDefault.List.qualifiedName -> ParameterKind.LIST
        else -> ParameterKind.OTHER
    }

/**
 * What the user has entered for one parameter. Values are kept as typed (text), so that an
 * unfinished or invalid value can stay in the form; they are converted when the configuration is built.
 */
class ParameterDraft(val definition: DsConfigParameterDefinition) {
    /** the value of a String, Integer, Real, List (one element per line) or other parameter, as typed */
    var text by mutableStateOf("")

    /** the value of a Boolean parameter, null when not set */
    var boolean by mutableStateOf<Boolean?>(null)

    /** for a sensitive parameter: a value, or a reference to a secret */
    var mode by mutableStateOf(ValueMode.VALUE)
    var secretSource by mutableStateOf("")
    var secretKey by mutableStateOf("")

    val isSet: Boolean
        get() = when {
            mode == ValueMode.SECRET_REFERENCE -> secretSource.isNotEmpty() || secretKey.isNotEmpty()
            definition.kind == ParameterKind.BOOLEAN -> null != boolean
            else -> text.isNotEmpty()
        }

    /** the parameter's default value from the schema, null if it has none; used by the configuration when the parameter is not set */
    val defaultValue: Any? get() = definition.defaultValue

    /**
     * true if the parameter is set to something other than its default value
     * (a value that cannot be converted differs; a parameter without a default differs whenever it is set)
     */
    val differsFromDefault: Boolean
        get() = when {
            isSet.not() -> false
            null == defaultValue -> true
            else -> argument().getOrNull()?.let { sameValue(it, defaultValue!!).not() } ?: true
        }

    /** the argument value, or an error message if what was typed cannot be converted; null value when not set */
    fun argument(): Result<Any?> = when {
        isSet.not() -> Result.success(null)
        mode == ValueMode.SECRET_REFERENCE -> when {
            definition.sensitive.not() -> Result.failure(IllegalStateException("Only a sensitive parameter can be given a secret reference"))
            secretSource.isBlank() -> Result.failure(IllegalStateException("Choose a secret source"))
            secretKey.isBlank() -> Result.failure(IllegalStateException("Give the key of the secret"))
            else -> Result.success(SecretReference(secretSource, secretKey))
        }

        else -> when (definition.kind) {
            ParameterKind.STRING, ParameterKind.OTHER -> Result.success(text)
            ParameterKind.INTEGER -> text.toLongOrNull()?.let { Result.success(it) } ?: Result.failure(IllegalStateException("'$text' is not an Integer"))
            ParameterKind.REAL -> text.toDoubleOrNull()?.let { Result.success(it) } ?: Result.failure(IllegalStateException("'$text' is not a Real"))
            ParameterKind.BOOLEAN -> Result.success(boolean)
            ParameterKind.LIST -> Result.success(listFromText(text))
        }
    }

    fun load(value: Any?) {
        when (value) {
            null -> {
                text = ""; boolean = null; mode = ValueMode.VALUE; secretSource = ""; secretKey = ""
            }

            is SecretReference -> {
                mode = ValueMode.SECRET_REFERENCE; secretSource = value.source; secretKey = value.key
            }

            is Boolean -> {
                mode = ValueMode.VALUE; boolean = value
            }

            is List<*> -> {
                mode = ValueMode.VALUE; text = if (definition.kind == ParameterKind.LIST) textOfList(value) else value.toString()
            }

            else -> {
                mode = ValueMode.VALUE; text = value.toString()
            }
        }
    }

    companion object {
        private val INTEGER_PREFIX = Regex("-?[0-9]*")
        private val REAL_PREFIX = Regex("-?[0-9]*(\\.[0-9]*)?([eE][-+]?[0-9]*)?")

        /** equal values, where numbers are compared by value (e.g. Int 30 and Long 30) */
        fun sameValue(a: Any, b: Any): Boolean = when {
            a is Number && b is Number -> when {
                a is Double || a is Float || b is Double || b is Float -> a.toDouble() == b.toDouble()
                else -> a.toLong() == b.toLong()
            }

            else -> a == b
        }

        /** the elements of a List parameter's text: one per non-blank line, trimmed */
        fun listFromText(text: String): List<String> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        /** the text of a List value: its elements joined by newlines */
        fun textOfList(value: List<*>): String = value.joinToString("\n") { it.toString() }

        /** true if 'text' may be typed into a field of this kind (it may be an unfinished number) */
        fun accepts(kind: ParameterKind, text: String): Boolean = when (kind) {
            ParameterKind.INTEGER -> INTEGER_PREFIX.matches(text)
            ParameterKind.REAL -> REAL_PREFIX.matches(text)
            else -> true
        }
    }
}

/**
 * What the user has entered for a datastore or access configuration of one provider,
 * against one of its parameter sets (`schema.datastore` or `schema.access`).
 */
class ConfigurationDraft(
    val providerIdentity: DatastoreProviderIdentity,
    val parameterSet: DsParameterSet,
) {
    /** a draft for every parameter in the set, including those in groups and in every alternative of a choice */
    val parameter: Map<String, ParameterDraft> = allDefinitions(parameterSet.parameterInfo).associate { Pair(it.identity, ParameterDraft(it)) }

    private val _choice = mutableStateMapOf<String, String>()

    /**
     * The chosen alternative of a choice. A choice is identified by its path: the indexes of the
     * parameter infos that lead to it, e.g. "2" or "1/0". The first alternative is chosen by default.
     */
    fun chosenAlternative(path: String, choice: DsConfigParameterChoice): String =
        _choice[path] ?: choice.alternative.keys.first()

    fun choose(path: String, alternative: String) {
        _choice[path] = alternative
    }

    /** the parameters that apply: those not inside an alternative that is not chosen */
    val activeParameters: List<ParameterDraft>
        get() = activeDefinitions(parameterSet.parameterInfo, "").mapNotNull { parameter[it.identity] }

    /** the problems with each active parameter on its own (missing, or cannot be converted) */
    val parameterIssues: Map<String, String>
        get() = activeParameters.mapNotNull { pd ->
            val msg = when {
                pd.isSet.not() -> if (pd.definition.optional || null != pd.defaultValue) null else "Required"
                else -> pd.argument().exceptionOrNull()?.message
            }
            msg?.let { Pair(pd.definition.identity, it) }
        }.toMap()

    /** the arguments of the active parameters that are set and valid */
    val arguments: Map<String, Any>
        get() = activeParameters.mapNotNull { pd -> pd.argument().getOrNull()?.let { Pair(pd.definition.identity, it) } }.toMap()

    /** the schema problems of the whole configuration (as reported by the configuration builders) */
    val schemaIssues: List<String>
        get() = DatastoreConfigurationValidator.validate(parameterSet, arguments)

    /** the configuration, or null if it has problems */
    fun build(): DatastoreConfiguration? = when {
        parameterIssues.isNotEmpty() -> null
        schemaIssues.isNotEmpty() -> null
        else -> DatastoreConfigurationDefault(providerIdentity, arguments, parameterSet)
    }

    /**
     * true if the advanced group at 'path' must be shown open: one of its active parameters is set
     * to something other than its default value (ParameterDraft.differsFromDefault), or has an error.
     * A group that is not advanced is always open.
     *
     * @param path the indexes of the parameter infos that lead to the group, as for chosenAlternative
     * @param errors the error of each parameter, by parameter identity
     */
    fun advancedGroupMustBeOpen(path: String, group: DsConfigParameterGroup, errors: Map<String, String> = parameterIssues): Boolean = when {
        group.advanced.not() -> true
        else -> activeDefinitions(group.parameterInfo, "$path/").any { def ->
            def.identity in errors || true == parameter[def.identity]?.differsFromDefault
        }
    }

    /** fills the draft from an existing configuration */
    fun load(configuration: DatastoreConfiguration) {
        parameter.values.forEach { it.load(configuration.argument[it.definition.identity]) }
        chooseAlternativesFor(parameterSet.parameterInfo, "", configuration.argument.keys)
    }

    private fun chooseAlternativesFor(infos: List<DsConfigParameterInfo>, prefix: String, given: Set<String>) {
        infos.forEachIndexed { i, info ->
            val path = prefix + i
            when (info) {
                is DsConfigParameterDefinition -> Unit
                is DsConfigParameterGroup -> chooseAlternativesFor(info.parameterInfo, "$path/", given)
                is DsConfigParameterChoice -> {
                    // the alternative that has a given argument, if any
                    val alt = info.alternative.entries.firstOrNull { (_, ps) -> allDefinitions(ps.parameterInfo).any { it.identity in given } }
                    alt?.let { _choice[path] = it.key }
                    info.alternative.values.forEachIndexed { ai, ps -> chooseAlternativesFor(ps.parameterInfo, "$path/$ai/", given) }
                }
            }
        }
    }

    private fun activeDefinitions(infos: List<DsConfigParameterInfo>, prefix: String): List<DsConfigParameterDefinition> =
        infos.flatMapIndexed { i, info ->
            val path = prefix + i
            when (info) {
                is DsConfigParameterDefinition -> listOf(info)
                is DsConfigParameterGroup -> activeDefinitions(info.parameterInfo, "$path/")
                is DsConfigParameterChoice -> {
                    val chosen = chosenAlternative(path, info)
                    val ai = info.alternative.keys.indexOf(chosen)
                    activeDefinitions(info.alternative[chosen]!!.parameterInfo, "$path/$ai/")
                }
            }
        }

    companion object {
        fun allDefinitions(infos: List<DsConfigParameterInfo>): List<DsConfigParameterDefinition> = infos.flatMap { info ->
            when (info) {
                is DsConfigParameterDefinition -> listOf(info)
                is DsConfigParameterGroup -> allDefinitions(info.parameterInfo)
                is DsConfigParameterChoice -> info.alternative.values.flatMap { allDefinitions(it.parameterInfo) }
            }
        }
    }
}
