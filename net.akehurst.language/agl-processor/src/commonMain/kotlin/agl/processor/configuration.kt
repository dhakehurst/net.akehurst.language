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

import net.akehurst.language.agl.default_.*
import net.akehurst.language.agl.grammarTypeModel.grammarTypeModel
import net.akehurst.language.transform.asm.TransformModelDefault
import net.akehurst.language.format.asm.AglFormatterModelFromAsm
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.api.asm.Asm
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.scanner.ScannerKind
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEngineAgl
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.regex.api.RegexEngineKind
import net.akehurst.language.scanner.common.ScannerClassic
import net.akehurst.language.scanner.common.ScannerOnDemand
import net.akehurst.language.style.asm.AglStyleModelDefault
import net.akehurst.language.typemodel.api.TypeModel

internal class LanguageProcessorConfigurationEmpty<AsmType : Any, ContextType : Any>(
    override var targetGrammarName: SimpleName? = null,
    override var defaultGoalRuleName: GrammarRuleName? = null,
    override val regexEngineKind: RegexEngineKind = RegexEngineKind.PLATFORM,
    override val scannerKind: ScannerKind = ScannerKind.OnDemand,
    override val scannerResolver: ScannerResolver<AsmType, ContextType>? = null,
    override val parserResolver: ParserResolver<AsmType, ContextType>? = null,
    override var asmTransformModelResolver: AsmTransformModelResolver<AsmType, ContextType>? = null,
    override var typeModelResolver: TypeModelResolver<AsmType, ContextType>? = null,
    override var crossReferenceModelResolver: CrossReferenceModelResolver<AsmType, ContextType>? = null,
    override var syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>? = null,
    override var semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>? = null,
    override var formatterResolver: FormatterResolver<AsmType, ContextType>? = null,
    override var styleResolver: StyleResolver<AsmType, ContextType>? = null,
    override var completionProvider: CompletionProviderResolver<AsmType, ContextType>? = null
) : LanguageProcessorConfiguration<AsmType, ContextType>

internal class LanguageProcessorConfigurationBase<AsmType : Any, ContextType : Any>(
    override var targetGrammarName: SimpleName? = null,
    override var defaultGoalRuleName: GrammarRuleName? = null,
    override val regexEngineKind: RegexEngineKind = RegexEngineKind.PLATFORM,
    override val scannerKind: ScannerKind = ScannerKind.OnDemand,
    override val scannerResolver: ScannerResolver<AsmType, ContextType>? = {
        val regexEngine = when (regexEngineKind) {
            RegexEngineKind.PLATFORM -> RegexEnginePlatform
            RegexEngineKind.AGL -> RegexEngineAgl
        }
        val scanner = when (scannerKind) {
            ScannerKind.Classic -> ScannerClassic(regexEngine, it.ruleSet.terminals)
            ScannerKind.OnDemand -> ScannerOnDemand(regexEngine, it.ruleSet.terminals)
        }
        ProcessResultDefault(scanner, IssueHolder(LanguageProcessorPhase.ALL))
    },
    override val parserResolver: ParserResolver<AsmType, ContextType>? = {
        ProcessResultDefault(
            LeftCornerParser(it.scanner!!, it.ruleSet),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var typeModelResolver: TypeModelResolver<AsmType, ContextType>? = { p ->
        ProcessResultDefault<TypeModel>(
//            TypeModelFromGrammar.create(p.grammar!!),
            grammarTypeModel(p.grammar!!.qualifiedName.value, p.grammar!!.name.value) {},
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var asmTransformModelResolver: AsmTransformModelResolver<AsmType, ContextType>? = { p ->
        TransformModelDefault.fromGrammar(p.grammar!!, p.baseTypeModel)
    },
    override var crossReferenceModelResolver: CrossReferenceModelResolver<AsmType, ContextType>? = { p ->
        ProcessResultDefault(
            CrossReferenceModelDefault(),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var syntaxAnalyserResolver: SyntaxAnalyserResolver<AsmType, ContextType>? = null,
    override var semanticAnalyserResolver: SemanticAnalyserResolver<AsmType, ContextType>? = null,
    override var formatterResolver: FormatterResolver<AsmType, ContextType>? = { p ->
        ProcessResultDefault(
            null,
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var styleResolver: StyleResolver<AsmType, ContextType>? = { p ->
        ProcessResultDefault(
            AglStyleModelDefault(SimpleName("DefaultStyles"), emptyList()),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var completionProvider: CompletionProviderResolver<AsmType, ContextType>? = null,
) : LanguageProcessorConfiguration<AsmType, ContextType>

internal class LanguageProcessorConfigurationDefault(
    override var targetGrammarName: SimpleName? = null,
    override var defaultGoalRuleName: GrammarRuleName? = null,
    override val regexEngineKind: RegexEngineKind = RegexEngineKind.PLATFORM,
    override val scannerKind: ScannerKind = ScannerKind.OnDemand,
    override val scannerResolver: ScannerResolver<Asm, ContextAsmDefault>? = {
        val regexEngine = when (regexEngineKind) {
            RegexEngineKind.PLATFORM -> RegexEnginePlatform
            RegexEngineKind.AGL -> RegexEngineAgl
        }
        val scanner = when (scannerKind) {
            ScannerKind.Classic -> ScannerClassic(regexEngine, it.ruleSet.terminals)
            ScannerKind.OnDemand -> ScannerOnDemand(regexEngine, it.ruleSet.terminals)
        }
        ProcessResultDefault(scanner, IssueHolder(LanguageProcessorPhase.ALL))
    },
    override val parserResolver: ParserResolver<Asm, ContextAsmDefault>? = {
        ProcessResultDefault(
            LeftCornerParser(it.scanner!!, it.ruleSet),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var typeModelResolver: TypeModelResolver<Asm, ContextAsmDefault>? = { p ->
        ProcessResultDefault<TypeModel>(
//            TypeModelFromGrammar.create(p.grammar!!),
            grammarTypeModel(p.grammar!!.qualifiedName.value, p.grammar!!.name.value) {},
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override val asmTransformModelResolver: AsmTransformModelResolver<Asm, ContextAsmDefault>? = { p ->
        TransformModelDefault.fromGrammar(p.grammar!!, p.baseTypeModel)
    },
    override var crossReferenceModelResolver: CrossReferenceModelResolver<Asm, ContextAsmDefault>? = { p ->
        CrossReferenceModelDefault.fromString(
            ContextFromTypeModel(p.typeModel),
            ""
        )
    },
    override var syntaxAnalyserResolver: SyntaxAnalyserResolver<Asm, ContextAsmDefault>? = { p ->
        ProcessResultDefault(
            SyntaxAnalyserDefault(p.typeModel, p.asmTransformModel, p.grammar!!.qualifiedName),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var semanticAnalyserResolver: SemanticAnalyserResolver<Asm, ContextAsmDefault>? = { p ->
        ProcessResultDefault(
            SemanticAnalyserDefault(p.typeModel, p.crossReferenceModel),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var formatterResolver: FormatterResolver<Asm, ContextAsmDefault>? = { p ->
        AglFormatterModelFromAsm.fromString(ContextFromTypeModel(p.typeModel), "")
    },
    override var styleResolver: StyleResolver<Asm, ContextAsmDefault>? = { p ->
        ProcessResultDefault(
            AglStyleModelDefault(SimpleName("DefaultStyles"), emptyList()),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var completionProvider: CompletionProviderResolver<Asm, ContextAsmDefault>? = { p ->
        ProcessResultDefault(
            CompletionProviderDefault(p.grammar!!, Grammar2TransformRuleSet.defaultConfiguration, p.typeModel, p.crossReferenceModel),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    }
) : LanguageProcessorConfiguration<Asm, ContextAsmDefault>