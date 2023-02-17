package net.akehurst.language.agl.api.generator

import net.akehurst.language.agl.api.automaton.Automaton
import net.akehurst.language.agl.api.automaton.ParseAction
import net.akehurst.language.agl.api.runtime.RuleSet
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.processor.Formatter

abstract class GeneratedLanguageProcessorAbstract<AsmType : Any, ContextType : Any> {

    companion object {
        const val GOAL_RULE = RuntimeRuleSet.GOAL_RULE_NUMBER

        const val SR = RulePosition.START_OF_RULE
        const val ER = RulePosition.END_OF_RULE

        val WIDTH = ParseAction.WIDTH
        val HEIGHT = ParseAction.HEIGHT
        val GRAFT = ParseAction.GRAFT
        val GOAL = ParseAction.GOAL
    }

    abstract val grammarString: String
    abstract val defaultGoalRuleName: String
    abstract val ruleSet: RuleSet
    abstract val mapToGrammar: (Int, Int) -> RuleItem
    abstract val scopeModel: ScopeModel?
    abstract val syntaxAnalyser: SyntaxAnalyser<AsmType, ContextType>?
    abstract val formatter: Formatter<AsmType>?
    abstract val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>?
    abstract val automata: Map<String, Automaton>

    val grammar: Grammar by lazy {
        Agl.registry.agl.grammar.processor!!.process(grammarString).asm!!.first() //FIXME
    }
}
