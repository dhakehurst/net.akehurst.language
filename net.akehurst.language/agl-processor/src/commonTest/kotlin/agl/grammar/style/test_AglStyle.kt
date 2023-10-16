/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.grammar.style

import net.akehurst.language.agl.grammar.grammar.ContextFromGrammar
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class test_AglStyle {

    private companion object {
        val aglProc = Agl.registry.agl.style.processor!!
        val testGrammar = Agl.registry.agl.grammar.processor?.process(
            """
            namespace test
            
            grammar Test {
                skip WS = "\s+" ;
            
                unit = declaration* ;
                declaration = datatype | primitive ;
                primitive = 'primitive' ID ;
                datatype = 'datatype' ID '{' property* '}' ;
                property = ID ':' typeReference ;
                typeReference = type typeArguments? ;
                typeArguments = '<' [typeReference / ',']+ '>' ;
            
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
                leaf type = ID;
            
                // not marked as leaf so can use to test patterns
                COMMENT = "\"(\\?.)*\"" ;
                INT = "[0-9]+" ;
            }
        """.trimIndent()
        )?.asm?.first()!!

        fun process(sentence: String) = aglProc.process(
            sentence,
            Agl.options {
                semanticAnalysis { context(ContextFromGrammar(testGrammar)) }
            })
    }


    @Test
    fun single_line_comment() {

        val text = """
            // single line comment
        """.trimIndent()

        val result = process(text)

        assertNotNull(result.asm)
        assertEquals(0, result.asm?.rules?.size)

        assertEquals(0, result.issues.size, result.issues.joinToString("\n") { "$it" })
    }

    @Test
    fun multi_line_comment() {

        val text = """
            /* multi
               line
               comment
            */
        """.trimIndent()

        val result = process(text)

        assertNotNull(result.asm)
        assertEquals(0, result.asm?.rules?.size)
        assertEquals(0, result.issues.size, result.issues.joinToString("\n") { "$it" })
    }

    @Test
    fun selector_notFound() {

        val text = """
            xxx { }
        """.trimIndent()

        val result = process(text)

        assertNotNull(result.asm)
        assertEquals(1, result.asm?.rules?.size)
        assertEquals(
            setOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(0, 1, 1, 3), "Grammar Rule 'xxx' not found for style rule", null)
            ), result.issues.all
        )
    }

    @Test
    fun emptyRule() {

        val text = """
            declaration { }
        """.trimIndent()

        val result = process(text)

        assertNotNull(result.asm)
        assertEquals(1, result.asm?.rules?.size)
        assertEquals(0, result.issues.size, result.issues.joinToString("\n") { "$it" })
    }

    @Test
    fun oneLeafRule() {

        val text = """
            ID {
                -fx-fill: green;
                -fx-font-weight: bold;
            }
        """.trimIndent()

        val result = process(text)

        assertNotNull(result.asm)
        assertEquals(1, result.asm?.rules?.size)
        assertEquals("ID", result.asm!!.rules[0].selector.first().value)
        assertEquals(2, result.asm!!.rules[0].styles.size)
        assertEquals(0, result.issues.size, result.issues.joinToString("\n") { "$it" })
    }

    @Test
    fun multiLeafRules() {

        val text = """
            ID {
                -fx-fill: green;
                -fx-font-weight: bold;
            }
            "[0-9]+" {
                -fx-fill: green;
                -fx-font-weight: bold;
            }
        """.trimIndent()

        val result = process(text)

        assertNotNull(result.asm)
        assertEquals(2, result.asm?.rules?.size)
        assertEquals(0, result.issues.size, result.issues.joinToString("\n") { "$it" })
    }

    @Test
    fun regexWithQuotes() {
        val text = """
            "\"(\\?.)*\"" {
              font-family: "Courier New";
              color: darkblue;
            }
        """.trimIndent()

        val result = process(text)

        assertNotNull(result.asm)
        assertEquals(1, result.asm?.rules?.size)
        assertEquals(0, result.issues.size, result.issues.joinToString("\n") { "$it" })
    }

    @Test
    fun selectorAndComposition() {

        val text = """
            property,typeReference,typeArguments { }
        """.trimIndent()

        val result = process(text)

        assertNotNull(result.asm)
        assertEquals(1, result.asm?.rules?.size)
        assertEquals(0, result.issues.size, result.issues.joinToString("\n") { "$it" })
    }
    //TODO more tests


    @Test
    fun dot() {

        val text = """
C_PREPROCESSOR {
  foreground: gray;
  font-style: italic;
}
SINGLE_LINE_COMMENT {
  foreground: DarkSlateGrey;
  font-style: italic;
}
MULTI_LINE_COMMENT {
  foreground: DarkSlateGrey;
  font-style: italic;
}
STRICT {
  foreground: purple;
  font-style: bold;
}
GRAPH {
  foreground: purple;
  font-style: bold;
}
DIGRAPH {
  foreground: purple;
  font-style: bold;
}
SUBGRAPH {
  foreground: purple;
  font-style: bold;
}
NODE {
  foreground: purple;
  font-style: bold;
}
EDGE {
  foreground: purple;
  font-style: bold;
}
ALPHABETIC_ID {
  foreground: red;
  font-style: italic;
}
HTML {
  background: LemonChiffon;
}
NAME {
    foreground: green;
}
        """.trimIndent()

        val result = process(text)

        assertNotNull(result.asm)
        assertEquals(12, result.asm?.rules?.size)
        assertEquals(0, result.issues.size, result.issues.joinToString("\n") { "$it" })
    }
}
