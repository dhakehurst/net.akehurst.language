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

package net.akehurst.language.agl.grammar.style

import net.akehurst.language.agl.Agl
import net.akehurst.language.style.api.AglStyleSelector
import net.akehurst.language.style.api.AglStyleSelectorKind
import net.akehurst.language.style.api.AglStyleTagRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * semantic selectors: 'type T', 'identify T', 'refers-to T'.
 * 'type', 'identify' and 'refers-to' alone are still rule names.
 */
class test_AglStyle_semanticSelectors {

    private fun selectorsOf(rule: String): List<AglStyleSelector> {
        val res = Agl.registry.agl.style.processor!!.process("namespace n\nstyles X {\n  $rule\n}")
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        return (res.asm!!.allDefinitions.single().rules.single() as AglStyleTagRule).selector
    }

    private fun roundTrip(rule: String) {
        val res = Agl.registry.agl.style.processor!!.process("namespace n\nstyles X {\n  $rule\n}")
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val again = Agl.registry.agl.style.processor!!.process(res.asm!!.asString())
        assertTrue(again.allIssues.errors.isEmpty(), again.allIssues.toString())
        assertEquals(res.asm!!.asString(), again.asm!!.asString())
    }

    @Test
    fun type() = assertEquals(listOf(AglStyleSelector("type Definition", AglStyleSelectorKind.TYPE)), selectorsOf("type Definition { foreground: blue; }"))

    @Test
    fun identify_qualified() = assertEquals(listOf(AglStyleSelector("identify a.b.PartDefinition", AglStyleSelectorKind.IDENTIFY)), selectorsOf("identify a.b.PartDefinition { font-weight: bold; }"))

    @Test
    fun refers_to() = assertEquals(listOf(AglStyleSelector("refers-to Definition", AglStyleSelectorKind.REFERS_TO)), selectorsOf("refers-to Definition { foreground: green; }"))

    @Test
    fun keywords_alone_are_rule_names() = assertEquals(
        listOf(
            AglStyleSelector("type", AglStyleSelectorKind.RULE_NAME),
            AglStyleSelector("identify", AglStyleSelectorKind.RULE_NAME),
            AglStyleSelector("refers-to", AglStyleSelectorKind.RULE_NAME)
        ),
        selectorsOf("type, identify, refers-to { foreground: red; }")
    )

    @Test
    fun type_named_like_a_keyword() = assertEquals(listOf(AglStyleSelector("type type", AglStyleSelectorKind.TYPE)), selectorsOf("type type { foreground: red; }"))

    @Test
    fun mixed_list() = assertEquals(
        listOf(
            AglStyleSelector("refers-to Def", AglStyleSelectorKind.REFERS_TO),
            AglStyleSelector("type", AglStyleSelectorKind.RULE_NAME),
            AglStyleSelector("NAME", AglStyleSelectorKind.RULE_NAME),
            AglStyleSelector("'if'", AglStyleSelectorKind.LITERAL)
        ),
        selectorsOf("refers-to Def, type, NAME, 'if' { foreground: red; }")
    )

    @Test
    fun asString_round_trip() = roundTrip("type Def, identify a.Def, refers-to Def { foreground: red; }")
}
