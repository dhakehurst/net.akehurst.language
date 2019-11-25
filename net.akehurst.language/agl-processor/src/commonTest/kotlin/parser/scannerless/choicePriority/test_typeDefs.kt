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

package net.akehurst.language.parser.scannerless.choicePriority

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test

class test_typeDefs : test_ScannerlessParserAbstract() {

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
        val r_builtIn = b.rule("builtIn").choiceEqual(b.literal("int"), b.literal("bool"))
        val r_type = b.rule("type").choicePriority( r_userDefined, r_builtIn)
        b.rule("S").concatenation(r_type, r_name)
        b.rule("WS").skip(true).concatenation(b.pattern("\\s+"))
        return b
    }

    @Test
    fun int_a() {
        val rrb = this.typeDefs()
        val goal = "S"
        val sentence = "int a"

        val expected = """
            S {
              type {
                builtIn { 'int' WS { '\s+' : ' ' } }
              }
              name {
                '[a-zA-Z]+' : 'a'
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun bool_a() {
        val rrb = this.typeDefs()
        val goal = "S"
        val sentence = "bool a"

        val expected = """
            S {
              type {
                builtIn { 'bool' WS { '\s+' : ' ' } }
              }
              name {
                '[a-zA-Z]+' : 'a'
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun A_a() {
        val rrb = this.typeDefs()
        val goal = "S"
        val sentence = "A a"

        val expected = """
            S {
              type {
                userDefined { '[a-zA-Z]+' : 'A' WS { '\s+' : ' ' } }
              }
              name {
                '[a-zA-Z]+' : 'a'
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun int_int() {
        val rrb = this.typeDefs()
        val goal = "S"
        val sentence = "int int"

        val expected = """
            S {
              type {
                builtIn { 'int' WS { '\s+' : ' ' } }
              }
              name {
                '[a-zA-Z]+' : 'int'
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }


    @Test
    fun A_int() {
        val rrb = this.typeDefs()
        val goal = "S"
        val sentence = "A int"

        val expected = """
            S {
              type {
                userDefined { '[a-zA-Z]+' : 'A' WS { '\s+' : ' ' } }
              }
              name {
                '[a-zA-Z]+' : 'int'
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }


}