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

package net.akehurst.language.agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.AsmTransformString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.TypesString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * asm-transform and types string cases found while writing the documentation.
 */
class test_ProvidedTransform_docsBugs {

    private companion object {
        val GRAMMAR = """
            namespace example
            grammar Shapes {
                skip leaf WS = "\s+" ;
                unit = item* ;
                item = circle | connection ;
                circle = 'circle' NAME radius? ;
                radius = 'r' NUMBER ;
                connection = 'connect' NAME 'to' NAME ;
                leaf NAME = "[a-z]+" ;
                leaf NUMBER = "[0-9]+" ;
            }
        """.trimIndent()
        const val SENTENCE = "circle c r 5 connect c to c"
    }

    @Test
    fun override_default_only__rule_with_existing_type() {
        // types of the default rules exist, without '#create-missing-types'
        val transform = """
            #override-default-transform
            namespace example
            asm-transform Shapes {
                connection: Connection() {
                    name := child[3]
                    name2 := child[1]
                }
            }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(GrammarString(GRAMMAR), transformStr = AsmTransformString(transform))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val processed = result.processor!!.process(SENTENCE)
        assertTrue(processed.allIssues.errors.isEmpty(), processed.allIssues.toString())
        val expected = """
            :Unit {
              item = [
                :Circle {
                  name = 'c'
                  radius = :Radius {
                    number = '5'
                  }
                }
                :Connection {
                  name = 'c'
                  name2 = 'c'
                }
              ]
            }
        """.trimIndent()
        assertEquals(expected, processed.asm!!.asString().trim())
    }

    @Test
    fun rule_type_cannot_be_determined__is_an_error_issue() {
        val transform = """
            #override-default-transform
            #create-missing-types
            namespace example
            asm-transform Shapes {
                radius: child[1]
            }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(GrammarString(GRAMMAR), transformStr = AsmTransformString(transform))
        val proc = result.processor!!
        proc.transformDomain // build it
        assertEquals(
            listOf("The type of rule 'radius' cannot be determined from 'child[1]', use 'Type() { ... }', 'tuple { ... }' or '<expression> as Type'"),
            proc.allIssues.errors.map { it.message }
        )
    }

    @Test
    fun types_string_with_transform__std_resolved() {
        val types = """
            namespace my.model
              data Disc { name: String }
        """.trimIndent()
        val transform = """
            namespace example
            asm-transform Shapes {
                #override-default-transform
                import-types my.model
                circle: Disc() { name := child[1] }
            }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(GrammarString(GRAMMAR), typeStr = TypesString(types), transformStr = AsmTransformString(transform))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val proc = result.processor!!
        val processed = proc.process(SENTENCE)
        assertTrue(proc.allIssues.errors.isEmpty(), proc.allIssues.toString())
        assertTrue(processed.allIssues.errors.isEmpty(), processed.allIssues.toString())
        val expected = """
            :Unit {
              item = [
                :Disc {
                  name = 'c'
                }
                :Connection {
                  name = 'c'
                  name2 = 'c'
                }
              ]
            }
        """.trimIndent()
        assertEquals(expected, processed.asm!!.asString().trim())
    }

    @Test
    fun no_options__missing_rules_are_error_issues() {
        // without '#override-default-transform' the transform has only the rules written
        val transform = """
            namespace example
            asm-transform Shapes {
                connection: Connection() {
                    name := child[1]
                    name2 := child[3]
                }
            }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(GrammarString(GRAMMAR), transformStr = AsmTransformString(transform))
        val proc = result.processor!!
        proc.transformDomain // build it
        assertEquals(
            listOf("unit", "item", "circle", "radius").map { "No transformation rule for grammar rule '$it' in 'example.Shapes', write one or use '#override-default-transform'" },
            proc.allIssues.errors.map { it.message }
        )
    }

    @Test
    fun types_string_in_grammar_namespace__used_by_default_transform() {
        val types = """
            namespace example.Shapes
              data Circle { name: String  colour: String? }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(GrammarString(GRAMMAR), typeStr = TypesString(types))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val proc = result.processor!!
        val processed = proc.process(SENTENCE)
        assertTrue(proc.allIssues.errors.isEmpty(), proc.allIssues.toString())
        assertTrue(processed.allIssues.errors.isEmpty(), processed.allIssues.toString())
        val circle = proc.typesDomain.findByQualifiedNameOrNull(net.akehurst.language.base.api.QualifiedName("example.Shapes.Circle"))!!
        // declared properties kept, missing ones generated
        listOf("name", "colour", "radius").forEach { pn ->
            assertTrue(null != circle.findOwnedPropertyOrNull(net.akehurst.language.types.api.PropertyName(pn)), "property '$pn' missing in\n" + proc.typesDomain.asString())
        }
        val expected = """
            :Unit {
              item = [
                :Circle {
                  name = 'c'
                  radius = :Radius {
                    number = '5'
                  }
                }
                :Connection {
                  name = 'c'
                  name2 = 'c'
                }
              ]
            }
        """.trimIndent()
        assertEquals(expected, processed.asm!!.asString().trim())
    }

    @Test
    fun create_missing_types__object_of_an_interface_is_an_error_issue() {
        // the types string declares an interface with the name of the object the transform creates
        val types = """
            namespace example.Shapes
              interface Shape { name: String }
        """.trimIndent()
        val transform = """
            #override-default-transform
            #create-missing-types
            namespace example
            asm-transform Shapes {
                circle: Shape() { name := child[1] }
            }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(GrammarString(GRAMMAR), typeStr = TypesString(types), transformStr = AsmTransformString(transform))
        val proc = result.processor!!
        proc.transformDomain // build it
        assertEquals(
            listOf("Cannot create an object of 'Shape' (in the transformation rule for 'circle'): it is an interface"),
            proc.allIssues.errors.map { it.message }
        )
    }

    @Test
    fun create_missing_types__provided_interface_is_the_type_of_a_choice_rule() {
        // the default type of rule 'item' would be a data type, the provided interface is used instead (David U20 A)
        val types = """
            namespace example.Shapes
              interface Item { }
        """.trimIndent()
        val transform = """
            #override-default-transform
            #create-missing-types
            namespace example
            asm-transform Shapes {
                unit: Unit() { items := children }
            }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(GrammarString(GRAMMAR), typeStr = TypesString(types), transformStr = AsmTransformString(transform))
        val proc = result.processor!!
        proc.transformDomain // build it
        assertTrue(proc.allIssues.errors.isEmpty(), proc.allIssues.toString())
        val processed = proc.process(SENTENCE)
        assertTrue(processed.allIssues.errors.isEmpty(), processed.allIssues.toString())
        val ns = proc.typesDomain.findNamespaceOrNull(net.akehurst.language.base.api.QualifiedName("example.Shapes"))!!
        assertTrue(ns.findOwnedTypeNamed(net.akehurst.language.base.api.SimpleName("Item")) is net.akehurst.language.types.api.InterfaceType)
        val expected = """
            :Unit {
              items = [
                :Circle {
                  name = 'c'
                  radius = :Radius {
                    number = '5'
                  }
                }
                :Connection {
                  name = 'c'
                  name2 = 'c'
                }
              ]
            }
        """.trimIndent()
        assertEquals(expected, processed.asm!!.asString().trim())
    }

    @Test
    fun create_missing_types__provided_interface_for_a_rule_that_creates_an_object_is_an_error_issue() {
        // rule 'circle' (default transform) creates an object, but the provided type 'Circle' is an interface
        val types = """
            namespace example.Shapes
              interface Circle { }
        """.trimIndent()
        val transform = """
            #override-default-transform
            #create-missing-types
            namespace example
            asm-transform Shapes {
                unit: Unit() { items := children }
            }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(GrammarString(GRAMMAR), typeStr = TypesString(types), transformStr = AsmTransformString(transform))
        val proc = result.processor!!
        proc.transformDomain // build it
        assertTrue(proc.allIssues.errors.isEmpty(), proc.allIssues.toString())
        val processed = proc.process(SENTENCE)
        assertEquals(1, processed.allIssues.errors.size, processed.allIssues.toString())
        assertTrue(processed.allIssues.errors.first().message.contains("Cannot create an object of type 'example.Shapes.Circle', it is not a data type or value type"))
    }

    @Test
    fun create_missing_types__incompatible_provided_property_is_an_error_issue() {
        // the types string has no 'import std', so neither type resolves; the message must not throw
        val types = """
            namespace example.Shapes
              data Radius { number: Integer }
        """.trimIndent()
        val transform = """
            #create-missing-types
            namespace example
            asm-transform Shapes {
                #override-default-transform
                radius: Radius() { number := child[1] as String }
            }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(GrammarString(GRAMMAR), typeStr = TypesString(types), transformStr = AsmTransformString(transform))
        val proc = result.processor!!
        proc.transformDomain // build it
        assertEquals(
            listOf("Trying to create missing property 'number: String (not resolved)', 'example.Shapes.Radius' already contains incompatible property 'number: Integer (not resolved)'."),
            proc.allIssues.errors.map { it.message }
        )
    }

    @Test
    fun create_missing_types__cast_to_provided_interface_that_is_not_a_rule_type() {
        // no grammar rule has a default type 'Shape', the provided interface is used as it is (David U20 A)
        val types = """
            namespace example.Shapes
              interface Shape { }
        """.trimIndent()
        val transform = """
            #create-missing-types
            namespace example
            asm-transform Shapes {
                #override-default-transform
                item: child[0] as Shape
            }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(GrammarString(GRAMMAR), typeStr = TypesString(types), transformStr = AsmTransformString(transform))
        val proc = result.processor!!
        proc.transformDomain // build it
        assertTrue(proc.allIssues.errors.isEmpty(), proc.allIssues.toString())
        val ns = proc.typesDomain.findNamespaceOrNull(net.akehurst.language.base.api.QualifiedName("example.Shapes"))!!
        assertTrue(ns.findOwnedTypeNamed(net.akehurst.language.base.api.SimpleName("Shape")) is net.akehurst.language.types.api.InterfaceType)
    }

    @Test
    fun references_path_through_a_type_that_does_not_resolve_is_an_error_issue() {
        // 'Info' is not defined (e.g. a missing import), the references check must not throw
        val types = """
            namespace example.Shapes
              data Connection { name: Info  name2: Info }
        """.trimIndent()
        val references = """
            namespace example.Shapes
                identify Circle by name
                references {
                    in Connection {
                        property name.text refers-to Circle
                    }
                }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(
            GrammarString(GRAMMAR),
            typeStr = TypesString(types),
            referenceStr = net.akehurst.language.api.processor.CrossReferenceString(references)
        )
        val proc = result.processor!!
        proc.crossReferenceDomain // build it
        val messages = proc.allIssues.errors.map { it.message }
        assertTrue(messages.any { it.contains("Cannot resolve type 'Info' in namespace 'example.Shapes', is an import needed?") }, messages.toString())
    }

    @Test
    fun process_with_missing_transformation_rules_gives_issues_not_an_exception() {
        // without '#override-default-transform' the rules not written are missing (error issues), process must still end gracefully
        val transform = """
            #create-missing-types
            namespace example
            asm-transform Shapes {
                connection: Connection() { name := child[1] }
            }
        """.trimIndent()
        val result = Agl.processorFromStringSimple(GrammarString(GRAMMAR), transformStr = AsmTransformString(transform))
        val proc = result.processor!!
        val processed = proc.process(SENTENCE)
        val messages = processed.allIssues.errors.map { it.message }
        assertTrue(messages.any { it.contains("No transformation rule for grammar rule 'unit'") }, messages.toString())
        assertTrue(messages.any { it.contains("No transformation rule found for grammar rule 'unit'") }, messages.toString())
    }
}
