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

import net.akehurst.language.agl.language.grammar.ContextFromGrammarRegistry
import net.akehurst.language.agl.processor.AglLanguages
import net.akehurst.language.api.language.base.Namespace
import net.akehurst.language.api.language.base.PossiblyQualifiedName
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.language.base.SimpleName
import net.akehurst.language.api.language.grammar.Grammar
import net.akehurst.language.api.language.grammar.GrammarModel
import net.akehurst.language.api.language.grammar.GrammarRuleName
import net.akehurst.language.api.language.reference.CrossReferenceModel
import net.akehurst.language.api.language.style.AglStyleModel
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.issues.api.IssueCollection
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.typemodel.api.TypeModel

interface GrammarRegistry {
    fun registerGrammar(grammar: Grammar)
    fun findGrammarOrNull(localNamespace: Namespace<Grammar>, nameOrQName: PossiblyQualifiedName): Grammar?
}

interface LanguageRegistry : GrammarRegistry {

    val agl: AglLanguages

    /**
     * create and register a LanguageDefinition as specified
     */
    fun <AsmType : Any, ContextType : Any> register(
        identity: QualifiedName,
        grammarStr: String?,
        aglOptions: ProcessOptions<GrammarModel, ContextFromGrammarRegistry>?,
        buildForDefaultGoal: Boolean,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>
    ): LanguageDefinition<AsmType, ContextType>

    fun unregister(identity: QualifiedName)

    fun <AsmType : Any, ContextType : Any> findOrNull(identity: QualifiedName): LanguageDefinition<AsmType, ContextType>?

    fun <AsmType : Any, ContextType : Any> findOrPlaceholder(
        identity: QualifiedName,
        aglOptions: ProcessOptions<GrammarModel, ContextFromGrammarRegistry>?,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>?
    ): LanguageDefinition<AsmType, ContextType>
}

interface LanguageDefinition<AsmType : Any, ContextType : Any> {

    val identity: QualifiedName
    val isModifiable: Boolean

    var grammarStr: String?
    var grammarList: GrammarModel
    val targetGrammar: Grammar?
    var targetGrammarName: SimpleName?
    var defaultGoalRule: GrammarRuleName?

    val typeModel: TypeModel?

    var crossReferenceModelStr: String?
    val crossReferenceModel: CrossReferenceModel?

    var configuration: LanguageProcessorConfiguration<AsmType, ContextType>

    val syntaxAnalyser: SyntaxAnalyser<AsmType>?
    val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>?

    //var formatStr: String?
    //val formatterModel:AglFormatterModel?
    val formatter: Formatter<AsmType>?

    /** the options for parsing/processing the grammarStr for this language */
    //var aglOptions: ProcessOptions<DefinitionBlock<Grammar>, GrammarContext>?
    val processor: LanguageProcessor<AsmType, ContextType>?

    var styleStr: String?
    val style: AglStyleModel?

    val issues: IssueCollection<LanguageIssue>

    val processorObservers: MutableList<(LanguageProcessor<AsmType, ContextType>?, LanguageProcessor<AsmType, ContextType>?) -> Unit>
    val grammarStrObservers: MutableList<(String?, String?) -> Unit>
    val grammarObservers: MutableList<(GrammarModel, GrammarModel) -> Unit>
    val crossReferenceModelStrObservers: MutableList<(String?, String?) -> Unit>

    //val crossReferenceModelObservers: MutableList<(CrossReferenceModel?, CrossReferenceModel?) -> Unit>
    val formatterStrObservers: MutableList<(String?, String?) -> Unit>

    //val formatterObservers: MutableList<(AglFormatterModel?, AglFormatterModel?) -> Unit>
    val styleStrObservers: MutableList<(String?, String?) -> Unit>
    //val styleObservers: MutableList<(AglStyleModel?, AglStyleModel?) -> Unit>

    fun update(grammarStr: String?, crossReferenceModelStr: String?, styleStr: String?)
}