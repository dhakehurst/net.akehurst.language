package net.akehurst.language.ogl.runtime.structure

import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals

class test_RuntimeRuleSet {

    @Test
    fun findFirstTerminals() {
        val rb = RuntimeRuleSetBuilder()
        val A = rb.literal("A")
        val a = rb.literal("a")
        val B = rb.literal("B")
        val b = rb.literal("b")
        val Aa = rb.rule("Aa").concatenation(A, a)
        val Bb = rb.rule("Bb").concatenation(B, b)
        val r = rb.rule("r").choicePriority(Aa, Bb)

        val sut = rb.ruleSet()
        val actual = sut.findFirstTerminals(r)
        val expected = setOf(A, B)

        assertEquals(expected, actual)
    }

}