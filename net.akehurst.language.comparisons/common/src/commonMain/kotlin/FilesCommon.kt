/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.comparisons.common

import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

data class FileDataCommon(
    val index: Int,
    val path: Path,
    val lineNumber: Int,
    val chars: Int,
    val charsNoComments: Int,
    val isError: Boolean
)

object FilesCommon {

    fun filesRecursiveFromDir(rootInfoFile: String, skipPatterns: Set<Regex>, filter: (path: String) -> Boolean): List<FileDataCommon> {
        val testFiles = readResource(rootInfoFile)
        val rootFs = Path(testFiles)
        val allFilteredFiles = getPathsRecursive(rootFs, filter)
        // val path_size = allFilteredFiles.map { Pair(rootFs[it.path], it.stat().size) }.associate { it }
        val data = allFilteredFiles.map { path ->
            val content = readResource(path.name)
            val chars = countChars(content , skipPatterns)
            FileDataCommon (0, path, 0, chars.first, chars.second, false)
        }
        val sorted = data.sortedBy { it.chars }
        var index = 0
        val files = sorted.map {
            FileDataCommon(index++, it.path, 0, it.chars, it.charsNoComments, it.isError)
        }
        return files
    }

    suspend fun javaFiles(rootInfoFile: String, skipPatterns: Set<Regex>, filter: (path: String) -> Boolean): List<FileDataCommon> {
        // You must create this file and add the full path to the folder containing the javaTestFiles
        val testFiles = readResource(rootInfoFile)
        val rootFs = Path(testFiles)
        val allFilteredFiles = getPathsRecursive(rootFs, filter)
        val data = allFilteredFiles.map { path ->
            val content = readResource(path.name)
            val chars = countChars(content, skipPatterns)
            val isError = containsError(path)
            FileDataCommon(0, path, 0, chars.first, chars.second, isError)
        }
        val sorted = data.sortedBy { it.chars }
        var index = 0
        val files = sorted.map {
            FileDataCommon(index++, it.path, 0, it.chars, it.charsNoComments, it.isError)
        }
        return files
    }

    fun countChars(sentence: String, skipPatterns: Set<Regex>): Pair<Int, Int> {
        var text = sentence
        val rawLength = text.length
        for (pat in skipPatterns) {
            text = text.replace(pat, " ") //replace any skip match with single char
        }
        //remove comments
        //val rem = text.replace(Regex("/\\*[^*]*\\*+([^*/][^*]*\\*+)*/"), "")
        //val rem2 = rem.replace(Regex("//[^\n]*$"), "")
        return Pair(rawLength, text.length)
    }

    private suspend fun containsError(path: Path): Boolean {
        val outFilePath = path.parent?.let { SystemFileSystem.list(it).firstOrNull { p -> p.name.endsWith(".out") } }
        return if (null!=outFilePath ) {
            val txt =readResource( outFilePath.name)
            //TODO: could make this test for errors better
            txt.contains(Regex("errors|error"))
        } else {
            val txt = readResource( path.name)
            //TODO: could make this test for errors better
            txt.contains(Regex("errors|error"))
        }
    }
}