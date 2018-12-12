package net.akehurst.language.processor

import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.ogl.grammar.runtime.Converter
import net.akehurst.language.parser.sppt.SPPTParser
import kotlin.test.assertEquals

abstract class test_ProcessorAbstract {

    fun test(grammar:Grammar, goal:String, sentence:String, vararg expectedTrees:String) {
        val processor = Ogl.processor(grammar)
        val actual = processor.parse(goal, sentence)

        val converter = Converter(grammar)
        converter.transform()
        val rrb = converter.builder
        val sppt = SPPTParser(rrb)
        expectedTrees.forEach { sppt.addTree(it) }
        val expected = sppt.tree
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

}