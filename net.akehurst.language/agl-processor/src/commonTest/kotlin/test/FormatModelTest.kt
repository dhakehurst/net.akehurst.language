/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.api.format

import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.formatter.AglFormatterRule
import kotlin.test.assertEquals
import kotlin.test.fail

object FormatModelTest {

    fun assertEqual(expected: AglFormatterModel?, actual: AglFormatterModel?) {
        when {
            (expected == null && actual == null) -> Unit // pass
            expected == null -> fail()
            actual == null -> fail()
            else -> {
                assertEquals(expected.rules.size, actual.rules.size, "number of rules in AglFormatterModel is different")
                for (k in expected.rules.keys) {
                    val expEl = expected.rules[k]
                    val actEl = actual.rules[k]
                    fmAssertEquals(expEl, actEl, "AglFormatterModel")
                }
            }
        }
    }

    private fun fmAssertEquals(expected: AglFormatterRule?, actual: AglFormatterRule?, source: String) {
        when {
            null == expected || null == actual -> fail("should never be null")
//TODO
        }
    }
}