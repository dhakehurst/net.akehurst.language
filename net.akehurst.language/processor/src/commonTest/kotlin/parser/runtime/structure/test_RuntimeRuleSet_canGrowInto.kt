package net.akehurst.language.ogl.runtime.structure

import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals

class test_RuntimeRuleSet_canGrowInto {


    /**
     *   S =  ;
     */
    @Test
    fun canGrowInto_empty() {
        val rb = RuntimeRuleSetBuilder()

        val r_S = rb.rule("S").empty()

        val sut = rb.ruleSet()

        val actual = sut.calcCanGrowInto(r_a, r_S, 0)
        assertEquals(true, actual)
    }

    /**
     *   S = 'a' ;
     */
    @Test
    fun canGrowInto_concatenation_1() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").concatenation(r_a)

        val sut = rb.ruleSet()

        val actual = sut.calcCanGrowInto(r_a, r_S, 0)
        assertEquals(true, actual)
    }

    /**
     *   S = 'a' 'b';
     */
    @Test
    fun canGrowInto_concatenation_2() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_S = rb.rule("S").concatenation(r_a, r_b)

        val sut = rb.ruleSet()

        val actual1 = sut.calcCanGrowInto(r_a, r_S, 0)
        assertEquals(true, actual1)

        val actual2 = sut.calcCanGrowInto(r_b, r_S, 0)
        assertEquals(false, actual2)

        val actual3 = sut.calcCanGrowInto(r_b, r_S, 1)
        assertEquals(true, actual3)
    }

    /**
     *   S = 'a' 'b' 'c';
     */
    @Test
    fun canGrowInto_concatenation_3() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_c = rb.literal("c")
        val r_S = rb.rule("S").concatenation(r_a, r_b, r_c)

        val sut = rb.ruleSet()

        val actual1 = sut.calcCanGrowInto(r_a, r_S, 0)
        assertEquals(true, actual1)

        val actual2 = sut.calcCanGrowInto(r_b, r_S, 0)
        assertEquals(false, actual2)

        val actual3 = sut.calcCanGrowInto(r_b, r_S, 1)
        assertEquals(true, actual3)

        val actual4 = sut.calcCanGrowInto(r_c, r_S, 0)
        assertEquals(false, actual4)

        val actual5 = sut.calcCanGrowInto(r_c, r_S, 1)
        assertEquals(false, actual5)

        val actual6 = sut.calcCanGrowInto(r_c, r_S, 2)
        assertEquals(true, actual6)
    }

    /**
     *   S = 'a' ;
     */
    @Test
    fun canGrowInto_choiceEqual_1() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").choiceEqual(r_a)

        val sut = rb.ruleSet()

        val actual = sut.calcCanGrowInto(r_a, r_S, 0)
        assertEquals(true, actual)

        val actual2 = sut.calcCanGrowInto(r_a, r_S, 1)
        assertEquals(false, actual2)
    }

    /**
     *   S = 'a' | 'b' ;
     */
    @Test
    fun canGrowInto_choiceEqual_2() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_S = rb.rule("S").choiceEqual(r_a, r_b)

        val sut = rb.ruleSet()

        val actual1 = sut.calcCanGrowInto(r_a, r_S, 0)
        assertEquals(true, actual1)

        val actual2 = sut.calcCanGrowInto(r_b, r_S, 0)
        assertEquals(true, actual2)
    }

    /**
     *   S = 'a' | 'b' | 'c';
     */
    @Test
    fun canGrowInto_choiceEqual_3() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_b = rb.literal("b")
        val r_S = rb.rule("S").choiceEqual(r_a, r_b)

        val sut = rb.ruleSet()

        val actual1 = sut.calcCanGrowInto(r_a, r_S, 0)
        assertEquals(true, actual1)

        val actual2 = sut.calcCanGrowInto(r_b, r_S, 0)
        assertEquals(true, actual2)
    }

    /**
     *   S = 'a' ;
     */
    @Test
    fun canGrowInto_choicePriority_1() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").choicePriority(r_a)

        val sut = rb.ruleSet()

        val actual = sut.calcCanGrowInto(r_a, r_S, 0)
        assertEquals(true, actual)

        val actual2 = sut.calcCanGrowInto(r_a, r_S, 1)
        assertEquals(false, actual2)
    }

    /**
     *   S = 'a'?;
     */
    @Test
    fun canGrowInto_multi01_1() {
        val rb = RuntimeRuleSetBuilder()

        val r_S = rb.rule("S").choicePriority(r_a)

        val sut = rb.ruleSet()

        val actual = sut.calcCanGrowInto(r_a, r_S, 0)
        assertEquals(true, actual)

        val actual2 = sut.calcCanGrowInto(r_a, r_S, 1)
        assertEquals(false, actual2)
    }

    /**
     *   S = 'a'+;
     */
    @Test
    fun canGrowInto_multi1n_1() {
        val rb = RuntimeRuleSetBuilder()

        val r_S = rb.rule("S").choicePriority(r_a)

        val sut = rb.ruleSet()

        val actual = sut.calcCanGrowInto(r_a, r_S, 0)
        assertEquals(true, actual)

        val actual2 = sut.calcCanGrowInto(r_a, r_S, 1)
        assertEquals(false, actual2)
    }

    /**
     *   S = 'a'*;
     */
    @Test
    fun canGrowInto_multi0n_1() {
        val rb = RuntimeRuleSetBuilder()

        val r_S = rb.rule("S").choicePriority(r_a)

        val sut = rb.ruleSet()

        val actual = sut.calcCanGrowInto(r_a, r_S, 0)
        assertEquals(true, actual)

        val actual2 = sut.calcCanGrowInto(r_a, r_S, 1)
        assertEquals(false, actual2)
    }

    /**
     *   S = 'a'[2,5];
     */
    @Test
    fun canGrowInto_multi25_1() {
        val rb = RuntimeRuleSetBuilder()

        val r_S = rb.rule("S").choicePriority(r_a)

        val sut = rb.ruleSet()

        val actual = sut.calcCanGrowInto(r_a, r_S, 0)
        assertEquals(true, actual)

        val actual2 = sut.calcCanGrowInto(r_a, r_S, 1)
        assertEquals(false, actual2)
    }

    /**
     *   S = [ 'a' / ',']+;
     */
    @Test
    fun canGrowInto_sList1n_1() {
        val rb = RuntimeRuleSetBuilder()

        val r_S = rb.rule("S").choicePriority(r_a)

        val sut = rb.ruleSet()

        val actual = sut.calcCanGrowInto(r_a, r_S, 0)
        assertEquals(true, actual)

        val actual2 = sut.calcCanGrowInto(r_a, r_S, 1)
        assertEquals(false, actual2)
    }

    /**
     *   S = [ 'a' / ',']*;
     */
    @Test
    fun canGrowInto_sList0n_1() {
        val rb = RuntimeRuleSetBuilder()

        val r_S = rb.rule("S").choicePriority(r_a)

        val sut = rb.ruleSet()

        val actual = sut.calcCanGrowInto(r_a, r_S, 0)
        assertEquals(true, actual)

        val actual2 = sut.calcCanGrowInto(r_a, r_S, 1)
        assertEquals(false, actual2)
    }

    /**
     *   S = [ 'a' / ','][2,5];
     */
    @Test
    fun canGrowInto_sList25_1() {
        val rb = RuntimeRuleSetBuilder()

        val r_S = rb.rule("S").choicePriority(r_a)

        val sut = rb.ruleSet()

        val actual = sut.calcCanGrowInto(r_a, r_S, 0)
        assertEquals(true, actual)

        val actual2 = sut.calcCanGrowInto(r_a, r_S, 1)
        assertEquals(false, actual2)
    }
}