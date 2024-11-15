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
        test(td,"pa")
    }

    @Test
    fun aob() {
        test(td,"paob")
    }

    @Test
    fun aoboc() {
        test(td,"paoboc")
    }

    @Test
    fun aobocod() {
        test(td,"paobocod")
    }
}