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

import korlibs.io.file.VfsFile
import korlibs.io.file.fullNameWithoutExtension
import korlibs.io.file.std.StandardPaths
import korlibs.io.file.std.localVfs
import korlibs.io.file.std.resourcesVfs
import kotlinx.coroutines.flow.filter

data class FileDataCommon(
    val index: Int,
    val path: VfsFile,
    val chars: Int,
    val charsNoComments: Int,
    val isError: Boolean
)

object Java8TestFilesCommon {

    suspend fun files(): List<FileDataCommon> {
        // You must create this file and add the full path to the folder containing the javaTestFiles
        val javaTestFiles = resourcesVfs["nogit/javaTestFiles.txt"].readString()
        val rootFs = localVfs(javaTestFiles)
        val javaFiles = rootFs.listRecursiveSimple().filter { it.path.endsWith(".java") }
        val path_size = javaFiles.map { Pair(rootFs[it.path], it.stat().size) }.associate { it }
        val data = path_size.map { (path, size) ->
            val chars = countChars(path)
            val isError = containsError(path)
            FileDataCommon(0, path, chars.first, chars.second, isError)
        }
        val sorted = data.sortedBy { it.chars }
        var index = 0
        val files = sorted.map {
            FileDataCommon(index++, it.path, it.chars, it.charsNoComments, it.isError)
        }
        return files
    }

    private suspend fun countChars(path: VfsFile): Pair<Int, Int> {
        val text = path.readString()
        //remove comments
        val rem = text.replace(Regex("/\\*[^*]*\\*+([^*/][^*]*\\*+)*/"), "")
        val rem2 = rem.replace(Regex("//[^\n]*$"), "")
        return Pair(text.length, rem2.length)
    }

    private suspend fun containsError(path: VfsFile): Boolean {
        val outFilePath = path.parent[path.pathInfo.fullNameWithoutExtension + ".out"]
        return if (outFilePath.exists()) {
            val txt = outFilePath.readString()
            //TODO: could make this test for errors better
            txt.contains(Regex("errors|error"))
        } else {
            val txt = path.readString()
            //TODO: could make this test for errors better
            txt.contains(Regex("errors|error"))
        }
    }
}