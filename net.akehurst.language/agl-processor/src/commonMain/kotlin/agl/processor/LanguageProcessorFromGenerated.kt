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

import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.api.processor.LanguageObjectAbstract
import net.akehurst.language.api.processor.LanguageProcessorConfiguration
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.format.processor.FormatterOverAsmSimple
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.grammar.api.RuleItem
import net.akehurst.language.parser.api.RuleSet
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.reference.asm.CrossReferenceModelDefault

internal class LanguageProcessorFromGenerated<AsmType:Any, ContextType : Any>(
    val generated: LanguageObjectAbstract<AsmType, ContextType>,
) : LanguageProcessorAbstract<AsmType, ContextType>() {

    override val configuration: LanguageProcessorConfiguration<AsmType, ContextType>
        get() = TODO("not implemented")

    override val targetRuleSet: RuleSet = generated.ruleSet
    override val grammarModel: GrammarModel = generated.grammarModel
    override val mapToGrammar: (Int, Int) -> RuleItem = generated.mapToGrammar
    override val crossReferenceModel: CrossReferenceModel = generated.crossReferenceModel ?: CrossReferenceModelDefault(SimpleName("FromGrammar"+ grammarModel.name.value))
    override val syntaxAnalyser: SyntaxAnalyser<AsmType>? = generated.syntaxAnalyser
    override val formatter: Formatter<AsmType> = FormatterOverAsmSimple(generated.formatModel, generated.typeModel, this.issues) as Formatter<AsmType>
    override val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>? = generated.semanticAnalyser

    init {
        generated.automata.forEach { (userGoalRuleName, automaton) ->
            this.targetRuleSet.addPreBuiltFor(userGoalRuleName, automaton)
        }
    }
}