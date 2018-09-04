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

import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.UnableToAnalyseExeception
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.parser.Parser
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.ogl.runtime.converter.Converter
import net.akehurst.language.parser.scannerless.ScannerlessParser

internal class LanguageProcessorDefault(val grammar: Grammar, val semanticAnalyser: SemanticAnalyser?) : LanguageProcessor {

    private val converter: Converter= Converter(this.grammar)
    private val parser: Parser = ScannerlessParser(this.converter.builder.ruleSet())
    private val completionProvider: CompletionProvider = CompletionProvider()

    override fun parse(goalRuleName: String, inputText: CharSequence): SharedPackedParseTree {
        val sppt: SharedPackedParseTree = this.parser.parse(goalRuleName, inputText)
        return sppt
    }

    override fun <T> process(goalRuleName: String, inputText: CharSequence): T {
        val sppt: SharedPackedParseTree = this.parse(goalRuleName, inputText)
        if (null == this.semanticAnalyser) {
            throw UnableToAnalyseExeception("No SemanticAnalyser supplied", null);
        }
        val t: T = this.semanticAnalyser.analyse(sppt);

        return t;
    }

    override fun expectedAt(goalRuleName: String, inputText: CharSequence, position: Int, desiredDepth: Int): List<CompletionItem> {
        val parserExpected: List<RuleItem> = this.parser.expectedAt(goalRuleName, inputText, position);
        val expected = parserExpected.flatMap { this.completionProvider.provideFor(it, desiredDepth) }
        return expected
    }

}
