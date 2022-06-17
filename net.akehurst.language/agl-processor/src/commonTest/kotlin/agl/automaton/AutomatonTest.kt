package agl.automaton

import net.akehurst.language.agl.automaton.*
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import kotlin.test.fail

internal object AutomatonTest {

    private fun <E> Set<E>.matches(other: Set<E>, matches: (t: E, o: E) -> Boolean): Boolean {
        val thisList = this.toList()
        val foundThis = mutableListOf<E>()
        val foundOther = mutableListOf<E>()
        for (i in this.indices) {
            val thisElement = thisList[i]
            val otherElement = other.firstOrNull { matches(thisElement, it) }
            if (null != otherElement) {
                foundThis.add(thisElement)
                foundOther.add(otherElement)
            }
        }
        return foundThis.size == foundOther.size && foundThis.size==thisList.size
    }

    fun <E> assertMatches(setName: String, expected: Set<E>, actual: Set<E>, matches: (t: E, o: E) -> Boolean) {
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
            expected.size > foundThis.size -> kotlin.test.fail("Elements of $setName do not match, missing:\n${(expected - foundThis).joinToString(separator = "\n") { it.toString() }}")
            actual.size > foundOther.size -> kotlin.test.fail("Elements of $setName do not match, missing:\n${(actual - foundOther).joinToString(separator = "\n") { it.toString() }}")
            else -> Unit
        }
    }

    fun assertEquals(expected: ParserStateSet, actual: ParserStateSet) {
        val expected_states = expected.allBuiltStates.toSet()
        val actual_states = actual.allBuiltStates.toSet()

        assertMatches("allBuiltStates", expected_states, actual_states) { t, o -> t.matches(o) }
        assertMatches("allBuiltTransitions", expected.allBuiltTransitions.toSet(), actual.allBuiltTransitions.toSet()) { t, o -> t.matches(o) }

    }


    private fun ParserState.matches(other: ParserState): Boolean = this.rulePositions.toSet().matches(other.rulePositions.toSet()) { t, o -> t.matches(o) }
    private fun Transition.matches(other: Transition): Boolean = when {
        this.from.matches(other.from).not() -> false
        this.to.matches(other.to).not() -> false
        this.action != other.action -> false
        this.lookahead.matches(other.lookahead) { t, o -> t.matches(o) }.not() -> false
        else -> true
    }

    private fun RulePosition.matches(other: RulePosition): Boolean = when {
        this.option != other.option -> false
        this.position != other.position -> false
        else -> this.runtimeRule.matches(other.runtimeRule)
    }

    private fun RuntimeRule.matches(other: RuntimeRule): Boolean = when {
        this.name != other.name -> false
        this.value != other.value -> false
        this.kind != other.kind -> false
        this.isPattern != other.isPattern -> false
        this.isSkip != other.isSkip -> false
        this.kind==RuntimeRuleKind.NON_TERMINAL && this.rhs.matches(other.rhs).not() -> false
        //TODO: this.embeddedRuntimeRuleSet != other.embeddedRuntimeRuleSet -> false
        //TODO: this.embeddedStartRule?.matches(other.embeddedStartRule)?.not() -> false
        else -> true
    }

    private fun RuntimeRuleItem.matches(other: RuntimeRuleItem): Boolean = when {
        this.itemsKind != other.itemsKind -> false
        this.choiceKind != other.choiceKind -> false
        this.listKind != other.listKind -> false
        this.multiMin != other.multiMin -> false
        this.multiMax != other.multiMax -> false
        else -> this.items.all { t -> other.items.any { o -> t.name == o.name && t.value == o.value } }
    }

    private fun Lookahead.matches(other: Lookahead): Boolean = when {
        this.guard.matches(other.guard).not() -> false
        this.up.matches(other.up).not() -> false
        else -> true
    }

    private fun LookaheadSet.matches(other: LookaheadSet): Boolean = when {
        this.includesEOT != other.includesEOT -> false
        this.includesUP != other.includesUP -> false
        this.matchANY != other.matchANY -> false
        else -> this.content.matches(other.content) { t, o -> t.matches(o) }
    }

    fun assertEquals(expected: ParserState, actual: ParserState) {
        kotlin.test.assertEquals(expected.rulePositions.toSet(), actual.rulePositions.toSet(), "RulePositions do not match")

        kotlin.test.assertEquals(
            expected.outTransitions.allPrevious.size,
            actual.outTransitions.allPrevious.size,
            "Previous States for Transitions outgoing from ${expected} do not match"
        )
        for (expected_prev in expected.outTransitions.allPrevious) {
            val expected_trs = expected.outTransitions.findTransitionByPrevious(expected_prev) ?: emptyList()
            val actual_prev = actual.stateSet.fetchState(expected_prev.rulePositions) ?: fail("actual State not found for: ${expected_prev}")
            val actual_trs = actual.outTransitions.findTransitionByPrevious(actual_prev) ?: emptyList()
            kotlin.test.assertEquals(expected_trs.size, actual_trs.size, "Number of Transitions outgoing from ${expected_prev} -> ${expected} do not match")
            for (i in expected_trs.indices) {
                assertEquals(expected_prev, expected_trs[i], actual_trs[i])
            }
        }
        /*
        val expected_trans = expected.outTransitions.transitionsByPrevious
        val actual_trans = actual.outTransitions.transitionsByPrevious
        assertEquals(expected_trans.keys.map { it?.rulePositions }, actual_trans.keys.map { it?.rulePositions }, "Previous States for Transitions outgoing from ${expected} do not match")
        assertEquals(expected_trans.size, actual_trans.size, "Number of Transitions outgoing from ${expected} do not match")

        for (entry in expected_trans.entries) {
            val actual_key = if (null==entry.key) null else actual.stateSet.states[entry.key!!.rulePositions]
            val expected_outgoing = expected_trans[entry.key] ?: emptyList()
            val actual_outgoing = actual_trans[actual_key] ?: emptyList()
            assertEquals(expected_outgoing.size, actual_outgoing.size, "Number of Transitions outgoing from ${entry.key} -> ${expected} do not match")

            for (i in expected_outgoing.indices) {
                assertEquals(entry.key, expected_outgoing[i], actual_outgoing[i])
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
        kotlin.test.assertEquals(expected.prevGuard?.toSet(), actual.prevGuard?.toSet(), "Previous guard does not match for ${expPrev} -> $expected")

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