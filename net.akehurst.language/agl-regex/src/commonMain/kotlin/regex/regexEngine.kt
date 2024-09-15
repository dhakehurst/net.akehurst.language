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

package net.akehurst.language.regex.agl

import net.akehurst.language.regex.api.RegexEngine
import net.akehurst.language.regex.api.RegexEngineKind
import net.akehurst.language.regex.api.RegexMatcher


/*
//@JvmInline
//value
data class MatchResultPlatform(val value: kotlin.text.MatchResult) : MatchResult {
    override val matchedText: String
        get() = value.value
    override val eolPositions: List<Int>
        get() = TODO("not implemented")
}
*/

//@JvmInline
//value
data class RegexPlatform(val value: kotlin.text.Regex) : net.akehurst.language.regex.api.Regex {
    override fun matchAt(text: String, atPosition: Int): net.akehurst.language.regex.api.MatchResult? =
        value.matchAt(text, atPosition)?.let { RegexMatcher.MatchResultAgl(it.value) }

    override fun matchesAt(text: String, atPosition: Int): Boolean = value.matchesAt(text, atPosition)
}

//@JvmInline
//value
data class RegexAgl(val value: RegexMatcher) : net.akehurst.language.regex.api.Regex {
    override fun matchAt(text: String, atPosition: Int): RegexMatcher.MatchResultAgl? = value.matchAt(text, atPosition) as RegexMatcher.MatchResultAgl?
    override fun matchesAt(text: String, atPosition: Int): Boolean = value.matchesAt(text, atPosition)
}

object RegexEnginePlatform : RegexEngine {
    override val kind: RegexEngineKind = RegexEngineKind.PLATFORM
    override fun createFor(pattern: String): net.akehurst.language.regex.api.Regex = RegexPlatform(Regex(pattern))
}


object RegexEngineAgl : RegexEngine {
    override val kind: RegexEngineKind = RegexEngineKind.AGL
    override fun createFor(pattern: String): net.akehurst.language.regex.api.Regex = RegexAgl(regexMatcher(pattern))

}