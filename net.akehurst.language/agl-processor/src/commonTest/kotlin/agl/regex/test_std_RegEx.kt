/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.regex

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsPattern
import kotlin.test.Test
import kotlin.test.assertEquals

class test_std_RegEx {

    @Test
    fun t() {
        //'(', ID, chSep, ';'
        val rr = RuntimeRule(0, 0, "x", false).also {
            it.setRhs(RuntimeRuleRhsPattern(it, "|"))
        }
        val text = "?"
        val result = (rr.rhs as RuntimeRuleRhsPattern).matchable.using(RegexEnginePlatform).isLookingAt(text, 0)

        assertEquals(false, result)
    }

}