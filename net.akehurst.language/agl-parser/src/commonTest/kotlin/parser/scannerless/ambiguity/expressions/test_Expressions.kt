package net.akehurst.language.parser.leftcorner.ambiguity.expressions

import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import testFixture.data.ambiguity.Expressions
import kotlin.test.Test

class test_Expressions : test_LeftCornerParserAbstract() {

    private companion object {
        val td = Expressions.data[0]
    }

    @Test
    fun a() {
        test_pass(td,"pa")
    }

    @Test
    fun aob() {
        test_pass(td,"paob")
    }

    @Test
    fun aoboc() {
        test_pass(td,"paoboc")
    }

    @Test
    fun aobocod() {
        test_pass(td,"paobocod")
    }
}