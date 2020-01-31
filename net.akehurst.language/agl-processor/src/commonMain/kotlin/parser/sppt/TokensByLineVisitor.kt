/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.api.sppt.*


class TokensByLineVisitor : SharedPackedParseTreeVisitor<Unit, Unit> {

    val lines = mutableListOf<MutableList<SPPTLeaf>>()
    fun MutableList<MutableList<SPPTLeaf>>.getOrCreate(index:Int): MutableList<SPPTLeaf> {
        if (index >= this.size) {
            for(i in this.size-1 until index) {
                this.add(mutableListOf())
            }
        }
        return this[index]
    }

    fun visit(target: SPPTNode, arg: Unit) {
        return when (target) {
            is SPPTBranch -> this.visit(target, arg)
            is SPPTLeaf -> this.visit(target, arg)
            else -> throw SPPTException("Unknown subtype of SPPTNode ${target::class.simpleName}", null)
        }
    }

    override fun visit(target: SharedPackedParseTree, arg: Unit) {
        return this.visit(target.root, arg)
    }

    override fun visit(target: SPPTBranch, arg: Unit) {
        target.children.forEach {
            this.visit(it, arg)
        }
    }

    override fun visit(target: SPPTLeaf, arg: Unit) {
        lines.getOrCreate(target.location.line-1).add(target)
    }
}