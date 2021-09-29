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

package net.akehurst.language.agl.processor.java8

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
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.test.assertEquals


@RunWith(Parameterized::class)
class test_Java8Agl_Literals(val data:Data) {

    companion object {

        private val grammarStr = this::class.java.getResource("/java8/Java8AglOptm.agl").readText()
        val processor : LanguageProcessor by lazy {
            Agl.processorFromString(grammarStr, "Literal").buildFor("Literal")
        }
        var sourceFiles = arrayOf("/java8/sentences/literals-valid.txt")

        @JvmStatic
        @Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            val col = ArrayList<Array<Any>>()
            for (sourceFile in sourceFiles) {
                val inps = test_Java8Agl_Literals::class.java.getResourceAsStream(sourceFile)

                val br = BufferedReader(InputStreamReader(inps))
                var line: String? = br.readLine()
                while (null != line) {
                    line = line.trim { it <= ' ' }
                    if (line.isEmpty()) {
                        // blank line
                        line = br.readLine()
                    } else if (line.startsWith("//")) {
                        // comment
                        line = br.readLine()
                    } else {
                        col.add(arrayOf(Data(sourceFile, line)))
                        line = br.readLine()
                    }
                }
            }
            return col
        }
    }

    class Data(val file: String, val text: String) {

        // --- Object ---
        override fun toString(): String {
            return "${this.file} | ${this.text}"
        }
    }

    @Test
    fun test() {
        val result = processor.parse( this.data.text)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(this.data.text, resultStr)
        assertEquals(1, result.maxNumHeads)
    }

}
