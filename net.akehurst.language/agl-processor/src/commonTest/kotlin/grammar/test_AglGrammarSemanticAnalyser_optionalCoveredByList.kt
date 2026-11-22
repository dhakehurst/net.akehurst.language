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

package net.akehurst.language.grammar.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import kotlin.test.Test
import kotlin.test.assertEquals

class test_AglGrammarSemanticAnalyser_optionalCoveredByList {

    private companion object {
        fun coveredWarnings(rules: String): List<String> {
            val grammar = "namespace test grammar Test {\n$rules\n}"
            val res = Agl.processorFromStringSimple(GrammarString(grammar))
            return res.issues.warnings.map { it.message }.filter { it.contains("can never match") }
        }
    }

    @Test
    fun literal_covered_by_pattern() {
        val w = coveredWarnings(
            """
            S = 'b' vs oa ;
            vs = v+ ;
            oa = 'a'? ;
            leaf v = "[a-z]" ;
            """
        )
        assertEquals(listOf("In rule 'S', 'oa' can never match anything: lists are greedy and the list 'vs' before it matches everything 'oa' can contain."), w)
    }

    @Test
    fun nonTerminal_content_covered() {
        // optional content 'k' vList, all terminals matched by v
        val w = coveredWarnings(
            """
            S = 'b' vList kvlOpt ;
            vList = v* ;
            kvlOpt = kvl? ;
            kvl = 'k' vList ;
            leaf v = "[a-z]" ;
            """
        )
        assertEquals(1, w.size, w.toString())
    }

    @Test
    fun list_with_minimum_0_covered() {
        val w = coveredWarnings(
            """
            S = 'b' vs as ;
            vs = v+ ;
            as = 'a'* ;
            leaf v = "[a-z]" ;
            """
        )
        assertEquals(1, w.size, w.toString())
    }

    @Test
    fun inline_list_and_optional_covered() {
        val w = coveredWarnings(
            """
            S = 'b' "[a-z]"* 'a'? ;
            """
        )
        assertEquals(1, w.size, w.toString())
    }

    @Test
    fun covered_with_covered_optional_in_between() {
        val w = coveredWarnings(
            """
            S = 'b' vs xo oa ;
            vs = v+ ;
            xo = 'x'? ;
            oa = 'a'? ;
            leaf v = "[a-z]" ;
            """
        )
        assertEquals(
            listOf(
                "In rule 'S', 'xo' can never match anything: lists are greedy and the list 'vs' before it matches everything 'xo' can contain.",
                "In rule 'S', 'oa' can never match anything: lists are greedy and the list 'vs' before it matches everything 'oa' can contain."
            ), w
        )
    }

    @Test
    fun not_covered_after_optional_not_covered() {
        // ';'? can match, so 'a'? after it can too
        val w = coveredWarnings(
            """
            S = 'b' vs so oa ;
            vs = v+ ;
            so = ';'? ;
            oa = 'a'? ;
            leaf v = "[a-z]" ;
            """
        )
        assertEquals(emptyList(), w)
    }

    @Test
    fun not_covered_other_terminal() {
        val w = coveredWarnings(
            """
            S = 'b' vs oa ;
            vs = v+ ;
            oa = '1'? ;
            leaf v = "[a-z]" ;
            """
        )
        assertEquals(emptyList(), w)
    }

    @Test
    fun not_covered_some_terminal_not_matched() {
        val w = coveredWarnings(
            """
            S = 'b' vs oa ;
            vs = v+ ;
            oa = ('a' ';')? ;
            leaf v = "[a-z]" ;
            """
        )
        assertEquals(emptyList(), w)
    }

    @Test
    fun not_covered_list_with_maximum() {
        val w = coveredWarnings(
            """
            S = 'b' vs oa ;
            vs = v2..3 ;
            oa = 'a'? ;
            leaf v = "[a-z]" ;
            """
        )
        assertEquals(emptyList(), w)
    }

    @Test
    fun not_covered_not_directly_after() {
        val w = coveredWarnings(
            """
            S = 'b' vs ';' oa ;
            vs = v+ ;
            oa = 'a'? ;
            leaf v = "[a-z]" ;
            """
        )
        assertEquals(emptyList(), w)
    }

    @Test
    fun not_covered_list_item_not_a_terminal() {
        val w = coveredWarnings(
            """
            S = 'b' vs oa ;
            vs = x+ ;
            x = v ';' ;
            oa = 'a'? ;
            leaf v = "[a-z]" ;
            """
        )
        assertEquals(emptyList(), w)
    }
}
