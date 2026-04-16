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

package net.akehurst.language.types.asm

import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals

class test_CloningTypes {

    @Test
    fun findInOrCloneTo_Data() {
        val originalTD = typesDomain("Original", true) {
            namespace("test") {
                data("DataType")
            }
        }

        val clonedTD = typesDomain("Cloned", true) {}

        val original = originalTD.findFirstDefinitionByNameOrNull(SimpleName("DataType"))!!

        val clone = original.findInOrCloneTo(clonedTD)

        val actual = clonedTD.findFirstDefinitionByNameOrNull(SimpleName("DataType"))
        assertEquals(original, actual)
    }

}