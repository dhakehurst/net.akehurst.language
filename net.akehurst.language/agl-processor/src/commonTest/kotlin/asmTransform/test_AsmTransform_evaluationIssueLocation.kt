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

package net.akehurst.language.asmTransform

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.AsmTransformString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.issues.api.LanguageIssueKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * issue #20, ER7: an issue found while evaluating a transform rule on a sentence points at the part of the sentence
 * being transformed, and its message quotes the expression and names the grammar rule.
 */
class test_AsmTransform_evaluationIssueLocation {

    private companion object {
        val grammarStr = """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                S = 'a' item ;
                item = NAME NUM ;
                leaf NAME = "[a-z]+" ;
                leaf NUM = "[0-9]+" ;
            }
        """.trimIndent()

        fun issuesFor(rules: String, sentence: String) = run {
            val tr = "#override-default-transform\n#create-missing-types\nnamespace test\nasm-transform Test {\n$rules\n}"
            val pr = Agl.processorFromStringSimple(GrammarString(grammarStr), transformStr = AsmTransformString(tr))
            assertTrue(pr.issues.errors.isEmpty(), pr.issues.toString())
            pr.processor!!.process(sentence).allIssues.filter { it.kind != LanguageIssueKind.INFORMATION }
        }
    }

    @Test
    fun index_out_of_range__error_at_transformed_node() {
        val issues = issuesFor("  item: X() { n := child[7] }", "a bob 12")
        assertEquals(1, issues.size, issues.joinToString("\n"))
        val iss = issues[0]
        assertEquals(LanguageIssueKind.ERROR, iss.kind)
        assertTrue(iss.message.contains("in the expression 'X() { n := child[7] }'"), iss.message)
        assertTrue(iss.message.contains("grammar rule 'item'"), iss.message)
        val loc = assertNotNull(iss.location, iss.toString())
        // 'bob 12', the text of 'item'
        assertEquals(2, loc.position)
        assertEquals(3, loc.column)
    }

    @Test
    fun property_on_nothing__warning_at_transformed_node() {
        val issues = issuesFor("  item: X() { n := \$nothing.foo }", "a bob 12")
        assertEquals(1, issues.size, issues.joinToString("\n"))
        val iss = issues[0]
        assertEquals(LanguageIssueKind.WARNING, iss.kind)
        assertTrue(iss.message.contains("in the expression 'X() { n := \$nothing.foo }'"), iss.message)
        assertNotNull(iss.location, iss.toString())
    }

}
