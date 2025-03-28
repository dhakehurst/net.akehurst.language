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

package net.akehurst.language.parser.leftcorner.multi

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test

class test_optional_emptys : test_LeftCornerParserAbstract() {
    //TransitionReaction = StextTrigger? ('/' ReactionEffect)? ('#' TransitionProperty*)?;
    // TR = ST? RE? TP?
    // ST = ID
    // RE = '/' EX
    // TP = '#' ID
    // EX = ASS | ID
    // ASS = ID = ID
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("TR") { ref("optST"); ref("optRE"); ref("optTP") }
            optional("optST", "ST")
            concatenation("ST") { ref("ID") }
            optional("optRE", "RE")
            concatenation("RE") { literal("/"); ref("EX") }
            optional("optTP", "TP")
            concatenation("TP") { literal("#"); ref("ID") }

            choice("EX", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("ASS")
                ref("ID")
            }
            concatenation("ASS") { ref("ID"); literal("="); ref("ID") }

            pattern("ID", "[a-zA-Z]+")
        }
    }

    @Test
    fun empty() {
        val goal = "TR"
        val sentence = ""

        val expected = """
            TR {
              optST { §empty }
              optRE { §empty }
              optTP { §empty }
            }
        """.trimIndent()

        super.test_pass(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a() {
        val goal = "TR"
        val sentence = "a"

        val expected = """
            TR {
              optST { ST { ID:'a' } }
              optRE { §empty }
              optTP { §empty }
            }
        """.trimIndent()

        super.test_pass(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a_ass_b() {
        val goal = "TR"
        val sentence = "/a=b"

        val expected = """
            TR {
              optST { §empty }
              optRE { RE { '/' EX { ASS { ID:'a' '=' ID:'b' } } } }
              optTP { §empty }
            }
        """.trimIndent()

        super.test_pass(rrs, goal, sentence, 1, expected)
    }
}