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

import net.akehurst.language.agl.completionProvider.CompletionProviderAbstract
import net.akehurst.language.agl.completionProvider.SpineDefault
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.automaton.api.Automaton
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.format.processor.FormatterOverAsmSimple
import net.akehurst.language.formatter.api.AglFormatModel
import net.akehurst.language.grammar.api.*
import net.akehurst.language.grammar.processor.AglGrammar
import net.akehurst.language.grammar.processor.ConverterToRuntimeRules
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.issues.ram.plus
import net.akehurst.language.parser.api.ParseOptions
import net.akehurst.language.parser.api.ParseResult
import net.akehurst.language.parser.api.Parser
import net.akehurst.language.parser.api.RuleSet
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.scanner.api.ScanOptions
import net.akehurst.language.scanner.api.ScanResult
import net.akehurst.language.scanner.api.Scanner
import net.akehurst.language.sentence.common.SentenceDefault
import net.akehurst.language.sppt.api.SPPTParser
import net.akehurst.language.sppt.api.SharedPackedParseTree
import net.akehurst.language.sppt.treedata.SPPTParserDefault
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.transform.api.TransformRuleSet
import net.akehurst.language.transform.asm.TransformDomainDefault
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.builder.typeModel

internal abstract class LanguageProcessorAbstract<AsmType : Any, ContextType : Any>(
) : LanguageProcessor<AsmType, ContextType> {

    override val issues = IssueHolder(LanguageProcessorPhase.ALL)

    abstract override val targetRuleSet: RuleSet?
    protected abstract val mapToGrammar: (Int, Int) -> RuleItem?

    abstract override val grammarModel: GrammarModel

    override val scanner: Scanner? by lazy {
        val res = configuration.scannerResolver?.invoke(this)
        res?.let { this.issues.addAllFrom(res.allIssues) }
        res?.asm
    }

    override val parser: Parser? by lazy {
        //ScanOnDemandParser(this.runtimeRuleSet)
        val res = configuration.parserResolver?.invoke(this)
        res?.let { this.issues.addAllFrom(res.allIssues) }
        res?.asm
    }

    override val spptParser: SPPTParser by lazy {
        val embeddedRuntimeRuleSets = targetGrammar?.allResolvedEmbeddedGrammars?.map {
            val cvt = ConverterToRuntimeRules(it)
            val rrs = cvt.runtimeRuleSet
            Pair(it.qualifiedName.value, rrs)
        }?.associate { it } ?: emptyMap()
        SPPTParserDefault((parser as LeftCornerParser).ruleSet, embeddedRuntimeRuleSets)
    }

    protected val defaultGoalRuleName: GrammarRuleName? by lazy {
        configuration.defaultGoalRuleName
            ?: targetGrammar?.options?.get(AglGrammar.OPTION_defaultGoalRule)?.let { GrammarRuleName(it) }
            ?: targetGrammar?.grammarRule?.firstOrNull { it.isSkip.not() }?.name
    }

    override val targetGrammar: Grammar? by lazy {
        this.grammarModel.allDefinitions.lastOrNull { it.name == configuration.targetGrammarName } ?: this.grammarModel.primary
    }

    override val baseTypeModel: TypeModel by lazy {
        val res = configuration.typesResolver?.invoke(this)
        res?.let { this.issues.addAllFrom(res.allIssues) }
        res?.asm
            ?: typeModel("FromGrammar" + this.grammarModel.name.value, true) {}
    }

    override val typesModel: TypeModel get() = this.transformModel.typeModel ?: error("Should not happen")

    override val transformModel: TransformModel by lazy {
        val res = configuration.transformResolver?.invoke(this)
        res?.let { this.issues.addAllFrom(res.allIssues) }
        res?.asm
            ?: TransformDomainDefault.fromGrammarModel(this.grammarModel, this.baseTypeModel).asm
            ?: error("should not happen")
    }

    override val targetTransformRuleSet: TransformRuleSet by lazy {
        targetGrammar?.let { transformModel.findNamespaceOrNull(it.namespace.qualifiedName)?.findOwnedDefinitionOrNull(it.name) }
            ?: error("Target TransformRuleSet not found for grammar '${targetGrammar?.qualifiedName ?: "null"}'")
    }

    override val crossReferenceModel: CrossReferenceModel by lazy {
        val res = configuration.crossReferenceResolver?.invoke(this)
        res?.let { this.issues.addAllFrom(res.allIssues) }
        res?.asm ?: CrossReferenceModelDefault(SimpleName("FromGrammar" + grammarModel.name.value))
    }

    override val syntaxAnalyser: SyntaxAnalyser<AsmType>? by lazy {
        val res = configuration.syntaxAnalyserResolver?.invoke(this)
        res?.let { this.issues.addAllFrom(res.allIssues) }
        res?.asm
    }

    override val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>? by lazy {
        val res = configuration.semanticAnalyserResolver?.invoke(this)
        res?.let { this.issues.addAllFrom(res.allIssues) }
        res?.asm
    }

    override val formatModel: AglFormatModel? by lazy {
        val res = configuration.formatResolver?.invoke(this)
        res?.let { this.issues.addAllFrom(res.allIssues) }
        res?.asm
    }

    override val formatter: Formatter<AsmType>? by lazy {
        val res = configuration.formatResolver?.invoke(this)
        res?.let {
            this.issues.addAllFrom(res.allIssues)
            res.asm?.let {
                //TODO: make a formatter Resolver !
                FormatterOverAsmSimple(it, typesModel, this.issues) as Formatter<AsmType>
            }
        }
    }

    override val completionProvider: CompletionProvider<AsmType, ContextType>? by lazy {
        val res = configuration.completionProviderResolver?.invoke(this)
        res?.let { this.issues.addAllFrom(res.allIssues) }
        res?.asm
    }

    override fun usedAutomatonFor(goalRuleName: String): Automaton = this.parser!!.ruleSet.usedAutomatonFor(goalRuleName)

    override fun interrupt(message: String) {
        this.parser?.interrupt(message)
        //TODO: interrupt other parts of the processor
    }

    override fun parseOptionsDefault(): ParseOptions = ParseOptionsDefault(
        goalRuleName = this.defaultGoalRuleName?.value
    )

    override fun optionsDefault(): ProcessOptions<AsmType, ContextType> =
        ProcessOptionsDefault<AsmType, ContextType>().also {
            it.parse.goalRuleName = this.defaultGoalRuleName?.value
        }

    override fun buildFor(options: ParseOptions?): LanguageProcessor<AsmType, ContextType> {
        val opts = options ?: parseOptionsDefault()
        if (null == opts.goalRuleName) opts.goalRuleName = this.defaultGoalRuleName?.value
        this.parser?.buildFor(opts.goalRuleName!!)
        return this
    }

    override fun scan(sentence: String, options: ScanOptions?): ScanResult {
        return this.scanner?.scan(SentenceDefault(sentence, null), options)?.also { scanner?.reset() } //TODO: check do we need a sentenceID ?
            ?: error("The processor for grammar '${this.targetGrammar?.qualifiedName}' was not configured with a Scanner")
    }

    override fun parse(sentence: String, options: ParseOptions?): ParseResult {//Pair<SharedPackedParseTree?, List<LanguageIssue>> {
        val opts = options ?: parseOptionsDefault()
        if (null == opts.goalRuleName) opts.goalRuleName = this.defaultGoalRuleName?.value
        return this.parser?.parse(sentence, opts)?.also { scanner?.reset(); parser?.reset() }
            ?: error("The processor for grammar '${this.targetGrammar?.qualifiedName}' was not configured with a Parser")
    }

    override fun syntaxAnalysis(
        sppt: SharedPackedParseTree,
        options: ProcessOptions<AsmType, ContextType>?
    ): SyntaxAnalysisResult<AsmType> {
        val opts = defaultOptions(options)
        val sa: SyntaxAnalyser<AsmType> = this.syntaxAnalyser
            ?: error("The processor for grammar '${this.targetGrammar?.qualifiedName}' was not configured with a SyntaxAnalyser")
        sa.clear<AsmType>()
        return sa.transform(sppt, this.mapToGrammar) as SyntaxAnalysisResult<AsmType>
    }

    override fun semanticAnalysis(asm: AsmType, options: ProcessOptions<AsmType, ContextType>?): SemanticAnalysisResult {
        val opts = defaultOptions(options)
        val semAnalyser = this.semanticAnalyser
            ?: error("the processor for grammar '${this.targetGrammar?.qualifiedName}' was not configured with a SemanticAnalyser")
        semAnalyser.clear()
        val sentenceId = opts.parse.sentenceIdentity?.invoke()
        val lm = opts.semanticAnalysis.locationMap
        return semAnalyser.analyse(sentenceId, asm, lm, opts.semanticAnalysis)
    }

    override fun process(sentence: String, options: ProcessOptions<AsmType, ContextType>?): ProcessResult<AsmType> {
        val opts = defaultOptions(options)
        val parseResult = this.parse(sentence, opts.parse)
        val sppt = parseResult.sppt
        return if (null == sppt || opts.syntaxAnalysis.enabled.not()) {
            ProcessResultDefault(null, parseResult)
        } else {
            val synxResult: SyntaxAnalysisResult<AsmType> = this.syntaxAnalysis(sppt, opts)
            val asm = synxResult.asm
            if (null == asm || opts.semanticAnalysis.enabled.not() || null == this.semanticAnalyser) {
                val issues = IssueHolder()
                if (null == this.semanticAnalyser) issues.info(null, "There is no SemanticAnalyser configured")
                if (null == synxResult.asm) issues.info(null, "There was no ASM returned by the SyntaxAnalyser")
                if (opts.semanticAnalysis.enabled.not()) issues.info(null, "Semantic Analysis is inactive (switched off) in the options")
                ProcessResultDefault(synxResult.asm,parseResult,synxResult, processIssues = issues)
            } else {
                opts.semanticAnalysis.locationMap = synxResult.locationMap
                val result = this.semanticAnalysis(asm, opts)
                ProcessResultDefault(synxResult.asm, parseResult, synxResult, result)
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
        val formatSetName = this.targetGrammar!!.qualifiedName //TODO: make configuratble in options
        //val fm = formatModel?: error("the processor for grammar '${this.targetGrammar?.qualifiedName}' was not configured with a FormatModel")
        val frmtr = this.formatter
            ?: error("the processor for grammar '${this.targetGrammar?.qualifiedName}' was not configured with a Formatter")
        return frmtr.format(formatSetName, asm)
    }

    override fun expectedTerminalsAt(sentence: String, position: Int, options: ProcessOptions<AsmType, ContextType>?): ExpectedAtResult {
        val opts = defaultOptions(options)
        val parserExpected = this.parser?.expectedTerminalsAt(sentence, position, opts.parse)
            ?: error("The processor for grammar '${this.targetGrammar?.qualifiedName}' was not configured with a Parser")
        val terminalItems = parserExpected.mapNotNull {
            when {
                it.isEndOfText -> null
                it.isEmptyTerminal -> null
                it.isEmptyListTerminal -> null
                else -> {
                    val rr = it as RuntimeRule
                    val terminalRuleItem = mapToGrammar(rr.runtimeRuleSetNumber, rr.ruleNumber) as Terminal?
                    terminalRuleItem?.let { CompletionProviderAbstract.provideForTangible(it, opts.completionProvider) }
                    //when {
                    //    it.isLiteral -> CompletionItem(CompletionItemKind.LITERAL, it.unescapedTerminalValue, it.tag)
                    //    it.isPattern -> CompletionItem(CompletionItemKind.PATTERN, "<${it.tag}>", "${it.tag}")
                    //    else -> CompletionItem(CompletionItemKind.SEGMENT, it.tag, it.tag) //TODO
                    //}
                }
            }
        }.flatten()
        return ExpectedAtResultDefault(0, terminalItems, IssueHolder(LanguageProcessorPhase.ALL))
    }

    override fun expectedItemsAt(sentence: String, position: Int, options: ProcessOptions<AsmType, ContextType>?): ExpectedAtResult {
        return when {
            null != completionProvider -> {
                val opts = defaultOptions(options)
                val parserExpected = this.parser?.expectedAt(sentence, position, opts.parse)
                    ?: error("The processor for grammar '${this.targetGrammar?.qualifiedName}' was not configured with a Parser")
                val spines = parserExpected.spines.map { rtSpine -> SpineDefault(rtSpine, mapToGrammar) }.toSet()
                val items = completionProvider!!.provide(spines, opts.completionProvider)
                if (parserExpected.usedPosition == position) {
                    ExpectedAtResultDefault(0, items, IssueHolder(LanguageProcessorPhase.ALL))
                } else {
                    val prefix = sentence.substring(parserExpected.usedPosition, position)
                    val filtered = items.filter {
                        it.text.startsWith(prefix)
                    }
                    ExpectedAtResultDefault(position - parserExpected.usedPosition, filtered, IssueHolder(LanguageProcessorPhase.ALL))
                }
            }

            else -> expectedTerminalsAt(sentence, position, options)
        }
    }

    internal fun defaultOptions(options: ProcessOptions<AsmType, ContextType>?): ProcessOptions<AsmType, ContextType> {
        val opts = options ?: optionsDefault()
        if (null == opts.parse.goalRuleName) opts.parse.goalRuleName = this.defaultGoalRuleName?.value
        return opts
    }
}
