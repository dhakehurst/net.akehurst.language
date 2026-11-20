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
import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import net.akehurst.language.reference.asm.ReferenceExpressionCollectionDefault
import net.akehurst.language.reference.asm.ScopeImportDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 'of-type A | B' and 'of-type N' (N a type-group): only the elements that conform to one of the types are selected,
 * in a 'forall' of an 'in' block and in 'import forall' / 'inherit forall'.
 */
class test_References_ofTypeGroup {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = member* ;
                member = def | pkg | use ;
                pkg = 'pkg' ID ':' sup* '{' member* '}' ;
                sup = a | b | c ;
                a = 'a' ID ;
                b = 'b' ID ;
                c = 'c' ID ;
                def = 'def' ID ;
                use = 'use' ID ;
                leaf ID = "[a-z]+" ;
            }
            """.trimIndent()
        )

        val processor = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun refs(ofType: String) = """
            namespace test.Test
                type-group AB = A | B
                identify Pkg by id
                identify Def by id
                scope Pkg {
                    identify Def by id
                    inherit forall sup of-type $ofType members-of id
                }
                references {
                    in Pkg { forall sup of-type $ofType { property id refers-to Pkg } }
                    in Use { property id refers-to Def }
                }
            """.trimIndent()

        fun definitionErrors(references: String): List<String> {
            val res = CrossReferenceDomainDefault.fromString(contextFromTypesDomain(processor.typesDomain), CrossReferenceString(references))
            return res.allIssues.errors.map { it.message }
        }

        fun sentenceErrors(references: String, sentence: String): List<String> {
            val p = Agl.processorFromStringSimple(grammarStr, referenceStr = CrossReferenceString(references)).let {
                assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
                it.processor!!
            }
            return p.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } }).allIssues.errors.map { it.message }
        }
    }

    @Test
    fun no_definition_errors() {
        assertEquals(emptyList(), definitionErrors(refs("AB")))
        assertEquals(emptyList(), definitionErrors(refs("A | B")))
    }

    @Test
    fun group_expanded() {
        val res = CrossReferenceDomainDefault.fromString(contextFromTypesDomain(processor.typesDomain), CrossReferenceString(refs("AB")))
        val decl = res.asm!!.namespace[0].definition[0]
        val si = decl.scopeDefinition.values.first { it.scopeForTypeName.value == "Pkg" }.scopeImports[0] as ScopeImportDefault
        assertEquals(listOf("A", "B"), si.ofType.map { it.value })
        assertEquals(listOf("AB"), si.ofTypeAsWritten.map { it.value })
        val fa = decl.references.first { it.inTypeName.value == "Pkg" }.referenceExpressionList[0] as ReferenceExpressionCollectionDefault
        assertEquals(listOf("A", "B"), fa.ofType.map { it.value })
        assertEquals(listOf("AB"), fa.ofTypeAsWritten.map { it.value })
    }

    @Test
    fun forall_selects_each_type_of_the_group() {
        for (ot in listOf("AB", "A | B")) {
            // a and b: the reference is resolved (an error when it is not found); c: not selected, so not a reference
            assertEquals(emptyList(), sentenceErrors(refs(ot), "pkg p : { } pkg q : a p b p c zz { }"), ot)
            assertEquals(2, sentenceErrors(refs(ot), "pkg q : a xx b yy { }").size, ot)
        }
    }

    @Test
    fun inherit_selects_each_type_of_the_group() {
        for (ot in listOf("AB", "A | B")) {
            assertEquals(emptyList(), sentenceErrors(refs(ot), "pkg p : { def x } pkg q : a p { use x } pkg r : b p { use x }"), ot)
            // c is not of a type of the group: nothing is inherited through it
            assertEquals(1, sentenceErrors(refs(ot), "pkg p : { def x } pkg q : c p { use x }").size, ot)
        }
    }

    @Test
    fun asString_round_trip() {
        for (ot in listOf("AB", "A | B")) {
            val res = CrossReferenceDomainDefault.fromString(contextFromTypesDomain(processor.typesDomain), CrossReferenceString(refs(ot)))
            val str = res.asm!!.asString()
            assertTrue(str.contains("inherit forall sup of-type $ot members-of id"), str)
            assertTrue(str.contains("forall sup of-type $ot {"), str)
            assertEquals(emptyList(), definitionErrors(str))
        }
    }

    @Test
    fun unknown_type__error() {
        val errors = definitionErrors(refs("A | Nope"))
        assertEquals(2, errors.size, errors.toString())
        assertTrue(errors.all { it.contains("the of-type 'Nope' is not found") }, errors.toString())
    }
}
