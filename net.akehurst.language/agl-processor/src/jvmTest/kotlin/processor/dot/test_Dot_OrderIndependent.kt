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

package net.akehurst.language.agl.processor.dot

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.Asm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * In dot, ID also matches 'subgraph', so `subgraph cluster_1 { ... }` can also be parsed as
 * node_stmt 'subgraph', node_stmt 'cluster_1' and an anonymous subgraph (same stmt_list, same span).
 * The merge decision can not decide between them. The tree must not depend on what the processor parsed before,
 * and the subgraph parse (longer first differing stmt1) must win.
 */
class test_Dot_OrderIndependent {

    private companion object {
        val grammarStr = this::class.java.getResource("/dot/version_9.0.0/grammar.agl")!!.readText()
        fun sentence(name: String) = this::class.java.getResource("/dot/version_9.0.0/valid/$name")!!.readText()
        fun processor(): LanguageProcessor<Asm, SentenceContext> = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
    }

    private fun parse(p: LanguageProcessor<Asm, SentenceContext>, sentence: String): String {
        val result = p.parse(sentence)
        assertNotNull(result.sppt, result.issues.toString())
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        return result.sppt!!.toStringAllWithIndent("  ", true)
    }

    @Test
    fun cluster_sameTree_whateverParsedBefore() {
        val cluster = sentence("Cluster.dot")
        val fresh = parse(processor(), cluster)
        assertTrue(fresh.contains("SUBGRAPH : 'subgraph'"), "expected 'subgraph' to be parsed as the SUBGRAPH keyword")
        assertTrue(fresh.contains("ALPHABETIC_ID : 'subgraph'").not(), "'subgraph' must not be parsed as a node ID")
        for (before in listOf("Genetic_Programming.dot", "Synchronous_Digital_Hierarchy_Stack.dot")) {
            val p = processor()
            parse(p, sentence(before))
            assertEquals(fresh, parse(p, cluster), "different tree after parsing $before")
        }
    }
}
