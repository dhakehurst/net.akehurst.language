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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A choice between reference targets: 'property p when { c -> refers-to A ... else -> refers-to B ... }'.
 * A property has at most one 'property' line per block, and a type at most one 'in' block and one 'scope' block per namespace.
 */
class test_References_propertyWhen {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = member* ;
                member = def | block | use ;
                def = 'def' ID ;
                block = 'block' ID ;
                use = 'use' kind ID ;
                kind = 'd' | 'b' ;
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

        const val REFS_WHEN = """namespace test.Test
    identify Def by id
    identify Block by id
    references {
        in Use {
            property id when {
                kind == 'd' -> refers-to Def
                else -> refers-to Block
            }
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
    fun when_block__no_definition_errors() {
        assertEquals(emptyList(), definitionErrors(REFS_WHEN))
    }

    @Test
    fun when_block__first_option_whose_condition_holds_is_used() {
        assertEquals(emptyList(), sentenceErrors(REFS_WHEN, "def x block y use d x use b y"))
        // 'use d y': y is a Block, but the first option (kind == 'd') refers to a Def
        val errors = sentenceErrors(REFS_WHEN, "def x block y use d y")
        assertEquals(1, errors.size, errors.toString())
        assertTrue(errors[0].contains("'y'"), errors.toString())
    }

    @Test
    fun when_block__asString() {
        val res = CrossReferenceDomainDefault.fromString(contextFromTypesDomain(processor.typesDomain), CrossReferenceString(REFS_WHEN))
        val str = res.asm!!.asString()
        assertTrue(str.contains("property id when {"), str)
        assertTrue(str.contains("kind == 'd' -> refers-to Def"), str)
        assertTrue(str.contains("else -> refers-to Block"), str)
        // round trip
        assertEquals(emptyList(), definitionErrors(str))
    }

    @Test
    fun property_twice_in_a_block__error() {
        val errors = definitionErrors(
            """
            namespace test.Test
                identify Def by id
                references {
                    in Use {
                        property id refers-to Def when kind == 'd'
                        property id refers-to Def
                    }
                }
            """.trimIndent()
        )
        assertEquals(1, errors.size, errors.toString())
        assertTrue(errors[0].startsWith("For references in 'Use', property 'id' has more than one 'property' line"), errors.toString())
    }

    @Test
    fun two_in_blocks_for_a_type__error() {
        val errors = definitionErrors(
            """
            namespace test.Test
                identify Def by id
                references {
                    in Use { property id refers-to Def }
                    in Use { property kind refers-to Def }
                }
            """.trimIndent()
        )
        assertEquals(1, errors.size, errors.toString())
        assertTrue(errors[0].startsWith("There is more than one 'in Use { }' block in namespace 'test.Test'"), errors.toString())
    }

    @Test
    fun two_scope_blocks_for_a_type__error() {
        val errors = definitionErrors(
            """
            namespace test.Test
                scope Block { identify Def by id }
                scope Block { }
            """.trimIndent()
        )
        assertEquals(1, errors.size, errors.toString())
        assertTrue(errors[0].startsWith("There is more than one 'scope Block { }' block in namespace 'test.Test'"), errors.toString())
    }
}
