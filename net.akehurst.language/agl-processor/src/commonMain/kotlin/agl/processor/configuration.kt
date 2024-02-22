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

import net.akehurst.language.agl.default.CompletionProviderDefault
import net.akehurst.language.agl.default.SemanticAnalyserDefault
import net.akehurst.language.agl.default.SyntaxAnalyserDefault
import net.akehurst.language.agl.default.TypeModelFromGrammar
import net.akehurst.language.agl.language.asmTransform.AsmTransformModelSimple
import net.akehurst.language.agl.language.format.AglFormatterModelFromAsm
import net.akehurst.language.agl.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.agl.language.style.asm.AglStyleModelDefault
import net.akehurst.language.agl.parser.LeftCornerParser
import net.akehurst.language.agl.regex.RegexEngineAgl
import net.akehurst.language.agl.regex.RegexEnginePlatform
import net.akehurst.language.agl.scanner.ScannerClassic
import net.akehurst.language.agl.scanner.ScannerOnDemand
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.agl.semanticAnalyser.ContextSimple
import net.akehurst.language.api.asm.Asm
import net.akehurst.language.api.processor.*
import net.akehurst.language.typemodel.api.TypeModel

internal class LanguageProcessorConfigurationEmpty<AsmType : Any, ContextType : Any>(
    override var targetGrammarName: String? = null,
    override var defaultGoalRuleName: String? = null,
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
    override var targetGrammarName: String? = null,
    override var defaultGoalRuleName: String? = null,
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
            TypeModelFromGrammar.create(p.grammar!!),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var asmTransformModelResolver: AsmTransformModelResolver<AsmType, ContextType>? = { p ->
        AsmTransformModelSimple.fromGrammar(p.grammar!!, p.typeModel)
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
            AglStyleModelDefault(emptyList()),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var completionProvider: CompletionProviderResolver<AsmType, ContextType>? = null,
) : LanguageProcessorConfiguration<AsmType, ContextType>

internal class LanguageProcessorConfigurationDefault(
    override var targetGrammarName: String? = null,
    override var defaultGoalRuleName: String? = null,
    override val regexEngineKind: RegexEngineKind = RegexEngineKind.PLATFORM,
    override val scannerKind: ScannerKind = ScannerKind.OnDemand,
    override val scannerResolver: ScannerResolver<Asm, ContextSimple>? = {
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
    override val parserResolver: ParserResolver<Asm, ContextSimple>? = {
        ProcessResultDefault(
            LeftCornerParser(it.scanner!!, it.ruleSet),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var typeModelResolver: TypeModelResolver<Asm, ContextSimple>? = { p ->
        ProcessResultDefault<TypeModel>(
            TypeModelFromGrammar.create(p.grammar!!),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override val asmTransformModelResolver: AsmTransformModelResolver<Asm, ContextSimple>? = { p ->
        AsmTransformModelSimple.fromGrammar(p.grammar!!, p.baseTypeModel)
    },
    override var crossReferenceModelResolver: CrossReferenceModelResolver<Asm, ContextSimple>? = { p ->
        CrossReferenceModelDefault.fromString(
            ContextFromTypeModel(p.typeModel),
            ""
        )
    },
    override var syntaxAnalyserResolver: SyntaxAnalyserResolver<Asm, ContextSimple>? = { p ->
        ProcessResultDefault(
            SyntaxAnalyserDefault(p.grammar!!.qualifiedName, p.typeModel, p.asmTransformModel),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var semanticAnalyserResolver: SemanticAnalyserResolver<Asm, ContextSimple>? = { p ->
        ProcessResultDefault(
            SemanticAnalyserDefault(p.typeModel, p.crossReferenceModel),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var formatterResolver: FormatterResolver<Asm, ContextSimple>? = { p ->
        AglFormatterModelFromAsm.fromString(ContextFromTypeModel(p.typeModel), "")
    },
    override var styleResolver: StyleResolver<Asm, ContextSimple>? = { p ->
        ProcessResultDefault(
            AglStyleModelDefault(emptyList()),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    },
    override var completionProvider: CompletionProviderResolver<Asm, ContextSimple>? = { p ->
        ProcessResultDefault(
            CompletionProviderDefault(p.grammar!!, TypeModelFromGrammar.defaultConfiguration, p.typeModel, p.crossReferenceModel),
            IssueHolder(LanguageProcessorPhase.ALL)
        )
    }
) : LanguageProcessorConfiguration<Asm, ContextSimple>