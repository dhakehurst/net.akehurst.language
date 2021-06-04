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
package net.akehurst.language.agl.processor.dot

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.processor.LanguageProcessor
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

@RunWith(Parameterized::class)
class test_Dot(val data: Data) {

    companion object {

        private val grammarStr = this::class.java.getResource("/dot/Dot.agl")?.readText() ?: error("File not found")
        var processor: LanguageProcessor = Agl.processor(grammarStr).buildFor("graph") //TODO: use build

        val validDirectory = "/dot/valid/"
        var validFiles = this::class.java.getResourceAsStream(validDirectory).use { if (null == it) emptyList<String>() else BufferedReader(InputStreamReader(it)).readLines() }


        @JvmStatic
        @Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            val col = ArrayList<Array<Any>>()
            for (file in validFiles) {
                val text = this::class.java.getResource(validDirectory + file)?.readText() ?: error("File not found")
                col.add(arrayOf(Data(file, text)))
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
        val result = processor.parse("graph", this.data.text)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(this.data.text, resultStr)
    }

}
