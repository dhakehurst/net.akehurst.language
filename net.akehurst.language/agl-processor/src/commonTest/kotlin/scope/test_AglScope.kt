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

package net.akehurst.language.scope.processor

import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.typemodel.api.PropertyName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_AglScope {

    @Test
    fun typemodel() {
        val actual = AglScope.typeModel

        assertNotNull(actual)
    }


    @Test
    fun domainTypes() {
        val td = AglScope.typeModel.findFirstDefinitionByNameOrNull(SimpleName("Scope"))
        assertNotNull(td)
        assertEquals("Scope", td.name.value)
        val itemsProp = td.findAllPropertyOrNull(PropertyName("items"))
        assertNotNull(itemsProp)
        assertEquals("Map", itemsProp.typeInstance.typeName.value)
        assertEquals(2, itemsProp.typeInstance.typeArguments.size)
        val itemsArg2 = itemsProp.typeInstance.typeArguments[1]
        assertEquals("Map", itemsArg2.type.typeName.value)
        assertEquals(2, itemsArg2.type.typeArguments.size)

        val resItemsProp = td.type().allResolvedProperty[PropertyName("items")]
        assertNotNull(itemsProp)
    }
}