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


import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.api.sppt.SharedPackedParseTree

//TODO: currently this has to be public, because otherwise kotlin does not
// use the non-mangled names for properties
/*internal */ class SharedPackedParseTreeDefault(
    override val root: SPPTNode,
    override val seasons: Int,
    override val maxNumHeads: Int
) : SharedPackedParseTree {

    init {
        root.tree = this
    }

    override fun contains(other: SharedPackedParseTree): Boolean {
        return this.root.contains(other.root)
    }

    private val _tokensByLine: List<List<SPPTLeaf>> by lazy {
        val visitor = TokensByLineVisitor()
        visitor.visitTree(this, emptyList())
        visitor.lines
    }

    override fun tokensByLineAll(): List<List<SPPTLeaf>> {
        return this._tokensByLine
    }

    override fun tokensByLine(line: Int): List<SPPTLeaf> {
        val tbl = this._tokensByLine
        return if (tbl.isEmpty() || line >= tbl.size) {
            emptyList()
        } else {
            tbl[line]
        }
    }

    override val asString: String by lazy {
        SPPT2InputText().visitTree(this, "")
    }

    override val countTrees: Int by lazy {
        CountTreesVisitor().visitTree(this, Unit)
    }

    override val toStringAll: String by lazy {
        this.toStringAllWithIndent("")
    }

    override fun toStringAllWithIndent(indentIncrement: String): String {
        val visitor = ToStringVisitor("\n", indentIncrement)
        val all: Set<String> = visitor.visitTree(this, ToStringVisitor.Indent("", true))
        val total = all.size
        val sep = "\n"
        var cur = 0
        var res = ""
        for (pt in all) {
            cur++
            res += "Tree ${cur} of ${total}\n"
            res += pt
            res += "\n"
        }
        return all.joinToString(sep)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is SharedPackedParseTree) {
            this.root == other.root
        } else {
            false
        }
    }

}