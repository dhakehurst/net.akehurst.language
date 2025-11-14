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
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.base.api.Namespace
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.PublicValueType
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.issues.api.IssueCollection
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.style.api.AglStyleDomain
import net.akehurst.language.types.api.TypesDomain

interface GrammarRegistry {
    val grammars: List<Grammar>

    fun registerGrammar(grammar: Grammar)
    fun findGrammarOrNull(localNamespace: Namespace<Grammar>, nameOrQName: PossiblyQualifiedName): Grammar?
}

// @JvmInline
// TODO: value classes don't work (fully) in js and wasm
data class LanguageIdentity(override val value: String) : PublicValueType {
    val last: String get() = value.split(".").last()
}

// @JvmInline
// TODO: value classes don't work (fully) in js and wasm
data class GrammarString(override val value: String) : PublicValueType

// @JvmInline
// TODO: value classes don't work (fully) in js and wasm
data class TypesString(override val value: String) : PublicValueType

// @JvmInline
// TODO: value classes don't work (fully) in js and wasm
data class AsmTransformString(override val value: String) : PublicValueType

// @JvmInline
// TODO: value classes don't work (fully) in js and wasm
data class CrossReferenceString(override val value: String) : PublicValueType

// @JvmInline
// TODO: value classes don't work (fully) in js and wasm
data class StyleString(override val value: String) : PublicValueType

// @JvmInline
// TODO: value classes don't work (fully) in js and wasm
data class FormatString(override val value: String) : PublicValueType

interface LanguageRegistry : GrammarRegistry {

    val agl: AglLanguages

    val languages: Map<LanguageIdentity, LanguageDefinition<*, *>>

    /**
     * create and register a LanguageDefinition as specified
     */
    fun <AsmType : Any, ContextType : Any> register(
        identity: LanguageIdentity,
        aglOptions: ProcessOptions<GrammarDomain, SentenceContextAny>?,
        buildForDefaultGoal: Boolean,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>
    ): LanguageDefinition<AsmType, ContextType>

    fun unregister(identity: LanguageIdentity)

    fun <AsmType : Any, ContextType : Any> findOrNull(identity: LanguageIdentity): LanguageDefinition<AsmType, ContextType>?

    fun <AsmType : Any, ContextType : Any> findOrPlaceholder(
        identity: LanguageIdentity,
        aglOptions: ProcessOptions<GrammarDomain, SentenceContextAny>? = null,
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
    val grammarDomain: GrammarDomain?
    val targetGrammar: Grammar?
    val targetGrammarName: SimpleName?
    val defaultGoalRule: GrammarRuleName?

    val typesString: TypesString?
    val typesDomain: TypesDomain?

    val asmTransformString: AsmTransformString?
    val transformDomain: AsmTransformDomain?

    val crossReferenceString: CrossReferenceString?
    val crossReferenceDomain: CrossReferenceDomain?

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
    val styleDomain: AglStyleDomain?

    val issues: IssueCollection<LanguageIssue>

    val processorObservers: MutableList<(LanguageProcessor<AsmType, ContextType>?, LanguageProcessor<AsmType, ContextType>?) -> Unit>
    val grammarStrObservers: MutableList<(GrammarString?, GrammarString?) -> Unit>
    val grammarObservers: MutableList<(GrammarDomain?, GrammarDomain?) -> Unit>
    val typesStrObservers: MutableList<(TypesString?, TypesString?) -> Unit>
    val asmTransformStrObservers: MutableList<(AsmTransformString?, AsmTransformString?) -> Unit>
    val crossReferenceStrObservers: MutableList<(CrossReferenceString?, CrossReferenceString?) -> Unit>

    //val crossReferenceModelObservers: MutableList<(CrossReferenceModel?, CrossReferenceModel?) -> Unit>
    val formatterStrObservers: MutableList<(FormatString?, FormatString?) -> Unit>

    //val formatterObservers: MutableList<(AglFormatterModel?, AglFormatterModel?) -> Unit>
    val styleStrObservers: MutableList<(StyleString?, StyleString?) -> Unit>
    //val styleObservers: MutableList<(AglStyleModel?, AglStyleModel?) -> Unit>

    fun update(
        grammarString: GrammarString? = null,
        typesString: TypesString? = null,
        asmTransformString: AsmTransformString? = null,
        crossReferenceString: CrossReferenceString? = null,
        styleString: StyleString? = null,
        formatString: FormatString? = null,
    )
}