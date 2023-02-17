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

package net.akehurst.language.agl.processor

import net.akehurst.language.agl.agl.parser.Scanner
import net.akehurst.language.agl.grammar.grammar.ConverterToRuntimeRules
import net.akehurst.language.agl.grammar.grammar.GrammarContext
import net.akehurst.language.agl.parser.Parser
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsLiteral
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsPattern
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.semanticAnalyser.SemanticAnalyserSimple
import net.akehurst.language.agl.sppt.SPPTParserDefault
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserSimple
import net.akehurst.language.agl.syntaxAnalyser.TypeModelFromGrammar
import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.sppt.SPPTLeaf
import net.akehurst.language.api.sppt.SPPTParser
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.api.typeModel.TypeModel

internal class LanguageProcessorDefault<AsmType : Any, ContextType : Any>(
    override val configuration: LanguageProcessorConfiguration<AsmType, ContextType>,
) : LanguageProcessorAbstract<AsmType, ContextType>() {

    private val _converterToRuntimeRules: ConverterToRuntimeRules by lazy { ConverterToRuntimeRules(configuration.grammarResolver) }
    override val _runtimeRuleSet by lazy { this._converterToRuntimeRules.runtimeRuleSet }
    override val mapToGrammar: (Int, Int) -> RuleItem = { ruleSetNumber, ruleNumber -> this._converterToRuntimeRules.originalRuleItemFor(ruleSetNumber, ruleNumber) }

}
