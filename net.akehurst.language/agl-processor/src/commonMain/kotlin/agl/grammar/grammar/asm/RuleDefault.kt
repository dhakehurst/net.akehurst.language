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

package net.akehurst.language.agl.grammar.grammar.asm

import net.akehurst.language.agl.grammar.grammar.ConverterToRuntimeRules
import net.akehurst.language.api.grammar.*

data class RuleDefault(
		override val grammar: GrammarDefault,
		override val name: String,
		override val isOverride:Boolean,
		override val isSkip: Boolean,
		override val isLeaf: Boolean
) : Rule {

	companion object {
		class CompressedLeafRule(override val name: String, override val value: String, override val isPattern: Boolean) : Terminal, RuleItemAbstract() {
			override val allTerminal: Set<Terminal>  = setOf(this)
			override val allNonTerminal: Set<NonTerminal> = emptySet()

			override fun setOwningRule(rule: Rule, indices: List<Int>) {
				TODO("not implemented")
			}

			override fun subItem(index: Int): RuleItem {
				TODO("not implemented")
			}

		}

		private fun toRegEx(value: String): String {
			return Regex.escape(value)
		}

		private fun compressRuleItem(compressedName:String, item: RuleItem): CompressedLeafRule {
			val grammar = item.owningRule.grammar
			return when(item) {
				is Terminal -> when {
					item.isPattern -> CompressedLeafRule(compressedName, "(${item.value})", true)
					else -> CompressedLeafRule(compressedName, "(${toRegEx(item.value)})", true)
				}
				is Concatenation -> {
					if (1 == item.items.size) {
						this.compressRuleItem(compressedName,item.items[0])
					} else {
						val items = item.items.mapIndexed { idx,it -> this.compressRuleItem("$compressedName$idx",it) }
						val pattern = items.joinToString(separator = "") { "(${it.value})" }
						CompressedLeafRule(compressedName, pattern, true)
						//throw GrammarExeception("Rule ${rhs.owningRule.name}, compressing ${rhs::class} to leaf is not yet supported", null)
					}
				}
				is Choice -> {
					val ct = item.alternative.mapIndexed { idx,it -> this.compressRuleItem("$compressedName$idx",it) }
					val pattern = ct.joinToString(separator = "|") { it.value }
					CompressedLeafRule(compressedName, pattern, true)
				}
				is SimpleList -> {
					val ct = this.compressRuleItem("${compressedName}List",item.item)
					val min = item.min
					val max = if (-1 == item.max) "" else item.max
					val pattern = "(${ct.value}){${min},${max}}"
					CompressedLeafRule(compressedName, pattern, true)
				}
				is Group -> {
					val ct = this.compressRuleItem("${compressedName}Group",item.choice)
					val pattern = "(${ct.value})"
					CompressedLeafRule(compressedName, pattern, true)
				}
				is NonTerminal -> {
					//TODO: handle overridden vs embedded rules!
					//TODO: need to catch the recursion before this
					this.compressRuleItem(compressedName,item.referencedRule(grammar).rhs)
				}
				else -> throw GrammarExeception("Rule ${item.owningRule.name}, compressing ${item::class} to leaf is not yet supported", null)
			}
		}

	}

	init {
		this.grammar.rule.add(this)
	}
	
	private var _rhs: RuleItem? = null
	override var rhs: RuleItem
		get() {
			return this._rhs ?: throw GrammarExeception("rhs of rule must be set",null)
		}
		set(value) {
			value.setOwningRule(this, listOf(0))
			this._rhs = value
		}

	override val nodeType: NodeType = NodeTypeDefault(this.name)

	override val compressedLeaf: Terminal by lazy { compressRuleItem(this.name, this.rhs) }

	override fun toString(): String{
		var f = ""
		if (isOverride) f+="override "
		if (isSkip) f+="skip "
		if (isLeaf) f+="leaf "
		return "$f$name = $rhs ;"
	}
}
