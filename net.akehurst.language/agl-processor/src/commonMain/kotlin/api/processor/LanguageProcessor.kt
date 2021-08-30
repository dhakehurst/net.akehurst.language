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
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyserItem
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTParser
import net.akehurst.language.api.sppt.SharedPackedParseTree
import kotlin.reflect.KClass

interface LanguageProcessor {

    val grammar: Grammar

    val spptParser : SPPTParser

    fun interrupt(message: String)

    /**
     * build the parser before use. Optional, but will speed up the first use of the parser.
     */
    fun buildFor(goalRuleName: String, automatonKind: AutomatonKind = AutomatonKind.LOOKAHEAD_1): LanguageProcessor;

    fun scan(inputText: String): List<SPPTLeaf>

    /**
     * use default AutomatonKind LOOKAHEAD_1
     */
    fun parse(inputText: String): SharedPackedParseTree

    fun parseWithAutomatonKind(inputText: String, automatonKind:AutomatonKind): SharedPackedParseTree

    fun parseForGoal(goalRuleName: String, inputText: String, automatonKind:AutomatonKind=AutomatonKind.LOOKAHEAD_1): SharedPackedParseTree


    fun <T : Any> process(asmType: KClass<in T>, inputText: String, automatonKind:AutomatonKind=AutomatonKind.LOOKAHEAD_1): T

    fun <T : Any> processForGoal(asmType: KClass<in T>, goalRuleName: String, inputText: String, automatonKind:AutomatonKind=AutomatonKind.LOOKAHEAD_1): T

    fun <T : Any> processFromSPPT(asmType: KClass<in T>, sppt: SharedPackedParseTree): T

    fun <T : Any> formatText(asmType: KClass<in T>, inputText: String, automatonKind:AutomatonKind=AutomatonKind.LOOKAHEAD_1): String

    fun <T : Any> formatTextForGoal(asmType: KClass<in T>, goalRuleName: String, inputText: String, automatonKind:AutomatonKind=AutomatonKind.LOOKAHEAD_1): String

    fun <T : Any> formatAsm(asmType: KClass<in T>, asm: T): String

    //fun <T> process(reader: Reader, goalRuleName: String, targetType: Class<T>): T

    /**
     * returns list of names of expected rules
     *
     * @param inputText text to parse
     * @param goalRuleName name of a rule in the grammar that is the goal rule
     * @param position position in the text (from reader) at which to provide completions
     * @param desiredDepth depth of nested rules to search when constructing possible completions
     * @return list of possible completion items
     * @throws ParseFailedException
     * @throws ParseTreeException
     */
    fun expectedAt(inputText: String, position: Int, desiredDepth: Int, automatonKind:AutomatonKind=AutomatonKind.LOOKAHEAD_1): List<CompletionItem>

    fun expectedAtForGoal(goalRuleName: String, inputText: String, position: Int, desiredDepth: Int, automatonKind:AutomatonKind=AutomatonKind.LOOKAHEAD_1): List<CompletionItem>

    //List<CompletionItem> expectedAt(Reader reader, String goalRuleName, int position, int desiredDepth)

    fun <T : Any> analyseText(asmType: KClass<in T>, inputText: String): List<SemanticAnalyserItem>

    fun <T : Any> analyseTextForGoal(asmType: KClass<in T>, goalRuleName: String, inputText: String): List<SemanticAnalyserItem>

    fun <T : Any> analyseAsm(asmType: KClass<in T>, asm: T, locationMap: Map<Any, InputLocation> = emptyMap()): List<SemanticAnalyserItem>
}
