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


@RunWith(Parameterized::class)
class test_Xml(val data:Data) {

    companion object {

        private val grammarStr = this::class.java.getResource("/xml/Xml.agl").readText()
        //private val grammarStr = ""//runBlockingNoSuspensions { resourcesVfs["/xml/Xml.agl"].readString() }
        var processor: LanguageProcessor = tgqlprocessor()

        var xmlFiles = arrayOf("/xml/valid/empty.xml")

        fun tgqlprocessor() : LanguageProcessor {
            //val grammarStr = ClassLoader.getSystemClassLoader().getResource("vistraq/Query.ogl").readText()
            return Agl.processor(grammarStr)
         }

        @JvmStatic
        @Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            val col = ArrayList<Array<Any>>()
            for (xmlFile in xmlFiles) {
                val xmlText = this::class.java.getResource(xmlFile).readText()
                col.add(arrayOf(Data(xmlFile, xmlText)))
            }
            return col
        }
    }

    class Data(val file: String, val text: String) {

        // --- Object ---
        override fun toString(): String {
            return this.file
        }
    }

    @Test
    fun test() {
        val result = processor.parse("file", this.data.text)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(this.data.text, resultStr)
    }

}
