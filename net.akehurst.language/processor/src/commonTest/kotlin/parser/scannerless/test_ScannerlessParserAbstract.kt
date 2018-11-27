package net.akehurst.language.parser.scannerless

import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.sppt.SPPTParser
import kotlin.test.assertEquals

abstract class test_ScannerlessParserAbstract {

    fun test(rrsb:RuntimeRuleSetBuilder, goal:String, sentence:String, vararg expectedTrees:String) {
        val parser = ScannerlessParser(rrsb.ruleSet())
        val actual = parser.parse(goal, sentence)

        val sppt = SPPTParser(rrsb)
        expectedTrees.forEach { sppt.addTree(it) }
        val expected = sppt.tree
        assertEquals(expected, actual)
    }

    fun testStringResult(rrsb:RuntimeRuleSetBuilder, goal:String, sentence:String, vararg expectedTrees:String) {
        val parser = ScannerlessParser(rrsb.ruleSet())
        val actual = parser.parse(goal, sentence)

        val sppt = SPPTParser(rrsb)
        expectedTrees.forEach { sppt.addTree(it) }
        val expected = sppt.tree
        assertEquals(expected.toStringAll("  "), actual.toStringAll("  "))
    }
}