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

package net.akehurst.language.ogl.runtime.structure

import net.akehurst.language.api.parser.ParseException

class RuntimeRule(
        val number: Int,
        val name: String,
        val kind: RuntimeRuleKind,
        val isPattern: Boolean,
        val isSkip: Boolean
) {

    var rhs : RuntimeRuleItem? = null

    val emptyRule : RuntimeRule get() {
        val er = this.rhs?.items?.get(0) ?: throw ParseException("rhs does not contain any rules")
        if (er.isEmptyRule) {
            return er
        } else {
            throw ParseException("this is not an empty rule")
        }
    }

    val isEmptyRule: Boolean get() {
        return rhs?.kind == RuntimeRuleItemKind.EMPTY
    }

    val ruleThatIsEmpty: RuntimeRule get() {
        return this.rhs?.items?.get(0) ?: throw ParseException("There are no items defined")
    }
}