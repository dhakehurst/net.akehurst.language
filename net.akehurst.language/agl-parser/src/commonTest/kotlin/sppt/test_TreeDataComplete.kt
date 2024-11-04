/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.sppt

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.sentence.common.SentenceDefault
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNode
import net.akehurst.language.sppt.api.TreeData
import net.akehurst.language.sppt.treedata.CompleteTreeDataNode
import net.akehurst.language.sppt.treedata.SpptWalkerToString
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_TreeDataComplete {

    companion object {
        fun TreeData.asString(sentence: Sentence): String {
            val walker = SpptWalkerToString(sentence, "  ")
            this.traverseTreeDepthFirst(walker, false)
            return walker.output
        }

        fun test(sentence: String, rrs: RuntimeRuleSet, goal: String, parent: SpptDataNode, expected: List<Pair<Int, List<SpptDataNode>>>) {
            //when ()
            val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
            val tree = parser.parse(sentence, ParseOptionsDefault(goal)).sppt!!.treeData
            val actual = tree.childrenFor(parent)

            //then
            println(tree.asString(SentenceDefault(sentence)))
            assertEquals(expected, actual)
        }
    }

    @Test
    fun childrenFor__no_children() {
        val sentence = "a"
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val a = rrs.findRuntimeRule("'a'")

        val expected = listOf<Pair<Int, List<SpptDataNode>>>()
        val parent = CompleteTreeDataNode(a, 0, 1, 1, 0,-1)

        test(sentence, rrs, "S", parent, expected)
    }

    @Test
    fun childrenFor__no_alternatives() {
        val sentence = "a"
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val S = rrs.findRuntimeRule("S")
        val a = rrs.findRuntimeRule("'a'")

        val expected = listOf<Pair<Int, List<SpptDataNode>>>(
            Pair(0, listOf(CompleteTreeDataNode(a, 0, 1, 1, 0,-1)))
        )
        val parent = CompleteTreeDataNode(S, 0, 1, 1, 0,-1)

        test(sentence, rrs, "S", parent, expected)
    }

    @Test
    fun childrenFor__with_alternatives() {
        val sentence = "a"
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("A")
            }
            concatenation("A") {
                literal("a")
            }
        }
        val S = rrs.findRuntimeRule("S")
        val a = rrs.findRuntimeRule("'a'")

        val expected = listOf<Pair<Int, List<SpptDataNode>>>(
            Pair(0, listOf(CompleteTreeDataNode(a, 0, 1, 1, 0,-1)))
        )
        val parent = CompleteTreeDataNode(S, 0, 1, 1, 0,-1)

        test(sentence, rrs, "S", parent, expected)
    }

}