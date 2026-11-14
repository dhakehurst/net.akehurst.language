/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl

import net.akehurst.language.completion.api.AglCompletionDomain
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.language.agl.processor.*
import net.akehurst.language.agl.semanticAnalyser.contextFromTypesDomain
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.agl.syntaxAnalyser.*
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.asmTransform.asm.AsmTransformDomainDefault
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObjectCombined
import net.akehurst.language.format.asm.FormatDomainDefault
import net.akehurst.language.format.processor.FormatterOverTypedObject
import net.akehurst.language.formatter.api.AglFormatDomain
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.grammar.asm.GrammarDomainDefault
import net.akehurst.language.grammar.processor.contextFromGrammar
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.DomainReference
import net.akehurst.language.m2mTransform.processor.M2MTransformResult
import net.akehurst.language.m2mTransform.processor.M2mTransformInterpreter
import net.akehurst.language.m2mTransform.processor.M2mTransformInterpreterCombined
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.parser.api.ParseOptions
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import net.akehurst.language.style.api.AglStyleDomain
import net.akehurst.language.style.asm.AglStyleDomainDefault
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.asm.TypesDomainSimple

/**
 * The entry point to AGL: create language processors, options and configurations,
 * and evaluate expressions, formats and model transforms.
 * See 5_Documentation/GETTING_STARTED.md.
 */
object Agl {

    /** the version of this AGL library */
    val version: String = BuildConfig.version
    val buildStamp: String = BuildConfig.buildStamp

    /** the registry of known languages, including the AGL DSLs themselves (`registry.agl.grammar`, `registry.agl.style`, ...) */
    val registry: LanguageRegistry = LanguageRegistryDefault()

    /** a configuration with nothing set */
    fun <AsmType : Any, ContextType : Any> configurationEmpty(): LanguageProcessorConfiguration<AsmType, ContextType> = LanguageProcessorConfigurationEmpty()

    /** a configuration that generates the types and asm-transform from the grammar; no syntax or semantic analyser is set */
    fun <AsmType : Any, ContextType : Any> configurationBase(): LanguageProcessorConfiguration<AsmType, ContextType> = LanguageProcessorConfigurationBase()

    /** a configuration that takes every part from the given [languageObject] */
    fun <AsmType : Any, ContextType : Any> configurationFromLanguageObject(languageObject: LanguageObject<AsmType, ContextType>): LanguageProcessorConfiguration<AsmType, ContextType> =
        //TODO: would a LanguageProcessorConfigurationFromLanguageObject be faster here, delaying instantiation until later!
        LanguageProcessorConfigurationBase(
            targetGrammarName = languageObject.defaultTargetGrammar.name,
            defaultGoalRuleName = GrammarRuleName(languageObject.defaultTargetGoalRule),
            grammarString = GrammarString(languageObject.grammarString),
            typesString = TypesString(languageObject.typesString),
            asmTransformString = AsmTransformString(languageObject.asmTransformString),
            crossReferenceString = CrossReferenceString(languageObject.crossReferenceString),
            styleString = StyleString(languageObject.styleString),
            formatString = FormatString(languageObject.formatString),
            completionString = CompletionString(languageObject.completionString),
            typesResolver = { p -> ProcessResultDefault<TypesDomain>(languageObject.typesDomain) },
            transformResolver = { p -> ProcessResultDefault<AsmTransformDomain>(languageObject.asmTransformDomain) },
            crossReferenceResolver = { p -> ProcessResultDefault<CrossReferenceDomain>(languageObject.crossReferenceDomain) },
            syntaxAnalyserResolver = { p -> ProcessResultDefault<SyntaxAnalyser<AsmType>>(languageObject.syntaxAnalyser) },
            semanticAnalyserResolver = { p -> ProcessResultDefault<SemanticAnalyser<AsmType, ContextType>>(languageObject.semanticAnalyser) },
            formatResolver = { p -> ProcessResultDefault<AglFormatDomain>(languageObject.formatDomain) },
            styleResolver = { p -> ProcessResultDefault<AglStyleDomain>(languageObject.styleDomain) },
            completionResolver = { p -> ProcessResultDefault<AglCompletionDomain>(languageObject.completionDomain) },
            completionProviderResolver = { p -> ProcessResultDefault<CompletionProvider<AsmType, ContextType>>(languageObject.completionProvider) },
        )

    /** like [configurationBase], plus the default syntax analyser (builds an [Asm]) and the default semantic analyser */
    fun configurationSimple(): LanguageProcessorConfiguration<Asm, SentenceContext> = LanguageProcessorConfigurationSimple()

    /**
     * build a configuration for a language processor
     * (does not set the configuration, they must be passed as argument)
     */
    fun <AsmType : Any, ContextType : Any> configuration(
        base: LanguageProcessorConfiguration<AsmType, ContextType> = Agl.configurationBase(),
        init: LanguageProcessorConfigurationBuilder<AsmType, ContextType>.() -> Unit
    ): LanguageProcessorConfiguration<AsmType, ContextType> {
        val b = LanguageProcessorConfigurationBuilder<AsmType, ContextType>(base)
        b.init()
        return b.build()
    }

    /**
     * build a set of options for a parser
     * (does not set the options, they must be passed as argument)
     */
    fun parseOptions(base: ParseOptions = ParseOptionsDefault(), init: ParseOptionsBuilder.() -> Unit): ParseOptions {
        val b = ParseOptionsBuilder(base)
        b.init()
        return b.build()
    }

    /**
     * build a set of options for a language processor
     * (does not set the options, they must be passed as argument)
     */
    fun <AsmType : Any, ContextType : Any> options(
        base: ProcessOptions<AsmType, ContextType> = ProcessOptionsDefault(),
        init: ProcessOptionsBuilder<AsmType, ContextType>.() -> Unit
    ): ProcessOptions<AsmType, ContextType> {
        val b = ProcessOptionsBuilder<AsmType, ContextType>(base)
        b.init()
        return b.build()
    }

    /** create a processor for an already parsed grammar */
    fun <AsmType : Any, ContextType : Any> processorFromGrammar(
        grammarDomain: GrammarDomain,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>? = null
    ): LanguageProcessor<AsmType, ContextType> {
        val config = configuration ?: configurationBase()
        return LanguageProcessorDefault<AsmType, ContextType>(grammarDomain, config)
    }

    /**
     * Create a LanguageProcessor that builds an [Asm], from a grammar and, optionally, the other AGL DSLs.
     * Defaults:
     * - targetGrammar = last grammar defined in grammarDefinitionStr
     * - defaultGoalRuleName = first non skip rule in targetGrammar (or the grammar option '#defaultGoalRule')
     * - types and asm-transform generated from the grammar, unless given
     * - syntaxAnalyser = SyntaxAnalyserSimple, semanticAnalyser = SemanticAnalyserSimple
     *
     * The result holds the processor (null if no grammar could be created) and the issues found in the definitions.
     *
     * @param grammarDefinitionStr a string defining the grammar, may contain multiple grammars
     * @param typeStr types DSL
     * @param transformStr asm-transform DSL
     * @param referenceStr references (cross-reference) DSL
     * @param styleStr style DSL
     * @param formatterStr format DSL
     * @param configurationBase the configuration to start from
     * @param grammarAglOptions options to the AGL grammar processor for parsing the grammarDefinitionStr
     * @param completionStr completion DSL
     */
    fun processorFromStringSimple(
        grammarDefinitionStr: GrammarString,
        typeStr: TypesString? = null,
        transformStr: AsmTransformString? = null,
        referenceStr: CrossReferenceString? = null,
        styleStr: StyleString? = null,
        formatterStr: FormatString? = null,
        configurationBase: LanguageProcessorConfiguration<Asm, SentenceContext> = configurationSimple(),
        grammarAglOptions: ProcessOptions<GrammarDomain, SentenceContext>? = options { semanticAnalysis { sentenceContext(contextFromRegistryGrammars(registry)) } },
        completionStr: CompletionString? = null
    ): LanguageProcessorResult<Asm, SentenceContext> {
        val config = Agl.configuration(configurationBase) {
            if (null != completionStr) {
                completionString(completionStr)
            }
            if (null != typeStr) {
                typesResolver { p: LanguageProcessor<Asm, SentenceContext> ->
                    TypesDomainSimple.fromString(
                        (p.targetGrammar?.name ?: p.grammarDomain!!.name),
                        contextFromGrammar(p.grammarDomain!!),
                        typeStr
                    )
                }
            }
            if (null != transformStr) {
                transformResolver { p: LanguageProcessor<Asm, SentenceContext> ->
                    AsmTransformDomainDefault.fromString(
                        contextFromGrammarAndTypesDomain(p.grammarDomain!!, p.baseTypesDomain),
                        transformStr
                    )
                }
            }
            if (null != referenceStr) {
                crossReferenceResolver { p: LanguageProcessor<Asm, SentenceContext> -> CrossReferenceDomainDefault.fromString(contextFromTypesDomain(p.typesDomain), referenceStr) }
            }
            if (null != styleStr) {
                styleResolver { p: LanguageProcessor<Asm, SentenceContext> -> AglStyleDomainDefault.fromString(contextFromGrammar(p.grammarDomain!!), styleStr) }
            }
            if (null != formatterStr) {
                formatResolver { p: LanguageProcessor<Asm, SentenceContext> -> Agl.formatDomain(formatterStr, p.typesDomain) }
            }
        }
        val proc = processorFromString(grammarDefinitionStr.value, config, grammarAglOptions)
        // The cross-reference domain is created lazily (types may be added to the processor before it is used),
        // so only check that the references string parses, and report its parse issues here.
        val refParseIssues = referenceStr?.let { registry.agl.crossReference.processor?.parse(it.value)?.issues?.toList() } ?: emptyList()
        return when {
            refParseIssues.isEmpty() -> proc
            else -> LanguageProcessorResult(proc.processor, IssueHolder(LanguageProcessorPhase.ALL).also { it.addAllFrom(proc.issues); it.addAll(refParseIssues) })
        }
    }

    /**
     * Java can't (yet) handle Kotlin value classes, use this instead
     */
    fun processorFromStringSimpleJava(
        grammarDefinitionStr: String,
        typesStr: String? = null,
        asmTransformStr: String? = null,
        crossReferenceStr: String? = null,
        styleStr: String? = null,
        formatterStr: String? = null,
        configurationBase: LanguageProcessorConfiguration<Asm, SentenceContext> = configurationSimple(),
        grammarAglOptions: ProcessOptions<GrammarDomain, SentenceContext>? = options { semanticAnalysis { sentenceContext(contextFromRegistryGrammars(registry)) } },
        completionStr: String? = null
    ) = processorFromStringSimple(
        GrammarString(grammarDefinitionStr),
        typesStr?.let { TypesString(it) },
        asmTransformStr?.let { AsmTransformString(it) },
        crossReferenceStr?.let { CrossReferenceString(it) },
        styleStr?.let { StyleString(it) },
        formatterStr?.let { FormatString(it) },
        configurationBase,
        grammarAglOptions,
        completionStr?.let { CompletionString(it) }
    )

    /**
     * Create a LanguageProcessor from a grammar definition string
     *
     * @param grammarDefinitionStr a string defining the grammar, may contain multiple grammars
     * @param configuration options for configuring the created language processor
     * @param aglOptions options to the AGL grammar processor
     */
    fun <AsmType : Any, ContextType : Any> processorFromString(
        grammarDefinitionStr: String,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>? = configurationBase(),
        aglOptions: ProcessOptions<GrammarDomain, SentenceContext>? = Agl.options { semanticAnalysis { sentenceContext(contextFromRegistryGrammars(Agl.registry)) } }
    ): LanguageProcessorResult<AsmType, ContextType> {
        return try {
            val res = Agl.grammarFromString<GrammarDomain, SentenceContext>(
                grammarDefinitionStr,
                aglOptions ?: Agl.registry.agl.grammar.processor!!.optionsDefault()
            )
            when {
                null == res.asm || res.asm!!.isEmpty -> LanguageProcessorResult(null, res.allIssues)
                // the grammar has errors (e.g. a rule that is not found), a processor cannot be built from it
                res.allIssues.errors.isNotEmpty() -> LanguageProcessorResult(null, res.allIssues)
                else -> {
                    val grammarDomain = res.asm ?: error("Unable to create processor for $grammarDefinitionStr")
                    val proc = processorFromGrammar(
                        grammarDomain,
                        configuration
                    )
                    LanguageProcessorResult(proc, res.allIssues)
                }
            }
        } catch (e: Throwable) {
            throw LanguageProcessorException("Unable to create processor for grammarDefinitionStr: ${e.message}", e)
        }
    }

    /** create a processor whose parts all come from the given [languageObject] */
    fun <AsmType : Any, ContextType : Any> processorFromLanguageObject(
        languageObject: LanguageObjectAbstract<AsmType, ContextType>
    ): LanguageProcessor<AsmType, ContextType> {
        return LanguageProcessorFromLanguageObject(languageObject)
    }

    /** same as `proc.process(sentence, aglOptions)` */
    fun <AsmType : Any, ContextType : Any> fromString(
        proc: LanguageProcessor<AsmType, ContextType>,
        aglOptions: ProcessOptions<AsmType, ContextType>,
        sentence: String
    ): ProcessResult<AsmType> {
        return proc.process(sentence, aglOptions)
    }

    /** parse a grammar definition string into a [GrammarDomain] */
    fun <AsmType : Any, ContextType : Any> grammarFromString(
        sentence: String?,
        aglOptions: ProcessOptions<GrammarDomain, SentenceContext>? = Agl.options { semanticAnalysis { sentenceContext(contextFromRegistryGrammars(Agl.registry)) } }
    ): ProcessResult<GrammarDomain> {
        return if (null == sentence) {
            ProcessResultDefault(null)
        } else {
            val res = Agl.registry.agl.grammar.processor!!.process(sentence, aglOptions)
            ProcessResultDefault(res.asm ?: GrammarDomainDefault(SimpleName("fromString-error")), res.parse, res.syntaxAnalysis, res.semanticAnalysis)
        }
    }

    /**
     * Create a modifiable [LanguageDefinition] from the AGL DSL strings.
     * Its processor is created when first used, and recreated when a string is changed with [LanguageDefinition.update].
     */
    fun <AsmType : Any, ContextType : Any> languageDefinitionFromString(
        identity: LanguageIdentity,
        grammarDefinitionStr: GrammarString? = null,
        typesStr: TypesString? = null,
        asmTransformStr: AsmTransformString? = null,
        referenceStr: CrossReferenceString? = null,
        styleStr: StyleString? = null,
        formatterStr: FormatString? = null,
        completionStr: CompletionString? = null,
        configurationBase: LanguageProcessorConfiguration<AsmType, ContextType> = configurationBase(),
        grammarAglOptions: ProcessOptions<GrammarDomain, SentenceContext>? = options { semanticAnalysis { sentenceContext(contextFromRegistryGrammars(registry)) } }
    ): LanguageDefinition<AsmType, ContextType> {
        val config = Agl.configuration(configurationBase) {
            grammarString(grammarDefinitionStr)
            typesString(typesStr)
            transformString(asmTransformStr)
            crossReferenceString(referenceStr)
            styleString(styleStr)
            formatString(formatterStr)
            completionString(completionStr)
        }
        return LanguageDefinitionDefault(
            identity = identity,
            aglOptions = grammarAglOptions,
            buildForDefaultGoal = false,
            initialConfiguration = config
        )
    }

    /**
     * Like [languageDefinitionFromString], using [configurationSimple], so its processor builds an [Asm].
     * The [LanguageDefinition.styleDomain] holds the parsed styles.
     *
     * @param grammarDefinitionStr a string defining the grammar, may contain multiple grammars
     * @param grammarAglOptions options to the AGL grammar processor for parsing the grammarDefinitionStr
     */
    fun languageDefinitionFromStringSimple(
        identity: LanguageIdentity,
        grammarDefinitionStr: GrammarString? = null,
        typesStr: TypesString? = null,
        asmTransformStr: AsmTransformString? = null,
        referenceStr: CrossReferenceString? = null,
        styleStr: StyleString? = null,
        formatterStr: FormatString? = null,
        completionStr: CompletionString? = null,
        configurationBase: LanguageProcessorConfiguration<Asm, SentenceContext> = configurationSimple(),
        grammarAglOptions: ProcessOptions<GrammarDomain, SentenceContext>? = options { semanticAnalysis { sentenceContext(contextFromRegistryGrammars(registry)) } }
    ): LanguageDefinition<Asm, SentenceContext> {
        val config = Agl.configuration(configurationBase) {
            grammarString(grammarDefinitionStr)
            typesString(typesStr)
            transformString(asmTransformStr)
            crossReferenceString(referenceStr)
            styleString(styleStr)
            formatString(formatterStr)
            completionString(completionStr)
        }
        return LanguageDefinitionDefault(
            identity = identity,
            aglOptions = grammarAglOptions,
            buildForDefaultGoal = false,
            initialConfiguration = config
        )
    }

    /** parse a format DSL string, for objects of the given types */
    fun formatDomain(template: FormatString, typesDomain: TypesDomain): ProcessResult<AglFormatDomain> {
        return FormatDomainDefault.fromString(contextFromTypesDomain(typesDomain), template)
    }

    /** format [self] as text with the last format set in [formatDomain] */
    fun <SelfType : Any> format(
        formatDomain: AglFormatDomain,
        objectGraph: ObjectGraphAccessorMutator,
        self: SelfType,
        options: FormatOptions<SelfType> = FormatOptionsDefault()
    ): FormatResult {
        val formatter = FormatterOverTypedObject(formatDomain, objectGraph)
        val formatSetName = formatDomain.allDefinitions.lastOrNull()?.qualifiedName ?: error("No FormatSet found.")
        val namedValues = mutableMapOf<String, TypedObject>()
        options.environment.forEach { (k, v) ->
            namedValues[k] = objectGraph.toTypedObject(v, StdLibDefault.AnyType)
        }
        val evc = EvaluationContext.ofSelf(objectGraph.toTypedObject(self, StdLibDefault.AnyType), namedValues)
        val result = formatter.format(formatSetName, evc)
        return result
    }

    /** parse [template] (format DSL) and format [self] with it */
    fun <SelfType : Any> formatWithTemplate(
        template: FormatString,
        typesDomain: TypesDomain,
        objectGraph: ObjectGraphAccessorMutator,
        self: SelfType,
        options: FormatOptions<SelfType> = FormatOptionsDefault()
    ): FormatResult {
        val formatResult = formatDomain(template, typesDomain)
        val formatDomain = formatResult.asm ?: error("AglFormatDomain not created from template.")
        if (null==options.locationMap) {
            options.locationMap = formatResult.syntaxAnalysis?.locationMap
        }
        val result = format(formatDomain, objectGraph, self, options)
        return result
    }

    /** format a Kotlin object [self] (accessed by reflection) with [template] (format DSL) */
    fun <SelfType : Any> formatByReflection(template: FormatString, typesDomain: TypesDomain, self: SelfType, options: FormatOptions<Any> = FormatOptionsDefault()): FormatResult {
        val issueHolder = IssueHolder(defaultPhase = LanguageProcessorPhase.FORMAT)
        val objectGraph = ObjectGraphAccessorMutatorByReflection(typesDomain, issueHolder, options.locationMap ?: LocationMapDefault())
        return formatWithTemplate(template, typesDomain, objectGraph, self, options)
    }

    /**
     * Evaluate an expression (AGL expressions DSL) with [self] as `${'$'}self`.
     * [accessorMutator] gives access to the objects, e.g. `objectGraphSimpleAsm(...)` for an Asm.
     */
    fun executeExpression(accessorMutator:ObjectGraphAccessorMutator, self:Any?, expression:String): TypedObject {
        val typedSelf = accessorMutator.toTypedObject(self, StdLibDefault.AnyType)
        val evc = EvaluationContext.ofSelf(typedSelf)
        return executeExpressionWithEvaluationContext(accessorMutator, evc, expression)
    }

    /** like [executeExpression], with a full [EvaluationContext] (self and named values) */
    fun executeExpressionWithEvaluationContext(accessorMutator:ObjectGraphAccessorMutator, evc:EvaluationContext, expression:String): TypedObject {
        val interpreter = ExpressionsInterpreterOverTypedObject(accessorMutator)
        val result = interpreter.evaluateStr(evc, expression)
        return result
    }

    /** like [executeExpression], where the object graph has suspending accessors */
    suspend fun executeExpressionSuspend(accessorMutator: ObjectGraphAccessorMutator, self:Any?, expression:String): TypedObject {
        val typedSelf = accessorMutator.toTypedObject(self, StdLibDefault.AnyType)
        val evc = EvaluationContext.ofSelf(typedSelf)
        return executeExpressionWithEvaluationContextSuspend(accessorMutator, evc, expression)
    }

    /** like [executeExpressionWithEvaluationContext], where the object graph has suspending accessors */
    suspend fun executeExpressionWithEvaluationContextSuspend(accessorMutator: ObjectGraphAccessorMutator, evc:EvaluationContext, expression:String): TypedObject {
        val interpreter = ExpressionsInterpreterOverTypedObjectCombined(accessorMutator)
        val result = interpreter.evaluateStrSuspend(evc, expression)
        return result
    }

    /**
     * Run a model-to-model transform (m2m-transform DSL). The first transform in [m2m] is used.
     *
     * @param typeDomains the types of each domain parameter of the transform
     * @param accessorMutators how to access the objects of each types domain, keyed by the types domain name
     * @param domains the input objects of each source domain
     * @param ruleArguments values for rule parameters
     * @param targetDomainReference the domain to create
     */
    fun transform(
        m2m: M2mTransformString,
        typeDomains: Map<DomainReference, TypesDomain>,
        accessorMutators: Map<SimpleName, ObjectGraphAccessorMutator>,
        domains: Map<DomainReference, List<TypedObject>>,
        ruleArguments: Map<String, TypedObject>,
        targetDomainReference: DomainReference
    ): M2MTransformResult {
        val sentenceContext = SentenceContextAny()
        typeDomains.forEach { (k, v) ->
            sentenceContext.addToScope(null, listOf(v.name.value), QualifiedName("TypesDomain"), null, v)
        }
        val res = Agl.registry.agl.m2mTransform.processor!!.process(
            m2m.value,
            options = Agl.options {
                semanticAnalysis {
                    sentenceContext(sentenceContext)
                }
            }
        )
        val m2m = res.let {
            check(it.allIssues.errors.isEmpty()) { it.allIssues.toString() }
            it.asm!!
        }
        val issueHolder = IssueHolder(defaultPhase = LanguageProcessorPhase.INTERPRET)
        val interpreter = M2mTransformInterpreter(m2m, accessorMutators, issueHolder)
        val tgtTransform = m2m.allTransformRuleSet.first() //TODO: allow caller to choose
        val transResult = interpreter.transform(tgtTransform, targetDomainReference, ruleArguments, domains)
        return transResult
    }

    /** like [transform], where the object graphs have suspending accessors */
    suspend fun transformSuspend(
        m2m: M2mTransformString,
        typeDomains: Map<DomainReference, TypesDomain>,
        accessorMutators: Map<SimpleName, ObjectGraphAccessorMutator>,
        domains: Map<DomainReference, List<TypedObject>>,
        ruleArguments: Map<String, TypedObject>,
        targetDomainReference: DomainReference
    ): M2MTransformResult {
        val sentenceContext = SentenceContextAny()
        typeDomains.forEach { (k, v) ->
            sentenceContext.addToScope(null, listOf(v.name.value), QualifiedName("TypesDomain"), null, v)
        }
        val res = Agl.registry.agl.m2mTransform.processor!!.process(
            m2m.value,
            options = Agl.options {
                semanticAnalysis {
                    sentenceContext(sentenceContext)
                }
            }
        )
        val m2m = res.let {
            check(it.allIssues.errors.isEmpty()) { it.allIssues.toString() }
            it.asm!!
        }
        val issueHolder = IssueHolder(defaultPhase = LanguageProcessorPhase.INTERPRET)
        val interpreter = M2mTransformInterpreterCombined(m2m, accessorMutators, issueHolder)
        val tgtTransform = m2m.allTransformRuleSet.first() //TODO: allow caller to choose
        val transResult = interpreter.transformSuspend(tgtTransform, targetDomainReference, ruleArguments, domains)
        return transResult
    }
}