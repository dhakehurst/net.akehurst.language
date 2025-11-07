/**
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.processor.githubIssues.issue57

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.parser.api.RulePosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_Issue57 {

    private companion object {
        val grammarString = """
            namespace org.example.brackets.agl

            grammar Brackets {
            	sentence = '[' 'x' ']';
            	
            	skip WHITESPACE = "\s+";
            }
        """.trimIndent()

        fun doTest(sentence: String, position: Int, expected: Set<String>) {
            println("=== sentence: '$sentence' position: $position ===")
            val p = Agl.processorFromStringSimple(
                GrammarString(grammarString)
            ).let {
                check(it.issues.errors.isEmpty()) { it.issues.toString() }
                it.processor!!
            }
            val res = p.expectedItemsAt(sentence, position)
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())

            res.items.forEach {
                println("${it.kind}: '${it.text}' (${it.label})")
            }
            val actual = res.items.map { it.text }.toSet()

            assertEquals(expected, actual)
        }

        data class TestData(
            val sentence: String,
            val position: Int,
            val expected: Set<String>
        )

        val testData = listOf(
            TestData("", 0, setOf("[ x ]", "[")),
            TestData("[", 0, setOf("[ x ]", "[")),
            TestData("[", 1, setOf("[ x ]", "[")),
            TestData("[ ", 2, setOf("x")),
            TestData("[ x", 3, setOf("x")),
            TestData("[ x ", 4, setOf("]")),
            TestData("[ x ]", 5, setOf("]")),
            TestData("[ x ] ", 6, setOf()),
        )
    }

    @Test
    fun test() {
        for (data in testData) {
            doTest(data.sentence, data.position, data.expected)
        }
    }

    @Test
    fun test2() {
        val p = Agl.processorFromStringSimple(
            GrammarString("""
                namespace org.example.bracketscomplex.agl
                
                grammar BracketsComplex {
                    sentence = word | bracketed;
                    bracketed = '[' word* ']';
                    word = NAME;
                    
                    leaf NAME = NAME_CHAR+; //'12345'; //NAME_CHAR+;
                    leaf NAME_CHAR = "[-.0-9]";
                    
                    skip WHITESPACE = "\s+";
                }
                """.trimIndent())
        ).let {
            check(it.issues.errors.isEmpty()) { it.issues.toString() }
            it.processor!!
        }

        val sentence = "[ 12345 ] "
        for(pos in 4 until 11) {
            println("=== sentence: '$sentence' position: $pos ===")
            val res = p.expectedItemsAt(sentence, pos)
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())

            res.items.forEach {
                println("${it.kind}: '${it.text}' (${it.label})")
            }
        }

    }

    @Test
    fun test_answer_1() {
        val p = Agl.processorFromStringSimple(
            GrammarString("""
                    namespace org.example.brackets.agl
        
                    grammar Brackets {
                        sentence = '[' 'uvwxyz' ']';
                        
                        skip WHITESPACE = "\s+";
                    }
                """.trimIndent())
        ).let {
            check(it.issues.errors.isEmpty()) { it.issues.toString() }
            it.processor!!
        }
        val res = p.expectedItemsAt("[ uvwxyz ", 8)
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())

        res.items.forEach {
            println("${it.kind}: '${it.text}' (${it.label})")
        }


    }
}