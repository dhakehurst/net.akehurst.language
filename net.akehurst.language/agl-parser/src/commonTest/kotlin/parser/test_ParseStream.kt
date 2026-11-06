/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.leftcorner

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.issues.api.IssueCollection
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.parser.api.ParseStreamListener
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import net.akehurst.language.sppt.api.SpptDataNode
import net.akehurst.language.sppt.api.TreeData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class test_ParseStream {

    private companion object {

        class Recorder(val text: String) : ParseStreamListener {
            val events = mutableListOf<String>()
            var finished = false
            var failedAt: Int? = null
            override fun leaf(node: SpptDataNode) {
                events += "'${text.substring(node.startPosition, node.nextInputNoSkip)}'"
            }

            override fun skip(startPosition: Int, nextInputPosition: Int) {
                events += "~"
            }

            override fun branch(node: SpptDataNode, numChildren: Int) {
                events += "${node.rule.tag}/$numChildren"
            }

            override fun finished(issues: IssueCollection<LanguageIssue>) {
                finished = true
            }

            override fun failed(issues: IssueCollection<LanguageIssue>, committedUpTo: Int) {
                failedAt = committedUpTo
            }
        }

        fun parser(rrs: RuntimeRuleSet) = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)

        /** post-order of the tree from a normal parse, in the same form as [Recorder] */
        fun postOrder(text: String, td: TreeData): List<String> {
            val out = mutableListOf<String>()
            fun skip(s: TreeData?) {
                if (null != s) out += "~"
            }

            fun walk(n: SpptDataNode) {
                if (n.rule.isTerminal) {
                    out += "'${text.substring(n.startPosition, n.nextInputNoSkip)}'"
                    skip(td.skipDataAfter(n))
                } else {
                    val ch = td.childrenFor(n).first().second
                    ch.forEach { walk(it) }
                    out += "${n.rule.tag}/${ch.size}"
                }
            }
            skip(td.initialSkip)
            walk(td.userRoot)
            return out
        }

        fun assertSameAsTree(rrs: RuntimeRuleSet, goal: String, sentence: String) {
            val normal = parser(rrs).parse(sentence, ParseOptionsDefault(goalRuleName = goal))
            assertNotNull(normal.sppt, normal.issues.toString())
            val rec = Recorder(sentence)
            val streamed = parser(rrs).parse(sentence, ParseOptionsDefault(goalRuleName = goal, streamListener = rec))
            assertNull(streamed.sppt, "no tree is kept when streaming")
            assertTrue(rec.finished, "$goal '$sentence' ${streamed.issues}")
            assertEquals(postOrder(sentence, normal.sppt!!.treeData), rec.events)
        }

        val rrsConcatWS = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            concatenation("S") { literal("class"); ref("NAME"); literal(";") }
            pattern("NAME", "[a-zA-Z]+")
        }

        val rrsList = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            multi("S", 0, -1, "decl")
            concatenation("decl") { literal("var"); ref("NAME"); literal(";") }
            pattern("NAME", "[a-z]+")
        }

        val rrsListThenB = runtimeRuleSet {
            concatenation("S") { ref("as"); literal("b") }
            multi("as", 0, -1, "'a'")
            literal("'a'", "a")
        }

        // S = A 'x' | B 'y' ; A = 'a'* ; B = 'a'*  -- needs unbounded lookahead
        val rrsLongLookahead = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("A"); literal("x") }
                concatenation { ref("B"); literal("y") }
            }
            multi("A", 0, -1, "'a'")
            multi("B", 0, -1, "'a'")
            literal("'a'", "a")
        }

        // S = B S 'c' | 'a' ; B = 'b' | <empty>  -- hidden left recursion, the GSS has a cycle
        val rrsHiddenLeft = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("B"); ref("S"); literal("c") }
                concatenation { literal("a") }
            }
            choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("b")
                ref("Be")
            }
            concatenation("Be") { empty() }
        }
    }

    @Test
    fun concatenation_with_skip() {
        val sentence = "class A ;"
        val rec = Recorder(sentence)
        val r = parser(rrsConcatWS).parse(sentence, ParseOptionsDefault(goalRuleName = "S", streamListener = rec))
        assertNull(r.sppt)
        assertTrue(r.issues.errors.isEmpty(), r.issues.toString())
        assertTrue(rec.finished)
        assertEquals(listOf("'class'", "~", "'A'", "~", "';'", "S/3"), rec.events)
    }

    @Test
    fun same_as_tree() {
        assertSameAsTree(rrsConcatWS, "S", " class A ; ")
        assertSameAsTree(rrsList, "S", "")
        assertSameAsTree(rrsList, "S", "var a; var b ; var c;")
        assertSameAsTree(rrsListThenB, "S", "b")
        assertSameAsTree(rrsListThenB, "S", "aaab")
        assertSameAsTree(rrsLongLookahead, "S", "aaaay")
        assertSameAsTree(rrsHiddenLeft, "S", "a")
        assertSameAsTree(rrsHiddenLeft, "S", "bac")
        assertSameAsTree(rrsHiddenLeft, "S", "acc")
        assertSameAsTree(rrsHiddenLeft, "S", "bacc")
    }

    @Test
    fun nodes_are_sent_before_the_end_of_the_sentence() {
        // the parse fails at the end, but the start of the list was already sent
        val sentence = "aaaa"
        val rec = Recorder(sentence)
        val r = parser(rrsListThenB).parse(sentence, ParseOptionsDefault(goalRuleName = "S", streamListener = rec))
        assertNull(r.sppt)
        assertTrue(r.issues.errors.isNotEmpty())
        assertEquals(false, rec.finished)
        val failedAt = rec.failedAt
        assertNotNull(failedAt)
        assertTrue(failedAt > 0, "some text should have been sent, committed up to $failedAt")
        assertTrue(rec.events.isNotEmpty())
        assertTrue(rec.events.all { it == "'a'" }, rec.events.toString())
    }

    @Test
    fun long_lookahead_holds_back_and_warns() {
        val sentence = "a".repeat(20) + "y"
        val rec = Recorder(sentence)
        val r = parser(rrsLongLookahead).parse(sentence, ParseOptionsDefault(goalRuleName = "S", streamListener = rec, streamLagWarningLimit = 5))
        assertTrue(rec.finished)
        val w = r.issues.warnings.filter { it.message.startsWith("Streaming held back") }
        assertEquals(1, w.size, r.issues.toString())
        assertTrue(w[0].message.contains("A") && w[0].message.contains("B"), w[0].message)
    }

    @Test
    fun ambiguity_is_an_error_and_stops() {
        // S = S S | 'a' with an ambiguous choice: 'aaa' has two trees
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.AMBIGUOUS) {
                concatenation { ref("S"); ref("S") }
                concatenation { literal("a") }
            }
        }
        val sentence = "aaa"
        val rec = Recorder(sentence)
        val r = parser(rrs).parse(sentence, ParseOptionsDefault(goalRuleName = "S", streamListener = rec))
        assertNull(r.sppt)
        assertEquals(false, rec.finished)
        assertNotNull(rec.failedAt)
        val e = r.issues.errors.filter { it.message.startsWith("Ambiguity: text matching rule 'S'") }
        assertEquals(1, e.size, r.issues.toString())
        assertTrue(e[0].message.contains("Streaming stopped"), e[0].message)
    }

    @Test
    fun ambiguity_is_not_an_error_without_streaming() {
        // same grammar, normal parse: unchanged behaviour, a tree and no error
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.AMBIGUOUS) {
                concatenation { ref("S"); ref("S") }
                concatenation { literal("a") }
            }
        }
        val r = parser(rrs).parse("aaa", ParseOptionsDefault(goalRuleName = "S"))
        assertNotNull(r.sppt)
        assertTrue(r.issues.errors.isEmpty(), r.issues.toString())
    }

    @Test
    fun ambiguity_stops_before_the_end() {
        // the ambiguity is in the second decl; later text is not parsed further
        // S = decl* ; decl = E ';' ; E = E E | 'a' (ambiguous choice)
        val rrs = runtimeRuleSet {
            multi("S", 0, -1, "decl")
            concatenation("decl") { ref("E"); literal(";") }
            choice("E", RuntimeRuleChoiceKind.AMBIGUOUS) {
                concatenation { ref("E"); ref("E") }
                concatenation { literal("a") }
            }
        }
        val sentence = "a;" + "aaa;" + "a;".repeat(50)
        val rec = Recorder(sentence)
        val r = parser(rrs).parse(sentence, ParseOptionsDefault(goalRuleName = "S", streamListener = rec))
        assertEquals(false, rec.finished)
        val failedAt = rec.failedAt
        assertNotNull(failedAt)
        assertTrue(failedAt < 10, "stopped at the ambiguity, committed up to $failedAt")
        assertEquals(1, r.issues.errors.count { it.message.startsWith("Ambiguity: text matching rule 'E'") }, r.issues.toString())
    }

    @Test
    fun no_warning_when_streaming_keeps_up() {
        val sentence = "var a; ".repeat(200)
        val rec = Recorder(sentence)
        val r = parser(rrsList).parse(sentence, ParseOptionsDefault(goalRuleName = "S", streamListener = rec, streamLagWarningLimit = 50))
        assertTrue(rec.finished)
        assertTrue(r.issues.isEmpty(), r.issues.toString())
    }
}
