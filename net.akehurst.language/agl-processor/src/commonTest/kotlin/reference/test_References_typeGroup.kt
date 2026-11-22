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
import net.akehurst.language.reference.asm.ReferenceExpressionPropertyDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 'type-group N = A | B': a name for a list of type names, used in 'refers-to'. It is not a type.
 */
class test_References_typeGroup {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = member* ;
                member = def | block | other | use ;
                def = 'def' ID ;
                block = 'block' ID ;
                other = 'other' ID ;
                use = 'use' ID ;
                leaf ID = "[a-z]+" ;
            }
            """.trimIndent()
        )

        val processor = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun definitionErrors(references: String): List<String> {
            val res = CrossReferenceDomainDefault.fromString(contextFromTypesDomain(processor.typesDomain), CrossReferenceString(references))
            return res.allIssues.errors.map { it.message }
        }

        const val REFS = """namespace test.Test
    type-group Named = Def | Block
    identify Def by id
    identify Block by id
    identify Other by id
    references {
        in Use {
            property id refers-to Named
        }
    }"""

        fun sentenceErrors(references: String, sentence: String): List<String> {
            val p = Agl.processorFromStringSimple(grammarStr, referenceStr = CrossReferenceString(references)).let {
                assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
                it.processor!!
            }
            return p.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } }).allIssues.errors.map { it.message }
        }
    }

    @Test
    fun group__no_definition_errors() {
        assertEquals(emptyList(), definitionErrors(REFS))
    }

    @Test
    fun group__expanded_in_refers_to() {
        val res = CrossReferenceDomainDefault.fromString(contextFromTypesDomain(processor.typesDomain), CrossReferenceString(REFS))
        val re = res.asm!!.namespace[0].definition[0].references[0].referenceExpressionList[0] as ReferenceExpressionPropertyDefault
        assertEquals(listOf("Def", "Block"), re.refersToTypeName.map { it.value })
        assertEquals(listOf("Named"), re.refersToTypeNameAsWritten.map { it.value })
    }

    @Test
    fun group__resolves_to_any_of_its_types() {
        assertEquals(emptyList(), sentenceErrors(REFS, "def x block y use x use y"))
        val errors = sentenceErrors(REFS, "other z use z")
        assertEquals(1, errors.size, errors.toString())
        assertTrue(errors[0].contains("'z'"), errors.toString())
    }

    @Test
    fun group__asString_round_trip() {
        val res = CrossReferenceDomainDefault.fromString(contextFromTypesDomain(processor.typesDomain), CrossReferenceString(REFS))
        val str = res.asm!!.asString()
        assertTrue(str.contains("type-group Named = Def | Block"), str)
        assertTrue(str.contains("property id refers-to Named"), str)
        assertEquals(emptyList(), definitionErrors(str))
    }

    @Test
    fun group_using_group() {
        val refs = """
            namespace test.Test
                type-group Named = Def | Block
                type-group All = Named | Other
                references { in Use { property id refers-to All } }
            """.trimIndent()
        val res = CrossReferenceDomainDefault.fromString(contextFromTypesDomain(processor.typesDomain), CrossReferenceString(refs))
        assertEquals(emptyList(), res.allIssues.errors.map { it.message })
        val re = res.asm!!.namespace[0].definition[0].references[0].referenceExpressionList[0] as ReferenceExpressionPropertyDefault
        assertEquals(listOf("Def", "Block", "Other"), re.refersToTypeName.map { it.value })
    }

    @Test
    fun group_twice__error() {
        val errors = definitionErrors(
            """
            namespace test.Test
                type-group Named = Def
                type-group Named = Block
            """.trimIndent()
        )
        assertEquals(1, errors.size, errors.toString())
        assertTrue(errors[0].startsWith("There is more than one 'type-group Named'"), errors.toString())
    }

    @Test
    fun group_using_itself__error() {
        val errors = definitionErrors(
            """
            namespace test.Test
                type-group A = Def | B
                type-group B = A
            """.trimIndent()
        )
        assertTrue(errors.isNotEmpty() && errors.all { it.contains("uses itself") }, errors.toString())
    }
}
