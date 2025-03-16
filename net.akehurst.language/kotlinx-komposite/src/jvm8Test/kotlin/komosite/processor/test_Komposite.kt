/**
 * Copyright (C) 2019 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.kotlinx.komposite.processor

import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertNotNull


@RunWith(Parameterized::class)
class test_Komposite(val data: Data) {

    class Data(val sourceFileName: String, val fileContent: String) {

        // --- Object ---
        override fun toString(): String {
            return this.sourceFileName
        }
    }

    private companion object {
        var processor = Komposite.processor()

        val sourceFiles = listOf(
            "komposite/empty.komposite",
            "komposite/emptyNamespace.komposite",
            "komposite/emptyDatatype.komposite",
            "komposite/primitive.komposite",
            "komposite/enum.komposite",
            "komposite/datatypes.komposite",
            "komposite/generictypes.komposite"
        )


        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Data> {
            val col = ArrayList<Data>()
            for (sourceFile in sourceFiles) {
                val fileContent = ClassLoader.getSystemClassLoader().getResourceAsStream(sourceFile).reader().readText()
                col.add(Data(sourceFile, fileContent))
            }
            return col
        }
    }


    @Test
    fun parse() {
        val result = processor.parse(this.data.fileContent)
        assertNotNull(result.sppt,result.issues.joinToString(separator = "\n"){"$it"})
        //val resultStr = result.asString
        //assertEquals(original, resultStr)
    }

    @Test
    fun process() {
        val result = processor.process(this.data.fileContent)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n"){"$it"})
        //val resultStr = result.asString
        //assertEquals(original, resultStr)
    }

}