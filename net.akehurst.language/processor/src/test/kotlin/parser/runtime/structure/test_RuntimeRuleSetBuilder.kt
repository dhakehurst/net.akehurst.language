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

package net.akehurst.language.ogl.runtime.structure

import net.akehurst.language.api.parser.ParseException
import kotlin.test.*

class test_RuntimeRuleSetBuilder {

    @Test
    fun literal() {
        val sut = RuntimeRuleSetBuilder()
        sut.literal("a")

        val actual = sut.ruleSet()

        assertNotNull(actual)
        assertEquals(1,actual.rules.size)
        assertEquals(0,actual.rules[0].number)
        assertEquals("a", actual.rules[0].name)
        assertEquals(RuntimeRuleKind.TERMINAL, actual.rules[0].kind)
        assertEquals(false, actual.rules[0].isEmptyRule)
        assertEquals(false, actual.rules[0].isPattern)
        assertEquals(false, actual.rules[0].isSkip)
        assertEquals(null, actual.rules[0].rhs)
        assertFailsWith(ParseException::class) {
            actual.rules[0].ruleThatIsEmpty
        }
    }

    @Test
    fun pattern() {
        val sut = RuntimeRuleSetBuilder()
        sut.pattern("[a-z]")

        val actual = sut.ruleSet()

        assertNotNull(actual)
        assertEquals(0,actual.rules[0].number)
        assertEquals("[a-z]", actual.rules[0].name)
        assertEquals(RuntimeRuleKind.TERMINAL, actual.rules[0].kind)
        assertEquals(false, actual.rules[0].isEmptyRule)
        assertEquals(true, actual.rules[0].isPattern)
        assertEquals(false, actual.rules[0].isSkip)
        assertEquals(null, actual.rules[0].rhs)
        assertFailsWith(ParseException::class) {
            actual.rules[0].ruleThatIsEmpty
        }
    }

    @Test
    fun empty() {
        val sut = RuntimeRuleSetBuilder()
        val ruleThatIsEmpty = RuntimeRule(0,"a", RuntimeRuleKind.NON_TERMINAL, false, false)
        sut.rules.add(ruleThatIsEmpty)
        sut.empty(ruleThatIsEmpty)

        val actual = sut.ruleSet()

        assertNotNull(actual)
        assertEquals(1,actual.rules[1].number)
        assertEquals("${'$'}empty.a", actual.rules[1].name)
        assertEquals(RuntimeRuleKind.TERMINAL, actual.rules[1].kind)
        assertEquals(true, actual.rules[1].isEmptyRule)
        assertEquals(false, actual.rules[1].isPattern)
        assertEquals(false, actual.rules[1].isSkip)
        assertNotNull(actual.rules[1].rhs)
        assertEquals(ruleThatIsEmpty,actual.rules[1].ruleThatIsEmpty)
    }

    @Test
    fun rule_empty() {
        val sut = RuntimeRuleSetBuilder()
        sut.rule("a").empty()

        val actual = sut.ruleSet()

        assertNotNull(actual)
        assertEquals(0,actual.rules[0].number)
        assertEquals("a", actual.rules[0].name)
        assertEquals(RuntimeRuleKind.NON_TERMINAL, actual.rules[0].kind)
        assertEquals(false, actual.rules[0].isEmptyRule)
        assertEquals(false, actual.rules[0].isPattern)
        assertEquals(false, actual.rules[0].isSkip)
        assertEquals(RuntimeRuleItemKind.EMPTY,actual.rules[1].rhs?.kind)
        assertEquals(0,actual.rules[1].rhs?.multiMin)
        assertEquals(0,actual.rules[1].rhs?.multiMax)
        assertNotNull(actual.rules[0].emptyRule)
        assertEquals(1,actual.rules[0].emptyRule.number)
        assertEquals("${'$'}empty.a", actual.rules[0].emptyRule.name)
        assertEquals(RuntimeRuleKind.TERMINAL, actual.rules[0].emptyRule.kind)
        assertEquals(true, actual.rules[0].emptyRule.isEmptyRule)
        assertEquals(false, actual.rules[0].emptyRule.isPattern)
        assertEquals(false, actual.rules[0].emptyRule.isSkip)
    }

    @Test
    fun rule_concatenation() {
        val sut = RuntimeRuleSetBuilder()
        val r0 = sut.literal("a")
        val r1 = sut.literal("b")
        val r2 = sut.literal("c")
        val r3 = sut.rule("abc").concatenation(r0, r1, r2).build()

        val actual = sut.ruleSet()

        assertNotNull(actual)
        assertEquals(4, actual.rules.size)
        assertEquals(0,actual.rules[0].number)
        assertEquals(1,actual.rules[1].number)
        assertEquals("abc", actual.rules[3].name)
        assertEquals(RuntimeRuleKind.NON_TERMINAL, actual.rules[3].kind)
        assertEquals(false, actual.rules[3].isEmptyRule)
        assertEquals(false, actual.rules[3].isPattern)
        assertEquals(false, actual.rules[3].isSkip)
        assertEquals(RuntimeRuleItemKind.CONCATENATION,actual.rules[3].rhs?.kind)
        assertEquals(0,actual.rules[3].rhs?.multiMin)
        assertEquals(0,actual.rules[3].rhs?.multiMax)
        assertEquals(r0,actual.rules[3].rhs?.items?.get(0))
        assertFailsWith(ParseException::class) {
            actual.rules[3].emptyRule
        }
    }

    @Test
    fun rule_choiceEqual() {
        val sut = RuntimeRuleSetBuilder()
        val r0 = sut.literal("a")
        val r1 = sut.literal("b")
        val r2 = sut.literal("c")
        val r3 = sut.rule("abc").choiceEqual(r0, r1, r2).build()

        val actual = sut.ruleSet()

        assertNotNull(actual)
        assertEquals(4, actual.rules.size)
        assertEquals(0,actual.rules[0].number)
        assertEquals(1,actual.rules[1].number)
        assertEquals("abc", actual.rules[3].name)
        assertEquals(RuntimeRuleKind.NON_TERMINAL, actual.rules[3].kind)
        assertEquals(false, actual.rules[3].isEmptyRule)
        assertEquals(false, actual.rules[3].isPattern)
        assertEquals(false, actual.rules[3].isSkip)
        assertEquals(RuntimeRuleItemKind.CHOICE_EQUAL,actual.rules[3].rhs?.kind)
        assertEquals(0,actual.rules[3].rhs?.multiMin)
        assertEquals(0,actual.rules[3].rhs?.multiMax)
        assertEquals(r0,actual.rules[3].rhs?.items?.get(0))
        assertFailsWith(ParseException::class) {
            actual.rules[3].emptyRule
        }
    }

    @Test
    fun rule_choicePriority() {
        val sut = RuntimeRuleSetBuilder()
        val r0 = sut.literal("a")
        val r1 = sut.literal("b")
        val r2 = sut.literal("c")
        val r3 = sut.rule("abc").choicePriority(r0, r1, r2).build()

        val actual = sut.ruleSet()

        assertNotNull(actual)
        assertEquals(4, actual.rules.size)
        assertEquals(0,actual.rules[0].number)
        assertEquals(1,actual.rules[1].number)
        assertEquals("abc", actual.rules[3].name)
        assertEquals(RuntimeRuleKind.NON_TERMINAL, actual.rules[3].kind)
        assertEquals(false, actual.rules[3].isEmptyRule)
        assertEquals(false, actual.rules[3].isPattern)
        assertEquals(false, actual.rules[3].isSkip)
        assertEquals(RuntimeRuleItemKind.CHOICE_PRIORITY,actual.rules[3].rhs?.kind)
        assertEquals(0,actual.rules[3].rhs?.multiMin)
        assertEquals(0,actual.rules[3].rhs?.multiMax)
        assertEquals(r0,actual.rules[3].rhs?.items?.get(0))
        assertFailsWith(ParseException::class) {
            actual.rules[3].emptyRule
        }
    }

    @Test
    fun add_rules_before_build() {
        val sut = RuntimeRuleSetBuilder()

        val actual = sut.ruleSet()

        assertNotNull(actual)
        assertFailsWith(ParseException::class) {
            sut.literal("a")
        }
        assertFailsWith(ParseException::class) {
            sut.pattern("[a-z]")
        }
        assertFailsWith(ParseException::class) {
            sut.rule("a").empty()
        }

    }
}