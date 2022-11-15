package net.akehurst.language.agl.api.runtime

import net.akehurst.language.agl.api.automaton.Automaton
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.api.processor.AutomatonKind
import net.akehurst.language.collections.MapNotNull

interface RuleSet {
    val goalRuleFor: MapNotNull<String, Rule>

    fun findRuntimeRule(tag: String): Rule
    fun fetchStateSetFor(userGoalRuleName: String, automatonKind: AutomatonKind): Automaton
    fun usedAutomatonToString(userGoalRuleName: String, withStates: Boolean = false): String
}

interface Rule {
}

data class RulePosition(
    val rule: Rule,
    val position: Int
) {
    companion object {
        const val START_OF_RULE = 0
        const val END_OF_RULE = -1

        const val OPTION_MULTI_ITEM = 0
        const val OPTION_MULTI_EMPTY = 1

        const val OPTION_SLIST_ITEM_OR_SEPERATOR = 0
        const val OPTION_SLIST_EMPTY = 1

        //for use in multi and separated list
        const val POSITION_MULIT_ITEM = 1 //TODO: make -ve
        const val POSITION_SLIST_SEPARATOR = 1 //TODO: make -ve
        const val POSITION_SLIST_ITEM = 2 //TODO: make -ve
    }

    val isAtStart get() = START_OF_RULE == position
    val isAtEnd get() = END_OF_RULE == position
}