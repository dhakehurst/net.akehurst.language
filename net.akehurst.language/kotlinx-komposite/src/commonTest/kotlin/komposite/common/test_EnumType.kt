/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.kotlinx.komposite.common

import net.akehurst.kotlinx.reflect.EnumValuesFunction
import net.akehurst.kotlinx.reflect.KotlinxReflect
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.typemodel.api.EnumType
import kotlin.test.Test
import kotlin.test.assertEquals

enum class EEEE {
    RED, GREEN, BLUE
}

class test_EnumType {

    @Test
    fun valueOfEnum() {
        KotlinxReflect.registerClass("net.akehurst.kotlin.komposite.common.EEEE",EEEE::class,EEEE::values as EnumValuesFunction)

        val typeModel = """
        namespace net.akehurst.kotlin.komposite.common
          enum EEE
        """.trimIndent()
        val dt = DatatypeRegistry()
        dt.registerFromAglTypesString(typeModel, emptyMap())

        val et = dt.findFirstDefinitionByNameOrNull(SimpleName("EEEE")) as EnumType

        val actual = et.valueOf("RED")
        assertEquals(EEEE.RED, actual)
    }
}