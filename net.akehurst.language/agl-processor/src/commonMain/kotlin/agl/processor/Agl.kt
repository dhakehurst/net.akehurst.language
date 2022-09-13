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

import net.akehurst.language.agl.grammar.grammar.GrammarContext
import net.akehurst.language.agl.semanticAnalyser.SemanticAnalyserSimple
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserSimple
import net.akehurst.language.agl.syntaxAnalyser.TypeModelFromGrammar
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.processor.*

object Agl {

    val version: String = BuildConfig.version
    val buildStamp: String = BuildConfig.buildStamp

    val registry = LanguageRegistry()

    fun <AsmType : Any, ContextType : Any> register(definition: LanguageDefinition<AsmType, ContextType>) {
        registry.registerFromDefinition(definition)
    }

    fun <AsmType : Any, ContextType : Any> configurationDefault(): LanguageProcessorConfiguration<AsmType, ContextType> =
        LanguageProcessorConfigurationDefault()

    /**
     * build a configuration for a language processor
     * (does not set the configuration, they must be passed as argument)
     */
    fun <AsmType : Any, ContextType : Any> configuration(init: LanguageProcessorConfigurationBuilder<AsmType, ContextType>.() -> Unit): LanguageProcessorConfiguration<AsmType, ContextType> {
        val b = LanguageProcessorConfigurationBuilder<AsmType, ContextType>()
        b.init()
        return b.build()
    }

    fun <AsmType : Any, ContextType : Any> processorFromGrammar(
        grammar: Grammar,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>? = null
    ): LanguageProcessor<AsmType, ContextType> {
        val config = configuration ?: configurationDefault()
        val goal = config.defaultGoalRuleName ?: grammar.rule.first { it.isSkip.not() }.name
        return LanguageProcessorDefault<AsmType, ContextType>(
            grammar,
            goal,
            config.syntaxAnalyserResolver?.invoke(grammar),
            config.formatter,
            config.semanticAnalyserResolver?.invoke(grammar)
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
    fun processorFromStringDefault(
        grammarDefinitionStr: String,
        aglOptions: ProcessOptions<List<Grammar>, GrammarContext>? = null
    ): LanguageProcessor<AsmSimple, ContextSimple> {
        try {
            val aglProc = Agl.registry.agl.grammar.processor ?: error("Internal error: AGL language processor not found")
            val aglOpts = aglOptions ?: Agl.registry.agl.grammar.processor?.optionsDefault()
            val result = aglProc.process(grammarDefinitionStr, aglOpts)
            val grammars = result.asm
            return if (null != grammars) {
                val grammar = grammars.last()
                processorFromGrammar(
                    grammar,
                    Agl.configuration {
                        targetGrammarName(grammar.name)
                        defaultGoalRuleName(grammar.rule.first { it.isSkip.not() }.name)
                        syntaxAnalyserResolver{ g -> SyntaxAnalyserSimple(TypeModelFromGrammar(g))}
                        semanticAnalyserResolver{ _ -> SemanticAnalyserSimple() }
                        formatter(null) //TODO
                    }
                )
            } else {
                if (result.issues.isEmpty()) {
                    throw LanguageProcessorException("Unable to parse grammarDefinitionStr - unknown reason", null)
                } else {
                    val issuesStr = result.issues.joinToString(separator = "\n") {
                        "at line: ${it.location?.line} column: ${it.location?.column} expected one of: ${it.data}"
                    }
                    throw LanguageProcessorException("Unable to parse grammarDefinitionStr:\n $issuesStr", null)
                }
            }
        } catch (e: LanguageProcessorException) {
            throw e
        } catch (e: Throwable) {
            throw LanguageProcessorException("Unable to create processor for grammarDefinitionStr: ${e.message}", e)
        }
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
    ): LanguageProcessor<AsmType, ContextType> {
        val config = configuration ?: configurationDefault()
        val aglOpts = aglOptions ?: Agl.registry.agl.grammar.processor?.optionsDefault()
        try {
            val aglProc = Agl.registry.agl.grammar.processor ?: error("Internal error: AGL language processor not found")
            val result = aglProc.process(grammarDefinitionStr, aglOpts)
            val grammars = result.asm
            if (null != grammars) {
                val tgtGrammar = config.targetGrammarName?.let { tg-> grammars.find { it.name == tg } } ?: grammars.last()
                val goal = config.defaultGoalRuleName ?: tgtGrammar.rule.first { it.isSkip.not() }.name
                //TODO: what to do with issues if there are any?
                return when {
                    goal.contains(".") -> {
                        val grammarName = goal.substringBefore(".")
                        val grammar = grammars.find { it.name == grammarName } ?: throw LanguageProcessorException("Grammar with name $grammarName not found", null)
                        //val goalName = goal.substringAfter(".")
                        processorFromGrammar(grammar, config)
                    }
                    else -> {
                        processorFromGrammar(tgtGrammar, config)
                    }
                }
            } else {
                if (result.issues.isEmpty()) {
                    throw LanguageProcessorException("Unable to parse grammarDefinitionStr - unknown reason", null)
                } else {
                    val issuesStr = result.issues.joinToString(separator = "\n") {
                        "at line: ${it.location?.line} column: ${it.location?.column} expected one of: ${it.data}"
                    }
                    throw LanguageProcessorException("Unable to parse grammarDefinitionStr:\n $issuesStr", null)
                }
            }
        } catch (e: LanguageProcessorException) {
            throw e
        } catch (e: Throwable) {
            throw LanguageProcessorException("Unable to create processor for grammarDefinitionStr: ${e.message}", e)
        }
    }

}