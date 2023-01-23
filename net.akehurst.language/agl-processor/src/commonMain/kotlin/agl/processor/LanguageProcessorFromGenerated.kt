/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.processor

import net.akehurst.language.agl.api.generator.GeneratedLanguageProcessorAbstract
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.processor.Formatter

internal class LanguageProcessorFromGenerated<AsmType : Any, ContextType : Any>(
    val generated : GeneratedLanguageProcessorAbstract<AsmType, ContextType>,
) : LanguageProcessorAbstract<AsmType, ContextType>() {

    override val defaultGoalRuleName: String  get() = generated.defaultGoalRuleName
    override val _runtimeRuleSet: RuntimeRuleSet = generated.ruleSet as RuntimeRuleSet
    override val mapToGrammar: (Int, Int) -> RuleItem = generated.mapToGrammar
    override val syntaxAnalyser: SyntaxAnalyser<AsmType, ContextType>? = generated.syntaxAnalyser
    override val formatter: Formatter? = generated.formatter
    override val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>? = generated.semanticAnalyser
    override val grammar: Grammar = generated.grammar

    init {
        generated.automata.forEach { (userGoalRuleName, automaton) ->
            this._runtimeRuleSet.addGeneratedBuildFor(userGoalRuleName, automaton)
        }
    }
}