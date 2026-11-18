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

package net.akehurst.language.agl.language.reference

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.semanticAnalyser.contextFromTypesDomain
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.TypesString
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checking (semantic analysis) of 'forall' in a references definition, when the type of the forall expression
 * is not a collection or structured type: Nothing (not found), Any (not known), or an of-type that is not found.
 */
class test_ReferencesSemanticAnalyser_forall {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = item* ;
                item = 'item' ID ;
                leaf ID = "[a-z]+" ;
            }
            """.trimIndent()
        )

        // 'Unit.anything' has the type Any, 'Unit.things' is a list of an interface type
        val typesStr = TypesString(
            """
            namespace test.Test
              import std
              data Unit {
                cmp item: List<Item>
                cmp anything: Any
                cmp things: List<Thing>
              }
              data Item {
                cmp str id: String
              }
              interface Thing
              data Named : Thing {
                cmp str id: String
              }
            """.trimIndent()
        )

        val processor = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, typeStr = typesStr).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun check(references: String): Pair<List<String>, List<String>> {
            val res = CrossReferenceDomainDefault.fromString(contextFromTypesDomain(processor.typesDomain), CrossReferenceString(references))
            return Pair(res.allIssues.errors.map { it.message }, res.allIssues.warnings.map { it.message })
        }

        fun refs(inUnit: String) = """
            namespace test.Test
                identify Item by id
                references {
                    in Unit {
                        $inUnit
                    }
                }
        """.trimIndent()
    }

    @Test
    fun forall_list() {
        val (errors, warnings) = check(refs("forall item { property id refers-to Item }"))
        assertEquals(emptyList(), errors)
        assertEquals(emptyList(), warnings)
    }

    @Test
    fun forall_expression_with_property_not_found__error() {
        val (errors, _) = check(refs("forall nope { property id refers-to Item }"))
        assertEquals(1, errors.size, errors.toString())
        assertTrue(errors[0].contains("no property named 'nope'"), errors.toString())
    }

    @Test
    fun forall_expression_of_type_Any__warning() {
        val (errors, warnings) = check(refs("forall anything { property id refers-to Item }"))
        assertEquals(emptyList(), errors)
        assertEquals(
            listOf("For references in 'Unit', the type of forall expression 'anything' cannot be determined, its references are not type checked"),
            warnings
        )
    }

    @Test
    fun forall_expression_of_type_Any__refers_to_type_still_checked() {
        val (errors, _) = check(refs("forall anything { property id refers-to Nope }"))
        assertEquals(listOf("For references in 'Unit', referred to type 'Nope' not found"), errors)
    }

    @Test
    fun forall_of_type_not_found__error() {
        val (errors, _) = check(refs("forall item of-type Nope { property id refers-to Item }"))
        assertEquals(listOf("For references in 'Unit', forall 'item', the of-type 'Nope' is not found"), errors)
    }

    @Test
    fun forall_of_type_subtype_of_interface_element_type() {
        val (errors, _) = check(refs("forall things of-type Named { property id refers-to Item }"))
        assertEquals(emptyList(), errors)
    }

    @Test
    fun forall_of_type_not_subtype_of_interface_element_type__error() {
        val (errors, _) = check(refs("forall things of-type Item { property id refers-to Item }"))
        // (followed by the error that 'id' is not a property of 'Thing', the loop variable type then)
        assertEquals("The of-type 'Item' is not a subtype of the loop variable type 'Thing'", errors.firstOrNull(), errors.toString())
    }
}
