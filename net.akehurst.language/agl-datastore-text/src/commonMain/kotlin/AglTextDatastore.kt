package net.akehurst.datastore.text

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
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
import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.api.processor.ProcessOptions
import net.akehurst.language.api.processor.ResolvedReference
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.api.AsmStructure
import net.akehurst.language.asm.api.AsmStructureProperty
import net.akehurst.language.asm.api.AsmTreeWalker
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.sppt.api.ParsePath
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

    /** where each element of 'asm' is in 'text'. Set by read; cleared by write, because the text changes. */
    internal var locations: Map<Any, InputLocation> = emptyMap()

    /**
     * the references in 'asm' that read resolved: source and target elements, and their locations.
     * The 'sentenceIdentity' of a location is the qualified identity (List<DataItemIdentity>) of the unit's item,
     * so a target in another unit (see AglTextProvider.ACCESS_RESOLVE_ACROSS_UNITS) says which unit it is in.
     * Empty if the datastore has no cross-reference definition. Cleared by write.
     */
    var resolvedReferences: List<ResolvedReference> = emptyList()
        internal set

    /** the location in 'text' of an element of 'asm' (the element, or a TypedObject of it), or null if it is not known */
    fun locationOf(element: Any?): InputLocation? = when (element) {
        null -> null
        is TypedObject -> locations[element.self]
        else -> locations[element]
    }

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

            val crossReferenceStr by lazy {
                configuration.getAsStringOrNull(AglTextProvider.CONFIG_CROSS_REFERENCE_STR)
            }

            val processor: LanguageProcessor<Asm, SentenceContext> by lazy {
                Agl.processorFromString(
                    grammarDefinitionStr = grammarStr,
                    configuration = Agl.configuration(base = Agl.configurationSimple()){
                        this.formatString(FormatString(formatStr))
                        crossReferenceStr?.let { this.crossReferenceString(CrossReferenceString(it)) }
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
        val acrossUnits = accessConfiguration.getAsBooleanOrNull(AglTextProvider.ACCESS_RESOLVE_ACROSS_UNITS) ?: false
        return when {
            acrossUnits && null != _parsedConfig.crossReferenceStr -> readAcrossUnits(input)
            else -> input.map { parseUnit(it) }
        }
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

    /** the unit an input item holds, or for String data a new unit; null if the data is neither */
    private fun unitOf(item: DataItem): ParsableUnit? = when (val self = item.data.self) {
        is ParsableUnit -> self
        is String -> ParsableUnit(self)
        else -> null
    }

    private suspend fun notAUnitItem(item: DataItem): DataItem = DataItemDefault(
        item, DataItemIdentity("parsableUnit"), accessorMutator().nothing(), AglTextProvenance(identity),
        issuesOf(IssueDefault(IssueKind.ERROR, AglTextIssueLocation(null), "the read input to an AglTextDatastore must be a ParsableUnit or a String."))
    )

    /** the qualified identity of the item read will output for 'unit': the sentence identity of the unit's text */
    private fun sentenceIdentityFor(item: DataItem, unit: ParsableUnit): List<DataItemIdentity> = when {
        item.data.self === unit -> item.qualifiedIdentity
        else -> item.qualifiedIdentity + DataItemIdentity("parsableUnit")
    }

    /** a context for semantic analysis, if the datastore has a cross-reference definition */
    private fun newContextOrNull(): SentenceContext? = _parsedConfig.crossReferenceStr?.let { contextAsmSimple() }

    private fun processOptions(
        sentenceIdentity: Any?,
        context: SentenceContext?,
        semanticAnalysis: Boolean,
        buildScope: Boolean,
        resolve: Boolean,
        locationMap: LocationMap? = null
    ): ProcessOptions<Asm, SentenceContext> = Agl.options<Asm, SentenceContext> {
        parse { sentenceIdentity { sentenceIdentity } }
        semanticAnalysis {
            enabled(semanticAnalysis)
            sentenceContext(context)
            buildScope(buildScope)
            checkReferences(resolve)
            resolveReferences(resolve)
            locationMap?.let { locationMap(it) }
        }
    }

    /**
     * The processor is called in separate steps (syntax analysis, building the scope, resolving references), and it reports
     * the steps it was told to skip as INFORMATION issues; those are not passed on.
     */
    private fun relevant(issues: Iterable<LanguageIssue>) = issues.filter { it.kind != LanguageIssueKind.INFORMATION }

    private fun toItemIssues(issues: Iterable<LanguageIssue>) =
        issues.map { IssueDefault<DataItemIssueLocation>(IssueKind.valueOf(it.kind.name), AglTextIssueLocation(it.location), it.message, it.data) }

    /**
     * A unit being read. 'locations' is a list, not a map, because the hash code of an AsmStructureSimple changes when
     * semantic analysis gives it a qualified name; the map is built (again) after each step that may change it.
     */
    private class Parsed(
        val item: DataItem,
        val unit: ParsableUnit,
        val sentenceIdentity: List<DataItemIdentity>,
        val asm: Asm?,
        val locations: List<Pair<Any, InputLocation>>,
        val issues: MutableList<LanguageIssue>
    ) {
        var resolvedReferences: List<ResolvedReference> = emptyList()
        val hasErrors get() = issues.any { it.kind == LanguageIssueKind.ERROR }
        fun locationMap(others: Map<Any, InputLocation> = emptyMap()) = LocationMapOf(locations.toMap().toMutableMap(), others)

        /**
         * the locations of the elements that have a qualified name in the scope (the possible targets of references from other units).
         * Other elements are left out: an AsmStructureSimple with no qualified name is equal to one in another unit at the same parse path.
         */
        fun scopedLocations() = locations.filter { (it.first as? AsmStructure)?.semanticQualifiedPath != null }
    }

    /** parses the unit's text, without semantic analysis; for an input that is not a unit, the item with an error */
    private suspend fun syntaxPass(item: DataItem): Any {
        val unit = unitOf(item) ?: return notAUnitItem(item)
        val sentenceIdentity = sentenceIdentityFor(item, unit)
        val res = _parsedConfig.processor.process(unit.text, processOptions(sentenceIdentity, null, semanticAnalysis = false, buildScope = false, resolve = false))
        val asm = res.asm
        val lm = res.syntaxAnalysis?.locationMap
        // copied now: the processor clears its location map on the next parse
        val locations = if (null != asm && null != lm) locationsOf(asm, lm) else emptyList()
        return Parsed(item, unit, sentenceIdentity, asm, locations, relevant(res.allIssues.all).toMutableList())
    }

    /** adds the unit's scope to 'context' */
    private fun scopePass(p: Parsed, context: SentenceContext?) {
        if (null == context || null == p.asm || p.hasErrors) return
        val res = _parsedConfig.processor.semanticAnalysis(
            p.asm, processOptions(p.sentenceIdentity, context, semanticAnalysis = true, buildScope = true, resolve = false, locationMap = p.locationMap())
        )
        p.issues.addAll(relevant(res.issues.all))
    }

    /** resolves the unit's references in 'context', sets the unit's asm, locations and references, and gives the output item */
    private suspend fun resolvePass(p: Parsed, context: SentenceContext?, otherUnitLocations: Map<Any, InputLocation> = emptyMap()): DataItem {
        if (null != context && null != p.asm && p.hasErrors.not()) {
            val res = _parsedConfig.processor.semanticAnalysis(
                p.asm, processOptions(p.sentenceIdentity, context, semanticAnalysis = true, buildScope = false, resolve = true, locationMap = p.locationMap(otherUnitLocations))
            )
            // copied now: the processor reuses its lists for the next analysis
            p.resolvedReferences = res.resolvedReferences.toList()
            p.issues.addAll(relevant(res.issues.all))
        }
        val ok = p.hasErrors.not()
        // one parse gives one root
        p.unit.asm = if (ok) p.asm?.root?.firstOrNull() else null
        p.unit.locations = if (ok) p.locations.toMap() else emptyMap()
        p.unit.resolvedReferences = if (ok) p.resolvedReferences else emptyList()
        return unitItem(p.item, p.unit, net.akehurst.kotlinx.issues.ram.IssueHolder(toItemIssues(p.issues)))
    }

    /**
     * Parses the text of the input unit, and sets its 'asm', 'locations' and 'resolvedReferences'.
     * References are resolved within the unit (a new context for each unit).
     * If parsing fails, the item has the issues and 'asm' is null, so the rest of the flow is still read.
     */
    private suspend fun parseUnit(item: DataItem): DataItem = when (val p = syntaxPass(item)) {
        is Parsed -> {
            val context = newContextOrNull()
            scopePass(p, context)
            resolvePass(p, context)
        }

        else -> p as DataItem
    }

    /**
     * Reads all the input, then resolves references across its units, with one context for this read:
     * first each unit is parsed and its scope added to the context, then the references of each unit are resolved.
     * Emits the items, in input order, once all are done. A unit whose identity is the same as an earlier unit's
     * gets an error issue, and its references are resolved within the unit only.
     */
    private fun readAcrossUnits(input: Flow<DataItem>): Flow<DataItem> = flow {
        val context = newContextOrNull()
        val seen = mutableSetOf<List<DataItemIdentity>>()
        // pass 1: parse each unit and add its scope to the shared context
        val parsed: List<Any> = input.toList().map { item ->
            when (val p = syntaxPass(item)) {
                is Parsed -> when {
                    seen.add(p.sentenceIdentity) -> p.also { scopePass(it, context) }
                    else -> {
                        val single = parseUnit(item)
                        DataItemDefault(
                            single.parent, single.identity, single.data, single.provenance,
                            net.akehurst.kotlinx.issues.ram.IssueHolder(
                                listOf(
                                    IssueDefault<DataItemIssueLocation>(
                                        IssueKind.ERROR, AglTextIssueLocation(null),
                                        "Another unit in this read has the same identity ${p.sentenceIdentity}, so references are resolved within this unit only."
                                    )
                                ) + single.issues.all
                            )
                        )
                    }
                }

                else -> p
            }
        }
        // pass 2: resolve the references of each unit; the locations of targets in other units come from those units
        val scopedLocations = parsed.filterIsInstance<Parsed>().flatMap { it.scopedLocations() }.toMap()
        for (p in parsed) {
            emit(
                when (p) {
                    is Parsed -> resolvePass(p, context, scopedLocations)
                    else -> p as DataItem
                }
            )
        }
    }

    /** copies the locations of the elements of 'asm' from 'locationMap' */
    private fun locationsOf(asm: Asm, locationMap: LocationMap): List<Pair<Any, InputLocation>> {
        val result = mutableListOf<Pair<Any, InputLocation>>()
        fun put(value: Any?) {
            if (null != value) locationMap[value]?.let { result.add(Pair(value, it)) }
        }
        asm.traverseDepthFirst(object : AsmTreeWalker {
            override fun beforeRoot(root: Any) = put(root)
            override fun afterRoot(root: Any) {}
            override fun onNothing(owningProperty: AsmStructureProperty?, value: Unit) {}
            override fun onPrimitive(owningProperty: AsmStructureProperty?, value: Any) = put(value)
            override fun beforeStructure(owningProperty: AsmStructureProperty?, value: AsmStructure) = put(value)
            override fun onProperty(owner: AsmStructure, property: AsmStructureProperty) {}
            override fun afterStructure(owningProperty: AsmStructureProperty?, value: AsmStructure) {}
            override fun beforeList(owningProperty: AsmStructureProperty?, value: Collection<*>) = put(value)
            override fun afterList(owningProperty: AsmStructureProperty?, value: Collection<*>) {}
        })
        return result
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
        if (res.issues.errors.isEmpty()) {
            unit.text = res.sentence ?: ""
            unit.locations = emptyMap()
            unit.resolvedReferences = emptyList()
        }
        return unitItem(item, unit, issues)
    }
}
/** a LocationMap over locations copied from a processor's location map (which the processor clears on its next parse) */
private class LocationMapOf(
    private val map: MutableMap<Any, InputLocation>,
    /** looked up when 'map' has no location for an object, e.g. the locations of elements in other units */
    private val others: Map<Any, InputLocation> = emptyMap()
) : LocationMap {
    override fun clear() = map.clear()
    override fun add(path: ParsePath, obj: Any, location: InputLocation) {
        map[obj] = location
    }

    override fun get(obj: Any?): InputLocation? = obj?.let { map[it] ?: others[it] }
    override fun getByPath(obj: Any?, path: ParsePath): InputLocation? = get(obj)
}
