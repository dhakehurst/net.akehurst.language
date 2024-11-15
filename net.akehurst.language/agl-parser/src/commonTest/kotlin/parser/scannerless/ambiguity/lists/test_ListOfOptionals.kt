package net.akehurst.language.parser.leftcorner.ambiguity.lists

import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import testFixture.data.ambiguity.ListOfOptionals
import kotlin.test.Test

class test_ListOfOptionals : test_LeftCornerParserAbstract() {

    private companion object {
        val td = ListOfOptionals.data[0]
    }

    @Test
    fun empty() {
        test(td,"")
    }

    @Test
    fun a() {
        test(td,"a")
    }

    @Test
    fun aa() {
        test(td,"aa")
    }

    @Test
    fun aaa() {
        test(td,"aaa")
    }
}