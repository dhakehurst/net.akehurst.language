/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.syntaxAnalyser

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class test_SyntaxAnalysisStream {

    private companion object {
        val grammar = """
            namespace test
            grammar Test {
              skip leaf WS = "\s+" ;
              unit = decl* ;
              decl = 'var' ID ':' type '=' value ';' ;
              type = ID ;
              value = NUM | ID | list ;
              list = '[' [value / ',']* ']' ;
              leaf ID = "[a-z]+" ;
              leaf NUM = "[0-9]+" ;
            }
        """.trimIndent()

        val processor = Agl.processorFromStringSimple(GrammarString(grammar)).processor!!

        /** streams the sentence through the parser into the syntax analyser */
        fun streamed(sentence: String): SyntaxAnalysisStream<Asm, Any> {
            @Suppress("UNCHECKED_CAST")
            val sa = processor.syntaxAnalyser as SyntaxAnalyserFromAsmTransformAbstract<Asm, Any>
            sa.clear<Asm>()
            val stream = SyntaxAnalysisStream(sa)
            processor.parser!!.parse(sentence, ParseOptionsDefault(goalRuleName = "unit", streamListener = stream))
            processor.parser!!.reset()
            return stream
        }

        fun test(sentence: String) {
            val stream = streamed(sentence)
            val sr = stream.result
            assertNotNull(sr)
            val streamedAsm = sr.asm
            assertNotNull(streamedAsm, sr.issues.toString())
            val streamedText = streamedAsm.asString()
            val streamedRoots = streamedAsm.root.toList()

            val pr = processor.process(sentence)
            assertTrue(pr.allIssues.errors.isEmpty(), pr.allIssues.toString())
            val treeAsm = pr.asm!!
            assertEquals(treeAsm.asString(), streamedText)
            // identities come from the parse paths, set when the parse finishes
            assertEquals(treeAsm.root, streamedRoots)
        }
    }

    @Test
    fun empty() = test("")

    @Test
    fun one_decl() = test("var a : t = 1 ;")

    @Test
    fun lists() = test("var a : t = [1, x, [2, 3]] ; var b : u = [] ; var c : v = q ;")

    @Test
    fun parse_error_gives_no_asm() {
        val stream = streamed("var a : t = 1 ; var")
        val sr = stream.result
        assertNotNull(sr)
        assertNull(sr.asm)
    }
}
