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

import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.regex.regexMatcher
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.regex.RegexMatcher
import net.akehurst.language.api.sppt.*
import net.akehurst.language.collections.MutableStack
import net.akehurst.language.collections.mutableStackOf

internal class SPPTParserDefault(
    val rootRuntimeRuleSet: RuntimeRuleSet,
    val embeddedRuntimeRuleSets: Map<String, RuntimeRuleSet> = emptyMap()
) : SPPTParser {

    private val WS = regexMatcher("\\s+")
    private val EMPTY = regexMatcher("§empty")
    private val QNAME = regexMatcher("[a-zA-Z_§][.a-zA-Z_0-9§]*")
    private val OPTION = regexMatcher("[|][0-9]+")
    private val LITERAL = regexMatcher("'([^'\\\\]|\\\\.)*'")
    private val PATTERN = regexMatcher("\"([^\"\\\\]|\\\\.)*\"")
    private val COLON = regexMatcher("[:]")
    private val CHILDREN_START = regexMatcher("[{]")
    private val CHILDREN_END = regexMatcher("[}]")

    private var runtimeRuleSetInUse = mutableStackOf<RuntimeRuleSet>(rootRuntimeRuleSet)
    private val node_cache: MutableMap<Pair<SPPTNodeIdentity, Int>, SPPTNode> = mutableMapOf()

    var root: SPPTNode? = null

    override val tree: SharedPackedParseTree
        get() {
            val root = this.root ?: throw SPPTException("At least one tree must be added", null)
            return SharedPackedParseTreeDefault(root, -1, -1)
        }

    private data class QName(val full: String) {
        val isQualified get() = this.full.contains(".")
        val name get() = this.full.substringAfter(".", this.full)
        val qualifier get() = this.full.substringBefore(".", "")
    }

    private data class NodeStart(
        val qname: QName,
        val option: Int,
        val sentenceStartPosition: Int,
        val sentenceNextInputPosition: Int
    )

    class SPPTParserException(message: String) : Exception(message) {}

    private class SimpleScanner(private val input: CharSequence) {
        var position: Int = 0

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

        fun hasNext(pattern: RegexMatcher): Boolean {
            val lookingAt = pattern.match(this.input, this.position) != null
            return lookingAt
        }

        fun next(pattern: RegexMatcher): String {
            val match = pattern.match(this.input, this.position)
            if (null != match) {
                val matchStr = match.matchedText
                this.position += matchStr.length
                return matchStr
            } else {
                throw SPPTParserException("Error scanning for pattern ${pattern} at Position ${this.position}")
            }
        }
    }

    private fun cacheNode(node: SPPTNode) {
        this.node_cache[Pair(node.identity, node.matchedTextLength)] = node
    }

    private fun findNode(id: SPPTNodeIdentity, length: Int): SPPTNode? {
        return this.node_cache[Pair(id, length)]
    }

    private fun findLeaf(id: SPPTNodeIdentity, length: Int): SPPTLeaf? {
        val n = this.findNode(id, length)
        return n?.asLeaf
    }

    private fun findBranch(id: SPPTNodeIdentity, length: Int): SPPTBranch? {
        val n = this.findNode(id, length)
        return n?.asBranch
    }

    override fun clear() {
        this.node_cache.clear()
    }

    override fun parse(treeAsString: String, addTree: Boolean): SharedPackedParseTree {
        if (addTree) {
            //do not reset cache
        } else {
            this.node_cache.clear()
        }
        val input = InputFromString(runtimeRuleSetInUse.peek().terminalRules.size, "") //TODO: not sure we should reuse this here? maybe ok
        val scanner = SimpleScanner(treeAsString)
        val nodeNamesStack = MutableStack<NodeStart>()
        val childrenStack = MutableStack<MutableList<SPPTNode>>()
        // add rootList
        childrenStack.push(mutableListOf<SPPTNode>())
        //var sentenceLocation = InputLocation(0, 1, 1, 0)
        var sentenceStartPosition = 0
        var sentenceNextInputPosition = 0
        while (scanner.hasMore()) {
            when {
                scanner.hasNext(WS) -> scanner.next(WS)
                scanner.hasNext(EMPTY) -> { //must do this before NAME, as EMPTY is also a valid NAME
                    val empty = scanner.next(EMPTY)
                    val ruleStartThatIsEmpty = nodeNamesStack.peek()
                    //val location = InputLocation(sentenceLocation.position, sentenceLocation.column, sentenceLocation.line, 0)
                    sentenceStartPosition = sentenceNextInputPosition
                    //sentenceNextInputPosition = sentenceNextInputPosition + 0
                    val emptyNode = this.emptyLeaf(ruleStartThatIsEmpty.qname.name, input, sentenceStartPosition, sentenceNextInputPosition)
                    childrenStack.peek().add(emptyNode)
                }
                scanner.hasNext(QNAME) -> {
                    val scanned = scanner.next(QNAME)
                    val qname = QName(scanned)
                    if (qname.isQualified) {
                        val gname = qname.qualifier
                        val embrrs = this.embeddedRuntimeRuleSets[gname] ?: error("No embedded RuntimeRuleSet with name '${gname}' passed to SPPTParser")
                        this.runtimeRuleSetInUse.push(embrrs)
                    } else {
                        // do nothing
                    }
                    val option = if (scanner.hasNext(OPTION)) {
                        val optionStr = scanner.next(OPTION)
                        val num = optionStr.substring(1).toInt()
                        num
                    } else {
                        0
                    }
                    nodeNamesStack.push(NodeStart(qname, option, sentenceStartPosition, sentenceNextInputPosition))
                }
                scanner.hasNext(CHILDREN_START) -> {
                    scanner.next(CHILDREN_START)
                    childrenStack.push(ArrayList<SPPTNode>())
                }
                scanner.hasNext(LITERAL) -> {
                    val leafName = scanner.next(LITERAL).replace("\\'", "'")
                    val text = leafName.substring(1, leafName.length - 1)
                    while (scanner.hasNext(WS)) {
                        scanner.next(WS)
                    }
                    if (scanner.hasNext(COLON)) {
                        scanner.next(COLON)
                        while (scanner.hasNext(WS)) {
                            scanner.next(WS)
                        }
                        val newText = scanner.next(LITERAL)
                        val newText2 = newText.replace("\\'", "'")
                        val newText3 = newText2.substring(1, newText2.length - 1)
                        //val location = InputLocation(sentenceLocation.position, sentenceLocation.column, sentenceLocation.line, newText3.length)
                        sentenceStartPosition = sentenceNextInputPosition
                        sentenceNextInputPosition += newText3.length
                        val leaf = this.leaf(leafName, newText3, input, sentenceStartPosition, sentenceNextInputPosition)
                        //sentenceLocation = input.nextLocation(location, leaf.matchedText.length)
                        childrenStack.peek().add(leaf)
                    } else {
                        //val location = InputLocation(sentenceLocation.position, sentenceLocation.column, sentenceLocation.line, text.length)
                        sentenceStartPosition = sentenceNextInputPosition
                        sentenceNextInputPosition += text.length
                        val leaf = this.leaf(leafName, text, input, sentenceStartPosition, sentenceNextInputPosition)
                        //sentenceLocation = input.nextLocation(location, text.length)
                        childrenStack.peek().add(leaf)
                    }
                }
                scanner.hasNext(PATTERN) -> {
                    val leafName = scanner.next(PATTERN).replace("\\'", "'")
                    val text = leafName.substring(1, leafName.length - 1)
                    while (scanner.hasNext(WS)) {
                        scanner.next(WS)
                    }
                    if (scanner.hasNext(COLON)) {
                        scanner.next(COLON)
                        while (scanner.hasNext(WS)) {
                            scanner.next(WS)
                        }
                        val newText = scanner.next(LITERAL)
                        val newText2 = newText.replace("\\'", "'")
                        val newText3 = newText2.substring(1, newText2.length - 1)
                        //val location = InputLocation(sentenceLocation.position, sentenceLocation.column, sentenceLocation.line, newText3.length)
                        sentenceStartPosition = sentenceNextInputPosition
                        sentenceNextInputPosition += newText3.length
                        val leaf = this.leaf(leafName, newText3, input, sentenceStartPosition, sentenceNextInputPosition)
                        //sentenceLocation = input.nextLocation(location, leaf.matchedText.length)
                        childrenStack.peek().add(leaf)
                    } else {
                        // val location = InputLocation(sentenceLocation.position, sentenceLocation.column, sentenceLocation.line, text.length)
                        sentenceStartPosition = sentenceNextInputPosition
                        sentenceNextInputPosition += text.length
                        val leaf = this.leaf(leafName, text, input, sentenceStartPosition, sentenceNextInputPosition)
                        // sentenceLocation = input.nextLocation(location, text.length)
                        childrenStack.peek().add(leaf)
                    }
                }
                scanner.hasNext(COLON) -> {
                    scanner.next(COLON)
                    while (scanner.hasNext(WS)) {
                        scanner.next(WS)
                    }
                    val qname = nodeNamesStack.pop().qname
                    val newText = scanner.next(LITERAL)
                    val newText2 = newText.substring(1, newText.length - 1) // remove ' from begin and end
                    val newText3 = newText2.replace("\\'", "'") // substitute excape chars
                    //val location = InputLocation(sentenceLocation.position, sentenceLocation.column, sentenceLocation.line, newText3.length)
                    sentenceStartPosition = sentenceNextInputPosition
                    sentenceNextInputPosition += newText3.length
                    val leaf = this.leaf(qname.name, newText3, input, sentenceStartPosition, sentenceNextInputPosition)
                    //sentenceLocation = input.nextLocation(location, leaf.matchedText.length)
                    childrenStack.peek().add(leaf)
                }
                scanner.hasNext(CHILDREN_END) -> {
                    scanner.next(CHILDREN_END)
                    check(nodeNamesStack.isEmpty.not()) { "Error parsing tree at position line:${scanner.line},column:${scanner.column}" }
                    val lastNodeStart = nodeNamesStack.pop()

                    val children = childrenStack.pop()
                    val qname = lastNodeStart.qname
                    val node = if (qname.isQualified) {
                        val embNode = this.embedded(input, qname.name, lastNodeStart.option, children)
                        this.runtimeRuleSetInUse.pop()
                        embNode
                    } else {
                        this.branch(input, qname.name, lastNodeStart.option, children)
                    }
                    childrenStack.peek().add(node)
                }
                else -> {
                    val before = treeAsString.substring(maxOf(0, scanner.position - 5), scanner.position)
                    val after = treeAsString.substring(scanner.position, minOf(treeAsString.length, scanner.position + 5))
                    val seg = "${before}^${after}"
                    throw RuntimeException("Tree String invalid at position ${scanner.position}, ...$seg...")
                }
            }
        }
        this.root = childrenStack.pop()[0]
        return this.tree
    }

    internal fun emptyLeaf(ruleNameThatIsEmpty: String, input: InputFromString, startPosition: Int, nextInputPosition: Int): SPPTLeaf {
        val ruleThatIsEmpty = this.runtimeRuleSetInUse.peek().findRuntimeRule(ruleNameThatIsEmpty)
        val terminalRule = ruleThatIsEmpty.emptyRuleItem
        //val n = SPPTLeafDefault(terminalRule, location, true, "", 0)
        val n = SPPTLeafFromInput(input, terminalRule, startPosition, nextInputPosition, 0)
        var existing: SPPTLeaf? = this.findLeaf(n.identity, n.matchedTextLength)
        if (null == existing) {
            this.cacheNode(n)
            existing = n
        }
        return existing
    }

    internal fun leaf(pattern: String, text: String, input: InputFromString, startPosition: Int, nextInputPosition: Int): SPPTLeaf {
        input.append(text)
        val terminalRule = this.runtimeRuleSetInUse.peek().findTerminalRule(pattern)
        //val n = SPPTLeafDefault(terminalRule, location, false, text, 0)
        val eolPositions = Regex("\n", setOf(RegexOption.MULTILINE)).findAll(text).toList().map { it.range.first }
        val n = SPPTLeafFromInput(input, terminalRule, startPosition, nextInputPosition, 0)
        var existing: SPPTLeaf? = this.findLeaf(n.identity, n.matchedTextLength)
        if (null == existing) {
            this.cacheNode(n)
            existing = n
        }

        return existing
    }

    internal fun embedded(input: InputFromString, ruleName: String, option: Int, children: List<SPPTNode>): SPPTBranch {
        val rr = this.runtimeRuleSetInUse.peek().findRuntimeRule(ruleName)
        val startPosition = children.firstOrNull()?.startPosition ?: 0
        val nextInputPosition = children.lastOrNull()?.nextInputPosition ?: 0
        val n = SPPTBranchFromInput(input, rr, option, startPosition, nextInputPosition, 0)
        n.childrenAlternatives.add(children)

        var existing: SPPTBranch? = this.findBranch(n.identity, n.matchedTextLength)
        if (null == existing) {
            this.cacheNode(n)
            existing = n
        } else {
            (existing as SPPTBranchFromInput).childrenAlternatives.add(n.children)
        }

        return existing
    }

    internal fun branch(input: InputFromString, ruleName: String, option: Int, children: List<SPPTNode>): SPPTBranch {
        val rr = this.runtimeRuleSetInUse.peek().findRuntimeRule(ruleName)
        val startPosition = children.firstOrNull()?.startPosition ?: 0
        val nextInputPosition = children.lastOrNull()?.nextInputPosition ?: 0
        val n = SPPTBranchFromInput(input, rr, option, startPosition, nextInputPosition, 0)
        n.childrenAlternatives.add(children)

        var existing: SPPTBranch? = this.findBranch(n.identity, n.matchedTextLength)
        if (null == existing) {
            this.cacheNode(n)
            existing = n
        } else {
            (existing as SPPTBranchFromInput).childrenAlternatives.add(n.children)
        }

        return existing
    }

    fun addTree(treeString: String): SharedPackedParseTree {
        this.parse(treeString, true)
        return this.tree
    }
}