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

package net.akehurst.language.issues.ram

import net.akehurst.language.issues.api.IssueCollection
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.sentence.api.InputLocation

operator fun IssueCollection<LanguageIssue>.plus(other: IssueCollection<LanguageIssue>): IssueHolder {
    val issues = IssueHolder(LanguageProcessorPhase.ALL)
    issues.addAllFrom(this)
    issues.addAllFrom(other)
    return issues
}

class IssueHolder(
    private val defaultPhase: LanguageProcessorPhase
) : IssueCollection<LanguageIssue> {

    private val _issues = mutableSetOf<LanguageIssue>()

    override val all: Set<LanguageIssue> get() = _issues
    override val errors: List<LanguageIssue> get() = _issues.filter { it.kind == LanguageIssueKind.ERROR }
    override val warnings: List<LanguageIssue> get() = _issues.filter { it.kind == LanguageIssueKind.WARNING }
    override val informations: List<LanguageIssue> get() = _issues.filter { it.kind == LanguageIssueKind.INFORMATION }

    fun clear() {
        _issues.clear()
    }

    fun raise(kind: LanguageIssueKind, phase: LanguageProcessorPhase, location: InputLocation?, message: String, data: Any? = null) {
        _issues.add(LanguageIssue(kind, phase, location, message, data))
    }

    fun info(location: InputLocation?, message: String, data: Any? = null) =
        raise(LanguageIssueKind.INFORMATION, defaultPhase, location, message, data)


    fun warn(location: InputLocation?, message: String, data: Any? = null) =
        raise(LanguageIssueKind.WARNING, defaultPhase, location, message, data)


    fun error(location: InputLocation?, message: String, data: Any? = null) =
        raise(LanguageIssueKind.ERROR, defaultPhase, location, message, data)

    fun addAll(issues: Iterable<LanguageIssue>) {
        this._issues.addAll(issues)
    }

    fun addAllFrom(other: IssueCollection<LanguageIssue>) {
        this.addAll(other.all)
    }

    override val size: Int get() = this._issues.size
    override fun iterator(): Iterator<LanguageIssue> = this._issues.iterator()
    override fun isEmpty(): Boolean = this._issues.isEmpty()
    override fun contains(element: LanguageIssue): Boolean = this._issues.contains(element)
    override fun containsAll(elements: Collection<LanguageIssue>): Boolean = this._issues.containsAll(elements)

    override fun toString(): String = this._issues.joinToString(separator = "\n") { "$it" }

}