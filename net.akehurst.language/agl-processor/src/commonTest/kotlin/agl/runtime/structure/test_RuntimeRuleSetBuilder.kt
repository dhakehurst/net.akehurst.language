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

package net.akehurst.language.agl.runtime.structure

import net.akehurst.language.api.parser.ParserException
import kotlin.test.*

class test_RuntimeRuleSetBuilder {

    @Test
    fun literal() {
        val sut = RuntimeRuleSetBuilder()
        sut.literal("a")

        val actual = sut.ruleSet()

        assertNotNull(actual)
        assertEquals(1, actual.runtimeRules.size)
        assertEquals(0, actual.runtimeRules[0].number)
        assertEquals("a", actual.runtimeRules[0].value)
        assertEquals(RuntimeRuleKind.TERMINAL, actual.runtimeRules[0].kind)
        assertEquals(false, actual.runtimeRules[0].isEmptyRule)
        assertEquals(false, actual.runtimeRules[0].isPattern)
        assertEquals(false, actual.runtimeRules[0].isSkip)
        assertEquals(null, actual.runtimeRules[0].rhsOpt)
        assertFailsWith(ParserException::class) {
            actual.runtimeRules[0].rhs
        }
        assertFailsWith(ParserException::class) {
            actual.runtimeRules[0].ruleThatIsEmpty
        }
    }

    @Test
    fun pattern() {
        val sut = RuntimeRuleSetBuilder()
        sut.pattern("[a-z]")

        val actual = sut.ruleSet()

        assertNotNull(actual)
        assertEquals(0, actual.runtimeRules[0].number)
        assertEquals("[a-z]", actual.runtimeRules[0].value)
        assertEquals(RuntimeRuleKind.TERMINAL, actual.runtimeRules[0].kind)
        assertEquals(false, actual.runtimeRules[0].isEmptyRule)
        assertEquals(true, actual.runtimeRules[0].isPattern)
        assertEquals(false, actual.runtimeRules[0].isSkip)
        assertEquals(null, actual.runtimeRules[0].rhsOpt)
        assertFailsWith(ParserException::class) {
            actual.runtimeRules[0].ruleThatIsEmpty
        }
    }

    @Test
    fun empty() {
        val sut = RuntimeRuleSetBuilder()
        val ruleThatIsEmpty = RuntimeRule(sut.runtimeRuleSet.number,0, "a", "", RuntimeRuleKind.NON_TERMINAL, false, false)
        sut.rules.add(ruleThatIsEmpty)
        sut.empty(ruleThatIsEmpty)

        val actual = sut.ruleSet()

        assertNotNull(actual)
        assertEquals(1, actual.runtimeRules[1].number)
        assertEquals("§empty.a", actual.runtimeRules[1].tag)
        assertEquals(RuntimeRuleKind.TERMINAL, actual.runtimeRules[1].kind)
        assertEquals(true, actual.runtimeRules[1].isEmptyRule)
        assertEquals(false, actual.runtimeRules[1].isPattern)
        assertEquals(false, actual.runtimeRules[1].isSkip)
        assertNotNull(actual.runtimeRules[1].rhs)
        assertEquals(ruleThatIsEmpty, actual.runtimeRules[1].ruleThatIsEmpty)
    }

    @Test
    fun rule_empty() {
        val sut = RuntimeRuleSetBuilder()
        sut.rule("a").empty()

        val actual = sut.ruleSet()

        assertNotNull(actual)
        assertEquals(0, actual.runtimeRules[0].number)
        assertEquals("a", actual.runtimeRules[0].tag)
        assertEquals(RuntimeRuleKind.NON_TERMINAL, actual.runtimeRules[0].kind)
        assertEquals(false, actual.runtimeRules[0].isEmptyRule)
        assertEquals(false, actual.runtimeRules[0].isPattern)
        assertEquals(false, actual.runtimeRules[0].isSkip)
        assertEquals(RuntimeRuleRhsItemsKind.EMPTY, actual.runtimeRules[1].rhs.itemsKind)
        assertEquals(0, actual.runtimeRules[1].rhs.multiMin)
        assertEquals(0, actual.runtimeRules[1].rhs.multiMax)
        assertNotNull(actual.runtimeRules[0].emptyRuleItem)
        assertEquals(1, actual.runtimeRules[0].emptyRuleItem.number)
        assertEquals("§empty.a", actual.runtimeRules[0].emptyRuleItem.tag)
        assertEquals(RuntimeRuleKind.TERMINAL, actual.runtimeRules[0].emptyRuleItem.kind)
        assertEquals(true, actual.runtimeRules[0].emptyRuleItem.isEmptyRule)
        assertEquals(false, actual.runtimeRules[0].emptyRuleItem.isPattern)
        assertEquals(false, actual.runtimeRules[0].emptyRuleItem.isSkip)
    }

    @Test
    fun rule_concatenation() {
        val sut = RuntimeRuleSetBuilder()
        val r0 = sut.literal("a")
        val r1 = sut.literal("b")
        val r2 = sut.literal("c")
        val r3 = sut.rule("abc").concatenation(r0, r1, r2)

        val actual = sut.ruleSet()

        assertNotNull(actual)
        assertEquals(4, actual.runtimeRules.size)
        assertEquals(0, actual.runtimeRules[0].number)
        assertEquals(1, actual.runtimeRules[1].number)
        assertEquals("abc", actual.runtimeRules[3].tag)
        assertEquals(RuntimeRuleKind.NON_TERMINAL, actual.runtimeRules[3].kind)
        assertEquals(false, actual.runtimeRules[3].isEmptyRule)
        assertEquals(false, actual.runtimeRules[3].isPattern)
        assertEquals(false, actual.runtimeRules[3].isSkip)
        assertEquals(RuntimeRuleRhsItemsKind.CONCATENATION, actual.runtimeRules[3].rhs.itemsKind)
        assertEquals(0, actual.runtimeRules[3].rhs.multiMin)
        assertEquals(0, actual.runtimeRules[3].rhs.multiMax)
        assertEquals(r0, actual.runtimeRules[3].rhs.items.get(0))
        assertFailsWith(ParserException::class) {
            actual.runtimeRules[3].emptyRuleItem
        }
    }

    @Test
    fun rule_choiceEqual() {
        val sut = RuntimeRuleSetBuilder()
        val r0 = sut.literal("a")
        val r1 = sut.literal("b")
        val r2 = sut.literal("c")
        val r3 = sut.rule("abc").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY,r0, r1, r2)

        val actual = sut.ruleSet()

        assertNotNull(actual)
        assertEquals(4, actual.runtimeRules.size)
        assertEquals(0, actual.runtimeRules[0].number)
        assertEquals(1, actual.runtimeRules[1].number)
        assertEquals("abc", actual.runtimeRules[3].tag)
        assertEquals(RuntimeRuleKind.NON_TERMINAL, actual.runtimeRules[3].kind)
        assertEquals(false, actual.runtimeRules[3].isEmptyRule)
        assertEquals(false, actual.runtimeRules[3].isPattern)
        assertEquals(false, actual.runtimeRules[3].isSkip)
        assertEquals(RuntimeRuleRhsItemsKind.CHOICE, actual.runtimeRules[3].rhs.itemsKind)
        assertEquals(RuntimeRuleChoiceKind.LONGEST_PRIORITY, actual.runtimeRules[3].rhs.choiceKind)
        assertEquals(0, actual.runtimeRules[3].rhs.multiMin)
        assertEquals(0, actual.runtimeRules[3].rhs.multiMax)
        assertEquals(r0, actual.runtimeRules[3].rhs.items.get(0))
        assertFailsWith(ParserException::class) {
            actual.runtimeRules[3].emptyRuleItem
        }
    }

    @Test
    fun rule_choicePriority() {
        val sut = RuntimeRuleSetBuilder()
        val r0 = sut.literal("a")
        val r1 = sut.literal("b")
        val r2 = sut.literal("c")
        val r3 = sut.rule("abc").choice(RuntimeRuleChoiceKind.PRIORITY_LONGEST,r0, r1, r2)

        val actual = sut.ruleSet()

        assertNotNull(actual)
        assertEquals(4, actual.runtimeRules.size)
        assertEquals(0, actual.runtimeRules[0].number)
        assertEquals(1, actual.runtimeRules[1].number)
        assertEquals("abc", actual.runtimeRules[3].tag)
        assertEquals(RuntimeRuleKind.NON_TERMINAL, actual.runtimeRules[3].kind)
        assertEquals(false, actual.runtimeRules[3].isEmptyRule)
        assertEquals(false, actual.runtimeRules[3].isPattern)
        assertEquals(false, actual.runtimeRules[3].isSkip)
        assertEquals(RuntimeRuleRhsItemsKind.CHOICE, actual.runtimeRules[3].rhs.itemsKind)
        assertEquals(RuntimeRuleChoiceKind.PRIORITY_LONGEST, actual.runtimeRules[3].rhs.choiceKind)
        assertEquals(0, actual.runtimeRules[3].rhs.multiMin)
        assertEquals(0, actual.runtimeRules[3].rhs.multiMax)
        assertEquals(r0, actual.runtimeRules[3].rhs.items.get(0))
        assertFailsWith(ParserException::class) {
            actual.runtimeRules[3].emptyRuleItem
        }
    }

    @Test
    fun rule_multi() {
        val sut = RuntimeRuleSetBuilder()
        val r0 = sut.literal("a")
        val r1 = sut.rule("abc").multi(1, -1, r0)

        val actual = sut.ruleSet()

        assertNotNull(actual)
        assertEquals(2, actual.runtimeRules.size)
        assertEquals(0, actual.runtimeRules[0].number)
        assertEquals(1, actual.runtimeRules[1].number)
        assertEquals("abc", actual.runtimeRules[1].tag)
        assertEquals(RuntimeRuleKind.NON_TERMINAL, actual.runtimeRules[1].kind)
        assertEquals(false, actual.runtimeRules[1].isEmptyRule)
        assertEquals(false, actual.runtimeRules[1].isPattern)
        assertEquals(false, actual.runtimeRules[1].isSkip)
        assertEquals(RuntimeRuleRhsItemsKind.LIST, actual.runtimeRules[1].rhs.itemsKind)
        assertEquals(RuntimeRuleListKind.MULTI, actual.runtimeRules[1].rhs.listKind)
        assertEquals(1, actual.runtimeRules[1].rhs.multiMin)
        assertEquals(-1, actual.runtimeRules[1].rhs.multiMax)
        assertEquals(r0, actual.runtimeRules[1].rhs.items.get(0))
        assertFailsWith(ParserException::class) {
            actual.runtimeRules[1].emptyRuleItem
        }
    }

    @Test
    fun add_rules_before_build() {
        val sut = RuntimeRuleSetBuilder()

        val actual = sut.ruleSet()

        assertNotNull(actual)
        assertFailsWith(ParserException::class) {
            sut.literal("a")
        }
        assertFailsWith(ParserException::class) {
            sut.pattern("[a-z]")
        }
        assertFailsWith(ParserException::class) {
            sut.rule("a").empty()
        }

    }
}