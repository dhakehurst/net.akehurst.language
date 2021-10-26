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

package net.akehurst.language.api.grammar

interface RuleItem {
    val owningRule: Rule
	fun setOwningRule(rule: Rule, indices: List<Int>) //TODO: make internal not on interface
    val allTerminal: Set<Terminal>
    val allNonTerminal: Set<NonTerminal>
    fun subItem(index: Int): RuleItem
}

interface Choice : RuleItem {
    val alternative: List<Concatenation>
}
interface Concatenation : RuleItem {
    val items: List<ConcatenationItem>
}
interface ConcatenationItem : RuleItem

interface ChoiceEqual : Choice
interface ChoicePriority : Choice
interface ChoiceAmbiguous : Choice

interface SimpleItem : ConcatenationItem
interface ListOfItems : ConcatenationItem {
    val min: Int
    val max: Int
    val item: SimpleItem
}

interface SimpleList : ListOfItems
interface SeparatedList : ListOfItems {
    val separator: SimpleItem
   // val associativity: SeparatedListKind
}
enum class SeparatedListKind { Flat, Left, Right }

interface Group : SimpleItem {
    val choice: Choice
}
interface TangibleItem : SimpleItem {
    val name: String
}

interface EmptyRule : TangibleItem
interface Terminal : TangibleItem {
    val isPattern: Boolean
    val value: String
}
interface NonTerminal : TangibleItem {
    val embedded:Boolean
    val referencedRule: Rule
}
