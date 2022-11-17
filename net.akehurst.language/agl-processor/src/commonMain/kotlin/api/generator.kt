package net.akehurst.language.agl.api.generator

import net.akehurst.language.agl.api.automaton.ParseAction
import net.akehurst.language.agl.runtime.structure.RulePosition

abstract class GeneratorConstants {

    companion object {
        val SR = RulePosition.START_OF_RULE
        val ER = RulePosition.END_OF_RULE
        val WIDTH = ParseAction.WIDTH
        val HEIGHT = ParseAction.HEIGHT
        val GRAFT = ParseAction.GRAFT
        val GOAL = ParseAction.GOAL
    }

}