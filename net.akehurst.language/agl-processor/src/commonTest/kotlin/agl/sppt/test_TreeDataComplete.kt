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

import net.akehurst.language.agl.agl.parser.SentenceDefault
import net.akehurst.language.agl.agl.sppt.SpptWalkerToString
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import net.akehurst.language.api.sppt.Sentence
import net.akehurst.language.api.sppt.SpptDataNode
import kotlin.test.Test
import kotlin.test.assertEquals

class test_TreeDataComplete {

    fun TreeData.asString(sentence: Sentence): String {
        val walker = SpptWalkerToString(sentence, "  ")
        this.traverseTreeDepthFirst(walker, false)
        return walker.output
    }

    @Test
    fun childrenFor__no_children() {
        //given (set up tree)
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val ss = rrs.fetchStateSetFor("S", AutomatonKind.LOOKAHEAD_1)
        val G = ss.goalRule
        val S = rrs.findRuntimeRule("S")
        val a = rrs.findRuntimeRule("'a'")
        val sentence = "a"
        val root = CompleteTreeDataNode(G, 0, sentence.length, sentence.length, 0)
        val tree = treeData(ss.number)
        tree.setRoot(root)
        val G_children = listOf(
            CompleteTreeDataNode(S, 0, 1, 1, 0)
        )
        tree.setChildren(root, G_children, false)
        val S_children = listOf(
            CompleteTreeDataNode(a, 0, 1, 1, 0)
        )
        tree.setChildren(G_children[0], S_children, false)

        //when ()
        val actual = tree.childrenFor(root)

        //then
        println(tree.asString(SentenceDefault(sentence)))
        val expected = listOf<Pair<Int, List<SpptDataNode>>>(
            Pair(0, listOf(CompleteTreeDataNode(S, 0, 1, 1, 0)))
        )
        assertEquals(expected, actual)

    }

    @Test
    fun childrenFor__no_alternatives() {

    }

    @Test
    fun childrenFor__with_alternatives() {

    }

}