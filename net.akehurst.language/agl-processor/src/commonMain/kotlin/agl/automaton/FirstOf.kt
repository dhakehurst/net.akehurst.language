package net.akehurst.language.agl.agl.automaton

import net.akehurst.language.agl.automaton.LookaheadSetPart
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.processor.AutomatonKind

internal class FirstOf(
    val runtimeRulesSize: Int
) {

    companion object {
        data class FirstOfResult(val needsFirstOfParentNext: Boolean, val result: LookaheadSetPart) {
            fun union(other: FirstOfResult) = FirstOfResult(this.needsFirstOfParentNext || other.needsFirstOfParentNext, this.result.union(other.result))
            fun endResult(firstOfParentNext: LookaheadSetPart) = when {
                needsFirstOfParentNext -> result.union(firstOfParentNext)
                else -> result
            }
        }

    }

    // index by RuntimeRule.number
    private val _firstOfNotEmpty = MutableList<FirstOfResult?>(this.runtimeRulesSize, { null })

    /*
     * return the LookaheadSet for the given RulePosition.
     * i.e. the set of all possible Terminals that would be expected in a sentence after the given RulePosition.
     *
     * firstOf needs to iterate along a rule (calling .next()) and down (recursively stopping appropriately)
     * next() needs to be called to skip over empty rules (empty or empty lists)
    */
    fun expectedAt(rulePosition: RulePosition, ifReachedEnd: LookaheadSetPart): LookaheadSetPart {
        this._firstOfNotEmpty.clear()
        return when {
            rulePosition.isAtEnd -> ifReachedEnd
            else -> {
                // this will iterate .next() until end of rule so no need to do it here
                val res = firstOfRpNotEmpty(rulePosition, mutableMapOf(), BooleanArray(this.runtimeRulesSize))
                res.endResult(ifReachedEnd)
            }
        }
    }

    private fun firstOfRpNotEmpty(rulePosition: RulePosition, doneRp: MutableMap<RulePosition, FirstOfResult>, done: BooleanArray): FirstOfResult {
        var existing = doneRp[rulePosition]
        if (null == existing) {
            /*DEBUG*/ if (rulePosition.isAtEnd) error("Internal Error")
            var needsNext = false
            var result = LookaheadSetPart.EMPTY
            var rps = setOf(rulePosition)
            while (rps.isNotEmpty()) { // loop here to handle empties
                val nrps = mutableSetOf<RulePosition>()
                for (rp in rps) {
                    //TODO: handle self recursion, i.e. multi/slist perhaps filter out rp from rp.next or need a 'done' map to results
                    for (item in rp.items) {
                        val rhs = item?.rhs
                        when {
                            //item is null only when rp.isAtEnd
                            null == rhs /*rp.isAtEnd*/ -> needsNext = true
                            else -> when (rhs) {
                                is RuntimeRuleRhsEmpty -> nrps.addAll(rp.next())
                                is RuntimeRuleRhsGoal -> TODO()
                                is RuntimeRuleRhsEmbedded -> {
                                    val embSS = rhs.embeddedRuntimeRuleSet.fetchStateSetFor(rhs.embeddedStartRule.tag, AutomatonKind.LOOKAHEAD_1)
                                    val f = embSS.firstOf.firstOfNotEmpty(
                                        rhs.embeddedStartRule,
                                        doneRp,
                                        BooleanArray(rhs.embeddedRuntimeRuleSet.runtimeRules.size)
                                    )
                                    result = result.union(f.result)
                                    if (f.needsFirstOfParentNext) {
                                        needsNext = true
                                    }
                                }

                                is RuntimeRuleRhsTerminal -> result = result.union(LookaheadSetPart(false, false, false, setOf(item)))

                                is RuntimeRuleRhsNonTerminal -> {
                                    val f = firstOfNotEmpty(item, doneRp, done)
                                    result = result.union(f.result)
                                    if (f.needsFirstOfParentNext) nrps.addAll(rp.next())
                                }

                            }
                        }
                    }
                }
                rps = nrps
            }
            existing = FirstOfResult(needsNext, result)
            doneRp[rulePosition] = existing
        }
        return existing
    }

    private fun firstOfNotEmpty(rule: RuntimeRule, doneRp: MutableMap<RulePosition, FirstOfResult>, done: BooleanArray): FirstOfResult {
        return when {
            0 > rule.ruleNumber -> when { // handle special kinds of RuntimeRule
                RuntimeRuleSet.GOAL_RULE_NUMBER == rule.ruleNumber -> TODO()
                RuntimeRuleSet.EOT_RULE_NUMBER == rule.ruleNumber -> TODO()
                RuntimeRuleSet.SKIP_RULE_NUMBER == rule.ruleNumber -> TODO()
                RuntimeRuleSet.SKIP_CHOICE_RULE_NUMBER == rule.ruleNumber -> firstOfNotEmptySafe(rule, doneRp, done)
                RuntimeRuleSet.RUNTIME_LOOKAHEAD_RULE_NUMBER == rule.ruleNumber -> TODO()
                else -> error("unsupported rule number $rule")
            }

            done[rule.ruleNumber] -> _firstOfNotEmpty[rule.ruleNumber] ?: FirstOfResult(false, LookaheadSetPart.EMPTY)
            else -> {
                var result: FirstOfResult? = null//_firstOfNotEmpty[rule.number]
                if (null == result) {
                    done[rule.ruleNumber] = true
                    result = firstOfNotEmptySafe(rule, doneRp, done)
                    _firstOfNotEmpty[rule.ruleNumber] = result
                }
                result
            }
        }
    }

    private fun firstOfNotEmptySafe(rule: RuntimeRule, doneRp: MutableMap<RulePosition, FirstOfResult>, done: BooleanArray): FirstOfResult {
        var needsNext = false
        var result = LookaheadSetPart.EMPTY
        val rps = rule.rulePositionsAtStart
        for (rp in rps) {
            for (item in rp.items) {
                val rhs = item.rhs
                when {
                    item.isEmptyTerminal -> needsNext = true //should not happen
                    else -> when (rhs) {
                        is RuntimeRuleRhsGoal -> error("should never happen")
                        is RuntimeRuleRhsEmbedded -> {
                            val embSS = rhs.embeddedRuntimeRuleSet.fetchStateSetFor(rhs.embeddedStartRule.tag, AutomatonKind.LOOKAHEAD_1)
                            val f = embSS.firstOf.firstOfNotEmpty(rhs.embeddedStartRule, doneRp, BooleanArray(rhs.embeddedRuntimeRuleSet.runtimeRules.size))
                            result = result.union(f.result)
                            if (f.needsFirstOfParentNext) {
                                needsNext = true
                            }
                        }

                        is RuntimeRuleRhsTerminal -> result = result.union(LookaheadSetPart(false, false, false, setOf(item)))
                        is RuntimeRuleRhsNonTerminal -> {
                            val f = firstOfRpNotEmpty(rp, doneRp, done)
                            result = result.union(f.result)
                            needsNext = needsNext || f.needsFirstOfParentNext
                        }
                    }
                }
            }
        }
        return FirstOfResult(needsNext, result)
    }

}