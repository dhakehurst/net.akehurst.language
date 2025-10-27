/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.simple

import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.processor.EvaluationContext
import net.akehurst.language.api.processor.ResolvedReference
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.asm.api.*
import net.akehurst.language.asm.simple.isStdString
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.processor.*
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault

class SemanticAnalyserSimple(
    val typesDomain: TypesDomain,
    val crossReferenceDomain: CrossReferenceDomain
) : SemanticAnalyser<Asm, ContextWithScope<Any, Any>> {

    companion object {
        fun identifyingValueInFor(interpreter: ExpressionsInterpreterOverTypedObject<AsmValue>, crossReferenceDomain: CrossReferenceDomain, inScopeForTypeName: SimpleName, self: AsmStructure): Any? {
            return when {
                //crossReferenceModel.isScopeDefinedFor(self.qualifiedTypeName).not() -> null
                else -> {
                    val exp = crossReferenceDomain.identifyingExpressionFor(inScopeForTypeName, self.qualifiedTypeName)
                    when (exp) {
                        null -> null
                        else -> {
                            val elType = interpreter.typeModel.findByQualifiedNameOrNull(self.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
                            val value = interpreter.evaluateExpression(EvaluationContext.ofSelf(TypedObjectAsmValue(elType, self)), exp).self
                            when {
                                value is AsmPrimitive && value.isStdString -> value.value as String
                                value is AsmList && value.elements.all { it is AsmPrimitive && it.isStdString } -> value.elements.map { (it as AsmPrimitive).value as String }
                                else -> error("Cannot get identifying value for $value")
                            }
                        }
                    }
                }
            }
        }
    }

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private var _resolvedReferences = mutableListOf<ResolvedReference>()
    private lateinit var _locationMap: LocationMap

    private val _interpreter = ExpressionsInterpreterOverTypedObject(ObjectGraphAccessorMutatorAsmSimple(typesDomain, _issues), _issues)

    override fun clear() {
        _resolvedReferences.clear()
        _issues.clear()
    }

    override fun analyse(
        sentenceIdentity: Any?,
        asm: Asm,
        locationMap: LocationMap?,
        options: SemanticAnalysisOptions<ContextWithScope<Any, Any>>
    ): SemanticAnalysisResult {
        val context = options.context
        this._locationMap = locationMap ?: LocationMapDefault()
        when {
            null == context -> _issues.info(null, "No context provided, references not built, checked or resolved, switch off semanticAnalysis or provide a context.")
            else -> {
                buildScope(options, sentenceIdentity, asm, context)
                checkAndResolveReferences(options, sentenceIdentity, asm, _locationMap, context)
            }
        }
        return SemanticAnalysisResultDefault(_resolvedReferences,_issues)
    }

    private fun checkAndResolveReferences(options: SemanticAnalysisOptions<ContextWithScope<Any, Any>>, sentenceIdentity: Any?, asm: Asm, locationMap: LocationMap, context: ContextWithScope<Any, Any>) {
        when {
            options.checkReferences.not() -> _issues.info(null, "Semantic Analysis option 'checkReferences' is off, references not checked.")
            crossReferenceDomain.isEmpty -> _issues.warn(null, "Empty CrossReferenceDomain")
            else -> {
                val resolve = if (options.resolveReferences) {
                    true
                } else {
                    _issues.info(null, "Semantic Analysis option 'resolveReferences' is off, references checked but not resolved.")
                    false
                }
                this.walkReferences(sentenceIdentity, asm, _locationMap, context, resolve)
            }
        }
    }

    private fun walkReferences(sentenceId: Any?, asm: Asm, locationMap: LocationMap, context: ContextWithScope<Any, Any>, resolve: Boolean) {
        val resFunc: ((ref: Any) -> AsmStructure?)? = if (resolve) {
            { ref -> context.resolveScopedItem.invoke(ref) as AsmStructure }
        } else {
            null
        }
        val sentenceScope = context.getScopeForSentenceOrNull(sentenceId)
        if(null!=sentenceScope) {
            val resolver = ReferenceResolverSimple(
                typesDomain,
                crossReferenceDomain,
                context,
                sentenceId,
                this::identifyingValueInFor,
                resFunc,
                locationMap, _issues
            )
            asm.traverseDepthFirst(resolver)
            _resolvedReferences = resolver.resolvedReferences
        } else {
            _issues.info(null, "Scope for sentence with Identity '${sentenceId}' not found, so cannot resolve references.")
            false
        }
    }

    private fun buildScope(options: SemanticAnalysisOptions<ContextWithScope<Any, Any>>, sentenceIdentity: Any?, asm: Asm, context: ContextWithScope<Any, Any>) {
        when {
            options.buildScope.not() -> _issues.info(null, "Semantic Analysis option 'buildScope' is off, scope is not built.")
            else -> {
                val scopeCreator = ScopeCreator(
                    typesDomain,
                    crossReferenceDomain as CrossReferenceDomainDefault,
                    context,
                    sentenceIdentity,
                    options.replaceIfItemAlreadyExistsInScope,
                    options.ifItemAlreadyExistsInScopeIssueKind,
                    this::identifyingValueInFor,
                    _locationMap, _issues
                )
                asm.traverseDepthFirst(scopeCreator)
            }
        }
    }

    private fun identifyingValueInFor(inScopeNamed: SimpleName, self: AsmStructure): Any? =
        identifyingValueInFor(_interpreter, crossReferenceDomain, inScopeNamed, self)

}