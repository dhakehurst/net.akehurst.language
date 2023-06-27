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

import net.akehurst.language.agl.processor.Agl
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


@RunWith(Parameterized::class)
class test_Xml(val data: Data) {

    private companion object {

        val grammarStr = this::class.java.getResource("/xml/Xml.agl").readText()
        const val goal = "document"

        var processor = Agl.processorFromStringDefault(grammarStr).processor!!

        val validDir = "/xml/valid"
        var invalidDir = "/xml/invalid"

        @JvmStatic
        @Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            val col = ArrayList<Array<Any>>()
            for (xmlFile in this::class.java.getResourceAsStream(validDir).reader().readLines()) {
                val xmlText = this::class.java.getResource("$validDir/$xmlFile").readText()
                col.add(arrayOf(Data(xmlFile, xmlText, true)))
            }
            for (xmlFile in this::class.java.getResourceAsStream(invalidDir).reader().readLines()) {
                val xmlText = this::class.java.getResource("$invalidDir/$xmlFile").readText()
                col.add(arrayOf(Data(xmlFile, xmlText, false)))
            }
            return col
        }
    }

    class Data(val file: String, val text: String, val valid: Boolean) {

        // --- Object ---
        override fun toString(): String = file
    }

    @Test
    fun parse() {
        val result = processor.parse(this.data.text, Agl.parseOptions { goalRuleName(goal) })

        if (data.valid) {
            assertNotNull(result.sppt)
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
            val resultStr = result.sppt!!.asString
            assertEquals(this.data.text, resultStr)
        } else {
            assertNull(result.sppt)
            assertTrue(result.issues.errors.isNotEmpty())
        }
    }

    @Test
    fun process() {
        val result = processor.process(this.data.text, Agl.options { parse { goalRuleName(goal) } })

        if (data.valid) {
            assertNotNull(result.asm, result.issues.toString())
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        } else {
            assertNull(result.asm)
            assertTrue(result.issues.isNotEmpty())
        }
    }

}
