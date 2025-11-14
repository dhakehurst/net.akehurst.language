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
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypesDomain
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.api.processor.*
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.asmTransform.processor.AsmTransform
import net.akehurst.language.base.api.Namespace
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.format.processor.*
import net.akehurst.language.formatter.api.AglFormatDomain
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammar.asm.asGrammarDomain
import net.akehurst.language.grammar.processor.*
import net.akehurst.language.m2mTransform.api.M2mTransformDomain
import net.akehurst.language.m2mTransform.processor.M2mTransform
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.processor.AglCrossReference
import net.akehurst.language.reference.processor.ReferencesCompletionProvider
import net.akehurst.language.reference.processor.ReferencesSemanticAnalyser
import net.akehurst.language.reference.processor.ReferencesSyntaxAnalyser
import net.akehurst.language.style.api.AglStyleDomain
import net.akehurst.language.style.processor.AglStyle
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.processor.AglTypes

fun contextFromGrammarRegistry(registry: GrammarRegistry = Agl.registry): SentenceContextAny {
    val context = SentenceContextAny()
    registry.grammars.forEach {
        context.addToScope(registry, it.qualifiedName.parts.map { it.value }, QualifiedName("GrammarNamespace"), null, it)
    }
    return context
}

interface AglLanguages {
    val baseLanguageIdentity: LanguageIdentity
    val expressionsLanguageIdentity: LanguageIdentity
    val grammarLanguageIdentity: LanguageIdentity
    val typesLanguageIdentity: LanguageIdentity
    val crossReferenceLanguageIdentity: LanguageIdentity
    val asmTransformLanguageIdentity: LanguageIdentity
    val m2mTransformLanguageIdentity: LanguageIdentity
    val styleLanguageIdentity: LanguageIdentity
    val formatLanguageIdentity: LanguageIdentity

    val base: LanguageDefinition<Any, SentenceContextAny>
    val expressions: LanguageDefinition<Expression, SentenceContextAny>
    val grammar: LanguageDefinition<GrammarDomain, SentenceContextAny>
    val types: LanguageDefinition<TypesDomain, SentenceContextAny>
    val crossReference: LanguageDefinition<CrossReferenceDomain, ContextFromTypesDomain>
    val asmTransform: LanguageDefinition<AsmTransformDomain, SentenceContextAny>
    val m2mTransform: LanguageDefinition<M2mTransformDomain, SentenceContextAny>
    val style: LanguageDefinition<AglStyleDomain, SentenceContextAny>
    val format: LanguageDefinition<AglFormatDomain, SentenceContextAny>
}

class LanguageRegistryDefault : LanguageRegistry {

    private val _grammars = mutableMapOf<QualifiedName, Grammar>()

    //private val _registryLock = Mutex
    private val _registry = mutableMapOf<LanguageIdentity, LanguageDefinition<*, *>>()

    override val languages: Map<LanguageIdentity, LanguageDefinition<*, *>> get() = _registry

    override val grammars: List<Grammar> get() = languages.flatMap { it.value.grammarDomain!!.allDefinitions.toList() }

    override val agl: AglLanguages = object : AglLanguages {
        override val baseLanguageIdentity: LanguageIdentity get() = AglBase.identity
        override val expressionsLanguageIdentity: LanguageIdentity get() = AglExpressions.identity
        override val grammarLanguageIdentity: LanguageIdentity get() = AglGrammar.identity
        override val typesLanguageIdentity: LanguageIdentity get() = AglTypes.identity
        override val crossReferenceLanguageIdentity: LanguageIdentity get() = AglCrossReference.identity
        override val asmTransformLanguageIdentity: LanguageIdentity get() = AsmTransform.identity
        override val m2mTransformLanguageIdentity: LanguageIdentity get() = M2mTransform.identity
        override val styleLanguageIdentity: LanguageIdentity get() = AglStyle.identity
        override val formatLanguageIdentity: LanguageIdentity get() = AglFormat.identity

        override val base: LanguageDefinition<Any, SentenceContextAny> by lazy {
            this@LanguageRegistryDefault.registerFromLanguageObject(AglBase)
        }

        override val expressions: LanguageDefinition<Expression, SentenceContextAny> by lazy {
            base //ensure base is instantiated
            this@LanguageRegistryDefault.registerFromLanguageObject(AglExpressions)
        }

        override val grammar: LanguageDefinition<GrammarDomain, SentenceContextAny> by lazy {
            base //ensure base is instantiated
            this@LanguageRegistryDefault.registerFromLanguageObject(AglGrammar)
        }

        override val types: LanguageDefinition<TypesDomain, SentenceContextAny> by lazy {
            base // ensure base is instantiated
            this@LanguageRegistryDefault.registerFromLanguageObject(AglTypes)
        }

        override val asmTransform: LanguageDefinition<AsmTransformDomain, SentenceContextAny> by lazy {
            base //ensure base is instantiated
            expressions // ensure expressions is instantiated
            this@LanguageRegistryDefault.registerFromLanguageObject(AsmTransform)
        }

        override val m2mTransform: LanguageDefinition<M2mTransformDomain, SentenceContextAny> by lazy {
            base //ensure base is instantiated
            expressions // ensure expressions is instantiated
            this@LanguageRegistryDefault.registerFromLanguageObject(M2mTransform)
        }
        override val crossReference: LanguageDefinition<CrossReferenceDomain, ContextFromTypesDomain> by lazy {
            expressions //ensure expressions is instantiated
            this@LanguageRegistryDefault.registerFromDefinition(
                LanguageDefinitionFromAsm<CrossReferenceDomain, ContextFromTypesDomain>(
                    identity = crossReferenceLanguageIdentity,
                    AglCrossReference.grammar.asGrammarDomain(),
                    buildForDefaultGoal = false,
                    initialConfiguration = Agl.configuration {
                        targetGrammarName(AglCrossReference.grammar.name.value)
                        defaultGoalRuleName(AglCrossReference.goalRuleName)
                        //scannerResolver { ProcessResultDefault(ScannerOnDemand(RegexEnginePlatform, it.ruleSet.terminals), IssueHolder(LanguageProcessorPhase.ALL)) }
                        //parserResolver { ProcessResultDefault(LeftCornerParser(it.scanner!!, it.ruleSet), IssueHolder(LanguageProcessorPhase.ALL)) }
                        //typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(it.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                        //crossReferenceModelResolver { ProcessResultDefault(CrossReferenceModelDefault(), IssueHolder(LanguageProcessorPhase.ALL)) }
                        syntaxAnalyserResolver { ProcessResultDefault(ReferencesSyntaxAnalyser()) }
                        semanticAnalyserResolver { ProcessResultDefault(ReferencesSemanticAnalyser()) }
                        //formatterResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                        styleResolver { Agl.fromString(Agl.registry.agl.style.processor!!, Agl.registry.agl.style.processor!!.optionsDefault(), AglCrossReference.styleStr) }
                        completionProvider { ProcessResultDefault(ReferencesCompletionProvider()) }
                    }
                )
            )
        }

        override val format by lazy {
            expressions //ensure expressions is instantiated
            this@LanguageRegistryDefault.registerFromLanguageObject(AglFormat)
        }

        override val style: LanguageDefinition<AglStyleDomain, SentenceContextAny> by lazy {
            base //ensure base is instantiated
            this@LanguageRegistryDefault.registerFromLanguageObject(AglStyle)
        }
    }

    fun <AsmType : Any, ContextType : Any> registerFromDefinition(definition: LanguageDefinition<AsmType, ContextType>): LanguageDefinition<AsmType, ContextType> {
        return if (this._registry.containsKey(definition.identity)) {
            error("LanguageDefinition '${definition.identity}' is already registered, please unregister the old one first")
        } else {
            this._registry[definition.identity] = definition
            definition.grammarDomain?.allDefinitions?.forEach {
                registerGrammar(it)
            }
            definition
        }
    }

    override fun <AsmType : Any, ContextType : Any> register(
        identity: LanguageIdentity,
        aglOptions: ProcessOptions<GrammarDomain, SentenceContextAny>?,
        buildForDefaultGoal: Boolean,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>
    ): LanguageDefinition<AsmType, ContextType> = this.registerFromDefinition(
        LanguageDefinitionDefault<AsmType, ContextType>(
            identity = identity,
            aglOptions = aglOptions,
            buildForDefaultGoal = buildForDefaultGoal,
            initialConfiguration = configuration
        )
    )

    fun <AsmType : Any, ContextType : Any> registerFromLanguageObject(languageObject: LanguageObject<AsmType, ContextType>): LanguageDefinition<AsmType, ContextType> {
        return if (this._registry.containsKey(languageObject.identity)) {
            error("LanguageDefinition '${languageObject.identity}' is already registered, please unregister the old one first")
        } else {
            val definition = LanguageDefinitionFromAsm<AsmType, ContextType>(
                identity = languageObject.identity,
                grammarDomain = languageObject.grammarDomain,
                buildForDefaultGoal = false,
                initialConfiguration = Agl.configurationFromLanguageObject(languageObject)
            )
            registerFromDefinition(definition) as LanguageDefinition<AsmType, ContextType>
        }
    }

    override fun unregister(identity: LanguageIdentity) {
        val definition = this._registry.remove(identity)
        definition?.grammarDomain?.allDefinitions?.forEach {
            unregisterGrammar(it.qualifiedName)
        }
    }

    override fun <AsmType : Any, ContextType : Any> findOrNull(identity: LanguageIdentity): LanguageDefinition<AsmType, ContextType>? {
        // the agl languages are not registered until they are first accessed
        // thus need to check for them explicitly
        return when (identity) {
            agl.baseLanguageIdentity -> agl.base
            agl.expressionsLanguageIdentity -> agl.expressions
            agl.grammarLanguageIdentity -> agl.grammar
            agl.asmTransformLanguageIdentity -> agl.asmTransform
            agl.styleLanguageIdentity -> agl.style
            agl.formatLanguageIdentity -> agl.format
            agl.crossReferenceLanguageIdentity -> agl.crossReference
            else -> this._registry[identity]
        } as LanguageDefinition<AsmType, ContextType>?
    }

    /**
     * try to find localNamespace.nameOrQName or if not found try to find nameOrQName
     */
    fun <AsmType : Any, ContextType : Any> findWithNamespaceOrNull(localNamespace: String, nameOrQName: String): LanguageDefinition<AsmType, ContextType>? {
        return findOrNull(LanguageIdentity("$localNamespace.$nameOrQName")) ?: findOrNull(LanguageIdentity(nameOrQName))
    }

    fun <AsmType : Any, ContextType : Any> createLanguageDefinition(
        identity: LanguageIdentity,
        aglOptions: ProcessOptions<GrammarDomain, SentenceContextAny>?,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>?
    ): LanguageDefinition<AsmType, ContextType> = LanguageDefinitionDefault<AsmType, ContextType>(
        identity = identity,
        aglOptions = aglOptions,
        buildForDefaultGoal = false,
        initialConfiguration = configuration ?: Agl.configurationEmpty()
    )

    override fun <AsmType : Any, ContextType : Any> findOrPlaceholder(
        identity: LanguageIdentity,
        aglOptions: ProcessOptions<GrammarDomain, SentenceContextAny>?,
        configuration: LanguageProcessorConfiguration<AsmType, ContextType>?
    ): LanguageDefinition<AsmType, ContextType> {
        val existing = this.findOrNull<AsmType, ContextType>(identity)
        return if (null == existing) {
            val placeholder = createLanguageDefinition(identity, aglOptions, configuration)
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
        }

    override fun registerGrammar(grammar: Grammar) {
        _grammars[grammar.qualifiedName] = grammar
    }

    fun unregisterGrammar(qualifiedName: QualifiedName) {
        _grammars.remove(qualifiedName)
    }
}