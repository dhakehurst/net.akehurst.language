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

class  test_a : test_ScanOnDemandParserAbstract() {

    // S = A D C ;
    // A = a b c ;
    // D = d a e ;
    // F = f g a ;

    companion object {

        val S = runtimeRuleSet {
            concatenation("S") { literal("a"); literal("a"); literal("a") }
        }
    }
    @Test
    fun a() {
        val rrs = S
        val goal = "S"
        val sentence = "aaa"

        val expected = """
            S { 'a' 'a' 'a' }
        """.trimIndent()

        val actual = super.test(rrs, goal, sentence, expected)
        assertEquals(1, actual.maxNumHeads)
    }


}