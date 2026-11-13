package net.akehurst.datastore.text

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import net.akehurst.language.api.processor.AsmTransformString
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.StyleString
import net.akehurst.language.api.processor.TypesString
import net.akehurst.kotlinx.filesystem.UserFileSystemDefault
import net.akehurst.kotlinx.filesystem.api.FileAccessMode
import net.akehurst.kotlinx.filesystem.api.FileHandle
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
import net.akehurst.language.types.api.TypeDefinition
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.agl.simple.SemanticAnalyserSimple
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.expressions.processor.objectGraphSimpleAsm
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import net.akehurst.language.sentence.common.SentenceDefault

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
 * Where an element of a unit's asm is in the unit's text, e.g. for a deep link into a file.
 * 'position' and 'length' are as in the element's InputLocation (ParsableUnit.locationOf);
 * lines and columns count from 1: 'start' is the first character of its first token, 'end' the last character of its last token.
 */
data class AglTextElementLocation(
    val position: Int,
    val length: Int,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int
)

/**
 * Provenance of a DataItem produced by an AglTextDatastore.
 * 'elementLocation' is set for the item of an element of a unit's asm (see AglTextProvider.CONFIG_ITEM_TYPES), null for a unit's item.
 */
data class AglTextProvenance(
    val datastoreIdentity: DatastoreIdentity,
    val elementLocation: AglTextElementLocation? = null
) : DataItemProvenance

open class AglTextDatastore(
    identity: DatastoreIdentity,
    configuration: DatastoreConfiguration
) : DatastoreAbstract(identity, configuration) {

    companion object {

        /** the definitions of a language, as strings; only the grammar is required */
        data class AglTextDefinitions(
            val grammar: String,
            val types: String? = null,
            val transform: String? = null,
            val crossReference: String? = null,
            val style: String? = null,
            val format: String? = null
        )

        class AglTextConfig(val configuration: DatastoreConfiguration) {

            /** the language directory, if given; then the definitions come from its files, not from the definition strings */
            val languageDirectory: String? get() = configuration.getAsStringOrNull(AglTextProvider.CONFIG_LANGUAGE_DIRECTORY)

            /** the names given by config.item-types (empty if not given) */
            val itemTypeNames: List<String>
                get() = (configuration.valueOf(AglTextProvider.CONFIG_ITEM_TYPES) as List<*>?)?.map { it.toString().trim() }?.filter { it.isNotEmpty() } ?: emptyList()

            private val _mutex = Mutex()
            private var _definitions: AglTextDefinitions? = null
            private var _processor: LanguageProcessor<Asm, SentenceContext>? = null
            private var _itemTypes: List<TypeDefinition>? = null

            /**
             * the types named by config.item-types, found in the processor's types: by qualified name, or by simple name if
             * exactly one type has it. Fails if a name is not found, or a simple name is ambiguous. Found once.
             */
            suspend fun itemTypes(): List<TypeDefinition> {
                _itemTypes?.let { return it }
                val names = itemTypeNames
                val result = when {
                    names.isEmpty() -> emptyList()
                    else -> {
                        val td = processor().typesDomain
                        names.map { name ->
                            when {
                                name.contains('.') -> td.findByQualifiedNameOrNull(QualifiedName(name))
                                    ?: error("${AglTextProvider.CONFIG_ITEM_TYPES}: there is no type '$name' in the language's types")

                                else -> {
                                    val found = td.namespace.mapNotNull { it.findOwnedTypeNamed(SimpleName(name)) }
                                    when (found.size) {
                                        0 -> error("${AglTextProvider.CONFIG_ITEM_TYPES}: there is no type '$name' in the language's types")
                                        1 -> found[0]
                                        else -> error("${AglTextProvider.CONFIG_ITEM_TYPES}: more than one type is named '$name' (${found.joinToString { it.qualifiedName.value }}), use a qualified name")
                                    }
                                }
                            }
                        }
                    }
                }
                _itemTypes = result
                return result
            }

            /** the definitions, from the language directory if given, else from the definition strings. Read once. */
            suspend fun definitions(): AglTextDefinitions = _mutex.withLock { definitionsUnlocked() }

            private suspend fun definitionsUnlocked(): AglTextDefinitions = _definitions ?: run {
                val dirPath = languageDirectory
                when (dirPath) {
                    null -> AglTextDefinitions(
                        grammar = configuration.getAsString(AglTextProvider.CONFIG_GRAMMAR_STR),
                        crossReference = configuration.getAsStringOrNull(AglTextProvider.CONFIG_CROSS_REFERENCE_STR),
                        format = configuration.getAsString(AglTextProvider.CONFIG_FORMAT_STR)
                    )

                    else -> definitionsFromDirectory(dirPath)
                }
            }.also { _definitions = it }

            /** the language processor, built from the definitions. Built once. */
            suspend fun processor(): LanguageProcessor<Asm, SentenceContext> = _mutex.withLock {
                _processor ?: run {
                    val defs = definitionsUnlocked()
                    Agl.processorFromString(
                        grammarDefinitionStr = defs.grammar,
                        configuration = Agl.configuration(base = Agl.configurationSimple()) {
                            defs.types?.let { this.typesString(TypesString(it)) }
                            defs.transform?.let { this.transformString(AsmTransformString(it)) }
                            defs.crossReference?.let { this.crossReferenceString(CrossReferenceString(it)) }
                            defs.style?.let { this.styleString(StyleString(it)) }
                            defs.format?.let { this.formatString(FormatString(it)) }
                        },
                        aglOptions = Agl.options { semanticAnalysis { sentenceContext(contextFromRegistryGrammars(Agl.registry)) } }
                    ).let {
                        check(it.issues.errors.isEmpty()) { "Failed to construct AglTextDatastore Configuration:\n${it.issues}" }
                        it.processor!!
                    }
                }.also { _processor = it }
            }

            /**
             * reads the definitions from the files of the directory, found by extension (see AglTextProvider.FILE_EXTENSION_*).
             * Fails if the directory is not found, there is no grammar file, or there is more than one file of a kind.
             */
            private suspend fun definitionsFromDirectory(dirPath: String): AglTextDefinitions {
                val dir = UserFileSystemDefault.getDirectory(dirPath, FileAccessMode.READ_ONLY)
                    ?: error("Language Directory '$dirPath' is not found")
                val files = dir.listContent().filterIsInstance<FileHandle>()
                suspend fun read(extension: String): String? {
                    val found = files.filter { it.extension == extension }
                    return when (found.size) {
                        0 -> null
                        1 -> found[0].readContent() ?: error("Cannot read '${found[0].path}' in Language Directory '$dirPath'")
                        else -> error("Language Directory '$dirPath' has more than one '.$extension' file: ${found.joinToString { it.name }}")
                    }
                }
                return AglTextDefinitions(
                    grammar = read(AglTextProvider.FILE_EXTENSION_GRAMMAR)
                        ?: error("Language Directory '$dirPath' must hold a grammar file (a '.${AglTextProvider.FILE_EXTENSION_GRAMMAR}' file)"),
                    types = read(AglTextProvider.FILE_EXTENSION_TYPES),
                    transform = read(AglTextProvider.FILE_EXTENSION_TRANSFORM),
                    crossReference = read(AglTextProvider.FILE_EXTENSION_CROSS_REFERENCE),
                    style = read(AglTextProvider.FILE_EXTENSION_STYLE),
                    format = read(AglTextProvider.FILE_EXTENSION_FORMAT)
                )
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
            .merge(_parsedConfig.processor().typesDomain)
            .merge(_inputTypes)

    /**
     * input: DataItem with ParsableUnit data (or String data, for which a ParsableUnit is created)
     * output: DataItem with the same ParsableUnit, whose 'asm' is set to the result of parsing its 'text'.
     * The output item has the input item's parent and identity (for String input, the input item is its parent).
     */
    override suspend fun doRead(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> {
        val acrossUnits = accessConfiguration.getAsBooleanOrNull(AglTextProvider.ACCESS_RESOLVE_ACROSS_UNITS) ?: false
        return when {
            acrossUnits && null != _parsedConfig.definitions().crossReference -> readAcrossUnits(input)
            else -> flow { input.collect { item -> parseUnit(item).forEach { emit(it) } } }
        }
    }

    /**
     * input: DataItem with ParsableUnit data
     * output: DataItem with the same ParsableUnit, whose 'text' is set to the result of formatting its 'asm'
     */
    override suspend fun doWrite(accessConfiguration: DatastoreConfiguration, input: Flow<DataItem>): Flow<DataItem> {
        return input.map { formatUnit(accessConfiguration, it) }
    }

    /**
     * AglTextProvenance of this datastore, for every item read or written.
     * 'source' is the ParsableUnit of the item, or null for an error item whose input is not a unit.
     */
    override fun provenanceFor(input: DataItem?, source: Any?): DataItemProvenance = AglTextProvenance(identity)

    /**
     * Provenance of the item of an element of a unit's asm (see AglTextProvider.CONFIG_ITEM_TYPES):
     * by default AglTextProvenance of this datastore with the element's location.
     * 'unitItem' is the item of the element's unit, 'location' is null if the element's location is not known.
     */
    open fun provenanceForElement(unitItem: DataItem, element: Any, location: AglTextElementLocation?): DataItemProvenance =
        AglTextProvenance(identity, location)

    // ---

    /**
     * the output item for 'unit'. If the input item holds the unit, the output keeps its parent, identity and data
     * (so the data's accessor-mutator, and any links it holds, e.g. in a composition, still apply);
     * otherwise the output is a child of the input item.
     */
    private suspend fun unitItem(input: DataItem, unit: ParsableUnit, issues: IssueCollection<DataItemIssueLocation>): DataItem = when {
        input.data.self === unit -> DataItemDefault(input.parent, input.identity, input.data, provenanceFor(input, unit), issues)
        else -> DataItemDefault(input, DataItemIdentity("parsableUnit"), accessorMutator().toTypedObject(unit, StdLibDefault.AnyType), provenanceFor(input, unit), issues)
    }

    private fun issuesOf(vararg issue: IssueDefault<DataItemIssueLocation>) = net.akehurst.kotlinx.issues.ram.IssueHolder<DataItemIssueLocation>(issue.toList())

    /** the unit an input item holds, or for String data a new unit; null if the data is neither */
    private fun unitOf(item: DataItem): ParsableUnit? = when (val self = item.data.self) {
        is ParsableUnit -> self
        is String -> ParsableUnit(self)
        else -> null
    }

    private suspend fun notAUnitItem(item: DataItem): DataItem = DataItemDefault(
        item, DataItemIdentity("parsableUnit"), accessorMutator().nothing(), provenanceFor(item, null),
        issuesOf(IssueDefault(IssueKind.ERROR, AglTextIssueLocation(null), "the read input to an AglTextDatastore must be a ParsableUnit or a String."))
    )

    /** the qualified identity of the item read will output for 'unit': the sentence identity of the unit's text */
    private fun sentenceIdentityFor(item: DataItem, unit: ParsableUnit): List<DataItemIdentity> = when {
        item.data.self === unit -> item.qualifiedIdentity
        else -> item.qualifiedIdentity + DataItemIdentity("parsableUnit")
    }

    /** a context for semantic analysis, if the datastore has a cross-reference definition */
    private suspend fun newContextOrNull(): SentenceContext? = _parsedConfig.definitions().crossReference?.let { contextAsmSimple() }

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

    /** a unit being read */
    private class Parsed(
        val item: DataItem,
        val unit: ParsableUnit,
        val sentenceIdentity: List<DataItemIdentity>,
        val asm: Asm?,
        val locations: Map<Any, InputLocation>,
        val issues: MutableList<LanguageIssue>
    ) {
        var resolvedReferences: List<ResolvedReference> = emptyList()
        val hasErrors get() = issues.any { it.kind == LanguageIssueKind.ERROR }
        fun locationMap(others: Map<Any, InputLocation> = emptyMap()) = LocationMapOf(locations.toMutableMap(), others)
    }

    /** parses the unit's text, without semantic analysis; for an input that is not a unit, the item with an error */
    private suspend fun syntaxPass(item: DataItem): Any {
        val unit = unitOf(item) ?: return notAUnitItem(item)
        val sentenceIdentity = sentenceIdentityFor(item, unit)
        val res = _parsedConfig.processor().process(unit.text, processOptions(sentenceIdentity, null, semanticAnalysis = false, buildScope = false, resolve = false))
        val asm = res.asm
        val lm = res.syntaxAnalysis?.locationMap
        // copied now: the processor clears its location map on the next parse
        val locations = if (null != asm && null != lm) locationsOf(asm, lm) else emptyMap()
        return Parsed(item, unit, sentenceIdentity, asm, locations, relevant(res.allIssues.all).toMutableList())
    }

    /** adds the unit's scope to 'context' */
    private suspend fun scopePass(p: Parsed, context: SentenceContext?) {
        if (null == context || null == p.asm || p.hasErrors) return
        val res = _parsedConfig.processor().semanticAnalysis(
            p.asm, processOptions(p.sentenceIdentity, context, semanticAnalysis = true, buildScope = true, resolve = false, locationMap = p.locationMap())
        )
        p.issues.addAll(relevant(res.issues.all))
    }

    /** resolves the unit's references in 'context', sets the unit's asm, locations and references, and gives the output item */
    private suspend fun resolvePass(p: Parsed, context: SentenceContext?, otherUnitLocations: Map<Any, InputLocation> = emptyMap()): DataItem {
        if (null != context && null != p.asm && p.hasErrors.not()) {
            val res = _parsedConfig.processor().semanticAnalysis(
                p.asm, processOptions(p.sentenceIdentity, context, semanticAnalysis = true, buildScope = false, resolve = true, locationMap = p.locationMap(otherUnitLocations))
            )
            // copied now: the processor reuses its lists for the next analysis
            p.resolvedReferences = res.resolvedReferences.toList()
            p.issues.addAll(relevant(res.issues.all))
        }
        val ok = p.hasErrors.not()
        // one parse gives one root
        p.unit.asm = if (ok) p.asm?.root?.firstOrNull() else null
        p.unit.locations = if (ok) p.locations else emptyMap()
        p.unit.resolvedReferences = if (ok) p.resolvedReferences else emptyList()
        return unitItem(p.item, p.unit, net.akehurst.kotlinx.issues.ram.IssueHolder(toItemIssues(p.issues)))
    }

    /**
     * Parses the text of the input unit, and sets its 'asm', 'locations' and 'resolvedReferences'.
     * References are resolved within the unit (a new context for each unit).
     * If parsing fails, the item has the issues and 'asm' is null, so the rest of the flow is still read.
     * Gives the unit's item, then the items of its elements (see elementItems).
     */
    private suspend fun parseUnit(item: DataItem): List<DataItem> = when (val p = syntaxPass(item)) {
        is Parsed -> withElementItems(p, resolveWithinUnit(p))
        else -> listOf(p as DataItem)
    }

    /** builds the scope of the unit and resolves its references within the unit only; gives the unit's item */
    private suspend fun resolveWithinUnit(p: Parsed): DataItem {
        val context = newContextOrNull()
        scopePass(p, context)
        return resolvePass(p, context)
    }

    private suspend fun withElementItems(p: Parsed, unitItem: DataItem): List<DataItem> = listOf(unitItem) + elementItems(p, unitItem)

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
                        val single = resolveWithinUnit(p)
                        val unitItem = DataItemDefault(
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
                        withElementItems(p, unitItem)
                    }
                }

                else -> p
            }
        }
        // pass 2: resolve the references of each unit; the locations of targets in other units come from those units
        // (the identity of an AsmStructureSimple includes its sentence identity, so elements of different units are never equal)
        val allLocations = parsed.filterIsInstance<Parsed>().flatMap { it.locations.entries }.associate { it.key to it.value }
        for (p in parsed) {
            when (p) {
                is Parsed -> withElementItems(p, resolvePass(p, context, allLocations)).forEach { emit(it) }
                is List<*> -> p.forEach { emit(it as DataItem) }
                else -> emit(p as DataItem)
            }
        }
    }

    /** an element that is an item, and what is needed to give its children unique identities */
    private class ItemFrame(val element: Any?, val item: DataItem) {
        val usedIdentities = mutableSetOf<String>()
        val countByType = mutableMapOf<String, Int>()

        /** 'base', or if a child already has it, 'base~n' with the smallest n from 1 that no child has */
        fun uniqueIdentity(base: String): String {
            var id = base
            var n = 1
            while (usedIdentities.contains(id)) {
                id = "$base~$n"
                n++
            }
            usedIdentities.add(id)
            return id
        }

        /** the index of the next child of the type among the children of that type, from 0 */
        fun nextIndex(typeSimpleName: String): Int {
            val i = countByType[typeSimpleName] ?: 0
            countByType[typeSimpleName] = i + 1
            return i
        }
    }

    /**
     * The items of the elements of the unit's asm whose type is, or is a subtype of, one of config.item-types
     * (empty if it is not given, or the unit has errors and so no asm). Depth first, in text order.
     * Each is a child of the item of its nearest enclosing element that is an item, else of 'unitItem'.
     * Identity '<type simple name>:<id>': <id> is the element's identifying value ('identify ... by' in the cross-references,
     * in the scope the element is in), else its index among the children of its parent item with that type;
     * '~n' is added if the parent item already has a child with that identity.
     * The data is the element, typed by its type, by the accessor-mutator of the unit's item.
     * The issues of the unit whose location is inside the element's text are on the element's item too.
     */
    private suspend fun elementItems(p: Parsed, unitItem: DataItem): List<DataItem> {
        val itemTypes = _parsedConfig.itemTypes()
        val root = p.unit.asm
        if (itemTypes.isEmpty() || p.hasErrors || root !is AsmStructure) return emptyList()
        val processor = _parsedConfig.processor()
        val typesDomain = processor.typesDomain
        val crossReferenceDomain: CrossReferenceDomain? = _parsedConfig.definitions().crossReference?.let { processor.crossReferenceDomain }
        val interpreter = crossReferenceDomain?.let {
            ExpressionsInterpreterOverTypedObject(objectGraphSimpleAsm(typesDomain, it, IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS), p.locationMap()))
        }
        val accessor = unitItem.data.accessor
        val sentence = SentenceDefault(p.unit.text, p.sentenceIdentity)

        fun identifyingValue(scopeForTypeName: QualifiedName, el: AsmStructure): String? = when {
            null == crossReferenceDomain || null == interpreter -> null
            else -> when (val v = SemanticAnalyserSimple.identifyingValueInFor(interpreter, crossReferenceDomain, scopeForTypeName, el)) {
                is String -> v
                is List<*> -> v.takeIf { it.isNotEmpty() }?.joinToString(".")
                else -> null
            }
        }

        fun elementLocation(loc: InputLocation?): AglTextElementLocation? = loc?.let {
            // the last character of the last token (not trailing white space)
            var last = maxOf(it.position, it.endPosition - 1)
            while (last > it.position && last < p.unit.text.length && p.unit.text[last].isWhitespace()) last--
            val end = sentence.locationFor(last, 1)
            AglTextElementLocation(it.position, it.length, it.line, it.column, end.line, end.column)
        }

        val result = mutableListOf<DataItem>()
        val items = ArrayDeque<ItemFrame>().apply { addLast(ItemFrame(null, unitItem)) }
        // as ScopeCreator: the type of the element that created the scope each element is in
        val scopes = ArrayDeque<QualifiedName>().apply { addLast(CrossReferenceDomainDefault.ROOT_SCOPE_TYPE_NAME) }
        AsmSimple.traverseDepthFirst(listOf(root), object : AsmTreeWalker {
            override fun beforeRoot(root: Any) {}
            override fun afterRoot(root: Any) {}
            override fun onNothing(owningProperty: AsmStructureProperty?, value: Unit) {}
            override fun onPrimitive(owningProperty: AsmStructureProperty?, value: Any) {}
            override fun onProperty(owner: AsmStructure, property: AsmStructureProperty) {}
            override fun beforeList(owningProperty: AsmStructureProperty?, value: Collection<*>) {}
            override fun afterList(owningProperty: AsmStructureProperty?, value: Collection<*>) {}

            override fun beforeStructure(owningProperty: AsmStructureProperty?, value: AsmStructure) {
                val scopeForTypeName = scopes.last()
                val id = identifyingValue(scopeForTypeName, value)
                val createsScope = null != crossReferenceDomain && null != id && crossReferenceDomain.isScopeDefinedFor(typesDomain, value.qualifiedTypeName)
                scopes.addLast(if (createsScope) value.qualifiedTypeName else scopeForTypeName)

                val elType = typesDomain.findByQualifiedNameOrNull(value.qualifiedTypeName) ?: return
                if (itemTypes.none { elType == it || elType.conformsTo(it) }) return
                val parent = items.last()
                val typeName = value.qualifiedTypeName.last.value
                val index = parent.nextIndex(typeName)
                val identity = parent.uniqueIdentity("$typeName:${id ?: index}")
                val loc = p.locations[value]
                val issues = when (loc) {
                    null -> emptyList()
                    else -> p.issues.filter { iss -> iss.location?.let { it.position >= loc.position && it.endPosition <= loc.endPosition } ?: false }
                }
                val type = (accessor.typesDomain.findByQualifiedNameOrNull(value.qualifiedTypeName) ?: elType).type()
                val item = DataItemDefault(
                    parent.item, DataItemIdentity(identity), accessor.typedAs(value, type),
                    provenanceForElement(unitItem, value, elementLocation(loc)),
                    net.akehurst.kotlinx.issues.ram.IssueHolder(toItemIssues(issues))
                )
                result.add(item)
                items.addLast(ItemFrame(value, item))
            }

            override fun afterStructure(owningProperty: AsmStructureProperty?, value: AsmStructure) {
                scopes.removeLast()
                if (items.last().element === value) items.removeLast()
            }
        })
        return result
    }

    /** copies the locations of the elements of 'asm' from 'locationMap' */
    private fun locationsOf(asm: Asm, locationMap: LocationMap): Map<Any, InputLocation> {
        val result = mutableMapOf<Any, InputLocation>()
        fun put(value: Any?) {
            if (null != value) locationMap[value]?.let { result[value] = it }
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
                item.parent, item.identity, item.data, provenanceFor(item, null),
                issuesOf(IssueDefault(IssueKind.ERROR, AglTextIssueLocation(null), "the write input to an AglTextDatastore must be a ParsableUnit."))
            )
        val root = unit.asm
            ?: return unitItem(item, unit, issuesOf(IssueDefault(IssueKind.ERROR, AglTextIssueLocation(null), "the ParsableUnit has no asm to format.")))
        val asm = AsmSimple(am)
        asm.addRoot(root)
        val res = _parsedConfig.processor().formatAsm(
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
