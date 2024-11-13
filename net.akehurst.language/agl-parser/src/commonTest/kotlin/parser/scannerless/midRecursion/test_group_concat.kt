/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.leftcorner.midRecursion

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test

class test_group_concat : test_LeftCornerParserAbstract() {

    /*
        skip WS = "\s+" ;
        S = rules ;
        rules = normalRule* ;
        normalRule = ID '=' concat ';' ;
        concat = concatItem+ ;
        concatItem = ID | group ;
        group = '(' concat ')' ;
        ID = "[a-zA-Z]+" ;
     */

    private companion object {

        val rrs = runtimeRuleSet {
            concatenation("WS", true) { pattern("\\s+") }
            concatenation("S") { ref("rules") }
            multi("rules",0,-1,"normalRule")
            concatenation("normalRule") { ref("ID"); literal("="); ref("concat"); literal(";")}
            multi("concat", 1, -1, "concatItem")
            choice("concatItem",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("ID")
                ref("group")
            }
            concatenation("group") { literal("("); ref("concat"); literal(")") }
            pattern("ID","[a-zA-Z]+")
        }

        val goal = "S"
    }

    @Test
    fun rEQaSEMI() {
        val sentence = "r=a;"

        val expected = """
         S { rules { normalRule {
              ID : 'r'
              '='
              concat { concatItem { ID : 'a' } }
              ';'
            } } }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun rEQPOaPC() {
        val sentence = "r=(a);"

        val expected = """
         S { rules { normalRule {
              ID : 'r'
              '='
              concat { concatItem|1 { group {
                    '('
                    concat { concatItem { ID : 'a' } }
                    ')'
                  } } }
              ';'
            } } }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }
    @Test
    fun bbacc() {
        val sentence = "r=((a));"

        val expected = """
         S { rules { normalRule {
              ID : 'r'
              '='
              concat { concatItem|1 { group {
                    '('
                    concat { concatItem|1 { group {
                          '('
                          concat { concatItem { ID : 'a' } }
                          ')'
                        } } }
                    ')'
                  } } }
              ';'
            } } }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

}