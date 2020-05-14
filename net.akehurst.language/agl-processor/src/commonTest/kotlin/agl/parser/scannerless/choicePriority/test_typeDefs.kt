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

package net.akehurst.language.parser.scanondemand.choicePriority

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

class test_typeDefs : test_ScanOnDemandParserAbstract() {

    // S = type name ;
    // type = userDefined < builtIn;
    // builtIn = 'int' | 'bool' ;
    // userDefined = "[a-zA-Z]+" ;
    // name = "[a-zA-Z]+" ;
    // WS = "\s+" ;
    private fun typeDefs(): RuntimeRuleSetBuilder {
        val b = RuntimeRuleSetBuilder()
        val r_azAZ = b.pattern("[a-zA-Z]+")
        val r_name = b.rule("name").concatenation(r_azAZ)
        val r_userDefined = b.rule("userDefined").concatenation(r_azAZ)
        val r_builtIn = b.rule("builtIn").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, b.literal("int"), b.literal("bool"))
        val r_type = b.rule("type").choice(RuntimeRuleChoiceKind.PRIORITY_LONGEST, r_userDefined, r_builtIn)
        b.rule("S").concatenation(r_type, r_name)
        b.pattern("WS","\\s+", isSkip = true)
        return b
    }

    val S = runtimeRuleSet {
        concatenation("S") { ref("type"); ref("name") }
        choice("type",RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
            ref("userDefined")
            ref("builtIn")
        }
        choice("builtIn", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            literal("int")
            literal("bool")
        }
        concatenation("userDefined") { ref("ID") }
        concatenation("name") { ref("ID") }
        pattern("ID", "[a-zA-Z]+")
        pattern("WS", "\\s+", true)
    }


    @Test
    fun int_a() {
        val rrb = this.S
        val goal = "S"
        val sentence = "int a"

        val expected = """
            S {
              type|1 {
                builtIn { 'int' WS : ' '  }
              }
              name {
                ID : 'a'
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun bool_a() {
        val rrb = this.S
        val goal = "S"
        val sentence = "bool a"

        val expected = """
            S {
              type|1 {
                builtIn|1 { 'bool' WS  : ' '  }
              }
              name {
                ID : 'a'
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun A_a() {
        val rrb = this.S
        val goal = "S"
        val sentence = "A a"

        val expected = """
            S {
              type {
                userDefined { ID : 'A' WS : ' '  }
              }
              name {
                ID : 'a'
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun int_int() {
        val rrb = this.S
        val goal = "S"
        val sentence = "int int"

        val expected = """
            S {
              type|1 {
                builtIn { 'int' WS:' ' }
              }
              name {
                ID : 'int'
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }


    @Test
    fun A_int() {
        val rrb = this.S
        val goal = "S"
        val sentence = "A int"

        val expected = """
            S {
              type {
                userDefined { ID : 'A' WS:' ' }
              }
              name {
                ID : 'int'
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }


}