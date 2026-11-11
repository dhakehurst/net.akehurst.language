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
import net.akehurst.language.agl.semanticAnalyser.contextFromTypesDomain
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * scope, identify and 'in' declarations match by type conformance (supertypes and union types)
 */
class test_SemanticAnalyserSimple_supertypes {

    private companion object {
        // decl and member are choices of data types, so Decl and Member are supertypes
        val grammarStr = """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = decl* ;
                decl = pkg | cls ;
                member = cls | fld ;
                pkg = 'package' ID '{' decl* '}' ;
                cls = 'class' ID ':' NAME '{' member* '}' ;
                fld = 'field' ID ;
                leaf ID = "[a-z]+" ;
                leaf NAME = "[A-Z]+" ;
            }
        """.trimIndent()

        fun processor(referencesStr: String) = Agl.processorFromStringSimple(
            grammarDefinitionStr = GrammarString(grammarStr),
            referenceStr = CrossReferenceString(referencesStr)
        )

        fun scopeOf(referencesStr: String, sentence: String): Pair<String, List<String>> {
            val res = processor(referencesStr)
            check(res.issues.errors.isEmpty()) { res.issues.toString() }
            val proc = res.processor!!
            val context = contextAsmSimpleWithAsmPath()
            val result = proc.process(sentence, Agl.options<Asm, SentenceContext> {
                semanticAnalysis {
                    checkReferences(true)
                    sentenceContext(context)
                }
            })
            return Pair(context.asString(), result.allIssues.errors.map { it.message })
        }

        fun lookup(referencesStr: String): Pair<net.akehurst.language.reference.asm.CrossReferenceDomainDefault, net.akehurst.language.types.api.TypesDomain> {
            val res = processor(referencesStr)
            check(res.issues.errors.isEmpty()) { res.issues.toString() }
            val proc = res.processor!!
            return Pair(proc.crossReferenceDomain as CrossReferenceDomainDefault, proc.typesDomain)
        }

        val Decl = QualifiedName("test.Test.Decl")
        val Member = QualifiedName("test.Test.Member")
        val Pkg = QualifiedName("test.Test.Pkg")
        val Cls = QualifiedName("test.Test.Cls")
        val Fld = QualifiedName("test.Test.Fld")
        val ROOT = CrossReferenceDomainDefault.ROOT_SCOPE_TYPE_NAME
    }

    @Test
    fun types_have_expected_supertypes() {
        val (_, td) = lookup("namespace test.Test")
        val cls = td.findByQualifiedNameOrNull(Cls)!!
        assertEquals(setOf("Decl", "Member"), cls.supertypes.map { it.typeName.value }.toSet())
    }

    @Test
    fun scope_for_supertype_applies_to_subtypes() {
        val (crd, td) = lookup(
            """
            namespace test.Test
                scope Decl { }
            """.trimIndent()
        )
        assertTrue(crd.isScopeDefinedFor(td, Pkg))
        assertTrue(crd.isScopeDefinedFor(td, Cls))
        assertTrue(crd.isScopeDefinedFor(td, Decl))
        assertEquals(false, crd.isScopeDefinedFor(td, Fld))
    }

    @Test
    fun scope_with_qualified_name() {
        val (crd, td) = lookup(
            """
            namespace test.Test
                scope test.Test.Decl { }
            """.trimIndent()
        )
        assertTrue(crd.isScopeDefinedFor(td, Pkg))
    }

    @Test
    fun toplevel_identify_applies_in_every_scope() {
        val (crd, td) = lookup(
            """
            namespace test.Test
                identify Cls by id
                scope Pkg { }
            """.trimIndent()
        )
        assertEquals("id", crd.identifyingExpressionFor(td, ROOT, Cls).singleOrNull?.identifiedBy?.asString())
        assertEquals("id", crd.identifyingExpressionFor(td, Pkg, Cls).singleOrNull?.identifiedBy?.asString())
        assertEquals(null, crd.identifyingExpressionFor(td, Pkg, Fld).singleOrNull)
    }

    @Test
    fun scope_identify_wins_over_toplevel() {
        val (crd, td) = lookup(
            """
            namespace test.Test
                identify Cls by id
                scope Pkg {
                    identify Cls by name
                }
            """.trimIndent()
        )
        assertEquals("id", crd.identifyingExpressionFor(td, ROOT, Cls).singleOrNull?.identifiedBy?.asString())
        assertEquals("name", crd.identifyingExpressionFor(td, Pkg, Cls).singleOrNull?.identifiedBy?.asString())
    }

    @Test
    fun nearest_scope_wins() {
        val (crd, td) = lookup(
            """
            namespace test.Test
                scope Decl {
                    identify Cls by id
                }
                scope Pkg {
                    identify Cls by name
                }
            """.trimIndent()
        )
        // scope type Pkg: exact beats supertype
        assertEquals("name", crd.identifyingExpressionFor(td, Pkg, Cls).singleOrNull?.identifiedBy?.asString())
        // scope type Cls: only Decl matches
        assertEquals("id", crd.identifyingExpressionFor(td, Cls, Cls).singleOrNull?.identifiedBy?.asString())
    }

    @Test
    fun equally_near_scopes_are_ambiguous() {
        val (crd, td) = lookup(
            """
            namespace test.Test
                scope Decl {
                    identify Cls by id
                }
                scope Member {
                    identify Cls by name
                }
            """.trimIndent()
        )
        // Cls conforms to both Decl and Member, each 1 step
        val match = crd.identifyingExpressionFor(td, Cls, Cls)
        assertTrue(match.isAmbiguous)
        assertEquals(2, match.candidates.size)
    }

    @Test
    fun equally_near_scopes_with_same_expression_are_not_ambiguous() {
        val (crd, td) = lookup(
            """
            namespace test.Test
                scope Decl {
                    identify Cls by id
                }
                scope Member {
                    identify Cls by id
                }
            """.trimIndent()
        )
        val match = crd.identifyingExpressionFor(td, Cls, Cls)
        assertEquals("id", match.singleOrNull?.identifiedBy?.asString())
    }

    @Test
    fun identify_supertype_must_own_property() {
        val (_, td) = lookup("namespace test.Test")
        val result = Agl.registry.agl.crossReference.processor!!.process(
            """
            namespace test.Test
                identify Decl by id
            """.trimIndent(),
            Agl.options { semanticAnalysis { sentenceContext(contextFromTypesDomain(td)) } }
        )
        assertTrue(result.allIssues.errors.any { it.message.contains("'id' not found for identifying property of 'Decl'") }, result.allIssues.toString())
    }

    @Test
    fun nested_scopes_from_supertype() {
        val (scope, errors) = scopeOf(
            """
            namespace test.Test
                identify Pkg by id
                identify Cls by id
                identify Fld by id
                scope Decl { }
            """.trimIndent(),
            "package p { class a : A { field f } package q { class b : B { } } }"
        )
        assertEquals(emptyList(), errors)
        // every named element is in the scope of its enclosing Pkg or Cls
        val expected = contextAsmSimple {
            forSentence(null) {
                scopedItem("p", "test.Test.Pkg", null, "/p") {
                    scopedItem("a", "test.Test.Cls", null, "/p/a") {
                        item("f", "test.Test.Fld", null, "/p/a/f")
                    }
                    scopedItem("q", "test.Test.Pkg", null, "/p/q") {
                        scopedItem("b", "test.Test.Cls", null, "/p/q/b") {}
                    }
                }
            }
        }
        assertEquals(expected.asString(), scope)
    }

    @Test
    fun ambiguous_identification_is_an_error() {
        val (_, errors) = scopeOf(
            """
            namespace test.Test
                identify Cls by id
                scope Decl {
                    identify Cls by id
                }
                scope Member {
                    identify Cls by name
                }
            """.trimIndent(),
            "class a : A { class b : B { } }"
        )
        assertTrue(errors.any { it.startsWith("Ambiguous identification of type 'test.Test.Cls'") }, errors.toString())
    }
}
