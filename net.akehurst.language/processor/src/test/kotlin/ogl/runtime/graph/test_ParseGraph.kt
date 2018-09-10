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

package net.akehurst.language.ogl.runtime.graph

import net.akehurst.language.ogl.runtime.structure.RuntimeRule
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleKind
import kotlin.test.Test
import kotlin.test.assertNotNull

class test_ParseGraph {

    @Test
    fun construct() {
        val sut = ParseGraph()

        assertNotNull(sut)
    }

    @Test
    fun start() {

        val goalRule = RuntimeRule(0,"a", RuntimeRuleKind.TERMINAL, false, false)

        val sut = ParseGraph()
        sut.start(goalRule)

        assertNotNull(sut)
    }

}