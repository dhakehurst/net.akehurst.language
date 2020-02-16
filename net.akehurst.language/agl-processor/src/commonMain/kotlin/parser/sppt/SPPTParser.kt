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

package net.akehurst.language.parser.sppt

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.sppt.*
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.scannerless.InputFromCharSequence

class SPPTParser(val runtimeRuleSet: RuntimeRuleSet) {
    constructor(rrsb: RuntimeRuleSetBuilder) : this(rrsb.ruleSet())

    private val WS = Regex("(\\s)+")
    private val EMPTY = Regex("§empty")
    private val NAME = Regex("[a-zA-Z_§][a-zA-Z_0-9§]*")
    private val LITERAL = Regex("'(?:\\\\?.)*?'")
    private val PATTERN = Regex("\"(?:\\\\?.)*?\"")
    private val COLON = Regex("[:]")
    private val CHILDREN_START = Regex("[{]")
    private val CHILDREN_END = Regex("[}]")

    private val node_cache: MutableMap<Pair<SPPTNodeIdentity, Int>, SPPTNode> = mutableMapOf()

    var root: SPPTNode? = null

    val tree: SharedPackedParseTree
        get() {
            val root = this.root ?: throw SPPTException("At least one tree must be added", null)
            return SharedPackedParseTreeDefault(root, -1, -1)
        }

    private data class NodeStart(
            val name: String,
            val location: InputLocation
    ) {

    }

    private class Stack<T>() {
        private val list = mutableListOf<T>()
        fun push(item: T) {
            list.add(item)
        }

        fun peek(): T {
            return list.last();
        }

        fun pop(): T {
            return list.removeAt(list.size - 1)
        }
    }

    class SPPTParserException(message: String) : Exception(message) {}

    private class SimpleScanner(private val input: CharSequence) {
        var position: Int = 0

        fun hasMore(): Boolean {
            return this.position < this.input.length
        }

        fun hasNext(pattern: Regex): Boolean {
            val lookingAt = pattern.find(this.input, this.position)?.range?.start == this.position
            return lookingAt
        }

        fun next(pattern: Regex): String {
            val m = pattern.find(this.input, this.position)
            val lookingAt = (m?.range?.start == this.position)
            if (lookingAt) {
                val match = m?.value ?: throw SPPTParserException("Should never happen")
                this.position += m.value.length
                return match
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

    private fun parse(treeString: String) {
        val input = InputFromCharSequence(treeString) //TODO: not sure we should reuse this here? maybe ok
        val scanner = SimpleScanner(treeString)
        val nodeNamesStack = Stack<NodeStart>()
        val childrenStack = Stack<MutableList<SPPTNode>>()
        // add rootList
        childrenStack.push(mutableListOf<SPPTNode>())
        var sentenceLocation = InputLocation(0, 1, 1, 0)
        while (scanner.hasMore()) {
            when {
                scanner.hasNext(WS) -> scanner.next(WS)
                scanner.hasNext(EMPTY) -> { //must do this before NAME, as EMPTY is also a valid NAME
                    val empty = scanner.next(EMPTY)
                    val ruleStartThatIsEmpty = nodeNamesStack.peek()
                    val location = InputLocation(sentenceLocation.position, sentenceLocation.column, sentenceLocation.line, 0)
                    val emptyNode = this.emptyLeaf(ruleStartThatIsEmpty.name, location)
                    childrenStack.peek().add(emptyNode)
                }
                scanner.hasNext(NAME) -> {
                    val name = scanner.next(NAME)
                    nodeNamesStack.push(NodeStart(name, sentenceLocation))
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
                        val location = InputLocation(sentenceLocation.position, sentenceLocation.column, sentenceLocation.line, newText3.length)
                        val leaf = this.leaf(leafName, newText3, location)
                        sentenceLocation = input.nextLocation(location, leaf.matchedText.length)
                        childrenStack.peek().add(leaf)
                    } else {
                        val location = InputLocation(sentenceLocation.position, sentenceLocation.column, sentenceLocation.line, text.length)
                        val leaf = this.leaf(leafName, text, location)
                        sentenceLocation = input.nextLocation(location, text.length)
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
                        val location = InputLocation(sentenceLocation.position, sentenceLocation.column, sentenceLocation.line, newText3.length)
                        val leaf = this.leaf(leafName, newText3, location)
                        sentenceLocation = input.nextLocation(location, leaf.matchedText.length)
                        childrenStack.peek().add(leaf)
                    } else {
                        val location = InputLocation(sentenceLocation.position, sentenceLocation.column, sentenceLocation.line, text.length)
                        val leaf = this.leaf(leafName, text, location)
                        sentenceLocation = input.nextLocation(location, text.length)
                        childrenStack.peek().add(leaf)
                    }
                }
                scanner.hasNext(COLON) -> {
                    scanner.next(COLON)
                    while (scanner.hasNext(WS)) {
                        scanner.next(WS)
                    }
                    val name = nodeNamesStack.pop().name
                    val newText = scanner.next(LITERAL)
                    val newText2 = newText.replace("\\'", "'")
                    val newText3 = newText2.substring(1, newText2.length - 1)
                    val location = InputLocation(sentenceLocation.position, sentenceLocation.column, sentenceLocation.line, newText3.length)
                    val leaf = this.leaf(name, newText3, location)
                    sentenceLocation = input.nextLocation(location, leaf.matchedText.length)
                    childrenStack.peek().add(leaf)
                }
                scanner.hasNext(CHILDREN_END) -> {
                    scanner.next(CHILDREN_END)
                    val lastNodeStart = nodeNamesStack.pop()

                    val children = childrenStack.pop()
                    val node = this.branch(lastNodeStart.name, children)
                    childrenStack.peek().add(node)
                }
                else -> throw RuntimeException("Tree String invalid at position " + scanner.position)
            }
        }
        this.root = childrenStack.pop()[0]
    }

    fun emptyLeaf(ruleNameThatIsEmpty: String, location: InputLocation): SPPTLeaf {
        val ruleThatIsEmpty = this.runtimeRuleSet.findRuntimeRule(ruleNameThatIsEmpty)
        val terminalRule = ruleThatIsEmpty.emptyRuleItem
        val n = SPPTLeafDefault(terminalRule, location, true, "", 0)

        var existing: SPPTLeaf? = this.findLeaf(n.identity, n.matchedTextLength)
        if (null == existing) {
            this.cacheNode(n)
            existing = n
        }
        return existing
    }

    fun leaf(pattern: String, text: String, location: InputLocation): SPPTLeaf {
        val terminalRule = this.runtimeRuleSet.findTerminalRule(pattern)
        val n = SPPTLeafDefault(terminalRule, location, false, text, 0)
        n.eolPositions = Regex("\n", setOf(RegexOption.MULTILINE)).findAll(text).toList().map { it.range.first }
        var existing: SPPTLeaf? = this.findLeaf(n.identity, n.matchedTextLength)
        if (null == existing) {
            this.cacheNode(n)
            existing = n
        }

        return existing
    }

    fun branch(ruleName: String, children: List<SPPTNode>): SPPTBranch {
        val rr = this.runtimeRuleSet.findRuntimeRule(ruleName)
        val firstLocation = children.first().location
        val lastLocation = children.last().location
        val length = (lastLocation.position - firstLocation.position) + lastLocation.length
        val location = InputLocation(firstLocation.position, firstLocation.column, firstLocation.line, length)
        val nextInputPosition = location.position + location.length
        val n = SPPTBranchDefault(rr, location, nextInputPosition, 0)
        n.childrenAlternatives.add(children)

        var existing: SPPTBranch? = this.findBranch(n.identity, n.matchedTextLength)
        if (null == existing) {
            this.cacheNode(n)
            existing = n
        } else {
            (existing as SPPTBranchDefault).childrenAlternatives.add(n.children)
        }

        return existing
    }

    fun addTree(treeString: String): SharedPackedParseTree {
        this.parse(treeString)
        return this.tree
    }
}