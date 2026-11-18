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
import io.kotest.engine.stable.IsStableType
import korlibs.io.file.VfsFile
import korlibs.io.file.extension
import korlibs.io.file.std.localCurrentDirVfs
import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.LanguageProcessorResult
import net.akehurst.language.api.processor.AsmTransformString
import net.akehurst.language.api.processor.CompletionString
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.StyleString
import net.akehurst.language.api.processor.TypesString
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.api.AsmValue
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val rootFs = localCurrentDirVfs["languages"].jail()
val filterBy = { n:String -> true }
//val filterBy = { n:String -> n == "_private" }

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
    val vfsFile: VfsFile,
    /** directory that holds the other definition files of the language (types, transform, style, ...), null if there are none */
    val definitionDir: VfsFile?,
    /** extensions of definition files that are not used, see [TestOptions] */
    val skippedExtensions: Set<String>
) {
    constructor(vfsFile: VfsFile, definitionDir: VfsFile?, skippedExtensions: Set<String>) : this(vfsFile.path, vfsFile, definitionDir, skippedExtensions)

    suspend fun grammarStr() = GrammarString(vfsFile.readString())

    /** the single file with the given extension in the definition directory, null if there is none, error if there are several */
    suspend fun definitionStr(extension: String): String? {
        val dir = definitionDir ?: return null
        if (skippedExtensions.contains(extension)) return null
        val files = dir.listSimple().filter { it.isFile() && it.extension == extension }
        return when (files.size) {
            0 -> null
            1 -> files[0].readString()
            else -> error("More than one '.$extension' file in ${dir.path}: ${files.joinToString { it.path }}")
        }
    }

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
    /** '<rule>' or '<grammar>::<rule>' */
    val goalRule: String?,
    /** expected type name of the root asm element, null if not checked */
    val expectedType: String? = null
) {
    /** the grammar to build the processor for, null for the default grammar */
    val targetGrammarName: String? get() = goalRule?.substringBefore("::", "")?.ifEmpty { null }
    val goalRuleName: String? get() = goalRule?.substringAfter("::")
}

// one processor per grammar file and target grammar, building a processor for every sentence is too slow
private val processorResults = mutableMapOf<Pair<String, String?>, LanguageProcessorResult<Asm, SentenceContext>>()

/** a language definition and the grammar to build the processor for (null for the default grammar) */
@IsStableType
data class DefinitionData(
    val grammar: GrammarData,
    val targetGrammarName: String?
) {
    override fun toString(): String = "${grammar.name}${targetGrammarName?.let { " - $it" } ?: ""}"
}

suspend fun processorResultFor(testData: TestData): LanguageProcessorResult<Asm, SentenceContext> =
    processorResultFor(DefinitionData(testData.grammar, testData.sentence.targetGrammarName))

suspend fun processorResultFor(definition: DefinitionData): LanguageProcessorResult<Asm, SentenceContext> {
    val key = Pair(definition.grammar.name, definition.targetGrammarName)
    val grammar = definition.grammar
    return processorResults[key] ?: Agl.processorFromStringSimple(
        grammarDefinitionStr = grammar.grammarStr(),
        typeStr = grammar.definitionStr("agl-typ")?.let { TypesString(it) },
        transformStr = grammar.definitionStr("agl-trf")?.let { AsmTransformString(it) },
        referenceStr = grammar.definitionStr("agl-ref")?.let { CrossReferenceString(it) },
        styleStr = grammar.definitionStr("agl-sty")?.let { StyleString(it) },
        formatterStr = grammar.definitionStr("agl-fmt")?.let { FormatString(it) },
        completionStr = grammar.definitionStr("agl-cpl")?.let { CompletionString(it) },
        configurationBase = Agl.configuration(Agl.configurationSimple()) {
            definition.targetGrammarName?.let { targetGrammarName(it) }
        }
    ).also { processorResults[key] = it }
}

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

/** the grammar of a directory that holds all definition files of one language */
suspend fun fetchLanguageDefinition(defDir: VfsFile, skipped: Set<String>): GrammarData? = when {
    defDir["grammar.agl-grm"].exists() && defDir["grammar.agl-grm"].isFile() -> GrammarData(defDir["grammar.agl-grm"], defDir, skipped)
    else -> null // definition directory does not contain a 'grammar.agl-grm' file
}

/**
 * options of test_AllLanguages for a language/version, in a file 'test_AllLanguages.options' in the language/version directory,
 * one option per line, '//' starts a comment (give the reason for a skip):
 * - 'skip: <extension>' do not use the definition files with that extension (e.g. 'skip: agl-ref')
 * - 'sentences: one-per-line' read every sentence file with one sentence per line (see [fetchSentencesOnePerLine])
 */
data class TestOptions(
    val skippedExtensions: Set<String>,
    val sentencesOnePerLine: Boolean
)

suspend fun fetchTestOptions(langDir: VfsFile): TestOptions {
    val optionsFile = langDir["test_AllLanguages.options"]
    val lines = when {
        optionsFile.exists() && optionsFile.isFile() -> optionsFile.readLines()
            .map { it.substringBefore("//").trim() }
            .filter { it.isNotEmpty() }.toList()

        else -> emptyList()
    }
    val skipped = mutableSetOf<String>()
    var onePerLine = false
    lines.forEach { line ->
        val key = line.substringBefore(":").trim()
        val value = line.substringAfter(":", "").trim()
        when (key) {
            "skip" -> skipped.add(value)
            "sentences" -> when (value) {
                "one-per-line" -> onePerLine = true
                else -> error("Unknown value '$value' for option 'sentences' in ${optionsFile.path}")
            }

            else -> error("Unknown option '$line' in ${optionsFile.path}")
        }
    }
    return TestOptions(skipped, onePerLine)
}

suspend fun fetchGrammars(langDir: VfsFile, options: TestOptions): List<GrammarData> {
    val grammars = mutableListOf<GrammarData>()
    val skipped = options.skippedExtensions
    when {
        langDir["grammar.agl-grm"].exists() && langDir["grammar.agl-grm"].isFile() -> {
            grammars.add(GrammarData(langDir["grammar.agl-grm"], langDir, skipped))
        }

        langDir["language"].exists() && langDir["language"].isDirectory() -> {
            fetchLanguageDefinition(langDir["language"], skipped)?.let { grammars.add(it) }
        }

        langDir["languages"].exists() && langDir["languages"].isDirectory() -> {
            langDir["languages"].walk(false) { variant ->
                when {
                    variant.isDirectory() -> fetchLanguageDefinition(variant, skipped)?.let { grammars.add(it) }
                    else -> Unit // only directories hold language variants
                }
            }
        }

        langDir["grammars"].exists() && langDir["grammars"].isDirectory() -> {
            langDir["grammars"].walk(false) { grammarsEntry ->
                when {
                    grammarsEntry.isDirectory() -> fetchLanguageDefinition(grammarsEntry, skipped)?.let { grammars.add(it) }

                    grammarsEntry.isFile() -> {
                        when {
                            "agl-grm" == grammarsEntry.extension -> grammars.add(GrammarData(grammarsEntry, null, skipped))
                            else -> Unit // file does not have a '.agl-grm' extension
                        }
                    }

                    else -> Unit // not a file or a directory
                }
            }
        }

        else -> Unit // no grammar.agl-grm file, language, languages or grammars directory
    }
    return grammars
}

/**
 * one sentence per line, the goal and the expected type start unset in each file:
 * - '#<goal>' sets the goal rule ('<rule>' or '<grammar>::<rule>') for the following lines
 * - '#!<type>' sets the expected type of the root asm element for the following lines
 * - '!<type>:<sentence>' a sentence with its own expected type
 * - '//' at the start of a line is a comment, blank lines are ignored
 * - any other line is a sentence
 */
suspend fun fetchSentencesOnePerLine(file: VfsFile, name: String): List<SentenceData> {
    val sentences = mutableListOf<SentenceData>()
    var goal: String? = null
    var type: String? = null
    file.readLines().forEachIndexed { index, line ->
        val sentenceName = "$name:${index + 1}"
        when {
            line.isBlank() -> Unit // blank line
            line.startsWith("//") -> Unit // comment
            line.startsWith("#!") -> type = line.substringAfter("#!").trim().ifEmpty { null }
            line.startsWith("#") -> goal = line.substringAfter("#").trim().ifEmpty { null }
            line.startsWith("!") && line.contains(":") -> sentences.add(SentenceData(sentenceName, line.substringAfter(":"), goal, line.substring(1).substringBefore(":").trim()))
            else -> sentences.add(SentenceData(sentenceName, line, goal, type))
        }
    }
    return sentences
}

suspend fun fetchSentences(langDir: VfsFile, sentenceKind: String, options: TestOptions): List<SentenceData> {
    val sentences = mutableListOf<SentenceData>()
    when {
        options.sentencesOnePerLine -> when {
            langDir["${sentenceKind}.txt"].exists() && langDir["${sentenceKind}.txt"].isFile() -> {
                sentences.addAll(fetchSentencesOnePerLine(langDir["${sentenceKind}.txt"], "${sentenceKind}.txt"))
            }

            langDir[sentenceKind].exists() && langDir[sentenceKind].isDirectory() -> {
                langDir[sentenceKind].walk(true) { entry ->
                    when {
                        entry.isFile() -> sentences.addAll(fetchSentencesOnePerLine(entry, entry.relativePathTo(langDir)!!))
                        else -> Unit // directories are walked, nothing else is used
                    }
                }
            }

            else -> println("No '$sentenceKind' sentence file or directory in ${langDir.path}") // no sentences of this kind
        }

        langDir["${sentenceKind}.txt"].exists() && langDir["${sentenceKind}.txt"].isFile() -> {
            // one sentence per line
            // '//# <goal>' sets the goal rule for the following lines, '//# ' alone goes back to the default goal
            // '// ...' is a comment, blank lines are ignored
            var goal: String? = null
            langDir["${sentenceKind}.txt"].readLines().forEach { rawLine ->
                val line = rawLine.trim()
                when {
                    line.isEmpty() -> Unit // blank line
                    line.startsWith("//#") -> goal = line.substringAfter("//#").trim().ifEmpty { null }
                    line.startsWith("//") -> Unit // comment
                    else -> sentences.add(SentenceData(line, line, goal))
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

        else -> println("No '$sentenceKind' sentence file or directory in ${langDir.path}") // no sentences of this kind
    }

    return sentences
}


/**
 * <lang1>
 *     <version 1>
 *         grammar.agl-grm
 *         valid
 *             <sentence-name1>.txt
 *             <sentence-name2>.txt
 *         invalid
 *             <sentence-name1>.txt
 *     <version 2>
 *         grammars
 *             <grammar-name1>.agl-grm
 *             <grammar-name2>.agl-grm
 *         valid.txt
 *         invalid.txt
 * <lang2>
 *     <version 1>
 *         grammars
 *             <grammar-name1>
 *                 grammar.agl-grm
 *             <grammar-name2>
 *                 grammar.agl-grm
 *
 * each language should be in a directory with a name that identifies the language
 * versions of the languages are named directories under that
 *
 * each language/version should have one or more grammars defined either:
 * - a single grammar named 'grammar.agl-grm', or
 * - a directory named 'language' that holds all definition files of the language, or
 * - a directory named 'languages' with one named subdirectory per variant of the language, each holds all definition files of that variant, or
 * - a directory named 'grammars' containing either:
 * -- one or more '<name>.agl-grm' files that define grammars, or
 * -- one or more named subdirectories containing a 'grammar.agl-grm' file, the name of the directory is the name for the grammar
 *
 * the other definition files of a language are found by extension next to its 'grammar.agl-grm':
 * '.agl-typ' types, '.agl-trf' asm-transform, '.agl-ref' references, '.agl-sty' style, '.agl-fmt' format, '.agl-cpl' completion
 * (at most one file of each), all are used to build the processor;
 * a file 'test_AllLanguages.options' in the language/version directory can switch off the use of definition files, see [TestOptions]
 *
 * sentences are either:
 * - a directory 'valid' / 'invalid', each file is one sentence; a first line '#<goal>' gives the goal rule ('<rule>' or '<grammar>::<rule>'), or
 * - a file 'valid.txt' / 'invalid.txt' with one sentence per line;
 *   a line '//# <goal>' sets the goal rule ('<rule>' or '<grammar>::<rule>') for the following lines, a line '//' starts a comment, blank lines are ignored
 * - with the option 'sentences: one-per-line' (see [TestOptions]): the file 'valid.txt' / 'invalid.txt', or every file in the directory 'valid' / 'invalid',
 *   holds one sentence per line, with '#<goal>', '#!<type>' and '!<type>:<sentence>' lines, see [fetchSentencesOnePerLine]
 *
 */

private suspend fun fetchTestData(): List<TestData> {
    val testData = mutableListOf<TestData>()
    println("languages ${rootFs.listNames()}")
    rootFs.listNames().filter(filterBy)
        .forEach { lang ->
        if (lang.startsWith(".")) {
            //do nothing, avoid dirs such as .DS_Store
        } else {
            rootFs[lang].listNames().forEach { ver ->
                //val grammarPath = "$lang/$ver"
                val options = fetchTestOptions(rootFs[lang][ver])
                val grammars = fetchGrammars(rootFs[lang][ver], options)
                println("grammars for $lang/$ver: ${grammars.joinToString { it.name }}")
                val validSentences = fetchSentences(rootFs[lang][ver], "valid", options)
                //println("validSentences ${validSentences.joinToString { it.name }}")
                val invalidSentences = fetchSentences(rootFs[lang][ver], "invalid", options)
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
    // errors in the definition files are reported once per definition here, process gives them again for every sentence
    context("definitions") {
        withData(tests.map { DefinitionData(it.grammar, it.sentence.targetGrammarName) }.distinct()) { definition ->
            val aglRes = processorResultFor(definition)
            assertTrue(aglRes.issues.errors.isEmpty(), aglRes.issues.toString())
            val processor = aglRes.processor!!
            // the definitions are created lazily, create them all so that their issues are collected
            processor.transformDomain
            processor.crossReferenceDomain
            processor.syntaxAnalyser
            processor.semanticAnalyser
            processor.formatDomain
            processor.completionDomain
            processor.completionProvider
            val styleIssues = when (definition.grammar.definitionStr("agl-sty")) {
                null -> emptyList() // no style file, the default style resolver is not used here
                else -> processor.configuration.styleResolver?.invoke(processor)?.allIssues?.errors?.map { it.copy(message = "In the style definition of the language: ${it.message}") } ?: emptyList()
            }
            val errors = processor.allIssues.errors + styleIssues
            assertTrue(errors.isEmpty(), errors.joinToString("\n"))
        }
    }
    context("parse") {
        withData(tests) { testData ->
            val aglRes = processorResultFor(testData)
            assertTrue(aglRes.issues.errors.isEmpty(), aglRes.issues.toString())
            val processor = aglRes.processor!!
            val parseResult = when {
                null != testData.sentence.goalRule -> processor.parse(testData.sentence.text, Agl.parseOptions { goalRuleName(testData.sentence.goalRuleName) })
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
            val aglRes = processorResultFor(testData)
            assertTrue(aglRes.issues.errors.isEmpty(), aglRes.issues.toString())
            val processor = aglRes.processor!!

            val procResult = when {
                null != testData.sentence.goalRule -> processor.process(testData.sentence.text, Agl.options { parse { goalRuleName(testData.sentence.goalRuleName) } })
                else -> processor.process(testData.sentence.text)
            }
            if (testData.isValid) {
                assertTrue(procResult.allIssues.errors.isEmpty(), procResult.allIssues.toString())
                testData.sentence.expectedType?.let { expectedType ->
                    val root = procResult.asm!!.root
                    assertEquals(1, root.size, "Expected one root asm element of type '$expectedType'")
                    val actualType = (root[0] as? AsmValue)?.typeName?.value
                    assertEquals(expectedType, actualType, "Type of the root asm element")
                }
                //TODO: format and check resulting asm
                //val asm = procResult.asm!!
                //val frmRes = processor.formatAsm(asm)
                // assertEquals(testData.sentence, frmRes.sentence)
            } else {
                assertTrue(procResult.allIssues.errors.isNotEmpty())
            }
        }
    }
})
