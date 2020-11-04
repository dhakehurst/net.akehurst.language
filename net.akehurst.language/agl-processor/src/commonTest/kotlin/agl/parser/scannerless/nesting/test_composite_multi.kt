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

package net.akehurst.language.parser.scanondemand.nesting

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals

class  test_composite_multi : test_ScanOnDemandParserAbstract() {

    // S = NUM (',' NUM)* ;
    // NUM = "[0-9]+"

    companion object {

        val S = runtimeRuleSet {
            concatenation("S") { ref("NUM"); ref("cmNumMulti") }
            multi("cmNumMulti",0,-1,"cmNum")
            concatenation("cmNum") { ref("CMR"); ref("NUM"); }
            literal("CMR",",")
            pattern("NUM","[0-9]+")
        }
    }
    @Test
    fun _1() {
        val rrs = S
        val goal = "S"
        val sentence = "1"

        val expected = """
            S {
                NUM : '1'
                cmNumMulti|1 { §empty }
            }
        """.trimIndent()

        val actual = super.test(rrs, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }

    @Test
    fun _1c2() {
        val rrs = S
        val goal = "S"
        val sentence = "1,2"

        val expected = """
            S {
                NUM : '1'
                cmNumMulti { cmNum {
                  CMR:','
                  NUM : '2'
                } }
            }
        """.trimIndent()

        val actual = super.test(rrs, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }


}