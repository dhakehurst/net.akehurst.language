/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.regex.api

enum class RegexEngineKind {
    PLATFORM, AGL
}

interface RegexMatcher {

    data class MatchResultAgl(
        override val matchedText: String,
        //override val eolPositions: List<Int>
    ) : MatchResult

    fun match(text: CharSequence, startPosition: Int = 0): MatchResult?

    fun matchAt(text: String, atPosition: Int): MatchResult?

    fun matchesAt(text: String, atPosition: Int): Boolean

}

interface Regex {
    fun matchesAt(text: String, atPosition: Int): Boolean
    fun matchAt(text: String, atPosition: Int): MatchResult?
}

interface RegexEngine {
    val kind: RegexEngineKind
    fun createFor(pattern: String): Regex
}

interface MatchResult {
    val matchedText: String
    //val eolPositions: List<Int>
}
