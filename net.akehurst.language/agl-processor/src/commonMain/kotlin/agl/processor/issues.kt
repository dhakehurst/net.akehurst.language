/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.processor

import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase

class IssueHolder(
    val phase: LanguageProcessorPhase
) {

    private val _issues = mutableListOf<LanguageIssue>()

    val issues: List<LanguageIssue> get() = _issues

    fun clear() {
        _issues.clear()
    }

    fun info(location: InputLocation?, message: String, data: Any? = null) {
        _issues.add(LanguageIssue(LanguageIssueKind.INFORMATION, phase,location,message,data))
    }
    fun warn(location: InputLocation?, message: String, data: Any? = null) {
        _issues.add(LanguageIssue(LanguageIssueKind.WARNING, phase,location,message,data))
    }
    fun error(location: InputLocation?, message: String, data: Any? = null) {
        _issues.add(LanguageIssue(LanguageIssueKind.ERROR, phase,location,message,data))
    }

}