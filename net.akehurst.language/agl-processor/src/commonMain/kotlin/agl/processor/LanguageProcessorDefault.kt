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

import net.akehurst.language.agl.agl.parser.Scanner
import net.akehurst.language.agl.grammar.grammar.ConverterToRuntimeRules
import net.akehurst.language.agl.parser.Parser
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.semanticAnalyser.SemanticAnalyserSimple
import net.akehurst.language.agl.sppt.SPPTParserDefault
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserSimple
import net.akehurst.language.agl.syntaxAnalyser.TypeModelFromGrammar
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.AutomatonKind
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTParser
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.typeModel.TypeModel

internal class LanguageProcessorDefault(
    override val grammar: Grammar,
    val defaultGoalRuleName: String,
    val syntaxAnalyser: SyntaxAnalyser<*, *>?,
    val formatter: Formatter?,
    val semanticAnalyser: SemanticAnalyser<*, *>?
) : LanguageProcessor {

    private val _converterToRuntimeRules: ConverterToRuntimeRules by lazy { ConverterToRuntimeRules(this.grammar) }
    private val _runtimeRuleSet by lazy { this._converterToRuntimeRules.runtimeRuleSet }
    private val _scanner by lazy { Scanner(this._runtimeRuleSet) }

    //internal so that tests can use it
    internal val parser: Parser by lazy { ScanOnDemandParser(this._runtimeRuleSet) }
    private val _completionProvider: CompletionProvider by lazy { CompletionProvider(this.grammar) }

    override val spptParser: SPPTParser by lazy {
        SPPTParserDefault((parser as ScanOnDemandParser).runtimeRuleSet)
    }

    override val typeModel: TypeModel by lazy {
        TypeModelFromGrammar(this.grammar).derive()
    }

    override fun interrupt(message: String) {
        this.parser.interrupt(message)
        //TODO: interrupt processor
    }

    override fun buildForDefaultGoal(): LanguageProcessor = this.buildFor(this.defaultGoalRuleName)

    override fun buildFor(goalRuleName: String, automatonKind: AutomatonKind): LanguageProcessor {
        this.parser.buildFor(goalRuleName, automatonKind)
        return this
    }

    override fun scan(sentence: String): List<SPPTLeaf> {
        return this._scanner.scan(sentence, false)
    }

    override fun parse(sentence: String, goalRuleName: String?, automatonKind: AutomatonKind?): Pair<SharedPackedParseTree?,List<LanguageIssue>> {
        val goal = goalRuleName ?: this.defaultGoalRuleName
        val ak = automatonKind ?: AutomatonKind.LOOKAHEAD_1
        return this.parser.parseForGoal(goal, sentence, ak)
    }

    override fun <AsmType : Any,ContextType : Any> syntaxAnalysis(sppt: SharedPackedParseTree,context: ContextType?): Triple<AsmType?,List<LanguageIssue>,Map<*,InputLocation>> {
        val sa:SyntaxAnalyser<AsmType,ContextType> = (this.syntaxAnalyser ?: SyntaxAnalyserSimple(this.typeModel)) as SyntaxAnalyser<AsmType, ContextType>
        sa.clear()
        val (asm: AsmType, issues) = sa.transform(sppt,{rsn, rn -> this._converterToRuntimeRules.originalRuleItemFor(rsn,rn)}, context)
        return Triple(asm,issues, sa.locationMap)
    }

    override fun <AsmType : Any, ContextType : Any> semanticAnalysis(
        asm: AsmType,
        locationMap: Map<*, InputLocation>?,
        context: ContextType?
    ): List<LanguageIssue> {
        val semAnalyser: SemanticAnalyser<AsmType, ContextType> = ((this.semanticAnalyser as SemanticAnalyser<AsmType, ContextType>?)
            ?: SemanticAnalyserSimple<AsmType, ContextType>())
        semAnalyser.clear()
        val lm = locationMap ?: emptyMap<Any,InputLocation>()
        return semAnalyser.analyse(asm, lm, context)
    }

    override fun <AsmType : Any, ContextType : Any> process(
        sentence: String,
        goalRuleName: String?,
        automatonKind: AutomatonKind?,
        context: ContextType?
    ): Pair<AsmType?, List<LanguageIssue>> {
        val (sppt, issues1) = this.parse(sentence, goalRuleName, automatonKind)
        return if (null==sppt) {
            Pair(null, issues1)
        } else {
            val (asm, issues2, locationMap) = this.syntaxAnalysis<AsmType, ContextType>(sppt, context)
            if(null==asm) {
                Pair(null,issues1 + issues2)
            } else {
                val issues3 = this.semanticAnalysis(asm, locationMap, context)
                Pair(asm, issues1 + issues2 + issues3)
            }
        }
    }

    override fun <AsmType : Any, ContextType : Any> format(sentence: String, goalRuleName: String?, automatonKind: AutomatonKind?): String? {
        val (sppt, issues1) = this.parse(sentence, goalRuleName, automatonKind)
        return sppt?.let {
            val asm = this.syntaxAnalysis<AsmType, ContextType>(sppt).first
            asm?.let { this.formatAsm<AsmType, ContextType>(asm) }
        }
    }

    override fun <AsmType : Any, ContextType : Any> formatAsm(asm: AsmType): String {
        return if (null != formatter) {
            this.formatter.format(asm)
        } else {
            asm.toString()
        }
    }

    override fun expectedAt(sentence: String, position: Int, desiredDepth: Int, goalRuleName:String?, automatonKind: AutomatonKind?): List<CompletionItem>  {
        val goal = goalRuleName ?: this.defaultGoalRuleName
        val ak = automatonKind ?: AutomatonKind.LOOKAHEAD_1
        val parserExpected: Set<RuntimeRule> = this.parser.expectedAt(goal, sentence, position, ak)
        val grammarExpected: List<RuleItem> = parserExpected
            .filter { it !== RuntimeRuleSet.END_OF_TEXT }
            .map { this._converterToRuntimeRules.originalRuleItemFor(it.runtimeRuleSetNumber, it.number) }
        val expected = grammarExpected.flatMap { this._completionProvider.provideFor(it, desiredDepth) }
        return expected.toSet().toList()
    }

}
