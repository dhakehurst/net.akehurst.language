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


/**
 * when (kind) {
 *   EMPTY                   -> items[0] == the rule that is empty
 *   CHOICE                  -> items == what to chose between
 *   PRIORITY_CHOICE         -> items == what to chose between, 0 is highest priority (I think)
 *   CONCATENATION           -> items == what to concatinate, in order
 *   UNORDERED               -> items == what to concatinate, any order
 *   MULTI                   -> items[0] == the item to repeat, items[1] == empty rule if min==0
 *   SEPARATED_LIST          -> items[0] == the item to repeat, items[1] == separator
 *   LEFT_ASSOCIATIVE_LIST   -> items[0] == the item to repeat, items[1] == separator
 *   RIGHT_ASSOCIATIVE_LIST  -> items[0] == the item to repeat, items[1] == separator
 * }
 */
class RuntimeRuleItem(
  val kind: RuntimeRuleItemKind,
  val multiMin : Int,
  val multiMax : Int,
  val items : Array<out RuntimeRule>
) {
    init {
        if (this.items.isEmpty()) {
            throw ParseException("RHS of a non terminal rule must contain some items")
        }
    }

    val listSeparator: RuntimeRule get() { return this.items[1] } //should we check type here or is that runtime overhead?

    fun findItemAt(n: Int): Array<out RuntimeRule> {
        return when (this.kind) {
            RuntimeRuleItemKind.EMPTY -> emptyArray<RuntimeRule>()
            RuntimeRuleItemKind.CHOICE_PRIORITY -> this.items
            RuntimeRuleItemKind.CHOICE_EQUAL -> this.items //TODO: should maybe test n == 0
            RuntimeRuleItemKind.CONCATENATION -> if (this.items.size > n) arrayOf(this.items[n]) else emptyArray<RuntimeRule>()
            RuntimeRuleItemKind.MULTI -> {
                if ((this.multiMax == -1 || n <= this.multiMax - 1) && n >= this.multiMin - 1) {
                    arrayOf(this.items[0])
                } else {
                    emptyArray<RuntimeRule>()
                }
            }
            RuntimeRuleItemKind.SEPARATED_LIST -> {
                when (n % 2) {
                    0 -> if ((this.multiMax == -1 || n <= this.multiMax - 1) && n >= this.multiMin - 1) {
                        arrayOf(this.items[0])
                    } else {
                        emptyArray<RuntimeRule>()
                    }
                    1-> arrayOf(this.listSeparator)
                    else -> emptyArray<RuntimeRule>() // should never happen!
                }
            }
            RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST -> emptyArray<RuntimeRule>() //TODO:
            RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST -> emptyArray<RuntimeRule>() //TODO:
            RuntimeRuleItemKind.UNORDERED -> emptyArray<RuntimeRule>() //TODO:
        }
    }


    override fun toString(): String {
        val kindStr = when (this.kind) {
            RuntimeRuleItemKind.CONCATENATION -> "CONCAT"
            RuntimeRuleItemKind.CHOICE_PRIORITY -> "CHOPR"
            RuntimeRuleItemKind.CHOICE_EQUAL -> "CHOEQ"
            RuntimeRuleItemKind.MULTI -> "MULTI"
            RuntimeRuleItemKind.SEPARATED_LIST -> "SLIST"
            RuntimeRuleItemKind.EMPTY -> "EMPTY"
            else -> throw ParseException("Unsupported at present")
        }
        val itemsStr = items.map { "[${it.number}]" }.joinToString(" ")
        return "(${kindStr}) ${itemsStr}"
    }
}