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
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_LeftCornerParserAbstract
import kotlin.test.Test

internal class test_typeDefs : test_LeftCornerParserAbstract() {

    // S = type name ;
    // type = userDefined < builtIn;
    // builtIn = 'int' | 'bool' ;
    // userDefined = "[a-zA-Z]+" ;
    // name = "[a-zA-Z]+" ;
    // WS = "\s+" ;

    private val rrs = runtimeRuleSet {
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

        super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 2, //TODO: can we make this 1 anyhow?
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun bool_a() {
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

        super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 2, //TODO: can we make this 1 anyhow?
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun A_a() {
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

        super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun int_int() {

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

        super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 2, //TODO: can we make this 1 anyhow?
                expectedTrees = arrayOf(expected)
        )
    }


    @Test
    fun A_int() {
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

        super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }


}