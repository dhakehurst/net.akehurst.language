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

package net.akehurst.language.agl.processor

import net.akehurst.language.agl.language.grammar.AglGrammarGrammar
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_Agl {

    @Test
    fun parser_grammarDefinitionStr1() {
        val grammarStr = """
            namespace test
            grammar Test {
              a = 'a';
            }
        """.trimIndent()
        val p = Agl.processorFromStringDefault(grammarStr).processor!!
        p.parse("a", Agl.parseOptions { goalRuleName("a") })
    }

    @Test
    fun parser_grammarDefinitionStr2_1() {
        val grammarStr = """
            namespace test
            grammar Test {
              a = 'a';
              b = 'b';
            }
        """.trimIndent()
        val p = Agl.processorFromStringDefault(grammarStr).processor!!
        p.parse("a", Agl.parseOptions { goalRuleName("a") })
    }

    @Test
    fun parser_grammarDefinitionStr2_b() {
        val grammarStr = """
            namespace test
            grammar Test {
              a = 'a';
              b = 'b';
            }
        """.trimIndent()
        val p = Agl.processorFromStringDefault(grammarStr).processor!!
        p.parse("b", Agl.parseOptions { goalRuleName("b") })
    }

    @Test
    fun process() {
        val grammarStr = """
            namespace test

            grammar test {
              a = "[a-z]" ;
            }
        """.trimIndent()
        val sentence = "a"
        val myProcessor = Agl.processorFromStringDefault(grammarStr).processor!!
        val result = myProcessor.process(sentence)
        //TODO
    }

    @Test
    fun grammar_styleStr() {
        val actual = Agl.registry.agl.grammar.styleStr
        val expected = AglGrammarGrammar.styleStr

        assertEquals(expected, actual)

    }

}