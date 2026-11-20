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
import net.akehurst.language.api.processor.AsmTransformString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.TypesString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * List.foldRight in an asm-transform: nested objects made from a (separated) list, e.g. 'x, y, z' as N(x, N(y, z))
 */
class test_ProvidedTransform_foldRight {

    @Test
    fun nested_objects_from_a_separated_list() {
        val grammar = """
            namespace test
              grammar Test {
                S = [a / ',']+ ;
                leaf a = "[a-z]" ;
              }
        """.trimIndent()
        val types = """
            namespace test.model
              data N {
                v: String
                n: Any?
              }
        """.trimIndent()
        val transform = """
            namespace test
              asm-transform Test {
                #override-default-transform
                import-types test.model
                S: children.items.front.foldRight(children.items.last, { e, acc -> N() { v := e  n := acc } }) as Any
              }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(GrammarString(grammar), typeStr = TypesString(types), transformStr = AsmTransformString(transform))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val proc = result.processor!!
        val processed = proc.process("x,y,z")
        assertTrue(processed.allIssues.errors.isEmpty(), processed.allIssues.toString())
        val expected = """
            :N {
              v = 'x'
              n = :N {
                v = 'y'
                n = 'z'
              }
            }
        """.trimIndent()
        assertEquals(expected, processed.asm!!.asString().trim())
    }
}
