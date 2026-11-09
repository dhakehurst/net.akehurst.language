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
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.types.api.PropertyCharacteristic
import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.types.api.StructuredType
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.asm.resolveTypeDefinitionOrNull
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
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
    fun builtin_language_types() {
        // AglRegex.typesDomain is not implemented yet
        listOf(
            net.akehurst.language.base.processor.AglBase.typesDomain,
            net.akehurst.language.grammar.processor.AglGrammar.typesDomain,
            AglTypes.typesDomain,
            net.akehurst.language.expressions.processor.AglExpressions.typesDomain,
            net.akehurst.language.asmTransform.processor.AsmTransform.typesDomain,
            net.akehurst.language.reference.processor.AglCrossReference.typesDomain,
            net.akehurst.language.style.processor.AglStyle.typesDomain,
            net.akehurst.language.format.processor.AglFormat.typesDomain,
            net.akehurst.language.m2mTransform.processor.M2mTransform.typesDomain,
        ).forEach { assertRoundTrip(it) }
    }

    @Test
    fun names_that_are_not_identifiers_or_are_keywords() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A") {
                    constructor_ {
                        parameter(setOf(VAL), "\$value", "String")
                    }
                    propertyOf(setOf(VAR, CMP, STR), "\$group", "String")
                    propertyOf(setOf(VAR, CMP, STR), "str", "String")
                    propertyOf(setOf(VAR, CMP, STR), "end", "String")
                }
                data("B")
                association {
                    end("A", setOf(REF, VAR), "owned", ownedByAssociation = true)
                    end("B", setOf(REF, VAR), "\$b", isNullable = true, ownedByAssociation = true)
                }
            }
        }
        val actual = assertRoundTrip(types)
        assertTrue(actual.contains("constructor(val str `\$value`: String)"), actual)
        assertTrue(actual.contains("cmp var str `\$group`: String"), actual)
        assertTrue(actual.contains("cmp var str str: String"), actual)
        assertTrue(actual.contains("cmp var str end: String"), actual)
        assertTrue(actual.contains("end owned ref var owned: A"), actual)
    }

    @Test
    fun types_generated_from_a_grammar() {
        // these have names with '$' and tuple types (with named type arguments, possibly none)
        val grammarStr = """
            namespace test
            grammar Test {
              skip leaf WS = "\\s+" ;
              unit = decl* ;
              decl = 'var' ID (':' type)? ('=' expr)? ';' ;
              type = ID | ID '<' [type / ',']+ '>' ;
              expr = ID | NUM | expr '+' expr | '(' expr ')' | expr? '!' ;
              leaf ID = "[a-z]+" ;
              leaf NUM = "[0-9]+" ;
            }
        """.trimIndent()
        val res = Agl.processorFromStringSimple(net.akehurst.language.api.processor.GrammarString(grammarStr))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        val actual = assertRoundTrip(assertNotNull(res.processor).typesDomain)
        assertTrue(actual.contains("`\$group`: TupleType<>?"), actual)
        assertTrue(actual.contains("TupleType<expr:Expr, expr2:Expr>"), actual)
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
        // the types builder has no derived properties for data types
        (types.findFirstDefinitionByPossiblyQualifiedNameOrNull("test.Node".asPossiblyQualifiedName) as StructuredType).let { node ->
            node.appendPropertyDerived(PropertyName("size"), node.namespace.createTypeInstance(node.qualifiedName, "Integer".asPossiblyQualifiedName), "", "")
        }
        val actual = assertRoundTrip(types)
        assertTrue(actual.contains("val drv size: Integer"), actual)
        assertTrue(actual.contains("constructor(val str idn id: String, val str tags: List<String>, val str props: Map<String, String>, hint: String)"), actual)
    }

    @Test
    fun derived_property_is_parsed_as_derived() {
        val res = Agl.registry.agl.types.processor!!.process("""
            namespace test
              data A {
                val drv size: Integer
              }
        """.trimIndent())
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val a = assertNotNull(res.asm).findFirstDefinitionByPossiblyQualifiedNameOrNull("test.A".asPossiblyQualifiedName) as StructuredType
        val p = assertNotNull(a.findOwnedPropertyOrNull(PropertyName("size")))
        assertEquals(setOf(PropertyCharacteristic.READ_ONLY, PropertyCharacteristic.DERIVED), p.characteristics)
    }

    @Test
    fun property_cannot_be_stored_and_derived() {
        val res = Agl.registry.agl.types.processor!!.process("""
            namespace test
              data A {
                val str drv size: Integer
              }
        """.trimIndent())
        assertTrue(res.allIssues.errors.isNotEmpty(), res.allIssues.toString())
    }

    @Test
    fun data_constructor_parameter_that_is_a_property_in_two_constructors() {
        // the property 'label' is set by both constructors, it must be written once, else parsing adds it twice
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("Node") {
                    constructor_ {
                        parameter(setOf(REF, VAL), "label", "String")
                    }
                    constructor_ {
                        parameter(setOf(), "label", "String")
                        parameter(setOf(VAL), "size", "Integer")
                    }
                    constructor_ {
                        parameter(setOf(REF, VAL), "label", "String")
                        parameter(setOf(VAL), "size", "Integer")
                    }
                }
            }
        }
        val node = types.findFirstDefinitionByPossiblyQualifiedNameOrNull("test.Node".asPossiblyQualifiedName) as StructuredType
        assertEquals(listOf("label", "size"), node.property.map { it.name.value })
        val actual = assertRoundTrip(types)
        assertTrue(actual.contains("constructor(ref val str label: String)\n    constructor(label: String, val str size: Integer)\n    constructor(ref val str label: String, val str size: Integer)"), actual)
    }

    @Test
    fun data_constructor_parameter_conflicts_with_existing_property() {
        val res = Agl.registry.agl.types.processor!!.process("""
            namespace test
              data A {
                constructor(val a: String)
                constructor(var a: String)
              }
        """.trimIndent())
        assertTrue(res.allIssues.errors.isNotEmpty(), res.allIssues.toString())
    }

    @Test
    fun constructor_property_not_set_by_a_parameter_is_written_as_a_property() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("A") {
                    propertyOf(setOf(VAL, CON), "a", "String")
                }
            }
        }
        val actual = types.asString()
        assertTrue(actual.contains("val str a: String"), actual)
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

    @Test
    fun qualified_type_reference_to_namespace_that_is_not_imported_resolves_through_the_domain() {
        val text = """
            namespace other
              data B { }
            namespace test
              data A {
                val b: other.B
                val c: C
              }
              data C { }
        """.trimIndent()
        val res = Agl.registry.agl.types.processor!!.process(text)
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val domain = assertNotNull(res.asm)
        val a = domain.findByQualifiedNameOrNull(net.akehurst.language.base.api.QualifiedName("test.A")) as StructuredType
        val b = domain.findByQualifiedNameOrNull(net.akehurst.language.base.api.QualifiedName("other.B"))
        val c = domain.findByQualifiedNameOrNull(net.akehurst.language.base.api.QualifiedName("test.C"))
        val bType: TypeInstance = a.findOwnedPropertyOrNull(PropertyName("b"))!!.typeInstance
        val cType: TypeInstance = a.findOwnedPropertyOrNull(PropertyName("c"))!!.typeInstance

        // the namespace does not know the domain, so cannot resolve 'other.B' (it is not imported)
        assertNull(bType.resolvedDefinitionOrNull)
        // through the domain it resolves
        assertSame(b, domain.resolveTypeDefinitionOrNull(bType))
        assertSame(c, domain.resolveTypeDefinitionOrNull(cType))
        assertRoundTrip(domain)
    }
}
