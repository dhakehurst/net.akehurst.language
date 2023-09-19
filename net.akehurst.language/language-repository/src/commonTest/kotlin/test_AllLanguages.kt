/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package net.akehurst.language.repository

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import korlibs.io.file.baseName
import korlibs.io.file.std.localCurrentDirVfs
import kotlinx.coroutines.flow.collectIndexed
import net.akehurst.language.agl.processor.Agl
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val rootFs = localCurrentDirVfs["languages"]

data class TestData(
    val grammarPath: String,
    val grammarString: String,
    val sentencePath: String,
    val sentence: String,
    val isValid: Boolean
) {
    override fun toString(): String = "$grammarPath - $sentencePath"
}

private suspend fun fetchTestData(): List<TestData> {
    val testData = mutableListOf<TestData>()
    println("listNames ${rootFs.listNames()}")
    rootFs.listNames().forEach { lang ->
        rootFs[lang].listNames().forEach { ver ->
            val grammarPath = "$lang/$ver"
            rootFs[lang][ver].listNames().forEach { fileName ->
                if (fileName.startsWith("grammar") && fileName.endsWith(".agl")) {
                    val grammarStr = rootFs[lang][ver][fileName].readString()
                    rootFs[lang][ver]["valid"].list().collectIndexed { _, value ->
                        val sentence = value.readString()
                        testData.add(TestData("$grammarPath/$fileName", grammarStr, "valid/${value.baseName}", sentence, true))
                    }
                    rootFs[lang][ver]["invalid"].list().collectIndexed { _, value ->
                        val sentence = value.readString()
                        testData.add(TestData("$grammarPath/$fileName", grammarStr, "valid/${value.baseName}", sentence, false))
                    }
                }
            }
        }
    }

    return testData
}

lateinit var tests: List<TestData>

class test_AllLanguages : FunSpec({
    beforeSpec {
        println("Start Spec")
        tests = fetchTestData()
        println(tests)
    }
    context("parse") {
        withData(tests) { testData ->
            val aglRes = Agl.processorFromStringDefault(testData.grammarString)
            assertTrue(aglRes.issues.errors.isEmpty(), aglRes.issues.toString())
            val proc = aglRes.processor!!
            val parseResult = proc.parse(testData.sentence)
            if (testData.isValid) {
                assertTrue(parseResult.issues.errors.isEmpty(), parseResult.issues.toString())
                val resultStr = parseResult.sppt!!.asSentence
                assertEquals(testData.sentence, resultStr)
            } else {
                assertTrue(parseResult.issues.errors.isNotEmpty())
            }
        }
    }
    context("process") {
        withData(tests) { testData ->
            val aglRes = Agl.processorFromStringDefault(testData.grammarString)
            assertTrue(aglRes.issues.errors.isEmpty(), aglRes.issues.toString())
            val processor = aglRes.processor!!

            val procResult = processor.process(testData.sentence)
            if (testData.isValid) {
                assertTrue(procResult.issues.errors.isEmpty(), procResult.issues.toString())
                //TODO: format and check resulting asm
                //val asm = procResult.asm!!
                //val frmRes = processor.formatAsm(asm)
                // assertEquals(testData.sentence, frmRes.sentence)
            } else {
                assertTrue(procResult.issues.errors.isNotEmpty())
            }
        }
    }
})
