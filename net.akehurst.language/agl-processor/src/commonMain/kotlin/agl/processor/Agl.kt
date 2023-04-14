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
import net.akehurst.language.agl.grammar.format.AglFormatterModelDefault
import net.akehurst.language.agl.grammar.grammar.ContextFromGrammar
import net.akehurst.language.agl.grammar.grammar.GrammarContext
import net.akehurst.language.agl.grammar.scopes.ScopeModelAgl
import net.akehurst.language.agl.grammar.style.AglStyleModelDefault
import net.akehurst.language.agl.semanticAnalyser.SemanticAnalyserSimple
import net.akehurst.language.agl.syntaxAnalyser.ContextFromTypeModel
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserSimple
import net.akehurst.language.agl.syntaxAnalyser.TypeModelFromGrammar
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.processor.*

object Agl {

    val version: String = BuildConfig.version
    val buildStamp: String = BuildConfig.buildStamp

    val registry = LanguageRegistryDefault()

    fun configurationDefault(): LanguageProcessorConfiguration<AsmSimple, ContextSimple> = Agl.configuration {
            targetGrammarName(null) //use default
            defaultGoalRuleName(null) //use default
            typeModelResolver { p -> ProcessResultDefault(TypeModelFromGrammar(p.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
            scopeModelResolver { p -> ScopeModelAgl.fromString(ContextFromTypeModel(p.typeModel!!), "") }
            syntaxAnalyserResolver { p -> ProcessResultDefault(SyntaxAnalyserSimple(p.typeModel, p.scopeModel), IssueHolder(LanguageProcessorPhase.ALL)) }
            semanticAnalyserResolver { p -> ProcessResultDefault(SemanticAnalyserSimple(p.scopeModel), IssueHolder(LanguageProcessorPhase.ALL)) }
            styleResolver { p -> AglStyleModelDefault.fromString(ContextFromGrammar(p.grammar!!), "") }
            formatterResolver { p -> AglFormatterModelDefault.fromString(ContextFromTypeModel(p.typeModel!!), "") }
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
    fun <AsmType : Any, ContextType : Any> configuration(base:LanguageProcessorConfiguration<AsmType, ContextType>?=null, init: LanguageProcessorConfigurationBuilder<AsmType, ContextType>.() -> Unit): LanguageProcessorConfiguration<AsmType, ContextType> {
        val b = LanguageProcessorConfigurationBuilder<AsmType, ContextType>(base)
        b.init()
        return b.build()
    }

    fun <AsmType : Any, ContextType : Any> processorFromGrammar(
        grammar: Grammar,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>? = null
    ): LanguageProcessor<AsmType, ContextType> {
        val config = configuration ?: LanguageProcessorConfigurationDefault()
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
        scopeModelStr: String? = null,
        styleModelStr: String? = null,
        formatterModelStr: String? = null,
        grammarAglOptions: ProcessOptions<List<Grammar>, GrammarContext>? = null
    ): LanguageProcessorResult<AsmSimple, ContextSimple> {
        val config = Agl.configuration {
            targetGrammarName(null) //use default
            defaultGoalRuleName(null) //use default
            typeModelResolver { p -> ProcessResultDefault(TypeModelFromGrammar(p.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
            scopeModelResolver { p -> ScopeModelAgl.fromString(ContextFromTypeModel(p.typeModel!!), scopeModelStr ?: "") }
            syntaxAnalyserResolver { p -> ProcessResultDefault(SyntaxAnalyserSimple(p.typeModel, p.scopeModel), IssueHolder(LanguageProcessorPhase.ALL)) }
            semanticAnalyserResolver { p -> ProcessResultDefault(SemanticAnalyserSimple(p.scopeModel), IssueHolder(LanguageProcessorPhase.ALL)) }
            styleResolver { p -> AglStyleModelDefault.fromString(ContextFromGrammar(p.grammar!!), styleModelStr ?: "") }
            formatterResolver { p -> AglFormatterModelDefault.fromString(ContextFromTypeModel(p.typeModel!!), formatterModelStr ?: "") }
        }
        return processorFromString(grammarDefinitionStr, config, grammarAglOptions)
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
        aglOptions: ProcessOptions<List<Grammar>, GrammarContext>? = null
    ): LanguageProcessorResult<AsmType, ContextType> {
        return try {
            val res = Agl.grammarFromString<List<Grammar>, GrammarContext>(
                grammarDefinitionStr,
                aglOptions ?: Agl.registry.agl.grammar.processor!!.optionsDefault()
            )
            if (null == res.asm) {
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

    fun <AsmType : Any, ContextType : Any> grammarFromString(sentence: String?, aglOptions: ProcessOptions<List<Grammar>, GrammarContext>? = null): ProcessResult<List<Grammar>> {
        return if (null == sentence) {
            ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL))
        } else {
            val res = Agl.registry.agl.grammar.processor!!.process(sentence, aglOptions)
            ProcessResultDefault(res.asm ?: emptyList(), res.issues)
        }
    }
}