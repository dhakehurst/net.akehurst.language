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
import net.akehurst.language.agl.language.asmTransform.AsmTransform
import net.akehurst.language.agl.language.asmTransform.AsmTransformCompletionProvider
import net.akehurst.language.agl.language.base.AglBase
import net.akehurst.language.agl.language.expressions.AglExpressions
import net.akehurst.language.agl.language.expressions.ExpressionsSemanticAnalyser
import net.akehurst.language.agl.language.expressions.ExpressionsSyntaxAnalyser
import net.akehurst.language.agl.language.format.AglFormat
import net.akehurst.language.agl.language.format.AglFormatCompletionProvider
import net.akehurst.language.agl.language.format.AglFormatSemanticAnalyser
import net.akehurst.language.agl.language.format.AglFormatSyntaxAnalyser
import net.akehurst.language.agl.language.grammar.*
import net.akehurst.language.agl.language.reference.AglCrossReferences
import net.akehurst.language.agl.language.reference.ReferencesCompletionProvider
import net.akehurst.language.agl.language.reference.ReferencesSemanticAnalyser
import net.akehurst.language.agl.language.reference.ReferencesSyntaxAnalyser
import net.akehurst.language.agl.language.style.AglStyle
import net.akehurst.language.agl.language.style.AglStyleCompletionProvider
import net.akehurst.language.agl.language.style.AglStyleSemanticAnalyser
import net.akehurst.language.agl.language.style.AglStyleSyntaxAnalyser
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.api.language.asmTransform.TransformModel
import net.akehurst.language.api.language.base.DefinitionBlock
import net.akehurst.language.api.language.base.Namespace
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.language.grammar.Grammar
import net.akehurst.language.api.language.reference.CrossReferenceModel
import net.akehurst.language.api.language.style.AglStyleModel
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.formatter.api.AglFormatterModel

interface AglLanguages {
    val expressionsLanguageIdentity: QualifiedName
    val grammarLanguageIdentity: QualifiedName
    val asmTransformLanguageIdentity: QualifiedName
    val crossReferenceLanguageIdentity: QualifiedName
    val styleLanguageIdentity: QualifiedName
    val formatLanguageIdentity: QualifiedName

    val expressions: LanguageDefinition<Expression, SentenceContext<String>>
    val grammar: LanguageDefinition<DefinitionBlock<Grammar>, ContextFromGrammarRegistry>
    val asmTransform: LanguageDefinition<List<TransformModel>, ContextFromGrammar>
    val crossReference: LanguageDefinition<CrossReferenceModel, ContextFromTypeModel>
    val style: LanguageDefinition<AglStyleModel, ContextFromGrammar>
    val formatter: LanguageDefinition<AglFormatterModel, SentenceContext<String>>
}

class LanguageRegistryDefault : LanguageRegistry {

    private val _grammars = mutableMapOf<QualifiedName, Grammar>()
    private val _registry = mutableMapOf<QualifiedName, LanguageDefinition<*, *>>()

    override val agl: AglLanguages = object : AglLanguages {
        val baseLanguageIdentity: QualifiedName = AglBase.grammar.qualifiedName
        override val expressionsLanguageIdentity: QualifiedName = AglExpressions.grammar.qualifiedName
        override val grammarLanguageIdentity: QualifiedName = AglGrammar.grammar.qualifiedName
        override val asmTransformLanguageIdentity: QualifiedName = AsmTransform.grammar.qualifiedName
        override val styleLanguageIdentity: QualifiedName = AglStyle.grammar.qualifiedName
        override val formatLanguageIdentity: QualifiedName = AglFormat.grammar.qualifiedName
        override val crossReferenceLanguageIdentity: QualifiedName = AglCrossReferences.grammar.qualifiedName

        val base: LanguageDefinition<Any, SentenceContext<String>> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm<Any, SentenceContext<String>>(
                identity = baseLanguageIdentity,
                AglBase.grammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(AglBase.grammar.name)
                    defaultGoalRuleName(AglBase.goalRuleName)
                    //scannerResolver { ProcessResultDefault(ScannerOnDemand(RegexEnginePlatform, it.ruleSet.terminals), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //parserResolver { ProcessResultDefault(LeftCornerParser(it.scanner!!, it.ruleSet), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(it.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //asmTransformResolver { AsmTransformModelSimple.fromGrammar(it.grammar!!, it.typeModel) }
                    //crossReferenceModelResolver { ProcessResultDefault(CrossReferenceModelDefault(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver { ProcessResultDefault(ExpressionsSyntaxAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //semanticAnalyserResolver { ProcessResultDefault(ExpressionsSemanticAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //formatterResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AglBase.styleStr
                        )
                    }
                    //completionProvider { ProcessResultDefault(ExpressionsCompletionProvider(), IssueHolder(LanguageProcessorPhase.ALL)) }
                }
            )
        )

        override val expressions: LanguageDefinition<Expression, SentenceContext<String>> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm<Expression, SentenceContext<String>>(
                identity = expressionsLanguageIdentity,
                AglExpressions.grammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(AglExpressions.grammar.name)
                    defaultGoalRuleName(AglExpressions.goalRuleName)
                    //scannerResolver { ProcessResultDefault(ScannerOnDemand(RegexEnginePlatform, it.ruleSet.terminals), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //parserResolver { ProcessResultDefault(LeftCornerParser(it.scanner!!, it.ruleSet), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(it.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //crossReferenceModelResolver { ProcessResultDefault(CrossReferenceModelDefault(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver { ProcessResultDefault(ExpressionsSyntaxAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    semanticAnalyserResolver { ProcessResultDefault(ExpressionsSemanticAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //formatterResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AglExpressions.styleStr
                        )
                    }
                    completionProvider { ProcessResultDefault(ExpressionsCompletionProvider(), IssueHolder(LanguageProcessorPhase.ALL)) }
                }
            )
        )

        override val grammar: LanguageDefinition<DefinitionBlock<Grammar>, ContextFromGrammarRegistry> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = grammarLanguageIdentity,
                AglGrammar.grammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(AglGrammar.grammar.name)
                    defaultGoalRuleName(AglGrammar.goalRuleName)
                    // scannerResolver { ProcessResultDefault(ScannerOnDemand(RegexEnginePlatform, it.ruleSet.terminals), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //parserResolver { ProcessResultDefault(LeftCornerParser(it.scanner!!, it.ruleSet), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(it.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //crossReferenceModelResolver { ProcessResultDefault(CrossReferenceModelDefault(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver { ProcessResultDefault(AglGrammarSyntaxAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    semanticAnalyserResolver { ProcessResultDefault(AglGrammarSemanticAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //formatterResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AglGrammar.styleStr
                        )
                    }
                    completionProvider { ProcessResultDefault(AglGrammarCompletionProvider(), IssueHolder(LanguageProcessorPhase.ALL)) }
                }
            )
        )

        override val asmTransform: LanguageDefinition<List<TransformModel>, ContextFromGrammar> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = asmTransformLanguageIdentity,
                AsmTransform.grammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(AsmTransform.grammar.name)
                    defaultGoalRuleName(AsmTransform.goalRuleName)
                    //scannerResolver { ProcessResultDefault(ScannerOnDemand(RegexEnginePlatform, it.ruleSet.terminals), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //parserResolver { ProcessResultDefault(LeftCornerParser(it.scanner!!, it.ruleSet), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(it.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //crossReferenceModelResolver { ProcessResultDefault(CrossReferenceModelDefault(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver { ProcessResultDefault(AsmTransformSyntaxAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    semanticAnalyserResolver { ProcessResultDefault(AsmTransformSemanticAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //formatterResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AsmTransform.styleStr
                        )
                    }
                    completionProvider { ProcessResultDefault(AsmTransformCompletionProvider(), IssueHolder(LanguageProcessorPhase.ALL)) }
                }
            )
        )

        override val crossReference: LanguageDefinition<CrossReferenceModel, ContextFromTypeModel> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm<CrossReferenceModel, ContextFromTypeModel>(
                identity = crossReferenceLanguageIdentity,
                AglCrossReferences.grammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(AglCrossReferences.grammar.name)
                    defaultGoalRuleName(AglCrossReferences.goalRuleName)
                    //scannerResolver { ProcessResultDefault(ScannerOnDemand(RegexEnginePlatform, it.ruleSet.terminals), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //parserResolver { ProcessResultDefault(LeftCornerParser(it.scanner!!, it.ruleSet), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(it.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //crossReferenceModelResolver { ProcessResultDefault(CrossReferenceModelDefault(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver { ProcessResultDefault(ReferencesSyntaxAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    semanticAnalyserResolver { ProcessResultDefault(ReferencesSemanticAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //formatterResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AglCrossReferences.styleStr
                        )
                    }
                    completionProvider { ProcessResultDefault(ReferencesCompletionProvider(), IssueHolder(LanguageProcessorPhase.ALL)) }
                }
            )
        )

        override val formatter = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm<AglFormatterModel, SentenceContext<String>>(
                identity = formatLanguageIdentity,
                AglFormat.grammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(AglFormat.grammar.name)
                    defaultGoalRuleName(AglFormat.goalRuleName)
                    //scannerResolver { ProcessResultDefault(ScannerOnDemand(RegexEnginePlatform, it.ruleSet.terminals), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //parserResolver { ProcessResultDefault(LeftCornerParser(it.scanner!!, it.ruleSet), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(it.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //crossReferenceModelResolver { ProcessResultDefault(CrossReferenceModelDefault(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver {
                        ProcessResultDefault(
                            AglFormatSyntaxAnalyser(it.grammar!!.qualifiedName, it.typeModel, it.asmTransformModel),
                            IssueHolder(LanguageProcessorPhase.ALL)
                        )
                    }
                    semanticAnalyserResolver { ProcessResultDefault(AglFormatSemanticAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //formatterResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AglFormat.styleStr
                        )
                    }
                    completionProvider { ProcessResultDefault(AglFormatCompletionProvider(), IssueHolder(LanguageProcessorPhase.ALL)) }
                }
            )
        )

        override val style: LanguageDefinition<AglStyleModel, ContextFromGrammar> = this@LanguageRegistryDefault.registerFromDefinition(
            LanguageDefinitionFromAsm(
                identity = styleLanguageIdentity,
                AglStyle.grammar,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configuration {
                    targetGrammarName(AglStyle.grammar.name)
                    defaultGoalRuleName(AglStyle.goalRuleName)
                    //scannerResolver { ProcessResultDefault(ScannerOnDemand(RegexEnginePlatform, it.ruleSet.terminals), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //parserResolver { ProcessResultDefault(LeftCornerParser(it.scanner!!, it.ruleSet), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(it.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //crossReferenceModelResolver { ProcessResultDefault(CrossReferenceModelDefault(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    syntaxAnalyserResolver { ProcessResultDefault(AglStyleSyntaxAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    semanticAnalyserResolver { ProcessResultDefault(AglStyleSemanticAnalyser(), IssueHolder(LanguageProcessorPhase.ALL)) }
                    //formatterResolver {  }
                    styleResolver {
                        Agl.fromString(
                            Agl.registry.agl.style.processor!!,
                            Agl.registry.agl.style.processor!!.optionsDefault(),
                            AglStyle.styleStr
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
        identity: QualifiedName,
        grammarStr: String?,
        aglOptions: ProcessOptions<DefinitionBlock<Grammar>, ContextFromGrammarRegistry>?,
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

    override fun unregister(identity: QualifiedName) {
        this._registry.remove(identity)
    }

    override fun <AsmType : Any, ContextType : Any> findOrNull(identity: QualifiedName): LanguageDefinition<AsmType, ContextType>? {
        return this._registry[identity] as LanguageDefinition<AsmType, ContextType>?
    }

    /**
     * try to find localNamespace.nameOrQName or if not found try to find nameOrQName
     */
    fun <AsmType : Any, ContextType : Any> findWithNamespaceOrNull(localNamespace: String, nameOrQName: String): LanguageDefinition<AsmType, ContextType>? {
        return findOrNull(QualifiedName("$localNamespace.$nameOrQName")) ?: findOrNull(QualifiedName(nameOrQName))
    }

    override fun <AsmType : Any, ContextType : Any> findOrPlaceholder(
        identity: QualifiedName,
        aglOptions: ProcessOptions<DefinitionBlock<Grammar>, ContextFromGrammarRegistry>?,
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

    fun findGrammarOrNullByQualifiedName(qualifiedName: QualifiedName): Grammar? = _grammars[qualifiedName]

    override fun findGrammarOrNull(localNamespace: Namespace<Grammar>, nameOrQName: String): Grammar? =
        findGrammarOrNullByQualifiedName(QualifiedName("${localNamespace.qualifiedName}.$nameOrQName")) ?: findGrammarOrNullByQualifiedName(QualifiedName(nameOrQName))

    override fun registerGrammar(grammar: Grammar) {
        _grammars[grammar.qualifiedName] = grammar
    }
}