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

package net.akehurst.language.types.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.types.api.PropertyCharacteristic
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TypesDomain.asString() gives text that the AglTypes processor parses back to a TypesDomain with the same asString().
 */
class test_TypesAsStringRoundTrip {

    private companion object {
        fun assertRoundTrip(types: TypesDomain): String {
            val expected = types.asString()
            println(expected)
            val res = Agl.registry.agl.types.processor!!.process(expected)
            assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
            val parsed = assertNotNull(res.asm)
            val actual = parsed.asString()
            assertEquals(expected, actual)
            return actual
        }
    }

    @Test
    fun association_both_ends_owned_by_association() {
        // as agl-datastore-common's composition createTypes creates them
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("FileHandle")
                data("ParsableUnit")
                association {
                    end("FileHandle", setOf(REF, VAR, STR), "owner", ownedByAssociation = true)
                    end("ParsableUnit", setOf(REF, VAR, STR), "parsableUnit", isNullable = true, ownedByAssociation = true)
                }
            }
        }
        val actual = assertRoundTrip(types)
        assertTrue(actual.contains("end owned ref var str owner: FileHandle"), actual)
        assertTrue(actual.contains("end owned ref var str parsableUnit: ParsableUnit?"), actual)
    }

    @Test
    fun association_end_owned_by_type() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A")
                data("B")
                association {
                    end("A", setOf(CMP, VAR), "a", ownedByAssociation = true)
                    end("B", setOf(REF, VAR), "b")
                }
            }
        }
        val actual = assertRoundTrip(types)
        // the property for end 'b' is on type A, it is written once, in the association
        assertTrue(actual.contains("end ref var b: B"), actual)
        assertTrue(actual.contains("data A {\n    \n  }"), actual)
    }

    @Test
    fun association_collection_end() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("Doc")
                data("Part")
                association {
                    end("Doc", setOf(REF, VAR), "section", isNullable = true, ownedByAssociation = true)
                    end("Part", setOf(REF, VAR), "sections", collectionTypeName = "List", ownedByAssociation = true)
                }
            }
        }
        val actual = assertRoundTrip(types)
        assertTrue(actual.contains("end owned ref var sections: List<Part>"), actual)
    }

    @Test
    fun association_ends_in_other_namespaces() {
        val types = typesDomain("Test", true) {
            namespace("a") { data("A") }
            namespace("b") { data("B") }
            namespace("comp", imports = listOf("std", "a", "b")) {
                association {
                    end("A", setOf(REF, VAR, STR), "a", ownedByAssociation = true)
                    end("B", setOf(REF, VAR, STR), "b", isNullable = true, ownedByAssociation = true)
                }
            }
        }
        assertRoundTrip(types)
    }

    @Test
    fun data_constructor_with_collection_parameters() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("Node") {
                    constructor_ {
                        parameter(setOf(VAL, PropertyCharacteristic.IDENTITY), "id", "String")
                        parameter(setOf(VAL), "tags", "List") { typeArgument("String") }
                        parameter(setOf(VAL), "props", "Map") { typeArgument("String"); typeArgument("String") }
                        parameter(setOf(), "hint", "String")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "children", "List", false) { typeArgument("Node") }
                }
            }
        }
        val actual = assertRoundTrip(types)
        assertTrue(actual.contains("constructor(val str idn id: String, val str tags: List<String>, val str props: Map<String, String>, hint: String)"), actual)
    }

    @Test
    fun data_collection_type_not_resolvable() {
        // std not imported, printing must not fail
        val types = typesDomain("Test", true) {
            namespace("test", imports = emptyList()) {
                data("Node") {
                    constructor_ {
                        parameter(setOf(VAL), "tags", "List") { typeArgument("String") }
                    }
                }
            }
        }
        val actual = assertRoundTrip(types)
        assertTrue(actual.contains("val str tags: List<String>"), actual)
    }

    @Test
    fun enum_literals() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                enum("Colour", listOf("RED", "GREEN", "BLUE"))
            }
        }
        val actual = assertRoundTrip(types)
        assertTrue(actual.contains("enum Colour { RED, GREEN, BLUE }"), actual)
    }

    @Test
    fun value_type() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                value("Name") {
                    constructor_ {
                        parameter(setOf(VAL), "value", "String")
                    }
                }
            }
        }
        val actual = assertRoundTrip(types)
        assertTrue(actual.contains("value Name ( val str value:String )"), actual)
    }

    @Test
    fun supertype_with_nested_type_arguments() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                interface_("Holder") {
                    typeParameters("T")
                }
                interface_("A") {
                    supertype("Holder") { ref("List") { ref("String") } }
                }
                value("V") {
                    supertype("Holder") { ref("List") { ref("String") } }
                    constructor_ {
                        parameter(setOf(VAL), "value", "String")
                    }
                }
            }
        }
        val actual = assertRoundTrip(types)
        assertTrue(actual.contains("interface A : Holder<List<String>>"), actual)
        assertTrue(actual.contains("value V : Holder<List<String>>"), actual)
    }
}
