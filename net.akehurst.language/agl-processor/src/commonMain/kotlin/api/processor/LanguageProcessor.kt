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

package net.akehurst.language.api.processor

import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.automaton.Automaton
import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammarTypeModel.GrammarTypeModel
import net.akehurst.language.api.sppt.LeafData
import net.akehurst.language.api.sppt.SPPTParser
import net.akehurst.language.api.sppt.SharedPackedParseTree

/**
 * A LanguageProcessor is used to process a sentence using a given grammar.
 * In this context, the stages in processing a language are defined as:
 *   - scan: produce list of tokens / leaves
 *   - parse: produce a SharedPackedParseTree
 *   - syntaxAnalysis: produce an abstract syntax tree
 *   - semanticAnalysis: produce a list of SemanticAnalyserIssue to indicate errors and warnings about the semantics of the sentence
 */
interface LanguageProcessor<AsmType : Any, ContextType : Any> {

    val issues: IssueCollection

    val grammar: Grammar?

    /**
     * An SPPT parser for this language,
     * will parse the SPPT text syntax,
     * useful for testing parser output
     */
    val spptParser: SPPTParser

    /**
     * model of the types instantiated by syntaxAnalysis for the LanguageDefinition of this LanguageProcessor
     */
    val typeModel: GrammarTypeModel

    /*
     * model of the scopes and references for the LanguageDefinition of this LanguageProcessor
     */
    val scopeModel: ScopeModel

    val formatterModel: AglFormatterModel?

    val syntaxAnalyser: SyntaxAnalyser<AsmType>?

    val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>?

    val formatter: Formatter<AsmType>?

    /**
     * can be called from a different thread to stop the parser
     */
    fun interrupt(message: String)

    /**
     * get the default options for this parser
     */
    fun parseOptionsDefault(): ParseOptions

    /**
     * get the default options for this language processor
     */
    fun optionsDefault(): ProcessOptions<AsmType, ContextType>

    /**
     * build the parser before use. Optional, but will speed up the first use of the parser.
     */
    fun buildFor(options: ParseOptions? = null): LanguageProcessor<AsmType, ContextType>

    fun usedAutomatonFor(goalRuleName: String): Automaton

    /**
     * Specifically scan the sentence using the terminal rules found in the grammar
     */
    fun scan(sentence: String): List<LeafData>

    /**
     * Parse the sentence using the grammar for this language and output a SharedPackedParseTree.
     * Parsing is performed without scanning up front, tokens are scanned for on-demand during the parse process.
     *
     * @param goalRuleName - if null the first non skip rule defined in the grammar is used
     * @param sentence the sentence to parse
     * @param automatonKind default LOOKAHEAD_1
     */
    fun parse(sentence: String, options: ParseOptions? = null): ParseResult//Pair<SharedPackedParseTree?, List<LanguageIssue>>

    /**
     * Converts the SharedPackedParseTree into a language specific Abstract Syntax Tree/Model
     */
    fun syntaxAnalysis(sppt: SharedPackedParseTree, options: ProcessOptions<AsmType, ContextType>? = null): SyntaxAnalysisResult<AsmType>

    /**
     *
     */
    fun semanticAnalysis(asm: AsmType, options: ProcessOptions<AsmType, ContextType>? = null): SemanticAnalysisResult

    /**
     * Process the sentence, performing all phases where possible.
     */
    fun process(
        sentence: String,
        options: ProcessOptions<AsmType, ContextType>? = null
    ): ProcessResult<AsmType>

    //fun <T> process(reader: Reader, goalRuleName: String, targetType: Class<T>): T

    /**
     *
     */
    fun format(sentence: String, options: ProcessOptions<AsmType, ContextType>? = null): FormatResult

    fun formatAsm(asm: AsmType, options: ProcessOptions<AsmType, ContextType>? = null): FormatResult

    /**
     * returns list of terminals expected at the given position
     *
     * @param sentence text to parse
     * @param position position in the text (from reader) at which to provide expectations
     * @param desiredDepth depth of nested rules to search when constructing possible completions
     * @param goalRuleName name of a rule in the grammar that is the goal rule
     * @return list of possible completion items
     * @throws ParseFailedException
     * @throws ParseTreeException
     */
    fun expectedTerminalsAt(sentence: String, position: Int, desiredDepth: Int, options: ProcessOptions<AsmType, ContextType>? = null): ExpectedAtResult

    //List<CompletionItem> expectedAt(Reader reader, String goalRuleName, int position, int desiredDepth)

}
