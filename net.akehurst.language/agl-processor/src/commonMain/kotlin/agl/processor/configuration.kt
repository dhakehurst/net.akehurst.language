/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.processor

import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.agl.simple.*
import net.akehurst.language.api.processor.*
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.format.asm.AglFormatModelDefault
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.grammar.processor.contextFromGrammar
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.regex.agl.RegexEngineAgl
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.regex.api.RegexEngineKind
import net.akehurst.language.scanner.api.ScannerKind
import net.akehurst.language.scanner.common.ScannerClassic
import net.akehurst.language.scanner.common.ScannerOnDemand
import net.akehurst.language.style.asm.AglStyleModelDefault
import net.akehurst.language.transform.asm.TransformDomainDefault
import net.akehurst.language.typemodel.asm.TypeModelSimple


internal class LanguageProcessorConfigurationEmpty<AsmType : Any, ContextType : Any>(
    override var targetGrammarName: SimpleName? = null,
    override var defaultGoalRuleName: GrammarRuleName? = null,

    override val grammarString: GrammarString? = null,
    override val typesString: TypesString? = null,
    override val transformString: TransformString? = null,
    override val crossReferenceString: CrossReferenceString? = null,
    override val styleString: StyleString? = null,
    override val formatString: FormatString? = null,

    override val regexEngineKind: RegexEngineKind = RegexEngineKind.PLATFORM,
    override val scannerKind: ScannerKind = ScannerKind.OnDemand,
    override val scannerResolver: ScannerResolver<AsmType, ContextType>? = null,
    override val parserResolver: ParserResolver<AsmType, ContextType>? = null,
    override var transformResolver: TransformResolver<AsmType, ContextType>? = null,
    override var typesResolver: TypesResolver<AsmType, ContextType>? = null,
    override var crossReferenceResolver: CrossReferenceResolver<AsmType, ContextType>? = null,
    override var syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>? = null,
    override var semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>? = null,

    override var formatResolver: FormatResolver<AsmType, ContextType>? = null,
    override var styleResolver: StyleResolver<AsmType, ContextType>? = null,
    override var completionProviderResolver: CompletionProviderResolver<AsmType, ContextType>? = null

) : LanguageProcessorConfiguration<AsmType, ContextType>

internal class LanguageProcessorConfigurationBase<AsmType : Any, ContextType : Any>(
    override var targetGrammarName: SimpleName? = null,
    override var defaultGoalRuleName: GrammarRuleName? = null,

    override val grammarString: GrammarString? = null,
    override val typesString: TypesString? = null,
    override val transformString: TransformString? = null,
    override val crossReferenceString: CrossReferenceString? = null,
    override val styleString: StyleString? = null,
    override val formatString: FormatString? = null,

    override val regexEngineKind : RegexEngineKind = RegexEngineKind.PLATFORM,
    override val scannerKind: ScannerKind = ScannerKind.OnDemand,

    override val scannerResolver: ScannerResolver<AsmType, ContextType>? = {
        val regexEngine = when (regexEngineKind) {
            RegexEngineKind.PLATFORM -> RegexEnginePlatform
            RegexEngineKind.AGL -> RegexEngineAgl
        }
        val scanner = when (scannerKind) {
            ScannerKind.Classic -> ScannerClassic(regexEngine, it.targetRuleSet?.let { it.terminals + it.embeddedTerminals } ?: emptyList())
            ScannerKind.OnDemand -> ScannerOnDemand(regexEngine, it.targetRuleSet?.let { it.terminals + it.embeddedTerminals } ?: emptyList())
        }
        ProcessResultDefault(scanner, IssueHolder(LanguageProcessorPhase.ALL))
    },
    override val parserResolver: ParserResolver<AsmType, ContextType>? = { p ->
        ProcessResultDefault(
            p.targetRuleSet?.let { LeftCornerParser(p.scanner!!, it) },
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var typesResolver: TypesResolver<AsmType, ContextType>? = { p ->
        TypeModelSimple.fromString(SimpleName("FromGrammar"+p.grammarModel!!.name.value), contextFromGrammar(p.grammarModel!!),p.configuration.typesString ?: TypesString(""))
    },
    //override val asmFactoryResolver: AsmFactoryResolver<AsmFactory<AsmType,*,*>>? = null,
    override var transformResolver: TransformResolver<AsmType, ContextType>? = { p ->
        p.configuration.transformString?.let {
            TransformDomainDefault.fromString(ContextFromGrammarAndTypeModel(p.grammarModel!!, p.baseTypeModel), it)
        } ?: TransformDomainDefault.fromGrammarModel(p.grammarModel!!, p.baseTypeModel)
    },
    override var crossReferenceResolver: CrossReferenceResolver<AsmType, ContextType>? = { p ->
        CrossReferenceModelDefault.fromString(ContextFromTypeModel(p.typesModel), p.configuration.crossReferenceString ?: CrossReferenceString(""))
    },
    override var syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>? = null,
    override var semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>? = null,
    override var formatResolver: FormatResolver<AsmType, ContextType>? = { p ->
        AglFormatModelDefault.fromString(ContextFromTypeModel(p.typesModel), p.configuration.formatString ?: FormatString(""))
    },
    override var styleResolver: StyleResolver<AsmType, ContextType>? = { p ->
        AglStyleModelDefault.fromString(contextFromGrammar(p.grammarModel!!), p.configuration.styleString ?: StyleString(""))
    },
    override var completionProviderResolver: CompletionProviderResolver<AsmType, ContextType>? = null,
) : LanguageProcessorConfiguration<AsmType, ContextType>

internal class LanguageProcessorConfigurationSimple(
    override var targetGrammarName: SimpleName? = null,
    override var defaultGoalRuleName: GrammarRuleName? = null,

    override val grammarString: GrammarString? = null,
    override val typesString: TypesString? = null,
    override val transformString: TransformString? = null,
    override val crossReferenceString: CrossReferenceString? = null,
    override val styleString: StyleString? = null,
    override val formatString: FormatString? = null,

    override val regexEngineKind: RegexEngineKind = RegexEngineKind.PLATFORM,
    override val scannerKind: ScannerKind = ScannerKind.OnDemand,
    override val scannerResolver: ScannerResolver<Asm, ContextWithScope<Any, Any>>? = {
        val regexEngine = when (regexEngineKind) {
            RegexEngineKind.PLATFORM -> RegexEnginePlatform
            RegexEngineKind.AGL -> RegexEngineAgl
        }
        val scanner = when (scannerKind) {
            ScannerKind.Classic -> ScannerClassic(regexEngine, it.targetRuleSet?.let { it.terminals + it.embeddedTerminals } ?: emptyList())
            ScannerKind.OnDemand -> ScannerOnDemand(regexEngine, it.targetRuleSet?.let { it.terminals + it.embeddedTerminals } ?: emptyList())
        }
        ProcessResultDefault(scanner, IssueHolder(LanguageProcessorPhase.ALL))
    },
    override val parserResolver: ParserResolver<Asm, ContextWithScope<Any, Any>>? = { p ->
        ProcessResultDefault(
            p.targetRuleSet?.let { LeftCornerParser(p.scanner!!, it) },
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var typesResolver: TypesResolver<Asm, ContextWithScope<Any, Any>>? = { p ->
        TypeModelSimple.fromString(SimpleName("FromGrammar"+p.grammarModel!!.name.value), contextFromGrammar(p.grammarModel!!),p.configuration.typesString ?: TypesString(""))
    },
    //override val asmFactoryResolver: AsmFactoryResolver<AsmFactorySimple>? = { AsmFactorySimple() },
    override val transformResolver: TransformResolver<Asm, ContextWithScope<Any, Any>>? = { p ->
        p.configuration.transformString?.let {
            TransformDomainDefault.fromString(ContextFromGrammarAndTypeModel(p.grammarModel!!, p.baseTypeModel), it)
        } ?: TransformDomainDefault.fromGrammarModel(p.grammarModel!!, p.baseTypeModel)
    },
    override var crossReferenceResolver: CrossReferenceResolver<Asm, ContextWithScope<Any, Any>>? = { p ->
        CrossReferenceModelDefault.fromString(ContextFromTypeModel(p.typesModel), p.configuration.crossReferenceString ?: CrossReferenceString(""))
    },
    override var syntaxAnalyserResolver: SyntaxAnalyserResolver<Asm, ContextWithScope<Any, Any>>? = { p ->
        ProcessResultDefault(
            SyntaxAnalyserSimple(p.typesModel, p.asmTransformModel, p.targetAsmTransformRuleSet.qualifiedName), //FIXME
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var semanticAnalyserResolver: SemanticAnalyserResolver<Asm, ContextWithScope<Any, Any>>? = { p ->
        ProcessResultDefault(
            SemanticAnalyserSimple(p.typesModel, p.crossReferenceModel),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var formatResolver: FormatResolver<Asm, ContextWithScope<Any, Any>>? = { p ->
        AglFormatModelDefault.fromString(ContextFromTypeModel(p.typesModel), p.configuration.formatString ?: FormatString(""))
    },
    override var styleResolver: StyleResolver<Asm, ContextWithScope<Any, Any>>? = { p ->
        AglStyleModelDefault.fromString(contextFromGrammar(p.grammarModel!!), p.configuration.styleString ?: StyleString(""))
    },
    override var completionProviderResolver: CompletionProviderResolver<Asm, ContextWithScope<Any, Any>>? = { p ->
        ProcessResultDefault(
            CompletionProviderSimple(p.targetGrammar!!, Grammar2TransformRuleSet.defaultConfiguration, p.typesModel, p.crossReferenceModel),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    }
) : LanguageProcessorConfiguration<Asm, ContextWithScope<Any, Any>>