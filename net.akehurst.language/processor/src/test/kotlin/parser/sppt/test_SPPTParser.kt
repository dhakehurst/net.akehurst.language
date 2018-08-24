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

package net.akehurst.language.parser.sppt

import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.parser.runtime.RuntimeRuleSetBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class test_SPPTParser {

    @Test fun construct() {
        val rrb = RuntimeRuleSetBuilder()
        val sut = SPPTParser(rrb)

        assertNotNull(sut)
    }

    @Test fun leaf_literal() {
        val rrb = RuntimeRuleSetBuilder()
        val sut = SPPTParser(rrb)

        sut.leaf("a")
    }

    @Test fun leaf_pattern() {
        val rrb = RuntimeRuleSetBuilder()
        val sut = SPPTParser(rrb)

        sut.leaf("[a-z]","a")
    }

    @Test fun emptyLeaf() {
        val rrb = RuntimeRuleSetBuilder()
        val sut = SPPTParser(rrb)

        sut.emptyLeaf("a")
    }

    @Test fun branch() {
        val rrb = RuntimeRuleSetBuilder()
        val sut = SPPTParser(rrb)

        sut.branch("a", listOf<SPPTNode>())
    }
}