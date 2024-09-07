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

package net.akehurst.language.agl.sppt

import net.akehurst.language.agl.agl.parser.SentenceDefault
import net.akehurst.language.agl.regex.regexMatcher
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsLiteral
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsPattern
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.regex.RegexMatcher
import net.akehurst.language.api.sppt.SPPTParser
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.sppt.TreeData
import net.akehurst.language.collections.mutableStackOf

internal object Tokens {
    val WS = regexMatcher("\\s+")
    val EMPTY = regexMatcher("§empty|<EMPTY>")
    val EMPTY_LIST = regexMatcher("<EMPTY_LIST>")
    val QNAME = regexMatcher("[a-zA-Z_§][.a-zA-Z_0-9§]*")
    val ID = regexMatcher("[a-zA-Z_§][a-zA-Z_0-9§]*")
    val EMBED = regexMatcher("::")
    val OPTION = regexMatcher("[|][0-9]+")
    val LITERAL = regexMatcher("'([^'\\\\]|\\\\.)*'")
    val PATTERN = regexMatcher("\"([^\"\\\\]|\\\\.)*\"")
    val COLON = regexMatcher("[:]")
    val CHILDREN_START = regexMatcher("[{]")
    val CHILDREN_END = regexMatcher("[}]")
}

internal class SimpleScanner(
    private val input: CharSequence,
    private val skip: RegexMatcher
) {
    var position: Int = consumeSkipAt(0).second

    val line: Int
        get() {
            val parsed = input.subSequence(0, position)
            return parsed.count { it == '\n' }
        }
    val column: Int
        get() {
            val parsed = input.subSequence(0, position)
            return parsed.length - parsed.lastIndexOf('\n')
        }

    fun hasMore(): Boolean {
        return this.position < this.input.length
    }

    fun hasNext(vararg patterns: RegexMatcher): Boolean {
        var pos = this.position
        for (pat in patterns) {
            val match = pat.match(this.input, pos)
            when (match) {
                null -> return false
                else -> {
                    pos += match.matchedText.length
                    pos = this.consumeSkipAt(pos).second
                }
            }
        }
        return true
    }

    /**
     * consumes 'skip' after each match
     */
    fun next(pattern: RegexMatcher): String {
        val match = pattern.match(this.input, this.position)
        return if (null != match) {
            val matchStr = match.matchedText
            this.position += matchStr.length
            this.position = this.consumeSkipAt(this.position).second
            matchStr
        } else {
            throw error("Error scanning for pattern ${pattern} at Position ${this.position}")
        }
    }

    fun consumeSkipAt(at: Int): Pair<String, Int> {
        var pos = at
        val match = this.skip.match(this.input, pos)
        return if (null != match) {
            val matchStr = match.matchedText
            pos += matchStr.length
            Pair(matchStr, pos)
        } else {
            // no skip to consume
            Pair("", pos)
        }
    }

    override fun toString(): String {
        val x = 5
        val before = input.substring(maxOf(0, position - x), position)
        val after = input.substring(position, minOf(input.length, position + x))
        return "${before}^${after}"
    }
}

internal class SPPTParserDefault(
    val rootRuntimeRuleSet: RuntimeRuleSet,
    val embeddedRuntimeRuleSets: Map<QualifiedName, RuntimeRuleSet> = emptyMap()
) : SPPTParser {

    private var _oldTreeData: TreeData? = null

    // --- SPPTParser ---
    override lateinit var tree: SharedPackedParseTree

    override fun clear() {
    }

    override fun parse(treeAsString: String, addTree: Boolean): SharedPackedParseTree {
        val tp = TreeParser(treeAsString, this.embeddedRuntimeRuleSets)
        val oldTree = if (addTree) {
            this._oldTreeData
        } else {
            null
        }
        val td = tp.parse(rootRuntimeRuleSet, oldTree)
        this._oldTreeData = td
        this.tree = SPPTFromTreeData(td, SentenceDefault(tp.sentence), -1, -1)
        return this.tree
    }

    fun addTree(treeString: String): SharedPackedParseTree {
        this.parse(treeString, true)
        return this.tree
    }
}

internal data class QName(val full: String) {
    val isQualified get() = this.full.contains(".")
    val name get() = this.full.substringAfter(".", this.full)
    val qualifier get() = this.full.substringBefore(".", "")
}

internal data class RuleReference(val qname: QName?, val name: String) {
    val isQualified get() = null != this.qname
}

internal data class NodeStart(
    val ref: RuleReference,
    val option: Int,
    val sentenceStartPosition: Int,
    val sentenceNextInputPosition: Int
)

internal class TreeParser(
    val treeAsString: String,
    val embeddedRuntimeRuleSets: Map<QualifiedName, RuntimeRuleSet>
) {

    val sentence: String get() = this._sentenceBuilder.toString()

    fun parse(rootRuntimeRuleSet: RuntimeRuleSet, oldTree: TreeData?): TreeData {
        this.beginTree(rootRuntimeRuleSet, oldTree)
        childrenStack.push(mutableListOf<CompleteTreeDataNode>())
        while (scanner.hasMore()) {
            when {
                scanner.hasNext(Tokens.EMPTY) -> scanEmpty()
                scanner.hasNext(Tokens.EMPTY_LIST) -> scanEmptyList()
                scanner.hasNext(Tokens.ID, Tokens.COLON, Tokens.LITERAL) -> scanLeafLiteral()
                scanner.hasNext(Tokens.ID, Tokens.COLON, Tokens.ID, Tokens.EMBED, Tokens.ID, Tokens.CHILDREN_START) -> scanEmbedded()
                scanner.hasNext(Tokens.ID, Tokens.OPTION, Tokens.CHILDREN_START) -> scanBranchStartWithOption()
                scanner.hasNext(Tokens.ID, Tokens.CHILDREN_START) -> scanBranchStart()
                scanner.hasNext(Tokens.LITERAL) -> scanLiteral()
                scanner.hasNext(Tokens.PATTERN, Tokens.COLON, Tokens.LITERAL) -> scanPattern()
                scanner.hasNext(Tokens.CHILDREN_END) -> scanBranchEnd()
                else -> scanError()
            }
        }

        val urn = childrenStack.pop().last()
        return this.endTree(urn)

    }

    private val scanner = SimpleScanner(treeAsString, Tokens.WS)
    private var sentenceStartPosition = 0
    private var sentenceNextInputPosition = 0
    private val treeDataStack = mutableStackOf<TreeData>()
    private val nodeNamesStack = mutableStackOf<NodeStart>()
    private val childrenStack = mutableStackOf<MutableList<CompleteTreeDataNode>>()
    private var runtimeRuleSetInUse = mutableStackOf<RuntimeRuleSet>()

    //    private val node_cache: MutableMap<Pair<SPPTNodeIdentity, Int>, SPPTNode> = mutableMapOf()
    private val _sentenceBuilder = StringBuilder()

    private fun scanError() {
        val before = treeAsString.substring(maxOf(0, scanner.position - 5), scanner.position)
        val after = treeAsString.substring(scanner.position, minOf(treeAsString.length, scanner.position + 5))
        val seg = "${before}^${after}"
        error("Tree String invalid at position ${scanner.position}, ...$seg...")
    }

    // EMPTY
    private fun scanEmpty() {
        scanner.next(Tokens.EMPTY)
        sentenceStartPosition = sentenceNextInputPosition
        val emptyNode = this.emptyLeaf(sentenceStartPosition, sentenceNextInputPosition)
        childrenStack.peek().add(emptyNode)
    }

    // EMPTY_LIST
    private fun scanEmptyList() {
        scanner.next(Tokens.EMPTY_LIST)
        sentenceStartPosition = sentenceNextInputPosition
        val emptyNode = this.emptyListLeaf(sentenceStartPosition, sentenceNextInputPosition)
        childrenStack.peek().add(emptyNode)
    }

    // LITERAL
    private fun scanLiteral() {
        val literal = scanner.next(Tokens.LITERAL)
        val textWithQuotes = literal
        val text = textWithQuotes.substring(1, textWithQuotes.length - 1)
        val textUnescaped = RuntimeRuleRhsLiteral.unescape(text)
        sentenceStartPosition = sentenceNextInputPosition
        sentenceNextInputPosition += textUnescaped.length
        val literalUnescaped = RuntimeRuleRhsLiteral.unescape(literal)
        this.leaf(literalUnescaped, textUnescaped, sentenceStartPosition, sentenceNextInputPosition)
    }

    // ID COLON LITERAL
    private fun scanLeafLiteral() {
        val literal = scanner.next(Tokens.ID)
        scanner.next(Tokens.COLON)
        val textWithQuotes = scanner.next(Tokens.LITERAL)
        val text = textWithQuotes.substring(1, textWithQuotes.length - 1)
        val textUnescaped = RuntimeRuleRhsLiteral.unescape(text)
        sentenceStartPosition = sentenceNextInputPosition
        sentenceNextInputPosition += textUnescaped.length
        val literalUnescaped = RuntimeRuleRhsLiteral.unescape(literal)
        this.leaf(literalUnescaped, textUnescaped, sentenceStartPosition, sentenceNextInputPosition)
    }

    // PATTERN COLON LITERAL
    private fun scanPattern() {
        val pattern = scanner.next(Tokens.PATTERN)
        scanner.next(Tokens.COLON)
        val textWithQuotes = scanner.next(Tokens.LITERAL)
        val text = textWithQuotes.substring(1, textWithQuotes.length - 1)
        val textUnescaped = RuntimeRuleRhsLiteral.unescape(text)
        sentenceStartPosition = sentenceNextInputPosition
        sentenceNextInputPosition += textUnescaped.length
        val patternUnescaped = RuntimeRuleRhsPattern.unescape(pattern)
        this.leaf(patternUnescaped, textUnescaped, sentenceStartPosition, sentenceNextInputPosition)
    }

    // ID CHILDREN_START
    private fun scanBranchStart() {
        val id = scanner.next(Tokens.ID)
        scanner.next(Tokens.CHILDREN_START)
        beginBranch(id, 0)
    }

    // ID OPTION CHILDREN_START('{')
    private fun scanBranchStartWithOption() {
        val id = scanner.next(Tokens.ID)
        val option = scanner.next(Tokens.OPTION).substring(1).toInt()
        scanner.next(Tokens.CHILDREN_START)
        beginBranch(id, option)
    }

    // CHILDREN_END('}')
    private fun scanBranchEnd() {
        scanner.next(Tokens.CHILDREN_END)
        this.endBranch()
    }

    // ID COLON ID EMBED ID CHILDREN_START('{')
    private fun scanEmbedded() {
        val leafId = scanner.next(Tokens.ID)
        scanner.next(Tokens.COLON)
        val embGram = scanner.next(Tokens.ID)
        scanner.next(Tokens.EMBED)
        val embGoal = scanner.next(Tokens.ID)
        scanner.next(Tokens.CHILDREN_START)
        beginEmbedded(leafId, QualifiedName(embGram), embGoal)
        RuleReference(QName(embGram), embGoal)
    }

    //
    private fun beginTree(rrs: RuntimeRuleSet, oldTree: TreeData?) {
        val treeData = oldTree ?: treeData(rrs.number)
        this.treeDataStack.push(treeData)
        this.runtimeRuleSetInUse.push(rrs)
    }

    private fun endTree(userGoalNode: CompleteTreeDataNode): TreeData {
        val rrs = this.runtimeRuleSetInUse.pop()
        val treeData = this.treeDataStack.pop()
        val gr = rrs.goalRuleFor[userGoalNode.rule]!!
        val pseudoRoot = CompleteTreeDataNode(gr, userGoalNode.startPosition, userGoalNode.nextInputPosition, userGoalNode.nextInputNoSkip, userGoalNode.option)
        treeData.setChildren(pseudoRoot, listOf(userGoalNode), false)
        treeData.setRoot(pseudoRoot)
        return treeData
    }

    private fun emptyLeaf(startPosition: Int, nextInputPosition: Int): CompleteTreeDataNode {
        val terminalRule = RuntimeRuleSet.EMPTY
        return CompleteTreeDataNode(terminalRule, startPosition, nextInputPosition, nextInputPosition, 0)
    }

    private fun emptyListLeaf(startPosition: Int, nextInputPosition: Int): CompleteTreeDataNode {
        val terminalRule = RuntimeRuleSet.EMPTY_LIST
        return CompleteTreeDataNode(terminalRule, startPosition, nextInputPosition, nextInputPosition, 0)
    }

    private fun leaf(tag: String, text: String, startPosition: Int, nextInputPosition: Int) {
        _sentenceBuilder.append(text)
        val terminalRule = this.runtimeRuleSetInUse.peek().findTerminalRule(tag)
        val leaf = CompleteTreeDataNode(terminalRule, startPosition, nextInputPosition, nextInputPosition, 0)
        childrenStack.peek().add(leaf)
    }

    private fun beginBranch(ruleName: String, option: Int) {
        nodeNamesStack.push(NodeStart(RuleReference(null, ruleName), option, sentenceStartPosition, sentenceNextInputPosition))
        childrenStack.push(mutableListOf<CompleteTreeDataNode>())
    }

    private fun endBranch() {
        val lastNodeStart = nodeNamesStack.pop()
        val ntRef = lastNodeStart.ref
        val rr = this.runtimeRuleSetInUse.peek().findRuntimeRule(ntRef.name)
        val children = this.childrenStack.pop()
        val startPosition = children.firstOrNull()?.startPosition ?: 0
        val nextInputPosition = children.lastOrNull()?.nextInputPosition ?: 0
        val nextInputNoSkip = children.lastOrNull()?.nextInputNoSkip ?: 0

        val tn = CompleteTreeDataNode(rr, startPosition, nextInputPosition, nextInputNoSkip, lastNodeStart.option)
        val isAlternative = this.treeDataStack.peek().childrenFor(tn).isNotEmpty()
        this.treeDataStack.peek().setChildren(tn, children, isAlternative)
        when {
            ntRef.isQualified -> this.endEmbedded(tn)
            else -> this.childrenStack.peek().add(tn)
        }
    }

    private fun beginEmbedded(embLeafName: String, embGramName: QualifiedName, embGoalName: String) {
        // outer leaf
        nodeNamesStack.push(NodeStart(RuleReference(null, embLeafName), 0, sentenceStartPosition, sentenceNextInputPosition))

        // embedded branch start
        nodeNamesStack.push(NodeStart(RuleReference(QName(embGramName.value), embGoalName), 0, sentenceStartPosition, sentenceNextInputPosition))
        childrenStack.push(mutableListOf<CompleteTreeDataNode>())

        // embedded tree
        val embrrs = this.embeddedRuntimeRuleSets[embGramName] ?: error("No embedded RuntimeRuleSet with name '${embGramName}' passed to SPPTParser")
        this.runtimeRuleSetInUse.push(embrrs)
        val embTreeData = treeData(embrrs.number)
        this.treeDataStack.push(embTreeData)
    }

    private fun endEmbedded(embUserRoot: CompleteTreeDataNode): TreeData {
        val lastNodeStart = nodeNamesStack.pop()
        val embLeafName = lastNodeStart.ref.name
        val embTreeData = endTree(embUserRoot)
        val emRoot = embTreeData.root!!

        val terminalRule = this.runtimeRuleSetInUse.peek().findTerminalRule(embLeafName)
        val startPosition = emRoot.startPosition
        val nextInputPosition = emRoot.nextInputPosition
        val nextInputNoSkip = emRoot.nextInputNoSkip
        val leaf = CompleteTreeDataNode(terminalRule, startPosition, nextInputPosition, nextInputNoSkip, 0)
        childrenStack.peek().add(leaf)
        this.treeDataStack.peek().setEmbeddedTreeFor(leaf, embTreeData)
        return embTreeData
    }

    override fun toString(): String = scanner.toString()

}