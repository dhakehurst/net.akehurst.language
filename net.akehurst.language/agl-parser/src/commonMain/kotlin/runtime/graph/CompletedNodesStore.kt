/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.runtime.graph

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.collections.MapIntTo
import kotlin.math.pow

internal class CompletedNodesStore2<T>(val num: Int, val inputLength: Int) {

    companion object {
        fun nPowersOf2(n: Int): List<Int> = when (n) {
            1 -> listOf(2)
            else -> nPowersOf2(n - 1) + (2.0).pow(n).toInt()
        }

        val powersOf2 = nPowersOf2(31)

        fun closestGreaterPowerOf2(n: Int): Int {
            val i = powersOf2.indexOfFirst { it > n }
            if (i < 0) {
                error("Too many rules")
            } else {
                return i + 1
            }
        }
    }

    private val _map = MapIntTo<MapIntTo<T>>(closestGreaterPowerOf2(num), 1.0) { MapIntTo<T>() } //should never need to resize
    private var _goal: T? = null
    private var _eot = MapIntTo<T>()
    private var _skip: T? = null

    fun clear() {
        this._map.clear()
    }

    operator fun set(runtimeRule: RuntimeRule, inputPosition: Int, value: T) {
        when {
            runtimeRule.ruleNumber == RuntimeRuleSet.GOAL_RULE_NUMBER -> {
                //check(0 == inputPosition) //TODO: remove this
                _goal = value
            }

            runtimeRule.ruleNumber == RuntimeRuleSet.EOT_RULE_NUMBER -> {
                _eot[inputPosition] = value
            }

            runtimeRule.ruleNumber == RuntimeRuleSet.SKIP_RULE_NUMBER -> {
                //check(0 == inputPosition) //TODO: remove this
                _skip = value
            }
            //0 > runtimeRule.number -> error("") //TODO: remove this
            else -> {
                var m2 = _map[runtimeRule.ruleNumber]
                if (null == m2) {
                    //m2 = MapIntTo<T>()
                    if (_map.isInitialised) {
                        _map.setToInitialised(runtimeRule.ruleNumber)
                        m2 = _map[runtimeRule.ruleNumber]!!
                    } else {
                        m2 = MapIntTo<T>()
                        _map[runtimeRule.ruleNumber] = m2
                    }
                }
                m2[inputPosition] = value
            }
        }
    }

    operator fun get(runtimeRule: RuntimeRule, inputPosition: Int): T? {
        return when {
            runtimeRule.ruleNumber == RuntimeRuleSet.GOAL_RULE_NUMBER -> {
                //check(0 == inputPosition) //TODO: remove this
                _goal
            }

            runtimeRule.ruleNumber == RuntimeRuleSet.EOT_RULE_NUMBER -> {
                _eot[inputPosition]
            }

            runtimeRule.ruleNumber == RuntimeRuleSet.SKIP_RULE_NUMBER -> {
                //check(0 == inputPosition) //TODO: remove this
                _skip
            }

            else -> {
                val m2 = _map[runtimeRule.ruleNumber] ?: return null
                m2[inputPosition]
            }
        }
    }


}

internal class CompletedNodesStore<T>(val num: Int) {

    private val _map = HashMap<Pair<RuntimeRule, Int>, T>() //TODO: make this more efficient
    private var _goal: T? = null
    private var _eot = HashMap<Int, T>()//MapIntTo<T>()
    private var _skip: T? = null

    fun clear() {
        this._map.clear()
        this._goal = null
        this._eot.clear()
        this._skip = null
    }

    operator fun set(runtimeRule: RuntimeRule, inputPosition: Int, value: T) {
        when {
            runtimeRule.ruleNumber == RuntimeRuleSet.GOAL_RULE_NUMBER -> {
                //check(0 == inputPosition) //TODO: remove this
                _goal = value
            }

            runtimeRule.ruleNumber == RuntimeRuleSet.EOT_RULE_NUMBER -> {
                _eot[inputPosition] = value
            }

            runtimeRule.ruleNumber == RuntimeRuleSet.SKIP_RULE_NUMBER -> {
                //check(0 == inputPosition) //TODO: remove this
                _skip = value
            }
            //0 > runtimeRule.number -> error("") //TODO: remove this
            else -> {
                _map[Pair(runtimeRule, inputPosition)] = value
            }
        }
    }

    operator fun get(runtimeRule: RuntimeRule, inputPosition: Int): T? {
        return when {
            runtimeRule.ruleNumber == RuntimeRuleSet.GOAL_RULE_NUMBER -> {
                //check(0 == inputPosition) //TODO: remove this
                _goal
            }

            runtimeRule.ruleNumber == RuntimeRuleSet.EOT_RULE_NUMBER -> {
                _eot[inputPosition]
            }

            runtimeRule.ruleNumber == RuntimeRuleSet.SKIP_RULE_NUMBER -> {
                //check(0 == inputPosition) //TODO: remove this
                _skip
            }

            else -> {
                _map[Pair(runtimeRule, inputPosition)]
            }
        }
    }


}