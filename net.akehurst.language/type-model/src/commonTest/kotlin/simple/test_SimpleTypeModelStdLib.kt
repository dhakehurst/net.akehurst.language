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

package net.akehurst.language.typemodel.simple

import kotlin.test.Test
import kotlin.test.assertEquals

class test_SimpleTypeModelStdLib {

    @Test
    fun primitive_AnyType() {
        assertEquals("std.Any", SimpleTypeModelStdLib.AnyType.qualifiedTypeName)

    }

    @Test
    fun primitive_NothingType() {
        assertEquals("std.Nothing", SimpleTypeModelStdLib.NothingType.qualifiedTypeName)

    }

    @Test
    fun primitive_String() {
        assertEquals("std.String", SimpleTypeModelStdLib.String.qualifiedTypeName)

    }

    @Test
    fun primitive_Boolean() {
        assertEquals("std.Boolean", SimpleTypeModelStdLib.Boolean.qualifiedTypeName)

    }

    @Test
    fun primitive_Integer() {
        assertEquals("std.Integer", SimpleTypeModelStdLib.Integer.qualifiedTypeName)

    }

    @Test
    fun primitive_Real() {
        assertEquals("std.Real", SimpleTypeModelStdLib.Real.qualifiedTypeName)

    }

    @Test
    fun primitive_Timestamp() {
        assertEquals("std.Timestamp", SimpleTypeModelStdLib.Timestamp.qualifiedTypeName)

    }

    @Test
    fun collection_List() {
        assertEquals("std.List", SimpleTypeModelStdLib.List.qualifiedName)
        assertEquals("size", SimpleTypeModelStdLib.List.findPropertyOrNull("size")?.name)
        assertEquals("std.Integer", SimpleTypeModelStdLib.List.findPropertyOrNull("size")?.typeInstance?.qualifiedTypeName)


    }
}