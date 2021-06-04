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

import java.time.Duration
import java.time.Instant

class TimeLogger(private val col: String, private val fileData: FileData) : AutoCloseable {
    private val start: Instant
    private var success: Boolean
    fun success() {
        success = true
    }

    override fun close() {
        val end = Instant.now()
        val d = Duration.between(start, end)
        Results.log(success, col, fileData, d)
        println("Duration : ${d.toMillis()} ms")
    }

    init {
        start = Instant.now()
        success = false
    }
}