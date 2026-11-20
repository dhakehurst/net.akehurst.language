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
package net.akehurst.language.agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The special values '$owner', '$container' and '$root' in 'identify ... by':
 * '$container' is the element that contains the element in the ASM, '$owner' the element that owns the scope it is in, '$root' the root element.
 */
class test_SemanticAnalyserSimple_identifySpecialValues {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = ID member* ;
                member = block | def | use ;
                block = 'block' ID '{' wrap* '}' ;
                wrap = 'wrap' ID '{' member* '}' ;
                def = 'def' ID ;
                use = 'use' ID ;
                leaf ID = "[a-z]+" ;
            }
            """.trimIndent()
        )

        fun errorsFor(references: String, sentence: String): List<String> {
            val res = Agl.processorFromStringSimple(grammarStr, referenceStr = CrossReferenceString(references))
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            return res.processor!!.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
                .allIssues.errors.map { it.message }
        }

        fun refs(identifyDef: String) = """
            namespace test.Test
                identify Block by id
                scope Block { $identifyDef }
                references {
                    in Use { property id refers-to Def }
                }
        """.trimIndent()
    }

    @Test
    fun without_special_value() {
        // 'def x' is named 'x' in the scope of block b
        assertEquals(emptyList(), errorsFor(refs("identify Def by id"), "u block b { wrap w { def x use x } }"))
    }

    @Test
    fun owner() {
        // named by its owner (the block): 'def x' in block 'b' is named 'b'
        assertEquals(emptyList(), errorsFor(refs("identify Def by ${'$'}owner.id"), "u block b { wrap w { def x use b } }"))
        val errors = errorsFor(refs("identify Def by ${'$'}owner.id"), "u block b { wrap w { def x use x } }")
        assertEquals(1, errors.size, errors.toString())
        assertTrue(errors[0].contains("'x'"), errors.toString())
    }

    @Test
    fun container() {
        // named by its container (the wrap, which has no scope): 'def x' in wrap 'w' is named 'w'
        assertEquals(emptyList(), errorsFor(refs("identify Def by ${'$'}container.id"), "u block b { wrap w { def x use w } }"))
    }

    @Test
    fun root() {
        // named by the root element: 'def x' in sentence 'u ...' is named 'u'
        assertEquals(emptyList(), errorsFor(refs("identify Def by ${'$'}root.id"), "u block b { wrap w { def x use u } }"))
    }
}
