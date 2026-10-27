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
 * A unit of text and its ASM.
 * read parses 'text' and sets 'asm'; write formats 'asm' and sets 'text'.
 * When a composition connects a datastore to this one, the object that owns the text (e.g. a FileHandle) is linked
 * through an association end such as 'owner'; the link is held by the composition's accessor-mutator, not by this object.
 */
class ParsableUnit(
    var text: String
) {
    /** the root of the ASM: set by read from 'text', formatted into 'text' by write. Null if not parsed, or parsing failed. */
    var asm: Any? = null

    override fun toString(): String = "ParsableUnit(text=$text)"
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
                    propertyOfWithBinding<ParsableUnit, Any?>(setOf(CMP, VAR), "asm", "Any", isNullable = true,
                        accessor = { it.asm }, mutator = { it, v -> it.asm = v })
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
     * input: DataItem with ParsableUnit data (or String data, for which a ParsableUnit is created)
     * output: DataItem with the same ParsableUnit, whose 'asm' is set to the result of parsing its 'text'.
     * The output item has the input item's parent and identity (for String input, the input item is its parent).
     */
    override suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> {
        return input.map { parseUnit(accessConfiguration, it) }
    }

    /**
     * input: DataItem with ParsableUnit data
     * output: DataItem with the same ParsableUnit, whose 'text' is set to the result of formatting its 'asm'
     */
    override suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> {
        return input.map { formatUnit(accessConfiguration, it) }
    }

    // ---

    /**
     * the output item for 'unit'. If the input item holds the unit, the output keeps its parent, identity and data
     * (so the data's accessor-mutator, and any links it holds, e.g. in a composition, still apply);
     * otherwise the output is a child of the input item.
     */
    private suspend fun unitItem(input: DataItem, unit: ParsableUnit, issues: IssueCollection<DataItemIssueLocation>): DataItem = when {
        input.data.self === unit -> DataItemDefault(input.parent, input.identity, input.data, AglTextProvenance(identity), issues)
        else -> DataItemDefault(input, DataItemIdentity("parsableUnit"), accessorMutator().toTypedObject(unit, StdLibDefault.AnyType), AglTextProvenance(identity), issues)
    }

    private fun issuesOf(vararg issue: IssueDefault<DataItemIssueLocation>) = net.akehurst.kotlinx.issues.ram.IssueHolder<DataItemIssueLocation>(issue.toList())

    /**
     * Parses the text of the input unit, and sets its 'asm'.
     * If parsing fails, the item has the issues and 'asm' is null, so the rest of the flow is still read.
     */
    private suspend fun parseUnit(accessConfiguration: DatastoreConfiguration, item: DataItem): DataItem {
        val input = item.data
        val am = accessorMutator()
        val unit = when {
            input.self is ParsableUnit -> input.self as ParsableUnit
            input.self is String -> ParsableUnit(input.self as String)
            else -> return DataItemDefault(
                item, DataItemIdentity("parsableUnit"), am.nothing(), AglTextProvenance(identity),
                issuesOf(IssueDefault(IssueKind.ERROR, AglTextIssueLocation(null), "the read input to an AglTextDatastore must be a ParsableUnit or a String."))
            )
        }
        val res = _parsedConfig.processor.process(
            unit.text,
            //TODO: process options !
        )
        val issues = net.akehurst.kotlinx.issues.ram.IssueHolder<DataItemIssueLocation>(
            res.allIssues.all.map { IssueDefault(IssueKind.valueOf(it.kind.name), AglTextIssueLocation(it.location), it.message, it.data) }
        )
        // one parse gives one root
        unit.asm = if (res.allIssues.errors.isEmpty()) res.asm?.root?.firstOrNull() else null
        return unitItem(item, unit, issues)
    }

    /**
     * Formats the 'asm' of the input unit, and sets its 'text'.
     * If formatting fails, or there is no asm, the item has the issues and 'text' is unchanged.
     */
    private suspend fun formatUnit(accessConfiguration: DatastoreConfiguration, item: DataItem): DataItem {
        val am = accessorMutator()
        val unit = item.data.self as? ParsableUnit
            ?: return DataItemDefault(
                item.parent, item.identity, item.data, AglTextProvenance(identity),
                issuesOf(IssueDefault(IssueKind.ERROR, AglTextIssueLocation(null), "the write input to an AglTextDatastore must be a ParsableUnit."))
            )
        val root = unit.asm
            ?: return unitItem(item, unit, issuesOf(IssueDefault(IssueKind.ERROR, AglTextIssueLocation(null), "the ParsableUnit has no asm to format.")))
        val asm = AsmSimple(am)
        asm.addRoot(root)
        val res = _parsedConfig.processor.formatAsm(
            asm,
            //TODO: options
        )
        val issues = net.akehurst.kotlinx.issues.ram.IssueHolder<DataItemIssueLocation>(
            res.issues.all.map { IssueDefault(IssueKind.valueOf(it.kind.name), AglTextIssueLocation(it.location), it.message, it.data) }
        )
        if (res.issues.errors.isEmpty()) unit.text = res.sentence ?: ""
        return unitItem(item, unit, issues)
    }
}