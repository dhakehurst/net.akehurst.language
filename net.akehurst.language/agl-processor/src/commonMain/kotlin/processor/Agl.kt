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

import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.agl.grammar.AglGrammar
import net.akehurst.language.agl.sppt2ast.AglSppt2AstTransformer
import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.api.sppt2ast.SyntaxAnalyser
import kotlin.js.JsName

object Agl {

    private val aglProcessor: LanguageProcessor by lazy {
        val grammar = AglGrammar()
        val sppt2ast: SyntaxAnalyser = AglSppt2AstTransformer()
        processor(grammar, sppt2ast)
    }

    val version: String = BuildInfo.version
    val buildStamp: String = BuildInfo.buildStamp

    @JsName("processorFromGrammar")
    fun processor(grammar: Grammar, syntaxAnalyser: SyntaxAnalyser?=null, formatter: Formatter?=null): LanguageProcessor {
        return LanguageProcessorDefault(grammar, syntaxAnalyser, formatter)
    }

    @JsName("processorFromString")
    fun processor(grammarDefinitionStr: String, syntaxAnalyser: SyntaxAnalyser?=null, formatter: Formatter?=null): LanguageProcessor {
        try {
            val grammar = aglProcessor.process<Grammar>("grammarDefinition", grammarDefinitionStr)
            return processor(grammar, syntaxAnalyser, formatter)
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
            val grammar = aglProcessor.process<Grammar>("grammarDefinition", grammarStr)
            return LanguageProcessorDefault(grammar, syntaxAnalyser, formatter)
        } catch (e: ParseFailedException) {
            //TODO: better, different exception to detect which list item fails
            val newCol = e.location.column.minus(prefix.length)
            val location = InputLocation(newCol, 1, 0)
            throw ParseFailedException("Unable to parse list of rules", e.longestMatch, location)
        }
    }

}