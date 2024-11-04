/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.runtime.graph

import net.akehurst.language.agl.runtime.structure.RulePositionRuntime
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.automaton.leftcorner.LookaheadSet
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import net.akehurst.language.sentence.common.SentenceDefault
import net.akehurst.language.sppt.api.SpptDataNode
import net.akehurst.language.sppt.treedata.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class test_TreeData {

    private val graph = ParseGraph(SentenceDefault(""), ScannerOnDemand(RegexEnginePlatform, emptyList()), 0)

    @Test
    fun construct() {
        assertNull(graph.treeData.complete.root)
        assertTrue(graph.treeData.complete.isEmpty)
        assertTrue(graph.treeData.growingChildren.isEmpty())
        assertNull(graph.treeData.complete.initialSkip)
        assertNull(graph.treeData.complete.root!!.startPosition)
        assertNull(graph.treeData.complete.root!!.nextInputNoSkip)
    }

    @Test
    fun literal_as_root() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val sppt = SPPTParserDefault(rrs)
        val rule_S = rrs.findRuntimeRule("S")
        val rule_a = rrs.findTerminalRule("'a'")
        val SM = rrs.fetchStateSetFor(rule_S, AutomatonKind.LOOKAHEAD_1)
        val state_G0 = SM.startState
        val rule_G = state_G0.firstRule
        val state_Ge = SM.createState(listOf(RulePositionRuntime(rule_G, 0, RulePositionRuntime.END_OF_RULE)))
        val state_S = SM.createState(listOf(RulePositionRuntime(rule_S, 0, RulePositionRuntime.END_OF_RULE)))
        val state_a = SM.createState(listOf(RulePositionRuntime(rule_a, 0, RulePositionRuntime.END_OF_RULE)))
        val sut = graph.treeData

        val sentence = "a"

        sut.setFirstChildForGrowing(
            graph.createGrowingNodeIndex(state_S, setOf(LookaheadSet.ANY), 0, 1, 1, 1, null),
            graph.createGrowingNodeIndex(state_a, setOf(LookaheadSet.ANY), 0, 1, 1, 0, null).complete,
        )
        sut.setFirstChildForGrowing(
            graph.createGrowingNodeIndex(state_Ge, setOf(LookaheadSet.ANY), 0, 1, 1, 1, null),
            graph.createGrowingNodeIndex(state_S, setOf(LookaheadSet.ANY), 0, 1, 1, 1, null).complete
        )
        graph.treeData.complete.setRootTo(CompleteTreeDataNode(state_Ge.firstRule, 0, 1, 1,0,0))

        val expected = sppt.addTree(
            """
            S { 'a' }
        """.trimIndent()
        )

        val actual = SPPTFromTreeData(graph.treeData.complete as TreeDataComplete2, SentenceDefault(sentence), -1, -1)

        assertEquals(sppt.tree.toStringAll, actual.toStringAll)
        assertEquals(sppt.tree, actual)
    }

    @Test
    fun concat_of_literals() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a");literal("b");literal("c") }
        }
        val sppt = SPPTParserDefault(rrs)
        val rule_S = rrs.findRuntimeRule("S")
        val rule_a = rrs.findTerminalRule("'a'")
        val rule_b = rrs.findTerminalRule("'b'")
        val rule_c = rrs.findTerminalRule("'c'")
        val SM = rrs.fetchStateSetFor(rule_S, AutomatonKind.LOOKAHEAD_1)
        val state_G0 = SM.startState
        val rule_G = state_G0.firstRule
        val state_Ge = SM.createState(listOf(RulePositionRuntime(rule_G, 0, RulePositionRuntime.END_OF_RULE)))
        val state_S1 = SM.createState(listOf(RulePositionRuntime(rule_S, 0, 1)))
        val state_S2 = SM.createState(listOf(RulePositionRuntime(rule_S, 0, 2)))
        val state_S3 = SM.createState(listOf(RulePositionRuntime(rule_S, 0, RulePositionRuntime.END_OF_RULE)))
        val state_a = SM.createState(listOf(RulePositionRuntime(rule_a, 0, RulePositionRuntime.END_OF_RULE)))
        val state_b = SM.createState(listOf(RulePositionRuntime(rule_b, 0, RulePositionRuntime.END_OF_RULE)))
        val state_c = SM.createState(listOf(RulePositionRuntime(rule_c, 0, RulePositionRuntime.END_OF_RULE)))
        val sut = TreeDataGrowing<GrowingNodeIndex, SpptDataNode>(0)

        val sentence = "abc"

        sut.setFirstChildForGrowing(
            graph.createGrowingNodeIndex(state_S1, setOf(LookaheadSet.ANY), 0, 1, 1, 1, null),
            graph.createGrowingNodeIndex(state_a, setOf(LookaheadSet.ANY), 0, 1, 1, 0, null).complete,
        )
        sut.setNextChildForGrowingParent(
            graph.createGrowingNodeIndex(state_S1, setOf(LookaheadSet.ANY), 0, 1, 1, 1, null),
            graph.createGrowingNodeIndex(state_S2, setOf(LookaheadSet.ANY), 0, 2, 2, 2, null),
            graph.createGrowingNodeIndex(state_b, setOf(LookaheadSet.ANY), 1, 2, 2, 0, null).complete,
        )
        sut.setNextChildForGrowingParent(
            graph.createGrowingNodeIndex(state_S2, setOf(LookaheadSet.ANY), 0, 2, 2, 2, null),
            graph.createGrowingNodeIndex(state_S3, setOf(LookaheadSet.ANY), 0, 3, 3, 3, null),
            graph.createGrowingNodeIndex(state_c, setOf(LookaheadSet.ANY), 2, 3, 3, 0, null).complete,
        )
        sut.setFirstChildForGrowing(
            graph.createGrowingNodeIndex(state_Ge, setOf(LookaheadSet.ANY), 0, 3, 3, 1, null),
            graph.createGrowingNodeIndex(state_S3, setOf(LookaheadSet.ANY), 0, 3, 3, 1, null).complete,
        )
        graph.treeData.complete.setRootTo(CompleteTreeDataNode(state_Ge.firstRule, 0, 3, 3,0,0))

        val expected = sppt.addTree(
            """
            S { 'a' 'b' 'c' }
        """.trimIndent()
        )

        val actual = SPPTFromTreeData(graph.treeData.complete as TreeDataComplete2, SentenceDefault(sentence), -1, -1)

        assertEquals(sppt.tree.toStringAll, actual.toStringAll)
        assertEquals(sppt.tree, actual)
    }
}