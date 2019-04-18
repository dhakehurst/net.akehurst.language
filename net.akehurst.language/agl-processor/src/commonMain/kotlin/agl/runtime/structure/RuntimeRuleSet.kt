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

    private val paths = lazyMapNonNull<RuntimeRule, MutableSet<RulePositionPath>> {
        mutableSetOf()
    }
    private val skipPaths = mutableSetOf<RulePositionPath>()

    private val transitions_cache = lazyMapNonNull<RuntimeRule, MutableMap<RulePositionPath, Set<Transition>>> { userGoalRule ->
        mutableMapOf()
    }
    private val skipTransitions_cache = lazyMapNonNull<RuntimeRule, MutableMap<RulePositionPath, Set<Transition>>> { userGoalRule ->
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

    private fun createRulePositionPath(userGoalRule: RuntimeRule, rulePosition: RulePositionState, parent: RulePositionState?): RulePositionPath {
        val rps = RulePositionPath(parent, rulePosition)
        return rps
    }

    private fun createAllRulePositionPaths(userGoalRule: RuntimeRule, goalRP: RulePositionState): Set<RulePositionPath> {
        val currentPaths = this.paths[userGoalRule]
        val startSet = goalRP.runtimeRule.rulePositions.map { rp ->
            val rps = RulePositionState(rp, emptySet(), emptySet())
            val rpp = this.createRulePositionPath(userGoalRule, rps, null)
            rpp
        }.toSet()
        currentPaths.addAll(startSet)
        val states = startSet.transitveClosure { parent ->
            val parentRP = parent.rulePosition
            parentRP.items.flatMap { rr ->
                rr.rulePositions.mapNotNull { childRP ->
                    val lh = this.calcLookahead(parentRP, childRP, parentRP.graftLookahead)
                    val nlh = this.calcNextLookahead(parentRP, childRP, parentRP.graftLookahead)
                    val childRPS = RulePositionState(childRP, lh, nlh)
                    val rpp = this.createRulePositionPath(userGoalRule, childRPS, parentRP)
                    currentPaths.add(rpp)
                    rpp
                }
            }.toSet()
        }
        return states
    }

    private fun createAllSkipRulePositionPaths(userGoalRule: RuntimeRule): Set<RulePositionPath> {

        val startSet = this.allSkipRules.flatMap { skipRule ->
            skipRule.rulePositions.map { rp ->
                val rps = RulePositionState(rp, emptySet(), emptySet())
                val rpp = this.createRulePositionPath(userGoalRule, rps, null)
                //this.skipPaths.add(rpp)
                rpp
            }
        }.toSet()
        this.skipPaths.addAll(startSet)
        val states = startSet.transitveClosure { parent ->
            val parentRP = parent.rulePosition
            //val ancestors = parent.ancestorRPs + parentRP
            parentRP.items.flatMap { rr ->
                rr.rulePositions.mapNotNull { childRP ->
                    val lh = this.calcLookahead(parentRP, childRP, parentRP.graftLookahead)
                    val nlh = this.calcNextLookahead(parentRP, childRP, parentRP.graftLookahead)
                    val childRPS = RulePositionState(childRP, lh, nlh)
                    //if (ancestors.contains(childRPS)) {
                    //val rpp=this.createRulePositionPath(userGoalRule, childRP, ancestors)
                    //currentPaths.add(rpp)
                    //null
                    //} else {
                    val rpp = this.createRulePositionPath(userGoalRule, childRPS, parentRP)
                    //currentPaths.add(rpp)
                    rpp
                    //}
                }
            }.toSet()
        }
        this.skipPaths.addAll(states)
        return this.skipPaths
    }


    private fun calcNextClosureNumber(userGoalRule: RuntimeRule): ClosureNumber {
        val num: ClosureNumber = this.nextClosure[userGoalRule] ?: ClosureNumber(0)
        this.nextClosure[userGoalRule] = ClosureNumber(num.value + 1)
        return num
    }

    private fun fetchRulePositionPath(userGoalRule: RuntimeRule, rulePosition: RulePosition, parent: RulePositionState): RulePositionPath {
        val currentPaths = this.paths[userGoalRule]
        val paths = currentPaths.filter {
            it.rulePosition.rulePosition == rulePosition
                    && it.directParent == parent
        }
        return if (paths.size == 1) {
            paths[0]
        } else {
            throw Exception("Should never be happen, states shaould already be created")
        }
    }

    private fun fetchRulePositionPaths(pathSet: Set<RulePositionPath>, rp: RulePosition, lh: Set<RuntimeRule>): Set<RulePositionPath> {
        val paths = pathSet.filter {
            it.rulePosition.rulePosition == rp && it.rulePosition.graftLookahead == lh
        }.toSet()
        return if (paths.size > 0) {
            paths
        } else {
            throw Exception("Should never be happen, states shaould already be created")
        }
    }


    private fun fetchRulePositionPathsContaining(pathSet: Set<RulePositionPath>, rp: RulePosition, lh: Set<RuntimeRule>): Set<RulePositionPath> {
        val paths = pathSet.filter {
            it.rulePosition.rulePosition == rp && lh.containsAll(it.rulePosition.graftLookahead)
        }.toSet()
        return if (paths.size > 0) {
            paths
        } else {
            throw Exception("Should never be happen, states shaould already be created")
        }
    }

    private fun fetchRulePositionPaths(pathSet: Set<RulePositionPath>, rulePosition: RulePosition): Set<RulePositionPath> {
        val paths = pathSet.filter {
            it.rulePosition.rulePosition == rulePosition
        }.toSet()
        return if (paths.size > 0) {
            paths
        } else {
            throw Exception("Should never be happen, states shaould already be created")
        }
    }

    private fun fetchRulePositionPaths(userGoalRule: RuntimeRule, rulePosition: RulePositionState): Set<RulePositionPath> {
        val currentPaths = this.paths[userGoalRule]
        return this.fetchRulePositionPaths(currentPaths, rulePosition.rulePosition, rulePosition.graftLookahead)
    }

    private fun fetchRulePositionPaths(userGoalRule: RuntimeRule, rulePosition: RulePosition): Set<RulePositionPath> {
        val currentPaths = this.paths[userGoalRule]
        val paths = currentPaths.filter {
            it.rulePosition.rulePosition == rulePosition
        }.toSet()
        return if (paths.size > 0) {
            paths
        } else {
            throw Exception("Should never be happen, states shaould already be created")
        }
    }

    public fun fetchSkipRulePositionPaths(rulePosition: RulePosition): Set<RulePositionPath> {
        return this.fetchRulePositionPaths(this.skipPaths, rulePosition)
    }

    private fun calcNextLookahead(parent: RulePositionState?, childRP: RulePosition, ifEmpty: Set<RuntimeRule>): Set<RuntimeRule> {
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

    private fun calcLookahead(parent: RulePositionState?, childRP: RulePosition, ifEmpty: Set<RuntimeRule>): Set<RuntimeRule> {
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

    internal fun createClosure(userGoalRule: RuntimeRule, root: RulePositionPath): RulePositionClosure {
        // create path from root down, it may include items from root up
        val rootDown = RulePositionPathDown(root.directParent, root.rulePosition)
        val closureSet = setOf(rootDown).transitveClosure { parentRPP ->
            val parentRP = parentRPP.rulePosition
            parentRP.items.flatMap { rr ->
                val childrenRP = rr.calcExpectedRulePositions(0)
                childrenRP.mapNotNull { childRP ->
                    //val childAncestors = root.ancestorRPs + ancestorRPs
                    val lh = this.calcLookahead(parentRP, childRP, parentRP.graftLookahead)
                    val nlh = this.calcNextLookahead(parentRP, childRP, parentRP.graftLookahead)
                    val childRPS = RulePositionState(childRP, lh, nlh)
                    RulePositionPathDown(parentRP, childRPS)
                }
            }.toSet()
        }
        val closureNumber = this.calcNextClosureNumber(userGoalRule)
        return RulePositionClosure(closureNumber, root.rulePosition.rulePosition, closureSet)
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

    private fun createWidthTransition(pathSet: Set<RulePositionPath>, from: RulePositionPath, closureRPS: RulePositionPathDown): Set<Transition> {
        val action = Transition.ParseAction.WIDTH
        val item = closureRPS.runtimeRule
        var glh = closureRPS.rulePosition.graftLookahead //this.calcGraftLookahead(userGoalRule,  closureRPS.rulePosition)
        //var lookaheadGuard = this.calcGraftLookahead(closureRPS.ancestorRPs, closureRPS.rulePosition) //closureRPS.graftLookahead
        //if (lookaheadGuard.isEmpty()) lookaheadGuard = from.graftLookahead
        val lookaheadGuard = glh
        val toSet = this.fetchRulePositionPaths(pathSet, closureRPS.rulePosition.rulePosition, closureRPS.rulePosition.graftLookahead)
        return toSet.filter { it.directParent == closureRPS.directParent }.map { to ->
            Transition(from, to, action, item, lookaheadGuard, null)
        }.toSet()
    }

    private fun createHeightTransition(pathSet: Set<RulePositionPath>, from: RulePositionPath, parentRP: RulePositionState): Set<Transition> {
        //val parentRP = closureRPS.directParent ?: throw ParseException("Should never be null")
        val prevGuard = parentRP //for height, previous must not match prevGuard
        val action = Transition.ParseAction.HEIGHT
        val item = from.runtimeRule
        val nextRPs = this.nextRulePosition(parentRP.rulePosition, item)
        return nextRPs.flatMap { nextRP ->
            val lh = parentRP.nextLookahead
            val toSet = this.fetchRulePositionPathsContaining(pathSet, nextRP, lh)
            val lookaheadGuard = from.rulePosition.graftLookahead //this.calcHeightLookahead(closureRPS.ancestorRPs.map { it.rulePosition }, closureRPS.rulePosition.rulePosition) //from.heightLookahead
            toSet.map { to ->
                Transition(from, to, action, item, lookaheadGuard, prevGuard)
            }
        }.toSet()
    }

    private fun createGraftTransition(pathSet: Set<RulePositionPath>, from: RulePositionPath, parentRP: RulePositionState): Set<Transition> {
        //val parentRP = closureRPS.directParent ?: throw ParseException("Should never be null")
        val prevGuard = parentRP //for graft, previous must match prevGuard
        val action = Transition.ParseAction.GRAFT
        val itemRP = from.rulePosition
        val item = from.runtimeRule
        //val ansRPs = closureRPS.ancestorRPs
        val lookaheadGuard = from.rulePosition.graftLookahead //this.calcGraftLookahead(parentRP, itemRP)
        val nextRPs = this.nextRulePosition(parentRP.rulePosition, item)
        return nextRPs.flatMap { nextRP ->
            val lh = parentRP.nextLookahead
            val toSet = this.fetchRulePositionPathsContaining(pathSet, nextRP, lh)
            toSet.map { to ->
                Transition(from, to, action, item, lookaheadGuard, prevGuard)
            }
        }.toSet()
    }

    private fun calcTransitions(userGoalRule: RuntimeRule, pathSet: Set<RulePositionPath>, from: RulePositionPath): Set<Transition> {
        val transitions = mutableSetOf<Transition>()

        val goal = from.runtimeRule.isGoal && from.isAtEnd
        if (goal) {
            val action = Transition.ParseAction.GOAL
            val to = from
            transitions += Transition(from, to, action, RuntimeRuleSet.END_OF_TEXT, emptySet(), null)
        } else {
            if (from.isAtEnd) {
                val path = from
                val parentRP = path.directParent
                if (null != parentRP) {
                    if (parentRP != null && parentRP.rulePosition.runtimeRule.isGoal) {
                        transitions += this.createGraftTransition(pathSet, from, parentRP)
                    } else {
                        val height = parentRP.isAtStart
                        val graft = parentRP.isAtStart.not()

                        if (height) {
                            transitions += this.createHeightTransition(pathSet, from, parentRP)
                        }
                        if (graft) {
                            transitions += this.createGraftTransition(pathSet, from, parentRP)
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
                        transitions += this.createWidthTransition(pathSet, from, closureRPS)
                    }
                }
            }
        }
        return transitions
    }

    private fun calcSkipTransitions(userGoalRule: RuntimeRule, from: RulePositionPath): Set<Transition> {
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

    fun buildCaches() {

    }

    fun startingRulePosition(userGoalRule: RuntimeRule): RulePositionPath {
        val goalRule = RuntimeRuleSet.createGoalRule(userGoalRule)
        val goalRp = RulePosition(goalRule, 0, 0)
        val goalState = RulePositionState(goalRp, emptySet(), emptySet())
        this.createAllSkipRulePositionPaths(userGoalRule)
        this.createAllRulePositionPaths(userGoalRule, goalState)
        return this.paths[userGoalRule].first()
    }

    fun transitions(userGoalRule: RuntimeRule, from: RulePositionPath): Set<Transition> {
        val transitions = this.transitions_cache[userGoalRule][from]
        if (null == transitions) {
            val pathSet = this.paths[userGoalRule]
            val t = this.calcTransitions(userGoalRule,pathSet, from)
            this.transitions_cache[userGoalRule][from] = t
            return t
        } else {
            return transitions
        }
    }

    fun skipTransitions(userGoalRule: RuntimeRule, from: RulePositionPath): Set<Transition> {
        val transitions = this.skipTransitions_cache[userGoalRule][from]
        if (null == transitions) {
            val pathSet = this.skipPaths
            val t = this.calcSkipTransitions(userGoalRule,  from)
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