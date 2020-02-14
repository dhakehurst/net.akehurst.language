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

import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SharedPackedParseTree
import kotlin.js.JsName

interface LanguageProcessor {

    /**
     * build the parser before use. Optional, but will speed up the first use of the parser.
     */
    @JsName("build")
    fun build(): LanguageProcessor;

    @JsName("scan")
    fun scan(inputText: CharSequence): List<SPPTLeaf>

    @JsName("parse")
    fun parse(inputText: CharSequence): SharedPackedParseTree

    @JsName("parseForGoal")
    fun parse(goalRuleName: String, inputText: CharSequence): SharedPackedParseTree


    @JsName("process")
    fun <T> process(inputText: CharSequence): T

    @JsName("processForGoal")
    fun <T> process(goalRuleName: String, inputText: CharSequence): T

    @JsName("processFromSPPT")
    fun <T> process(sppt: SharedPackedParseTree): T

    @JsName("formatText")
    fun <T> format(inputText: CharSequence): String

    @JsName("formatTextForGoal")
    fun <T> format(goalRuleName: String, inputText: CharSequence): String


    @JsName("formatAsm")
    fun <T> format(asm: T): String


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
    @JsName("expectedAt")
    fun expectedAt(inputText: CharSequence, position: Int, desiredDepth: Int): List<CompletionItem>

    @JsName("expectedAtForGoal")
    fun expectedAt(goalRuleName: String, inputText: CharSequence, position: Int, desiredDepth: Int): List<CompletionItem>


    //List<CompletionItem> expectedAt(Reader reader, String goalRuleName, int position, int desiredDepth)
}
