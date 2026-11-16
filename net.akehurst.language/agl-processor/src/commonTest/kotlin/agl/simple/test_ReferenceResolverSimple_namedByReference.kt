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
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.asm.api.AsmStructure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * - two children of one concatenation with the same rule ('link' ref 'to' ref) are different elements (their parse paths differ)
 * - a reference does not resolve to an element whose name is computed from that same reference ('perform tp' is named 'tp')
 */
class test_ReferenceResolverSimple_namedByReference {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = element* ;
                element = def | perform | link | use ;
                def = 'def' ID body? ;
                body = '{' element* '}' ;
                perform = 'perform' ID ;
                link = 'link' ref 'to' ref ;
                ref = ID ;
                use = 'use' ID ':' ID ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
            """.trimIndent()
        )
        val referenceStr = CrossReferenceString(
            """
            namespace test.Test
                identify Def by id
                identify Perform by id
                identify Use by id
                scope Def { }
                references {
                    in Perform { property id refers-to Def|Perform }
                    in Ref { property id refers-to Def|Perform }
                    in Use { property id2 refers-to Def|Perform }
                }
            """.trimIndent()
        )
        val processor = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = referenceStr).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun test(sentence: String, expectedErrors: List<String> = emptyList()) = processor.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } }).also { result ->
            assertTrue(result.allIssues.errors.none { it.phase.name != "SEMANTIC_ANALYSIS" }, result.allIssues.toString())
            assertEquals(expectedErrors, result.allIssues.errors.map { it.message })
        }
    }

    @Test
    fun same_rule_children__different_parse_paths() {
        val result = test("def A def B link A to B")
        val link = (result.asm!!.root[0] as AsmStructure).getProperty(net.akehurst.language.asm.api.PropertyValueName("element")).let { (it as List<*>)[2] as AsmStructure }
        val r1 = link.getProperty(net.akehurst.language.asm.api.PropertyValueName("ref")) as AsmStructure
        val r2 = link.getProperty(net.akehurst.language.asm.api.PropertyValueName("ref2")) as AsmStructure
        assertNotEquals(r1.parsePath, r2.parsePath)
        assertNotEquals(r1, r2)
    }

    @Test
    fun same_rule_children__second_is_resolved() = test(
        "def A link A to B",
        listOf("Reference 'B' not resolved, to type(s) [Def, Perform] in scope '/'")
    ).let { }

    @Test
    fun named_by_own_reference__resolves_outwards() = test(
        "def tp def p { perform tp }"
    ).let { }

    @Test
    fun named_by_own_reference__not_to_itself() = test(
        "def p { perform tp }",
        listOf("Reference 'tp' not resolved, to type(s) [Def, Perform] in scope '/p'")
    ).let { }

    @Test
    fun other_reference__can_resolve_to_element_named_by_reference() = test(
        "def tp def p { perform tp use u : tp }"
    ).let { }
}
