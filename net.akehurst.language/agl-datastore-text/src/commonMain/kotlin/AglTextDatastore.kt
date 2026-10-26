package net.akehurst.datastore.text

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemProvenance
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.common.DataItemDefault
import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.contextFromRegistryGrammars
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.simple.AsmSimple
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault

/**
 * Provenance of a DataItem produced by an AglTextDatastore.
 */
data class AglTextProvenance(
    val datastoreIdentity: DatastoreIdentity
) : DataItemProvenance

class AglTextDatastore(
    override val identity: DatastoreIdentity,
    override val configuration: DatastoreConfiguration
) : Datastore {

    companion object {

        class AglTextConfig(val configuration: DatastoreConfiguration) {
            val grammarStr by lazy {
                configuration.getAsString(AglTextProvider.CONFIG_GRAMMAR_STR)
            }

            val formatStr by lazy {
                configuration.getAsString(AglTextProvider.CONFIG_FORMAT_STR)
            }

            val processor: LanguageProcessor<Asm, SentenceContext> by lazy {
                Agl.processorFromString(
                    grammarDefinitionStr = grammarStr,
                    configuration = Agl.configuration(base = Agl.configurationSimple()){
                        this.formatString(FormatString(formatStr))
                    },
                    aglOptions = Agl.options { semanticAnalysis { sentenceContext(contextFromRegistryGrammars(Agl.registry)) } }
                ).let {
                    check(it.issues.errors.isEmpty()) { "Failed to construct AglTextDatastore Configuration:\n${it.issues}" }
                    it.processor!!
                }
            }
        }

    }

    private val _parsedConfig by lazy { AglTextConfig(configuration) }
    private val _issues = IssueHolder()

    override suspend fun types(): TypesDomain = _parsedConfig.processor.typesDomain

    override suspend fun accessorMutator() = ObjectGraphAccessorMutatorByReflection(
        typesDomain = _parsedConfig.processor.typesDomain,
        locationMap = LocationMapDefault(),
        issues = _issues,
    )

    /**
     * input: DataItem with String data
     * output: DataItem with Asm data, whose parent is the input item
     */
    override suspend fun read(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> {
        return input.map { derivedItem(it, parseString(accessConfiguration, it.data)) }
    }

    /**
     * input: DataItem with Asm data
     * output: DataItem with String data, whose parent is the input item
     */
    override suspend fun write(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> {
        return input.map { derivedItem(it, formatObject(accessConfiguration, it.data)) }
    }

    // ---

    private fun derivedItem(parent: DataItem, data: TypedObject): DataItem = DataItemDefault(
        parent = parent,
        identity = parent.identity,
        data = data,
        provenance = AglTextProvenance(identity)
    )

    private suspend fun parseString(accessConfiguration: DatastoreConfiguration, input: TypedObject): TypedObject {
        return when {
            input.type == StdLibDefault.String && input.self is String -> {
                val value = input.self as String
                val res = _parsedConfig.processor.process(
                    value,
                    //TODO: process options !
                )
                when {
                    res.allIssues.errors.isEmpty() -> {
                        val v = res.asm?.root[0]
                        accessorMutator().toTypedObject(v, StdLibDefault.AnyType)
                    }
                    else -> {
                        error("Could not parse")
                    }
                }
            }

            else -> error("the read input to an AglTextDatastore must be a String.")
        }
    }

    private suspend fun formatObject(accessConfiguration: DatastoreConfiguration, input: TypedObject): TypedObject {
        return when {
            input.self!=Unit -> {
                val og = accessorMutator()
                val asm = AsmSimple(og)
                asm.addRoot(input.self)
                val res = _parsedConfig.processor.formatAsm(
                    asm,
                    //TODO: options
                )
                when {
                    res.issues.errors.isEmpty() -> {
                        val v = res.sentence ?: ""
                        accessorMutator().toTypedObject(v, StdLibDefault.String)
                    }
                    else -> {
                        error("Could not parse")
                    }
                }
            }

            else -> error("the write input to an AglTextDatastore must be an Object.")
        }
    }
}