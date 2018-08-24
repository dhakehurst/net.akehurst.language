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
import net.akehurst.language.api.sppt.SharedPackedParseTreeVisitor

class ToStringVisitor(val lineSeparator: String, val indentIncrement: String) : SharedPackedParseTreeVisitor<Set<String>, ToStringVisitor.Indent> {

    class Indent(val text: String, val onlyChild: Boolean) {
        fun next( increment: String, onlyChild: Boolean) : Indent {
            return Indent(this.text+increment, onlyChild)
        }
    }

    override fun visit(target: SharedPackedParseTree, arg: Indent): Set<String> {
        return target.root.accept(this, arg)
    }


    override fun visit(target: SPPTLeaf, arg: Indent): Set<String> {
        val s = (if (arg.onlyChild)  " " else arg.text) + target.name + " : \"" + target.matchedText.replace("\n", 0x23CE.toString())
        return setOf(s)
    }


    override fun visit(target: SPPTBranch, arg: Indent): Set<String> {
        val r = HashSet<String>()

        for (children in target.childrenAlternatives) {
            var s = if (arg.onlyChild) " " else arg.text
            s += target.name
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
        val ssc = children.get(0).accept(this, indent.next(this.indentIncrement, true))

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
        val ssc = children.get(index).accept(this, indent.next(this.indentIncrement, false))

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
}