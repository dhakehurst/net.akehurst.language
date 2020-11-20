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

package net.akehurst.language.parser.scanondemand.multi

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_multi01_x2_nested : test_ScanOnDemandParserAbstract() {

    // S = AB V 'd'
    // AB = A B
    // A = 'a'?
    // B = 'b'?
    // V = "[a-c]"
    private val S = runtimeRuleSet {
        concatenation("S") { ref("AB"); ref("V"); literal("d") }
        concatenation("AB") { ref("A"); ref("B") }
        multi("A",0,1,"'a'")
        literal("'a'","a")
        multi("B",0,1,"'b'")
        literal("'b'","b")
        pattern("V","[a-c]")
    }

    @Test
    fun empty_fails() {
        val rrb = this.S
        val goal = "S"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }

        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
        assertEquals(setOf("'a'","'b'","V"), e.expected)
    }

    @Test
    fun abcd() {
        val rrb = this.S
        val goal = "S"
        val sentence = "abcd"

        val expected = """
            S {
              AB {
                A { 'a' }
                B { 'b' }
              }
              V:'c'
              'd'
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }

    @Test
    fun acd() {
        val rrb = this.S
        val goal = "S"
        val sentence = "acd"

        val expected = """
            S {
              AB {
                A { 'a' }
                B|1 { §empty }
              }
              V:'c'
              'd'
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected)
    }


}