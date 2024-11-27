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

package net.akehurst.language.typemodel.asm

import net.akehurst.language.typemodel.api.PropertyName
import kotlin.test.Test
import kotlin.test.assertEquals

class test_SimpleTypeModelStdLib {

    @Test
    fun primitive_AnyType() {
        assertEquals("std.Any", StdLibDefault.AnyType.qualifiedTypeName.value)

    }

    @Test
    fun primitive_NothingType() {
        assertEquals("std.Nothing", StdLibDefault.NothingType.qualifiedTypeName.value)

    }

    @Test
    fun primitive_String() {
        assertEquals("std.String", StdLibDefault.String.qualifiedTypeName.value)

    }

    @Test
    fun primitive_Boolean() {
        assertEquals("std.Boolean", StdLibDefault.Boolean.qualifiedTypeName.value)

    }

    @Test
    fun primitive_Integer() {
        assertEquals("std.Integer", StdLibDefault.Integer.qualifiedTypeName.value)

    }

    @Test
    fun primitive_Real() {
        assertEquals("std.Real", StdLibDefault.Real.qualifiedTypeName.value)

    }

    @Test
    fun primitive_Timestamp() {
        assertEquals("std.Timestamp", StdLibDefault.Timestamp.qualifiedTypeName.value)

    }

    @Test
    fun tupleType() {
        val actual = StdLibDefault.TupleType
        assertEquals("std.TupleType",actual.qualifiedName.value)
    }

    @Test
    fun tupleType_instance() {
        val args = listOf(
            TypeArgumentNamedSimple(PropertyName("prop1"),StdLibDefault.Integer),
            TypeArgumentNamedSimple(PropertyName("prop2"),StdLibDefault.String),
            TypeArgumentNamedSimple(PropertyName("prop3"),StdLibDefault.Boolean),
        )
        val actual = StdLibDefault.TupleType.typeTuple(args)
        assertEquals(StdLibDefault.TupleType,actual.declaration)
        assertEquals("std.TupleType",actual.declaration.qualifiedName.value)
        assertEquals("std.TupleType",actual.qualifiedTypeName.value)
    }

    @Test
    fun collection_List() {
        assertEquals("std.List", StdLibDefault.List.qualifiedName.value)
        assertEquals("size", StdLibDefault.List.findAllPropertyOrNull(PropertyName("size"))!!.name.value)
        assertEquals("std.Integer", StdLibDefault.List.findAllPropertyOrNull(PropertyName("size"))!!.typeInstance.qualifiedTypeName.value)


    }
}