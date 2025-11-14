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

import net.akehurst.language.api.processor.*
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.issues.api.IssueCollection
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.style.api.AglStyleDomain
import net.akehurst.language.types.api.TypesDomain

//TODO: has to be public at present because otherwise JSNames are not correct for properties
internal class LanguageDefinitionFromLanguageObject<AsmType : Any, ContextType : Any>(
    val languageObject: LanguageObject<AsmType, ContextType>,
) : LanguageDefinition<AsmType, ContextType> {
    override val identity: LanguageIdentity get() = languageObject.identity
    override val isModifiable: Boolean = false

    override val grammarString: GrammarString? get() = GrammarString(languageObject.grammarString)
    override val grammarDomain: GrammarDomain? get() = languageObject.grammarDomain
    override val targetGrammar: Grammar? get() = languageObject.defaultTargetGrammar
    override val targetGrammarName: SimpleName? get() = targetGrammar?.name
    override val defaultGoalRule: GrammarRuleName? get() = GrammarRuleName(languageObject.defaultTargetGoalRule)

    override val typesString: TypesString? get() = TypesString(languageObject.typesString)
    override val typesDomain: TypesDomain? get() = languageObject.typesDomain

    override val asmTransformString: AsmTransformString? get() = AsmTransformString(languageObject.asmTransformString)
    override val transformDomain: AsmTransformDomain? get() = languageObject.asmTransformDomain

    override val crossReferenceString: CrossReferenceString? get() = CrossReferenceString(languageObject.crossReferenceString)
    override val crossReferenceDomain: CrossReferenceDomain? get() = languageObject.crossReferenceDomain

    override var configuration: LanguageProcessorConfiguration<AsmType, ContextType>
        get() = TODO()
        set(value) {
            TODO()
        }

    override val syntaxAnalyser: SyntaxAnalyser<AsmType>? get() = languageObject.syntaxAnalyser
    override val semanticAnalyser: SemanticAnalyser<AsmType, ContextType>? get() = languageObject.semanticAnalyser

    override val formatString: FormatString? get() = FormatString(languageObject.formatString)

    //val formatterModel:AglFormatterModel?
    override val formatter: Formatter<AsmType>? get() = TODO()

    /** the options for parsing/processing the grammarStr for this language */
    //var aglOptions: ProcessOptions<DefinitionBlock<Grammar>, GrammarContext>?
    override val processor: LanguageProcessor<AsmType, ContextType>? by lazy {
        LanguageProcessorFromLanguageObject(languageObject as LanguageObjectAbstract<AsmType, ContextType>)
    }

    override val styleString: StyleString? get() = StyleString(languageObject.styleString)
    override val styleDomain: AglStyleDomain? get() = languageObject.styleDomain

    override val issues: IssueCollection<LanguageIssue> = IssueHolder(LanguageProcessorPhase.ALL)

    override val processorObservers: MutableList<(LanguageProcessor<AsmType, ContextType>?, LanguageProcessor<AsmType, ContextType>?) -> Unit> = mutableListOf()
    override val grammarStrObservers: MutableList<(GrammarString?, GrammarString?) -> Unit> = mutableListOf()
    override val grammarObservers: MutableList<(GrammarDomain?, GrammarDomain?) -> Unit> = mutableListOf()
    override val typesStrObservers: MutableList<(TypesString?, TypesString?) -> Unit> = mutableListOf()
    override val asmTransformStrObservers: MutableList<(AsmTransformString?, AsmTransformString?) -> Unit> = mutableListOf()
    override val crossReferenceStrObservers: MutableList<(CrossReferenceString?, CrossReferenceString?) -> Unit> = mutableListOf()

    //val crossReferenceModelObservers: MutableList<(CrossReferenceModel?, CrossReferenceModel?) -> Unit>
    override val formatterStrObservers: MutableList<(FormatString?, FormatString?) -> Unit> = mutableListOf()

    //val formatterObservers: MutableList<(AglFormatterModel?, AglFormatterModel?) -> Unit>
    override val styleStrObservers: MutableList<(StyleString?, StyleString?) -> Unit> = mutableListOf()
    //val styleObservers: MutableList<(AglStyleModel?, AglStyleModel?) -> Unit>

    override fun update(
        grammarString: GrammarString?,
        typesString: TypesString?,
        asmTransformString: AsmTransformString?,
        crossReferenceString: CrossReferenceString?,
        styleString: StyleString?,
        formatString: FormatString?,
    ) {
        error("Cannot update a LanguageDefinition that is based on a LanguageObject")
    }
}