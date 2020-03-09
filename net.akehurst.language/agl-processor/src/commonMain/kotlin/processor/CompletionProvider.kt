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

package net.akehurst.language.processor

import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.processor.CompletionItem

class CompletionProvider {

	fun provideFor(item: RuleItem, desiredDepth: Int): List<CompletionItem> {
		val rule = item.owningRule
		val cis = getText(item, desiredDepth, emptySet())
		return cis.map { CompletionItem(item.owningRule,it.text) }
	}

	fun getText(item: RuleItem, desiredDepth: Int, done:Set<RuleItem>) : List<CompletionItem> {
		return when {
			done.contains(item) -> emptyList()
			else -> when (item) {
				is Choice -> item.alternative.flatMap { getText(it, desiredDepth, done+item) }
				is Concatenation -> getText(item.items[0], desiredDepth, done+item)
				is Terminal -> if (item.isPattern) {
					listOf(CompletionItem(item.owningRule, item.name))
				} else {
					listOf(CompletionItem(item.owningRule, item.value))
				}
				is NonTerminal -> getText(item.referencedRule.rhs, desiredDepth - 1, done+item)
				else ->  error("not yet supported!")
			}
		}
	}
}