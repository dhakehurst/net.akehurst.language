/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.ogl.grammar

import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.processor.processor
import kotlin.test.Test
import kotlin.test.assertNotNull

class test_OglGrammar_item {

    private fun parse(goalRule:String, inputText: CharSequence): SharedPackedParseTree {
        val grammar = OglGrammar()
        val lp = processor(grammar)
        return lp.parse(goalRule, inputText)
    }

    @Test
    fun IDENTIFIER() {
        val actual = parse("IDENTIFIER", "a")
        assertNotNull(actual)
    }

    @Test
    fun literal() {
        val actual = parse("LITERAL", "'a'")
        assertNotNull(actual)
    }

    @Test
    fun pattern() {
        val actual = parse("PATTERN", "\"[a-c]\"")
        assertNotNull(actual)
    }

    @Test
    fun terminal_literal() {
        val actual = parse("terminal", "'a'")
        assertNotNull(actual)
    }
    @Test
    fun terminal_pattern() {
        val actual = parse("terminal", "\"[a-c]\"")
        assertNotNull(actual)
    }
    @Test
    fun qualifiedName_1() {
        val actual = parse("qualifiedName", "a")
        assertNotNull(actual)
    }
    @Test
    fun qualifiedName_2() {
        val actual = parse("qualifiedName", "a.b")
        assertNotNull(actual)
    }
    @Test
    fun qualifiedName_3() {
        val actual = parse("qualifiedName", "a.b.c")
        assertNotNull(actual)
    }
    @Test
    fun concatenation_literal_1() {
        val actual = parse("concatenation", "'a'")
        assertNotNull(actual)
    }
    @Test
    fun concatenation_literal_3() {
        val actual = parse("concatenation", "'a' 'a' 'a'")
        assertNotNull(actual)
    }
    @Test
    fun simpleItem_literal() {
        val actual = parse("simpleItem", "'a'")
        assertNotNull(actual)
    }
    @Test
    fun simpleItem_nonTerminal() {
        val actual = parse("simpleItem", "a")
        assertNotNull(actual)
    }
    @Test
    fun multiplicity_0_n() {
        val actual = parse("multiplicity", "*")
        assertNotNull(actual)
    }
    @Test
    fun multiplicity_0_1() {
        val actual = parse("multiplicity", "?")
        assertNotNull(actual)
    }
    @Test
    fun multiplicity_1_n() {
        val actual = parse("multiplicity", "+")
        assertNotNull(actual)
    }
    @Test
    fun multi_literal() {
        val actual = parse("multi", "'a'*")
        assertNotNull(actual)
    }
    @Test
    fun multi_nonTerminal() {
        val actual = parse("multi", "a*")
        assertNotNull(actual)
    }
    @Test
    fun concatenationItem_literal() {
        val actual = parse("concatenationItem", "'a'")
        assertNotNull(actual)
    }
    @Test
    fun concatenationItem_nonTermianal() {
        val actual = parse("concatenationItem", "a")
        assertNotNull(actual)
    }
    @Test
    fun concatenation_nonTerminal_1() {
        val actual = parse("concatenation", "a")
        assertNotNull(actual)
    }
    @Test
    fun concatenation_nonTerminal_3() {
        val actual = parse("concatenation", "a b c")
        assertNotNull(actual)
    }
}
