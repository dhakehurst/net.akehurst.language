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

import korlibs.io.file.std.StandardPaths
import korlibs.io.file.std.localVfs
import korlibs.io.file.std.resourcesVfs
import korlibs.time.DateTime
import kotlin.time.Duration

object ResultsCommon {

    data class Result(
        val success: Boolean,
        val col: String,
        val fileData: FileDataCommon,
        val value: Duration
    )

    val resultsByIndex = mutableMapOf<Int, MutableMap<String, Result>>()

    fun reset() {
        this.resultsByIndex.clear()
    }

    fun log(success: Boolean, col: String, fileData: FileDataCommon, value: Duration) {
        val res = Result(success, col, fileData, value)
        val results = if (resultsByIndex.containsKey(fileData.index)) {
            resultsByIndex[fileData.index]!!
        } else {
            val m = mutableMapOf<String, Result>()
            resultsByIndex[fileData.index] = m
            m
        }
        results[col] = res
    }

    fun logError(col: String, fileData: FileDataCommon) {
        val res = Result(false, col, fileData, Duration.ZERO)
        val results = if (resultsByIndex.containsKey(fileData.index)) {
            resultsByIndex[fileData.index]!!
        } else {
            val m = mutableMapOf<String, Result>()
            resultsByIndex[fileData.index] = m
            m
        }
        results[col] = res
    }

    private fun dateTimeNow(): String = DateTime.now().format("yyyy-MM-dd_HH-mm")

    suspend fun write(name: String) {
        try {
            // You must create this file and add the full path to the folder containing the javaTestFiles
            val resultFolder = resourcesVfs["nogit/resultFolder.txt"].readString()
            val rootFs = localVfs(resultFolder)
            val sorted = this.resultsByIndex.entries.sortedBy { it.key }
            val lines = mutableListOf<String>()
            val header = this.resultsByIndex.values.first().values.joinToString { it.col }
            lines.add(header)
            for (entry in sorted) {
                val l = entry.value.values.joinToString { "${it.value.inWholeMicroseconds}" }
                lines.add(l)
            }
            val resultsFileOut = rootFs["${name}_${dateTimeNow()}.csv"]
            resultsFileOut.ensureParents()
            resultsFileOut.writeLines(lines)
            println("Written: ${resultsFileOut.getUnderlyingUnscapedFile().path}")
        } catch (ex: Throwable) {
            throw RuntimeException("Error logging results", ex)
        }
    }

}