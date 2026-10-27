package net.akehurst.datastore.text

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.akehurst.datastore.api.DataItem
import net.akehurst.datastore.api.DataItemIdentity
import net.akehurst.datastore.api.DataItemProvenance
import net.akehurst.datastore.api.Datastore
import net.akehurst.datastore.api.DatastoreConfiguration
import net.akehurst.datastore.api.DatastoreIdentity
import net.akehurst.datastore.common.DataItemDefault
import net.akehurst.datastore.common.DatastoreAbstract
import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.contextFromRegistryGrammars
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.simple.AsmSimple
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.datastore.api.DataItemIssueLocation
import net.akehurst.kotlinx.issues.api.IssueCollection
import net.akehurst.kotlinx.issues.api.IssueKind
import net.akehurst.kotlinx.issues.ram.IssueDefault
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain

/**
 * A unit of text to be parsed.
 * 'owner' is the object that owns the text (e.g. a FileHandle).
 * It is not a constructor argument, it is an association end, set when a composition connects a datastore to this one.
 */
class ParsableUnit(
    val text: String
) {
    var owner: Any? = null

    override fun toString(): String = "ParsableUnit(owner=$owner, text=$text)"
}

/**
 * Location of an issue in a DataItem produced by an AglTextDatastore: the position in the parsed text, if known.
 */
data class AglTextIssueLocation(
    val inputLocation: InputLocation?
) : DataItemIssueLocation

/**
 * Provenance of a DataItem produced by an AglTextDatastore.
 */
data class AglTextProvenance(
    val datastoreIdentity: DatastoreIdentity
) : DataItemProvenance

class AglTextDatastore(
    identity: DatastoreIdentity,
    configuration: DatastoreConfiguration
) : DatastoreAbstract(identity, configuration) {

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

    private val _inputTypes by lazy {
        typesDomain("AglTextDatastore", true) {
            namespace("net.akehurst.datastore.text") {
                data("ParsableUnit", implementation = ParsableUnit::class) {
                    constructor_ {
                        parameter(setOf(REF, VAL), "text", "String")
                        execution { args -> ParsableUnit(args[0] as String) }
                    }
                }
            }
        }
    }

    /** the processor's types plus the input types (ParsableUnit) */
    override suspend fun createTypes(): TypesDomain =
        typesDomain("AglText", true) {}
            .merge(_parsedConfig.processor.typesDomain)
            .merge(_inputTypes)

    /**
     * input: DataItem with String or ParsableUnit data
     * output: DataItem with Asm data, whose parent is the input item
     */
    override suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> {
        return input.map { parseString(accessConfiguration, it) }
    }

    /**
     * input: DataItem with Asm data
     * output: DataItem with String data, whose parent is the input item
     */
    override suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> {
        return input.map { derivedItem(it, formatObject(accessConfiguration, it.data)) }
    }

    // ---

    /** the derived item's identity is its index among the items derived from the same parent (currently always 0) */
    private fun derivedItem(parent: DataItem, data: TypedObject, issues: IssueCollection<DataItemIssueLocation> = net.akehurst.kotlinx.issues.ram.IssueHolder(), index: Int = 0): DataItem = DataItemDefault(
        parent = parent,
        identity = DataItemIdentity(index.toString()),
        data = data,
        provenance = AglTextProvenance(identity),
        issues = issues
    )

    /**
     * Parses the text of the input item.
     * If parsing fails, the result item has the issues and its data is nothing, so the rest of the flow is still read.
     */
    private suspend fun parseString(accessConfiguration: DatastoreConfiguration, item: DataItem): DataItem {
        val input = item.data
        val text = when {
            input.self is ParsableUnit -> (input.self as ParsableUnit).text
            input.type == StdLibDefault.String && input.self is String -> input.self as String
            else -> null
        }
        val am = accessorMutator()
        return when {
            null != text -> {
                val res = _parsedConfig.processor.process(
                    text,
                    //TODO: process options !
                )
                val issues = net.akehurst.kotlinx.issues.ram.IssueHolder<DataItemIssueLocation>(
                    res.allIssues.all.map { IssueDefault(IssueKind.valueOf(it.kind.name), AglTextIssueLocation(it.location), it.message, it.data) }
                )
                when {
                    res.allIssues.errors.isEmpty() -> {
                        val v = res.asm?.root[0]
                        derivedItem(item, am.toTypedObject(v, StdLibDefault.AnyType), issues)
                    }

                    else -> derivedItem(item, am.nothing(), issues)
                }
            }

            else -> derivedItem(
                item, am.nothing(),
                net.akehurst.kotlinx.issues.ram.IssueHolder<DataItemIssueLocation>(
                    listOf(IssueDefault(IssueKind.ERROR, AglTextIssueLocation(null), "the read input to an AglTextDatastore must be a String or a ParsableUnit."))
                )
            )
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