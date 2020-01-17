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

package net.akehurst.language.agl.runtime.structure

import net.akehurst.language.api.parser.ParseException


/**
 * when (kind) {
 *   EMPTY                   -> items[0] == the rule that is empty
 *   CHOICE                  -> items == what to chose between
 *   PRIORITY_CHOICE         -> items == what to chose between, 0 is highest priority (I think)
 *   CONCATENATION           -> items == what to concatinate, in order
 *   UNORDERED               -> items == what to concatinate, any order
 *   MULTI                   -> items[0] == the item to repeat, items[1] == empty rule if min==0
 *   SEPARATED_LIST          -> items[0] == the item to repeat, items[1] == separator, items[2] == empty rule if min==0
 *   LEFT_ASSOCIATIVE_LIST   -> items[0] == the item to repeat, items[1] == separator
 *   RIGHT_ASSOCIATIVE_LIST  -> items[0] == the item to repeat, items[1] == separator
 * }
 */
class RuntimeRuleItem(
  val kind: RuntimeRuleItemKind,
  val choiceKind : RuntimeRuleChoiceKind,
  val multiMin : Int,
  val multiMax : Int,
  val items : Array<out RuntimeRule>
) {

    companion object {
        val EMPTY__RULE_THAT_IS_EMPTY = 0
        val MULTI__ITEM = 0
        val MULTI__EMPTY_RULE = 1
        val SLIST__ITEM = 0
        val SLIST__SEPARATOR = 1
        val SLIST__EMPTY_RULE = 2

    }

    init {
        if (this.items.isEmpty()) {
            throw ParseException("RHS of a non terminal rule must contain some items")
        }
    }

    val EMPTY__ruleThatIsEmpty: RuntimeRule get() { return this.items[EMPTY__RULE_THAT_IS_EMPTY] }
    val MULTI__repeatedItem: RuntimeRule get() { return this.items[MULTI__ITEM] }
    val MULTI__emptyRule: RuntimeRule get() { return this.items[MULTI__EMPTY_RULE] }

    val SLIST__repeatedItem: RuntimeRule get() { return this.items[SLIST__ITEM] }
    val SLIST__separator: RuntimeRule get() { return this.items[SLIST__SEPARATOR] }
    val SLIST__emptyRule: RuntimeRule get() { return this.items[SLIST__EMPTY_RULE] }

    //val listSeparator: RuntimeRule get() { return this.items[1] } //should we check type here or is that runtime overhead?

    fun findItemAt(n: Int): Array<out RuntimeRule> {
        return when (this.kind) {
            RuntimeRuleItemKind.EMPTY -> emptyArray<RuntimeRule>()
            RuntimeRuleItemKind.CHOICE -> this.items //TODO: should maybe test n == 0
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
                    1-> arrayOf(this.SLIST__separator)
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
            RuntimeRuleItemKind.CHOICE -> when(this.choiceKind) {
                RuntimeRuleChoiceKind.NONE -> "ERROR"
                RuntimeRuleChoiceKind.AMBIGUOUS -> "CH_AMB"
                RuntimeRuleChoiceKind.LONGEST_PRIORITY -> "CH_LNG"
                RuntimeRuleChoiceKind.PRIORITY_LONGEST -> "CH_PRI"
            }
            RuntimeRuleItemKind.MULTI -> "MULTI"
            RuntimeRuleItemKind.SEPARATED_LIST -> "SLIST"
            RuntimeRuleItemKind.EMPTY -> "EMPTY"
            else -> throw ParseException("Unsupported at present")
        }
        val itemsStr = items.map { "[${it.number}]" }.joinToString(" ")
        return "(${kindStr}) ${itemsStr}"
    }
}