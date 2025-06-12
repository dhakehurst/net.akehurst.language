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

import net.akehurst.language.agl.processor.*
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.agl.semanticAnalyser.contextFromTypeModel
import net.akehurst.language.agl.simple.ContextFromGrammarAndTypeModel
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.agl.syntaxAnalyser.*
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.format.asm.AglFormatModelDefault
import net.akehurst.language.formatter.api.AglFormatModel
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.grammar.asm.GrammarModelDefault
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.grammar.processor.contextFromGrammar
import net.akehurst.language.parser.api.ParseOptions
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.style.api.AglStyleModel
import net.akehurst.language.style.asm.AglStyleModelDefault
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.transform.asm.TransformDomainDefault
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.TypeModelSimple

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
            transformString = TransformString(languageObject.asmTransformString),
            crossReferenceString = CrossReferenceString(languageObject.crossReferenceString),
            styleString = StyleString(languageObject.styleString),
            formatString = FormatString(languageObject.formatString),
            typesResolver = { p -> ProcessResultDefault<TypeModel>(languageObject.typesModel) },
            transformResolver = { p -> ProcessResultDefault<TransformModel>(languageObject.asmTransformModel) },
            crossReferenceResolver = { p -> ProcessResultDefault<CrossReferenceModel>(languageObject.crossReferenceModel) },
            syntaxAnalyserResolver = { p -> ProcessResultDefault<SyntaxAnalyser<AsmType>>(languageObject.syntaxAnalyser) },
            semanticAnalyserResolver = { p -> ProcessResultDefault<SemanticAnalyser<AsmType, ContextType>>(languageObject.semanticAnalyser) },
            formatResolver = { p -> ProcessResultDefault<AglFormatModel>(languageObject.formatModel) },
            styleResolver = { p -> ProcessResultDefault<AglStyleModel>(languageObject.styleModel) },
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
        grammarModel: GrammarModel,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>? = null
    ): LanguageProcessor<AsmType, ContextType> {
        val config = configuration ?: configurationBase()
        return LanguageProcessorDefault<AsmType, ContextType>(grammarModel, config)
    }

    /**
     * Create a LanguageProcessor from a grammar definition string,
     * using default configuration of:
     * - targetGrammar = last grammar defined in grammarDefinitionStr
     * - defaultGoalRuleName = first non skip goal in targetGrammar
     * - syntaxAnalyser = SyntaxAnalyserSimple(TypeModelFromGrammar)
     * - semanticAnalyser = SemanticAnalyserSimple
     * - formatter = null (TODO)
     *
     * @param grammarDefinitionStr a string defining the grammar, may contain multiple grammars
     * @param aglOptions options to the AGL grammar processor for parsing the grammarDefinitionStr
     */
    fun processorFromStringSimple(
        grammarDefinitionStr: GrammarString,
        typeStr: TypesString? = null,
        transformStr: TransformString? = null,
        referenceStr: CrossReferenceString? = null,
        styleStr: StyleString? = null,
        formatterModelStr: FormatString? = null,
        configurationBase: LanguageProcessorConfiguration<Asm, ContextWithScope<Any, Any>> = configurationSimple(),
        grammarAglOptions: ProcessOptions<GrammarModel, ContextWithScope<Any, Any>>? = options { semanticAnalysis { context(ContextFromGrammarRegistry(registry)) } }
    ): LanguageProcessorResult<Asm, ContextWithScope<Any, Any>> {
        val config = Agl.configuration(configurationBase) {
            if (null != typeStr) {
                typesResolver { p: LanguageProcessor<Asm, ContextWithScope<Any, Any>> ->
                    TypeModelSimple.fromString(
                        SimpleName("FromGrammar" + p.grammarModel!!.name.value),
                        contextFromGrammar(p.grammarModel!!),
                        typeStr
                    )
                }
            }
            if (null != transformStr) {
                transformResolver { p: LanguageProcessor<Asm, ContextWithScope<Any, Any>> ->
                    TransformDomainDefault.fromString(
                        ContextFromGrammarAndTypeModel(p.grammarModel!!, p.baseTypeModel),
                        transformStr
                    )
                }
            }
            if (null != referenceStr) {
                crossReferenceResolver { p: LanguageProcessor<Asm, ContextWithScope<Any, Any>> -> CrossReferenceModelDefault.fromString(ContextFromTypeModel(p.typesModel), referenceStr) }
            }
            if (null != styleStr) {
                styleResolver { p: LanguageProcessor<Asm, ContextWithScope<Any, Any>> -> AglStyleModelDefault.fromString(contextFromGrammar(p.grammarModel!!), styleStr) }
            }
            if (null != formatterModelStr) {
                formatResolver { p: LanguageProcessor<Asm, ContextWithScope<Any, Any>> -> AglFormatModelDefault.fromString(contextFromTypeModel(p.typesModel), formatterModelStr) }
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
        typeModelStr: String? = null,
        transformStr: String? = null,
        crossReferenceModelStr: String? = null,
        styleModelStr: String? = null,
        formatterModelStr: String? = null,
        configurationBase: LanguageProcessorConfiguration<Asm, ContextWithScope<Any, Any>> = configurationSimple(),
        grammarAglOptions: ProcessOptions<GrammarModel, ContextWithScope<Any, Any>>? = options { semanticAnalysis { context(ContextFromGrammarRegistry(registry)) } }
    ) = processorFromStringSimple(
        GrammarString(grammarDefinitionStr),
        typeModelStr?.let { TypesString(it) },
        transformStr?.let { TransformString(it) },
        crossReferenceModelStr?.let { CrossReferenceString(it) },
        styleModelStr?.let { StyleString(it) },
        formatterModelStr?.let { FormatString(it) },
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
        aglOptions: ProcessOptions<GrammarModel, ContextWithScope<Any, Any>>? = Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } }
    ): LanguageProcessorResult<AsmType, ContextType> {
        return try {
            val res = Agl.grammarFromString<GrammarModel, ContextWithScope<Any, Any>>(
                grammarDefinitionStr,
                aglOptions ?: Agl.registry.agl.grammar.processor!!.optionsDefault()
            )
            if (null == res.asm || res.asm!!.isEmpty) {
                LanguageProcessorResult(null, res.allIssues)
            } else {
                val grammarModel = res.asm ?: error("Unable to create processor for $grammarDefinitionStr")
                val proc = processorFromGrammar(
                    grammarModel,
                    configuration
                )
                LanguageProcessorResult(proc, res.allIssues)
            }
        } catch (e: Throwable) {
            throw LanguageProcessorException("Unable to create processor for grammarDefinitionStr: ${e.message}", e)
        }
    }

    fun <AsmType : Any, ContextType : Any> processorFromGeneratedCode(
        generated: LanguageObjectAbstract<AsmType, ContextType>
    ): LanguageProcessor<AsmType, ContextType> {
        return LanguageProcessorFromLanguageObject(generated)
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
        aglOptions: ProcessOptions<GrammarModel, ContextWithScope<Any, Any>>? = Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } }
    ): ProcessResult<GrammarModel> {
        return if (null == sentence) {
            ProcessResultDefault(null)
        } else {
            val res = Agl.registry.agl.grammar.processor!!.process(sentence, aglOptions)
            ProcessResultDefault(res.asm ?: GrammarModelDefault(SimpleName("fromString-error")), res.parse, res.syntaxAnalysis, res.semanticAnalysis)
        }
    }

    fun <AsmType : Any, ContextType : Any> languageDefinitionFromString(
        identity: LanguageIdentity,
        grammarDefinitionStr: GrammarString? = null,
        typeStr: TypesString? = null,
        transformStr: TransformString? = null,
        referenceStr: CrossReferenceString? = null,
        styleStr: StyleString? = null,
        formatterModelStr: FormatString? = null,
        configurationBase: LanguageProcessorConfiguration<AsmType, ContextType> = configurationBase(),
        grammarAglOptions: ProcessOptions<GrammarModel, ContextWithScope<Any, Any>>? = options { semanticAnalysis { context(ContextFromGrammarRegistry(registry)) } }
    ): LanguageDefinition<AsmType, ContextType> {
        val config = Agl.configuration(configurationBase) {
            grammarString(grammarDefinitionStr)
            typesString(typeStr)
            transformString(transformStr)
            crossReferenceString(referenceStr)
            styleString(styleStr)
            formatString(formatterModelStr)
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
     * - syntaxAnalyser = SyntaxAnalyserSimple(TypeModelFromGrammar)
     * - semanticAnalyser = SemanticAnalyserSimple
     * - formatter = null (TODO)
     *
     * @param grammarDefinitionStr a string defining the grammar, may contain multiple grammars
     * @param aglOptions options to the AGL grammar processor for parsing the grammarDefinitionStr
     */
    fun languageDefinitionFromStringSimple(
        identity: LanguageIdentity,
        grammarDefinitionStr: GrammarString? = null,
        typeStr: TypesString? = null,
        transformStr: TransformString? = null,
        referenceStr: CrossReferenceString? = null,
        styleStr: StyleString? = null,
        formatterModelStr: FormatString? = null,
        configurationBase: LanguageProcessorConfiguration<Asm, ContextWithScope<Any, Any>> = configurationSimple(),
        grammarAglOptions: ProcessOptions<GrammarModel, ContextWithScope<Any, Any>>? = options { semanticAnalysis { context(ContextFromGrammarRegistry(registry)) } }
    ): LanguageDefinition<Asm, ContextWithScope<Any, Any>> {
        val config = Agl.configuration(configurationBase) {
            grammarString(grammarDefinitionStr)
            typesString(typeStr)
            transformString(transformStr)
            crossReferenceString(referenceStr)
            styleString(styleStr)
            formatString(formatterModelStr)
        }
        return LanguageDefinitionDefault(
            identity = identity,
            aglOptions = grammarAglOptions,
            buildForDefaultGoal = false,
            initialConfiguration = config
        )
    }
}