package net.akehurst.language.agl.runtime.structure.runtimeRuleSet

import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals

class test_buildFor {

    @Test
    fun concatenation() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a"); literal("c"); literal("c") }
        }

        val actual = rrs.buildFor("S")

        assertEquals(6,actual.states.values.size)
        //TODO: expected Transitions
    }


    @Test
    fun leftRecursion() {
        val rrs = runtimeRuleSet {
            choice("S",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { ref("S"); literal("a") }
        }

        val actual = rrs.buildFor("S")

        assertEquals(9,actual.states.values.size)
        assertEquals(12, actual.allBuiltTransitions.size)
        //TODO: expected Transitions
    }
}
