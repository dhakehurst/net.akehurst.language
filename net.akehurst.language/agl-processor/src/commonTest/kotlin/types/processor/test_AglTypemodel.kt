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

package net.akehurst.language.types.processor

import net.akehurst.language.base.api.SimpleName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_AglTypemodel {

    @Test
    fun grammarModel() {
        val actual = AglTypes.grammarDomain

        assertNotNull(actual)
        assertEquals(AglTypes.grammarString, actual.asString())
    }

    @Test
    fun typemodel() {
        val actual = AglTypes.typesDomain

        assertNotNull(actual)
    }


    @Test
    fun domainTypes() {
        val td = AglTypes.typesDomain.findFirstDefinitionByNameOrNull(SimpleName("TypesDomain"))
        assertNotNull(td)
        assertEquals("TypesDomain", td.name.value)
        assertEquals("Domain", td.supertypes[0].typeName.value)
        assertEquals("TypesNamespace", td.supertypes[0].typeArguments[0].type.typeName.value)
        assertEquals("TypeDeclaration", td.supertypes[0].typeArguments[1].type.typeName.value)
    }
}