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
import io.kotest.datatest.IsStableType
import io.kotest.datatest.withData
import korlibs.io.file.VfsFile
import korlibs.io.file.extension
import korlibs.io.file.std.localCurrentDirVfs
import net.akehurst.language.agl.processor.Agl
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val rootFs = localCurrentDirVfs["languages"].jail()

@IsStableType
data class TestData(
    val grammar: GrammarData,
    val sentence: SentenceData,
    val isValid: Boolean
) {
    override fun toString(): String = "${grammar.name} - ${sentence.name}"
}

class GrammarData(
    val name: String,
    val vfsFile: VfsFile
) {
    constructor(vfsFile: VfsFile) : this(vfsFile.path, vfsFile)

    suspend fun grammarStr() = vfsFile.readString()

    override fun hashCode(): Int = name.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        !is GrammarData -> false
        else -> this.name == other.name
    }

    override fun toString(): String = name
}

data class SentenceData(
    val name: String,
    val text: String,
    val goalRule: String?
)

suspend fun VfsFile.walk(recursive: Boolean, func: suspend (file: VfsFile) -> Unit = { }) {
    for (file in listSimple()) {
        val stat = file.stat()
        if (stat.isDirectory) {
            func(file)
            if (recursive) {
                file.walk(recursive, func)
            }
        } else {
            func(file)
        }
    }
}

suspend fun fetchGrammars(langDir: VfsFile): List<GrammarData> {
    val grammars = mutableListOf<GrammarData>()
    when {
        langDir["grammar.agl"].exists() && langDir["grammar.agl"].isFile() -> {
            grammars.add(GrammarData(langDir["grammar.agl"]))
        }

        langDir["grammars"].exists() && langDir["grammars"].isDirectory() -> {
            langDir["grammars"].walk(false) { grammarsEntry ->
                when {
                    grammarsEntry.isDirectory() -> {
                        when {
                            grammarsEntry["grammar.agl"].exists() && grammarsEntry["grammar.agl"].isFile() -> grammars.add(
                                GrammarData(grammarsEntry["grammar.agl"])
                            )

                            else -> Unit // grammar-directory does not contain a 'grammar.agl' file
                        }
                    }

                    grammarsEntry.isFile() -> {
                        when {
                            "agl" == grammarsEntry.extension -> grammars.add(GrammarData(grammarsEntry))
                            else -> Unit // file does not have a '.agl' extension
                        }
                    }

                    else -> Unit // not a file or a directory
                }
            }
        }

        else -> Unit // no grammar.agl file or grammars directory
    }
    return grammars
}

suspend fun fetchSentences(langDir: VfsFile, sentenceKind: String): List<SentenceData> {
    val sentences = mutableListOf<SentenceData>()
    when {
        langDir["${sentenceKind}.txt"].exists() && langDir["${sentenceKind}.txt"].isFile() -> {
            langDir["${sentenceKind}.txt"].readLines().forEach { line ->
                when {
                    line.startsWith("#") -> {
                        val goal = line.substringBefore(":")
                        val sentence = line.substringAfter(":")
                        sentences.add(SentenceData(sentence, sentence, goal))
                    }

                    else -> {
                        if (line.isNotBlank()) {
                            sentences.add(SentenceData(line, line, null))
                        }
                    }
                }
            }
        }

        langDir[sentenceKind].exists() && langDir[sentenceKind].isDirectory() -> {
            langDir[sentenceKind].walk(true) { entry ->
                when {
                    entry.isFile() -> {
                        val content = entry.readString()
                        when {
                            content.startsWith("#") -> {
                                val firstLine = content.substringBefore("\n")
                                val goal = firstLine.substringAfter("#")
                                val sentence = content.substringAfter("\n")
                                sentences.add(SentenceData(entry.relativePathTo(langDir)!!, sentence, goal))
                            }

                            else -> {
                                val sentence = content
                                sentences.add(SentenceData(entry.relativePathTo(langDir)!!, sentence, null))
                            }
                        }
                    }

                    entry.isDirectory() -> Unit // don't use subdirectories
                    else -> Unit // not a file or directory
                }
            }
        }

        else -> error("No '$sentenceKind' sentence file or directory in ${langDir.path}")
    }

    return sentences
}


/**
 * <lang1>
 *     <version 1>
 *         grammar.agl
 *         valid
 *             <sentence-name1>.txt
 *             <sentence-name2>.txt
 *         invalid
 *             <sentence-name1>.txt
 *     <version 2>
 *         grammars
 *             <grammar-name1>.agl
 *             <grammar-name2>.agl
 *         valid.txt
 *         invalid.txt
 * <lang2>
 *     <version 1>
 *         grammars
 *             <grammar-name1>
 *                 grammar.agl
 *             <grammar-name2>
 *                 grammar.agl
 *
 * each language should be in a directory with a name that identifies the language
 * versions of the languages are named directories under that
 *
 * each language/version should have one or more grammars defined either:
 * - a single grammar named 'grammar.agl', or
 * - a directory named 'grammars' containing either:
 * -- one or more '<name>.agl' files that define grammars, or
 * -- one or more named subdirectories containing a 'grammar.agl' file, the name of the directory is the name for the grammar
 *
 */

private suspend fun fetchTestData(): List<TestData> {
    val testData = mutableListOf<TestData>()
    println("languages ${rootFs.listNames()}")
    rootFs.listNames().forEach { lang ->
        rootFs[lang].listNames().forEach { ver ->
            //val grammarPath = "$lang/$ver"
            val grammars = fetchGrammars(rootFs[lang][ver])
            println("grammars for $lang/$ver: ${grammars.joinToString { it.name }}")
            val validSentences = fetchSentences(rootFs[lang][ver], "valid")
            //println("validSentences ${validSentences.joinToString { it.name }}")
            val invalidSentences = fetchSentences(rootFs[lang][ver], "invalid")
            grammars.forEach { grammarData ->
                validSentences.forEach { sentenceData ->
                    testData.add(TestData(grammarData, sentenceData, true))
                }
                invalidSentences.forEach { sentenceData ->
                    testData.add(TestData(grammarData, sentenceData, false))
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
            val aglRes = Agl.processorFromStringDefault(testData.grammar.grammarStr())
            assertTrue(aglRes.issues.errors.isEmpty(), aglRes.issues.toString())
            val processor = aglRes.processor!!
            val parseResult = when {
                null != testData.sentence.goalRule -> processor.parse(testData.sentence.text, Agl.parseOptions { goalRuleName(testData.sentence.goalRule) })
                else -> processor.parse(testData.sentence.text)
            }
            if (testData.isValid) {
                assertTrue(parseResult.issues.errors.isEmpty(), parseResult.issues.toString())
                val resultStr = parseResult.sppt!!.asSentence
                assertEquals(testData.sentence.text, resultStr)
            } else {
                assertTrue(parseResult.issues.errors.isNotEmpty())
            }
        }
    }
    context("process") {
        withData(tests) { testData ->
            val aglRes = Agl.processorFromStringDefault(testData.grammar.grammarStr())
            assertTrue(aglRes.issues.errors.isEmpty(), aglRes.issues.toString())
            val processor = aglRes.processor!!

            val procResult = when {
                null != testData.sentence.goalRule -> processor.process(testData.sentence.text, Agl.options { parse { goalRuleName(testData.sentence.goalRule) } })
                else -> processor.process(testData.sentence.text)
            }
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
