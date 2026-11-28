/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.format.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** which format rule is used when several match: the most specific type wins (David FD1, FD1a) */
class test_FormatRuleSelection {

    open class Sup
    class Sub : Sup()
    interface A
    interface B
    class C : A, B
    class D : A, B

    private val types = typesDomain("Test", true) {
        namespace("net.akehurst.language.format.processor.test_FormatRuleSelection") {
            data("Sup")
            data("Sub") { supertype("Sup") }
            data("A")
            data("B")
            data("C") { supertype("A"); supertype("B") }
            data("D") { supertype("A"); supertype("B") }
        }
    }

    private fun fmt(rules: String) = FormatString("namespace net.akehurst.language.format.processor.test_FormatRuleSelection\nformat T {\n$rules\n}")

    private fun format(rules: String, self: Any) = Agl.formatByReflection(fmt(rules), types, self)

    @Test
    fun subtype_rule_wins_when_written_first() {
        val res = format("  Sub -> \"SUB\"\n  Sup -> \"SUP\"", Sub())
        assertTrue(res.issues.isEmpty(), res.issues.toString())
        assertEquals("SUB", res.sentence)
    }

    @Test
    fun subtype_rule_wins_when_written_last() {
        val res = format("  Sup -> \"SUP\"\n  Sub -> \"SUB\"", Sub())
        assertTrue(res.issues.isEmpty(), res.issues.toString())
        assertEquals("SUB", res.sentence)
    }

    @Test
    fun supertype_rule_used_for_subtype_without_rule() {
        val res = format("  Sup -> \"SUP\"", Sub())
        assertTrue(res.issues.isEmpty(), res.issues.toString())
        assertEquals("SUP", res.sentence)
    }

    @Test
    fun specific_rule_wins_over_Any() {
        val res = format("  Any -> \"ANY\"\n  Sup -> \"SUP\"", Sub())
        assertTrue(res.issues.isEmpty(), res.issues.toString())
        assertEquals("SUP", res.sentence)
    }

    @Test
    fun ambiguous_uses_first_written_and_warns() {
        val res = format("  B -> \"B\"\n  A -> \"A\"", C())
        assertEquals("B", res.sentence)
        assertEquals(1, res.issues.warnings.size, res.issues.toString())
        assertTrue(res.issues.warnings.first().message.contains("'C'"), res.issues.toString())
    }

    @Test
    fun ambiguous_own_rule_before_extended_rule() {
        val str = FormatString(
            """
            namespace net.akehurst.language.format.processor.test_FormatRuleSelection
            format Base {
              A -> "A"
            }
            format T : Base {
              B -> "B"
            }
            """
        )
        val res = Agl.formatByReflection(str, types, C())
        assertEquals("B", res.sentence)
        assertEquals(1, res.issues.warnings.size, res.issues.toString())
    }

    @Test
    fun not_ambiguous_with_rule_for_common_subtype() {
        val res = format("  A -> \"A\"\n  B -> \"B\"\n  C -> \"C\"", C())
        assertEquals("C", res.sentence)
        assertTrue(res.issues.isEmpty(), res.issues.toString())
    }

    @Test
    fun semanticAnalysis_warns_for_overlapping_rules() {
        val res = Agl.formatDomain(fmt("  A -> \"A\"\n  B -> \"B\"\n  C -> \"C\""), types)
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val w = res.allIssues.warnings
        assertEquals(1, w.size, res.allIssues.toString())
        assertTrue(w.first().message.contains("'D'") && w.first().message.contains("'C'").not(), res.allIssues.toString())
    }

    @Test
    fun semanticAnalysis_no_warning_for_related_rules() {
        val res = Agl.formatDomain(fmt("  Sup -> \"SUP\"\n  Sub -> \"SUB\"\n  Any -> \"ANY\""), types)
        assertTrue(res.allIssues.isEmpty(), res.allIssues.toString())
    }
}
