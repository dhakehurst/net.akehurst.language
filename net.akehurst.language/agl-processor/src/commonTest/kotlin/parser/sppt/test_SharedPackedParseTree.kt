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

package net.akehurst.language.parser.sppt

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.processor.Agl
import kotlin.test.Test
import kotlin.test.assertEquals


class test_SharedPackedParseTree {

    @Test
    fun tokensByLine_a() {
        val proc = Agl.processor("""
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                
                S = expr ;
                expr = VAR < infix ;
                infix = expr '+' expr ;
                VAR = "[a-z]+" ;
            }
        """.trimIndent())

        val sut = proc.parse("""
            a + b
            + c
        """.trimIndent())

        val actual = sut.tokensByLine

        assertEquals("a", actual[0][0].matchedText)
    }

}