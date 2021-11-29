/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.runtime.graph

import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsItemsKind

internal class TreeData {

    class ChildData(
        val state: ParserState,
        val startPosition: Int,
        val nextInputPosition: Int,
        val numNonSkipChildren: Int
    )

    // child --> parent
    private val _parent = mutableMapOf<GrowingNodeIndex, GrowingNodeIndex>()

    private val _growing = mutableMapOf<GrowingNodeIndex, MutableList<GrowingNodeIndex>>()

    // (state,startPosition) --> listOf<child> //maybe optimise because only ambiguous choice nodes have multiple child options
    private val _complete = mutableMapOf<GrowingNodeIndex, MutableList<GrowingNodeIndex>>()

    var root: GrowingNodeIndex? = null; private set

    // needed when parsing embedded sentences and skip
    val nextInputPosition: Int? get() = root?.nextInputPosition

    fun childrenFor(runtimeRule: RuntimeRule, option:Int, startPosition: Int): Set<List<GrowingNodeIndex>> {
        val keys = this._complete.keys.filter {
            it.startPosition == startPosition
                    && it.state.options.contains( option)
                    && it.state.runtimeRules.contains(runtimeRule) }
        return when (keys.size) {
            0 -> emptySet()
            1 -> setOf(this._complete[keys[0]]!!)
            else -> error("should not happen")
        }
    }

    fun setInitialSkip(treeData: TreeData?) {

    }

    fun setRoot(root:GrowingNodeIndex) {
        this.root = root
    }


    fun start(key: GrowingNodeIndex) {
        val growing = mutableListOf<GrowingNodeIndex>()
        this._growing[key] = growing
    }

    fun setFirstChild(parent: GrowingNodeIndex,child:GrowingNodeIndex) {
        if (parent.state.isAtEnd) {
            var complete = this._complete[parent]
            if (null == complete) {
                complete = mutableListOf(child)
                this._complete[parent] = complete
            }
        } else {
            var growing = this._growing[parent]
            if (null == growing) {
                growing = mutableListOf(child)
                this._growing[parent] = growing
            }
        }
    }

    fun appendChild(oldParent: GrowingNodeIndex, newParent: GrowingNodeIndex, nextChild: GrowingNodeIndex) {
        val children = this._growing[oldParent]!! //should never be null
        if (newParent.state.isAtEnd) {
            this._complete[newParent] = children
        } else {
            this._growing[newParent] = children
        }
        children.add(nextChild)  // should never be null
    }


}