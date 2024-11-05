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

package net.akehurst.language.parser.leftcorner.multi

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_list_terminated_by_valid_list_content : test_LeftCornerParserAbstract() {

    // skip WS = "\s+"
    // S = L A
    // L = 'a'+
    // A = 'a'
    private companion object {
        val rrs = runtimeRuleSet {
            pattern("WS","\\s+",isSkip = true)
            concatenation("S") { ref("L"); ref("A") }
            multi("L", 1,-1,"'a'")
            literal("A", "a")
            literal( "a")
        }
        val goal = "S"
    }

    @Test
    fun empty() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0,1,1,1),"^",setOf("A","'a'"))
        ),issues.errors)
    }

    @Test
    fun a() {
        val sentence = "a"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt, "${sppt?.toStringAllWithIndent("  ")}")
        assertEquals(listOf(
            parseError(InputLocation(1,2,1,1),"a^",setOf("A","'a'"))
        ),issues.errors)
    }

    @Test
    fun aa_pass() {
        val sentence = "aa"

        val expeccted = """
            S {
              L { 'a' }
              A : 'a'
            } 
            """

        super.test(rrs, goal, sentence, 1, expeccted)

    }

    @Test
    fun a_a_pass() {
        val sentence = "a a"

        val expeccted = """
            S {
              L { 'a' WS : ' ' }
              A : 'a'
            } 
            """

       super.test(rrs, goal, sentence, 1, expeccted)

    }

    @Test
    fun aaa_pass() {
        val sentence = "aaa"

        val expeccted = """
            S {
              L { 'a' 'a' }
              A : 'a'
            } 
            """

        super.test(rrs, goal, sentence, 1, expeccted)
    }

    @Test
    fun a_a_a_pass() {
        val sentence = "a a a"

        val expeccted = """
            S {
              L { 'a' WS:' ' 'a' WS:' '}
              A : 'a'
            } 
            """

        super.test(rrs, goal, sentence, 1, expeccted)

    }

    @Test
    fun aaaa_pass() {
        val sentence = "aaaa"

        val expeccted = """
            S {
              L { 'a' 'a' 'a'}
              A : 'a'
            } 
            """

        super.test(rrs, goal, sentence, 1, expeccted)

    }

}