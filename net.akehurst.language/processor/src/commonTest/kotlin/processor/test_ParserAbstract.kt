package processor

import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.ogl.grammar.runtime.Converter
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.ScannerlessParser
import net.akehurst.language.parser.sppt.SPPTParser
import net.akehurst.language.processor.processor
import kotlin.test.assertEquals

abstract class test_ParserAbstract {

    fun test(grammar:Grammar, goal:String, sentence:String, vararg expectedTrees:String) {
        val processor = processor(grammar)
        val actual = processor.parse(goal, sentence)

        val converter = Converter(grammar)
        converter.transform()
        val rrb = converter.builder
        val sppt = SPPTParser(rrb)
        expectedTrees.forEach { sppt.addTree(it) }
        val expected = sppt.tree
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    fun test(rrsb:RuntimeRuleSetBuilder, goal:String, sentence:String, vararg expectedTrees:String) {
        val parser = ScannerlessParser(rrsb.ruleSet())
        val actual = parser.parse(goal, sentence)

        val sppt = SPPTParser(rrsb)
        expectedTrees.forEach { sppt.addTree(it) }
        val expected = sppt.tree
        assertEquals(expected.toStringAll("  "), actual.toStringAll("  "))
    }

}