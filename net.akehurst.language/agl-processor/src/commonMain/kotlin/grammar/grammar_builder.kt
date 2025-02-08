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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.SemanticAnalysisOptionsDefault
import net.akehurst.language.api.processor.GrammarRegistry
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.base.api.*
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.grammar.api.*
import net.akehurst.language.grammar.asm.*
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.typemodel.api.TypeNamespace
import net.akehurst.language.typemodel.asm.StdLibDefault
import net.akehurst.language.typemodel.asm.TypeModelSimple

@DslMarker
annotation class GrammarBuilderMarker

fun grammarModel(name: String, namespaces: List<GrammarNamespace> = emptyList(), grammarRegistry: GrammarRegistry? = null, init: GrammarModelBuilder.() -> Unit): GrammarModel {
    val b = GrammarModelBuilder(SimpleName(name), namespaces)
    b.init()
    val gm = b.build()
    grammarRegistry?.let { gr ->
        gm.allDefinitions.forEach { gr.registerGrammar(it) }
        val sa = AglGrammarSemanticAnalyser()
        val opts = SemanticAnalysisOptionsDefault(
            context = ContextFromGrammarRegistry(gr)
        )
        sa.analyse(gm,emptyMap(), opts)
    }
    return gm
}

fun grammar(namespace: String, name: String, init: GrammarBuilder.() -> Unit): Grammar {
    val ns = GrammarNamespaceDefault(QualifiedName(namespace))
    val b = GrammarBuilder(ns, SimpleName(name))
    b.init()
    val gr = b.build()
    ns.addDefinition(gr)
    return b.build()
}

@GrammarBuilderMarker
class GrammarModelBuilder(
    name: SimpleName,
    namespaces: List<GrammarNamespace>
) {
    private val _options = mutableMapOf<String, String>()
    private val _model = GrammarModelDefault(name, OptionHolderDefault(null, _options), namespaces)

    fun option(key: String, value: String) {
        _options[key] = value
    }

    fun namespace(qualifiedName: String, init: GrammarNamespaceBuilder.() -> Unit) {
        val b = GrammarNamespaceBuilder(_model, qualifiedName)
        b.init()
        val ns = b.build()
        _model.addNamespace(ns)
    }

    fun build() = _model
}

@GrammarBuilderMarker
class GrammarNamespaceBuilder(
    val grammarModel: GrammarModel,
    qualifiedName: String
) {
    private val _options = mutableMapOf<String, String>()
    private val _import = mutableListOf<Import>()
    private val _namespace = GrammarNamespaceDefault(qualifiedName.asQualifiedName, OptionHolderDefault(null, _options), _import)

    fun option(key: String, value: String) {
        _options[key] = value
    }

    fun import(qualifiedName: String) {
        _import.add(Import(qualifiedName))
    }

    fun grammar(name: String, init: GrammarBuilder.() -> Unit) {
        val b = GrammarBuilder(_namespace, SimpleName(name))
        b.init()
        val g = b.build()
        _namespace.addDefinition(g)
    }

    fun build() = _namespace
}

@GrammarBuilderMarker
class GrammarBuilder(
    namespace: GrammarNamespace,
    name: SimpleName,
) {

    private val _grammar = GrammarDefault(namespace, name)
    private val _terminals = mutableMapOf<String, Terminal>()

    fun extends(nameOrQName: String) {
        _grammar.extends.add(GrammarReferenceDefault(_grammar.namespace, nameOrQName.asPossiblyQualifiedName))
    }

    fun extendsGrammar(extended: GrammarReference) {
        _grammar.extends.add(extended)
    }

    private fun terminal(value: String, isPattern: Boolean): Terminal {
        val t = _terminals[value]
        return if (null == t) {
            val tt = TerminalDefault(value, isPattern)
            _terminals[value] = tt
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
            val gr = NormalRuleDefault(_grammar, grammarRuleName, isSkip, isLeaf)
            gr.rhs = rhs
            gr
        }

        else -> {
            val gr = OverrideRuleDefault(_grammar, grammarRuleName, isSkip, isLeaf, overrideKind)
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
        _grammar.grammarRule.add(gr)
    }

    fun choice(grammarRuleName: String, overrideKind: OverrideKind? = null, isSkip: Boolean = false, isLeaf: Boolean = false, init: ChoiceItemBuilder.() -> Unit) {
        val ib = ChoiceItemBuilder(_grammar.namespace)
        ib.init()
        val items = ib.build()
        val rhs = ChoiceLongestDefault(items)
        val gr = createRule(GrammarRuleName(grammarRuleName), overrideKind, isSkip, isLeaf, rhs)
        _grammar.grammarRule.add(gr)
    }

    fun concatenation(grammarRuleName: String, overrideKind: OverrideKind? = null, isSkip: Boolean = false, isLeaf: Boolean = false, init: ConcatenationItemBuilder.() -> Unit) {
        val ib = ConcatenationItemBuilder(_grammar.namespace)
        ib.init()
        val items = ib.build()
        val rhs = ConcatenationDefault(items)
        val gr = createRule(GrammarRuleName(grammarRuleName), overrideKind, isSkip, isLeaf, rhs)
        _grammar.grammarRule.add(gr)
    }

    fun optional(grammarRuleName: String, overrideKind: OverrideKind? = null, isSkip: Boolean = false, isLeaf: Boolean = false, init: SimpleItemsBuilder.() -> Unit) {
        val ib = SimpleItemsBuilder(_grammar.namespace)
        ib.init()
        val items = ib.build()
        when (items.size) {
            0 -> error("An optional must have one item defined")
            1 -> Unit
            else -> error("An optional must have only one item defined")
        }
        val rhs = OptionalItemDefault(items[0])
        val gr = createRule(GrammarRuleName(grammarRuleName), overrideKind, isSkip, isLeaf, rhs)
        _grammar.grammarRule.add(gr)
    }

    fun list(grammarRuleName: String, min: Int, max: Int, overrideKind: OverrideKind? = null, isSkip: Boolean = false, isLeaf: Boolean = false, init: SimpleItemsBuilder.() -> Unit) {
        val ib = SimpleItemsBuilder(_grammar.namespace)
        ib.init()
        val items = ib.build()
        when (items.size) {
            0 -> error("A simple list must have one item defined")
            1 -> Unit
            else -> error("A simple list must have only one item defined")
        }
        val rhs = SimpleListDefault(min, max, items[0])
        val gr = createRule(GrammarRuleName(grammarRuleName), overrideKind, isSkip, isLeaf, rhs)
        _grammar.grammarRule.add(gr)
    }

    fun separatedList(grammarRuleName: String, min: Int, max: Int, overrideKind: OverrideKind? = null, isSkip: Boolean = false, isLeaf: Boolean = false, init: SimpleItemsBuilder.() -> Unit) {
        val ib = SimpleItemsBuilder(_grammar.namespace)
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
        _grammar.grammarRule.add(gr)
    }

    fun preference(itemReference: String, init: PreferenceRuleBuilder.() -> Unit) {
        val forItem = NonTerminalDefault(_grammar.selfReference, GrammarRuleName(itemReference))
        val prb = PreferenceRuleBuilder(_grammar, forItem)
        prb.init()
        val pr = prb.build()
        _grammar.preferenceRule.add(pr)
    }

    fun build(): Grammar = _grammar
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
    fun grp(init: ConcatenationItemBuilder.() -> Unit) {
        val gb = ConcatenationItemBuilder(localNamespace)
        gb.init()
        val items = gb.build()
        addItem(GroupDefault(ConcatenationDefault(items)))
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

    fun optionLeft(spine: List<String>, choiceIndicator: ChoiceIndicator, choiceNumber: Int, terminals: List<String>) {
        optionList.add(
            PreferenceOptionDefault(
                spine = SpineDefault(spine.map { NonTerminalDefault(null, GrammarRuleName(it)) }),
                choiceIndicator = choiceIndicator,
                choiceNumber = choiceNumber,
                onTerminals = terminals.map { TerminalDefault(it, false) },
                Associativity.LEFT
            )
        )
    }

    fun optionRight(spine: List<String>, choiceIndicator: ChoiceIndicator, choiceNumber: Int, terminals: List<String>) {
        optionList.add(
            PreferenceOptionDefault(
                spine = SpineDefault(spine.map { NonTerminalDefault(null, GrammarRuleName(it)) }),
                choiceIndicator = choiceIndicator,
                choiceNumber = choiceNumber,
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