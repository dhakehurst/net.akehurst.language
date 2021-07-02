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

package net.akehurst.language.agl.parser

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.api.grammar.GrammarRuleNotFoundException
import net.akehurst.language.api.processor.AutomatonKind
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SharedPackedParseTree

internal interface Parser {

    fun interrupt(message:String)

    /**
     * It is not necessary to call this method, but doing so will speed up future calls to parse as it will build the internal caches for the parser,
     */
    fun buildFor(goalRuleName: String, automatonKind: AutomatonKind)

    /**
     * get a list of the types of node (is this useful!)
     */
    //val nodeTypes: Set<NodeType>

    /**
     * using the terminals (literals and patterns) from the grammar, scan the input text
     * and return a list of "tokens" leaves. Useful for syntax highlighting.
     * Where more than one terminal matches, the longest takes priority, and literals take priority over patterns
     */
    fun scan(inputText: String, includeSkipRules: Boolean = false): List<SPPTLeaf>

    /**
     * parse the inputText starting with the given grammar rule and return the shared packed parse Tree.
     *
     * @param goalRuleName
     * @param inputText
     * @return the result of parsing
     * @throws ParseFailedException
     * @throws ParseTreeException
     * @throws GrammarRuleNotFoundException
     */
    fun parse(goalRuleName: String, inputText: String, automatonKind: AutomatonKind): SharedPackedParseTree

    //fun parse(goalRuleName: String, inputText: Reader): SharedPackedParseTree

    /**
     * list of non-terminal or terminal runtime rules expected at the position
     *
     * @throws ParseFailedException
     * @throws ParseTreeException
     * @throws GrammarRuleNotFoundException
     **/
    fun expectedAt(goalRuleName: String, inputText: String, position: Int, automatonKind: AutomatonKind): Set<RuntimeRule>

    /*
     * List of terminal rules expected at the position
     */
    fun expectedTerminalsAt(goalRuleName: String, inputText: String, position: Int, automatonKind: AutomatonKind): Set<RuntimeRule>

    //fun expectedAt(goalRuleName: String, inputText: Reader, position: Long): List<RuleItem>
}
