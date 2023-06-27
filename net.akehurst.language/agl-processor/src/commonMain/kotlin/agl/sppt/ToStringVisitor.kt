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

import net.akehurst.language.api.sppt.SPPTBranch
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.collections.MutableStack

internal class ToStringVisitor(val lineSeparator: String, val indentIncrement: String) {

    private data class Item(
        val node: SPPTNode,
        val indent: String,
        val onlyChild: Boolean,
        val prefix: String?
    )

    fun visitTree(target: SharedPackedParseTree, indentText: String): Set<String> {
        return this.visitNode(target.root, indentText)
    }

    fun visitNode(start: SPPTNode, indentText: String): Set<String> {
        val result = MutableStack<Set<String>>()
        val stack = MutableStack<Item>() // node, onlychild, prefix
        stack.push(Item(start, "", true, null))
        while (stack.isEmpty.not()) {
            val (node, indent, onlyChild, prefix) = stack.pop()
            when (node) {
                is SPPTLeaf -> {
                    val s = handleLeaf(node, indent, onlyChild)
                    result.push(setOf(s))
                }

                is SPPTBranch -> when {
                    null == prefix -> {
                        val prefix2 = branchPrefix(node, indent, onlyChild)
                        stack.push(Item(node, indent, onlyChild, prefix2))
                        for ((alt, children) in node.childrenAlternatives.entries.sortedBy { it.key }) {
                            when {
                                children.isEmpty() -> Unit
                                1 == children.size -> stack.push(Item(children[0], indent, true, null))
                                else -> {
                                    children.forEach { stack.push(Item(it, indent + indentText, false, null)) }
                                }
                            }
                        }
                    }

                    else -> {
                        val r = HashSet<String>()
                        for ((alt, children) in node.childrenAlternatives.entries.sortedBy { it.key }) {
                            val prefix2 = if (1 == node.childrenAlternatives.size) {
                                "$prefix {"
                            } else {
                                "$prefix|$alt {"
                            }
                            when {
                                children.isEmpty() -> {
                                    r.add("$prefix2 }")
                                }

                                1 == children.size -> {
                                    val childrenStr = result.pop()
                                    r.add("$prefix2 ${childrenStr.first()} }")
                                }

                                else -> {
                                    val childs = children.map { result.pop() }
                                    val cs = childs.joinToString(separator = this.lineSeparator) { it.joinToString(separator = this.lineSeparator) }
                                    val rs = "${prefix2}${this.lineSeparator}$cs${this.lineSeparator}${indent}}"
                                    r.add(rs)
                                }
                            }
                        }
                        result.push(r)
                    }
                }
            }
        }
        return result.pop()
    }


    fun handleLeaf(target: SPPTLeaf, indent: String, onlyChild: Boolean): String {
        val t = when {
            target.isEmptyLeaf -> target.name
            (target.isLiteral) -> {
                "'${target.matchedText.replace("\n", "\u23CE").replace("\t", "\u2B72")}'"
            }

            (target.isPattern) -> {
                "${target.name} : '${target.matchedText.replace("\n", "\u23CE").replace("\t", "\u2B72")}'"
            }

            else -> throw RuntimeException("should not happen")
        }
        val s = (if (onlyChild) "" else indent) + t
        return s
    }

    fun branchPrefix(target: SPPTBranch, indent: String, onlyChild: Boolean): String {
        var s = if (onlyChild) "" else indent
        s += target.name
        if (target.option != 0) {
            //s += "|${target.option}"
        }
        // s += if (target.childrenAlternatives.size > 1) "*" else ""
        //s += " {"
        return s
    }
    /*
         fun visitBranch(target: SPPTBranch, arg: Indent): Set<String> {
            val r = HashSet<String>()

            for (children in target.childrenAlternatives) {
                var s = if (arg.onlyChild) " " else arg.text
                s += target.name
                if (target.option!=0) {
     //               s+="|${target.option}"
                }
                s += if (target.childrenAlternatives.size > 1) "*" else ""
                s += " {"
                if (children.isEmpty()) {
                    s += "}"
                    r.add(s)
                } else if (children.size == 1) {
                    var currentSet: MutableSet<String> = mutableSetOf<String>();
                    currentSet.add(s)
                    currentSet = this.visitOnlyChild(currentSet, children, arg)
                    for (sc in currentSet) {
                        var sc1 = sc
                        sc1 += "}"
                        r.add(sc1)
                    }
                } else {
                    s += this.lineSeparator

                    var currentSet: MutableSet<String> = HashSet()
                    currentSet.add(s)
                    for (i in 0 until children.size) {
                        currentSet = this.visitChild(currentSet, children, i, arg)
                    }

                    for (sc in currentSet) {
                        var sc1 = sc
                        sc1 += arg.text
                        sc1 += "}"
                        r.add(sc1)
                    }
                }
            }
            return r
        }

        private fun visitOnlyChild(currentSet: MutableSet<String>, children: List<SPPTNode>, indent: Indent): MutableSet<String> {
            val r = HashSet<String>()
            val ssc = visitNode(children.get(0), indent.next(this.indentIncrement, true))

            for (current in currentSet) {
                for (sc in ssc) {
                    val b = StringBuilder(current)
                    b.append(sc)
                    b.append(" ")
                    r.add(b.toString())
                }
            }
            return r
        }

        private fun visitChild(currentSet: MutableSet<String>, children: List<SPPTNode>, index: Int, indent: Indent): MutableSet<String> {
            val r = HashSet<String>()
            val ssc = visitNode(children.get(index), indent.next(this.indentIncrement, false))

            for (current in currentSet) {
                for (sc in ssc) {
                    val b = StringBuilder(current)
                    b.append(sc)
                    b.append(this.lineSeparator)
                    r.add(b.toString())
                }
            }
            return r
        }
     */
}