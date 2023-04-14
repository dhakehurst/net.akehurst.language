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

package net.akehurst.language.agl.grammar.grammar

import net.akehurst.language.agl.grammar.grammar.asm.*
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SyntaxAnalysisResultDefault
import net.akehurst.language.agl.syntaxAnalyser.BranchHandler
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserAbstract
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SharedPackedParseTree


internal class AglGrammarSyntaxAnalyser(
    //val languageRegistry: LanguageRegistryDefault
) : SyntaxAnalyserAbstract<List<Grammar>>() {

    override val locationMap = mutableMapOf<Any, InputLocation>()

    private val _issues = IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS)

    init {
        this.register("grammarDefinition", this::grammarDefinition as BranchHandler<List<Grammar>>)
        this.register("namespace", this::namespace as BranchHandler<Namespace>)
        this.register("definitions", this::definitions as BranchHandler<List<Grammar>>)
        this.register("grammar", this::grammar as BranchHandler<Grammar>)
        this.register("extendsOpt", this::extendsOpt as BranchHandler<List<GrammarReference>>)
        this.register("extends", this::extends as BranchHandler<List<GrammarReference>>)
        this.register("extendsList", this::extendsList as BranchHandler<List<GrammarReference>>)
        this.register("rules", this::rules as BranchHandler<List<GrammarRule>>)
        this.register("rule", this::rule as BranchHandler<GrammarItem>)
        this.register("grammarRule", this::grammarRule as BranchHandler<GrammarRule>)
        this.register("preferenceRule", this::preferenceRule as BranchHandler<PreferenceRule>)
        this.register("ruleTypeLabels", this::ruleTypeLabels as BranchHandler<List<String>>)
        // this.register("ruleType", this::ruleType as BranchHandler<GrammarRule>)
        this.register("rhs", this::rhs as BranchHandler<RuleItem>)
        this.register("empty", this::empty as BranchHandler<RuleItem>)
        this.register("choice", this::choice as BranchHandler<RuleItem>)
        this.register("simpleChoice", this::simpleChoice as BranchHandler<RuleItem>)
        this.register("priorityChoice", this::priorityChoice as BranchHandler<RuleItem>)
        this.register("ambiguousChoice", this::ambiguousChoice as BranchHandler<RuleItem>)
        this.register("concatenation", this::concatenation as BranchHandler<Concatenation>)
        this.register("concatenationItem", this::concatenationItem as BranchHandler<ConcatenationItem>)
        this.register("simpleItemOrGroup", this::simpleItemOrGroup as BranchHandler<SimpleItem>)
        this.register("simpleItem", this::simpleItem as BranchHandler<SimpleItem>)
        this.register("listOfItems", this::listOfItems as BranchHandler<ListOfItems>)
        this.register("multiplicity", this::multiplicity as BranchHandler<Pair<Int, Int>>)
        this.register("range", this::range as BranchHandler<Pair<Int, Int>>)
        this.register("rangeUnBraced", this::rangeUnBraced as BranchHandler<Pair<Int, Int>>)
        this.register("rangeBraced", this::rangeBraced as BranchHandler<Pair<Int, Int>>)
        this.register("rangeMaxOpt", this::rangeMaxOpt as BranchHandler<Int>)
        this.register("rangeMax", this::rangeMax as BranchHandler<Int>)
        this.register("rangeMaxBounded", this::rangeMaxBounded as BranchHandler<Int>)
        this.register("rangeMaxUnbounded", this::rangeMaxUnbounded as BranchHandler<Int>)
        this.register("simpleList", this::simpleList as BranchHandler<SimpleList>)
        this.register("group", this::group as BranchHandler<Group>)
        this.register("groupedContent", this::groupedContent as BranchHandler<RuleItem>)
        this.register("separatedList", this::separatedList as BranchHandler<SeparatedList>)
        this.register("nonTerminal", this::nonTerminal as BranchHandler<NonTerminal>)
        this.register("embedded", this::embedded as BranchHandler<Embedded>)
        this.register("terminal", this::terminal as BranchHandler<Terminal>)
        this.register("qualifiedName", this::qualifiedName as BranchHandler<String>)
        this.register("preferenceOption", this::preferenceOption as BranchHandler<PreferenceOption>)
        this.register("choiceNumber", this::choiceNumber as BranchHandler<Int?>)
    }

    override fun clear() {
        locationMap.clear()
        _issues.clear()
    }

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        //TODO
        return emptyList()
    }

    override fun transform(sppt: SharedPackedParseTree, mapToGrammar: (Int, Int) -> RuleItem): SyntaxAnalysisResult<List<Grammar>> {
        val grammars = this.transformBranch<List<Grammar>>(sppt.root.asBranch, "")
        return SyntaxAnalysisResultDefault(grammars, _issues, locationMap)
    }

    // grammarDefinition : namespace definitions ;
    private fun grammarDefinition(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<Grammar> {
        val namespace = this.transformBranch<Namespace>(children[0], null)
        val definitions = this.transformBranch<List<Grammar>>(children[1], namespace)
        return definitions
    }

    // definitions = grammar+ ;
    private fun definitions(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<Grammar> {
        val definitions = children[0].branchNonSkipChildren.map {
            this.transformBranch<Grammar>(it, arg)
        }
        return definitions
    }

    // namespace : 'namespace' qualifiedName ;
    private fun namespace(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Namespace {
        val qualifiedName = this.transformBranch<String>(children[0], null)
        return NamespaceDefault(qualifiedName).also { this.locationMap[it] = target.location }
    }

    // grammar : 'grammar' IDENTIFIER extendsOpt '{' rules '}' ;
    private fun grammar(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Grammar {
        val namespace = arg as Namespace
        val name = target.nonSkipChildren[1].nonSkipMatchedText
        val extends = this.transformBranch<List<GrammarReference>>(children[0], namespace)
        val grmr = GrammarDefault(namespace, name).also { this.locationMap[it] = target.location }
        grmr.extends.addAll(extends)
        this.transformBranch<List<GrammarRule>>(children[1], grmr) //creating a GrammarRule adds it to the grammar
        return grmr
        /*
        val def = this.languageRegistry.findWithNamespaceOrNull<Any, Any>(namespace.qualifiedName, name)
        return if (null == def) {
            this.languageRegistry.registerFromDefinition(
                LanguageDefinitionFromAsm(
                    identity = grmr.qualifiedName,
                    grammarArg = grmr,
                    targetGrammar = grmr.name,
                    defaultGoalRuleArg = null,
                    buildForDefaultGoal = false,
                    styleArg = null,
                    formatArg = null,
                    syntaxAnalyserResolverArg = { g -> SyntaxAnalyserSimple(TypeModelFromGrammar(g)) },
                    semanticAnalyserResolverArg = { _ -> SemanticAnalyserSimple() },
                    aglOptionsArg = null
                )
            )
            grmr
        } else {
            if(def.identity != grmr.qualifiedName) {
                _issues.add(LanguageIssue(LanguageIssueKind.WARNING,LanguageProcessorPhase.SYNTAX_ANALYSIS,target.location, "Registered identity '${def.identity}' does not match qualified name of grammar",null))
            } else {
                //
            }
            def.grammar = grmr
            grmr
        }
*/
    }

    // extendsOpt = extends?
    private fun extendsOpt(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<GrammarReference> {
        return this.transformBranchOpt<List<GrammarReference>>(children[0], arg)?: emptyList()
    }

    // extends = 'extends' extendsList ;
    private fun extends(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<GrammarReference> {
        return this.transformBranch<List<GrammarReference>>(children[0], arg)?: emptyList()
    }

    // extendsList = [qualifiedName / ',']+ ;
    private fun extendsList(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<GrammarReference> {
        val localNamespace = arg as Namespace
        val extendNameList = children[0].branchNonSkipChildren.map { it.nonSkipMatchedText }
        val extendedGrammars = extendNameList.map {
            val qn = localNamespace.qualifiedName + "." + it
            GrammarReferenceDefault(localNamespace, it).also { this.locationMap[it] = target.location }
        }
        return extendedGrammars
    }

    // rules : rule+ ;
    private fun rules(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<GrammarItem> {
        return children[0].branchNonSkipChildren.mapIndexed { index, it ->
            this.transformBranch<GrammarItem>(it, arg)
        }
    }

    // rule = grammarRule | preferenceRule
    private fun rule(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): GrammarItem {
        return this.transformBranch<GrammarItem>(children[0], arg)
    }

    // grammarRule : ruleTypeLabels IDENTIFIER ':' rhs ';' ;
    private fun grammarRule(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): GrammarRule {
        val grammar = arg as GrammarDefault
        val type = this.transformBranch<List<String>>(children[0], arg)
        val isOverride = type.contains("override")
        val isSkip = type.contains("skip")
        val isLeaf = type.contains("leaf")
        val name = target.nonSkipChildren[1].nonSkipMatchedText
        val result = GrammarRuleDefault(grammar, name, isOverride, isSkip, isLeaf).also { this.locationMap[it] = target.location }
        val rhs = this.transformBranch<RuleItem>(children[1], arg)
        result.rhs = rhs
        return result
    }

    // ruleTypeLabels : isSkip isLeaf ;
    // isOverride = 'override' ? ;
    // isSkip = 'leaf' ? ;
    // isLeaf = 'skip' ? ;
    private fun ruleTypeLabels(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): List<String> {
        return children.mapNotNull {
            when {
                it.nonSkipChildren[0].isEmptyLeaf -> null
                else -> it.nonSkipChildren[0].nonSkipMatchedText
            }
        }
    }

    // rhs = empty | concatenation | choice ;
    private fun rhs(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        return this.transformBranch<RuleItem>(children[0], arg)
    }

    // empty = ;
    private fun empty(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        return EmptyRuleDefault().also { this.locationMap[it] = target.location }
    }

    // choice = ambiguousChoice | priorityChoice | simpleChoice ;
    private fun choice(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        return this.transformBranch<RuleItem>(children[0], arg)
    }

    // simpleChoice : [concatenation, '|']* ;
    private fun simpleChoice(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        // children will have one element, an sList
        val alternative = children[0].branchNonSkipChildren.mapIndexed { index, it ->
            this.transformBranch<Concatenation>(it, arg)
        }
        return ChoiceLongestDefault(alternative).also { this.locationMap[it] = target.location }
    }

    // priorityChoice : [concatenation, '<']* ;
    private fun priorityChoice(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        val alternative = children[0].branchNonSkipChildren.mapIndexed { index, it ->
            this.transformBranch<Concatenation>(it, arg)
        }
        return ChoicePriorityDefault(alternative).also { this.locationMap[it] = target.location }
    }

    // ambiguousChoice : [concatenation, '||']* ;
    private fun ambiguousChoice(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        val alternative = children[0].branchNonSkipChildren.mapIndexed { index, it ->
            this.transformBranch<Concatenation>(it, arg)
        }
        return ChoiceAmbiguousDefault(alternative).also { this.locationMap[it] = target.location }
    }

    // concatenation : concatenationItem+ ;
    private fun concatenation(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Concatenation {
        val items = children[0].branchNonSkipChildren.mapIndexed { index, it ->
            this.transformBranch<ConcatenationItem>(it, arg)
        }
        return ConcatenationDefault(items).also { this.locationMap[it] = target.location }
    }

    // concatenationItem = simpleItem | listOfItems ;
    private fun concatenationItem(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): ConcatenationItem = this.transformBranch<ConcatenationItem>(children[0], arg)

    // simpleItemOrGroup : simpleItem | group ;
    private fun simpleItemOrGroup(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): SimpleItem = this.transformBranch<SimpleItem>(children[0], arg)

    // simpleItem : terminal | nonTerminal | embedded ;
    private fun simpleItem(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): SimpleItem = this.transformBranch<SimpleItem>(children[0], arg)

    // listOfItems = simpleList | separatedList ;
    private fun listOfItems(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): ListOfItems = this.transformBranch<ListOfItems>(children[0], arg)

    // multiplicity = '*' | '+' | '?' | range ;
    private fun multiplicity(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Pair<Int, Int> {
        val symbol = target.nonSkipMatchedText
        return when (symbol) {
            "*" -> Pair(0, -1)
            "+" -> Pair(1, -1)
            "?" -> Pair(0, 1)
            else -> this.transformBranch<Pair<Int, Int>>(children[0], arg)
        }
    }

    //range = rangeBraced | rangeUnBraced ;
    private fun range(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Pair<Int, Int> = this.transformBranch<Pair<Int, Int>>(children[0], arg)

    //rangeUnBraced = POSITIVE_INTEGER rangeMaxOpt ;
    private fun rangeUnBraced(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Pair<Int, Int> {
        val min = target.nonSkipChildren[0].nonSkipMatchedText.toInt()
        val max = if (children[0].isEmptyMatch) {
            min
        } else {
            this.transformBranch<Int>(children[0], arg)
        }
        return Pair(min, max)
    }

    //rangeBraced = '{' POSITIVE_INTEGER rangeMaxOpt '}' ;
    private fun rangeBraced(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Pair<Int, Int> {
        val min = target.nonSkipChildren[1].nonSkipMatchedText.toInt()
        val max = if (children[0].isEmptyMatch) {
            min
        } else {
            this.transformBranch<Int>(children[0], arg)
        }
        return Pair(min, max)
    }

    // rangeMaxOpt = rangeMax? ;
    private fun rangeMaxOpt(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Int {
        return this.transformBranch<Int>(children[0].branchNonSkipChildren[0], arg)
    }

    //rangeMax = rangeMaxUnbounded | rangeMaxBounded ;
    private fun rangeMax(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Int {
        return this.transformBranch<Int>(children[0], arg)
    }

    //rangeMaxUnbounded = '+' ;
    private fun rangeMaxUnbounded(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Int = -1

    //rangeMaxBounded = '..' POSITIVE_INTEGER ;
    private fun rangeMaxBounded(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Int = target.nonSkipChildren[1].nonSkipMatchedText.toInt()

    // simpleList = simpleItem multiplicity ;
    private fun simpleList(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): SimpleList {
        val (min, max) = this.transformBranch<Pair<Int, Int>>(children[1], arg)
        val item = this.transformBranch<SimpleItem>(children[0], arg)
        return SimpleListDefault(min, max, item).also { this.locationMap[it] = target.location }
    }

    // separatedList : '[' simpleItem '/' terminal ']' multiplicity ;
    private fun separatedList(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): SeparatedList {
        val (min, max) = this.transformBranch<Pair<Int, Int>>(children[2], arg)
        val separator = this.transformBranch<SimpleItem>(children[1], arg)
        val item = this.transformBranch<SimpleItem>(children[0], arg)
        return SeparatedListDefault(min, max, item, separator).also { this.locationMap[it] = target.location }
    }

    // group : '(' choice ')' ;
    private fun group(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Group {
        val groupContent = this.transformBranch<RuleItem>(children[0], arg)
        return when (groupContent) {
            is Choice -> GroupDefault(groupContent).also { this.locationMap[it] = target.location }
            is Concatenation -> GroupDefault(ChoiceLongestDefault(listOf(groupContent))).also { this.locationMap[it] = target.location }
            else -> error("Internal Error: type of group content not handled '${groupContent::class.simpleName}'")
        }
    }

    private fun groupedContent(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): RuleItem {
        val content = this.transformBranch<RuleItem>(children[0], arg)
        return content
    }

    // nonTerminal : IDENTIFIER ;
    private fun nonTerminal(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): NonTerminal {
        val thisGrammar = arg as Grammar
        val nonTerminalRef = target.nonSkipChildren[0].nonSkipMatchedText
        val nt = NonTerminalDefault(nonTerminalRef).also { this.locationMap[it] = target.location }
        return nt
    }

    // embedded = qualifiedName '::' nonTerminal ;
    private fun embedded(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Embedded {
        val thisGrammar = arg as Grammar
        val embeddedGrammarStr = target.nonSkipChildren[0].nonSkipMatchedText
        val embeddedStartRuleRef = target.nonSkipChildren[2].nonSkipMatchedText

        //val def = this.languageRegistry.findWithNamespaceOrNull<Any, Any>(thisGrammar.namespace.qualifiedName, embeddedGrammarRef)
        //    ?: error("Trying to embed but failed to find grammar '$embeddedGrammarRef' as a qualified name or in namespace '${thisGrammar.namespace.qualifiedName}'")
        //val embeddedGrammar = def.processor!!.grammar
        val embeddedGrammarRef = GrammarReferenceDefault(thisGrammar.namespace, embeddedGrammarStr).also { this.locationMap[it] = target.location }
        return EmbeddedDefault(embeddedStartRuleRef, embeddedGrammarRef).also { this.locationMap[it] = target.location }
    }

    // terminal : LITERAL | PATTERN ;
    private fun terminal(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): Terminal {
        // Must match what is done in AglStyleSyntaxAnalyser.selectorSingle
        val isPattern = target.nonSkipChildren[0].name == "PATTERN"
        val mt = target.nonSkipMatchedText
        val escaped = mt.substring(1, mt.length - 1)
        //TODO: check these unescapings, e.g. '\\n'
        val value = if (isPattern) {
            escaped.replace("\\\"", "\"")
        } else {
            escaped.replace("\\'", "'").replace("\\\\", "\\")
        }
        return TerminalDefault(value, isPattern).also { this.locationMap[it] = target.location }
    }

    // qualifiedName : (IDENTIFIER / '.')+ ;
    private fun qualifiedName(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): String {
        return target.nonSkipMatchedText //children[0].branchNonSkipChildren.map { it.nonSkipMatchedText }.joinToString(".")
    }

    // preferenceRule = 'preference' simpleItem '{' preferenceOptionList '}' ;
    private fun preferenceRule(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): PreferenceRule {
        val grammar = arg as GrammarDefault
        val forItem = this.transformBranch<SimpleItem>(children[0], arg)
        val optionList = children[1].branchNonSkipChildren[0].branchNonSkipChildren.map {
            this.transformBranch<PreferenceOption>(it, arg)
        }
        return PreferenceRuleDefault(grammar, forItem, optionList)
    }

    // preferenceOption = nonTerminal choiceNumber 'on' terminalList associativity ;
    private fun preferenceOption(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?): PreferenceOption {
        val item = this.transformBranch<NonTerminal>(children[0], arg)
        val choiceNumber = when {
            children[1].isEmptyMatch -> 0
            else -> this.transformBranch<Int>(children[1], arg)
        }
        val terminalList = children[2].branchNonSkipChildren[0].branchNonSkipChildren.map {
            this.transformBranch<SimpleItem>(it, arg)
        }
        val assStr = children[3].nonSkipMatchedText
        val associativity = when(assStr) {
            "left" -> PreferenceOption.Associativity.LEFT
            "right" -> PreferenceOption.Associativity.RIGHT
            else -> error("Internal Error: associativity value '$assStr' not supported")
        }
        return PreferenceOptionDefault(item, choiceNumber, terminalList, associativity)
    }

    private fun choiceNumber(target: SPPTBranch, children: List<SPPTBranch>, arg: Any?):Int {
        val str = target.nonSkipMatchedText
        return str.toInt()
    }

}