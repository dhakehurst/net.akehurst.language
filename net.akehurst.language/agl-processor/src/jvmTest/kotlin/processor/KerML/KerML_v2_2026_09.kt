/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.agl.processor.KerML

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.StyleString
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.issues.api.LanguageIssue
import java.io.File
import kotlin.test.assertTrue

/**
 * The KerML 2026-09 language ('agl' grammar, with references) from the language-repository,
 * also used as the base of the SysML 2026-09 language.
 */
object KerML_v2_2026_09 {

    val languageDir = File("../language-repository/languages/KerML/v2_2026-09")
    val grammarDir = File(languageDir, "grammars/agl")

    /** a new processor for the 'agl' grammar with its style and references (each call creates a new one, as a SysML processor adds its types to it) */
    fun processorWithReferences(): LanguageProcessor<Asm, SentenceContext> {
        val res = Agl.processorFromStringSimple(
            GrammarString(File(grammarDir, "grammar.agl").readText()),
            referenceStr = CrossReferenceString(File(grammarDir, "references.agl").readText()),
            styleStr = StyleString(File(grammarDir, "style.agl").readText()),
        )
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        return res.processor!!
    }

    /** the KerML model libraries (.kerml) */
    val libraryFiles: List<File> get() = sentenceFiles(File(languageDir, "valid/library"), "kerml")

    fun sentenceFiles(dir: File, extension: String) = dir.walkTopDown()
        .filter { it.isFile && it.name.endsWith(".$extension") }
        .sortedBy { it.path }
        .toList()

    /** processes the KerML model libraries into 'context' (references are not checked), returns the errors */
    fun processLibrary(processor: LanguageProcessor<Asm, SentenceContext>, context: SentenceContext): List<LanguageIssue> =
        processFiles(processor, libraryFiles, context, false)

    /** processes 'files' into 'context', each with its path as sentence identity, returns the errors */
    fun processFiles(processor: LanguageProcessor<Asm, SentenceContext>, files: List<File>, context: SentenceContext, checkReferences: Boolean): List<LanguageIssue> =
        files.flatMap { f ->
            processor.process(f.readText(), Agl.options {
                parse { sentenceIdentity { f.path } }
                semanticAnalysis { checkReferences(checkReferences); sentenceContext(context) }
            }).allIssues.errors
        }

    private val packageRegex = Regex("^\\s*(?:library\\s+|standard\\s+)*package\\s+('[^']+'|\\w+)", RegexOption.MULTILINE)

    /**
     * the files of 'files' (other than f) whose top-level packages f uses (as 'P::'), transitively.
     * A package is taken from the folder of the file that uses it; if no file there defines it, from the first file (of 'files') that does.
     */
    fun usedFiles(f: File, files: List<File>): List<File> {
        val pkgs = files.associateWith { g -> packageRegex.findAll(g.readText()).map { it.groupValues[1] }.toList() }
        val definedBy = mutableMapOf<String, MutableList<File>>()
        pkgs.forEach { (g, ps) -> ps.forEach { definedBy.getOrPut(it) { mutableListOf() }.add(g) } }
        val used = mutableListOf<File>()
        val todo = mutableListOf(f)
        while (todo.isNotEmpty()) {
            val u = todo.removeAt(0)
            val t = u.readText()
            for ((pkg, defs) in definedBy) {
                if (t.contains("$pkg::")) {
                    val sameFolder = defs.filter { it.parentFile == u.parentFile }
                    for (g in sameFolder.ifEmpty { defs.take(1) }) {
                        if (g != f && g !in used) {
                            used.add(g); todo.add(g)
                        }
                    }
                }
            }
        }
        return used
    }

    /**
     * processes each of 'files' with references checked, in a context holding the libraries ('libraryContext')
     * and the files that it uses (see usedFiles).
     * Returns the files (relative to 'baseDir') that have errors.
     */
    fun filesWithReferenceErrors(processor: LanguageProcessor<Asm, SentenceContext>, files: List<File>, libraryContext: SentenceContext, baseDir: File): Set<String> =
        files.filter { f ->
            val ctx = libraryContext.union(contextAsmSimple())
            processFiles(processor, usedFiles(f, files), ctx, false)
            processFiles(processor, listOf(f), ctx, true).isNotEmpty()
        }.map { it.relativeTo(baseDir).invariantSeparatorsPath }.toSet()
}
