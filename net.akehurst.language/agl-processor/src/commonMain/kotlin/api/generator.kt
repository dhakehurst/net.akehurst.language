package net.akehurst.language.agl.api.generator

import net.akehurst.language.agl.api.automaton.ParseAction
import net.akehurst.language.agl.runtime.structure.RuleOptionPosition

abstract class GeneratorConstants {

    companion object {
        val SR = RuleOptionPosition.START_OF_RULE
        val ER = RuleOptionPosition.END_OF_RULE
        val WIDTH = ParseAction.WIDTH
    }

}