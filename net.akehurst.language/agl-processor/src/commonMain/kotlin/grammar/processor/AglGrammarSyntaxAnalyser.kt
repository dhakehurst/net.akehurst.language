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

package net.akehurst.language.grammar.processor

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.*
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.base.processor.BaseSyntaxAnalyser
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.grammar.api.*
import net.akehurst.language.grammar.asm.*
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo
import net.akehurst.language.sppt.treedata.locationForNode
import net.akehurst.language.transform.api.TransformNamespace
import net.akehurst.language.transform.api.TransformRuleSet
import net.akehurst.language.transform.asm.TransformNamespaceDefault

internal class AglGrammarSyntaxAnalyser(
    //val languageRegistry: LanguageRegistryDefault
) : SyntaxAnalyserByMethodRegistrationAbstract<GrammarModel>() {

    private val _issues = IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)

    private val _localStore = mutableMapOf<String, Any>()

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        QualifiedName("Base") to BaseSyntaxAnalyser()
    )

    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<GrammarModel>> = emptyMap()

    override fun <T : Any> clear(done: Set<SyntaxAnalyser<T>>) {
        super.clear(done)
        this._localStore.clear()
    }

    override fun registerHandlers() {
        this.register(this::unit)
        this.register(this::namespace)
        this.register(this::grammar)
        this.register(this::extends)
        this.register(this::rules)
        this.register(this::rule)
        this.register(this::grammarRule)
        this.register(this::overrideRule)
        this.register(this::overrideOperator)
        this.register(this::preferenceRule)
        this.register(this::ruleTypeLabels)
        this.register(this::rhs)
        this.register(this::empty)
        this.register(this::choice)
        this.register(this::simpleChoice)
        this.register(this::priorityChoice)
        this.register(this::ambiguousChoice)
        this.register(this::concatenation)
        this.register(this::concatenationItem)
        this.register(this::simpleItemOrGroup)
        this.register(this::simpleItem)
        this.register(this::listOfItems)
        this.register(this::multiplicity)
        this.register(this::range)
        this.register(this::rangeUnBraced)
        this.register(this::rangeBraced)
        this.register(this::rangeMaxOpt)
        this.register(this::rangeMax)
        this.register(this::rangeMaxBounded)
        this.register(this::rangeMaxUnbounded)
        this.register(this::simpleList)
        this.register(this::group)
        this.register(this::groupedContent)
        this.register(this::separatedList)
        this.register(this::nonTerminal)
        this.register(this::embedded)
        this.register(this::terminal)
        this.register(this::preferenceOption)
        this.register(this::spine)
        this.register(this::choiceNumber)
        this.register(this::terminalList)
        this.register(this::associativity)
    }

    // Base.unit = option* namespace* ;
    private fun unit(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): GrammarModel {
        val options = children[0] as List<Pair<String, String>>
        val namespaces = children[1] as List<GrammarNamespace>

        val optHolder = OptionHolderDefault(null, options.associate { it })
        val unit = GrammarModelDefault(name = SimpleName("ParsedGrammarUnit"), namespace = namespaces, options = optHolder)
        return unit
    }

    // override namespace : 'namespace' possiblyQualifiedName ;
//    private fun namespace(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): GrammarNamespace {
//        val pqn = children[1] as PossiblyQualifiedName
//        val nsName = pqn.asQualifiedName(null)
//        val ns = GrammarNamespaceDefault(nsName)//.also { this.locationMap[it] = target.node.locationIn(sentence) }
//        _localStore["namespace"] = ns
//        return ns
//    }

    // override namespace = 'namespace' possiblyQualifiedName option* import* transform* ;
    private fun namespace(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): GrammarNamespace {
        val pqn = children[1] as PossiblyQualifiedName
        val nsName = pqn.asQualifiedName(null)
        val options = children[2] as List<Pair<String, String>>
        val imports = children[3] as List<Import>
        val grammarBuilders = children[4] as List<(GrammarNamespace) -> Grammar>

        val optHolder = OptionHolderDefault(null, options.associate { it })
        val namespace = GrammarNamespaceDefault(nsName, optHolder, imports)
        grammarBuilders.map { it.invoke(namespace) }
        return namespace
    }

    // grammar : 'grammar' IDENTIFIER extends? '{' option* rule+ '}' ;
    private fun grammar(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (GrammarNamespace) -> Grammar {
        //val namespace = _localStore["namespace"] as GrammarNamespace
        val name = SimpleName(children[1] as String)
        val extends = (children[2] as ((GrammarNamespace) -> List<GrammarReference>)?) ?: { emptyList() }
        val options = (children[4] as List<Pair<String, String>>)
        val rules = children[5] as List<Pair<Boolean, (Grammar) -> GrammarItem>>

        val optHolder = OptionHolderDefault(null, options.associate { it })
        val grmRules = rules.filter { it.first }.map { it.second }
        val precRules = rules.filter { it.first.not() }.map { it.second }

        // _localStore["grammar"] = grmr

        return { namespace ->
            val grmr = GrammarDefault(namespace, name, optHolder)
            grmr.extends.addAll(extends.invoke(namespace))
            val grmrRules = grmRules.map { f -> f(grmr) as GrammarRule }
            grmr.grammarRule.addAll(grmrRules)
            val prfRules = precRules.map { f -> f(grmr) as PreferenceRule }
            grmr.preferenceRule.addAll(prfRules)
            grmr.also { setLocationFor(it, target, sentence) }
        }
    }

    // extends = 'extends' [possiblyQualifiedName / ',']+ ;
    private fun extends(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (GrammarNamespace) -> List<GrammarReference> {
        //val localNamespace = _localStore["namespace"] as Namespace<Grammar>
        val extendNameList = children[1] as List<PossiblyQualifiedName>
        val sl = extendNameList.toSeparatedList<Any, PossiblyQualifiedName, String>()
        return { localNamespace ->
            sl.items.map {
                GrammarReferenceDefault(localNamespace, it).also { setLocationFor(it, target, sentence) }
            }
        }
    }

    // rules : rule+ ;
    private fun rules(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<Pair<Boolean, (Grammar) -> GrammarItem>> =
        children as List<Pair<Boolean, (Grammar) -> GrammarItem>>

    // rule = overrideRule | grammarRule | preferenceRule
    private fun rule(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<Boolean, (Grammar) -> GrammarItem> =
        children[0] as Pair<Boolean, (Grammar) -> GrammarItem>

    // grammarRule : ruleTypeLabels IDENTIFIER '=' rhs ';' ;
    private fun grammarRule(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<Boolean, (Grammar) -> GrammarRule> {
        val type = children[0] as List<String>
        val isSkip = type.contains("skip")
        val isLeaf = type.contains("leaf")
        val name = GrammarRuleName(children[1] as String)
        val rhs = children[3] as (Grammar) -> RuleItem

        return Pair(true, { grammar ->
            val result = NormalRuleDefault(grammar, name, isSkip, isLeaf)
            result.rhs = rhs.invoke(grammar)
            result.also { setLocationFor(it, target, sentence) }
        })
    }

    // overrideRule : 'override' ruleTypeLabels IDENTIFIER overrideOperator rhs ';' ;
    private fun overrideRule(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<Boolean, (Grammar) -> OverrideRule> {
        val type = children[1] as List<String>
        val isSkip = type.contains("skip")
        val isLeaf = type.contains("leaf")
        val identifier = GrammarRuleName(children[2] as String)
        val overrideOperator = children[3] as String
        val overrideKind = when (overrideOperator) {
            "==" -> OverrideKind.SUBSTITUTION
            "=" -> OverrideKind.REPLACE
            "+=|" -> OverrideKind.APPEND_ALTERNATIVE
            else -> error("overrideOperator $overrideOperator not handled")
        }
        val rhs = children[4] as (Grammar) -> RuleItem

        return Pair(true, { grammar ->
            val result = OverrideRuleDefault(grammar, identifier, isSkip, isLeaf, overrideKind)
            result.overriddenRhs = rhs.invoke(grammar)
            result.also { setLocationFor(it, target, sentence) }
        })
    }

    // overrideOperator = '=' | '+|' ;
    private fun overrideOperator(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String =
        children[0] as String

    // ruleTypeLabels : isOverride isSkip isLeaf ;
    // isSkip = 'leaf' ? ;
    // isLeaf = 'skip' ? ;
    private fun ruleTypeLabels(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<String> {
        return (children as List<String?>).filterNotNull()
    }

    // rhs = empty | concatenation | choice ;
    private fun rhs(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> RuleItem {
        val rhsFunc = children[0] as (Grammar) -> RuleItem
        return { grm ->
            val rhs = rhsFunc.invoke(grm)
            when (rhs) {
                is Concatenation -> reduceConcatenation(rhs)
                else -> rhs
            }.also { setLocationFor(it, target, sentence) }
        }
    }

    // empty = ;
    private fun empty(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> RuleItem {
        return { EmptyRuleDefault().also { setLocationFor(it, target, sentence) } }
    }

    // choice = ambiguousChoice | priorityChoice | simpleChoice ;
    private fun choice(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> RuleItem {
        return children[0] as (Grammar) -> RuleItem
    }

    // simpleChoice : [concatenation, '|']* ;
    private fun simpleChoice(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> RuleItem {
        val simpleChoice = (children as List<Any>).toSeparatedList<Any, ((Grammar) -> Concatenation), String>()
        return { grm ->
            val alternative = simpleChoice.items.map { reduceConcatenation(it.invoke(grm)) }
            ChoiceLongestDefault(alternative).also { setLocationFor(it, target, sentence) }
        }
    }

    // priorityChoice : [concatenation, '<']* ;
    private fun priorityChoice(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> RuleItem {
        val priorityChoice = (children as List<Any>).toSeparatedList<Any, ((Grammar) -> Concatenation), String>()
        return { grm ->
            val alternative = priorityChoice.items.map { reduceConcatenation(it.invoke(grm)) }
            ChoicePriorityDefault(alternative).also { setLocationFor(it, target, sentence) }
        }
    }

    // ambiguousChoice : [concatenation, '||']* ;
    private fun ambiguousChoice(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> RuleItem {
        val ambiguousChoice = (children as List<Any>).toSeparatedList<Any, ((Grammar) -> Concatenation), String>()
        return { grm ->
            val alternative = ambiguousChoice.items.map { reduceConcatenation(it.invoke(grm)) }
            ChoiceAmbiguousDefault(alternative).also { setLocationFor(it, target, sentence) }
        }
    }

    // concatenation : concatenationItem+ ;
    private fun concatenation(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> Concatenation {
        val items = children as List<(Grammar) -> RuleItem>
        return { grm ->
            ConcatenationDefault(items.map { it.invoke(grm) }).also { setLocationFor(it, target, sentence) }
        }
    }

    // concatenationItem = simpleItemOrGroup | listOfItems ; // a group can be mapped to a Choice so return RuleItem
    private fun concatenationItem(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> RuleItem =
        children[0] as (Grammar) -> RuleItem

    // simpleItemOrGroup : simpleItem | group ; // a group can be mapped to a Choice so return RuleItem
    private fun simpleItemOrGroup(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> RuleItem =
        children[0] as (Grammar) -> RuleItem

    // simpleItem : terminal | nonTerminal | embedded ;
    private fun simpleItem(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> SimpleItem =
        children[0] as (Grammar) -> SimpleItem

    // listOfItems = simpleList | separatedList ;
    // could also return optional
    private fun listOfItems(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> ConcatenationItem =
        children[0] as (Grammar) -> ConcatenationItem

    // multiplicity = '*' | '+' | '?' | range ;
    private fun multiplicity(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<Int, Int> {
        val m = children[0]
        return when (m) {
            is String -> when (m) {
                "*" -> Pair(0, -1)
                "+" -> Pair(1, -1)
                "?" -> Pair(0, 1)
                else -> error("should not happen")
            }

            is Pair<*, *> -> m as Pair<Int, Int>
            else -> error("should not happen")
        }
    }

    //range = rangeBraced | rangeUnBraced ;
    private fun range(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<Int, Int> =
        children[0] as Pair<Int, Int>

    //rangeUnBraced = POSITIVE_INTEGER rangeMaxOpt ;
    private fun rangeUnBraced(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<Int, Int> {
        val min = (children[0] as String).toInt()
        val max = if (children[1] == null) {
            min
        } else {
            children[1] as Int
        }
        return Pair(min, max)
    }

    //rangeBraced = '{' POSITIVE_INTEGER rangeMaxOpt '}' ;
    private fun rangeBraced(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<Int, Int> {
        val min = (children[1] as String).toInt()
        val max = if (null == children[2]) {
            min
        } else {
            children[2] as Int
        }
        return Pair(min, max)
    }

    // rangeMaxOpt = rangeMax? ;
    private fun rangeMaxOpt(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Int? {
        return children[0] as Int?
    }

    //rangeMax = rangeMaxUnbounded | rangeMaxBounded ;
    private fun rangeMax(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Int =
        children[0] as Int

    //rangeMaxUnbounded = '+' ;
    private fun rangeMaxUnbounded(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Int = -1

    //rangeMaxBounded = '..' POSITIVE_INTEGER ;
    private fun rangeMaxBounded(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Int =
        (children[1] as String).toInt()

    // simpleList = simpleItem multiplicity ;
    private fun simpleList(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> ConcatenationItem {
        val (min, max) = children[1] as Pair<Int, Int>
        val item = children[0] as (Grammar) -> RuleItem
        return { grm ->
            when {
                min == 0 && max == 1 -> OptionalItemDefault(item.invoke(grm))
                else -> SimpleListDefault(min, max, item.invoke(grm))
            }.also { setLocationFor(it, target, sentence) }
        }
    }

    // separatedList : '[' simpleItem '/' terminal ']' multiplicity ;
    private fun separatedList(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> SeparatedList {
        val (min, max) = children[5] as Pair<Int, Int>
        val separator = children[3] as (Grammar) -> RuleItem
        val item = children[1] as (Grammar) -> RuleItem
        return { grm ->
            SeparatedListDefault(min, max, item.invoke(grm), separator.invoke(grm)).also { setLocationFor(it, target, sentence) }
        }
    }

    // group = '(' groupedContent ')' ;
    private fun group(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> RuleItem {
        val groupContentFunc = children[1] as (Grammar) -> RuleItem
        return { grm ->
            val groupContent = groupContentFunc.invoke(grm)
            when (groupContent) {
                is Choice -> groupContent
                is Concatenation -> {
                    val reduced = reduceConcatenation(groupContent)
                    GroupDefault(reduced)
                }

                else -> error("Internal Error: subtype of RuleItem not handled - ${groupContent::class.simpleName}")
            }.also { setLocationFor(it, target, sentence) }
        }
    }

    // groupedContent = concatenation | choice ;
    private fun groupedContent(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> RuleItem =
        children[0] as (Grammar) -> RuleItem

    // nonTerminal : possiblyQualifiedName ;
    private fun nonTerminal(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> NonTerminal {
        val pqn = children[0] as PossiblyQualifiedName
        return { grm ->
            when (pqn) {
                is QualifiedName -> {
                    val localNamespace = grm.namespace
                    val nonTerminalRef = GrammarRuleName(pqn.last.value)
                    val grammarRef = pqn.front.asPossiblyQualified
                    val gr = GrammarReferenceDefault(localNamespace, grammarRef).also { setLocationFor(it, target, sentence) }
                    val nt = NonTerminalDefault(gr, nonTerminalRef)//.also { this.locationMap[it] = target.node.locationIn(sentence) }
                    nt
                }

                is SimpleName -> {
                    NonTerminalDefault(null, GrammarRuleName(pqn.value))//.also { this.locationMap[it] = target.node.locationIn(sentence) }
                }
            }.also { setLocationFor(it, target, sentence) }
        }
    }

    // embedded = possiblyQualifiedName '::' nonTerminal ;
    private fun embedded(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> Embedded {
        val embeddedGrammarStr = children[0] as PossiblyQualifiedName
        val embeddedStartRuleRef = children[2] as (Grammar) -> NonTerminal
        return { grm ->
            val namespace = grm.namespace
            val embeddedGrammarRef = GrammarReferenceDefault(namespace, embeddedGrammarStr)
            EmbeddedDefault(embeddedStartRuleRef.invoke(grm).ruleReference, embeddedGrammarRef)
                .also { setLocationFor(it, target, sentence) }
        }
    }

    // terminal : LITERAL | PATTERN ;
    private fun terminal(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> Terminal {
        // Must match what is done in AglStyleSyntaxAnalyser.selectorSingle
        val isPattern = when (target.alt.option.asIndex) {
            0 -> false
            else -> true
        }
        val mt = children[0] as String
        val value = mt.substring(1, mt.length - 1)
        val unescaped = when (isPattern) {
            false -> value.replace("\\'", "'").replace("\\\\","\\")
            true -> value.replace("\\\"", "\"").replace("\\\\","\\")
        }
        return { TerminalDefault(unescaped, isPattern).also { setLocationFor(it, target, sentence) } }
    }

    // preferenceRule = 'preference' simpleItem '{' preferenceOptionList '}' ;
    private fun preferenceRule(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<Boolean, (Grammar) -> PreferenceRule> {
        val forItem = children[1] as (Grammar) -> SimpleItem
        val optionListFunc = children[3] as List<(Grammar) -> PreferenceOption>
        return Pair(false, { grammar ->
            val optionList = optionListFunc.map { it.invoke(grammar) }
            PreferenceRuleDefault(grammar, forItem.invoke(grammar), optionList)
                .also { setLocationFor(it, target, sentence) }
        })
    }

    // preferenceOption = spine choiceNumber? 'on' terminalList associativity ;
    private fun preferenceOption(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> PreferenceOption {
        val spine = children[0] as (Grammar) -> Spine
        val ci = children[1] as Pair<ChoiceIndicator, Int>?
        val choiceIndicator = when {
            null == ci -> ChoiceIndicator.NONE
            else -> ci.first
        }
        val choiceNumber = ci?.second ?: -1

        val terminalList = children[3] as List<(Grammar) -> SimpleItem>
        val assStr = children[4] as String
        val associativity = when (assStr) {
            "left" -> Associativity.LEFT
            "right" -> Associativity.RIGHT
            else -> error("Internal Error: associativity value '$assStr' not supported")
        }
        return { grm ->
            PreferenceOptionDefault(spine.invoke(grm), choiceIndicator, choiceNumber, terminalList.map { it.invoke(grm) }, associativity)
                .also { setLocationFor(it, target, sentence) }
        }
    }

    // spine = [nonTerminal / '<-']+ ;
    private fun spine(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (Grammar) -> Spine {
        val slist = (children as List<Any>).toSeparatedList<Any, ((Grammar) -> NonTerminal), String>()
        val parts = slist.items
        return { grm -> SpineDefault(parts.map { it.invoke(grm) }) }
    }

    // choiceNumber = POSITIVE_INTEGER | CHOICE_INDICATOR ;
    private fun choiceNumber(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<ChoiceIndicator, Int> {
        return when (target.alt.option.asIndex) {
            0 -> Pair(ChoiceIndicator.NUMBER, (children[0] as String).toInt())
            1 -> Pair(ChoiceIndicator.valueOf(children[0] as String), -1)
            else -> error("Internal Error: choiceNumber not supported")
        }
    }

    // terminalList = [simpleItem / ',']+ ;
    private fun terminalList(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): List<(Grammar) -> SimpleItem> {
        val slist = (children as List<Any>).toSeparatedList<Any, (Grammar) -> SimpleItem, String>()
        return slist.items
    }

    // associativity = 'left' | 'right' ;
    private fun associativity(target: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): String =
        children[0] as String

    private fun reduceConcatenation(concat: Concatenation): RuleItem {
        return when (concat.items.size) {
            1 -> when (concat.items[0]) {
                is TangibleItem -> concat.items[0]
                is OptionalItem -> concat.items[0]
                is ListOfItems -> concat.items[0]
                is Choice -> concat.items[0]
                is Group -> {
                    val content = (concat.items[0] as Group).groupedContent
                    when (content) {
                        is Choice -> content
                        else -> concat
                    }
                }

                else -> error("Internal Error: subtype of ConcatenationItem not handled - ${concat.items[0]::class.simpleName}")
            }

            else -> concat
        }
    }

}