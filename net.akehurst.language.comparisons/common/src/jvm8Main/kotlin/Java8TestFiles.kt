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

import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

data class FileData(
        val index: Int,
        val path: Path,
        val chars: Int,
        val isError: Boolean
)

object Java8TestFiles {
    var javaTestFiles = "../javaTestFiles/javac"

    val files: List<FileData>
        get() {
            val params = mutableListOf<Pair<Path, Long>>()
            try {
                val matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java")
                Files.walkFileTree(Paths.get(javaTestFiles), object : SimpleFileVisitor<Path>() {
                    @Throws(IOException::class)
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        if (attrs.isRegularFile && matcher.matches(file)) {
                            val size = attrs.size()
                            params.add(Pair(file, size))
                        }
                        return FileVisitResult.CONTINUE
                    }
                })
            } catch (e: IOException) {
                throw RuntimeException("Error getting files", e)
            }


            val data = params.map {
                val chars = countChars(it.first)
                val isError = containsError(it.first)
                FileData(0, it.first, chars, isError)
            }
            var sorted = data.sortedBy { it.chars }
            var index = 0
            return sorted.map {
                FileData(index++, it.path, it.chars, it.isError)
            }
        }

    fun countChars(path:Path) : Int = path.toFile().readText().length


    fun containsError(path: Path): Boolean {
        val outFilePath = path.parent.resolve(path.fileName.toFile().nameWithoutExtension + ".out")
        return if (outFilePath.toFile().exists()) {
            val txt = outFilePath.toFile().readText()
            //TODO: could make this test for errors better
            txt.contains(Regex("errors|error"))
        } else {
            val txt = path.toFile().readText()
            //TODO: could make this test for errors better
            txt.contains(Regex("errors|error"))
        }
    }
}