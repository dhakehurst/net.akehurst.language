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
package net.akehurst.language.agl.processor.atom_basic

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


@RunWith(Parameterized::class)
class test_AtomBasic(val data: Data) {

    companion object {

        fun getResourceFiles(path: String): List<String> = getResourceAsStream(path).use {
            if (it == null) emptyList()
            else BufferedReader(InputStreamReader(it)).readLines()
        }

        private fun getResourceAsStream(resource: String): InputStream? =
            object {}.javaClass.getResourceAsStream(resource)
                ?: resource::class.java.getResourceAsStream(resource)

        private val grammarStr = this::class.java.getResource("/atom-basic/Grammar.agl")?.readText() ?: error("File not found")

        var processor = Agl.processorFromStringSimple(GrammarString(grammarStr)).let {
            it.processor ?: error("Unable to parse '/atom-basic/Grammar.agl'\n${it.issues}")
        }
        const val validSourceFilesFolderName = "/atom-basic/valid"
        const val inValidSourceFilesFolderName = "/atom-basic/invalid"

        @JvmStatic
        @Parameters(name = "{0}")
        fun collectData(): Collection<Array<Any>> {
            val col = ArrayList<Array<Any>>()
            val validSourceFiles = getResourceFiles(validSourceFilesFolderName)
            val inValidSourceFiles = getResourceFiles(inValidSourceFilesFolderName)
            for (file in validSourceFiles) {
                val inps = this::class.java.getResourceAsStream("$validSourceFilesFolderName/$file") ?: error("File not found")
                val lines = inps.use { BufferedReader(InputStreamReader(it)).lines().toList() }
                val text = lines.fold("") { acc, it -> "$acc\n$it" }
                col.add(arrayOf(Data(file, text, true)))
            }
            for (file in inValidSourceFiles) {
                val inps = this::class.java.getResourceAsStream("$inValidSourceFilesFolderName/$file") ?: error("File not found")
                val lines = inps.use { BufferedReader(InputStreamReader(it)).lines().toList() }
                val text = lines.fold("") { acc, it -> "$acc\n$it" }
                col.add(arrayOf(Data(file, text, false)))
            }
            return col
        }
    }

    data class Data(val file: String, val text: String, val valid: Boolean)

    @Test
    fun test() {
        if (data.valid) {
            val result = processor.parse(this.data.text, Agl.parseOptions { goalRuleName("file") })
            assertNotNull(result.sppt, result.issues.toString())
            assertTrue(result.issues.isEmpty())
            val resultStr = result.sppt!!.asSentence
            assertEquals(this.data.text, resultStr)
        } else {
            val result = processor.parse(this.data.text, Agl.parseOptions { goalRuleName("file") })
            assertTrue(result.issues.isNotEmpty())
        }
    }

}
