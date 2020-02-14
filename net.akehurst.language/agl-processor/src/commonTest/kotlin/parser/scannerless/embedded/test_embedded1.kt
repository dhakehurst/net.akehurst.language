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

package net.akehurst.language.parser.scannerless.embedded

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_embedded1 : test_ScannerlessParserAbstract() {

    // B = b ;
    val B = runtimeRuleSet {
        concatenation("B") { ref("b") }
        literal("b", "b")
    }
    // S = a gB a ;
    // gB = grammar B ;
    val S = runtimeRuleSet {
        concatenation("S") { ref("a"); ref("gB"); ref("a"); }
        literal("a", "a")
        embedded("gB", B)
    }

    @Test
    fun empty_fails() {
        val rrb = this.S
        val goal = "S"
        val sentence = ""

//        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
//        }
//        assertEquals(1, ex.location.line)
//        assertEquals(0, ex.location.column)
    }

    @Test
    fun ambiguous_a() {
        val rrb = this.S
        val goal = "S"
        val sentence = "a"

        val expected1 = """
            S {
              'a'
            }
        """.trimIndent()

        val expected2 = """
         S { S1 {
            'a'
            bOpt { §empty }
          } }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected1, expected2)

    }


    @Test
    fun deterministic_empty_fails() {
        val rrb = this.S
        val goal = "S"
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(0, ex.location.column)
    }


}