/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.api.semanticAnalyser

import net.akehurst.language.api.parser.InputLocation

class SemanticAnalyserException(message: String, cause: Throwable?) : RuntimeException(message, cause)

/**
 *
 * A Semantic Analyser, language specific functionality
 *
 */
interface SemanticAnalyser<in T> {

    fun clear()

    fun analyse(asm: T, locationMap: Map<Any,InputLocation> = emptyMap()): List<SemanticAnalyserItem>
}

enum class SemanticAnalyserItemKind { ERROR, WARNING }
//FIXME: added because currently Kotlin will not 'export' enums to JS
object SemanticAnalyserItemKind_api {
    val ERROR = SemanticAnalyserItemKind.ERROR
    val WARNING = SemanticAnalyserItemKind.WARNING
}

data class SemanticAnalyserItem (
    val kind: SemanticAnalyserItemKind,
    val location:InputLocation?,
    val message: String
)