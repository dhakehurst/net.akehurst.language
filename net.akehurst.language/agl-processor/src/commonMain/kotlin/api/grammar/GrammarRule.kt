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

interface PreferenceRule : GrammarItem {
    val grammar: Grammar
    val forItem: SimpleItem
    val optionList: List<PreferenceOption>
}

interface PreferenceOption {

    enum class Associativity { LEFT, RIGHT }

    val item: NonTerminal
    val choiceNumber:Int
    val onTerminals: List<SimpleItem>
    val associativity: Associativity
}

interface GrammarRule : GrammarItem {
    val grammar: Grammar
    val name: String
    val isOverride: Boolean
    val isSkip: Boolean
    val isLeaf: Boolean
    val isOneEmebedded: Boolean
    var rhs: RuleItem
    val nodeType: NodeType

    val compressedLeaf: Terminal
}
