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

import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser

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
     */
    fun processorFromString(
        grammarDefinitionStr: String,
        goalRuleName: String? = null,
        syntaxAnalyser: SyntaxAnalyser<*, *>? = null,
        semanticAnalyser: SemanticAnalyser<*, *>? = null,
        formatter: Formatter? = null,
    ): LanguageProcessor {
        try {
            val aglProc = this.registry.agl.grammar.processor ?: error("Internal error: AGL language processor not found")
            val (grammars, items) = aglProc.process<List<Grammar>, Any>(grammarDefinitionStr, "grammarDefinition", AutomatonKind.LOOKAHEAD_1, null)
            val goal = goalRuleName ?: grammars.last().rule.first { it.isSkip.not() }.name
            return when {
                goal.contains(".") -> {
                    val grammarName = goal.substringBefore(".")
                    val grammar = grammars.find { it.name == grammarName } ?: throw LanguageProcessorException("Grammar with name $grammarName not found", null)
                    val goalName = goal.substringAfter(".")
                    processorFromGrammar(grammar, goalName, syntaxAnalyser, semanticAnalyser, formatter)
                }
                else -> processorFromGrammar(grammars.last(), goal, syntaxAnalyser, semanticAnalyser, formatter)
            }
        } catch (e: ParseFailedException) {
            throw LanguageProcessorException("Unable to parse grammarDefinitionStr at line: ${e.location.line} column: ${e.location.column} expected one of: ${e.expected}", e)
        }
    }

}