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

package net.akehurst.language.api.processor

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.automaton.api.Automaton
import net.akehurst.language.automaton.api.ParseAction
import net.akehurst.language.formatter.api.AglFormatDomain
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammar.api.RuleItem
import net.akehurst.language.parser.api.RulePosition
import net.akehurst.language.parser.api.RuleSet
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.style.api.AglStyleDomain
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.types.api.TypesDomain

interface LanguageObject<AsmType : Any, ContextType : Any> {
    val identity: LanguageIdentity
    val extends: List<LanguageObject<Any, ContextType>>
    val grammarString: String
    val typesString: String
    val kompositeString: String
    val asmTransformString: String
    val crossReferenceString: String
    val styleString: String
    val formatString: String

    val grammarDomain: GrammarDomain
    val typesDomain: TypesDomain
    val kompositeDomain: TypesDomain
    val asmTransformDomain: AsmTransformDomain
    val crossReferenceDomain: CrossReferenceDomain
    val styleDomain: AglStyleDomain
    val formatDomain: AglFormatDomain

    val defaultTargetGrammar: Grammar
    val defaultTargetGoalRule: String

    val syntaxAnalyser: SyntaxAnalyser<AsmType>?
    val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>?
    val completionProvider: CompletionProvider<AsmType, ContextType>?
}

abstract class LanguageObjectAbstract<AsmType : Any, ContextType : Any> : LanguageObject<AsmType, ContextType> {

    companion object {
        const val GOAL_RULE = RuntimeRuleSet.GOAL_RULE_NUMBER

        val OP_NONE = RulePosition.OPTION_NONE

        const val SR = RulePosition.START_OF_RULE
        const val ER = RulePosition.END_OF_RULE

        val WIDTH = ParseAction.WIDTH
        val HEIGHT = ParseAction.HEIGHT
        val GRAFT = ParseAction.GRAFT
        val GOAL = ParseAction.GOAL
    }

    override val kompositeDomain: TypesDomain get() = typesDomain

//    override val grammarString: String by lazy { grammarModel.asString() }
//    override val typesString: String by lazy { typesModel.asString() }
//    override val kompositeString: String by lazy { kompositeModel.asString() }
//    override val asmTransformString: String by lazy { asmTransformModel.asString() }
//    override val crossReferenceString: String by lazy { crossReferenceModel.asString() }
//    override val styleString: String by lazy { styleModel.asString() }
//    override val formatString: String by lazy { formatModel.asString() }

    open val ruleSets: Map<String, RuleSet> get() = TODO()
    open val targetRuleSet: RuleSet get() = TODO()
    open val mapToGrammar: (Int, Int) -> RuleItem get() = TODO()
    open val automata: Map<String, Automaton> get() = TODO()
    override val syntaxAnalyser: SyntaxAnalyser<AsmType>? get() = TODO()
    override val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>? get() = TODO()

}
