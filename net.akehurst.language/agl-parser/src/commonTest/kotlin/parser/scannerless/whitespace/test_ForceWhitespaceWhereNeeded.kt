package parser.scannerless.whitespace

import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import testFixture.data.whitespace.ForceWhitespaceWhereNeeded
import kotlin.test.Test

class test_ForceWhitespaceWhereNeeded : test_LeftCornerParserAbstract() {

    private companion object {
        val td = ForceWhitespaceWhereNeeded.data[0]
    }

    @Test
    fun empty() {
        test(td,"")
    }

    @Test
    fun v() {
        test(td,"v")
    }

    @Test
    fun key() {
        test(td,"key")
    }

    @Test
    fun keyid() {
        test(td,"keyid")
    }

    @Test
    fun key_id() {
        test(td,"key id")
    }

}