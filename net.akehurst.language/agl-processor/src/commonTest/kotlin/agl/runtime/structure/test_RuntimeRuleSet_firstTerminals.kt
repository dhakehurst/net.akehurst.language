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

package net.akehurst.language.agl.runtime.structure

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals

class test_RuntimeRuleSet_firstTerminals {

    @Test
    fun firstTerminals() {
        val rb = RuntimeRuleSetBuilder()
        val A = rb.literal("A")
        val a = rb.literal("a")
        val B = rb.literal("B")
        val b = rb.literal("b")
        val Aa = rb.rule("Aa").concatenation(A, a)
        val Bb = rb.rule("Bb").concatenation(B, b)
        val r = rb.rule("r").choice(RuntimeRuleChoiceKind.PRIORITY_LONGEST, Aa, Bb)

        val sut = rb.ruleSet()
        val actual = sut.firstTerminals[r.number]
        val expected = setOf(A, B)

        assertEquals(expected, actual)
    }


}