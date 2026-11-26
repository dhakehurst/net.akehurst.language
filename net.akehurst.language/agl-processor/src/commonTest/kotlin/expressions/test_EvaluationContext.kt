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

package net.akehurst.language.expressions.processor

import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** EvaluationContext copies the given values (of/child), or uses them as given until a value is set (ofNoCopy/childNoCopy) */
class test_EvaluationContext {

    private val og = ObjectGraphAccessorMutatorByReflection(typesDomain("test", true) {}, IssueHolder(), LocationMapDefault())
    private fun v(s: String) = og.toTypedObject(s, net.akehurst.language.types.asm.StdLibDefault.String)

    @Test
    fun of_copies_the_given_map() {
        val m = mutableMapOf<String, TypedObject>("a" to v("1"))
        val evc = EvaluationContext.of(m)
        m["b"] = v("2")
        assertNull(evc.getOrInParent("b"))
    }

    @Test
    fun setNamedValue_on_a_no_copy_context_does_not_change_the_given_map() {
        val m = mutableMapOf<String, TypedObject>("a" to v("1"))
        val parent = EvaluationContext.of(emptyMap())
        val evc = parent.childNoCopy(m)
        assertEquals("1", evc.getOrInParent("a")?.self)
        evc.setNamedValue("b", v("2"))
        assertEquals("2", evc.getOrInParent("b")?.self)
        assertEquals(setOf("a"), m.keys)
        assertNull(parent.getOrInParent("b"))
    }

    @Test
    fun setNamedValue_writes_to_the_frame_not_the_parent() {
        val parent = EvaluationContext.of(mapOf("a" to v("1")))
        val child = parent.child()
        child.setNamedValue("a", v("2"))
        assertEquals("2", child.getOrInParent("a")?.self)
        assertEquals("1", parent.getOrInParent("a")?.self)
    }
}
