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

package net.akehurst.language.api.processor

import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.Namespace
import net.akehurst.language.api.style.AglStyleModel

interface GrammarRegistry {
    fun register(grammar: Grammar)
    fun findGrammarOrNull(localNamespace: Namespace, nameOrQName: String): Grammar?
}

interface LanguageDefinition<AsmType : Any, ContextType : Any> {

    val identity: String
    val isModifiable: Boolean

    var grammarStr: String?
    var grammar: Grammar?
    var targetGrammarName: String?
    var defaultGoalRule: String?

    var scopeModelStr: String?
    var scopeModel: ScopeModel?

    var configuration: LanguageProcessorConfiguration<AsmType, ContextType>

    val syntaxAnalyser: SyntaxAnalyser<AsmType>?
    val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>?

    //var formatStr: String?
    //val formatterModel:AglFormatterModel?
    val formatter: Formatter<AsmType>?

    /** the options for parsing/processing the grammarStr for this language */
    //var aglOptions: ProcessOptions<List<Grammar>, GrammarContext>?
    val processor: LanguageProcessor<AsmType, ContextType>?

    var styleStr: String?
    var style: AglStyleModel?

    val issues: IssueCollection

    val processorObservers: MutableList<(LanguageProcessor<AsmType, ContextType>?, LanguageProcessor<AsmType, ContextType>?) -> Unit>
    val grammarStrObservers: MutableList<(String?, String?) -> Unit>
    val grammarObservers: MutableList<(Grammar?, Grammar?) -> Unit>
    val scopeStrObservers: MutableList<(String?, String?) -> Unit>
    val scopeModelObservers: MutableList<(ScopeModel?, ScopeModel?) -> Unit>
    val formatterStrObservers: MutableList<(String?, String?) -> Unit>
    val formatterObservers: MutableList<(AglFormatterModel?, AglFormatterModel?) -> Unit>
    val styleStrObservers: MutableList<(String?, String?) -> Unit>
    val styleObservers: MutableList<(AglStyleModel?, AglStyleModel?) -> Unit>

    fun update(grammarStr: String?, scopeModelStr: String?, styleStr: String?)
}