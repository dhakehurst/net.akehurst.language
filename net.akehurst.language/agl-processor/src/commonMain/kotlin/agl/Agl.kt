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

import net.akehurst.language.agl.expressions.processor.ObjectGraphByReflection
import net.akehurst.language.agl.processor.*
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypesDomain
import net.akehurst.language.agl.semanticAnalyser.contextFromTypesDomain
import net.akehurst.language.agl.simple.ContextFromGrammarAndTypesDomain
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.agl.syntaxAnalyser.*
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.processor.ObjectGraph
import net.akehurst.language.format.asm.AglFormatDomainDefault
import net.akehurst.language.format.processor.FormatterOverTypedObject
import net.akehurst.language.formatter.api.AglFormatDomain
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.grammar.asm.GrammarDomainDefault
import net.akehurst.language.grammar.processor.contextFromGrammar
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.parser.api.ParseOptions
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import net.akehurst.language.style.api.AglStyleDomain
import net.akehurst.language.style.asm.AglStyleDomainDefault
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.asmTransform.asm.AsmTransformDomainDefault
import net.akehurst.language.expressions.processor.TypedObject
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.TypesDomainSimple

object Agl {

    val version: String = BuildConfig.version
    val buildStamp: String = BuildConfig.buildStamp

    val registry: LanguageRegistry = LanguageRegistryDefault()

    fun <AsmType : Any, ContextType : Any> configurationEmpty(): LanguageProcessorConfiguration<AsmType, ContextType> = LanguageProcessorConfigurationEmpty()

    fun <AsmType : Any, ContextType : Any> configurationBase(): LanguageProcessorConfiguration<AsmType, ContextType> = LanguageProcessorConfigurationBase()

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
            typesResolver = { p -> ProcessResultDefault<TypesDomain>(languageObject.typesDomain) },
            transformResolver = { p -> ProcessResultDefault<AsmTransformDomain>(languageObject.asmTransformDomain) },
            crossReferenceResolver = { p -> ProcessResultDefault<CrossReferenceDomain>(languageObject.crossReferenceDomain) },
            syntaxAnalyserResolver = { p -> ProcessResultDefault<SyntaxAnalyser<AsmType>>(languageObject.syntaxAnalyser) },
            semanticAnalyserResolver = { p -> ProcessResultDefault<SemanticAnalyser<AsmType, ContextType>>(languageObject.semanticAnalyser) },
            formatResolver = { p -> ProcessResultDefault<AglFormatDomain>(languageObject.formatDomain) },
            styleResolver = { p -> ProcessResultDefault<AglStyleDomain>(languageObject.styleDomain) },
            completionProviderResolver = { p -> ProcessResultDefault<CompletionProvider<AsmType, ContextType>>(languageObject.completionProvider) },
        )

    fun configurationSimple(): LanguageProcessorConfiguration<Asm, ContextWithScope<Any, Any>> = LanguageProcessorConfigurationSimple()

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

    fun <AsmType : Any, ContextType : Any> processorFromGrammar(
        grammarDomain: GrammarDomain,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>? = null
    ): LanguageProcessor<AsmType, ContextType> {
        val config = configuration ?: configurationBase()
        return LanguageProcessorDefault<AsmType, ContextType>(grammarDomain, config)
    }

    /**
     * Create a LanguageProcessor from a grammar definition string,
     * using default configuration of:
     * - targetGrammar = last grammar defined in grammarDefinitionStr
     * - defaultGoalRuleName = first non skip goal in targetGrammar
     * - syntaxAnalyser = SyntaxAnalyserSimple(TypesDomainFromGrammar)
     * - semanticAnalyser = SemanticAnalyserSimple
     * - formatter = null (TODO)
     *
     * @param grammarDefinitionStr a string defining the grammar, may contain multiple grammars
     * @param aglOptions options to the AGL grammar processor for parsing the grammarDefinitionStr
     */
    fun processorFromStringSimple(
        grammarDefinitionStr: GrammarString,
        typeStr: TypesString? = null,
        transformStr: AsmTransformString? = null,
        referenceStr: CrossReferenceString? = null,
        styleStr: StyleString? = null,
        formatterStr: FormatString? = null,
        configurationBase: LanguageProcessorConfiguration<Asm, ContextWithScope<Any, Any>> = configurationSimple(),
        grammarAglOptions: ProcessOptions<GrammarDomain, ContextWithScope<Any, Any>>? = options { semanticAnalysis { context(contextFromGrammarRegistry(registry)) } }
    ): LanguageProcessorResult<Asm, ContextWithScope<Any, Any>> {
        val config = Agl.configuration(configurationBase) {
            if (null != typeStr) {
                typesResolver { p: LanguageProcessor<Asm, ContextWithScope<Any, Any>> ->
                    TypesDomainSimple.fromString(
                        SimpleName("FromGrammar" + p.grammarDomain!!.name.value),
                        contextFromGrammar(p.grammarDomain!!),
                        typeStr
                    )
                }
            }
            if (null != transformStr) {
                transformResolver { p: LanguageProcessor<Asm, ContextWithScope<Any, Any>> ->
                    AsmTransformDomainDefault.fromString(
                        ContextFromGrammarAndTypesDomain(p.grammarDomain!!, p.baseTypesDomain),
                        transformStr
                    )
                }
            }
            if (null != referenceStr) {
                crossReferenceResolver { p: LanguageProcessor<Asm, ContextWithScope<Any, Any>> -> CrossReferenceDomainDefault.fromString(ContextFromTypesDomain(p.typesDomain), referenceStr) }
            }
            if (null != styleStr) {
                styleResolver { p: LanguageProcessor<Asm, ContextWithScope<Any, Any>> -> AglStyleDomainDefault.fromString(contextFromGrammar(p.grammarDomain!!), styleStr) }
            }
            if (null != formatterStr) {
                formatResolver { p: LanguageProcessor<Asm, ContextWithScope<Any, Any>> -> Agl.formatDomain(formatterStr, p.typesDomain) }
            }
        }
        val proc = processorFromString(grammarDefinitionStr.value, config, grammarAglOptions)
        return proc
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
        configurationBase: LanguageProcessorConfiguration<Asm, ContextWithScope<Any, Any>> = configurationSimple(),
        grammarAglOptions: ProcessOptions<GrammarDomain, ContextWithScope<Any, Any>>? = options { semanticAnalysis { context(contextFromGrammarRegistry(registry)) } }
    ) = processorFromStringSimple(
        GrammarString(grammarDefinitionStr),
        typesStr?.let { TypesString(it) },
        asmTransformStr?.let { AsmTransformString(it) },
        crossReferenceStr?.let { CrossReferenceString(it) },
        styleStr?.let { StyleString(it) },
        formatterStr?.let { FormatString(it) },
        configurationBase,
        grammarAglOptions
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
        aglOptions: ProcessOptions<GrammarDomain, ContextWithScope<Any, Any>>? = Agl.options { semanticAnalysis { context(contextFromGrammarRegistry(Agl.registry)) } }
    ): LanguageProcessorResult<AsmType, ContextType> {
        return try {
            val res = Agl.grammarFromString<GrammarDomain, ContextWithScope<Any, Any>>(
                grammarDefinitionStr,
                aglOptions ?: Agl.registry.agl.grammar.processor!!.optionsDefault()
            )
            if (null == res.asm || res.asm!!.isEmpty) {
                LanguageProcessorResult(null, res.allIssues)
            } else {
                val grammarDomain = res.asm ?: error("Unable to create processor for $grammarDefinitionStr")
                val proc = processorFromGrammar(
                    grammarDomain,
                    configuration
                )
                LanguageProcessorResult(proc, res.allIssues)
            }
        } catch (e: Throwable) {
            throw LanguageProcessorException("Unable to create processor for grammarDefinitionStr: ${e.message}", e)
        }
    }

    fun <AsmType : Any, ContextType : Any> processorFromLanguageObject(
        languageObject: LanguageObjectAbstract<AsmType, ContextType>
    ): LanguageProcessor<AsmType, ContextType> {
        return LanguageProcessorFromLanguageObject(languageObject)
    }

    fun <AsmType : Any, ContextType : Any> fromString(
        proc: LanguageProcessor<AsmType, ContextType>,
        aglOptions: ProcessOptions<AsmType, ContextType>,
        sentence: String
    ): ProcessResult<AsmType> {
        return proc.process(sentence, aglOptions)
    }

    fun <AsmType : Any, ContextType : Any> grammarFromString(
        sentence: String?,
        aglOptions: ProcessOptions<GrammarDomain, ContextWithScope<Any, Any>>? = Agl.options { semanticAnalysis { context(contextFromGrammarRegistry(Agl.registry)) } }
    ): ProcessResult<GrammarDomain> {
        return if (null == sentence) {
            ProcessResultDefault(null)
        } else {
            val res = Agl.registry.agl.grammar.processor!!.process(sentence, aglOptions)
            ProcessResultDefault(res.asm ?: GrammarDomainDefault(SimpleName("fromString-error")), res.parse, res.syntaxAnalysis, res.semanticAnalysis)
        }
    }

    fun <AsmType : Any, ContextType : Any> languageDefinitionFromString(
        identity: LanguageIdentity,
        grammarDefinitionStr: GrammarString? = null,
        typesStr: TypesString? = null,
        asmTransformStr: AsmTransformString? = null,
        referenceStr: CrossReferenceString? = null,
        styleStr: StyleString? = null,
        formatterStr: FormatString? = null,
        configurationBase: LanguageProcessorConfiguration<AsmType, ContextType> = configurationBase(),
        grammarAglOptions: ProcessOptions<GrammarDomain, ContextWithScope<Any, Any>>? = options { semanticAnalysis { context(contextFromGrammarRegistry(registry)) } }
    ): LanguageDefinition<AsmType, ContextType> {
        val config = Agl.configuration(configurationBase) {
            grammarString(grammarDefinitionStr)
            typesString(typesStr)
            transformString(asmTransformStr)
            crossReferenceString(referenceStr)
            styleString(styleStr)
            formatString(formatterStr)
        }
        return LanguageDefinitionDefault(
            identity = identity,
            aglOptions = grammarAglOptions,
            buildForDefaultGoal = false,
            initialConfiguration = config
        )
    }

    /**
     * Create a LanguageProcessor from a grammar definition string,
     * using default configuration of:
     * - targetGrammar = last grammar defined in grammarDefinitionStr
     * - defaultGoalRuleName = first non skip goal in targetGrammar
     * - syntaxAnalyser = SyntaxAnalyserSimple(TypesDomainFromGrammar)
     * - semanticAnalyser = SemanticAnalyserSimple
     * - formatter = null (TODO)
     *
     * @param grammarDefinitionStr a string defining the grammar, may contain multiple grammars
     * @param aglOptions options to the AGL grammar processor for parsing the grammarDefinitionStr
     */
    fun languageDefinitionFromStringSimple(
        identity: LanguageIdentity,
        grammarDefinitionStr: GrammarString? = null,
        typesStr: TypesString? = null,
        asmTransformStr: AsmTransformString? = null,
        referenceStr: CrossReferenceString? = null,
        styleStr: StyleString? = null,
        formatterStr: FormatString? = null,
        configurationBase: LanguageProcessorConfiguration<Asm, ContextWithScope<Any, Any>> = configurationSimple(),
        grammarAglOptions: ProcessOptions<GrammarDomain, ContextWithScope<Any, Any>>? = options { semanticAnalysis { context(contextFromGrammarRegistry(registry)) } }
    ): LanguageDefinition<Asm, ContextWithScope<Any, Any>> {
        val config = Agl.configuration(configurationBase) {
            grammarString(grammarDefinitionStr)
            typesString(typesStr)
            transformString(asmTransformStr)
            crossReferenceString(referenceStr)
            styleString(styleStr)
            formatString(formatterStr)
        }
        return LanguageDefinitionDefault(
            identity = identity,
            aglOptions = grammarAglOptions,
            buildForDefaultGoal = false,
            initialConfiguration = config
        )
    }

    fun formatDomain(template: FormatString, typesDomain: TypesDomain): ProcessResult<AglFormatDomain> {
        return AglFormatDomainDefault.fromString(contextFromTypesDomain(typesDomain), template)
    }

    fun <SelfType : Any> format(formatDomain: AglFormatDomain, objectGraph: ObjectGraph<SelfType>, self: SelfType, options: FormatOptions<SelfType> = FormatOptionsDefault()): FormatResult {
        val issueHolder = IssueHolder(defaultPhase = LanguageProcessorPhase.FORMAT)
        val formatter = FormatterOverTypedObject(formatDomain, objectGraph, issueHolder)
        val formatSetName = formatDomain.allDefinitions.lastOrNull()?.qualifiedName ?: error("No FormatSet found.")
        val namedValues = mutableMapOf<String, TypedObject<SelfType>>()
        options.environment.forEach { (k,v) ->
            namedValues[k] = objectGraph.toTypedObject(v)
        }
        val evc = EvaluationContext.ofSelf(objectGraph.toTypedObject(self), namedValues)
        val result = formatter.format(formatSetName, evc)
        return result
    }

    fun <SelfType : Any> formatWithTemplate(template: FormatString, typesDomain: TypesDomain, objectGraph: ObjectGraph<SelfType>, self: SelfType, options: FormatOptions<SelfType> = FormatOptionsDefault()): FormatResult {
        val formatResult = formatDomain(template, typesDomain)
        val formatDomain = formatResult.asm ?: error("AglFormatDomain not created from template.")
        val result = format(formatDomain, objectGraph, self, options)
        return result
    }

    fun <SelfType : Any> formatByReflection(template: FormatString, typesDomain: TypesDomain, self: SelfType, options: FormatOptions<SelfType> = FormatOptionsDefault()): FormatResult {
        val issueHolder = IssueHolder(defaultPhase = LanguageProcessorPhase.FORMAT)
        val objectGraph = ObjectGraphByReflection<SelfType>(typesDomain, issueHolder)
        return formatWithTemplate(template, typesDomain, objectGraph, self, options)
    }
}