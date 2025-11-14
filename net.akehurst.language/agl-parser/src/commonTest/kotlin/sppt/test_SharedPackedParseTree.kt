/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.sppt

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.api.ParseResult
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sentence.common.SentenceDefault
import net.akehurst.language.sppt.api.LeafData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class test_SharedPackedParseTree {

    private companion object {

        fun parse(rrs:RuntimeRuleSet, goal:String, sentence: Sentence): ParseResult {
            val scaner = ScannerOnDemand(RegexEnginePlatform, rrs.terminals)
            val parser = LeftCornerParser(scaner, rrs)
            return parser.parse(sentence.text, ParseOptionsDefault(goalRuleName = goal))
        }
    }

    @Test
    fun tokensByLine_a() {
          //  namespace test
          //  grammar Test {
          //      skip WS = "\s+" ;
          //
          //      S = expr ;
          //      expr = VAR < infix ;
          //      infix = expr '+' expr ;
          //      VAR = "[a-z]+" ;
          //  }
        val rrs = runtimeRuleSet("test.Test") {
            pattern("WS", "(\\s|[.])+", true)
            pattern("COMMENT", "/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/", true)

            concatenation("S") { ref("expr") }
            choice("expr",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("VAR")
                ref("infix")
            }
            concatenation("infix") { ref("expr"); literal("+"); ref("expr") }
            pattern("VAR", "[a-z]+")
        }
        val text = "a"
        val result = parse(rrs, "S",SentenceDefault(text, null))

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        val actual = result.sppt!!.tokensByLine(0)

        assertEquals("a", SentenceDefault(text, null).textAt(actual[0].position,actual[0].length))
    }

    @Test
    fun tokensByLine_eolx1() {
        //    namespace test
        //    grammar Test {
        //        skip leaf WS = "\s+" ;
        //
        //        S = expr ;
        //        expr = VAR < infix ;
        //        infix = expr '+' expr ;
        //        leaf VAR = "[a-z]+" ;
        //    }
        val rrs = runtimeRuleSet("test.Test") {
            pattern("WS", "(\\s|[.])+", true)
            pattern("COMMENT", "/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/", true)

            concatenation("S") { ref("expr") }
            choice("expr",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("VAR")
                ref("infix")
            }
            concatenation("infix") { ref("expr"); literal("+"); ref("expr") }
            pattern("VAR", "[a-z]+")
        }

        val text = """
            a + b
            + c
        """.trimIndent()
        val result = parse(rrs, "S",SentenceDefault(text, null))

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        val actual = listOf(
            result.sppt!!.tokensByLine(0),
            result.sppt!!.tokensByLine(1)
        )

        val expected_1 = listOf(
            LeafData("VAR", true, 0, 1, listOf("S", "expr", "infix", "expr", "infix", "expr", "VAR")),
            LeafData("WS", true, 1, 1, listOf("S", "expr", "infix", "expr", "infix", "expr", "WS")),
            LeafData("'+'", false, 2, 1, listOf("S", "expr", "infix", "expr", "infix", "'+'")),
            LeafData("WS", true, 3, 1, listOf("S", "expr", "infix", "expr", "infix", "WS")),
            LeafData("VAR", true, 4, 1, listOf("S", "expr", "infix", "expr", "infix", "expr", "VAR")),
            LeafData("WS", true, 5, 1, listOf("S", "expr", "infix", "expr", "infix", "expr", "WS")),
        )
        val expected_2 = listOf(
            LeafData("'+'", false, 6, 1, listOf("S", "expr", "infix", "'+'")),
            LeafData("WS", true, 7, 1, listOf("S", "expr", "infix", "WS")),
            LeafData("VAR", true, 8, 1, listOf("S", "expr", "infix", "expr", "VAR")),
        )


        assertEquals(expected_1, actual[0])
        assertEquals(expected_2, actual[1])
    }

    @Test
    fun tokensByLine_eolx1_indent() {
        //    namespace test
        //    grammar Test {
        //        skip leaf WS = "\s+" ;
        //
        //        S = expr ;
        //        expr = VAR < infix ;
        //        infix = expr '+' expr ;
        //        leaf VAR = "[a-z]+" ;
        //    }
        val rrs = runtimeRuleSet("test.Test") {
            pattern("WS", "(\\s|[.])+", true)
            pattern("COMMENT", "/\\*[^*]*\\*+(?:[^*/][^*]*\\*+)*/", true)

            concatenation("S") { ref("expr") }
            choice("expr",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("VAR")
                ref("infix")
            }
            concatenation("infix") { ref("expr"); literal("+"); ref("expr") }
            pattern("VAR", "[a-z]+")
        }

        val sentence = SentenceDefault(
            """
            a + b
              + c
        """.trimIndent(),
            null
        )
        val result = parse(rrs, "S",sentence)

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        val actual = listOf(
            result.sppt!!.tokensByLine(0),
            result.sppt!!.tokensByLine(1)
        )

        assertEquals("a + b\n", actual[0].map { sentence.textAt(it.position,it.length) }.joinToString(""))
//        assertEquals(1, actual[0][0].location.line)
//        assertEquals(2, actual[1][0].location.line)
        assertEquals("  ", sentence.textAt(actual[1][0].position, actual[1][0].length))
    }

    @Test
    fun tokensByLine_eolx2() {
        //    namespace test
        //    grammar Test {
        //        skip WS = "\s+" ;
        //
        //        declaration = 'class' ID '{' property* '}' ;
        //        property = ID ':' typeReference ;
        //        typeReference = ID typeArguments? ;
        //        typeArguments = '<' [typeReference / ',']+ '>' ;
        //
        //        leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
        //    }

        val rrs = runtimeRuleSet("test.Test") {
            TODO()
        }

        val text = """
class XXX {
    
    prop1 : String
    prop2 : Integer
    propList : List<String>
}
        """
        val text2 = text.trimStart()
        val sentence = SentenceDefault(text2, null)
        val result = parse(rrs, "declaration", sentence)

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        val actual = listOf(
            result.sppt!!.tokensByLine(0),
            result.sppt!!.tokensByLine(1),
            result.sppt!!.tokensByLine(2)
        )

        assertEquals("class XXX {\n", actual[0].map { sentence.textAt(it.position,it.length) }.joinToString(""))
//        assertEquals(1, actual[0][0].location.line)
//        assertEquals(2, actual[1][0].location.line)
//        assertEquals("    \n", actual[1][0].matchedText(sentence))
//        assertEquals(3, actual[2][0].location.line)
        assertEquals("    ", sentence.textAt(actual[2][0].position, actual[2][0].length))
        assertEquals("prop1", sentence.textAt(actual[2][1].position, actual[2][1].length))

    }


}