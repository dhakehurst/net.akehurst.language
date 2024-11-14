/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.grammar.builder

import net.akehurst.language.base.api.Namespace
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.grammar.api.*
import net.akehurst.language.grammar.asm.*

@DslMarker
annotation class GrammarBuilderMarker

fun grammar(namespace: String, name: String, init: GrammarBuilder.() -> Unit): Grammar {
    val ns = GrammarNamespaceDefault(QualifiedName(namespace))
    val gr = GrammarDefault(ns, SimpleName(name), emptyList())
    ns.addDefinition(gr)
    val b = GrammarBuilder(gr)
    b.init()
    return b.build()
}

//fun (grammar: GrammarAbstract, init: GrammarBuilder.() -> Unit): Grammar {
//    val b = GrammarBuilder(grammar)
//    b.init()
//    return b.build()
//}

@GrammarBuilderMarker
class GrammarBuilder internal constructor(internal val grammar: GrammarAbstract) {

    private val _terminals = mutableMapOf<String, Terminal>()

    fun extends(nameOrQName: String) {
        grammar.extends.add(GrammarReferenceDefault(grammar.namespace, nameOrQName.asPossiblyQualifiedName))
    }

    fun extendsGrammar(extended: GrammarReference) {
        grammar.extends.add(extended)
    }

    private fun terminal(value: String, isPattern: Boolean): Terminal {
        val t = _terminals[value]
        return if (null == t) {
            val tt = TerminalDefault(value, isPattern)
            _terminals[value] = tt
            //tt.grammar = this.grammar
            tt
        } else {
            if (isPattern == t.isPattern) {
                t
            } else {
                error("Error terminal defined as both pattern and literal!")
            }
        }
    }

    private fun createRule(grammarRuleName: GrammarRuleName, overrideKind: OverrideKind?, isSkip: Boolean, isLeaf: Boolean, rhs: RuleItem) = when (overrideKind) {
        null -> {
            val gr = NormalRuleDefault(this.grammar, grammarRuleName, isSkip, isLeaf)
            gr.rhs = rhs
            gr
        }

        else -> {
            val gr = OverrideRuleDefault(this.grammar, grammarRuleName, isSkip, isLeaf, overrideKind)
            gr.overriddenRhs = rhs
            gr
        }
    }

    fun terminalLiteral(value: String): Terminal {
        return terminal(value, false)
    }

    fun terminalPattern(value: String): Terminal {
        return terminal(value, true)
    }

    fun empty(grammarRuleName: String, overrideKind: OverrideKind? = null, isSkip: Boolean = false, isLeaf: Boolean = false) {
        val rhs = EmptyRuleDefault()
        val gr = createRule(GrammarRuleName(grammarRuleName), overrideKind, isSkip, isLeaf, rhs)
        this.grammar.grammarRule.add(gr)
    }

    fun choice(grammarRuleName: String, overrideKind: OverrideKind? = null, isSkip: Boolean = false, isLeaf: Boolean = false, init: ChoiceItemBuilder.() -> Unit) {
        val ib = ChoiceItemBuilder(grammar.namespace)
        ib.init()
        val items = ib.build()
        val rhs = ChoiceLongestDefault(items)
        val gr = createRule(GrammarRuleName(grammarRuleName), overrideKind, isSkip, isLeaf, rhs)
        this.grammar.grammarRule.add(gr)
    }

    fun concatenation(grammarRuleName: String, overrideKind: OverrideKind? = null, isSkip: Boolean = false, isLeaf: Boolean = false, init: ConcatenationItemBuilder.() -> Unit) {
        val ib = ConcatenationItemBuilder(grammar.namespace)
        ib.init()
        val items = ib.build()
        val rhs = ConcatenationDefault(items)
        val gr = createRule(GrammarRuleName(grammarRuleName), overrideKind, isSkip, isLeaf, rhs)
        this.grammar.grammarRule.add(gr)
    }

    fun optional(grammarRuleName: String, overrideKind: OverrideKind? = null, isSkip: Boolean = false, isLeaf: Boolean = false, init: SimpleItemsBuilder.() -> Unit) {
        val ib = SimpleItemsBuilder(grammar.namespace)
        ib.init()
        val items = ib.build()
        when (items.size) {
            0 -> error("An optional must have one item defined")
            1 -> Unit
            else -> error("An optional must have only one item defined")
        }
        val rhs = OptionalItemDefault(items[0])
        val gr = createRule(GrammarRuleName(grammarRuleName), overrideKind, isSkip, isLeaf, rhs)
        this.grammar.grammarRule.add(gr)
    }

    fun list(grammarRuleName: String, min: Int, max: Int, overrideKind: OverrideKind? = null, isSkip: Boolean = false, isLeaf: Boolean = false, init: SimpleItemsBuilder.() -> Unit) {
        val ib = SimpleItemsBuilder(grammar.namespace)
        ib.init()
        val items = ib.build()
        when (items.size) {
            0 -> error("A simple list must have one item defined")
            1 -> Unit
            else -> error("A simple list must have only one item defined")
        }
        val rhs = SimpleListDefault(min, max, items[0])
        val gr = createRule(GrammarRuleName(grammarRuleName), overrideKind, isSkip, isLeaf, rhs)
        this.grammar.grammarRule.add(gr)
    }

    fun separatedList(grammarRuleName: String, min: Int, max: Int, overrideKind: OverrideKind? = null, isSkip: Boolean = false, isLeaf: Boolean = false, init: SimpleItemsBuilder.() -> Unit) {
        val ib = SimpleItemsBuilder(grammar.namespace)
        ib.init()
        val items = ib.build()
        when (items.size) {
            0 -> error("A separated list must have two items defined - item & separator")
            1 -> error("A separated list must have two items defined - item & separator")
            2 -> Unit
            else -> error("A simple list must have only two items defined - item & separator")
        }
        val rhs = SeparatedListDefault(min, max, items[0], items[1])
        val gr = createRule(GrammarRuleName(grammarRuleName), overrideKind, isSkip, isLeaf, rhs)
        this.grammar.grammarRule.add(gr)
    }

    fun preference(itemReference: String, init: PreferenceRuleBuilder.() -> Unit) {
        val forItem = NonTerminalDefault(this.grammar.selfReference, GrammarRuleName(itemReference))
        val prb = PreferenceRuleBuilder(this.grammar, forItem)
        prb.init()
        val pr = prb.build()
        grammar.preferenceRule.add(pr)
    }

    fun build(): Grammar = grammar
}

@GrammarBuilderMarker
open class SimpleItemsBuilder(
    val localNamespace: Namespace<Grammar>
) {

    private val items = mutableListOf<SimpleItem>()

    protected open fun addItem(item: RuleItem) {
        items.add(item as SimpleItem)
    }

    fun lit(value: String) {
        addItem(TerminalDefault(value, false))
    }

    fun pat(value: String) {
        addItem(TerminalDefault(value, true))
    }

    fun ebd(embeddedGrammarReference: String, embeddedGoalName: String) {
        val gr = GrammarReferenceDefault(localNamespace, embeddedGrammarReference.asPossiblyQualifiedName)
        addItem(EmbeddedDefault(GrammarRuleName(embeddedGoalName), gr))
    }

    fun ebd(embeddedGrammarReference: GrammarReference, embeddedGoalName: String) {
        addItem(EmbeddedDefault(GrammarRuleName(embeddedGoalName), embeddedGrammarReference))
    }

    /** ref(erence) to grammar rule - non-terminal **/
    fun ref(ruleReference: String) {
        addItem(NonTerminalDefault(null, GrammarRuleName(ruleReference)))
    }

    /** a grouped concatenation **/
    fun grp(init: GroupConcatBuilder.() -> Unit) {
        val gb = GroupConcatBuilder()
        gb.init()
        val groupedContent = gb.build()
        addItem(GroupDefault(groupedContent))
    }

    /** a grouped choice **/
    fun chc(init: GroupChoiceBuilder.() -> Unit) {
        val gb = GroupChoiceBuilder(localNamespace)
        gb.init()
        val groupedContent = gb.build()
        addItem(GroupDefault(groupedContent))
    }

    open fun build(): List<RuleItem> {
        return items
    }
}

@GrammarBuilderMarker
class ChoiceItemBuilder(localNamespace: Namespace<Grammar>) : SimpleItemsBuilder(localNamespace) {
    private val items = mutableListOf<RuleItem>()

    override fun addItem(item: RuleItem) {
        items.add(item)
    }

    fun concat(init: ConcatenationItemBuilder.() -> Unit) {
        val b = ConcatenationItemBuilder(super.localNamespace)
        b.init()
        val items = b.build()
        addItem(ConcatenationDefault(items))
    }

    override fun build(): List<RuleItem> {
        return items
    }
}

@GrammarBuilderMarker
class ConcatenationItemBuilder(localNamespace: Namespace<Grammar>) : SimpleItemsBuilder(localNamespace) {
    private val items = mutableListOf<RuleItem>()

    override fun addItem(item: RuleItem) {
        items.add(item)
    }

    fun opt(init: SimpleItemsBuilder.() -> Unit) {
        val b = SimpleItemsBuilder(super.localNamespace)
        b.init()
        val items = b.build()
        when (items.size) {
            0 -> error("An optional must have one item defined")
            1 -> Unit
            else -> error("An optional must have only one item defined")
        }
        addItem(OptionalItemDefault(items[0]))
    }

    fun lst(min: Int, max: Int, init: SimpleItemsBuilder.() -> Unit) {
        val b = SimpleItemsBuilder(super.localNamespace)
        b.init()
        val items = b.build()
        when (items.size) {
            0 -> error("A simple list must have one item defined")
            1 -> Unit
            else -> error("A simple list must have only one item defined")
        }
        addItem(SimpleListDefault(min, max, items[0]))
    }

    fun spLst(min: Int, max: Int, init: SimpleItemsBuilder.() -> Unit) {
        val b = SimpleItemsBuilder(super.localNamespace)
        b.init()
        val items = b.build()
        when (items.size) {
            0 -> error("A separated list must have two items defined - item & separator")
            1 -> error("A separated list must have two items defined - item & separator")
            2 -> Unit
            else -> error("A simple list must have only two items defined - item & separator")
        }
        addItem(SeparatedListDefault(min, max, items[0], items[1]))
    }

    override fun build(): List<RuleItem> {
        return items
    }
}

@GrammarBuilderMarker
class GroupConcatBuilder() {

    val items = mutableListOf<RuleItem>()


    fun build(): Concatenation {
        return ConcatenationDefault(items)
    }
}

@GrammarBuilderMarker
class GroupChoiceBuilder(val localNamespace: Namespace<Grammar>) {

    val alternatives = mutableListOf<RuleItem>()

    fun alt(init: ConcatenationItemBuilder.() -> Unit) {
        val b = ConcatenationItemBuilder(localNamespace)
        b.init()
        val items = b.build()
        val alt = ConcatenationDefault(items)
        alternatives.add(alt)
    }

    fun build(): Choice {
        return ChoiceLongestDefault(alternatives)
    }
}

@GrammarBuilderMarker
class PreferenceRuleBuilder(
    val grammar: Grammar,
    val forItem: SimpleItem
) {
    val optionList = mutableListOf<PreferenceOption>()

    fun optionLeft(spine: List<String>, choiceIndicator:ChoiceIndicator,choiceNumber: Int, terminals: List<String>) {
        optionList.add(
            PreferenceOptionDefault(
                spine = SpineDefault(spine.map { NonTerminalDefault(null, GrammarRuleName(it)) }),
                choiceIndicator = choiceIndicator,
                choiceNumber =  choiceNumber,
                onTerminals = terminals.map { TerminalDefault(it, false) },
                Associativity.LEFT
            )
        )
    }

    fun optionRight(spine: List<String>, choiceIndicator:ChoiceIndicator,choiceNumber: Int, terminals: List<String>) {
        optionList.add(
            PreferenceOptionDefault(
                spine = SpineDefault(spine.map { NonTerminalDefault(null, GrammarRuleName(it)) }),
                choiceIndicator = choiceIndicator,
                choiceNumber =  choiceNumber,
                onTerminals = terminals.map { TerminalDefault(it, false) },
                Associativity.RIGHT
            )
        )
    }

    fun build(): PreferenceRule {
        return PreferenceRuleDefault(
            grammar = grammar,
            forItem = forItem,
            optionList = optionList
        )
    }
}