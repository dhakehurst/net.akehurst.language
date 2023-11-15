/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.agl.processor

import net.akehurst.language.agl.api.generator.GeneratedLanguageProcessorAbstract
import net.akehurst.language.agl.default.CompletionProviderDefault
import net.akehurst.language.agl.default.SemanticAnalyserDefault
import net.akehurst.language.agl.default.SyntaxAnalyserDefault
import net.akehurst.language.agl.default.TypeModelFromGrammar
import net.akehurst.language.agl.language.format.AglFormatterModelDefault
import net.akehurst.language.agl.language.grammar.ContextFromGrammar
import net.akehurst.language.agl.language.grammar.ContextFromGrammarRegistry
import net.akehurst.language.agl.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.agl.language.style.asm.AglStyleModelDefault
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.agl.semanticAnalyser.ContextSimple
import net.akehurst.language.agl.syntaxAnalyser.*
import net.akehurst.language.api.asm.Asm
import net.akehurst.language.api.language.grammar.Grammar
import net.akehurst.language.api.processor.*
import net.akehurst.language.typemodel.api.TypeModel

object Agl {

    val version: String = BuildConfig.version
    val buildStamp: String = BuildConfig.buildStamp

    val registry: LanguageRegistry = LanguageRegistryDefault()

    fun <AsmType : Any, ContextType : Any> configurationEmpty(): LanguageProcessorConfiguration<AsmType, ContextType> =
        LanguageProcessorConfigurationDefault(
            targetGrammarName = null,
            defaultGoalRuleName = null,
            typeModelResolver = null,
            crossReferenceModelResolver = null,
            syntaxAnalyserResolver = null,
            semanticAnalyserResolver = null,
            formatterResolver = null,
            styleResolver = null,
            completionProvider = null
        )

    fun configurationDefault(): LanguageProcessorConfiguration<Asm, ContextSimple> = Agl.configuration {
        targetGrammarName(null) //use default
        defaultGoalRuleName(null) //use default
        typeModelResolver { p -> ProcessResultDefault<TypeModel>(TypeModelFromGrammar.create(p.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
        crossReferenceModelResolver { p -> CrossReferenceModelDefault.fromString(ContextFromTypeModel(p.typeModel), "") }
        syntaxAnalyserResolver { p ->
            ProcessResultDefault(
                SyntaxAnalyserDefault(p.grammar!!.qualifiedName, p.typeModel, p.crossReferenceModel),
                IssueHolder(LanguageProcessorPhase.ALL)
            )
        }
        semanticAnalyserResolver { p -> ProcessResultDefault(SemanticAnalyserDefault(p.typeModel, p.crossReferenceModel), IssueHolder(LanguageProcessorPhase.ALL)) }
        styleResolver { p -> AglStyleModelDefault.fromString(ContextFromGrammar.createContextFrom(listOf(p.grammar!!)), "") }
        formatterResolver { p -> AglFormatterModelDefault.fromString(ContextFromTypeModel(p.typeModel), "") }
        completionProvider { p -> ProcessResultDefault(CompletionProviderDefault(p.grammar!!, p.typeModel, p.crossReferenceModel), IssueHolder(LanguageProcessorPhase.ALL)) }
    }

    /**
     * build a set of options for a parser
     * (does not set the options, they must be passed as argument)
     */
    fun parseOptions(init: ParseOptionsBuilder.() -> Unit): ParseOptions {
        val b = ParseOptionsBuilder()
        b.init()
        return b.build()
    }

    /**
     * build a set of options for a language processor
     * (does not set the options, they must be passed as argument)
     */
    fun <AsmType : Any, ContextType : Any> options(init: ProcessOptionsBuilder<AsmType, ContextType>.() -> Unit): ProcessOptions<AsmType, ContextType> {
        val b = ProcessOptionsBuilder<AsmType, ContextType>()
        b.init()
        return b.build()
    }

    /**
     * build a configuration for a language processor
     * (does not set the configuration, they must be passed as argument)
     */
    fun <AsmType : Any, ContextType : Any> configuration(
        base: LanguageProcessorConfiguration<AsmType, ContextType>? = null,
        init: LanguageProcessorConfigurationBuilder<AsmType, ContextType>.() -> Unit
    ): LanguageProcessorConfiguration<AsmType, ContextType> {
        val b = LanguageProcessorConfigurationBuilder<AsmType, ContextType>(base)
        b.init()
        return b.build()
    }

    fun <AsmType : Any, ContextType : Any> processorFromGrammar(
        grammar: Grammar,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>? = null
    ): LanguageProcessor<AsmType, ContextType> {
        val config = configuration ?: configurationEmpty()
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
        grammarDefinitionStr: String,
        crossReferenceModelStr: String? = null,
        styleModelStr: String? = null,
        formatterModelStr: String? = null,
        grammarAglOptions: ProcessOptions<List<Grammar>, ContextFromGrammarRegistry>? = Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } }
    ): LanguageProcessorResult<Asm, ContextSimple> {
        val config = Agl.configuration(Agl.configurationDefault()) {
            if (null != crossReferenceModelStr) {
                crossReferenceModelResolver { p -> CrossReferenceModelDefault.fromString(ContextFromTypeModel(p.typeModel), crossReferenceModelStr) }
            }
            if (null != styleModelStr) {
                styleResolver { p -> AglStyleModelDefault.fromString(ContextFromGrammar.createContextFrom(listOf(p.grammar!!)), styleModelStr) }
            }
            if (null != formatterModelStr) {
                formatterResolver { p -> AglFormatterModelDefault.fromString(ContextFromTypeModel(p.typeModel), formatterModelStr) }
            }
        }
        val proc = processorFromString(grammarDefinitionStr, config, grammarAglOptions)
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
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>? = null,
        aglOptions: ProcessOptions<List<Grammar>, ContextFromGrammarRegistry>? = Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } }
    ): LanguageProcessorResult<AsmType, ContextType> {
        return try {
            val res = Agl.grammarFromString<List<Grammar>, ContextFromGrammarRegistry>(
                grammarDefinitionStr,
                aglOptions ?: Agl.registry.agl.grammar.processor!!.optionsDefault()
            )
            if (null == res.asm || 0 == res.asm!!.size) {
                LanguageProcessorResult(null, res.issues)
            } else {
                val grammar = if (null == configuration?.targetGrammarName) {
                    res.asm?.lastOrNull() ?: error("Unable to create processor for $grammarDefinitionStr")
                } else {
                    res.asm?.firstOrNull { it.name == configuration.targetGrammarName }
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
        aglOptions: ProcessOptions<List<Grammar>, ContextFromGrammarRegistry>? = Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } }
    ): ProcessResult<List<Grammar>> {
        return if (null == sentence) {
            ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL))
        } else {
            val res = Agl.registry.agl.grammar.processor!!.process(sentence, aglOptions)
            ProcessResultDefault(res.asm ?: emptyList(), res.issues)
        }
    }
}