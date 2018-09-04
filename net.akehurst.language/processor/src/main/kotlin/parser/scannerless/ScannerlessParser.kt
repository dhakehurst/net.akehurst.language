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

package net.akehurst.language.parser.scannerless

import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.GrammarRuleNotFoundException
import net.akehurst.language.api.grammar.NodeType
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.parser.ParseTreeException
import net.akehurst.language.api.parser.Parser
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.ogl.runtime.graph.ParseGraph
import net.akehurst.language.ogl.runtime.structure.RuntimeRule
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.sppt.SharedPackedParseTreeDefault

class ScannerlessParser(val runtimeRuleSet: RuntimeRuleSet) : Parser {

    override val nodeTypes: Set<NodeType> by lazy {
        emptySet<NodeType>() //TODO:
    }

    override fun build() {
        throw UnsupportedOperationException()
    }

    override fun parse(goalRuleName: String, inputText: CharSequence): SharedPackedParseTree {
        val goalRule = this.runtimeRuleSet.findRuntimeRule(goalRuleName)
        val input = InputFromCharSequence(inputText)
        val graph = ParseGraph(goalRule, input)

        var seasons = 0

        graph.start(goalRule)
        seasons++

        do {
            graph.grow()
            seasons++
        } while (graph.canGrow)

        val match = graph.longestMatch
        return SharedPackedParseTreeDefault(match)
    }


    override fun expectedAt(goalRuleName: String, inputText: CharSequence, position: Int): List<RuleItem> {
        throw UnsupportedOperationException()
    }
}