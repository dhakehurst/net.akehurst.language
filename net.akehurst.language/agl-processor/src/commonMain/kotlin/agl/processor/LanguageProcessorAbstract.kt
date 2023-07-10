/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
import net.akehurst.language.agl.automaton.ParserStateSet
import net.akehurst.language.agl.formatter.FormatterSimple
import net.akehurst.language.agl.grammar.grammar.ConverterToRuntimeRules
import net.akehurst.language.agl.grammar.scopes.ScopeModelAgl
import net.akehurst.language.agl.grammarTypeModel.grammarTypeModel
import net.akehurst.language.agl.parser.Parser
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsLiteral
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsPattern
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.semanticAnalyser.SemanticAnalyserSimple
import net.akehurst.language.agl.sppt.SPPTParserDefault
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserSimple
import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.grammarTypeModel.GrammarTypeModel
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTParser
import net.akehurst.language.api.sppt.SharedPackedParseTree

internal abstract class LanguageProcessorAbstract<AsmType : Any, ContextType : Any>(
) : LanguageProcessor<AsmType, ContextType> {

    override val issues = IssueHolder(LanguageProcessorPhase.ALL)

    protected abstract val _runtimeRuleSet: RuntimeRuleSet
    protected abstract val mapToGrammar: (Int, Int) -> RuleItem

    abstract override val grammar: Grammar
    protected abstract val configuration: LanguageProcessorConfiguration<AsmType, ContextType>

    private val _completionProvider: CompletionProvider by lazy { CompletionProvider(this.grammar) }
    private val _scanner by lazy { Scanner(this._runtimeRuleSet) }
    private val parser: Parser by lazy { ScanOnDemandParser(this._runtimeRuleSet) }

    override val spptParser: SPPTParser by lazy {
        val embeddedRuntimeRuleSets = grammar.allResolvedEmbeddedGrammars.map {
            val cvt = ConverterToRuntimeRules(it)
            val rrs = cvt.runtimeRuleSet
            Pair(it.name, rrs)
        }.associate { it } ?: emptyMap()
        SPPTParserDefault((parser as ScanOnDemandParser).runtimeRuleSet, embeddedRuntimeRuleSets)
    }

    protected val defaultGoalRuleName: String? by lazy { configuration.defaultGoalRuleName ?: grammar.grammarRule.first { it.isSkip.not() }.name }

    //override val grammar: Grammar? by lazy {
    //    val res = configuration.grammarResolver?.invoke()
    //    res?.let { this.issues.addAll(res.issues) }
    //    res?.asm
    //}

    override val typeModel: GrammarTypeModel by lazy {
        val res = configuration.typeModelResolver?.invoke(this)
        res?.let { this.issues.addAll(res.issues) }
        res?.asm ?: grammarTypeModel("", "<Empty>", "None") {}
    }

    override val scopeModel: ScopeModel by lazy {
        val res = configuration.scopeModelResolver?.invoke(this)
        res?.let { this.issues.addAll(res.issues) }
        res?.asm ?: ScopeModelAgl()
    }

    override val syntaxAnalyser: SyntaxAnalyser<AsmType>? by lazy {
        val res = configuration.syntaxAnalyserResolver?.invoke(this)
        res?.let { this.issues.addAll(res.issues) }
        res?.asm
    }

    override val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>? by lazy {
        val res = configuration.semanticAnalyserResolver?.invoke(this)
        res?.let { this.issues.addAll(res.issues) }
        res?.asm
    }

    override val formatterModel: AglFormatterModel? by lazy {
        val res = configuration.formatterResolver?.invoke(this)
        res?.let { this.issues.addAll(res.issues) }
        res?.asm
    }

    override val formatter: Formatter<AsmType>? by lazy {
        val res = configuration.formatterResolver?.invoke(this)
        res?.let { this.issues.addAll(res.issues) }
        FormatterSimple<AsmType>(res?.asm)
    }

    override fun usedAutomatonFor(goalRuleName: String): ParserStateSet = (this.parser as ScanOnDemandParser).runtimeRuleSet.usedAutomatonFor(goalRuleName)

    override fun interrupt(message: String) {
        this.parser.interrupt(message)
        //TODO: interrupt processor
    }

    override fun parseOptionsDefault(): ParseOptions = ParseOptionsDefault(this.defaultGoalRuleName)

    override fun optionsDefault(): ProcessOptions<AsmType, ContextType> =
        ProcessOptionsDefault<AsmType, ContextType>().also {
            it.parse.goalRuleName = this.defaultGoalRuleName
        }

    override fun buildFor(options: ParseOptions?): LanguageProcessor<AsmType, ContextType> {
        val opts = options ?: parseOptionsDefault()
        if (null == opts.goalRuleName) opts.goalRuleName = this.defaultGoalRuleName
        this.parser.buildFor(opts.goalRuleName!!, opts.automatonKind)
        return this
    }

    override fun scan(sentence: String): List<SPPTLeaf> {
        return this._scanner.scan(sentence, false)
    }

    override fun parse(sentence: String, options: ParseOptions?): ParseResult {//Pair<SharedPackedParseTree?, List<LanguageIssue>> {
        val opts = options ?: parseOptionsDefault()
        if (null == opts.goalRuleName) opts.goalRuleName = this.defaultGoalRuleName
        return this.parser.parseForGoal(opts.goalRuleName!!, sentence)
    }

    override fun syntaxAnalysis(
        sppt: SharedPackedParseTree,
        options: ProcessOptions<AsmType, ContextType>?
    ): SyntaxAnalysisResult<AsmType> { //Triple<AsmType?, List<LanguageIssue>, Map<Any, InputLocation>> {
        val opts = defaultOptions(options)
        val sa: SyntaxAnalyser<AsmType> = this.syntaxAnalyser
            ?: SyntaxAnalyserSimple(this.typeModel!!, this.scopeModel!!) as SyntaxAnalyser<AsmType>
        sa.clear()
        return sa.transform(sppt, this.mapToGrammar)
    }

    override fun semanticAnalysis(
        asm: AsmType,
        options: ProcessOptions<AsmType, ContextType>?
    ): SemanticAnalysisResult {
        val opts = defaultOptions(options)
        val semAnalyser: SemanticAnalyser<AsmType, ContextType> = this.semanticAnalyser
            ?: SemanticAnalyserSimple(this.scopeModel) as SemanticAnalyser<AsmType, ContextType>
        semAnalyser.clear()
        val lm = opts.semanticAnalysis.locationMap ?: emptyMap<Any, InputLocation>()
        return semAnalyser.analyse(asm, lm, opts.semanticAnalysis.context, opts.semanticAnalysis.options)
    }

    override fun process(
        sentence: String,
        options: ProcessOptions<AsmType, ContextType>?
    ): ProcessResult<AsmType> {
        val opts = defaultOptions(options)
        val parseResult = this.parse(sentence, opts.parse)
        return if (null == parseResult.sppt || opts.syntaxAnalysis.active.not()) {
            ProcessResultDefault(null, parseResult.issues)
        } else {
            val synxResult = this.syntaxAnalysis(parseResult.sppt!!, opts)
            if (null == synxResult.asm || opts.semanticAnalysis.active.not()) {
                ProcessResultDefault(synxResult.asm, parseResult.issues + synxResult.issues)
            } else {
                opts.semanticAnalysis.locationMap = synxResult.locationMap
                val result = this.semanticAnalysis(synxResult.asm!!, opts)
                ProcessResultDefault(synxResult.asm, parseResult.issues + synxResult.issues + result.issues)
            }
        }
    }

    override fun format(sentence: String, options: ProcessOptions<AsmType, ContextType>?): FormatResult {
        val opts = defaultOptions(options)
        val parseResult = this.parse(sentence, opts.parse)
        return if (null == parseResult.sppt) {
            FormatResultDefault(null, parseResult.issues)
        } else {
            val synxResult = this.syntaxAnalysis(parseResult.sppt!!)
            if (null == synxResult.asm) {
                FormatResultDefault(null, parseResult.issues + synxResult.issues)
            } else {
                val frmtResult = this.formatAsm(synxResult.asm!!)
                FormatResultDefault(frmtResult.sentence, parseResult.issues + synxResult.issues + frmtResult.issues)
            }
        }
    }

    override fun formatAsm(asm: AsmType, options: ProcessOptions<AsmType, ContextType>?): FormatResult {
        val opts = defaultOptions(options)
        val fResult = if (null != formatter) {
            this.formatter!!.format(asm)
        } else {
            FormatResultDefault(asm.toString(), IssueHolder(LanguageProcessorPhase.FORMATTER))
        }
        return fResult
    }

    override fun expectedTerminalsAt(sentence: String, position: Int, desiredDepth: Int, options: ProcessOptions<AsmType, ContextType>?): ExpectedAtResult {
        val opts = defaultOptions(options)
        val parserExpected: Set<RuntimeRule> = this.parser.expectedTerminalsAt(opts.parse.goalRuleName!!, sentence, position, opts.parse.automatonKind)
        //val grammarExpected: List<RuleItem> = parserExpected
        //    .filter { it !== RuntimeRuleSet.END_OF_TEXT }
        //    .map { this._converterToRuntimeRules.originalRuleItemFor(it.runtimeRuleSetNumber, it.number) }
        //val expected = grammarExpected.flatMap { this._completionProvider.provideFor(it, desiredDepth) }
        val items = parserExpected.mapNotNull {
            when {
                it == RuntimeRuleSet.END_OF_TEXT -> null
                it == RuntimeRuleSet.EMPTY -> null
                else -> {
                    val rhs = it.rhs
                    when (rhs) {
                        is RuntimeRuleRhsLiteral -> CompletionItem(CompletionItemKind.LITERAL, it.tag, rhs.value)
                        is RuntimeRuleRhsPattern -> CompletionItem(CompletionItemKind.PATTERN, it.tag, rhs.pattern)
                        else -> CompletionItem(CompletionItemKind.LITERAL, it.tag, it.tag)
                    }
                }
            }
        }
        return ExpectedAtResultDefault(items, IssueHolder(LanguageProcessorPhase.ALL))
    }

    /*
    override fun expectedAt(
        sentence: String,
        position: Int,
        desiredDepth: Int,
        options: ProcessOptions<AsmType, ContextType>?
    ): ExpectedAtResult {
        val opts = defaultOptions(options)
        val parserExpected: Set<RuntimeRule> = this.parser.expectedAt(opts.parse.goalRuleName!!, sentence, position, opts.parse.automatonKind)
        val grammarExpected: List<RuleItem> = parserExpected
            .filter { it !== RuntimeRuleSet.END_OF_TEXT }
            .map { this._converterToRuntimeRules.originalRuleItemFor(it.runtimeRuleSetNumber, it.number) }
        val expected = grammarExpected.flatMap { this._completionProvider.provideFor(it, desiredDepth) }
        val items = expected.toSet().toList()
        return ExpectedAtResultDefault(items, emptyList()) //TODO: issues
    }
     */

    private fun defaultOptions(options: ProcessOptions<AsmType, ContextType>?): ProcessOptions<AsmType, ContextType> {
        val opts = options ?: optionsDefault()
        if (null == opts.parse.goalRuleName) opts.parse.goalRuleName = this.defaultGoalRuleName
        return opts
    }
}
