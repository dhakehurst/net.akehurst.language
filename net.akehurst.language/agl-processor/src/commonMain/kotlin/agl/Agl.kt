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

import net.akehurst.language.agl.api.generator.GeneratedLanguageProcessorAbstract
import net.akehurst.language.agl.language.asmTransform.TransformModelDefault
import net.akehurst.language.agl.language.format.AglFormatterModelFromAsm
import net.akehurst.language.agl.language.grammar.ContextFromGrammar
import net.akehurst.language.agl.language.grammar.ContextFromGrammarRegistry
import net.akehurst.language.agl.language.grammar.asm.GrammarModelDefault
import net.akehurst.language.agl.language.grammar.asm.asDefinitionBlock
import net.akehurst.language.agl.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.agl.language.style.asm.AglStyleModelDefault
import net.akehurst.language.agl.processor.*
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.agl.default.ContextAsmDefault
import net.akehurst.language.agl.syntaxAnalyser.*
import net.akehurst.language.api.asm.Asm
import net.akehurst.language.api.language.base.SimpleName
import net.akehurst.language.api.language.grammar.Grammar
import net.akehurst.language.api.language.grammar.GrammarModel
import net.akehurst.language.api.processor.*
import net.akehurst.language.parser.api.ParseOptions
import net.akehurst.language.typemodel.simple.TypeModelSimple
import kotlin.jvm.JvmInline

@JvmInline
value class GrammarString(val value: String)

@JvmInline
value class TypeModelString(val value: String)

@JvmInline
value class TransformString(val value: String)

@JvmInline
value class CrossReferenceString(val value: String)

@JvmInline
value class StyleString(val value: String)

@JvmInline
value class FormatString(val value: String)

object Agl {

    val version: String = BuildConfig.version
    val buildStamp: String = BuildConfig.buildStamp

    val registry: LanguageRegistry = LanguageRegistryDefault()

    fun <AsmType : Any, ContextType : Any> configurationEmpty(): LanguageProcessorConfiguration<AsmType, ContextType> = LanguageProcessorConfigurationEmpty()

    fun <AsmType : Any, ContextType : Any> configurationBase(): LanguageProcessorConfiguration<AsmType, ContextType> = LanguageProcessorConfigurationBase()

    fun configurationDefault(): LanguageProcessorConfiguration<Asm, ContextAsmDefault> = LanguageProcessorConfigurationDefault()

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
        grammar: Grammar,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>? = null
    ): LanguageProcessor<AsmType, ContextType> {
        val config = configuration ?: configurationBase()
        return LanguageProcessorDefault<AsmType, ContextType>(grammar, config)
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
    fun processorFromStringDefault(
        grammarDefinitionStr: GrammarString,
        typeModelStr: TypeModelString? = null,
        transformStr: TransformString? = null,
        crossReferenceModelStr: CrossReferenceString? = null,
        styleModelStr: StyleString? = null,
        formatterModelStr: FormatString? = null,
        base: LanguageProcessorConfiguration<Asm, ContextAsmDefault> = configurationDefault(),
        grammarAglOptions: ProcessOptions<GrammarModel, ContextFromGrammarRegistry>? = options { semanticAnalysis { context(ContextFromGrammarRegistry(registry)) } }
    ): LanguageProcessorResult<Asm, ContextAsmDefault> {
        val config = Agl.configuration(base) {
            if (null != typeModelStr) {
                typeModelResolver { p -> TypeModelSimple.fromString(typeModelStr.value) }
            }
            if (null != transformStr) {
                asmTransformResolver { p -> TransformModelDefault.fromString(ContextFromGrammar.createContextFrom(p.grammar!!.asDefinitionBlock()), transformStr.value) }
            }
            if (null != crossReferenceModelStr) {
                crossReferenceModelResolver { p -> CrossReferenceModelDefault.fromString(ContextFromTypeModel(p.typeModel), crossReferenceModelStr.value) }
            }
            if (null != styleModelStr) {
                styleResolver { p -> AglStyleModelDefault.fromString(ContextFromGrammar.createContextFrom(p.grammar!!.asDefinitionBlock()), styleModelStr.value) }
            }
            if (null != formatterModelStr) {
                formatterResolver { p -> AglFormatterModelFromAsm.fromString(ContextFromTypeModel(p.typeModel), formatterModelStr.value) }
            }
        }
        val proc = processorFromString(grammarDefinitionStr.value, config, grammarAglOptions)
        return proc
    }

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
        aglOptions: ProcessOptions<GrammarModel, ContextFromGrammarRegistry>? = Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } }
    ): LanguageProcessorResult<AsmType, ContextType> {
        return try {
            val res = Agl.grammarFromString<GrammarModel, ContextFromGrammarRegistry>(
                grammarDefinitionStr,
                aglOptions ?: Agl.registry.agl.grammar.processor!!.optionsDefault()
            )
            if (null == res.asm || res.asm!!.isEmpty) {
                LanguageProcessorResult(null, res.issues)
            } else {
                val grammar = if (null == configuration?.targetGrammarName) {
                    res.asm?.primary ?: error("Unable to create processor for $grammarDefinitionStr")
                } else {
                    res.asm?.allDefinitions?.firstOrNull { it.name == configuration.targetGrammarName }
                        ?: error("Unable to find target grammar '${configuration.targetGrammarName}' in $grammarDefinitionStr")
                }
                val proc = processorFromGrammar(
                    grammar,
                    configuration
                )
                LanguageProcessorResult(proc, res.issues)
            }
        } catch (e: Throwable) {
            throw LanguageProcessorException("Unable to create processor for grammarDefinitionStr: ${e.message}", e)
        }
    }

    fun <AsmType : Any, ContextType : Any> processorFromGeneratedCode(
        generated: GeneratedLanguageProcessorAbstract<AsmType, ContextType>
    ): LanguageProcessor<AsmType, ContextType> {
        return LanguageProcessorFromGenerated(generated)
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
        aglOptions: ProcessOptions<GrammarModel, ContextFromGrammarRegistry>? = Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } }
    ): ProcessResult<GrammarModel> {
        return if (null == sentence) {
            ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL))
        } else {
            val res = Agl.registry.agl.grammar.processor!!.process(sentence, aglOptions)
            ProcessResultDefault(res.asm ?: GrammarModelDefault(SimpleName("fromString-error"), emptyList()), res.issues)
        }
    }
}