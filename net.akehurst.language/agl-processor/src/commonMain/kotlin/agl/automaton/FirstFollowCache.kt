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

package net.akehurst.language.agl.automaton

import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.collections.LazyMapNonNull
import net.akehurst.language.collections.lazyMapNonNull

internal class FirstFollowCache {

    // prev/context -> ( RulePosition -> Set<Terminal-RuntimeRule> )
    private val _firstTerminal = lazyMapNonNull<RulePosition, LazyMapNonNull<RulePosition, MutableSet<RuntimeRule>>> { lazyMapNonNull { mutableSetOf() } }

    // prev/context -> ( RulePosition -> function to get value from _firstTerminal )
    private val _firstOfInContext = lazyMapNonNull<RulePosition, LazyMapNonNull<RulePosition, MutableSet<RuntimeRule>>> { lazyMapNonNull { mutableSetOf() } }

    // prev/context -> ( RulePosition -> function to get value from _firstTerminal )
    private val _followInContext = lazyMapNonNull<RulePosition, LazyMapNonNull<RulePosition, MutableSet<RuntimeRule>>> { lazyMapNonNull { mutableSetOf() } }

    fun containsFirstTerminal(prev: RulePosition, rulePosition: RulePosition): Boolean = this._firstTerminal[prev].containsKey(rulePosition)

    fun firstTerminal(prev: RulePosition, rulePosition: RulePosition): Set<RuntimeRule> {
        return this._firstTerminal[prev][rulePosition]
    }

    fun firstOfInContext(prev: RulePosition, rulePosition: RulePosition): Set<RuntimeRule> {
        return this._firstOfInContext[prev][rulePosition]
    }

    fun followInContext(prev: RulePosition, rulePosition: RulePosition): Set<RuntimeRule> {
        return this._followInContext[prev][rulePosition]
    }

    fun addFirstTerminalInContext(prev: RulePosition, rulePosition: RulePosition, terminal: RuntimeRule) {
        this._firstTerminal[prev][rulePosition].add(terminal)
    }

    fun addFirstOfInContext(prev: RulePosition, rulePosition: RulePosition, terminal: RuntimeRule) {
        this._firstOfInContext[prev][rulePosition].add(terminal)
    }

    fun addFollowInContext(prev: RulePosition, rulePosition: RulePosition, terminal: RuntimeRule) {
        this._followInContext[prev][rulePosition].add(terminal)
    }

}