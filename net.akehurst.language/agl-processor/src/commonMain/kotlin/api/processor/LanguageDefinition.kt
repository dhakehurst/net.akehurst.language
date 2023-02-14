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

import net.akehurst.language.agl.grammar.grammar.GrammarContext
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.Namespace

interface LanguageRegistry {
    fun findGrammarOrNull(namespace: Namespace, name:String) :Grammar?
}

interface LanguageDefinition<AsmType : Any, ContextType : Any> {
    val identity: String
    var grammarStr: String?
    val grammarIsModifiable: Boolean
    var targetGrammar:String?
    var defaultGoalRule: String?
    val syntaxAnalyser: SyntaxAnalyser<AsmType, ContextType>?
    val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>?
    var syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>?
    var semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>?
    /** the options to configure building the processor for the registered language */
    var aglOptions: ProcessOptions<List<Grammar>, GrammarContext>?
    var grammar: Grammar?
    val processor: LanguageProcessor<AsmType, ContextType>?
    val issues : List<LanguageIssue>

    var style: String?
    var format: String?

    val processorObservers: MutableList<(LanguageProcessor<AsmType, ContextType>?, LanguageProcessor<AsmType, ContextType>?) -> Unit>
    val grammarObservers: MutableList<(Grammar?, Grammar?) -> Unit>
    val styleObservers: MutableList<(String?, String?) -> Unit>
    val formatObservers: MutableList<(String?, String?) -> Unit>

}