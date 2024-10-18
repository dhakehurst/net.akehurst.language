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

import net.akehurst.language.agl.CrossReferenceString
import net.akehurst.language.agl.FormatString
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.agl.StyleString
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.agl.processor.AglLanguages
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.Namespace
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.PublicValueType
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.issues.api.IssueCollection
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.style.api.AglStyleModel
import net.akehurst.language.typemodel.api.TypeModel
import kotlin.jvm.JvmInline

interface GrammarRegistry {
    fun registerGrammar(grammar: Grammar)
    fun findGrammarOrNull(localNamespace: Namespace<Grammar>, nameOrQName: PossiblyQualifiedName): Grammar?
}

@JvmInline
value class LanguageIdentity(override val value:String): PublicValueType {
    val last:String get() = value.split(".").last()
}

interface LanguageRegistry : GrammarRegistry {

    val agl: AglLanguages

    /**
     * create and register a LanguageDefinition as specified
     */
    fun <AsmType : Any, ContextType : Any> register(
        identity: LanguageIdentity,
        grammarStr: GrammarString?,
        aglOptions: ProcessOptions<GrammarModel, ContextFromGrammarRegistry>?,
        buildForDefaultGoal: Boolean,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>
    ): LanguageDefinition<AsmType, ContextType>

    fun unregister(identity: LanguageIdentity)

    fun <AsmType : Any, ContextType : Any> findOrNull(identity: LanguageIdentity): LanguageDefinition<AsmType, ContextType>?

    fun <AsmType : Any, ContextType : Any> findOrPlaceholder(
        identity: LanguageIdentity,
        aglOptions: ProcessOptions<GrammarModel, ContextFromGrammarRegistry>?,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>?
    ): LanguageDefinition<AsmType, ContextType>
}

interface LanguageDefinition<AsmType : Any, ContextType : Any> {

    val identity: LanguageIdentity
    val isModifiable: Boolean

    var grammarStr: GrammarString?
    var grammarModel: GrammarModel
    val targetGrammar: Grammar?
    var targetGrammarName: SimpleName?
    var defaultGoalRule: GrammarRuleName?

    val typeModel: TypeModel?

    var crossReferenceModelStr: CrossReferenceString?
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

    var styleStr: StyleString?
    val style: AglStyleModel?

    val issues: IssueCollection<LanguageIssue>

    val processorObservers: MutableList<(LanguageProcessor<AsmType, ContextType>?, LanguageProcessor<AsmType, ContextType>?) -> Unit>
    val grammarStrObservers: MutableList<(GrammarString?, GrammarString?) -> Unit>
    val grammarObservers: MutableList<(GrammarModel, GrammarModel) -> Unit>
    val crossReferenceModelStrObservers: MutableList<(CrossReferenceString?, CrossReferenceString?) -> Unit>

    //val crossReferenceModelObservers: MutableList<(CrossReferenceModel?, CrossReferenceModel?) -> Unit>
    val formatterStrObservers: MutableList<(FormatString?, FormatString?) -> Unit>

    //val formatterObservers: MutableList<(AglFormatterModel?, AglFormatterModel?) -> Unit>
    val styleStrObservers: MutableList<(StyleString?, StyleString?) -> Unit>
    //val styleObservers: MutableList<(AglStyleModel?, AglStyleModel?) -> Unit>

    fun update(grammarStr: GrammarString?, crossReferenceModelStr: CrossReferenceString?, styleStr: StyleString?)
}