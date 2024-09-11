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

package net.akehurst.language.komposite.common

import net.akehurst.language.api.language.base.QualifiedName
import kotlin.js.JsExport
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals

@JsExport
data class A (
    val prop1:String = "hello"
)

@JvmInline
value class AValueClass(val value:String)

class test_KompositeWalker {

    @Test
    fun walk_null() {
        val reg = DatatypeRegistry()
        val sut = kompositeWalker<String?, String>(reg) {
            nullValue() { path,info->
                WalkInfo(path.lastOrNull(), "null")
            }
        }

        val data = null
        val actual = sut.walk(WalkInfo(null, ""), data)

        val expected = WalkInfo<String?, String>(null, "null")

        assertEquals( expected, actual)
    }

    @Test
    fun walk_primitive_String() {
        val reg = DatatypeRegistry()
        reg.registerFromConfigString("""
            namespace kotlin {
                primitive String
            }
        """, emptyMap())

        var result = ""
        val sut = kompositeWalker<String?, String>(reg) {
            primitive { path, info, obj, mapper ->
                result += obj
                info
            }
        }

        val data = "Hello"
        val actual = sut.walk(WalkInfo(null, ""), data)

        val expected = WalkInfo<String?, String>(null, "null")

        assertEquals( data, result)
    }

    @Test
    fun walk_primitive_AValueClass_no_type() {
        val reg = DatatypeRegistry()
        reg.registerFromConfigString("""
            namespace kotlin {
                primitive String
            }
            namespace net.akehurst.language.komposite.common {
                primitive AValueClass
            }
        """, emptyMap())

        var result = ""
        val sut = kompositeWalker<String?, String>(reg) {
            primitive { path, info, obj, mapper ->
                result += obj
                info
            }
        }

        val data = "Hello"
        val type = reg.findByQualifiedNameOrNull(QualifiedName("kotlin.String"))!!
        val actual = sut.walk(WalkInfo(null, ""), data, type)

        val expected = WalkInfo<String?, String>(null, "null")

        assertEquals( data, result)
    }

    @Test
    fun walk_primitive_AValueClass_with_type() {
        val reg = DatatypeRegistry()
        reg.registerFromConfigString("""
            namespace kotlin {
                primitive String
            }
            namespace net.akehurst.language.komposite.common {
                primitive AValueClass
            }
        """, emptyMap())

        var result = ""
        val sut = kompositeWalker<String?, String>(reg) {
            primitive { path, info, obj, mapper ->
                result += obj
                info
            }
        }

        val data = "Hello"
        val actual = sut.walk(WalkInfo(null, ""), data)

        val expected = WalkInfo<String?, String>(null, "null")

        assertEquals( data, result)
    }

    @Test
    fun walk_Map() {
        var result = ""
        val reg = DatatypeRegistry()
        reg.registerFromConfigString("""
            namespace kotlin {
                primitive Int
                primitive String
            }
            namespace kotlin.collections {
               collection Map<K,V>
            }
        """, emptyMap())
        val sut = kompositeWalker<String, String>(reg) {
            primitive { path,info, value, m ->
                when(value) {
                    is Int -> result += "${value}"
                    is String -> result += "'${value}'"
                }
                info
            }
            mapBegin { path, info, map ->
                result += "Map { "
                info
            }
            mapEntryKeyBegin { path, info, entry ->
                result += "["
                info
            }
            mapEntryKeyEnd { path, info, entry ->
                result += "]"
                info
            }
            mapEntryValueBegin { path, info, entry ->
                result += " = "
                info
            }
            mapEntryValueEnd { path, info, entry ->  info}
            mapSeparate { path, info, map, previousEntry ->
                result += ", "
                info
            }
            mapEnd { path, info, map ->
                result += " }"
                info
            }
        }

        val data = mapOf<String, Int>(
            "a" to 1,
            "b" to 2,
            "c" to 3
        )
        val actual = sut.walk(WalkInfo("", ""), data)
        val expected = "Map { ['a'] = 1, ['b'] = 2, ['c'] = 3 }"
        assertEquals(expected, result)
    }

    @Test
    fun walk_object() {
        val reg = DatatypeRegistry()
        reg.registerFromConfigString("""
            namespace kotlin {
                primitive Int
                primitive String
            }
            namespace net.akehurst.language.komposite.common {
               datatype A {
                 composite-val prop1:String
               }
            }
        """, emptyMap())

        var result = ""
        val sut = kompositeWalker<String, String>(reg) {
            objectBegin { path,info, obj, datatype ->
                result += "${datatype.name} { "
                info
            }
            objectEnd { path,info, obj, datatype ->
                result += " }"
                info
            }
            propertyBegin { path,info, property ->
                result += "${property.name} = "
                info
            }
            primitive { path,info, value, m ->
                when(value) {
                    is String -> result += "'${value}'"
                }
                info
            }
        }

        val data = A()
        val actual = sut.walk(WalkInfo("", ""), data)
        val expected = "A { prop1 = 'hello' }"
        assertEquals(expected, result)
    }

    //TODO
}