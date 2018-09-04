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
import net.akehurst.language.ogl.runtime.graph.CompleteNode
import net.akehurst.language.ogl.runtime.graph.GrowingNode

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

    fun isCompleteChildren(nextItemIndex: Int, numNonSkipChildren:Int, children:Array<CompleteNode>): Boolean {
        return if (RuntimeRuleKind.TERMINAL == this.kind) {
            true
        } else {
            val rhs: RuntimeRuleItem = this?.rhs ?: throw ParseException("Internal Error: Non Terminal must always have a rhs")
            when (rhs.kind) {
                RuntimeRuleItemKind.EMPTY -> true
                RuntimeRuleItemKind.CHOICE_EQUAL ->
                    // a choice can only have one child
                    // TODO: should never be 1, should always be -1 if we create nodes correctly
                    nextItemIndex == 1 || nextItemIndex == -1
                RuntimeRuleItemKind.CHOICE_PRIORITY ->
                    // a choice can only have one child
                    // TODO: should never be 1, should always be -1 if we create nodes correctly
                    nextItemIndex == 1 || nextItemIndex == -1
                RuntimeRuleItemKind.CONCATENATION -> {
                    // the -1 is used when creating dummy ?
                    // test here!
                    rhs.items.size <= nextItemIndex || nextItemIndex == -1
                }
                RuntimeRuleItemKind.MULTI -> {
                    var res = false
                    if (0 == rhs.multiMin && numNonSkipChildren == 1) {
                        // complete if we have an empty node as child
                        res = if (children.isEmpty()) false else children[0].runtimeRule.isEmptyRule
                    }
                    val size = numNonSkipChildren
                    res || size > 0 && size >= rhs.multiMin || nextItemIndex == -1 // the -1 is used when
                    // creating
                    // dummy branch...should
                    // really need the test here!
                }
                RuntimeRuleItemKind.SEPARATED_LIST -> {
                    val size = numNonSkipChildren
                    size % 2 == 1 || nextItemIndex == -1 // the -1 is used when creating dummy branch...should really need the test here!
                }
                else -> throw RuntimeException("Internal Error: rule kind not recognised")
            }
        }
    }

}