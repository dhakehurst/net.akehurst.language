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

import net.akehurst.language.agl.processor.AglLanguages
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.Namespace
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.PublicValueType
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.issues.api.IssueCollection
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.style.api.AglStyleModel
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.typemodel.api.TypeModel
import kotlin.jvm.JvmInline

interface GrammarRegistry {
    val grammars:List<Grammar>

    fun registerGrammar(grammar: Grammar)
    fun findGrammarOrNull(localNamespace: Namespace<Grammar>, nameOrQName: PossiblyQualifiedName): Grammar?
}

@JvmInline
value class LanguageIdentity(override val value: String) : PublicValueType {
    val last: String get() = value.split(".").last()
}

@JvmInline
value class GrammarString(override val value: String) : PublicValueType

@JvmInline
value class TypesString(override val value: String) : PublicValueType

@JvmInline
value class TransformString(override val value: String) : PublicValueType

@JvmInline
value class CrossReferenceString(override val value: String) : PublicValueType

@JvmInline
value class StyleString(override val value: String) : PublicValueType

@JvmInline
value class FormatString(override val value: String) : PublicValueType

interface LanguageRegistry : GrammarRegistry {

    val agl: AglLanguages

    val languages:Map<LanguageIdentity,LanguageDefinition<*, *>>

    /**
     * create and register a LanguageDefinition as specified
     */
    fun <AsmType : Any, ContextType : Any> register(
        identity: LanguageIdentity,
        aglOptions: ProcessOptions<GrammarModel, ContextWithScope<Any,Any>>?,
        buildForDefaultGoal: Boolean,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>
    ): LanguageDefinition<AsmType, ContextType>

    fun unregister(identity: LanguageIdentity)

    fun <AsmType : Any, ContextType : Any> findOrNull(identity: LanguageIdentity): LanguageDefinition<AsmType, ContextType>?

    fun <AsmType : Any, ContextType : Any> findOrPlaceholder(
        identity: LanguageIdentity,
        aglOptions: ProcessOptions<GrammarModel, ContextWithScope<Any,Any>>? = null,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>? = null
    ): LanguageDefinition<AsmType, ContextType>
}

/**
 * mutable, you can change the language components for a definition
 */
interface LanguageDefinition<AsmType : Any, ContextType : Any> {

    val identity: LanguageIdentity
    val isModifiable: Boolean

    val grammarString: GrammarString?
    val grammarModel: GrammarModel?
    val targetGrammar: Grammar?
    val targetGrammarName: SimpleName?
    val defaultGoalRule: GrammarRuleName?

    val typesString: TypesString?
    val typesModel: TypeModel?

    val transformString: TransformString?
    val transformModel: TransformModel?

    val crossReferenceString: CrossReferenceString?
    val crossReferenceModel: CrossReferenceModel?

    var configuration: LanguageProcessorConfiguration<AsmType, ContextType>

    val syntaxAnalyser: SyntaxAnalyser<AsmType>?
    val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>?

    val formatString: FormatString?
    //val formatterModel:AglFormatterModel?
    val formatter: Formatter<AsmType>?

    /** the options for parsing/processing the grammarStr for this language */
    //var aglOptions: ProcessOptions<DefinitionBlock<Grammar>, GrammarContext>?
    val processor: LanguageProcessor<AsmType, ContextType>?

    val styleString: StyleString?
    val styleModel: AglStyleModel?

    val issues: IssueCollection<LanguageIssue>

    val processorObservers: MutableList<(LanguageProcessor<AsmType, ContextType>?, LanguageProcessor<AsmType, ContextType>?) -> Unit>
    val grammarStrObservers: MutableList<(GrammarString?, GrammarString?) -> Unit>
    val grammarObservers: MutableList<(GrammarModel?, GrammarModel?) -> Unit>
    val typeModelStrObservers: MutableList<(TypesString?, TypesString?) -> Unit>
    val asmTransformStrObservers: MutableList<(TransformString?, TransformString?) -> Unit>
    val crossReferenceStrObservers: MutableList<(CrossReferenceString?, CrossReferenceString?) -> Unit>

    //val crossReferenceModelObservers: MutableList<(CrossReferenceModel?, CrossReferenceModel?) -> Unit>
    val formatterStrObservers: MutableList<(FormatString?, FormatString?) -> Unit>

    //val formatterObservers: MutableList<(AglFormatterModel?, AglFormatterModel?) -> Unit>
    val styleStrObservers: MutableList<(StyleString?, StyleString?) -> Unit>
    //val styleObservers: MutableList<(AglStyleModel?, AglStyleModel?) -> Unit>

    fun update(
        grammarString: GrammarString?=null,
        typesString: TypesString?=null,
        transformString: TransformString?=null,
        crossReferenceString: CrossReferenceString?=null,
        styleString: StyleString?=null,
        formatString: FormatString?=null,
    )
}