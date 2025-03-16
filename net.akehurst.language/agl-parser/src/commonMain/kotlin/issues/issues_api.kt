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

package net.akehurst.language.issues.api

import net.akehurst.language.sentence.api.InputLocation

enum class LanguageIssueKind { ERROR, WARNING, INFORMATION }

enum class LanguageProcessorPhase {
    GRAMMAR,
    SCAN, PARSE,
    SYNTAX_ANALYSIS, SEMANTIC_ANALYSIS,
    INTERPRET, GENERATE, FORMAT,
    ALL
}

data class LanguageIssue(
    val kind: LanguageIssueKind,
    val phase: LanguageProcessorPhase,
    val location: InputLocation?,
    val message: String,
    val data: Any? = null //Set<String> for PARSE
)

interface IssueCollection<T : Any> : Collection<T> {
    val all: Set<T>
    val errors: List<T>
    val warnings: List<T>
    val informations: List<T>
}