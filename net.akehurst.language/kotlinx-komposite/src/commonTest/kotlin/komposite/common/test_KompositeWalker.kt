/**
 * Copyright (C) 2019 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.builder.typeModel
import kotlin.js.JsExport
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals

@JsExport
data class A(
    val prop1: String = "hello"
)

@JvmInline
value class AValueClass(val value: String)

class test_KompositeWalker {

    @Test
    fun walk_null_with_no_type() {
        val reg = DatatypeRegistry()
        var result = ""
        val sut = kompositeWalker<String?, String>(reg) {
            nullValue() { path, info, type ->
                result += "null:${type.typeName}"
                info
            }
        }

        val data = null
        sut.walk(WalkInfo(null, ""), data)

        val expected = "null:Nothing"

        assertEquals(expected, result)
    }

    @Test
    fun walk_null_with_non_nullable_type() {
        val reg = DatatypeRegistry()
        reg.registerFromConfigString(
            """
            namespace kotlin {
                primitive String
            }
        """, emptyMap()
        )
        var result = ""
        val sut = kompositeWalker<String?, String>(reg) {
            nullValue() { path, info, type ->
                result += "null:${type.typeName}"
                info
            }
        }

        val data = null
        val type = reg.findFirstByNameOrNull(SimpleName("String"))!!.type(nullable = false)
        sut.walk(WalkInfo(null, ""), data, type)

        val expected = "null:Nothing"

        assertEquals(expected, result)
    }

    @Test
    fun walk_null_with_nullable_type() {
        val reg = DatatypeRegistry()
        reg.registerFromConfigString(
            """
            namespace kotlin {
                primitive String
            }
        """, emptyMap()
        )
        var result = ""
        val sut = kompositeWalker<String?, String>(reg) {
            nullValue() { path, info, type ->
                result += "null:${type.typeName}"
                info
            }
        }

        val data = null
        val type = reg.findFirstByNameOrNull(SimpleName("String"))!!.type(nullable = true)
        sut.walk(WalkInfo(null, ""), data, type)

        val expected = "null:String"

        assertEquals(expected, result)
    }

    @Test
    fun walk_primitive_String_no_type() {
        val reg = DatatypeRegistry()
        reg.registerFromConfigString(
            """
            namespace kotlin {
                primitive String
            }
        """, emptyMap()
        )

        var result = ""
        val sut = kompositeWalker<String?, String>(reg) {
            primitive { path, info, obj, type, mapper ->
                result += "'$obj':${type.name}"
                info
            }
        }

        val data = "Hello"
        sut.walk(WalkInfo(null, ""), data, null)

        assertEquals("'Hello':String", result)
    }

    @Test
    fun walk_primitive_String_with_type() {
        val reg = DatatypeRegistry()
        reg.registerFromConfigString(
            """
            namespace kotlin {
                primitive String
            }
        """, emptyMap()
        )

        var result = ""
        val sut = kompositeWalker<String?, String>(reg) {
            primitive { path, info, obj, type, mapper ->
                result += "'$obj':${type.name}"
                info
            }
        }

        val data = "Hello"
        val type = reg.findFirstByNameOrNull(SimpleName("String"))!!.type()
        sut.walk(WalkInfo(null, ""), data, type)

        assertEquals("'Hello':String", result)
    }

    @Test
    fun walk_primitive_AValueClass_no_type() {
        val reg = DatatypeRegistry()
        reg.registerFromConfigString(
            """
            namespace kotlin {
                primitive String
            }
            namespace net.akehurst.language.komposite.common {
                primitive AValueClass
            }
        """, emptyMap()
        )

        var result = ""
        val sut = kompositeWalker<String?, String>(reg) {
            primitive { path, info, obj, type, mapper ->
                result += "'$obj':${type.name}"
                info
            }
        }

        val data = "Hello"
        sut.walk(WalkInfo(null, ""), data, null)

        assertEquals("'Hello':String", result)
    }

    @Test
    fun walk_primitive_AValueClass_with_type() {
        val reg = DatatypeRegistry()
        reg.registerFromConfigString(
            """
            namespace kotlin {
                primitive String
            }
            namespace net.akehurst.language.komposite.common {
                primitive AValueClass
            }
        """, emptyMap()
        )

        var result = ""
        val sut = kompositeWalker<String?, String>(reg) {
            primitive { path, info, data, type, mapper ->
                result += "'$data':${type.name}"
                info
            }
            valueType { path, info, data, type, value, mapper ->
                result += "'$data':${type.name}"
                info
            }
        }

        val data = "Hello"
        val type = reg.findFirstByNameOrNull(SimpleName("AValueClass"))!!.type()
        sut.walk(WalkInfo(null, ""), data, type)

        assertEquals("'Hello':AValueClass", result)
    }

    @Test
    fun walk_Map_no_type() {
        var result = ""
        val reg = DatatypeRegistry()
        reg.registerFromConfigString(
            """
            namespace kotlin {
                primitive Int
                primitive String
            }
            namespace kotlin.collections {
               collection Map<K,V>
            }
        """, emptyMap()
        )
        val sut = kompositeWalker<String, String>(reg) {
            primitive { path, info, value, type, m ->
                when (value) {
                    is Int -> result += "${value}:${type.name}"
                    is String -> result += "'${value}':${type.name}"
                }
                info
            }
            mapBegin { path, info, map, type, et, vt ->
                result += "Map<${et.typeName},${vt.typeName}> { "
                info
            }
            mapEntryKeyBegin { path, info, entry, et, vt ->
                result += "["
                info
            }
            mapEntryKeyEnd { path, info, entry, et, vt ->
                result += "]"
                info
            }
            mapEntryValueBegin { path, info, entry, et, vt ->
                result += " = "
                info
            }
            mapEntryValueEnd { path, info, entry, et, vt -> info }
            mapSeparate { path, info, map, type, previousEntry, et, vt ->
                result += ", "
                info
            }
            mapEnd { path, info, map, type, et, vt ->
                result += " }"
                info
            }
        }

        val data = mapOf<String, Int>(
            "a" to 1,
            "b" to 2,
            "c" to 3
        )
        sut.walk(WalkInfo("", ""), data, null)
        val expected = "Map<Any,Any> { ['a':String] = 1:Int, ['b':String] = 2:Int, ['c':String] = 3:Int }"
        assertEquals(expected, result)
    }

    @Test
    fun walk_Map_with_type() {
        var result = ""
        val reg = DatatypeRegistry()
        val tm = typeModel("Test",true) {}
        reg.registerFromTypeModel(tm, emptyMap())
        val sut = kompositeWalker<String, String>(reg) {
            primitive { path, info, value, type, m ->
                when (value) {
                    is Int -> result += "${value}:${type.name}"
                    is String -> result += "'${value}':${type.name}"
                }
                info
            }
            mapBegin { path, info, map, type, et, vt ->
                result += "Map<${et.typeName},${vt.typeName}> { "
                info
            }
            mapEntryKeyBegin { path, info, entry, et, vt ->
                result += "["
                info
            }
            mapEntryKeyEnd { path, info, entry, et, vt ->
                result += "]"
                info
            }
            mapEntryValueBegin { path, info, entry, et, vt ->
                result += " = "
                info
            }
            mapEntryValueEnd { path, info, entry, et, vt -> info }
            mapSeparate { path, info, map, type, previousEntry, et, vt ->
                result += ", "
                info
            }
            mapEnd { path, info, map, type, et, vt ->
                result += " }"
                info
            }
        }

        val data = mapOf<String, Int>(
            "a" to 1,
            "b" to 2,
            "c" to 3
        )
        val type = SimpleTypeModelStdLib.Map.type(typeArguments = listOf(SimpleTypeModelStdLib.String.asTypeArgument, SimpleTypeModelStdLib.Integer.asTypeArgument))
        sut.walk(WalkInfo("", ""), data, type)
        val expected = "Map<String,Integer> { ['a':String] = 1:Integer, ['b':String] = 2:Integer, ['c':String] = 3:Integer }"
        assertEquals(expected, result)
    }

    @Test
    fun walk_object_primitive_property_no_type() {
        val reg = DatatypeRegistry()
        val tm = typeModel("Test",true) {
            namespace("net.akehurst.language.komposite.common", imports = listOf(SimpleTypeModelStdLib.qualifiedName.value)) {
                dataType("A") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        reg.registerFromTypeModel(tm, emptyMap())

        var result = ""
        val sut = kompositeWalker<String, String>(reg) {
            objectBegin { path, info, obj, datatype ->
                result += "${datatype.name} { "
                info
            }
            objectEnd { path, info, obj, datatype ->
                result += " }"
                info
            }
            propertyBegin { path, info, property ->
                result += "${property.name} = "
                info
            }
            primitive { path, info, value, type, m ->
                when (value) {
                    is String -> result += "'${value}'"
                }
                info
            }
        }

        val data = A()
        sut.walk(WalkInfo("", ""), data, null)

        val expected = "A { prop1 = 'hello' }"
        assertEquals(expected, result)
    }

    @Test
    fun walk_object_primitive_property_with_type() {
        val reg = DatatypeRegistry()
        val tm = typeModel("Test",true) {
            namespace("net.akehurst.language.komposite.common", imports = listOf(SimpleTypeModelStdLib.qualifiedName.value)) {
                dataType("A") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        reg.registerFromTypeModel(tm, emptyMap())

        var result = ""
        val sut = kompositeWalker<String, String>(reg) {
            objectBegin { path, info, obj, datatype ->
                result += "${datatype.name} { "
                info
            }
            objectEnd { path, info, obj, datatype ->
                result += " }"
                info
            }
            propertyBegin { path, info, property ->
                result += "${property.name} = "
                info
            }
            primitive { path, info, value, type, m ->
                when (value) {
                    is String -> result += "'${value}'"
                }
                info
            }
        }

        val data = A()
        sut.walk(WalkInfo("", ""), data, null)

        val expected = "A { prop1 = 'hello' }"
        assertEquals(expected, result)
    }

    //TODO
}