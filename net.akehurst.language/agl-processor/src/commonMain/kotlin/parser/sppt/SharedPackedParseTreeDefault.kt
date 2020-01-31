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

import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.sppt.SharedPackedParseTreeVisitor
import kotlin.js.JsName

class SharedPackedParseTreeDefault(
        override val root: SPPTNode,
        override val seasons: Int,
        override val maxNumHeads: Int
) : SharedPackedParseTree {

    override fun contains(other: SharedPackedParseTree): Boolean {
        return this.root.contains(other.root)
    }

    override val tokensByLine: List<List<SPPTLeaf>> by lazy {
        val visitor = TokensByLineVisitor()
        visitor.visit(this, Unit)
        visitor.lines
    }

    override val asString: String by lazy {
        this.accept(SPPT2InputText(), "")
    }

    override val countTrees: Int by lazy {
        CountTreesVisitor().visit(this, Unit)
    }

    override val toStringAll: String by lazy {
        this.toStringIndented("")
    }


    override fun toStringIndented(indentIncrement: String): String {
        val visitor = ToStringVisitor("\n", indentIncrement)
        val all: Set<String> = this.accept(visitor, ToStringVisitor.Indent("", true))
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

    override fun <T, A> accept(visitor: SharedPackedParseTreeVisitor<T, A>, arg: A): T {
        return visitor.visit(this, arg)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is SharedPackedParseTree) {
            this.root == other.root
        } else {
            false
        }
    }

}