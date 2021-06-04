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

package net.akehurst.language.agl.grammar.grammar

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyserItem
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyserItemKind
import kotlin.test.Test
import kotlin.test.assertEquals


class test_AglGrammarSemanticAnalyser {

    @Test
    fun nonTerminalNotFound() {
        val grammarStr = """
            namespace test
            grammar Test {
                a = b ;
            }
        """.trimIndent()
        //val proc = Agl.processor(grammarStr)
        val actual = Agl.grammarProcessor.analyseText(List::class, grammarStr)
        val expected = listOf(
                SemanticAnalyserItem(SemanticAnalyserItemKind.ERROR, InputLocation(38, 9, 3, 2), "Rule 'b' not found in grammar 'Test'")
        )
        assertEquals(expected, actual)
    }

    @Test
    fun duplicateRule() {
        //TODO: test with grammar extends
        val grammarStr = """
            namespace test
            grammar Test {
                a = b ;
                b = 'a' ;
                b = 'b' ;
            }
        """.trimIndent()
        //val proc = Agl.processor(grammarStr)
        val actual = Agl.grammarProcessor.analyseText(List::class, grammarStr)
        val expected = listOf(
                SemanticAnalyserItem(SemanticAnalyserItemKind.ERROR, InputLocation(38, 9, 3, 2), "More than one rule named 'b' in grammar 'Test', have you remembered the 'override' modifier")
        )
        assertEquals(expected, actual)
    }

    @Test
    fun ambiguity() {
        val grammarStr = """
            namespace test
            grammar Test {
                a = b1 | b2 ;
                b1 = 'b' ;
                b2 = 'b' ;
            }
        """.trimIndent()
        //val proc = Agl.processor(grammarStr)
        val actual = Agl.grammarProcessor.analyseText(List::class, grammarStr)
        val expected = listOf(
                SemanticAnalyserItem(SemanticAnalyserItemKind.WARNING, InputLocation(57, 10, 4, 4), "Ambiguity on [<EOT>] with b2"),
                SemanticAnalyserItem(SemanticAnalyserItemKind.WARNING, InputLocation(72, 10, 5, 4), "Ambiguity on [<EOT>] with b1")
        )
        actual.forEach {
            println(it)
        }
        assertEquals(expected, actual)
    }


}