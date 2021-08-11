/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.agl.processor.xml

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
import net.akehurst.language.agl.processor.Agl


@RunWith(Parameterized::class)
class test_Xml(val data:Data) {

    companion object {

        private val grammarStr = this::class.java.getResource("/xml/Xml.agl").readText()
        //private val grammarStr = ""//runBlockingNoSuspensions { resourcesVfs["/xml/Xml.agl"].readString() }
        var processor: LanguageProcessor = tgqlprocessor()

        var xmlFiles = arrayOf("/xml/valid/empty.xml")

        fun tgqlprocessor() : LanguageProcessor {
            //val grammarStr = ClassLoader.getSystemClassLoader().getResource("vistraq/Query.ogl").readText()
            return Agl.processorFromString(grammarStr)
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
        val result = processor.parseForGoal("file", this.data.text)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(this.data.text, resultStr)
    }

}
