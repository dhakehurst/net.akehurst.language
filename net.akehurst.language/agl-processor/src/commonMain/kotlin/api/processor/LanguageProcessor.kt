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

import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.SPPTLeaf
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
interface LanguageProcessor {

    val grammar: Grammar

    /**
     * An SPPT parser for this language,
     * will parse the SPPT text syntax,
     * useful for testing parser output
     */
    val spptParser: SPPTParser

    /**
     * can be called from a different thread to stop the parser
     */
    fun interrupt(message: String)

    /**
     * build the parser before use. Optional, but will speed up the first use of the parser.
     */
    fun buildFor(goalRuleName: String, automatonKind: AutomatonKind = AutomatonKind.LOOKAHEAD_1): LanguageProcessor;

    /**
     * Specifically scan the sentence using the terminal rules found in the grammar
     */
    fun scan(sentence: String): List<SPPTLeaf>

    /**
     * Parse the sentence using the grammar for this language and output a SharedPackedParseTree.
     * Parsing is performed without scanning up front, tokens are scanned for on-demand during the parse process.
     *
     * @param goalRuleName - if null the first non skip rule defined in the grammar is used
     * @param sentence the sentence to parse
     * @param automatonKind default LOOKAHEAD_1
     */
    fun parse(sentence: String, goalRuleName: String? = null, automatonKind: AutomatonKind? = null): Pair<SharedPackedParseTree?, List<LanguageIssue>>

    /**
     * Converts the SharedPackedParseTree into a language specific Abstract Syntax Tree/Model
     */
    fun <AsmType : Any, ContextType : Any> syntaxAnalysis(sppt: SharedPackedParseTree, context: ContextType? = null): Triple<AsmType?, List<LanguageIssue>, Map<*, InputLocation>>

    /**
     *
     */
    fun <AsmType : Any, ContextType : Any> semanticAnalysis(asm: AsmType, locationMap: Map<*, InputLocation>? = null, context: ContextType? = null): List<LanguageIssue>

    /**
     * Process the sentence, performing all phases where possible.
     */
    fun <AsmType : Any, ContextType : Any> process(
        sentence: String,
        goalRuleName: String? = null,
        automatonKind: AutomatonKind? = null,
        context: ContextType? = null
    ): Pair<AsmType?, List<LanguageIssue>>

    //fun <T> process(reader: Reader, goalRuleName: String, targetType: Class<T>): T

    /**
     *
     */
    fun <AsmType : Any, ContextType : Any> format(sentence: String, goalRuleName: String? = null, automatonKind: AutomatonKind? = null): String?

    fun <AsmType : Any, ContextType : Any> formatAsm(asm: AsmType): String?

    /**
     * returns list of names of expected rules
     *
     * @param sentence text to parse
     * @param position position in the text (from reader) at which to provide completions
     * @param desiredDepth depth of nested rules to search when constructing possible completions
     * @param goalRuleName name of a rule in the grammar that is the goal rule
     * @return list of possible completion items
     * @throws ParseFailedException
     * @throws ParseTreeException
     */
    fun expectedAt(sentence: String, position: Int, desiredDepth: Int, goalRuleName: String? = null, automatonKind: AutomatonKind? = null): List<CompletionItem>

    //List<CompletionItem> expectedAt(Reader reader, String goalRuleName, int position, int desiredDepth)

}
