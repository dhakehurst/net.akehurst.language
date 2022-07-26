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
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTParser
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.processor.*
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

    //override fun buildForDefaultGoal(): LanguageProcessor = this.buildFor(this.defaultGoalRuleName)

    override fun buildFor(options: ParserOptions?): LanguageProcessor {
        val opts = options ?: ParserOptions()
        if (null == opts.goalRuleName) opts.goalRuleName = this.defaultGoalRuleName
        this.parser.buildFor(opts.goalRuleName!!, opts.automatonKind)
        return this
    }

    override fun scan(sentence: String): List<SPPTLeaf> {
        return this._scanner.scan(sentence, false)
    }

    override fun parse(sentence: String, options: ParserOptions?): ParseResult {//Pair<SharedPackedParseTree?, List<LanguageIssue>> {
        val opts = options ?: ParserOptions()
        if (null == opts.goalRuleName) opts.goalRuleName = this.defaultGoalRuleName
        return this.parser.parseForGoal(opts.goalRuleName!!, sentence, opts.automatonKind)
    }

    override fun <AsmType : Any, ContextType : Any> syntaxAnalysis(
        sppt: SharedPackedParseTree,
        options: LanguageProcessorOptions<AsmType, ContextType>?
    ): SyntaxAnalysisResult<AsmType> { //Triple<AsmType?, List<LanguageIssue>, Map<Any, InputLocation>> {
        val opts = defaultOptions(options)
        val sa: SyntaxAnalyser<AsmType, ContextType> = (this.syntaxAnalyser ?: SyntaxAnalyserSimple(this.typeModel)) as SyntaxAnalyser<AsmType, ContextType>
        sa.clear()
        val (asm: AsmType, issues) = sa.transform(sppt, { rsn, rn -> this._converterToRuntimeRules.originalRuleItemFor(rsn, rn) }, opts.syntaxAnalyser.context)
        return SyntaxAnalysisResult(asm, issues, sa.locationMap)
    }

    override fun <AsmType : Any, ContextType : Any> semanticAnalysis(
        asm: AsmType,
        options: LanguageProcessorOptions<AsmType, ContextType>?
    ): SemanticAnalysisResult {
        val opts = defaultOptions(options)
        val semAnalyser: SemanticAnalyser<AsmType, ContextType> = ((this.semanticAnalyser as SemanticAnalyser<AsmType, ContextType>?)
            ?: SemanticAnalyserSimple<AsmType, ContextType>())
        semAnalyser.clear()
        val lm = opts.semanticAnalyser.locationMap ?: emptyMap<Any, InputLocation>()
        return semAnalyser.analyse(asm, lm, opts.syntaxAnalyser.context)
    }

    override fun <AsmType : Any, ContextType : Any> process(
        sentence: String,
        options: LanguageProcessorOptions<AsmType, ContextType>?
    ): ProcessResult<AsmType> {
        val opts = defaultOptions(options)
        val (sppt, issues1) = this.parse(sentence, opts.parser)
        return if (null == sppt) {
            ProcessResult(null, issues1)
        } else {
            val (asm, issues2, locationMap) = this.syntaxAnalysis<AsmType, ContextType>(sppt, opts)
            if (null == asm) {
                ProcessResult(null, issues1 + issues2)
            } else {
                opts.semanticAnalyser.locationMap = locationMap
                val result = this.semanticAnalysis(asm, opts)
                ProcessResult(asm, issues1 + issues2 + result.issues)
            }
        }
    }

    override fun <AsmType : Any, ContextType : Any> format(sentence: String, options: LanguageProcessorOptions<AsmType, ContextType>?): FormatResult {
        val opts = defaultOptions(options)
        val (sppt, parseIssues) = this.parse(sentence, opts.parser)
        return if (null == sppt) {
            FormatResult(null, parseIssues)
        } else {
            val (asm, syntIssues) = this.syntaxAnalysis<AsmType, ContextType>(sppt)
            if (null == asm) {
                FormatResult(null, parseIssues + syntIssues)
            } else {
                val (formattedSentence, frmtIssues) = this.formatAsm<AsmType, ContextType>(asm)
                FormatResult(formattedSentence, parseIssues + syntIssues + frmtIssues)
            }
        }
    }

    override fun <AsmType : Any, ContextType : Any> formatAsm(asm: AsmType, options: LanguageProcessorOptions<AsmType, ContextType>?): FormatResult {
        val opts = defaultOptions(options)
        val sentence = if (null != formatter) {
            this.formatter.format(asm)
        } else {
            asm.toString()
        }
        return FormatResult(sentence, emptyList())
    }

    override fun <AsmType : Any, ContextType : Any> expectedAt(
        sentence: String,
        position: Int,
        desiredDepth: Int,
        options: LanguageProcessorOptions<AsmType, ContextType>?
    ): ExpectedAtResult {
        val opts = defaultOptions(options)
        val parserExpected: Set<RuntimeRule> = this.parser.expectedAt(opts.parser.goalRuleName!!, sentence, position, opts.parser.automatonKind)
        val grammarExpected: List<RuleItem> = parserExpected
            .filter { it !== RuntimeRuleSet.END_OF_TEXT }
            .map { this._converterToRuntimeRules.originalRuleItemFor(it.runtimeRuleSetNumber, it.number) }
        val expected = grammarExpected.flatMap { this._completionProvider.provideFor(it, desiredDepth) }
        val items = expected.toSet().toList()
        return ExpectedAtResult(items, emptyList()) //TODO: issues
    }

    private fun <AsmType : Any, ContextType : Any> defaultOptions(options: LanguageProcessorOptions<AsmType, ContextType>?): LanguageProcessorOptions<AsmType, ContextType> {
        val opts = options ?: LanguageProcessorOptions()
        if (null == opts.parser.goalRuleName) opts.parser.goalRuleName = this.defaultGoalRuleName
        return opts
    }
}
