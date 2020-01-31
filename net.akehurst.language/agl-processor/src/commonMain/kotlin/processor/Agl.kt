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

package net.akehurst.language.processor

import net.akehurst.language.agl.grammar.GrammarRegistryDefault
import net.akehurst.language.agl.grammar.format.AglFormatGrammar
import net.akehurst.language.agl.grammar.format.AglFormatSyntaxAnalyser
import net.akehurst.language.agl.grammar.grammar.AglGrammarGrammar
import net.akehurst.language.agl.grammar.style.AglStyleGrammar
import net.akehurst.language.agl.grammar.style.AglStyleSyntaxAnalyser
import net.akehurst.language.agl.sppt2ast.AglGrammarSyntaxAnalyser
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.sppt2ast.SyntaxAnalyser
import kotlin.js.JsName

object Agl {

    val grammarProcessor: LanguageProcessor by lazy {
        val grammar = AglGrammarGrammar()
        val sppt2ast: SyntaxAnalyser = AglGrammarSyntaxAnalyser(GrammarRegistryDefault) //TODO: enable the registry to be changed
        processor(grammar, AglGrammarGrammar.goalRuleName, sppt2ast)
    }

    val styleProcessor: LanguageProcessor by lazy {
        val grammar = AglStyleGrammar()
        val sppt2ast: SyntaxAnalyser = AglStyleSyntaxAnalyser()
        processor(grammar, AglStyleGrammar.goalRuleName, sppt2ast)
    }

    val formatProcessor: LanguageProcessor by lazy {
        val grammar = AglFormatGrammar()
        val sppt2ast: SyntaxAnalyser = AglFormatSyntaxAnalyser()
        processor(grammar, AglFormatGrammar.goalRuleName, sppt2ast)
    }

    val version: String = BuildInfo.version
    val buildStamp: String = BuildInfo.buildStamp

    @JsName("processorFromGrammar")
    fun processor(grammar: Grammar, syntaxAnalyser: SyntaxAnalyser?=null, formatter: Formatter?=null): LanguageProcessor {
        val goalRuleName = grammar.rule.first { it.isSkip.not() }.name
        return LanguageProcessorDefault(grammar, goalRuleName, syntaxAnalyser, formatter)
    }

    @JsName("processorFromGrammarForGoal")
    fun processor(grammar: Grammar, goalRuleName:String, syntaxAnalyser: SyntaxAnalyser?=null, formatter: Formatter?=null): LanguageProcessor {
        return LanguageProcessorDefault(grammar, goalRuleName, syntaxAnalyser, formatter)
    }

    @JsName("processorFromString")
    fun processor(grammarDefinitionStr: String, syntaxAnalyser: SyntaxAnalyser?=null, formatter: Formatter?=null): LanguageProcessor {
        try {
            val grammar = grammarProcessor.process<Grammar>("grammarDefinition", grammarDefinitionStr)
            return processor(grammar, syntaxAnalyser, formatter)
        } catch (e: ParseFailedException) {
            //TODO: better, different exception to detect which list item fails
            throw ParseFailedException("Unable to parse grammarDefinitionStr ", e.longestMatch, e.location)
        }
    }
    @JsName("processorFromStringForGoal")
    fun processor(grammarDefinitionStr: String, goalRuleName:String, syntaxAnalyser: SyntaxAnalyser?=null, formatter: Formatter?=null): LanguageProcessor {
        try {
            val grammar = grammarProcessor.process<Grammar>("grammarDefinition", grammarDefinitionStr)
            return processor(grammar, goalRuleName, syntaxAnalyser, formatter)
        } catch (e: ParseFailedException) {
            //TODO: better, different exception to detect which list item fails
            throw ParseFailedException("Unable to parse grammarDefinitionStr ", e.longestMatch, e.location)
        }
    }

    @JsName("processorFromRuleList")
    fun processor(rules: List<String>, syntaxAnalyser: SyntaxAnalyser?=null, formatter: Formatter?=null): LanguageProcessor {
        val prefix = "namespace temp grammar Temp { "
        val grammarStr = prefix + rules.joinToString(" ") + "}"
        try {
            val grammar = grammarProcessor.process<Grammar>("grammarDefinition", grammarStr)
            return processor(grammar, syntaxAnalyser, formatter)
        } catch (e: ParseFailedException) {
            //TODO: better, different exception to detect which list item fails
            val newCol = e.location.column.minus(prefix.length)
            val location = InputLocation(newCol, 1, 1,0)
            throw ParseFailedException("Unable to parse list of rules", e.longestMatch, location)
        }
    }

}