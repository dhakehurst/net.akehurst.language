package net.akehurst.language.agl.agl.automaton

import net.akehurst.language.agl.automaton.LookaheadSetPart
import net.akehurst.language.agl.automaton.ParserStateSet
import net.akehurst.language.agl.runtime.structure.RuleOptionPosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind

internal class FirstOf(
    val stateSet: ParserStateSet
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
    private val _firstOfNotEmpty = Array<FirstOfResult?>(this.stateSet.runtimeRuleSet.runtimeRules.size, { null })

    /*
     * return the LookaheadSet for the given RuleOptionPosition.
     * i.e. the set of all possible Terminals that would be expected in a sentence after the given RuleOptionPosition.
     *
     * firstOf needs to iterate along a rule (calling .next()) and down (recursively stopping appropriately)
     * next() needs to be called to skip over empty rules (empty or empty lists)
    */
    fun expectedAt(rulePosition: RuleOptionPosition, ifReachedEnd: LookaheadSetPart): LookaheadSetPart {
        return when {
            rulePosition.isAtEnd -> ifReachedEnd
            else -> {
                // this will iterate .next() until end of rule so no need to do it here
                val res = firstOfRpNotEmpty(rulePosition, mutableMapOf(), BooleanArray(this.stateSet.runtimeRuleSet.runtimeRules.size))
                res.endResult(ifReachedEnd)
            }
        }
    }

    private fun firstOfRpNotEmpty(rulePosition: RuleOptionPosition, doneRp: MutableMap<RuleOptionPosition, FirstOfResult>, done: BooleanArray): FirstOfResult {
        var existing = doneRp[rulePosition]
        if (null == existing) {
            /*DEBUG*/ if (rulePosition.isAtEnd) error("Internal Error")
            var needsNext = false
            var result = LookaheadSetPart.EMPTY
            var rps = setOf(rulePosition)
            while (rps.isNotEmpty()) { // loop here to handle empties
                val nrps = mutableSetOf<RuleOptionPosition>()
                for (rp in rps) {
                    //TODO: handle self recursion, i.e. multi/slist perhaps filter out rp from rp.next or need a 'done' map to results
                    val item = rp.item
                    when {
                        //item is null only when rp.isAtEnd
                        null == item /*rp.isAtEnd*/ -> needsNext = true
                        item.isEmptyRule -> nrps.addAll(rp.next())
                        else -> when (item.kind) {
                            RuntimeRuleKind.GOAL -> TODO()
                            RuntimeRuleKind.TERMINAL -> result = result.union(LookaheadSetPart(false, false, false, setOf(item)))
                            RuntimeRuleKind.EMBEDDED -> {
                                val embSS = item.embeddedRuntimeRuleSet!!.fetchStateSetFor(item.embeddedStartRule!!, AutomatonKind.LOOKAHEAD_1)
                                val fo = FirstOf(embSS)
                                val f = fo.firstOfNotEmpty(
                                    item.embeddedStartRule,
                                    doneRp,
                                    BooleanArray(item.embeddedRuntimeRuleSet.runtimeRules.size)
                                )
                                result = result.union(f.result)
                                if (f.needsFirstOfParentNext) {
                                    needsNext = true
                                }
                            }

                            RuntimeRuleKind.NON_TERMINAL -> {
                                val f = firstOfNotEmpty(item, doneRp, done)
                                result = result.union(f.result)
                                if (f.needsFirstOfParentNext) nrps.addAll(rp.next())
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

    private fun firstOfNotEmpty(rule: RuntimeRule, doneRp: MutableMap<RuleOptionPosition, FirstOfResult>, done: BooleanArray): FirstOfResult {
        return when {
            0 > rule.ruleNumber -> when { // handle special kinds of RuntimeRule
                RuntimeRuleSet.GOAL_RULE_NUMBER == rule.ruleNumber -> TODO()
                RuntimeRuleSet.EOT_RULE_NUMBER == rule.ruleNumber -> TODO()
                RuntimeRuleSet.SKIP_RULE_NUMBER == rule.ruleNumber -> TODO()
                RuntimeRuleSet.SKIP_CHOICE_RULE_NUMBER == rule.ruleNumber -> firstOfNotEmptySafe(rule, doneRp, done)
                RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD_RULE_NUMBER == rule.ruleNumber -> TODO()
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

    private fun firstOfNotEmptySafe(rule: RuntimeRule, doneRp: MutableMap<RuleOptionPosition, FirstOfResult>, done: BooleanArray): FirstOfResult {
        var needsNext = false
        var result = LookaheadSetPart.EMPTY
        val pos = rule.rulePositionsAt[0]
        for (rp in pos) {
            val item = rp.item
            when {
                null == item -> error("should never happen")
                item.isEmptyRule -> needsNext = true //should not happen
                else -> when (item.kind) {
                    RuntimeRuleKind.GOAL -> error("should never happen")
                    RuntimeRuleKind.EMBEDDED -> {
                        val embSS = item.embeddedRuntimeRuleSet!!.fetchStateSetFor(item.embeddedStartRule!!, AutomatonKind.LOOKAHEAD_1)
                        val fo = FirstOf(embSS)
                        val f = fo.firstOfNotEmpty(item.embeddedStartRule, doneRp, BooleanArray(item.embeddedRuntimeRuleSet.runtimeRules.size))
                        result = result.union(f.result)
                        if (f.needsFirstOfParentNext) {
                            needsNext = true
                        }
                    }

                    RuntimeRuleKind.TERMINAL -> result = result.union(LookaheadSetPart(false, false, false, setOf(item)))
                    RuntimeRuleKind.NON_TERMINAL -> {
                        val f = firstOfRpNotEmpty(rp, doneRp, done)
                        result = result.union(f.result)
                        needsNext = needsNext || f.needsFirstOfParentNext
                    }
                }
            }
        }
        return FirstOfResult(needsNext, result)
    }

}