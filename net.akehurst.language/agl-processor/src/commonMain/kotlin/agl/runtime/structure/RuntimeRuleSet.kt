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

        fun createGoalRule(userGoalRule: RuntimeRule): RuntimeRule {
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

    val expectedTerminalRulePositions = lazyMap<RulePosition, Array<RulePosition>> {
        calcExpectedTerminalRulePositions(it).toTypedArray()
    }

    val firstTerminals2 = lazyMap<RulePosition, Set<RuntimeRule>> {
        val trps = expectedTerminalRulePositions[it] ?: arrayOf()
        trps.flatMap { it.items }.toSet()
    }

    /** Map of userGoalRule -> next Closure number **/
    private val nextClosure = mutableMapOf<RuntimeRule, ClosureNumber>()

    /** Map of goalRule -> (Map of ClosureNumber -> closure of RulePositionClosure)  **/
    private val closures = lazyMapNonNull<RuntimeRule, MutableMap<ClosureNumber, RulePositionClosure>> { userGoalRule ->
        mutableMapOf()
    }

    /** Map of userGoalRule -> next State number **/
    private val nextState = mutableMapOf<RuntimeRule, StateNumber>()

    private val state_cache = lazyMapNonNull<RuntimeRule, ParserStateSet> {
        ParserStateSet()
    }
    private val skipPaths = ParserStateSet()

    private val transitions_cache = lazyMapNonNull<RuntimeRule, MutableMap<ParserState, Set<Transition>>> { userGoalRule ->
        mutableMapOf()
    }
    private val skipTransitions_cache = lazyMapNonNull<RuntimeRule, MutableMap<ParserState, Set<Transition>>> { userGoalRule ->
        mutableMapOf()
    }

    init {
        for (rr in rules) {
            if (rr.isNonTerminal) {
                this.nonTerminalRuleNumber[rr.name] = rr.number
            } else {
                this.terminalRuleNumber[rr.name] = rr.number
            }
        }
    }

    private fun calcNextStateNumber(userGoalRule: RuntimeRule): StateNumber {
        val stateNumberValue: StateNumber = this.nextState[userGoalRule] ?: StateNumber(0)
        this.nextState[userGoalRule] = StateNumber(stateNumberValue.value + 1)
        return stateNumberValue
    }

    fun createNextStates(state: ParserState): Set<ParserState> {
        if (state.rulePosition.runtimeRule.isGoal) {
            // parent will be null
            return state.rulePosition.rulePosition.next().map { nextRP ->
                val childRPS = RulePositionWithLookahead(nextRP, emptySet())
                state.stateMap.fetchOrCreateParseState(childRPS, null)
            }.toSet()
        } else {
            val parentRPwl = state.directParent?.rulePosition ?: throw ParseException("should never be null")
            val possibleParents = state.stateMap.fetchAll(parentRPwl.rulePosition)
            return state.rulePosition.rulePosition.next().flatMap { nextRP ->
                possibleParents.map { posParentState ->
                    val posParentRPwl = posParentState.rulePosition
                    val posParentLH = posParentRPwl.graftLookahead
                    val lh = this.calcLookahead(posParentRPwl, nextRP, posParentLH)
                    val childRPS = RulePositionWithLookahead(nextRP, lh)
                    state.stateMap.fetchOrCreateParseState(childRPS, posParentState)
                }
            }.toSet()
        }
    }

    private fun createAllRulePositionPaths(userGoalRule: RuntimeRule, goalRP: RulePositionWithLookahead) {
        val stateMap = this.state_cache[userGoalRule]
        val startSet = goalRP.runtimeRule.rulePositions.map { rp ->
            val rps = RulePositionWithLookahead(rp, emptySet())
            val rpp = stateMap.fetchOrCreateParseState(rps, null)
            rpp
        }.toSet()
        startSet.transitveClosure { parent ->
            val parentRP = parent.rulePosition
            parentRP.items.flatMap { rr ->
                rr.rulePositions.mapNotNull { childRP ->
                    val lh = this.calcLookahead(parentRP, childRP, parentRP.graftLookahead)
                    val nlh = this.calcNextLookahead(parentRP, childRP, parentRP.graftLookahead)
                    val childRPS = RulePositionWithLookahead(childRP, lh)
                    val rpp = stateMap.fetchOrCreateParseState(childRPS, parent)
                    rpp
                }
            }.toSet()
        }
    }

    private fun createAllSkipRulePositionPaths(userGoalRule: RuntimeRule) {
        val stateMap = this.skipPaths
        val startSet = this.allSkipRules.flatMap { skipRule ->
            skipRule.rulePositions.map { rp ->
                val rps = RulePositionWithLookahead(rp, emptySet())
                val rpp = stateMap.fetchOrCreateParseState(rps, null)
                //this.skipPaths.add(rpp)
                rpp
            }
        }.toSet()
        val states = startSet.transitveClosure { parent ->
            val parentRP = parent.rulePosition
            //val ancestors = parent.ancestorRPs + parentRP
            parentRP.items.flatMap { rr ->
                rr.rulePositions.mapNotNull { childRP ->
                    val lh = this.calcLookahead(parentRP, childRP, parentRP.graftLookahead)
                    val nlh = this.calcNextLookahead(parentRP, childRP, parentRP.graftLookahead)
                    val childRPS = RulePositionWithLookahead(childRP, lh)
                    val rpp = stateMap.fetchOrCreateParseState(childRPS, parent)
                    rpp
                }
            }.toSet()
        }
    }

    private fun calcNextClosureNumber(userGoalRule: RuntimeRule): ClosureNumber {
        val num: ClosureNumber = this.nextClosure[userGoalRule] ?: ClosureNumber(0)
        this.nextClosure[userGoalRule] = ClosureNumber(num.value + 1)
        return num
    }

    private fun fetchRulePositionPaths(pathSet: Set<ParserState>, rp: RulePosition, lh: Set<RuntimeRule>): Set<ParserState> {
        val paths = pathSet.filter {
            it.rulePosition.rulePosition == rp && it.rulePosition.graftLookahead == lh
        }.toSet()
        return if (paths.size > 0) {
            paths
        } else {
            throw Exception("Should never be happen, states shaould already be created")
        }
    }

    private fun fetchState(states: MutableMap<RulePositionWithLookahead, ParserState>, rp: RulePosition, lh: Set<RuntimeRule>): Set<ParserState> {
        val paths = states.values.filter {
            it.rulePosition.rulePosition == rp && lh.containsAll(it.rulePosition.graftLookahead)
        }.toSet()
        return if (paths.size > 0) {
            paths
        } else {
            throw Exception("Should never be happen, states shaould already be created")
        }
    }

    private fun fetchRulePositionPaths(pathSet: Collection<ParserState>, rulePosition: RulePosition): Set<ParserState> {
        val paths = pathSet.filter {
            it.rulePosition.rulePosition == rulePosition
        }.toSet()
        return if (paths.size > 0) {
            paths
        } else {
            throw Exception("Should never be happen, states shaould already be created")
        }
    }


    fun fetchSkipStates(rulePosition: RulePosition): Set<ParserState> {
        return this.skipPaths.fetchAll(rulePosition)
    }

    private fun calcNextLookahead(parent: RulePositionWithLookahead?, childRP: RulePosition, ifEmpty: Set<RuntimeRule>): Set<RuntimeRule> {
        return if (childRP.isAtEnd) {
            // lh should be FIRST of next item in parent, so it can be tested against this childRP
            if (null == parent) {
                ifEmpty
            } else {
                if (parent.isAtEnd) {
                    parent.graftLookahead
                } else {
                    val nextRPs = nextRulePosition(parent.rulePosition, childRP.runtimeRule)
                    nextRPs.flatMap { nextRP ->
                        if (nextRP.isAtEnd) {
                            calcLookahead(null, parent.rulePosition, parent.graftLookahead)
                        } else {
                            val lh: Set<RuntimeRule> = this.firstTerminals2[nextRP] ?: throw ParseException("should never happen")
                            if (lh.isEmpty()) {
                                throw ParseException("should never happen")
                            } else {
                                lh
                            }
                        }
                    }.toSet()
                }
            }
        } else {
            //this childRP will not itself be applied to Height or GRAFT,
            // however it should carry the FIRST of next in the child,
            // so that this childs children can use it if needed
            childRP.items.flatMap { fstChildItem ->
                val nextRPs = nextRulePosition(childRP, fstChildItem)
                nextRPs.flatMap { nextChildRP ->
                    if (nextChildRP.isAtEnd) {
                        if (null == parent) {
                            ifEmpty
                        } else {
                            calcLookahead(null, parent.rulePosition, parent.graftLookahead)
                        }
                    } else {

                        nextChildRP.items.flatMap { sndChildItem ->
                            val nextNextRPs = nextRulePosition(nextChildRP, sndChildItem)
                            nextNextRPs.flatMap { nextNextChildRP ->
                                if (nextNextChildRP.isAtEnd) {
                                    if (null == parent) {
                                        ifEmpty
                                    } else {
                                        calcLookahead(null, parent.rulePosition, parent.graftLookahead)
                                    }
                                } else {
                                    val lh: Set<RuntimeRule> = this.firstTerminals2[nextNextChildRP] ?: throw ParseException("should never happen")
                                    if (lh.isEmpty()) {
                                        throw ParseException("should never happen")
                                    } else {
                                        lh
                                    }
                                }
                            }
                        }
                    }
                }
            }.toSet()
        }
    }

    private fun calcLookahead(parent: RulePositionWithLookahead?, childRP: RulePosition, ifEmpty: Set<RuntimeRule>): Set<RuntimeRule> {
        return if (childRP.isAtEnd) {
            // lh should be FIRST of next item in parent, so it can be tested against this childRP
            if (null == parent) {
                ifEmpty
            } else {
                if (parent.isAtEnd) {
                    parent.graftLookahead
                } else {
                    val nextRPs = nextRulePosition(parent.rulePosition, childRP.runtimeRule)
                    nextRPs.flatMap { nextRP ->
                        if (nextRP.isAtEnd) {
                            calcLookahead(null, parent.rulePosition, parent.graftLookahead)
                        } else {
                            val lh: Set<RuntimeRule> = this.firstTerminals2[nextRP] ?: throw ParseException("should never happen")
                            if (lh.isEmpty()) {
                                throw ParseException("should never happen")
                            } else {
                                lh
                            }
                        }
                    }.toSet()
                }
            }
        } else {
            //this childRP will not itself be applied to Height or GRAFT,
            // however it should carry the FIRST of next in the child,
            // so that this childs children can use it if needed
            childRP.items.flatMap { fstChildItem ->
                val nextRPs = nextRulePosition(childRP, fstChildItem)
                nextRPs.flatMap { nextChildRP ->
                    if (nextChildRP.isAtEnd) {
                        if (null == parent) {
                            ifEmpty
                        } else {
                            calcLookahead(null, parent.rulePosition, parent.graftLookahead)
                        }
                    } else {
                        val lh: Set<RuntimeRule> = this.firstTerminals2[nextChildRP] ?: throw ParseException("should never happen")
                        if (lh.isEmpty()) {
                            throw ParseException("should never happen")
                        } else {
                            lh
                        }
                    }
                }
            }.toSet()
        }
    }

    internal fun createClosure(userGoalRule: RuntimeRule, root: ParserState): RulePositionClosure {
        // create path from root down, it may include items from root up
        val states = root.stateMap
        val tempRoot = TemporaryParserState(emptyList(), root.rulePosition)
        val closureSet = setOf(tempRoot).transitveClosure { parentState ->
            val parentRP = parentState.rulePosition
            val ancestors = parentState.ancestors + parentRP
            parentRP.items.flatMap { rr ->
                val childrenRP = rr.calcExpectedRulePositions(0)
                childrenRP.mapNotNull { childRP ->
                    //val childAncestors = root.ancestorRPs + ancestorRPs
                    val lh = this.calcLookahead(parentRP, childRP, parentRP.graftLookahead)
                    val nlh = this.calcNextLookahead(parentRP, childRP, parentRP.graftLookahead)
                    val childRP = RulePositionWithLookahead(childRP, lh)
                    val childState = TemporaryParserState(ancestors, childRP)
                    childState
                }
            }.toSet()
        }

        val ancestorMap = mutableMapOf<RulePositionWithLookahead, List<ParserState>>()
        val rootAncestors = root.ancestors ?: emptyList()
        ancestorMap[tempRoot.rulePosition] = rootAncestors
        val extendedStates = mutableSetOf<ParserState>()
        for(tps in closureSet) {
            if (null==tps.directParent) {
                //must be tempRoot, and a state for this already exists, so do nothing
            } else {
                val parentAncestors = ancestorMap[tps.directParent] ?: throw ParseException("should never be null")
                val parent = states.fetch(tps.directParent, parentAncestors)
                val ps = states.fetchOrCreateParseState(tps.rulePosition, parent)
                ancestorMap[ps.rulePosition] = ps.ancestors
                extendedStates.add(ps)
            }
        }
        val closureNumber = this.calcNextClosureNumber(userGoalRule)
        return RulePositionClosure(closureNumber, root.rulePosition.rulePosition, extendedStates)
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
                    RuntimeRuleItem.MULTI__ITEM -> when { //TODO: reduce this set based on min/max
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

    private fun createWidthTransition(from: ParserState, closureState: ParserState): Set<Transition> {
        val action = Transition.ParseAction.WIDTH
        val item = closureState.runtimeRule
        var glh = closureState.rulePosition.graftLookahead
        val lookaheadGuard = glh
        val toSet = setOf(closureState) //this.fetchRulePositionPaths(pathSet, closureRPS.rulePosition.rulePosition, closureRPS.rulePosition.graftLookahead)
        return toSet.map { to ->
            //.filter { it.directParent == closureRPS.directParent }.map { to ->
            Transition(from, to, action, item, lookaheadGuard, null)
        }.toSet()
    }

    private fun createHeightTransition(from: ParserState, parentRP: RulePositionWithLookahead): Set<Transition> {
        //val parentRP = closureRPS.directParent ?: throw ParseException("Should never be null")
        val prevGuard = parentRP //for height, previous must not match prevGuard
        val action = Transition.ParseAction.HEIGHT
        val item = from.runtimeRule
        val lookaheadGuard = from.rulePosition.graftLookahead //this.calcHeightLookahead(closureRPS.ancestorRPs.map { it.rulePosition }, closureRPS.rulePosition.rulePosition) //from.heightLookahead
        val toSet = from.directParent?.next(this) ?: emptySet() //this.fetchState(states, nextRP, lh)
        return toSet.map { to ->
            Transition(from, to, action, item, lookaheadGuard, prevGuard)
        }.toSet()
    }

    private fun createGraftTransition(from: ParserState, parentRP: RulePositionWithLookahead): Set<Transition> {
        val prevGuard = parentRP //for graft, previous must match prevGuard
        val action = Transition.ParseAction.GRAFT
        val item = from.runtimeRule
        val lookaheadGuard = from.rulePosition.graftLookahead
        val toSet = from.directParent?.next(this) ?: emptySet()
        return toSet.map { to ->
            Transition(from, to, action, item, lookaheadGuard, prevGuard)
        }.toSet()

    }

    private fun calcTransitions(userGoalRule: RuntimeRule, from: ParserState): Set<Transition> {
        val transitions = mutableSetOf<Transition>()

        val goal = from.runtimeRule.isGoal && from.isAtEnd
        if (goal) {
            val action = Transition.ParseAction.GOAL
            val to = from
            transitions += Transition(from, to, action, RuntimeRuleSet.END_OF_TEXT, emptySet(), null)
        } else {
            if (from.isAtEnd) {
                val parentState = from.directParent
                if (null != parentState) {
                    val parentRP = parentState.rulePosition
                    if (parentState != null && parentState.rulePosition.runtimeRule.isGoal) {
                        transitions += this.createGraftTransition(from, parentRP)
                    } else {
                        val height = parentState.rulePosition.isAtStart
                        val graft = parentState.rulePosition.isAtStart.not()

                        if (height) {
                            transitions += this.createHeightTransition(from, parentRP)
                        }
                        if (graft) {
                            transitions += this.createGraftTransition(from, parentRP)
                        }
                    }
                }
            }
            if (from.isAtEnd.not()) {
                val closure = this.createClosure(userGoalRule, from)
                for (closureRPS in closure.content) {
                    val width = closureRPS.runtimeRule.isTerminal

                    // ?? do we need this case any more?
                    //special case because we 'artificially' create first child of goal in ParseGraph.start
                    // (because we want starting skip nodes (i.e. whitespace) to appear inside the userGoal node, rather than inside the top 'GOAL' node)
                    // val graftToGoal = parentRP.runtimeRule.isGoal && relevantHG && from.isAtEnd && closureRPS.isAtEnd && parentRP.isAtStart

                    if (width) {
                        transitions += this.createWidthTransition(from, closureRPS)
                    }
                }
            }
        }
        return transitions
    }

    /*
        private fun calcSkipTransitions(userGoalRule: RuntimeRule, from: ParserState): Set<Transition> {
            val pathSet = this.skipPaths
            val transitions = mutableSetOf<Transition>()
            //assume all closures are created already
            //val closures = this.fetchClosuresContaining(userGoalRule, from)

            val goal = from.runtimeRule.isGoal && from.isAtEnd
            if (goal) {
                val action = Transition.ParseAction.GOAL
                val to = from
                transitions += Transition(from, to, action, RuntimeRuleSet.END_OF_TEXT, emptySet(), null)
            } else {
    //TODO: only need to do either WIDTH or (HEIGHT or GRAFT) ! maybe?
                val path = from
                val parentRP = path.directParent
                if (null != parentRP) {
                    if (from.isAtEnd && parentRP != null && parentRP.rulePosition.runtimeRule.isGoal) {
                        transitions += this.createGraftTransition(pathSet, from, parentRP)
                    } else {
                        val height = from.isAtEnd && parentRP.isAtStart
                        val graft = from.isAtEnd && parentRP.isAtStart.not()

                        if (height) {
                            transitions += this.createHeightTransition(pathSet, from, parentRP)
                        }
                        if (graft) {
                            transitions += this.createGraftTransition(pathSet, from, parentRP)
                        }
                    }
                } else {
                    if (from.isAtEnd) {
                        val action = Transition.ParseAction.GOAL
                        val to = from
                        transitions += Transition(from, to, action, RuntimeRuleSet.END_OF_TEXT, emptySet(), null)
                    }
                }

                val closure = this.createClosure(userGoalRule, path)
                for (closureRPS in closure.content) {
                    val parentRP = closureRPS.directParent
                    val width = closureRPS.runtimeRule.isTerminal && from.isAtEnd.not()

                    // ?? do we need this case any more?
                    //special case because we 'artificially' create first child of goal in ParseGraph.start
                    // (because we want starting skip nodes (i.e. whitespace) to appear inside the userGoal node, rather than inside the top 'GOAL' node)
                    // val graftToGoal = parentRP.runtimeRule.isGoal && relevantHG && from.isAtEnd && closureRPS.isAtEnd && parentRP.isAtStart

                    if (width) {
                        transitions += this.createWidthTransition(pathSet, from, closureRPS)
                    }

                }
            }
            return transitions
        }
    */
    fun buildCaches() {

    }

    fun startingState(userGoalRule: RuntimeRule): ParserState {
        val goalRule = RuntimeRuleSet.createGoalRule(userGoalRule)
        val goalRp = RulePosition(goalRule, 0, 0)
        val goalRpLh = RulePositionWithLookahead(goalRp, emptySet())
        this.createAllSkipRulePositionPaths(userGoalRule)
        val states = this.state_cache[userGoalRule]
        val startState = states.fetchOrCreateParseState(goalRpLh, null)
        return startState
    }

    fun transitions(userGoalRule: RuntimeRule, from: ParserState): Set<Transition> {
        val transitions = this.transitions_cache[userGoalRule][from]
        if (null == transitions) {
            val t = this.calcTransitions(userGoalRule, from)
            this.transitions_cache[userGoalRule][from] = t
            return t
        } else {
            return transitions
        }
    }

    fun skipTransitions(userGoalRule: RuntimeRule, from: ParserState): Set<Transition> {
        val transitions = this.skipTransitions_cache[userGoalRule][from]
        if (null == transitions) {
            val states = this.skipPaths
            val t = this.calcTransitions(userGoalRule, from)
            this.skipTransitions_cache[userGoalRule][from] = t
            return t
        } else {
            return transitions
        }
    }

    // ---

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