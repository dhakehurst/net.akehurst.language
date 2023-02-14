/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.agl.grammar.grammar.GrammarContext
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.processor.*
import net.akehurst.language.util.CachedValue
import net.akehurst.language.util.cached
import kotlin.properties.Delegates

//TODO: has to be public at present because otherwise JSNames are not correct for properties
internal class LanguageDefinitionFromAsm<AsmType : Any, ContextType : Any>(
    override val identity: String,
    grammarArg: Grammar,
    override var targetGrammar: String?,
    defaultGoalRuleArg: String?,
    buildForDefaultGoal: Boolean,
    styleArg: String?,
    formatArg: String?,
    syntaxAnalyserResolverArg: SyntaxAnalyserResolver<AsmType, ContextType>?,
    semanticAnalyserResolverArg: SemanticAnalyserResolver<AsmType, ContextType>?,
    /** the options to configure building the processor for the registered language */
    aglOptionsArg: ProcessOptions<List<Grammar>, GrammarContext>?
) : LanguageDefinitionAbstract<AsmType, ContextType>(
    defaultGoalRuleArg,
    grammarArg,
    buildForDefaultGoal,
    styleArg,
    formatArg,
    syntaxAnalyserResolverArg,
    semanticAnalyserResolverArg,
    aglOptionsArg
) {
    override var grammarStr: String?
        get() = this.grammar.toString() //TODO:
        set(value) {
            error("Cannot set the grammar of a LanguageDefinitionFromAsm using a String")
        }

    override val grammarIsModifiable: Boolean = false

}