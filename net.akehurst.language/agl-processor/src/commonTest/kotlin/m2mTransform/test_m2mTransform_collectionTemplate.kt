/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.m2mTransform.processor

import net.akehurst.kotlinx.utils.Indent
import net.akehurst.language.agl.Agl
import net.akehurst.language.m2mTransform.api.CollectionTemplate
import net.akehurst.language.m2mTransform.processor.M2mTransformInterpreterHelpers.collectionTemplateMatches
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * collection templates: gaps ('...', 'name:...') anywhere, list order, each member a different element (review MT-Q15..MT-Q20)
 */
class test_m2mTransform_collectionTemplate {

    private companion object {
        fun template(tpl: String): CollectionTemplate {
            val text = "namespace example\ntransform T(s:S, d:D) {\n top mapping R { domain s x:X { p == $tpl } domain d y:Y := Y() }\n}"
            val proc = Agl.registry.agl.m2mTransform.processor!!
            val res = proc.parse(text)
            assertTrue(res.issues.errors.isEmpty(), "$tpl: ${res.issues}")
            val sa = proc.syntaxAnalysis(res.sppt!!)
            assertTrue(sa.issues.errors.isEmpty(), "$tpl: ${sa.issues}")
            val rule = sa.asm!!.allTransformRuleSet.first().rule.values.first() as net.akehurst.language.m2mTransform.api.M2mTransformPatternRule
            val objTpl = rule.domainTemplate.values.first() as net.akehurst.language.m2mTransform.api.ObjectTemplate
            return objTpl.propertyTemplate.values.first().rhs as CollectionTemplate
        }

        fun gaps(tpl: String) = template(tpl).gaps.map { it?.toString() }

        // members match the elements given, by element value
        fun matches(members: List<Int>, elements: List<Int>, ordered: Boolean, gaps: List<Boolean>, leftover: Int? = gaps.indexOfFirst { it }.takeIf { it >= 0 }) =
            collectionTemplateMatches(members.size, elements.size, ordered, gaps, leftover, members.map { m -> elements.map { e -> if (m == e) listOf("m$m@e") else emptyList() } })
                .map { cm -> cm.gapElements.map { g -> g?.map { elements[it] } } }
    }

    @Test
    fun parse_gaps() {
        assertEquals(listOf(null, null, null), gaps("[ a, b ]"))
        assertEquals(listOf("...", null), gaps("[ ... a ]"))
        assertEquals(listOf(null, "..."), gaps("[ a ... ]"))
        assertEquals(listOf("...", "..."), gaps("[ ... a ... ]"))
        assertEquals(listOf(null, "...", null), gaps("[ a ... b ]"))
        assertEquals(listOf(null, null, "rest:..."), gaps("[ a, b rest:... ]"))
        assertEquals(listOf("before:...", "after:..."), gaps("[ before:... a after:... ]"))
        assertEquals(listOf("..."), gaps("[ ... ]"))
        assertEquals(listOf(null), gaps("[ ]"))
    }

    @Test
    fun asString_gaps() {
        val str = template("[ before:... a ... b, c ]").asString(Indent())
        assertEquals("[\n  before:...\n  a\n  ...\n  b,\n  c\n]", str)
    }

    @Test
    fun ordered__no_gap_must_match_exactly_in_order() {
        assertEquals(listOf(listOf(null, null, null)), matches(listOf(1, 2), listOf(1, 2), true, listOf(false, false, false)))
        assertEquals(emptyList(), matches(listOf(2, 1), listOf(1, 2), true, listOf(false, false, false)))
        assertEquals(emptyList(), matches(listOf(1), listOf(1, 2), true, listOf(false, false)))
    }

    @Test
    fun ordered__gaps() {
        // [... 2 ...] on 1 2 3
        assertEquals(listOf(listOf(listOf(1), listOf(3))), matches(listOf(2), listOf(1, 2, 3), true, listOf(true, true)))
        // [1 ... 3]
        assertEquals(listOf(listOf(null, listOf(2), null)), matches(listOf(1, 3), listOf(1, 2, 3), true, listOf(false, true, false)))
        // [... 1] : 1 is not last
        assertEquals(emptyList(), matches(listOf(1), listOf(1, 2), true, listOf(true, false)))
    }

    @Test
    fun ordered__members_match_different_elements() {
        // [a, a] on [1] : one element cannot be matched by two members
        assertEquals(emptyList(), matches(listOf(1, 1), listOf(1), true, listOf(true, true, true)))
        // bag-like duplicates: [1, 1] on [1, 1] matches, each member one occurrence
        assertEquals(1, matches(listOf(1, 1), listOf(1, 1), true, listOf(false, false, false)).size)
    }

    @Test
    fun unordered__any_order() {
        assertEquals(1, matches(listOf(2, 1), listOf(1, 2), false, listOf(false, false, false)).size)
        // without a gap every element must be matched
        assertEquals(emptyList(), matches(listOf(2), listOf(1, 2), false, listOf(false, false)))
        // with a gap the others go into it
        assertEquals(listOf(listOf(null, listOf(1))), matches(listOf(2), listOf(1, 2), false, listOf(false, true)))
        // unordered bag: two members, each one occurrence of 1
        assertEquals(2, matches(listOf(1, 1), listOf(1, 1), false, listOf(false, false, false)).size)
    }
}
