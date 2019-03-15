package parser.runtime.structure.RuntimeRuleSet.nextPossibleRulePositionStates

import net.akehurst.language.agl.runtime.structure.RulePositionState
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun assertDeepEquals(expected: RulePositionState, actual: RulePositionState): Unit {
    assertTrue(actual.deepEquals(expected), "Expected <$expected>, actual <$actual>.")
}
fun assertDeepEquals(expected: Set<RulePositionState>, actual: Set<RulePositionState>): Unit {
    assertEquals(expected.size, actual.size, "\"Expected.size <${expected.size}>, actual.size <${actual.size}>.\"")
    expected.zip(actual).forEach {
        assertDeepEquals(it.first, it.second)
    }
}