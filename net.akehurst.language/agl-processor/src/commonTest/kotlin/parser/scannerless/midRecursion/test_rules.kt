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

package net.akehurst.language.parser.scannerless.rightRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test

class test_rules : test_ScannerlessParserAbstract() {

    companion object {
        val S = runtimeRuleSet {
            skip("W") { pattern("\\s+") }
            concatenation("S") { ref("rules") }
            multi("rules",0,-1,"normalRule")
            concatenation("normalRule") { ref("ID"); literal("="); ref("choice"); literal(";")}
            sList("choice",0,-1,"concat", "chSep")
            literal("chSep","|")
            multi("concat", 1, -1, "concatItem")
            choice("concatItem",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("simpleItem")
                ref("multi")
            }
            choice("simpleItem",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("ID")
                ref("group")
            }
            concatenation("multi") { ref("simpleItem"); ref("N") }
            choice("N", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("?")
                literal("*")
            }
            concatenation("group") { literal("("); ref("choice"); literal(")") }
            pattern("ID","[a-zA-Z]+")
        }
    }

    @Test
    fun a() {
        val sentence = "r=a;"
        val goal = "S"
        val expected = """
            S { Rs { R {
                Is { I { V:'a' } }
                ';'
            } } }
        """.trimIndent()
        super.test(S,goal,sentence,expected)
    }

    @Test
    fun bac() {
        val sentence = "r=(a);"
        val goal = "S"
        val expected = """
            S { Rs { R {
                Is { I { G {
                    'b' I { 'a' } 'c'
                } } }
                ';'
            } } }
        """.trimIndent()
        super.test(S,goal,sentence,expected)
    }
    @Test
    fun bbacc() {
        val sentence = "r=((a));"
        val goal = "S"
        val expected = """
             S { Rs { R {
                  Is { I { G {
                        'b'
                        I { G {
                            'b'
                            I { V:'a' }
                            'c'
                          } }
                        'c'
                      } } }
                  ';'
                } } }
        """.trimIndent()
        super.test(S,goal,sentence,expected)
    }
    @Test
    fun aqaq() {
        val sentence = "r=a?a?;"
        val goal = "S"
        val expected = """
            S { I { G {
              'b'
              I { G {
                  'b'
                  I { V:'a' }
                  'c'
                } }
              'c'
            } } }
        """.trimIndent()
        super.test(S,goal,sentence,expected)
    }
    @Test
    fun aq_aq_aq() {
        val sentence = "r=a?;r=a?;r=a?;"
        val goal = "S"
        val expected = """
            S { I { G {
              'b'
              I { G {
                  'b'
                  I { V:'a' }
                  'c'
                } }
              'c'
            } } }
        """.trimIndent()
        super.test(S,goal,sentence,expected)
    }
    @Test
    fun aaq() {
        val sentence = "r=aa?;"
        val goal = "S"
        val expected = """
            S { I { G {
              'b'
              I { G {
                  'b'
                  I { V:'a' }
                  'c'
                } }
              'c'
            } } }
        """.trimIndent()
        super.test(S,goal,sentence,expected)
    }
    @Test
    fun baaqc() {
        val sentence = "r=(aa?);"
        val goal = "S"
        val expected = """
            S { I { G {
              'b'
              I { G {
                  'b'
                  I { V:'a' }
                  'c'
                } }
              'c'
            } } }
        """.trimIndent()
        super.test(S,goal,sentence,expected)
    }
    @Test
    fun aq_aqaq_baaqc() {
        val sentence = """
            r=e?;
            e=w?a*;
            s=(s i?);
        """.trimIndent()
        val goal = "S"
        val expected = """
            S { W { "\s+" : ' ' } }
        """.trimIndent()
        super.test(S,goal,sentence,expected)
    }
}