/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.typemodel.processor

import net.akehurst.language.base.api.SimpleName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_AglTypemodel {

    @Test
    fun grammarModel() {
        val actual = AglTypemodel.grammarModel

        assertNotNull(actual)
        assertEquals(AglTypemodel.grammarString, actual.asString())
    }

    @Test
    fun typemodel() {
        val actual = AglTypemodel.typeModel

        assertNotNull(actual)
    }


    @Test
    fun domainTypes() {
        val td = AglTypemodel.typeModel.findFirstDefinitionByNameOrNull(SimpleName("TypeModel"))
        assertNotNull(td)
        assertEquals("TypeModel", td.name.value)
        assertEquals("Model", td.supertypes[0].typeName.value)
        assertEquals("TypeNamespace", td.supertypes[0].typeArguments[0].type.typeName.value)
        assertEquals("TypeDeclaration", td.supertypes[0].typeArguments[1].type.typeName.value)
    }
}