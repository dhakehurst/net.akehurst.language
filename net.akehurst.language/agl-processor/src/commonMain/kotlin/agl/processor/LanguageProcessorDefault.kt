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

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserSimple
import net.akehurst.language.agl.grammar.grammar.ConverterToRuntimeRules
import net.akehurst.language.agl.parser.Parser
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.AutomatonKind
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyserException
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyserItem
import kotlin.reflect.KClass

internal class LanguageProcessorDefault(
        override val grammar: Grammar,
        val goalRuleName: String,
        val syntaxAnalyser: SyntaxAnalyser?,
        val formatter: Formatter?,
        val semanticAnalyser: SemanticAnalyser?
) : LanguageProcessor {

    private val converterToRuntimeRules: ConverterToRuntimeRules = ConverterToRuntimeRules(this.grammar)
    //internal so that tests can use it
    internal val parser: Parser = ScanOnDemandParser(this.converterToRuntimeRules.transform())
    private val completionProvider: CompletionProvider = CompletionProvider(this.grammar)

    override fun interrupt(message: String) {
        this.parser.interrupt(message)
        //TODO: interrupt processor
    }

    override fun buildFor(goalRuleName: String, automatonKind:AutomatonKind): LanguageProcessor {
        this.parser.buildFor(goalRuleName,automatonKind)
        return this
    }

    override fun scan(inputText: String): List<SPPTLeaf> {
        return this.parser.scan(inputText);
    }

    override fun parse(inputText: String): SharedPackedParseTree = parseForGoal(this.goalRuleName, inputText, AutomatonKind.LOOKAHEAD_1)

    override fun parseWithAutomatonKind(inputText: String, automatonKind: AutomatonKind): SharedPackedParseTree = parseForGoal(this.goalRuleName, inputText, automatonKind)

    override fun parseForGoal(goalRuleName: String, inputText: String, automatonKind:AutomatonKind): SharedPackedParseTree {
        val sppt: SharedPackedParseTree = this.parser.parse(goalRuleName, inputText,automatonKind)
        return sppt
    }


    private fun <T : Any> _process(sppt: SharedPackedParseTree, asmType: KClass<in T> = Any::class): Pair<T, SyntaxAnalyser> {
        val sa = this.syntaxAnalyser ?: SyntaxAnalyserSimple()
        val asm: T = sa.transform(sppt)
        return Pair(asm, sa)
    }

    override fun <T : Any> processFromSPPT(asmType: KClass<in T>, sppt: SharedPackedParseTree): T = this._process(sppt, asmType).first
    override fun <T : Any> process(asmType: KClass<in T>, inputText: String, automatonKind:AutomatonKind): T = this.processForGoal(asmType, this.goalRuleName, inputText,automatonKind)
    override fun <T : Any> processForGoal(asmType: KClass<in T>, goalRuleName: String, inputText: String, automatonKind:AutomatonKind): T {
        val sppt: SharedPackedParseTree = this.parseForGoal(goalRuleName, inputText,automatonKind)
        return this.processFromSPPT(asmType, sppt)
    }

    override fun <T : Any> formatText(asmType: KClass<in T>, inputText: String, automatonKind:AutomatonKind): String = formatTextForGoal<T>(asmType, this.goalRuleName, inputText,automatonKind)
    override fun <T : Any> formatTextForGoal(asmType: KClass<in T>, goalRuleName: String, inputText: String, automatonKind:AutomatonKind): String {
        val asm = this.processForGoal<T>(asmType, goalRuleName, inputText, automatonKind)
        return this.formatAsm(asmType, asm)
    }

    override fun <T : Any> formatAsm(asmType: KClass<in T>, asm: T): String {
        return if (null != formatter) {
            this.formatter.format(asm)
        } else {
            asm.toString()
        }
    }

    override fun expectedAt(inputText: String, position: Int, desiredDepth: Int, automatonKind:AutomatonKind): List<CompletionItem> = expectedAtForGoal(this.goalRuleName, inputText, position, desiredDepth,automatonKind)
    override fun expectedAtForGoal(goalRuleName: String, inputText: String, position: Int, desiredDepth: Int, automatonKind:AutomatonKind): List<CompletionItem> {
        val parserExpected: Set<RuntimeRule> = this.parser.expectedAt(goalRuleName, inputText, position,automatonKind)
        val grammarExpected: List<RuleItem> = parserExpected.filter { it !== RuntimeRuleSet.END_OF_TEXT }.map { this.converterToRuntimeRules.originalRuleItemFor(it) }
        val expected = grammarExpected.flatMap { this.completionProvider.provideFor(it, desiredDepth) }
        return expected.toSet().toList()
    }

    override fun <T : Any> analyseText(asmType: KClass<in T>, inputText: String): List<SemanticAnalyserItem> {
        return this.analyseTextForGoal(asmType, this.goalRuleName, inputText)
    }

    override fun <T : Any> analyseTextForGoal(asmType: KClass<in T>, goalRuleName: String, inputText: String): List<SemanticAnalyserItem> {
        val sppt: SharedPackedParseTree = this.parseForGoal(goalRuleName, inputText)
        val p = this._process(sppt, asmType)
        val asm = p.first
        val sa = p.second
        return this.analyseAsm(asmType, asm, sa.locationMap)
    }

    override fun <T : Any> analyseAsm(asmType: KClass<in T>, asm: T, locationMap: Map<Any, InputLocation>): List<SemanticAnalyserItem> {
        val semAnalyser = this.semanticAnalyser ?: throw SemanticAnalyserException("No semantic analyser was supplied to the language processor", null)
        semAnalyser.clear()
        return semAnalyser.analyse(asm, locationMap)
    }

}
