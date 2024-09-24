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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.grammar.processor.AglGrammar
import kotlin.test.Test
import kotlin.test.assertEquals

class test_Agl {

    @Test
    fun parser_grammarDefinitionStr1() {
        val grammarStr = """
            namespace test
            grammar Test {
              a = 'a';
            }
        """.trimIndent()
        val p = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
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
        val p = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
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
        val p = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
        p.parse("b", Agl.parseOptions { goalRuleName("b") })
    }

    @Test
    fun process() {
        val grammarStr = """
            namespace test

            grammar Test {
              a = "[a-z]" ;
            }
        """.trimIndent()
        val sentence = "a"
        val myProcessor = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
        val result = myProcessor.process(sentence)
        //TODO
    }

    @Test
    fun grammar_styleStr() {
        val actual = Agl.registry.agl.grammar.styleStr
        // there is a default '$nostyle' added
        val expected = """
namespace test
${'$'}nostyle {
  foreground: black;
  background: white;
  font-style: normal;
}
${AglGrammar.styleStr}
        """.trimIndent()

        assertEquals(expected, actual)

    }

}