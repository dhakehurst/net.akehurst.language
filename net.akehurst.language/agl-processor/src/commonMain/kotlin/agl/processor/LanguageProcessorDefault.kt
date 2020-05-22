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

import net.akehurst.language.agl.analyser.SyntaxAnalyserSimple
import net.akehurst.language.agl.grammar.runtime.ConverterToRuntimeRules
import net.akehurst.language.agl.parser.Parser
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.analyser.SyntaxAnalyser

internal class LanguageProcessorDefault(
        override val grammar: Grammar,
        val goalRuleName: String,
        val syntaxAnalyser: SyntaxAnalyser?,
        val formatter: Formatter?
) : LanguageProcessor {

    private val converterToRuntimeRules: ConverterToRuntimeRules = ConverterToRuntimeRules(this.grammar)
    private val parser: Parser = ScanOnDemandParser(this.converterToRuntimeRules.transform())
    private val completionProvider: CompletionProvider = CompletionProvider(this.grammar)

    override fun interrupt(message: String) {
        this.parser.interrupt(message)
        //TODO: interrupt processor
    }
    override fun buildFor(goalRuleName: String): LanguageProcessor {
        this.parser.buildFor(goalRuleName)
        return this
    }

    override fun scan(inputText: CharSequence): List<SPPTLeaf> {
        return this.parser.scan(inputText);
    }

    override fun parse(inputText: CharSequence): SharedPackedParseTree = parse(this.goalRuleName, inputText)

    override fun parse(goalRuleName: String, inputText: CharSequence): SharedPackedParseTree {
        val sppt: SharedPackedParseTree = this.parser.parse(goalRuleName, inputText)
        return sppt
    }

    override fun <T> process(inputText: CharSequence): T = this.process(this.goalRuleName, inputText)
    override fun <T> process(goalRuleName: String, inputText: CharSequence): T {
        val sppt: SharedPackedParseTree = this.parse(goalRuleName, inputText)
        return this.process(sppt)
    }

    override fun <T> process(sppt: SharedPackedParseTree): T {
        val sa = if (null == this.syntaxAnalyser) {
            // No SyntaxAnalyser supplied, using SyntaxAnalyserSimple TODO: warning!
            SyntaxAnalyserSimple()
        } else {
            this.syntaxAnalyser
        }
        val t: T = sa.transform(sppt);

        return t;
    }

    override fun <T> formatText(inputText: CharSequence): String = formatTextForGoal<T>(this.goalRuleName, inputText)
    override fun <T> formatTextForGoal(goalRuleName: String, inputText: CharSequence): String {
        val asm = this.process<T>(goalRuleName, inputText)
        return this.formatAsm(asm)
    }

    override fun <T> formatAsm(asm: T): String {
        return if (null != formatter) {
            this.formatter.format(asm)
        } else {
            asm.toString()
        }
    }

    override fun expectedAt(inputText: CharSequence, position: Int, desiredDepth: Int): List<CompletionItem> = expectedAt(this.goalRuleName, inputText, position, desiredDepth)
    override fun expectedAt(goalRuleName: String, inputText: CharSequence, position: Int, desiredDepth: Int): List<CompletionItem> {
        val parserExpected: Set<RuntimeRule> = this.parser.expectedAt(goalRuleName, inputText, position);
        val grammarExpected: List<RuleItem> = parserExpected.filter { it!== RuntimeRuleSet.END_OF_TEXT }.map { this.converterToRuntimeRules.originalRuleItemFor(it) }
        val expected = grammarExpected.flatMap { this.completionProvider.provideFor(it, desiredDepth) }
        return expected.toSet().toList()
    }

}
