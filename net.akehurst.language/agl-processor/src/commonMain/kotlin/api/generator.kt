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

package net.akehurst.language.agl.api.generator

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.api.runtime.RuleSet
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.automaton.Automaton
import net.akehurst.language.api.automaton.ParseAction
import net.akehurst.language.api.language.grammar.Grammar
import net.akehurst.language.api.language.grammar.RuleItem
import net.akehurst.language.api.language.reference.CrossReferenceModel
import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser

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
    abstract val scopeModelString: String
    abstract val defaultGoalRuleName: String
    abstract val ruleSet: RuleSet
    abstract val mapToGrammar: (Int, Int) -> RuleItem
    abstract val crossReferenceModel: CrossReferenceModel?
    abstract val syntaxAnalyser: SyntaxAnalyser<AsmType>?
    abstract val formatter: Formatter<AsmType>?
    abstract val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>?
    abstract val automata: Map<String, Automaton>

    val grammar: Grammar by lazy {
        Agl.registry.agl.grammar.processor!!.process(grammarString).asm!!.first() //FIXME
    }
}
