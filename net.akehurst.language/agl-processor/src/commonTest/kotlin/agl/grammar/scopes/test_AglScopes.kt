/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.agl.grammar.scopes

import net.akehurst.language.agl.grammar.style.test_AglStyle
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.asm.asmSimple
import net.akehurst.language.api.style.AglStyleRule
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_AglScopes {

    private companion object {
        val aglProc = Agl.registry.agl.scopes.processor!!
    }

    @Test
    fun single_line_comment() {

        val text = """
            // single line comment
            references { }
        """.trimIndent()

        val (asm,issues) = aglProc.process<ScopeModel, Any>(text,AglScopesGrammar.goalRuleName)

        val expected = ScopeModel()

        assertEquals(expected.scopes, asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.identifiables }, asm?.scopes?.flatMap { it.identifiables })
        assertEquals(expected.references, asm?.references)
        assertEquals(emptyList(),issues)
    }

    @Test
    fun multi_line_comment() {

        val text = """
            /* multi
               line
               comment
            */
            references { }
        """.trimIndent()

        val (asm,issues) = aglProc.process<ScopeModel, Any>(text,AglScopesGrammar.goalRuleName)

        val expected = ScopeModel()

        assertEquals(expected.scopes, asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.identifiables }, asm?.scopes?.flatMap { it.identifiables })
        assertEquals(expected.references, asm?.references)
        assertEquals(emptyList(),issues)
    }

    @Test
    fun one_empty_scope() {
        val text = """
            scope ruleName { }
            references { }
        """.trimIndent()

        val (asm,issues) = aglProc.process<ScopeModel, Any>(text,AglScopesGrammar.goalRuleName)

        val expected = ScopeModel().apply {
            scopes.add(Scope("ruleName"))
        }

        assertEquals(expected.scopes, asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.identifiables }, asm?.scopes?.flatMap { it.identifiables })
        assertEquals(expected.references, asm?.references)
        assertEquals(emptyList(),issues)
    }

    @Test
    fun scope_one_identifiable() {
        val text = """
            scope ruleName {
                identify type by prop
            }
            references { }
        """.trimIndent()

        val (asm,issues) = aglProc.process<ScopeModel, Any>(text,AglScopesGrammar.goalRuleName)

        val expected = ScopeModel().apply {
            scopes.add(Scope("ruleName").apply {
                identifiables.add(Identifiable("type","prop"))
            })
        }

        assertEquals(expected.scopes, asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.identifiables }, asm?.scopes?.flatMap { it.identifiables })
        assertEquals(expected.references, asm?.references)
        assertEquals(emptyList(),issues)
    }

    @Test
    fun one_reference() {
        val text = """
            references {
                in type1 property prop refers-to type2
            }
        """.trimIndent()

        val (asm,issues) = aglProc.process<ScopeModel, Any>(text,AglScopesGrammar.goalRuleName)

        val expected = ScopeModel().apply {
            references.add(ReferenceDefinition("type1","prop",listOf("type2")))
        }

        assertEquals(expected.scopes, asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.identifiables }, asm?.scopes?.flatMap { it.identifiables })
        assertEquals(expected.references, asm?.references)
        assertEquals(emptyList(),issues)
    }

    //TODO more checks + check rules (types/properties) exist in context of grammar
}