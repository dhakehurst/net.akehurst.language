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
import net.akehurst.language.api.sppt2ast.SyntaxAnalyser
import kotlin.js.JsName

object Agl {

    private val aglProcessor: LanguageProcessor by lazy {
        val grammar = AglGrammar()
        val sppt2ast: SyntaxAnalyser = AglSppt2AstTransformer()
        processor(grammar, sppt2ast)
    }

    @JsName("processorFromGrammar")
    fun processor(grammar: Grammar): LanguageProcessor {
        return LanguageProcessorDefault(grammar, null)
    }

    @JsName("processorFromGrammarWithSyntaxAnalyser")
    fun processor(grammar: Grammar, syntaxAnalyser: SyntaxAnalyser): LanguageProcessor {
        return LanguageProcessorDefault(grammar, syntaxAnalyser)
    }

    @JsName("processor")
    fun processor(grammarDefinitionStr: String): LanguageProcessor {
        try {
            val grammar = aglProcessor.process<Grammar>("grammarDefinition", grammarDefinitionStr)
            return processor(grammar)
        } catch (e: ParseFailedException) {
            //TODO: better, different exception to detect which list item fails
            throw ParseFailedException("Unable to parse grammarDefinitionStr ", e.longestMatch, e.location)
        }
    }

    @JsName("processorWithSyntaxAnalyser")
    fun processor(grammarDefinitionStr: String, syntaxAnalyser: SyntaxAnalyser): LanguageProcessor {
        try {
            val grammar = aglProcessor.process<Grammar>("grammarDefinition", grammarDefinitionStr)
            return processor(grammar, syntaxAnalyser)
        } catch (e: ParseFailedException) {
            //TODO: better, different exception to detect which list item fails
            throw ParseFailedException("Unable to parse grammarDefinitionStr ", e.longestMatch, e.location)
        }
    }

    @JsName("processorFromRuleList")
    fun processor(rules: List<String>): LanguageProcessor {
        val prefix = "namespace temp grammar Temp { "
        val grammarStr = prefix + rules.joinToString(" ") + "}"
        try {
            val grammar = aglProcessor.process<Grammar>("grammarDefinition", grammarStr)
            return LanguageProcessorDefault(grammar, null)
        } catch (e: ParseFailedException) {
            //TODO: better, different exception to detect which list item fails
            val newCol = e.location.column.minus(prefix.length)
            val location = InputLocation(newCol, 1, 0)
            throw ParseFailedException("Unable to parse list of rules", e.longestMatch, location)
        }
    }

}