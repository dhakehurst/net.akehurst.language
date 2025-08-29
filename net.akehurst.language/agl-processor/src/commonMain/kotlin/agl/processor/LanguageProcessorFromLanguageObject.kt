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

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.api.processor.LanguageObjectAbstract
import net.akehurst.language.api.processor.LanguageProcessorConfiguration
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.format.processor.FormatterOverAsmSimple
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammar.api.RuleItem
import net.akehurst.language.parser.api.RuleSet
import net.akehurst.language.reference.api.CrossReferenceDomain

internal class LanguageProcessorFromLanguageObject<AsmType : Any, ContextType : Any>(
    languageObject: LanguageObjectAbstract<AsmType, ContextType>,
) : LanguageProcessorAbstract<AsmType, ContextType>() {

    override val configuration: LanguageProcessorConfiguration<AsmType, ContextType> =
        Agl.configurationFromLanguageObject(languageObject)

    override val ruleSets: Map<String, RuleSet> = languageObject.ruleSets
    override val targetRuleSet: RuleSet = languageObject.targetRuleSet
    override val grammarDomain: GrammarDomain = languageObject.grammarDomain
    override val mapToGrammar: (Int, Int) -> RuleItem = languageObject.mapToGrammar
    override val crossReferenceDomain: CrossReferenceDomain = languageObject.crossReferenceDomain //?: CrossReferenceModelDefault(SimpleName("FromGrammar"+ grammarModel.name.value))
    override val syntaxAnalyser: SyntaxAnalyser<AsmType>? = languageObject.syntaxAnalyser
    override val formatter: Formatter<AsmType> = FormatterOverAsmSimple(languageObject.formatDomain, languageObject.typesDomain, this.issues) as Formatter<AsmType>
    override val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>? = languageObject.semanticAnalyser

    init {
        languageObject.automata.forEach { (userGoalRuleName, automaton) ->
            this.targetRuleSet.addPreBuiltFor(userGoalRuleName, automaton)
        }
    }
}