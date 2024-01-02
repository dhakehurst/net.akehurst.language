/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.api.parser

import net.akehurst.language.agl.api.runtime.Rule
import net.akehurst.language.api.processor.AutomatonKind
import net.akehurst.language.api.processor.ParseOptions
import net.akehurst.language.api.processor.ParseResult

interface RuntimeSpine {
    val expectedNextTerminals: Set<Rule>
    val elements: List<Rule>
}

interface Parser {

    fun reset()

    fun interrupt(message: String)

    /**
     * It is not necessary to call this method, but doing so will speed up future calls to parse as it will build the internal caches for the parser,
     */
    fun buildFor(goalRuleName: String, automatonKind: AutomatonKind)

    /**
     * parse the inputText starting with the given grammar rule and return the shared packed parse Tree.
     *
     * @param goalRuleName
     * @param inputText
     * @return the result of parsing
     */
    fun parseForGoal(goalRuleName: String, sentenceText: String): ParseResult

    fun parse(sentenceText: String, options: ParseOptions): ParseResult

    /**
     * list of non-terminal or terminal runtime rules expected at the position
     *
     **/
    fun expectedAt(sentenceText: String, position: Int, options: ParseOptions): Set<RuntimeSpine>

    /*
     * List of terminal rules expected at the position
     */
    fun expectedTerminalsAt(sentenceText: String, position: Int, options: ParseOptions): Set<Rule>

}
