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

import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.parser.runtime.RuntimeRuleSetBuilder

class SPPTParser(val runtimeRuleSetBuilder: RuntimeRuleSetBuilder) {

    private val WS = Regex("(\\s)+")
    private val EMPTY = Regex("[$]empty")
    private val NAME = Regex("[a-zA-Z_][a-zA-Z_0-9]*")
    private val LITERAL = Regex("'(?:\\\\?.)*?'")
    private val COLON = Regex("[:]")
    private val CHILDREN_START = Regex("[{]")
    private val CHILDREN_END = Regex("[}]")

    private var textLength: Int = 0
    private val offset: Int = 0

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
            val lookingAt = (m?.range?.start == this.position) ?: false
            if (lookingAt) {
                val match = m?.value ?: throw SPPTParserException("Should never happen")
                this.position = m?.range.endInclusive ?: this.input.length
                return match
            } else {
                throw SPPTParserException("Error scanning for pattern ${pattern} at Position ${this.position}")
            }
        }
    }

    private fun parse(treeString: String): SharedPackedParseTree {
        val scanner = SimpleScanner(treeString)
        val nodeNamesStack = Stack<String>()
        val childrenStack = Stack<MutableList<SPPTNode>>()
        // add rootList
        childrenStack.push(mutableListOf<SPPTNode>())
        while (scanner.hasMore()) {
            when {
                scanner.hasNext(WS) -> scanner.next(WS)
                scanner.hasNext(NAME) -> {
                    val name = scanner.next(NAME)
                    nodeNamesStack.push(name)
                }
                scanner.hasNext(CHILDREN_START) -> {
                    scanner.next(CHILDREN_START)
                    childrenStack.push(ArrayList<SPPTNode>())
                }
                scanner.hasNext(LITERAL) -> {
                    val leafStr = scanner.next(LITERAL)
                    val text = leafStr.substring(1, leafStr.length - 1)
                    while (scanner.hasNext(WS)) {
                        scanner.next(WS)
                    }
                    if (scanner.hasNext(COLON)) {
                        scanner.next(COLON)
                        while (scanner.hasNext(WS)) {
                            scanner.next(WS)
                        }
                        val newText = scanner.next(LITERAL)
                        val newText2 = newText.substring(1, newText.length - 1)
                        val leaf = this.leaf(text, newText2)
                        childrenStack.peek().add(leaf)
                    } else {
                        val leaf = this.leaf(text)
                        childrenStack.peek().add(leaf)
                    }
                }
                scanner.hasNext(EMPTY) -> {
                    val empty = scanner.next(EMPTY)
                    val ruleNameThatIsEmpty = nodeNamesStack.peek()
                    val emptyNode = this.emptyLeaf(ruleNameThatIsEmpty)
                    childrenStack.peek().add(emptyNode)
                }
                scanner.hasNext(CHILDREN_END) -> {
                    scanner.next(CHILDREN_END)
                    val lastNodeName = nodeNamesStack.pop()

                    val children = childrenStack.pop()
                    val node = this.branch(lastNodeName, children)
                    childrenStack.peek().add(node)
                }
                else -> throw RuntimeException("Tree String invalid at position " + scanner.position)
            }
        }
        val tree = SharedPackedParseTreeDefault(childrenStack.pop().get(0))
        return tree
    }

    fun emptyLeaf(ruleNameThatIsEmpty: String): SPPTLeaf {
        val start = this.textLength + this.offset
        val ruleThatIsEmpty = this.runtimeRuleSetBuilder.runtimeRuleSet.findRuntimeRule(ruleNameThatIsEmpty)
        val terminalRule = this.runtimeRuleSetBuilder.runtimeRuleSet.findEmptyRule(ruleThatIsEmpty)
        val n = this.runtimeRuleSetBuilder.createEmptyLeaf(start, terminalRule)

        var existing: SPPTLeaf? = this.findLeaf(n.getIdentity())
        if (null == existing) {
            this.cacheNode(n)
            existing = n
        }
        return existing
    }

    fun leaf(text: String): SPPTLeaf {
        return this.leaf(text, text)
    }

    fun leaf(pattern: String, text: String): SPPTLeaf {

    }

    fun branch(ruleName: String, children: List<SPPTNode>): SPPTBranch {
        val rr = this.runtimeRuleSetBuilder.runtimeRuleSet.findRuntimeRule(ruleName)
        val n = this.runtimeRuleSetBuilder.createBranch(rr, children.toTypedArray())

        var existing: SPPTBranch? = this.findBranch(n.getIdentity())
        if (null == existing) {
            this.cacheNode(n)
            existing = n
        } else {
            val newChildren = n.getChildren() // no need to clone as list is fixed!
            existing.childrenAlternatives.add(newChildren)
        }

        return existing
    }
}