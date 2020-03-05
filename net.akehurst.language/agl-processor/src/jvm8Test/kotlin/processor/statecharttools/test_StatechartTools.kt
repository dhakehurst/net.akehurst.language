package net.akehurst.language.processor.xml

//import com.soywiz.korio.async.runBlockingNoSuspensions
//import com.soywiz.korio.file.std.resourcesVfs
//import java.io.BufferedReader
//import java.io.InputStreamReader
import java.util.ArrayList

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.processor.Agl
import net.akehurst.language.processor.vistraq.test_QueryParserValid
import java.io.BufferedReader
import java.io.InputStreamReader


@RunWith(Parameterized::class)
class test_StatechartTools(val data: Data) {

    companion object {

        private val grammarStr1 = this::class.java.getResource("/statechart-tools/Expressions.agl").readText()
        private val grammarStr2 = this::class.java.getResource("/statechart-tools/SText.agl").readText()
        //private val grammarStr = ""//runBlockingNoSuspensions { resourcesVfs["/xml/Xml.agl"].readString() }

        // must create processor for 'Expressions' so that SText can extend it
        val exprProcessor = Agl.processor(grammarStr1)
        var processor: LanguageProcessor = Agl.processor(grammarStr2)
        var sourceFiles = arrayOf("/statechart-tools/samplesValid.txt")

        @JvmStatic
        @Parameters(name = "{0}")
        fun collectData(): Collection<Array<Any>> {
            val col = ArrayList<Array<Any>>()
            var ruleName = ""
            for (file in sourceFiles) {
                val lines = BufferedReader(InputStreamReader(this::class.java.getResourceAsStream(file))).lines()
                lines.forEach { it ->
                    val line = it.trim { it <= ' ' }
                    if (line.isEmpty()) {
                        // blank line
                    } else if (line.startsWith("//#")) {
                        // change goal rule
                        ruleName = line.substringAfter("#").trim()
                    } else if (line.startsWith("//")) {
                        // comment
                    } else {
                        col.add(arrayOf(Data(file, ruleName, line)))
                    }
                }
                return col
            }
            return col
        }
    }

    data class Data(val file: String, val ruleName: String, val text: String)

    @Test
    fun test() {
        val result = processor.parse(this.data.ruleName, this.data.text)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(this.data.text, resultStr)
    }

}
