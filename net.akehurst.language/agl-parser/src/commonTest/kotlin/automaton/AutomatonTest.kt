/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.automaton.leftcorner

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetTest.matches
import net.akehurst.language.automaton.api.Automaton
import net.akehurst.language.collections.CollectionsTest.matches

//FIXME: REPEAT - because no MPP test-fixtures
object AutomatonTest {

    data class MatchConfiguration(
        val in_actual_substitue_lookahead_RT_with: Set<RuntimeRule>? = null,
        val no_lookahead_compare: Boolean = false
    )

    private fun <E> Set<E>.replace(anyOf: Set<E>, withReplacement: Set<E>) = this.flatMap {
        if (anyOf.contains(it)) {
            withReplacement
        } else {
            setOf(it)
        }
    }.toSet()


    fun <O, E> assertMatches(expectedObject: O, actualObject: O, setName: String, expected: Set<E>, actual: Set<E>, matches: (t: E, o: E) -> Boolean) {
        val thisList = expected.toList()
        val foundThis = mutableListOf<E>()
        val foundOther = mutableListOf<E>()
        for (i in expected.indices) {
            val thisElement = thisList[i]
            val otherElement = actual.firstOrNull { matches(thisElement, it) }
            if (null != otherElement) {
                foundThis.add(thisElement)
                foundOther.add(otherElement)
            }
        }
        when {
            expected.size > foundThis.size -> kotlin.test.fail(
                "Elements of $setName do not match for,\nexpected: $expectedObject\n  $expected\nactual: $actualObject\n  $actual\nmissing:\n${
                    (expected - foundThis).joinToString(
                        separator = "\n"
                    ) { "  $it" }
                }"
            )

            actual.size > foundOther.size -> kotlin.test.fail(
                "Elements of $setName do not match for,\nexpected: $expectedObject\n  $expected\nactual: $actualObject\n  $actual\nmissing:\n${
                    (actual - foundOther).joinToString(
                        separator = "\n"
                    ) { "  $it" }
                }"
            )

            else -> Unit
        }
    }

    fun assertEquals(expected: Automaton, actual: Automaton) {
        assertMatches(expected as ParserStateSet, actual as ParserStateSet, MatchConfiguration())
    }

    fun assertMatches(expected: ParserStateSet, actual: ParserStateSet, config: MatchConfiguration = MatchConfiguration()) {
        val expected_states = expected.allBuiltStates.toSet()
        val actual_states = actual.allBuiltStates.toSet()

        assertMatches(expected, actual, "allBuiltStates", expected_states, actual_states) { t, o -> t.matches(o) }
        assertMatches(expected, actual, "allBuiltTransitions", expected.allBuiltTransitions.toSet(), actual.allBuiltTransitions.toSet()) { t, o -> t.matches(o, config) }

        for (exp_state in expected_states) {
            val act_state = actual_states.first { it.matches(exp_state) }
            for (exp_trans in exp_state.outTransitions.allBuiltTransitions) {
                val act_trans = act_state.outTransitions.allBuiltTransitions.first { exp_trans.matches(it, config) }
                assertMatches(exp_trans, act_trans, "context", exp_trans.context, act_trans.context) { t, o -> t.matches(o) }
            }
        }
    }

    private fun ParserState.matches(other: ParserState): Boolean = this.rulePositions.toSet().matches(other.rulePositions.toSet()) { t, o -> t.matches(o) }
    private fun Transition.matches(other: Transition, config: MatchConfiguration): Boolean = when {
        this.from.matches(other.from).not() -> false
        this.to.matches(other.to).not() -> false
        this.action != other.action -> false
        (!config.no_lookahead_compare && this.lookahead.matches(other.lookahead) { t, o -> t.matches(o, config) }.not()) -> false
        else -> true
    }

    private fun TransitionPrevInfoKey.matches(other: TransitionPrevInfoKey): Boolean = when {
        this is CompleteKey && other is CompleteKey -> this.previous.matches(other.previous) && this.prevPrev.matches(other.prevPrev)
        this is IncompleteKey && other is IncompleteKey -> this.previous.matches(other.previous)
        else -> false
    }

    private fun Lookahead.matches(other: Lookahead, config: MatchConfiguration): Boolean = when {
        this.guard.matches(other.guard, config).not() -> false
        this.up.matches(other.up, config).not() -> false
        else -> true
    }

    private fun LookaheadSet.matches(other: LookaheadSet, config: MatchConfiguration): Boolean {
        val substituted = config.in_actual_substitue_lookahead_RT_with?.let {
            other.fullContent.replace(setOf(RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD), config.in_actual_substitue_lookahead_RT_with)
        } ?: other.fullContent
        return this.fullContent.matches(substituted) { t, o -> t.matches(o) }
    }

    fun assertEquals(expected: ParserState, actual: ParserState) {
        kotlin.test.assertEquals(expected.rulePositions.toSet(), actual.rulePositions.toSet(), "RulePositions do not match")

        kotlin.test.assertEquals(
            expected.outTransitions.allPrevious.size,
            actual.outTransitions.allPrevious.size,
            "Previous States for Transitions outgoing from ${expected} do not match"
        )
        TODO()
        /*
        for (expected_prev in expected.outTransitions.allPrevious) {
            val expected_trs = expected.outTransitions.findTransitionByKey(expected_prev) ?: emptyList()
            val actual_prev = actual.stateSet.fetchState(expected_prev.rulePositions) ?: fail("actual State not found for: ${expected_prev}")
            val actual_trs = actual.outTransitions.findTransitionByKey(actual_prev) ?: emptyList()
            kotlin.test.assertEquals(expected_trs.size, actual_trs.size, "Number of Transitions outgoing from ${expected_prev} -> ${expected} do not match")
            for (i in expected_trs.indices) {
                assertEquals(expected_prev, expected_trs[i], actual_trs[i])
            }
        }
*/
    }

    fun assertEquals(expPrev: ParserState, expected: Transition, actual: Transition) {
        kotlin.test.assertEquals(expected.from.rulePositions.toSet(), actual.from.rulePositions.toSet(), "From state does not match for ${expPrev} -> $expected")
        kotlin.test.assertEquals(expected.to.rulePositions.toSet(), actual.to.rulePositions.toSet(), "To state does not match for ${expPrev} -> $expected")
        kotlin.test.assertEquals(expected.action, actual.action, "Action does not match for ${expPrev} -> $expected")
        assertEquals(expected, expected.lookahead, actual.lookahead)//, "Lookahead content does not match for ${expPrev} -> $expected")
        //TODO kotlin.test.assertEquals(expected.upLookahead.includesUP, actual.upLookahead.includesUP, "Up lookahead content does not match for ${expPrev} -> $expected")
    }

    fun assertEquals(expTrans: Transition, expected: Set<Lookahead>, actual: Set<Lookahead>) {
        kotlin.test.assertEquals(expected.size, actual.size, "Lookahead size does not match for \nExpected: ${expTrans}\nActual:$actual")
        val expSorted = expected.sortedBy { it.guard.fullContent.map { it.tag }.joinToString() }
        val actSorted = actual.sortedBy { it.guard.fullContent.map { it.tag }.joinToString() }
        for (i in expSorted.indices) {
            assertEquals(expTrans, expSorted[i], actSorted[i])
        }
    }

    fun assertEquals(expTrans: Transition, expected: Lookahead, actual: Lookahead) {
        kotlin.test.assertEquals(
            expected.guard.fullContent.map { it.tag }.toSet(),
            actual.guard.fullContent.map { it.tag }.toSet(),
            "Lookahead guard content does not match for ${expTrans}\n$expected"
        )
        kotlin.test.assertEquals(
            expected.up.fullContent.map { it.tag }.toSet(),
            actual.up.fullContent.map { it.tag }.toSet(),
            "Lookahead up content does not match for ${expTrans}\n$expected"
        )
    }
}