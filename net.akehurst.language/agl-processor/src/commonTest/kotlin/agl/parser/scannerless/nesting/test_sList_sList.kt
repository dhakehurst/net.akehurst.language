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

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_LeftCornerParserAbstract
import kotlin.test.Test

internal class test_sList_sList : test_LeftCornerParserAbstract() {

    // S = [numList / ';']* ;
    // numList = [NUM / ',']+
    // NUM = "[0-9]+"

    private companion object {
        val rrs = runtimeRuleSet {
            sList("S", 0, -1, "numList", "SMI")
            sList("numList", 1, -1, "NUM", "CMR")
            literal("SMI", ";")
            literal("CMR", ",")
            pattern("NUM", "[0-9]+")
        }
    }

    @Test
    fun _1() {
        val goal = "S"
        val sentence = "1"

        val expected = """
            S { numList { NUM : '1' } }
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
    fun _1s2() {
        val goal = "S"
        val sentence = "1;2"

        val expected = """
            S { 
                numList { NUM : '1' }
                SMI:';'
                 numList { NUM : '2' }
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
    fun _1c2() {
        val goal = "S"
        val sentence = "1,2"

        val expected = """
            S { 
                numList { NUM : '1' CMR:','  NUM : '2'  }
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