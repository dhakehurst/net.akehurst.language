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

import net.akehurst.language.agl.grammar.grammar.AglGrammarGrammar
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.processor.*

object Agl {

    val version: String = BuildConfig.version
    val buildStamp: String = BuildConfig.buildStamp

    val registry = LanguageRegistry()

    fun register(definition: LanguageDefinition) {
        registry.registerFromDefinition(definition)
    }

    fun processorFromGrammar(
        grammar: Grammar,
        goalRuleName: String? = null,
        syntaxAnalyser: SyntaxAnalyser<*, *>? = null,
        semanticAnalyser: SemanticAnalyser<*, *>? = null,
        formatter: Formatter? = null,
    ): LanguageProcessor {
        val goal = goalRuleName ?: grammar.rule.first { it.isSkip.not() }.name
        return LanguageProcessorDefault(grammar, goal, syntaxAnalyser, formatter, semanticAnalyser)
    }

    /**
     * Create a LanguageProcessor from a grammar definition string
     *
     * grammarDefinitionStr may contain multiple grammars
     *
     * when {
     *   goalRuleName.contains(".") use before '.' to choose the grammar
     *   else use the last grammar in the grammarDefinitionStr
     * }
     * @param grammarDefinitionStr a string defining the grammar
     * @param targetGrammarName name of one of the grammars in the grammarDefinitionString to generate parser for (if null use last grammar found)
     * @param goalRuleName name of the default goal rule to use, it must be one of the rules in the target grammar or its super grammars (if null use first non-skip rule found in target grammar)
     * @param syntaxAnalyser a syntax analyser (if null use SyntaxAnalyserSimple)
     * @param semanticAnalyser a semantic analyser (if null use SemanticAnalyserSimple)
     * @param formatter a formatter
     */
    fun processorFromString(
        grammarDefinitionStr: String,
        targetGrammarName:String? = null,
        goalRuleName: String? = null,
        syntaxAnalyser: SyntaxAnalyser<*, *>? = null,
        semanticAnalyser: SemanticAnalyser<*, *>? = null,
        formatter: Formatter? = null,
    ): LanguageProcessor {
        try {
            val aglProc = this.registry.agl.grammar.processor ?: error("Internal error: AGL language processor not found")
            val (grammars, issues) = aglProc.process<List<Grammar>, Any>(grammarDefinitionStr, aglOptions {
                parser {
                    goalRule(AglGrammarGrammar.goalRuleName)
                }
                semanticAnalyser {
                    active(false) // switch off for performance
                }
            })
            if (null != grammars) {
                val goal = goalRuleName ?: grammars.last().rule.first { it.isSkip.not() }.name
                //TODO: what to do with issues if there are any?
                return when {
                    goal.contains(".") -> {
                        val grammarName = goal.substringBefore(".")
                        val grammar = grammars.find { it.name == grammarName } ?: throw LanguageProcessorException("Grammar with name $grammarName not found", null)
                        val goalName = goal.substringAfter(".")
                        processorFromGrammar(grammar, goalName, syntaxAnalyser, semanticAnalyser, formatter)
                    }
                    else -> processorFromGrammar(grammars.last(), goal, syntaxAnalyser, semanticAnalyser, formatter)
                }
            } else {
                if (issues.isEmpty()) {
                    throw LanguageProcessorException("Unable to parse grammarDefinitionStr - unknown reason", null)
                } else {
                    val issuesStr = issues.joinToString(separator = "\n") {
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