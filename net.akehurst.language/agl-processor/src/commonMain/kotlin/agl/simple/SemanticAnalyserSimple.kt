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
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.asm.api.*
import net.akehurst.language.asm.simple.isStdString
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.processor.*
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.StdLibDefault

class SemanticAnalyserSimple(
    val typeModel: TypeModel,
    val crossReferenceModel: CrossReferenceModel
) : SemanticAnalyser<Asm, ContextAsmSimple> {

    companion object {
        fun identifyingValueInFor(interpreter: ExpressionsInterpreterOverTypedObject<AsmValue>, crossReferenceModel: CrossReferenceModel, inScopeForTypeName: SimpleName, self: AsmStructure): Any? {
            return when {
                //crossReferenceModel.isScopeDefinedFor(self.qualifiedTypeName).not() -> null
                else -> {
                    val exp = crossReferenceModel.identifyingExpressionFor(inScopeForTypeName, self.qualifiedTypeName)
                    when (exp) {
                        null -> null
                        else -> {
                            val elType = interpreter.typeModel.findByQualifiedNameOrNull(self.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
                            val value = interpreter.evaluateExpression(EvaluationContext.ofSelf(TypedObjectAsmValue(elType,self)), exp).self
                            when {
                                value is AsmPrimitive && value.isStdString -> value.value as String
                                value is AsmList && value.elements.all { it is AsmPrimitive && it.isStdString } -> value.elements.map { (it as AsmPrimitive).value as String }
                                else -> TODO()
                            }
                        }
                    }
                }
            }
        }
    }

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private lateinit var _locationMap: Map<Any, InputLocation>

    private val _interpreter = ExpressionsInterpreterOverTypedObject(ObjectGraphAsmSimple(typeModel, _issues),_issues)

    override fun clear() {
        _issues.clear()
    }

    override fun analyse(
        asm: Asm,
        locationMap: Map<Any, InputLocation>?,
        options: SemanticAnalysisOptions<ContextAsmSimple>
    ): SemanticAnalysisResult {
        val context = options.context
        this._locationMap = locationMap ?: emptyMap<Any, InputLocation>()

        when {
            null == context -> _issues.info(null, "No context provided, references not built, checked or resolved, switch off semanticAnalysis or provide a context.")
            else -> {
                this.buildScope(options, asm, context)
                checkAndResolveReferences(options, asm, _locationMap, context)
            }
        }
        return SemanticAnalysisResultDefault(this._issues)
    }

    private fun checkAndResolveReferences(options: SemanticAnalysisOptions<ContextAsmSimple>, asm: Asm, locationMap: Map<Any, InputLocation>, context: ContextAsmSimple) {
        when {
            options.checkReferences.not() -> _issues.info(null, "Semantic Analysis option 'checkReferences' is off, references not checked.")
            crossReferenceModel.isEmpty -> _issues.warn(null, "Empty CrossReferenceModel")
            else -> {
                val resolve = if (options.resolveReferences) {
                    true
                } else {
                    _issues.info(null, "Semantic Analysis option 'resolveReferences' is off, references checked but not resolved.")
                    false
                }
                this.walkReferences(asm, _locationMap, context, resolve)
            }
        }
    }

    private fun walkReferences(asm: Asm, locationMap: Map<Any, InputLocation>, context: ContextAsmSimple, resolve: Boolean) {
        val resFunc: ((ref: Any) -> AsmStructure?)? = if (resolve) {
            { ref -> context.resolveScopedItem.invoke(ref) }
        } else {
            null
        }
        asm.traverseDepthFirst(
            ReferenceResolverSimple(
                typeModel, crossReferenceModel, context.rootScope,
                this::identifyingValueInFor,
                resFunc,
                locationMap, _issues
            )
        )
    }

    private fun buildScope(options: SemanticAnalysisOptions<ContextAsmSimple>, asm: Asm, context: ContextAsmSimple) {
        when {
            options.buildScope.not() -> _issues.info(null, "Semantic Analysis option 'buildScope' is off, scope is not built.")
            else -> {
                val createFunc = { ref: String, item: AsmStructure -> context.createScopedItem.invoke(ref, item) }
                val scopeCreator = ScopeCreator(
                    typeModel, crossReferenceModel as CrossReferenceModelDefault, context.rootScope,
                    options.replaceIfItemAlreadyExistsInScope,
                    options.ifItemAlreadyExistsInScopeIssueKind,
                    this::identifyingValueInFor,
                    createFunc,
                    _locationMap, _issues
                )
                asm.traverseDepthFirst(scopeCreator)
            }
        }
    }

    private fun identifyingValueInFor(inScopeNamed: SimpleName, self: AsmStructure): Any? =
        identifyingValueInFor(_interpreter, crossReferenceModel, inScopeNamed, self)

}