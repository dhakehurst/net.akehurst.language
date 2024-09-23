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
import net.akehurst.language.transform.processor.AsmTransformSemanticAnalyser
import net.akehurst.language.transform.processor.AsmTransformSyntaxAnalyser
import net.akehurst.language.expressions.processor.ExpressionsCompletionProvider
import net.akehurst.language.transform.processor.AsmTransform
import net.akehurst.language.transform.processor.AsmTransformCompletionProvider
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.expressions.processor.ExpressionsSemanticAnalyser
import net.akehurst.language.expressions.processor.ExpressionsSyntaxAnalyser
import net.akehurst.language.format.processor.AglFormat
import net.akehurst.language.format.processor.AglFormatCompletionProvider
import net.akehurst.language.format.processor.AglFormatSemanticAnalyser
import net.akehurst.language.format.processor.AglFormatSyntaxAnalyser
import net.akehurst.language.grammar.processor.*
import net.akehurst.language.reference.processor.AglCrossReferences
import net.akehurst.language.reference.processor.ReferencesCompletionProvider
import net.akehurst.language.reference.processor.ReferencesSemanticAnalyser
import net.akehurst.language.reference.processor.ReferencesSyntaxAnalyser
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.base.api.Namespace
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.formatter.api.AglFormatterModel
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.style.processor.AglStyle
import net.akehurst.language.style.api.AglStyleModel
import net.akehurst.language.style.processor.AglStyleCompletionProvider
import net.akehurst.language.style.processor.AglStyleSemanticAnalyser
import net.akehurst.language.style.processor.AglStyleSyntaxAnalyser

interface AglLanguages {
    val expressionsLanguageIdentity: LanguageIdentity
    val grammarLanguageIdentity: LanguageIdentity
    val asmTransformLanguageIdentity: LanguageIdentity
    val crossReferenceLanguageIdentity: LanguageIdentity
    val styleLanguageIdentity: LanguageIdentity
    val formatLanguageIdentity: LanguageIdentity

    val base: LanguageDefinition<Any, SentenceContext<String>>
    val expressions: LanguageDefinition<Expression, SentenceContext<String>>
    val grammar: LanguageDefinition<GrammarModel, ContextFromGrammarRegistry>
    val asmTransform: LanguageDefinition<TransformModel, ContextFromGrammar>
    val crossReference: LanguageDefinition<CrossReferenceModel, ContextFromTypeModel>
    val style: LanguageDefinition<AglStyleModel, ContextFromGrammar>
    val formatter: LanguageDefinition<AglFormatterModel, SentenceContext<String>>
}

class LanguageRegistryDefault : LanguageRegistry {

    private val _grammars = mutableMapOf<QualifiedName, Grammar>()
    private val _registry = mutableMapOf<LanguageIdentity, LanguageDefinition<*, *>>()

    override val agl: AglLanguages = object : AglLanguages {
        val baseLanguageIdentity: LanguageIdentity =LanguageIdentity( AglBase.grammar.qualifiedName.value)
        override val expressionsLanguageIdentity: LanguageIdentity = LanguageIdentity( AglExpressions.grammar.qualifiedName.value)
        override val grammarLanguageIdentity: LanguageIdentity = LanguageIdentity( AglGrammar.grammar.qualifiedName.value)
        override val asmTransformLanguageIdentity: LanguageIdentity = LanguageIdentity( AsmTransform.grammar.qualifiedName.value)
        override val styleLanguageIdentity: LanguageIdentity = LanguageIdentity( AglStyle.grammar.qualifiedName.value)
        override val formatLanguageIdentity: LanguageIdentity = LanguageIdentity( AglFormat.grammar.qualifiedName.value)
        override val crossReferenceLanguageIdentity: LanguageIdentity =LanguageIdentity(  AglCrossReferences.grammar.qualifiedName.value)

        override val base: LanguageDefinition<Any, SentenceContext<String>> by lazy {
            this@LanguageRegistryDefault.registerFromDefinition(
                LanguageDefinitionFromAsm<Any, SentenceContext<String>>(
                    identity = baseLanguageIdentity,
                    AglBase.grammar,
                    buildForDefaultGoal = false,
                    initialConfiguration = Agl.configuration {
                        targetGrammarName(AglBase.grammar.name.value)
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
        }

        override val expressions: LanguageDefinition<Expression, SentenceContext<String>> by lazy {
            this@LanguageRegistryDefault.registerFromDefinition(
                LanguageDefinitionFromAsm<Expression, SentenceContext<String>>(
                    identity = expressionsLanguageIdentity,
                    AglExpressions.grammar,
                    buildForDefaultGoal = false,
                    initialConfiguration = Agl.configuration {
                        targetGrammarName(AglExpressions.grammar.name.value)
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
        }

        override val grammar: LanguageDefinition<GrammarModel, ContextFromGrammarRegistry> by lazy {
            this@LanguageRegistryDefault.registerFromDefinition(
                LanguageDefinitionFromAsm(
                    identity = grammarLanguageIdentity,
                    AglGrammar.grammar,
                    buildForDefaultGoal = false,
                    initialConfiguration = Agl.configuration {
                        targetGrammarName(AglGrammar.grammar.name.value)
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
        }

        override val asmTransform: LanguageDefinition<TransformModel, ContextFromGrammar> by lazy {
            this@LanguageRegistryDefault.registerFromDefinition(
                LanguageDefinitionFromAsm(
                    identity = asmTransformLanguageIdentity,
                    AsmTransform.grammar,
                    buildForDefaultGoal = false,
                    initialConfiguration = Agl.configuration {
                        targetGrammarName(AsmTransform.grammar.name.value)
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
        }

        override val crossReference: LanguageDefinition<CrossReferenceModel, ContextFromTypeModel> by lazy {
            this@LanguageRegistryDefault.registerFromDefinition(
                LanguageDefinitionFromAsm<CrossReferenceModel, ContextFromTypeModel>(
                    identity = crossReferenceLanguageIdentity,
                    AglCrossReferences.grammar,
                    buildForDefaultGoal = false,
                    initialConfiguration = Agl.configuration {
                        targetGrammarName(AglCrossReferences.grammar.name.value)
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
        }

        override val formatter by lazy {
            this@LanguageRegistryDefault.registerFromDefinition(
                LanguageDefinitionFromAsm<AglFormatterModel, SentenceContext<String>>(
                    identity = formatLanguageIdentity,
                    AglFormat.grammar,
                    buildForDefaultGoal = false,
                    initialConfiguration = Agl.configuration {
                        targetGrammarName(AglFormat.grammar.name.value)
                        defaultGoalRuleName(AglFormat.goalRuleName)
                        //scannerResolver { ProcessResultDefault(ScannerOnDemand(RegexEnginePlatform, it.ruleSet.terminals), IssueHolder(LanguageProcessorPhase.ALL)) }
                        //parserResolver { ProcessResultDefault(LeftCornerParser(it.scanner!!, it.ruleSet), IssueHolder(LanguageProcessorPhase.ALL)) }
                        //typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(it.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                        //crossReferenceModelResolver { ProcessResultDefault(CrossReferenceModelDefault(), IssueHolder(LanguageProcessorPhase.ALL)) }
                        syntaxAnalyserResolver {
                            ProcessResultDefault(
                                AglFormatSyntaxAnalyser(it.typeModel, it.asmTransformModel, it.grammar!!.qualifiedName),
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
        }

        override val style: LanguageDefinition<AglStyleModel, ContextFromGrammar> by lazy {
            this@LanguageRegistryDefault.registerFromDefinition(
                LanguageDefinitionFromAsm(
                    identity = styleLanguageIdentity,
                    AglStyle.grammar,
                    buildForDefaultGoal = false,
                    initialConfiguration = Agl.configuration {
                        targetGrammarName(AglStyle.grammar.name.value)
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
        identity: LanguageIdentity,
        grammarStr: String?,
        aglOptions: ProcessOptions<GrammarModel, ContextFromGrammarRegistry>?,
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

    override fun unregister(identity: LanguageIdentity) {
        this._registry.remove(identity)
    }

    override fun <AsmType : Any, ContextType : Any> findOrNull(identity: LanguageIdentity): LanguageDefinition<AsmType, ContextType>? {
        return this._registry[identity] as LanguageDefinition<AsmType, ContextType>?
    }

    /**
     * try to find localNamespace.nameOrQName or if not found try to find nameOrQName
     */
    fun <AsmType : Any, ContextType : Any> findWithNamespaceOrNull(localNamespace: String, nameOrQName: String): LanguageDefinition<AsmType, ContextType>? {
        return findOrNull(LanguageIdentity("$localNamespace.$nameOrQName")) ?: findOrNull(LanguageIdentity(nameOrQName))
    }

    override fun <AsmType : Any, ContextType : Any> findOrPlaceholder(
        identity: LanguageIdentity,
        aglOptions: ProcessOptions<GrammarModel, ContextFromGrammarRegistry>?,
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

    override fun findGrammarOrNull(localNamespace: Namespace<Grammar>, nameOrQName: PossiblyQualifiedName): Grammar? =
        when (nameOrQName) {
            is QualifiedName -> findGrammarOrNullByQualifiedName(nameOrQName)
            is SimpleName -> findGrammarOrNullByQualifiedName(nameOrQName.asQualifiedName(localNamespace.qualifiedName))
            else -> error("Unsupported")
        }


    override fun registerGrammar(grammar: Grammar) {
        _grammars[grammar.qualifiedName] = grammar
    }
}