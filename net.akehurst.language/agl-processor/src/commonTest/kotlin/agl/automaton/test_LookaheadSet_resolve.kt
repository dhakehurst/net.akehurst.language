/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.automaton

import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_LookaheadSet_resolve : test_AutomatonUtilsAbstract() {

    @Test
    fun no_substitution() {
        val sut = LookaheadSet(1, false, false, false, setOf(UNDEFINED))
        val eot = LookaheadSet(2, false, false, false, setOf())
        val rt = LookaheadSet(3, false, false, false, setOf())

        val actual = sut.resolve(eot, rt)
        val expected = LookaheadSetPart(false, false, false, setOf(UNDEFINED))

        assertEquals(expected.fullContent,actual.fullContent)
    }

    @Test
    fun do_not_substitute_RT() {
        val sut = LookaheadSet(1, false, false, false, setOf())
        val eot = LookaheadSet(2, false, false, false, setOf())
        val rt = LookaheadSet(3, false, false, false, setOf(UNDEFINED))

        val actual = sut.resolve(eot, rt)
        val expected = LookaheadSetPart(false, false, false, setOf())

        assertEquals(expected.fullContent,actual.fullContent)
    }

    @Test
    fun do_substitute_RT() {
        val sut = LookaheadSet(1, true, false, false, setOf())
        val eot = LookaheadSet(2, false, false, false, setOf())
        val rt = LookaheadSet(3, false, false, false, setOf(UNDEFINED))

        val actual = sut.resolve(eot, rt)
        val expected = LookaheadSetPart(false, false, false, setOf(UNDEFINED))

        assertEquals(expected.fullContent,actual.fullContent)
    }

    @Test
    fun do_not_substitute_EOT() {
        val sut = LookaheadSet(1, false, false, false, setOf())
        val eot = LookaheadSet(2, false, false, false, setOf(UNDEFINED))
        val rt = LookaheadSet(3, false, false, false, setOf())

        val actual = sut.resolve(eot, rt)
        val expected = LookaheadSetPart(false, false, false, setOf())

        assertEquals(expected.fullContent,actual.fullContent)
    }

    @Test
    fun do_substitute_EOT() {
        val sut = LookaheadSet(1, false, true, false, emptySet())
        val eot = LookaheadSet(2, false, false, false, setOf(UNDEFINED))
        val rt = LookaheadSet(3, false, true, false, emptySet())

        val actual = sut.resolve(eot, rt)
        val expected = LookaheadSetPart(false, false, false, setOf(UNDEFINED))

        assertEquals(expected.fullContent,actual.fullContent)
    }

}