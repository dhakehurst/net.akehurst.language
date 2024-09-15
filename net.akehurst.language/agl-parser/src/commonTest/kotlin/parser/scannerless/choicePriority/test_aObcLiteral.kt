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

package net.akehurst.language.parser.leftcorner.choicePriority

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.parser.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_aObcLiteral : test_LeftCornerParserAbstract() {

    // S = a < b c;
    // a = 'a' ;
    // b = 'b' ;
    // c = 'c' ;
    private companion object {
        val rrs = runtimeRuleSet {
            choice("S",RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("a")
                ref("bc")
            }
            concatenation("a") { literal("a") }
            concatenation("bc") { ref("b"); ref("c") }
            concatenation("b") { literal("b") }
            concatenation("c") { literal("c") }
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt,issues) = super.testFail(rrs, goal, sentence,1)

        val expIssues = listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE, InputLocation(0,1,1,1),"^",setOf("'a'","'b'"))
        )
        assertEquals(null, sppt)
        assertEquals(expIssues, issues.errors)
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S {
              a { 'a' }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun ab_fails() {
        val sentence = "ab"

        val (sppt,issues) = super.testFail(rrs, goal, sentence,1)

        val expIssues = listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE, InputLocation(1,2,1,1),"a^b", setOf("<EOT>"))
        )
        assertEquals(null, sppt)
        assertEquals(expIssues, issues.errors)
    }

    @Test
    fun abc_fails() {
        val sentence = "abc"

        val (sppt,issues) = super.testFail(rrs, goal, sentence,1)

        val expIssues = listOf(
            LanguageIssue(LanguageIssueKind.ERROR,LanguageProcessorPhase.PARSE, InputLocation(1,2,1,1),"a^bc",setOf("<EOT>"))
        )
        assertEquals(null, sppt)
        assertEquals(expIssues, issues.errors)
    }

    @Test
    fun bc() {
        val sentence = "bc"

        val expected = """
            S|1 {
              bc {
                b { 'b' }
                c { 'c' }
              }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

}