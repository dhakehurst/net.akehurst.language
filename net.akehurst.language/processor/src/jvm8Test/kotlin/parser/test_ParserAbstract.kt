package parser

import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.ogl.grammar.runtime.Converter
import net.akehurst.language.parser.sppt.SPPTParser
import net.akehurst.language.processor.processor
import net.akehurst.language.processor.vistraq.test_QueryParserValid
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

}