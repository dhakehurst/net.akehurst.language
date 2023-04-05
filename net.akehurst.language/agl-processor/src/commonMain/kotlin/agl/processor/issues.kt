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
import net.akehurst.language.api.processor.IssueCollection
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase

operator fun IssueCollection.plus(other: IssueCollection):IssueHolder {
    val issues = IssueHolder(LanguageProcessorPhase.ALL)
    issues.addAll(this)
    issues.addAll(other)
    return issues
}

class IssueHolder(
    val phase: LanguageProcessorPhase
) : IssueCollection {

    private val _issues = mutableSetOf<LanguageIssue>()

    override val all: Set<LanguageIssue> get() = _issues
    override val errors: List<LanguageIssue> get() = _issues.filter { it.kind == LanguageIssueKind.ERROR }
    override val warnings: List<LanguageIssue> get() = _issues.filter { it.kind == LanguageIssueKind.WARNING }
    override val informations: List<LanguageIssue> get() = _issues.filter { it.kind == LanguageIssueKind.INFORMATION }

    fun clear() {
        _issues.clear()
    }

    fun info(location: InputLocation?, message: String, data: Any? = null) {
        _issues.add(LanguageIssue(LanguageIssueKind.INFORMATION, phase, location, message, data))
    }

    fun warn(location: InputLocation?, message: String, data: Any? = null) {
        _issues.add(LanguageIssue(LanguageIssueKind.WARNING, phase, location, message, data))
    }

    fun error(location: InputLocation?, message: String, data: Any? = null) {
        _issues.add(LanguageIssue(LanguageIssueKind.ERROR, phase, location, message, data))
    }

    fun addAll(other:IssueCollection) {
        this._issues.addAll(other.all)
    }

    override val size: Int get() = this._issues.size
    override fun iterator(): Iterator<LanguageIssue> = this._issues.iterator()
    override fun isEmpty(): Boolean = this._issues.isEmpty()
    override fun contains(element: LanguageIssue): Boolean = this._issues.contains(element)
    override fun containsAll(elements: Collection<LanguageIssue>): Boolean = this._issues.containsAll(elements)

    override fun toString(): String = this._issues.joinToString(separator = "\n") { "$it" }

}