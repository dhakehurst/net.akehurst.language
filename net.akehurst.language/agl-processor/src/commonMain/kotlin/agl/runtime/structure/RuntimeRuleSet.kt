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

package net.akehurst.language.agl.runtime.structure

import net.akehurst.language.api.parser.ParseException
import net.akehurst.language.collections.transitveClosure
import net.akehurst.language.parser.scannerless.InputFromCharSequence

class RuntimeRuleSet(rules: List<RuntimeRule>) {


    companion object {
        val GOAL_RULE_NUMBER = -1;
        val EOT_RULE_NUMBER = -2;
        val END_OF_TEXT = RuntimeRule(EOT_RULE_NUMBER, InputFromCharSequence.END_OF_TEXT, RuntimeRuleKind.TERMINAL, false, false)

        fun createGoal(userGoalRule: RuntimeRule): RuntimeRule {
            val gr = RuntimeRule(GOAL_RULE_NUMBER, "<GOAL>", RuntimeRuleKind.GOAL, false, false)
            gr.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, -1, 0, arrayOf(userGoalRule, END_OF_TEXT))
            return gr
        }
    }

    private val nonTerminalRuleNumber: MutableMap<String, Int> = mutableMapOf()
    private val terminalRuleNumber: MutableMap<String, Int> = mutableMapOf()

    data class IndexCanGrowIntoAt(
        val childRuleNumber: Int,
        val ancesstorRuleNumber: Int,
        val at: Int
    ) {}

    private val canGrowIntoAt_cache: MutableMap<IndexCanGrowIntoAt, Boolean> = mutableMapOf()

    //TODO: are Arrays faster than Lists?
    val runtimeRules: Array<out RuntimeRule> by lazy {
        rules.sortedBy { it.number }.toTypedArray()
    }

    val allSkipRules: Array<RuntimeRule> by lazy {
        this.runtimeRules.filter { it.isSkip }.toTypedArray()
    }

    val allSkipTerminals: Array<RuntimeRule> by lazy {
        this.allSkipRules.flatMap {
            if (it.isTerminal)
                listOf(it)
            else
                it.rhs.items.filter { it.isTerminal }
        }.toTypedArray()
    }

    val isSkipTerminal: Array<Boolean> by lazy {
        this.runtimeRules.map {
            this.calcIsSkipTerminal(it)
        }.toTypedArray()
    }

    val terminalRules: Array<RuntimeRule> by lazy {
        this.runtimeRules.mapNotNull {
            if (it.isTerminal)
                it
            else
                null
        }.toTypedArray()
    }

    val firstTerminals: Array<Set<RuntimeRule>> by lazy {
        this.runtimeRules.map { this.calcFirstTerminals(it) }
            .toTypedArray()
    }

    val firstSkipRuleTerminalPositions: Set<RulePosition> by lazy {
        this.calcFirstTerminalSkipRulePositions()
    }

    val expectedSkipItemRulePositionsTransitive: Set<RulePosition> by lazy {
        calcExpectedSkipItemRulePositionTransitive()
    }

    val expectedTerminalRulePositions = lazyMap<RulePosition, Array<RulePosition>> {
        calcExpectedTerminalRulePositions(it).toTypedArray()
    }
    /*
        val expectedItemRulePositions = lazyMap<RulePosition, Set<RulePosition>> {
            calcExpectedItemRulePositions(it)
        }

        val expectedItemRulePositionsTransitive = lazyMap<RulePosition, Set<RulePosition>> {
            calcExpectedItemRulePositionTransitive(it)
        }
    */
    val subTerminals: Array<Set<RuntimeRule>> by lazy {
        this.runtimeRules.map {
            var rr = it.findAllTerminal()
            for (r in this.subNonTerminals[it.number]) {
                rr += r.findAllTerminal()
            }
            rr += this.allSkipTerminals
            rr
        }.toTypedArray()
    }

    val subNonTerminals: Array<Set<RuntimeRule>> by lazy {
        this.runtimeRules.map { it.findSubRules() }.toTypedArray()
    }

    //val superRules: Array<List<RuntimeRule>> by lazy {
    //    this.runtimeRules.map { this.calcSuperRule(it) }.toTypedArray()
    //}

    val firstSuperNonTerminal by lazy {
        this.runtimeRules.map { this.calcFirstSuperNonTerminal(it) }.toTypedArray()
    }

    val firstTerminals2 = lazyMap<RulePosition, Set<RuntimeRule>> {
        val trps = expectedTerminalRulePositions[it] ?: arrayOf()
        trps.flatMap { it.items }.toSet()
    }

    private val growsInto: LazyArray<Set<RulePosition>> = lazyArray(runtimeRules.size) {
        calcGrowsInto(it)
    }

    /** Map of userGoalRule -> next Closure number **/
    private val nextClosure = mutableMapOf<RuntimeRule, ClosureNumber>()

    /** Map of userGoalRule -> next State number **/
    private val nextState = mutableMapOf<RuntimeRule, StateNumber>()

    data class StateKey(val rp: RulePosition, val graftLookahead: Set<RuntimeRule>)//val ancestorRPs: List<RulePosition>)

    //TODO: should include the glh also in the key
    private val states = lazyMapNonNull<RuntimeRule, LazyMapNonNull<StateKey, MutableSet<RulePositionState>>> { userGoalRule ->
        lazyMapNonNull<StateKey, MutableSet<RulePositionState>> { mutableSetOf() }
    }

    /** Map of goalRule -> (Map of ClosureNumber -> closure of RulePositionState)  **/
    private val closures = lazyMapNonNull<RuntimeRule, MutableMap<ClosureNumber, RulePositionStateClosure>> { userGoalRule ->
        mutableMapOf()
    }

    init {
        for (rr in rules) {
//            if (null == rrule) {
//                throw ParserConstructionFailedException("RuntimeRuleSet must not contain a null rule!")
//            }
            if (rr.isNonTerminal) {
                this.nonTerminalRuleNumber[rr.name] = rr.number
            } else {
                this.terminalRuleNumber[rr.name] = rr.number
            }
        }
    }

    private fun calcNextClosureNumber(userGoalRule: RuntimeRule): ClosureNumber {
        val num: ClosureNumber = this.nextClosure[userGoalRule] ?: ClosureNumber(0)
        this.nextClosure[userGoalRule] = ClosureNumber(num.value + 1)
        return num
    }

    private fun calcNextStateNumber(userGoalRule: RuntimeRule): StateNumber {
        val stateNumberValue: StateNumber = this.nextState[userGoalRule] ?: StateNumber(0)
        this.nextState[userGoalRule] = StateNumber(stateNumberValue.value + 1)
        return stateNumberValue
    }

    private fun createRulePositionState(userGoalRule: RuntimeRule, closureNumber: ClosureNumber, rulePosition: RulePosition, ancestorRPs: List<RulePosition>, heightLookahead: Set<RuntimeRule>, graftLookahead: Set<RuntimeRule>): RulePositionState {
        val stateNumber = this.calcNextStateNumber(userGoalRule)
        val rps = RulePositionState(stateNumber, rulePosition, ancestorRPs, heightLookahead, graftLookahead, closureNumber)
        return rps
    }

    private fun fetchRulePositionStates(userGoalRule: RuntimeRule, rulePosition: RulePosition, graftLookahead: Set<RuntimeRule>): Set<RulePositionState> {
        val currentStates = this.states[userGoalRule]
        val fkey = StateKey(rulePosition, graftLookahead)
        return if (currentStates.containsKey(fkey)) {
            currentStates[fkey] ?: throw Exception("Should never be null")
        } else {
            throw Exception("Should never be happen, states shaould already be created")
        }
    }

    /*
        private fun fetchOrCreateRulePositionState(userGoalRule: RuntimeRule, closureNumber: ClosureNumber, rulePosition: RulePosition, ancestorRPs:List<RulePosition>, heightLookahead: Set<RuntimeRule>, graftLookahead: Set<RuntimeRule>): RulePositionState {
            val currentStates = this.states[userGoalRule]
            val key = StateKey(rulePosition, graftLookahead)
            return if (currentStates.containsKey(key)) {
                currentStates[key] ?: throw Exception("Should never be null")
            } else {
                val rps = this.createRulePositionState(userGoalRule, closureNumber, rulePosition, ancestorRPs, heightLookahead, graftLookahead)
                currentStates[key] = rps
                rps
            }
        }
    */
    private fun calcHeightLookahead(ancestorRPs: List<RulePosition>, childRP: RulePosition): Set<RuntimeRule> {
        return if (childRP.isAtEnd) {
            if (ancestorRPs.isEmpty()) {
                emptySet<RuntimeRule>()
            } else {
                val parent = ancestorRPs.last()
                val parentAncestors = ancestorRPs.dropLast(1)
                this.calcHeightLookahead(parentAncestors, parent)
            }
        } else {
            childRP.items.flatMap { fstChildItem ->
                val nextRPs = nextRulePosition(childRP, fstChildItem)
                nextRPs.flatMap { nextRP ->
                    if (nextRP.isAtEnd) {
                        if (ancestorRPs.isEmpty()) {
                            emptySet<RuntimeRule>()
                        } else {
                            val parent = ancestorRPs.last()
                            val parentAncestors = ancestorRPs.dropLast(1)
                            this.calcHeightLookahead(parentAncestors, parent)
                        }
                    } else {
                        val lh: Set<RuntimeRule> = this.firstTerminals2[nextRP]
                            ?: throw ParseException("should never happen")
                        return if (lh.isEmpty()) {
                            throw ParseException("should never happen")
                        } else {
                            lh
                        }
                    }
                }
            }.toSet()
        }
    }

    private fun calcHeightLookahead(ancestorRPs: List<RulePosition>, childRP: RulePosition, ifEmpty: Set<RuntimeRule>): Set<RuntimeRule> {
        return this.calcHeightLookahead(ancestorRPs, childRP)
        /*
        //TODO: return emptySet where heightlookahead is not useful, maybe! i.e. when parentRP is not at start, maybe!
        return if (childRP.isAtEnd) {
            val nextRPs = this.nextRulePosition(parentRP, childRP.runtimeRule)
            nextRPs.flatMap { nextRP ->
                if (nextRP.isAtEnd) {
                    ifEmpty
                } else {
                    val lh: Set<RuntimeRule> = this.firstTerminals2[nextRP]
                        ?: throw ParseException("should never happen")
                    return if (lh.isEmpty()) {
                        ifEmpty
                    } else {
                        lh
                    }
                }
            }.toSet()
        } else {
            childRP.items.flatMap { fstChildItem ->
                val nextRPs = nextRulePosition(childRP, fstChildItem)
                nextRPs.flatMap { nextRP ->
                    if (nextRP.isAtEnd) {
                        ifEmpty
                    } else {
                        val lh: Set<RuntimeRule> = this.firstTerminals2[nextRP]
                            ?: throw ParseException("should never happen")
                        return if (lh.isEmpty()) {
                            ifEmpty
                        } else {
                            lh
                        }
                    }
                }
            }.toSet()
        }
        */
    }

    private fun calcGraftLookahead(ancestorRPs: List<RulePosition>, childRP: RulePosition): Set<RuntimeRule> {
        return if (ancestorRPs.isEmpty()) {
            emptySet()
        } else {
            val parent = ancestorRPs.last()
            val parentAncestors = ancestorRPs.dropLast(1)
            val nextRPs = nextRulePosition(parent, childRP.runtimeRule)
            return if (nextRPs.isEmpty()) {
                calcGraftLookahead(parentAncestors, parent)
            } else {
                nextRPs.flatMap { nextRP ->
                    if (nextRP.isAtEnd) {
                        calcGraftLookahead(parentAncestors, parent)
                    } else {
                        val lh: Set<RuntimeRule> = this.firstTerminals2[nextRP]
                            ?: throw ParseException("should never happen")
                        return if (lh.isEmpty()) {
                            throw ParseException("should never happen")
                        } else {
                            lh
                        }
                    }
                }.toSet()
            }
        }
    }

    private fun calcGraftLookahead(rp: RulePosition, nextItem: RuntimeRule, ifEmpty: Set<RuntimeRule>): Set<RuntimeRule> {
        val nextRPs = nextRulePosition(rp, nextItem)
        return if (nextRPs.isEmpty()) {
            ifEmpty
        } else {
            nextRPs.flatMap { nextRP ->
                if (nextRP.isAtEnd) {
                    ifEmpty
                } else {
                    val lh: Set<RuntimeRule> = this.firstTerminals2[nextRP]
                        ?: throw ParseException("should never happen")
                    return if (lh.isEmpty()) {
                        ifEmpty
                    } else {
                        lh
                    }
                }
            }.toSet()
        }
    }

    internal fun createClosure(userGoalRule: RuntimeRule, closureNumber: ClosureNumber, root: RulePositionState): RulePositionStateClosure {
        val closureSet = setOf(root).transitveClosure { parentRPS ->
            val parentRP = parentRPS.rulePosition
            parentRPS.items.flatMap { rr ->
                val childrenRP = rr.calcExpectedRulePositions(0)
                childrenRP.map { childRP ->
                    val hlh = this.calcHeightLookahead(parentRPS.ancestorRPs + parentRPS.rulePosition, childRP, parentRPS.graftLookahead)
                    val glh2 = this.calcGraftLookahead(parentRPS.ancestorRPs + parentRPS.rulePosition, childRP)
                    val glh = this.calcGraftLookahead(parentRP, rr, parentRPS.graftLookahead)
                    val ancestorRPs = parentRPS.ancestorRPs + parentRP //be nicer to have an OrderedSet !
                    val childRPS = this.createRulePositionState(userGoalRule, closureNumber, childRP, ancestorRPs, hlh, glh2)
                    childRPS
                }
            }.toSet()
        }
        return RulePositionStateClosure(closureNumber, root, closureSet)
    }

    internal fun fetchOrCreateClosure(userGoalRule: RuntimeRule, rps: RulePositionState): RulePositionStateClosure {
        val thisClosures = this.closures[userGoalRule]
        //
        var result = thisClosures[rps.closureNumber]
        if (null == result) {
            result = this.createClosure(userGoalRule, rps.closureNumber, rps)
            thisClosures[rps.closureNumber] = result
        }
        return result
    }

    fun fetchOrCreateRulePositionState(userGoalRule: RuntimeRule, rulePosition: RulePosition, parentRP: RulePosition?, heightLookahead: Set<RuntimeRule>, graftLookahead: Set<RuntimeRule>): RulePositionState {
        TODO("no longer needed")
    }

    private fun fetchOrCreateRulePositionStateAndItsClosure(userGoalRule: RuntimeRule, rulePosition: RulePosition, ancestorRPs: List<RulePosition>, heightLookahead: Set<RuntimeRule>, graftLookahead: Set<RuntimeRule>): Set<RulePositionState> {
        val currentStates = this.states[userGoalRule]
        val currentClosure = this.closures[userGoalRule]
        val fkey = StateKey(rulePosition, graftLookahead)
        val stateSet = currentStates[fkey]
        val alreadyDone = stateSet.any { rps -> rps.ancestorRPs == ancestorRPs }
        if (alreadyDone.not()) {
            //TODO: can we create the closures reachable by transition on-demand rather than up front?
            //Do all rulePositions from here, rather than just the nextRulePosition, otherwise we can't keep the ancestorRPs
            val firstClosures = rulePosition.runtimeRule.rulePositions.map { rp ->
                val closureNumber = this.calcNextClosureNumber(userGoalRule)
                val rps = this.createRulePositionState(userGoalRule, closureNumber, rp, ancestorRPs, heightLookahead, graftLookahead)
                val key = StateKey(rp, graftLookahead)
                stateSet.add(rps)
                val closure = this.createClosure(userGoalRule, closureNumber, rps)
                currentClosure[closureNumber] = closure
                closure
            }.toSet()

            firstClosures.transitveClosure { closure ->
                closure.content.flatMap { rps ->
                    //Do all rulePositions from here, rather than just the nextRulePosition, otherwise we can't keep the ancestorRPs
                    val rpsToDo = rps.runtimeRule.rulePositions.filter { it.isAtStart.not() && it.runtimeRule.isNonTerminal }
                    rpsToDo.mapNotNull { rp ->
                        val skey = StateKey(rp, rps.graftLookahead)
                        if (currentStates.containsKey(skey)) {
                            //no nothing, closure already created
                            null
                        } else {
                            val closureNumber = this.calcNextClosureNumber(userGoalRule)
                            val hlh = this.calcHeightLookahead(rps.ancestorRPs, rp, rps.graftLookahead)
                            val glh = rps.graftLookahead//this.calcGraftLookahead(rps.rulePosition, rp.runtimeRule, rps.graftLookahead)
                            val newRps = this.createRulePositionState(userGoalRule, closureNumber, rp, rps.ancestorRPs, hlh, glh) //TODO: lhs are wrong here I think!
                            val key = StateKey(rp, glh)
                            currentStates[key].add(newRps)
                            val newClosure = this.createClosure(userGoalRule, closureNumber, newRps)
                            currentClosure[closureNumber] = newClosure
                            newClosure
                        }
                    }
                }.toSet()
            }

        }
        return currentStates[fkey] ?: throw Exception("Should never be null")
    }

    /**
     * itemRule is the rule we use to increment rp
     */
    fun nextRulePosition(rp: RulePosition, itemRule: RuntimeRule): Set<RulePosition> { //TODO: cache this
        return if (RulePosition.END_OF_RULE == rp.position) {
            emptySet() //TODO: use goal rule to find next position? maybe
        } else {
            when (rp.runtimeRule.rhs.kind) {
                RuntimeRuleItemKind.EMPTY -> throw ParseException("This should never happen!")
                RuntimeRuleItemKind.CHOICE_EQUAL -> when {
                    itemRule == rp.runtimeRule.rhs.items[rp.choice] -> setOf(RulePosition(rp.runtimeRule, rp.choice, RulePosition.END_OF_RULE))
                    else -> emptySet() //throw ParseException("This should never happen!")
                }
                RuntimeRuleItemKind.CHOICE_PRIORITY -> when {
                    itemRule == rp.runtimeRule.rhs.items[rp.choice] -> setOf(RulePosition(rp.runtimeRule, rp.choice, RulePosition.END_OF_RULE))
                    else -> emptySet() //throw ParseException("This should never happen!")
                }
                RuntimeRuleItemKind.CONCATENATION -> { //TODO: check itemRule?
                    val np = rp.position + 1
                    if (np < rp.runtimeRule.rhs.items.size) {
                        setOf(RulePosition(rp.runtimeRule, 0, np))
                    } else {
                        setOf(RulePosition(rp.runtimeRule, 0, RulePosition.END_OF_RULE))
                    }
                }
                RuntimeRuleItemKind.MULTI -> when (rp.choice) {
                    RuntimeRuleItem.MULTI__EMPTY_RULE -> when {
                        0 == rp.position && rp.runtimeRule.rhs.multiMin == 0 && itemRule == rp.runtimeRule.rhs.MULTI__emptyRule -> setOf(
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.MULTI__EMPTY_RULE, RulePosition.END_OF_RULE)
                        )
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
                    RuntimeRuleItem.MULTI__ITEM -> when {
                        itemRule == rp.runtimeRule.rhs.MULTI__repeatedItem -> setOf(
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.MULTI__ITEM, 1),
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.MULTI__ITEM, RulePosition.END_OF_RULE)
                        )
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
                    else -> throw ParseException("This should never happen!")
                }
                RuntimeRuleItemKind.SEPARATED_LIST -> when (rp.choice) {
                    RuntimeRuleItem.SLIST__EMPTY_RULE -> when {
                        0 == rp.position && rp.runtimeRule.rhs.multiMin == 0 && itemRule == rp.runtimeRule.rhs.SLIST__emptyRule -> setOf(
                            RulePosition(rp.runtimeRule, rp.choice, RulePosition.END_OF_RULE)
                        )
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
                    RuntimeRuleItem.SLIST__ITEM -> when {
                        0 == rp.position && (rp.runtimeRule.rhs.multiMax == 1) && itemRule == rp.runtimeRule.rhs.SLIST__repeatedItem -> setOf(
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE)
                        )
                        0 == rp.position && (rp.runtimeRule.rhs.multiMax > 1 || -1 == rp.runtimeRule.rhs.multiMax) && itemRule == rp.runtimeRule.rhs.SLIST__repeatedItem -> setOf(
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__SEPARATOR, 1),
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE)
                        )
                        2 == rp.position && (rp.runtimeRule.rhs.multiMax > 1 || -1 == rp.runtimeRule.rhs.multiMax) && itemRule == rp.runtimeRule.rhs.SLIST__repeatedItem -> setOf(
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__SEPARATOR, 1),
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE)
                        )
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
                    RuntimeRuleItem.SLIST__SEPARATOR -> when {
                        1 == rp.position && (rp.runtimeRule.rhs.multiMax > 1 || -1 == rp.runtimeRule.rhs.multiMax) && itemRule == rp.runtimeRule.rhs.SLIST__separator -> setOf(
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__ITEM, 2)
                        )
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
                    else -> throw ParseException("This should never happen!")
                }
                RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST -> throw ParseException("Not yet supported")
                RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST -> throw ParseException("Not yet supported")
                RuntimeRuleItemKind.UNORDERED -> throw ParseException("Not yet supported")
            }
        }
    }

    private fun createWidthTransition(from: RulePositionState, closureRPS: RulePositionState): Set<Transition> {
        val action = Transition.ParseAction.WIDTH
        val item = closureRPS.runtimeRule
        val lookaheadGuard = closureRPS.graftLookahead
        val to = closureRPS
        return setOf(Transition(from, to, action, item, lookaheadGuard))
    }

    private fun createHeightTransition(userGoalRule: RuntimeRule, from: RulePositionState, closureRPS: RulePositionState): Set<Transition> {
        val parentRP = closureRPS.directParent ?: throw ParseException("Should never be null")
        val action = Transition.ParseAction.HEIGHT
        val item = closureRPS.runtimeRule
        val nextRPs = this.nextRulePosition(parentRP, item)
        return nextRPs.flatMap { nextRP ->
            val glh = this.calcGraftLookahead(closureRPS.ancestorRPs.dropLast(1), nextRP)
            val toParent = from.rulePosition //closureRPS.parentAncestors.lastOrNull()
            val toSet = this.fetchRulePositionStates(userGoalRule, nextRP, glh)
            val lookaheadGuard = this.calcHeightLookahead(from.ancestorRPs, from.rulePosition) //from.heightLookahead
            toSet.map { to -> Transition(from, to, action, item, lookaheadGuard) }
        }.toSet()
    }

    private fun createGraftTransition(userGoalRule: RuntimeRule, from: RulePositionState, closureRPS: RulePositionState): Set<Transition> {
        val parentRP = closureRPS.directParent ?: throw ParseException("Should never be null")
        val action = Transition.ParseAction.GRAFT
        val item = closureRPS.runtimeRule
        val nextRPs = this.nextRulePosition(parentRP, item)
        return nextRPs.flatMap { nextRP ->
            val toSet = this.fetchRulePositionStates(userGoalRule, nextRP, closureRPS.graftLookahead)
            val lookaheadGuard = from.graftLookahead
            toSet.map { to -> Transition(from, to, action, item, lookaheadGuard) }
        }.toSet()
    }

    private fun calcTransitions2(userGoalRule: RuntimeRule, from: RulePositionState, prevState: RulePositionState?): Set<Transition> {
        val closure = this.fetchOrCreateClosure(userGoalRule, from)
        val transitions = mutableSetOf<Transition>()

        for (closureRPS in closure.content) {
            val parentRP = closureRPS.directParent
            val width = closureRPS.runtimeRule.isTerminal && from.isAtEnd.not()
            val height = closureRPS.ancestorRPs.contains(from.directParent) && from.isAtEnd && closureRPS.isAtEnd && (parentRP?.isAtStart
                ?: false)
            val graft = closureRPS.ancestorRPs.contains(from.directParent) && from.isAtEnd && closureRPS.isAtEnd && (parentRP?.isAtStart?.not()
                ?: false)

            if (width) {
                transitions += this.createWidthTransition(from, closureRPS)
            }
            if (height) {
                transitions += this.createHeightTransition(userGoalRule, from, closureRPS)
            }
            if (graft) {
                transitions += this.createGraftTransition(userGoalRule, from, closureRPS)
            }
        }

        return transitions
    }

    private fun calcTransitions(userGoalRule: RuntimeRule, from: RulePositionState, prevState: RulePositionState?): Set<Transition> {
        val prevRP = prevState?.rulePosition
        val closure = this.fetchOrCreateClosure(userGoalRule, from)

        val transitions: Set<Transition> = closure.content.flatMap { closureRPS ->
            val closureRP = closureRPS.rulePosition
            val parentRP = closureRPS.directParent
            //val parentClosure = if (null==parentRPS) emptySet<RulePositionState>() else setOf(closureRPS).transitveClosure { if (null==it.parent) emptySet() else setOf(it.parent) }
            val relevantW = closureRP.runtimeRule.isTerminal //closureRP==from.rulePosition || closureRPS.ancestorRPs.contains(from.rulePosition) //parentClosure.contains(from)
            val relevantHG = closureRPS.ancestorRPs.contains(from.directParent)
            when {
                (relevantW && from.isAtEnd.not()) -> {
                    val tr = this.createWidthTransition(from, closureRPS)
                    return tr
                }
                (null == parentRP) -> when {
                    (from.runtimeRule.isGoal && from.isAtEnd && closureRPS.isAtEnd) -> {
                        val action = Transition.ParseAction.GOAL
                        val to = from
                        setOf(Transition(from, to, action, RuntimeRuleSet.END_OF_TEXT, emptySet()))
                    }
                    else -> emptySet<Transition>()
                }
                //special case because we 'artificially' create first child of goal in ParseGraph.start
                // (because we want starting skip nodes (i.e. whitespace) to appear inside the userGoal node, rather than inside the top 'GOAL' node)
                (parentRP.runtimeRule.isGoal && relevantHG && from.isAtEnd && closureRPS.isAtEnd && parentRP.isAtStart) -> {
                    val action = Transition.ParseAction.GRAFT
                    val item = closureRP.runtimeRule
                    val nextRPs = this.nextRulePosition(parentRP, item)
                    nextRPs.flatMap { nextRP ->
                        val toSet = this.fetchRulePositionStates(userGoalRule, nextRP, closureRPS.graftLookahead)
                        val lookaheadGuard = from.graftLookahead
                        toSet.map { to -> Transition(from, to, action, item, lookaheadGuard) }
                    }
                }
                (relevantHG && from.isAtEnd && closureRPS.isAtEnd && parentRP.isAtStart) -> {
                    this.createHeightTransition(userGoalRule, from, closureRPS)
                }
                (relevantHG && from.isAtEnd && closureRPS.isAtEnd && parentRP.isAtStart.not()) -> {
                    this.createGraftTransition(userGoalRule, from, closureRPS)
                }
                else -> emptySet<Transition>()
            }
        }.toSet()
        return transitions
    }

    fun buildCaches() {
        this.allSkipRules.size
        this.allSkipTerminals.size
        this.isSkipTerminal.size
        this.terminalRules.size
        this.firstTerminals.size
        this.firstSkipRuleTerminalPositions.size
        this.firstSuperNonTerminal.size
        this.subNonTerminals.size
        this.subTerminals.size

        this.runtimeRules.forEach { rule ->
            this.growsInto[rule.number]
            rule.rulePositions.forEach { rp ->
                if (rp.isAtEnd) {
                    //no need to do for end-of-rule (at present)
                } else {
                    //TODO: this.lookahead[rp]?.get(rp)
                    this.firstTerminals2[rp]
                }
            }
        }
    }

    fun startingRulePositionState(userGoalRule: RuntimeRule): RulePositionState {
        val goalRule = RuntimeRuleSet.createGoal(userGoalRule)
        val goalRp = RulePosition(goalRule, 0, 0)
        val userGoalRP = userGoalRule.calcExpectedRulePositions(0).first()
        val eotLookahead = setOf(RuntimeRuleSet.END_OF_TEXT)
        val hlh = emptySet<RuntimeRule>()//this.calcHeightLookahead(goalRp, userGoalRP, eotLookahead)
        val glh = eotLookahead
        val startRPS = this.fetchOrCreateRulePositionStateAndItsClosure(userGoalRule, goalRp, emptyList(), hlh, glh)
        return startRPS.first()
    }

    fun transitions(userGoalRule: RuntimeRule, rps: RulePositionState, prevState: RulePositionState?): Set<Transition> {
        return this.calcTransitions2(userGoalRule, rps, prevState)
    }


    // ---

    /**
     * return the set of RulePositions that rule could grow into
     * the goal rule must be passed, as it is parse specific
     */
    fun growsInto(userGoalRule: RuntimeRule, startingAt: RulePositionState, lookingFor: RulePositionState): Set<RulePositionState> {
        val parents = calcParents(userGoalRule, startingAt, lookingFor)
        return parents.filter { it.second.runtimeRule == lookingFor.runtimeRule }.map { it.first }.toSet()
    }

    fun findRuntimeRule(ruleName: String): RuntimeRule {
        val number = this.nonTerminalRuleNumber[ruleName]
            ?: throw ParseException("NonTerminal RuntimeRule '${ruleName}' not found")
        return this.runtimeRules[number]
    }

    fun findTerminalRule(pattern: String): RuntimeRule {
        val number = this.terminalRuleNumber[pattern]
            ?: throw ParseException("Terminal RuntimeRule ${pattern} not found")
        return this.runtimeRules[number]
    }

    fun findNextExpectedItems(runtimeRule: RuntimeRule, nextItemIndex: Int): Set<RuntimeRule> {
        return runtimeRule.findNextExpectedItems(nextItemIndex)
    }

    fun findNextExpectedTerminals(runtimeRule: RuntimeRule, nextItemIndex: Int): Set<RuntimeRule> {
        val nextItems = this.findNextExpectedItems(runtimeRule, nextItemIndex)
        val result = mutableSetOf<RuntimeRule>()
        nextItems.forEach {
            result += this.firstTerminals[it.number]
            //result += this.calcFirstTerminals(it)
        }
        return result
    }

    fun possibleChildRulePositionStates(goalRule: RuntimeRule, current: RulePositionState, providedLookahead: Set<RuntimeRule>): Set<RulePositionState> {
        return current.items.flatMap { rule: RuntimeRule ->
            val rps: Set<RulePosition> = rule.calcExpectedRulePositions(0)
            rps.flatMap { rp ->
                val items = rp.items
                if (items.isEmpty()) {
                    setOf(fetchOrCreateRulePositionState(goalRule, rp, null, emptySet(), providedLookahead))
                } else {
                    rp.items.flatMap { r ->
                        nextRulePosition(rp, r)
                            .map {
                                val lh = this.firstTerminals2[it] ?: throw ParseException("should never happen")
                                fetchOrCreateRulePositionState(goalRule, rp, null, lh, providedLookahead)
                            }
                    }
                }
            }
        }.toSet()
    }

    fun currentPossibleRulePositionStates(goalRule: RuntimeRule, current: RulePositionState, previous: RulePositionState): Set<RulePositionState> {
        return setOf(current).transitveClosure { parentRPS ->
            parentRPS.items.flatMap { rule: RuntimeRule ->
                val rps: Set<RulePosition> = rule.calcExpectedRulePositions(0)
                rps.flatMap { rp ->
                    val items = rp.items
                    if (items.isEmpty()) {
                        setOf(fetchOrCreateRulePositionState(goalRule, rp, null, previous.heightLookahead, previous.graftLookahead))
                    } else {
                        rp.items.flatMap { r ->
                            nextRulePosition(rp, r)
                                .map {
                                    val lh = this.firstTerminals2[it] ?: throw ParseException("should never happen")
                                    if (lh.isEmpty()) {
                                        fetchOrCreateRulePositionState(goalRule, rp, null, previous.heightLookahead, previous.graftLookahead)
                                    } else {
                                        fetchOrCreateRulePositionState(goalRule, rp, null, lh, previous.graftLookahead)
                                    }
                                }
                        }
                    }
                }
            }.toSet()
        }
    }

    fun nextPossibleRulePositionStates(goalRule: RuntimeRule, current: RulePositionState, providedLookahead: Set<RuntimeRule>): Set<RulePositionState> {
/*
        val z = currentPossibleRulePositionStates(goalRule, current, providedLookahead)

        val nexts = z.flatMap { rps ->
            rps.items.flatMap { rr ->
                val nextRPs = nextRulePosition(rps.rulePosition, rr)
                nextRPs.map { nextRP ->
                    fetchOrCreateRulePositionState(goalRule, nextRP, rps.heightLookahead, rps.graftLookahead) //TODO: not sure lh is correct here!
                }
            }
        }.toSet()
*/


        val z = setOf(current)//currentPossibleRulePositionStates(goalRule, current, providedLookahead)

        val nexts = z.flatMap { rps ->
            rps.items.flatMap { rr ->
                val nextRPs = nextRulePosition(rps.rulePosition, rr)
                nextRPs.flatMap { nextRP ->
                    val nextNextItems = nextRP.items
                    val hlh = rps.heightLookahead //if (nextRP.isAtStart) rps.heightLookahead else rps.graftLookahead
                    if (nextNextItems.isEmpty()) {
                        setOf(fetchOrCreateRulePositionState(goalRule, nextRP, null, hlh, rps.graftLookahead)) //TODO: not sure lh is correct here!
                    } else {
                        val nextNext: Set<RulePosition> = nextNextItems.flatMap { nextItem -> nextRulePosition(nextRP, nextItem) }.toSet()
                        nextNext.map { nn ->
                            val lh: Set<RuntimeRule> = this.firstTerminals2[nn]
                                ?: throw ParseException("should never happen")
                            if (lh.isEmpty()) {
                                fetchOrCreateRulePositionState(goalRule, nextRP, null, hlh, rps.graftLookahead)
                            } else {
                                fetchOrCreateRulePositionState(goalRule, nextRP, null, hlh, lh) //TODO: not sure lh is correct here!
                            }
                        }.toSet()
                    }
                }
            }
        }.toSet()

        return nexts
    }


    /**
     * return Pair(parent,child)
     */
    private fun calcParents(userGoalRule: RuntimeRule, startingAt: RulePositionState, current: RulePositionState): Set<Pair<RulePositionState, RulePositionState>> {

        val firstParentRP = startingAt.rulePosition
        val rps: Set<RulePosition> = firstParentRP.items.flatMap {
            it.calcExpectedRulePositions(0)
        }.toSet()
        val rpsStates: Set<RulePositionState> = rps.flatMap { childRP ->
            val childAtEndRP = RulePosition(childRP.runtimeRule, childRP.choice, RulePosition.END_OF_RULE)
            if (childAtEndRP.runtimeRule.isTerminal) {
                setOf(fetchOrCreateRulePositionState(userGoalRule, childAtEndRP, null, startingAt.heightLookahead, startingAt.graftLookahead))
            } else {
                childRP.items.map { rr ->
                    val lh = calcHeightLookahead(startingAt.ancestorRPs, childRP, startingAt.heightLookahead)
                    fetchOrCreateRulePositionState(userGoalRule, childAtEndRP, null, lh, startingAt.graftLookahead)
                }
            }
        }.toSet()
        val start = rpsStates.map { Pair(startingAt, it) }.toSet()
        val parents = start.transitveClosure { parentChild ->
            val oldChildRP = parentChild.second.rulePosition
            val oldChildRPS = parentChild.second
            val newParentRP = RulePosition(oldChildRP.runtimeRule, oldChildRP.choice, 0) // could be a problem with multi/slist!
            val newChildRPs = newParentRP.items.flatMap {
                it.calcExpectedRulePositions(0)
            } //filter out if not at start!
            val newChildStates: Set<RulePositionState> = newChildRPs.flatMap { childRP ->
                val childAtEndRP = RulePosition(childRP.runtimeRule, childRP.choice, RulePosition.END_OF_RULE)
                if (childAtEndRP.runtimeRule.isTerminal) {
                    setOf(fetchOrCreateRulePositionState(userGoalRule, childAtEndRP, null, oldChildRPS.heightLookahead, startingAt.graftLookahead))
                } else {
                    childRP.items.map { rr ->
                        val lh = calcHeightLookahead(oldChildRPS.ancestorRPs, childRP, oldChildRPS.heightLookahead)
                        fetchOrCreateRulePositionState(userGoalRule, childAtEndRP, null, lh, startingAt.graftLookahead)
                    }
                }
            }.toSet()
            val newParentRPS = fetchOrCreateRulePositionState(userGoalRule, newParentRP, null, oldChildRPS.heightLookahead, oldChildRPS.graftLookahead)
            newChildStates.map { newChild -> Pair(newParentRPS, newChild) }.toSet()
        }
        return parents
    }

    private fun calcLookahead1(startingAt: Pair<RulePosition, Set<RuntimeRule>>, lookingFor: RulePosition): Set<RuntimeRule> {
        //startingFrom the tgtRPs, find the rp calculating lh as we go
        val start = setOf(startingAt)
        val closure = setOf(start).transitveClosure { seq ->
            //in kotlin setOf and toSet give a LinkedSet which is ordered
            val last = seq.last()
            if (last.first == lookingFor) {
                setOf(seq)
            } else {
                val x = last.first.items.flatMap { rule ->
                    rule.rulePositions
                }
                val y = x.flatMap { rp: RulePosition ->
                    rp.items.flatMap { r -> nextRulePosition(rp, r).map { Pair(rp, it) } }
                }
                val z = y.map { p: Pair<RulePosition, RulePosition> ->
                    val rplh = this.firstTerminals2[p.second] ?: throw ParseException("should never happen")
                    if (rplh.isEmpty()) {
                        seq + Pair(p.first, last.second)
                    } else {
                        seq + Pair(p.first, rplh)
                    }
                }.toSet()
                z
            }
        }

        val lhs = closure.filter { it.last().first == lookingFor }
        val lh: Set<RuntimeRule> = when (lhs.size) {
            0 -> emptySet()
            1 -> lhs.first().last().second
            else -> lhs.flatMap { it.last().second }.toSet()
        }
        return lh
    }

    private fun calcLookahead2(startingAt: Pair<RulePosition, Set<RuntimeRule>>, lookingFor: RulePosition): Set<RuntimeRule> {
        //startingFrom the tgtRPs, find the rp calculating lh as we go
        val closure = setOf(startingAt).transitveClosure {
            it.first.items.flatMap { it2 ->
                //nextRulePosition(it.first, it2).toSet()
                it2.rulePositions
            }.map { rp ->
                val x = rp.items.flatMap { rr -> rr.rulePositions.flatMap { rp2 -> nextRulePosition(rp2, rr) } }
                val y: Set<RuntimeRule> = x.flatMap { firstTerminals2[it] ?: emptySet() }.toSet()
                val rplh = y//this.firstTerminals2[rp] ?: throw ParseException("should never happen")
                if (rplh.isEmpty()) {
                    Pair(rp, it.second)
                } else {
                    Pair(rp, rplh)
                }
            }.toSet()
        }

        val lhs = closure.filter { it.first == lookingFor }
        val lh: Set<RuntimeRule> = when (lhs.size) {
            0 -> emptySet()
            1 -> lhs.first().second
            else -> {
                TODO();
            }
        }
        return lh
    }

    private fun calcExpectedItemRulePositions(rp: RulePosition): Set<RulePosition> {
        return rp.runtimeRule.calcExpectedRulePositions(rp.position)
    }

    private fun calcExpectedItemRulePositionTransitive(rp: RulePosition): Set<RulePosition> {
        var s = setOf(rp)//rp.runtimeRule.calcExpectedRulePositions(rp.position)

        return s.transitveClosure { rp ->
            if (RulePosition.END_OF_RULE == rp.position) {
                emptySet()
            } else {
                if (rp.runtimeRule.isTerminal) {
                    emptySet<RulePosition>()
                } else {
                    rp.runtimeRule.items(rp.choice, rp.position).flatMap {
                        if (it.isTerminal) {
                            setOf(rp)
                        } else {
                            it.calcExpectedRulePositions(0)
                        }
                    }
                }
            }.toSet()
        }
    }

    private fun calcExpectedTerminalRulePositions(rp: RulePosition): Set<RulePosition> {
        val nextItems = this.calcExpectedItemRulePositionTransitive(rp)
        return nextItems.filter {
            if (it.runtimeRule.isTerminal) { //should never happen!
                false
            } else {
                if (RulePosition.END_OF_RULE == it.position) {
                    false
                } else {
                    it.items.any { it.isTerminal }
                }
            }
        }.toSet() //TODO: cache ?
    }

    private fun calcExpectedSkipItemRulePositionTransitive(): Set<RulePosition> {
        val skipRuleStarts = allSkipRules.map {
            val x = firstTerminals[it.number]
            RulePosition(it, 0, 0)
        }
        return skipRuleStarts.flatMap {
            this.calcExpectedItemRulePositionTransitive(it)
        }.toSet()
    }

    private fun calcFirstTerminalSkipRulePositions(): Set<RulePosition> {
        val skipRPs = calcExpectedSkipItemRulePositionTransitive()
        return skipRPs.filter {
            it.runtimeRule.itemsAt[it.position].any { it.isTerminal }
        }.toSet() //TODO: cache ?
    }

    private fun calcGrowsInto(ruleNumber: Int): Set<RulePosition> {
        val rule = this.runtimeRules[ruleNumber]
        return this.runtimeRules.filter {
            it.isNonTerminal && it.rhs.items.contains(rule)
        }.flatMap {
            when (it.rhs.kind) {
                RuntimeRuleItemKind.EMPTY -> emptySet<RulePosition>()
                RuntimeRuleItemKind.CONCATENATION -> it.rhs.items.mapIndexedNotNull { index, item ->
                    if (item == rule) {
                        RulePosition(it, 0, index)
                    } else {
                        null
                    }
                }
                RuntimeRuleItemKind.CHOICE_EQUAL -> setOf(RulePosition(it, it.rhs.items.indexOf(rule), 0))
                RuntimeRuleItemKind.CHOICE_PRIORITY -> setOf(RulePosition(it, it.rhs.items.indexOf(rule), 0))
                RuntimeRuleItemKind.UNORDERED -> TODO()
                RuntimeRuleItemKind.MULTI -> setOf(RulePosition(it, RuntimeRuleItem.MULTI__ITEM, 0)) //do we also need empty and/or position 1?
                RuntimeRuleItemKind.SEPARATED_LIST -> it.rhs.items.mapIndexedNotNull { index, item ->
                    if (item == rule) {
                        when (index) {
                            RuntimeRuleItem.SLIST__ITEM -> RulePosition(it, index, 0) //TODO: might this also be pos=2?
                            RuntimeRuleItem.SLIST__SEPARATOR -> RulePosition(it, index, 1)
                            RuntimeRuleItem.SLIST__EMPTY_RULE -> RulePosition(it, index, 0)
                            else -> throw ParseException("Should never happen")
                        }
                    } else {
                        null
                    }
                }
                RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
                RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST -> TODO()
            }
        }.toSet()
    }

    private fun calcFirstSubRules(runtimeRule: RuntimeRule): Set<RuntimeRule> {
        return runtimeRule.findSubRulesAt(0)
    }

    private fun calcFirstTerminals(runtimeRule: RuntimeRule): Set<RuntimeRule> {
        var rr = runtimeRule.findTerminalAt(0)
        for (r in this.calcFirstSubRules(runtimeRule)) {
            rr += r.findTerminalAt(0)
        }
        return rr
    }

    private fun calcIsSkipTerminal(rr: RuntimeRule): Boolean {
        val b = this.allSkipTerminals.contains(rr)
        return b
    }

    private fun calcFirstSuperNonTerminal(runtimeRule: RuntimeRule): List<RuntimeRule> {
        return this.runtimeRules.filter {
            it.isNonTerminal && it.couldHaveChild(runtimeRule, 0)
        } + if (runtimeRule.isEmptyRule) listOf(runtimeRule.ruleThatIsEmpty) else emptyList()
    }

    //TODO: should be private and only a cache is public
    fun calcCanGrowInto(childRule: RuntimeRule, ancesstorRule: RuntimeRule, ancesstorItemIndex: Int): Boolean {
        return if (-1 == ancesstorItemIndex) {
            false
        } else {
            //return canGrowIntoAt_cache[childRule.number][ancesstorRule.number][ancesstorItemIndex];
            val index = IndexCanGrowIntoAt(childRule.number, ancesstorRule.number, ancesstorItemIndex)
            var result = canGrowIntoAt_cache[index]
            if (null == result) {
                //TODO: try using RulePositions to do this calculation
                val nextExpectedForStacked = this.findNextExpectedItems(ancesstorRule, ancesstorItemIndex)
                if (nextExpectedForStacked.contains(childRule)) {
                    result = true
                } else {
                    result = false
                    for (rr in nextExpectedForStacked) {
                        if (rr.isNonTerminal) {
                            // todo..can we reduce the possibles!
                            val possibles = this.calcFirstSubRules(rr)
                            if (possibles.contains(childRule)) {
                                result = true
                                break
                            }
                        } else {
                            val possibles = this.firstTerminals[rr.number]
                            if (possibles.contains(childRule)) {
                                result = true
                                break
                            }
                        }
                    }
                }
                canGrowIntoAt_cache[index] = result ?: throw ParseException("Should never happen")
            }
            return result
        }
    }

    override fun toString(): String {
        val rulesStr = this.runtimeRules.map {
            "  " + it.toString()
        }.joinToString("\n")
        return """
RuntimeRuleSet {
${rulesStr}
}
        """.trimIndent()
    }
}