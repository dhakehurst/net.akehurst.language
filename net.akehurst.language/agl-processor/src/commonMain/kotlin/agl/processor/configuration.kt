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

import net.akehurst.language.agl.semanticAnalyser.ContextFromTypesDomain
import net.akehurst.language.agl.semanticAnalyser.contextFromTypesDomain
import net.akehurst.language.agl.simple.*
import net.akehurst.language.api.processor.*
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asmTransform.asm.AsmTransformDomainDefault
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.format.asm.AglFormatDomainDefault
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.grammar.processor.contextFromGrammar
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import net.akehurst.language.regex.agl.RegexEngineAgl
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.regex.api.RegexEngineKind
import net.akehurst.language.scanner.api.ScannerKind
import net.akehurst.language.scanner.common.ScannerClassic
import net.akehurst.language.scanner.common.ScannerOnDemand
import net.akehurst.language.style.asm.AglStyleDomainDefault
import net.akehurst.language.types.asm.TypesDomainSimple


internal class LanguageProcessorConfigurationEmpty<AsmType : Any, ContextType : Any>(
    override var targetGrammarName: SimpleName? = null,
    override var defaultGoalRuleName: GrammarRuleName? = null,

    override val grammarString: GrammarString? = null,
    override val typesString: TypesString? = null,
    override val asmTransformString: AsmTransformString? = null,
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
    override val asmTransformString: AsmTransformString? = null,
    override val crossReferenceString: CrossReferenceString? = null,
    override val styleString: StyleString? = null,
    override val formatString: FormatString? = null,

    override val regexEngineKind: RegexEngineKind = RegexEngineKind.PLATFORM,
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
        ProcessResultDefault(scanner)
    },
    override val parserResolver: ParserResolver<AsmType, ContextType>? = { p ->
        ProcessResultDefault(p.targetRuleSet?.let { LeftCornerParser(p.scanner!!, it) })
    },
    override var typesResolver: TypesResolver<AsmType, ContextType>? = { p ->
        TypesDomainSimple.fromString(SimpleName("FromGrammar" + p.grammarDomain!!.name.value), contextFromGrammar(p.grammarDomain!!), p.configuration.typesString ?: TypesString(""))
    },
    //override val asmFactoryResolver: AsmFactoryResolver<AsmFactory<AsmType,*,*>>? = null,
    override var transformResolver: TransformResolver<AsmType, ContextType>? = { p ->
        p.configuration.asmTransformString?.let {
            AsmTransformDomainDefault.fromString(ContextFromGrammarAndTypesDomain(p.grammarDomain!!, p.baseTypesDomain), it)
        } ?: AsmTransformDomainDefault.fromGrammarDomain(p.grammarDomain!!, p.baseTypesDomain)
    },
    override var crossReferenceResolver: CrossReferenceResolver<AsmType, ContextType>? = { p ->
        CrossReferenceDomainDefault.fromString(ContextFromTypesDomain(p.typesDomain), p.configuration.crossReferenceString ?: CrossReferenceString(""))
    },
    override var syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>? = null,
    override var semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>? = null,
    override var formatResolver: FormatResolver<AsmType, ContextType>? = { p ->
        AglFormatDomainDefault.fromString(contextFromTypesDomain(p.typesDomain), p.configuration.formatString ?: FormatString(""))
    },
    override var styleResolver: StyleResolver<AsmType, ContextType>? = { p ->
        AglStyleDomainDefault.fromString(contextFromGrammar(p.grammarDomain!!), p.configuration.styleString ?: StyleString(""))
    },
    override var completionProviderResolver: CompletionProviderResolver<AsmType, ContextType>? = null,
) : LanguageProcessorConfiguration<AsmType, ContextType>

internal class LanguageProcessorConfigurationSimple(
    override var targetGrammarName: SimpleName? = null,
    override var defaultGoalRuleName: GrammarRuleName? = null,

    override val grammarString: GrammarString? = null,
    override val typesString: TypesString? = null,
    override val asmTransformString: AsmTransformString? = null,
    override val crossReferenceString: CrossReferenceString? = null,
    override val styleString: StyleString? = null,
    override val formatString: FormatString? = null,

    override val regexEngineKind: RegexEngineKind = RegexEngineKind.PLATFORM,
    override val scannerKind: ScannerKind = ScannerKind.OnDemand,
    override val scannerResolver: ScannerResolver<Asm, SentenceContextAny>? = {
        val regexEngine = when (regexEngineKind) {
            RegexEngineKind.PLATFORM -> RegexEnginePlatform
            RegexEngineKind.AGL -> RegexEngineAgl
        }
        val scanner = when (scannerKind) {
            ScannerKind.Classic -> ScannerClassic(regexEngine, it.targetRuleSet?.let { it.terminals + it.embeddedTerminals } ?: emptyList())
            ScannerKind.OnDemand -> ScannerOnDemand(regexEngine, it.targetRuleSet?.let { it.terminals + it.embeddedTerminals } ?: emptyList())
        }
        ProcessResultDefault(scanner)
    },
    override val parserResolver: ParserResolver<Asm, SentenceContextAny>? = { p ->
        ProcessResultDefault(
            p.targetRuleSet?.let { LeftCornerParser(p.scanner!!, it) },
        )
    },
    override var typesResolver: TypesResolver<Asm, SentenceContextAny>? = { p ->
        TypesDomainSimple.fromString(SimpleName("FromGrammar" + p.grammarDomain!!.name.value), contextFromGrammar(p.grammarDomain!!), p.configuration.typesString ?: TypesString(""))
    },
    //override val asmFactoryResolver: AsmFactoryResolver<AsmFactorySimple>? = { AsmFactorySimple() },
    override val transformResolver: TransformResolver<Asm, SentenceContextAny>? = { p ->
        p.configuration.asmTransformString?.let {
            AsmTransformDomainDefault.fromString(ContextFromGrammarAndTypesDomain(p.grammarDomain!!, p.baseTypesDomain), it)
        } ?: AsmTransformDomainDefault.fromGrammarDomain(p.grammarDomain!!, p.baseTypesDomain)
    },
    override var crossReferenceResolver: CrossReferenceResolver<Asm, SentenceContextAny>? = { p ->
        CrossReferenceDomainDefault.fromString(ContextFromTypesDomain(p.typesDomain), p.configuration.crossReferenceString ?: CrossReferenceString(""))
    },
    override var syntaxAnalyserResolver: SyntaxAnalyserResolver<Asm, SentenceContextAny>? = { p ->
        ProcessResultDefault(
            SyntaxAnalyserSimple(p.typesDomain, p.transformDomain, p.targetTransformRuleSet.qualifiedName), //FIXME
        )
    },
    override var semanticAnalyserResolver: SemanticAnalyserResolver<Asm, SentenceContextAny>? = { p ->
        ProcessResultDefault(
            SemanticAnalyserSimple(p.typesDomain, p.crossReferenceDomain),
        )
    },
    override var formatResolver: FormatResolver<Asm, SentenceContextAny>? = { p ->
        AglFormatDomainDefault.fromString(contextFromTypesDomain(p.typesDomain), p.configuration.formatString ?: FormatString(""))
    },
    override var styleResolver: StyleResolver<Asm, SentenceContextAny>? = { p ->
        AglStyleDomainDefault.fromString(contextFromGrammar(p.grammarDomain!!), p.configuration.styleString ?: StyleString(""))
    },
    override var completionProviderResolver: CompletionProviderResolver<Asm, SentenceContextAny>? = { p ->
        ProcessResultDefault(
            CompletionProviderSimple(p.targetGrammar!!, Grammar2TransformRuleSet.defaultConfiguration, p.typesDomain, p.crossReferenceDomain),
        )
    }
) : LanguageProcessorConfiguration<Asm, SentenceContextAny>