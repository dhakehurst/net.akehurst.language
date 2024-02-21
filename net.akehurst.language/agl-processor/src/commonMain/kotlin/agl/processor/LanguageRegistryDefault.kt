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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.agl.language.asmTransform.AsmTransformSemanticAnalyser
import net.akehurst.language.agl.agl.language.asmTransform.AsmTransformSyntaxAnalyser
import net.akehurst.language.agl.agl.language.expressions.ExpressionsCompletionProvider
import net.akehurst.language.agl.default.TypeModelFromGrammar
import net.akehurst.language.agl.language.asmTransform.AsmTransformCompletionProvider
import net.akehurst.language.agl.language.asmTransform.AsmTransformGrammar
import net.akehurst.language.agl.language.base.BaseGrammar
import net.akehurst.language.agl.language.expressions.ExpressionsGrammar
import net.akehurst.language.agl.language.expressions.ExpressionsSemanticAnalyser
import net.akehurst.language.agl.language.expressions.ExpressionsSyntaxAnalyser
import net.akehurst.language.agl.language.format.AglFormatCompletionProvider
import net.akehurst.language.agl.language.format.AglFormatGrammar
import net.akehurst.language.agl.language.format.AglFormatSemanticAnalyser
import net.akehurst.language.agl.language.format.AglFormatSyntaxAnalyser
import net.akehurst.language.agl.language.grammar.*
import net.akehurst.language.agl.language.reference.ReferencesCompletionProvider
import net.akehurst.language.agl.language.reference.ReferencesGrammar
import net.akehurst.language.agl.language.reference.ReferencesSemanticAnalyser
import net.akehurst.language.agl.language.reference.ReferencesSyntaxAnalyser
import net.akehurst.language.agl.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.agl.language.style.AglStyleCompletionProvider
import net.akehurst.language.agl.language.style.AglStyleGrammar
import net.akehurst.language.agl.language.style.AglStyleSemanticAnalyser
import net.akehurst.language.agl.language.style.AglStyleSyntaxAnalyser
import net.akehurst.language.agl.parser.LeftCornerParser
import net.akehurst.language.agl.regex.RegexEnginePlatform
import net.akehurst.language.agl.scanner.ScannerOnDemand
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.api.language.asmTransform.AsmTransformModel
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.language.grammar.Grammar
import net.akehurst.language.api.language.grammar.Namespace
import net.akehurst.language.api.language.reference.CrossReferenceModel
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.style.AglStyleModel
import net.akehurst.language.formatter.api.AglFormatterModel

interface AglLanguages {
    val expressionsLanguageIdentity: String
    val grammarLanguageIdentity: String
    val asmTransformLanguageIdentity: String
    val crossReferenceLanguageIdentity: String
    val styleLanguageIdentity: String
    val formatLanguageIdentity: String

    val expressions: LanguageDefinition<Expression, SentenceContext<String>>
    val grammar: LanguageDefinition<List<Grammar>, ContextFromGrammarRegistry>
    val asmTransform: LanguageDefinition<List<AsmTransformModel>, ContextFromGrammar>
    val crossReference: LanguageDefinition<CrossReferenceModel, ContextFromTypeModel>
    val style: LanguageDefinition<AglStyleModel, ContextFromGrammar>
    val formatter: LanguageDefinition<AglFormatterModel, SentenceContext<String>>
}

class LanguageRegistryDefault : LanguageRegistry {

    private val _grammars = mutableMapOf<String, Grammar>()
    private val _registry = mutableMapOf<String, LanguageDefinition<*, *>>()

    override val agl: AglLanguages = object : AglLanguages {
        val baseLanguageIdentity: String = BaseGrammar.qualifiedName
        override val expressionsLanguageIdentity: String = ExpressionsGrammar.qualifiedName
        override val grammarLanguageIdentity: String = AglGrammarGrammar.qualifiedName
        override val asmTransformLanguageIdentity: String = AsmTransformGrammar.qualifiedName
        override val styleLanguageIdentity: String = AglStyleGrammar.qualifiedName
        override val formatLanguageIdentity: String = AglFormatGrammar.qualifiedName
        override val crossReferenceLanguageIdentity: String = ReferencesGrammar.qualifiedName

        val base: LanguageDefinition<Any, SentenceContext<String>> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm<Any, SentenceContext<String>>(
                identity = baseLanguageIdentity,
                BaseGrammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(BaseGrammar.name)
                    defaultGoalRuleName(BaseGrammar.goalRuleName)
                    scannerResolver { ProcessResultDefault(ScannerOnDemand(RegexEnginePlatform, it.ruleSet.terminals), IssueHolder(LanguageProcessorPhase.ALL)) }
                    parserResolver { ProcessResultDefault(LeftCornerParser(it.scanner!!, it.ruleSet), IssueHolder(LanguageProcessorPhase.ALL)) }
                    typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(BaseGrammar), IssueHolder(LanguageProcessorPhase.ALL)) }
                    crossReferenceModelResolver { ProcessResultDefault(CrossReferenceModelDefault(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver { ProcessResultDefault(ExpressionsSyntaxAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //semanticAnalyserResolver { ProcessResultDefault(ExpressionsSemanticAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    formatterResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            BaseGrammar.styleStr
                        )
                    }
                    //completionProvider { ProcessResultDefault(ExpressionsCompletionProvider(), IssueHolder(LanguageProcessorPhase.ALL)) }
                }
            )
        )

        override val expressions: LanguageDefinition<Expression, SentenceContext<String>> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm<Expression, SentenceContext<String>>(
                identity = expressionsLanguageIdentity,
                ExpressionsGrammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(ExpressionsGrammar.name)
                    defaultGoalRuleName(ExpressionsGrammar.goalRuleName)
                    scannerResolver { ProcessResultDefault(ScannerOnDemand(RegexEnginePlatform, it.ruleSet.terminals), IssueHolder(LanguageProcessorPhase.ALL)) }
                    parserResolver { ProcessResultDefault(LeftCornerParser(it.scanner!!, it.ruleSet), IssueHolder(LanguageProcessorPhase.ALL)) }
                    typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(ExpressionsGrammar), IssueHolder(LanguageProcessorPhase.ALL)) }
                    crossReferenceModelResolver { ProcessResultDefault(CrossReferenceModelDefault(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver { ProcessResultDefault(ExpressionsSyntaxAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    semanticAnalyserResolver { ProcessResultDefault(ExpressionsSemanticAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    formatterResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            ExpressionsGrammar.styleStr
                        )
                    }
                    completionProvider { ProcessResultDefault(ExpressionsCompletionProvider(), IssueHolder(LanguageProcessorPhase.ALL)) }
                }
            )
        )

        override val grammar: LanguageDefinition<List<Grammar>, ContextFromGrammarRegistry> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = grammarLanguageIdentity,
                AglGrammarGrammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(AglGrammarGrammar.name)
                    defaultGoalRuleName(AglGrammarGrammar.goalRuleName)
                    scannerResolver { ProcessResultDefault(ScannerOnDemand(RegexEnginePlatform, it.ruleSet.terminals), IssueHolder(LanguageProcessorPhase.ALL)) }
                    parserResolver { ProcessResultDefault(LeftCornerParser(it.scanner!!, it.ruleSet), IssueHolder(LanguageProcessorPhase.ALL)) }
                    typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(AglGrammarGrammar), IssueHolder(LanguageProcessorPhase.ALL)) }
                    crossReferenceModelResolver { ProcessResultDefault(CrossReferenceModelDefault(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver { ProcessResultDefault(AglGrammarSyntaxAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    semanticAnalyserResolver { ProcessResultDefault(AglGrammarSemanticAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    formatterResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AglGrammarGrammar.styleStr
                        )
                    }
                    completionProvider { ProcessResultDefault(AglGrammarCompletionProvider(), IssueHolder(LanguageProcessorPhase.ALL)) }
                }
            )
        )

        override val asmTransform: LanguageDefinition<List<AsmTransformModel>, ContextFromGrammar> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = asmTransformLanguageIdentity,
                AsmTransformGrammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(AsmTransformGrammar.name)
                    defaultGoalRuleName(AsmTransformGrammar.goalRuleName)
                    scannerResolver { ProcessResultDefault(ScannerOnDemand(RegexEnginePlatform, it.ruleSet.terminals), IssueHolder(LanguageProcessorPhase.ALL)) }
                    parserResolver { ProcessResultDefault(LeftCornerParser(it.scanner!!, it.ruleSet), IssueHolder(LanguageProcessorPhase.ALL)) }
                    typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(AsmTransformGrammar), IssueHolder(LanguageProcessorPhase.ALL)) }
                    crossReferenceModelResolver { ProcessResultDefault(CrossReferenceModelDefault(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver { ProcessResultDefault(AsmTransformSyntaxAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    semanticAnalyserResolver { ProcessResultDefault(AsmTransformSemanticAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    formatterResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AsmTransformGrammar.styleStr
                        )
                    }
                    completionProvider { ProcessResultDefault(AsmTransformCompletionProvider(), IssueHolder(LanguageProcessorPhase.ALL)) }
                }
            )
        )

        override val crossReference: LanguageDefinition<CrossReferenceModel, ContextFromTypeModel> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm<CrossReferenceModel, ContextFromTypeModel>(
                identity = crossReferenceLanguageIdentity,
                ReferencesGrammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(ReferencesGrammar.name)
                    defaultGoalRuleName(ReferencesGrammar.goalRuleName)
                    scannerResolver { ProcessResultDefault(ScannerOnDemand(RegexEnginePlatform, it.ruleSet.terminals), IssueHolder(LanguageProcessorPhase.ALL)) }
                    parserResolver { ProcessResultDefault(LeftCornerParser(it.scanner!!, it.ruleSet), IssueHolder(LanguageProcessorPhase.ALL)) }
                    typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(ReferencesGrammar), IssueHolder(LanguageProcessorPhase.ALL)) }
                    crossReferenceModelResolver { ProcessResultDefault(CrossReferenceModelDefault(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver { ProcessResultDefault(ReferencesSyntaxAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    semanticAnalyserResolver { ProcessResultDefault(ReferencesSemanticAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    formatterResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            ReferencesGrammar.styleStr
                        )
                    }
                    completionProvider { ProcessResultDefault(ReferencesCompletionProvider(), IssueHolder(LanguageProcessorPhase.ALL)) }
                }
            )
        )

        override val formatter = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm<AglFormatterModel, SentenceContext<String>>(
                identity = formatLanguageIdentity,
                AglFormatGrammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(AglFormatGrammar.name)
                    defaultGoalRuleName(AglFormatGrammar.goalRuleName)
                    scannerResolver { ProcessResultDefault(ScannerOnDemand(RegexEnginePlatform, it.ruleSet.terminals), IssueHolder(LanguageProcessorPhase.ALL)) }
                    parserResolver { ProcessResultDefault(LeftCornerParser(it.scanner!!, it.ruleSet), IssueHolder(LanguageProcessorPhase.ALL)) }
                    typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(AglFormatGrammar), IssueHolder(LanguageProcessorPhase.ALL)) }
                    crossReferenceModelResolver { ProcessResultDefault(CrossReferenceModelDefault(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver {
                        ProcessResultDefault(
                            AglFormatSyntaxAnalyser(it.grammar!!.qualifiedName, it.typeModel, it.crossReferenceModel),
                            IssueHolder(LanguageProcessorPhase.ALL)
                        )
                    }
                    semanticAnalyserResolver { ProcessResultDefault(AglFormatSemanticAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    formatterResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AglFormatGrammar.styleStr
                        )
                    }
                    completionProvider { ProcessResultDefault(AglFormatCompletionProvider(), IssueHolder(LanguageProcessorPhase.ALL)) }
                }
            )
        )

        override val style: LanguageDefinition<AglStyleModel, ContextFromGrammar> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = styleLanguageIdentity,
                AglStyleGrammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(AglStyleGrammar.name)
                    defaultGoalRuleName(AglStyleGrammar.goalRuleName)
                    scannerResolver { ProcessResultDefault(ScannerOnDemand(RegexEnginePlatform, it.ruleSet.terminals), IssueHolder(LanguageProcessorPhase.ALL)) }
                    parserResolver { ProcessResultDefault(LeftCornerParser(it.scanner!!, it.ruleSet), IssueHolder(LanguageProcessorPhase.ALL)) }
                    typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(it.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                    crossReferenceModelResolver { ProcessResultDefault(CrossReferenceModelDefault(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver { ProcessResultDefault(AglStyleSyntaxAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    semanticAnalyserResolver { ProcessResultDefault(AglStyleSemanticAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //formatterResolver {  }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AglStyleGrammar.styleStr
                        )
                    }
                    completionProvider { ProcessResultDefault(AglStyleCompletionProvider(), IssueHolder(LanguageProcessorPhase.ALL)) }
                }
            )
        )
    }

    fun <AsmType : Any, ContextType : Any> registerFromDefinition(definition: LanguageDefinition<AsmType, ContextType>): LanguageDefinition<AsmType, ContextType> {
        return if (this._registry.containsKey(definition.identity)) {
            error("LanguageDefinition '${definition.identity}' is already registered, please unregister the old one first")
        } else {
            this._registry[definition.identity] = definition
            definition
        }
    }

    override fun <AsmType : Any, ContextType : Any> register(
        identity: String,
        grammarStr: String?,
        aglOptions: ProcessOptions<List<Grammar>, ContextFromGrammarRegistry>?,
        buildForDefaultGoal: Boolean,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>
    ): LanguageDefinition<AsmType, ContextType> = this.registerFromDefinition(
        LanguageDefinitionDefault<AsmType, ContextType>(
            identity = identity,
            grammarStrArg = grammarStr,
            aglOptions = aglOptions,
            buildForDefaultGoal = buildForDefaultGoal,
            initialConfiguration = configuration
        )
    )

    override fun unregister(identity: String) {
        this._registry.remove(identity)
    }

    override fun <AsmType : Any, ContextType : Any> findOrNull(identity: String): LanguageDefinition<AsmType, ContextType>? {
        return this._registry[identity] as LanguageDefinition<AsmType, ContextType>?
    }

    /**
     * try to find localNamespace.nameOrQName or if not found try to find nameOrQName
     */
    fun <AsmType : Any, ContextType : Any> findWithNamespaceOrNull(localNamespace: String, nameOrQName: String): LanguageDefinition<AsmType, ContextType>? {
        return findOrNull("$localNamespace.$nameOrQName") ?: findOrNull(nameOrQName)
    }

    override fun <AsmType : Any, ContextType : Any> findOrPlaceholder(
        identity: String,
        aglOptions: ProcessOptions<List<Grammar>, ContextFromGrammarRegistry>?,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>?
    ): LanguageDefinition<AsmType, ContextType> {
        val existing = this.findOrNull<AsmType, ContextType>(identity)
        return if (null == existing) {
            val placeholder = LanguageDefinitionDefault<AsmType, ContextType>(
                identity = identity,
                grammarStrArg = null,
                aglOptions = aglOptions,
                buildForDefaultGoal = false,
                initialConfiguration = configuration ?: Agl.configurationEmpty()
            )
            registerFromDefinition(placeholder)
        } else {
            existing
        }
    }

    fun findGrammarOrNullByQualifiedName(qualifiedName: String): Grammar? = _grammars[qualifiedName]

    override fun findGrammarOrNull(localNamespace: Namespace, nameOrQName: String): Grammar? =
        findGrammarOrNullByQualifiedName("${localNamespace.qualifiedName}.$nameOrQName") ?: findGrammarOrNullByQualifiedName(nameOrQName)

    override fun registerGrammar(grammar: Grammar) {
        _grammars[grammar.qualifiedName] = grammar
    }
}