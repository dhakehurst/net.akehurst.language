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

package net.akehurst.language.agl.agl.grammar.scopes

import net.akehurst.language.agl.grammar.scopes.*
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.agl.semanticAnalyser.propertyDeclarationFor
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.typemodel.api.CollectionType
import net.akehurst.language.typemodel.api.DataType
import net.akehurst.language.typemodel.api.TypeDefinition

class AglScopesSemanticAnalyser(
) : SemanticAnalyser<ScopeModelAgl, SentenceContext<String>> {

    private val issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private var _locationMap: Map<Any, InputLocation> = emptyMap()

    private var _grammarNamespace: GrammarTypeNamespace? = null

    override fun clear() {
        _grammarNamespace = null
        _locationMap = emptyMap()
        issues.clear()
    }

    override fun analyse(
        asm: ScopeModelAgl,
        locationMap: Map<Any, InputLocation>?,
        context: SentenceContext<String>?,
        options: SemanticAnalysisOptions<ScopeModelAgl, SentenceContext<String>>
    ): SemanticAnalysisResult {
        this._locationMap = locationMap ?: mapOf()
        if (null != context) {
            _grammarNamespace = (context as ContextFromTypeModel).targetNamespace as GrammarTypeNamespace
            asm.scopes.forEach { (_, scope) ->
                val msgStart = if (ScopeModelAgl.ROOT_SCOPE_TYPE_NAME == scope.scopeFor) {
                    //do nothing
                    "In root scope"
                } else {
                    val scopedType = _grammarNamespace?.findTypeNamed(scope.scopeFor)
                    if (null == scopedType) {
                        //if (context.rootScope.isMissing(scope.scopeFor, ContextFromTypeModel.TYPE_NAME_FOR_TYPES)) {
                        raiseError(AglScopesSyntaxAnalyser.PropertyValue(scope, "typeReference"), "Type '${scope.scopeFor}' not found")
                    } else {
                        //OK
                    }
                    "In scope for '${scope.scopeFor}'"
                }
                scope.identifiables.forEach { identifiable ->
                    val identifiedType = _grammarNamespace?.findTypeNamed(identifiable.typeName)
                    //val typeScope = context.rootScope.childScopes[identifiable.typeName]
                    when {
                        null == identifiedType -> {
                            raiseError(AglScopesSyntaxAnalyser.PropertyValue(identifiable, "typeReference"), "Type '${identifiable.typeName}' not found")
                        }

                        identifiable.navigation.isNothing -> Unit
                        else -> {
                            // only check this if the typeName is valid - else it is always invalid
                            //TODO: check this in context of typeName GrammarRule
                            val identifyingProperty = identifiable.navigation.propertyDeclarationFor(identifiedType)
                            if (null == identifyingProperty) {
                                //if (typeScope.isMissing(part, ContextFromTypeModel.TYPE_NAME_FOR_PROPERTIES)) {
                                raiseError(
                                    AglScopesSyntaxAnalyser.PropertyValue(identifiable, "propertyName"),
                                    "$msgStart, '${identifiable.navigation}' not found for identifying property of '${identifiable.typeName}'"
                                )
                            } else {
                                //OK
                            }
                        }
                    }
                }
            }

            asm.references.forEach { ref ->
                checkReferenceDefinition(ref)
            }
        } else {
            issues.raise(
                LanguageIssueKind.WARNING,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                null,
                "No context provided, cannot perform semantic analysis. If you do not want to do semantic analysis then switch it off."
            )
        }

        return SemanticAnalysisResultDefault(issues)
    }

    private fun raiseError(obj: Any, message: String) {
        val loc = _locationMap[obj]
        issues.error(loc, message)
    }

    private fun checkReferenceDefinition(ref: ReferenceDefinition) {
        val contextType = _grammarNamespace?.findOwnedTypeNamed(ref.inTypeName)
        when {
            (null == contextType) -> {
                //if (context.rootScope.isMissing(ref.inTypeName, ContextFromTypeModel.TYPE_NAME_FOR_TYPES)) {
                raiseError(
                    AglScopesSyntaxAnalyser.PropertyValue(ref, "in"),
                    "Referring type '${ref.inTypeName}' not found"
                )
            }

            else -> {
//            val typeScope = context.rootScope.childScopes[ref.inTypeName]
//            if (null == typeScope) {
//                raiseError(
//                    AglScopesSyntaxAnalyser.PropertyValue(ref, "propertyReference"),
//                    "Child scope '${ref.inTypeName}' not found"
//                )
//            } else {
                for (refExpr in ref.referenceExpressionList) {
                    checkReferenceExpression(contextType, ref, refExpr)
                }
//            }
            }
        }
    }

    private fun checkReferenceExpression(contextType: TypeDefinition, ref: ReferenceDefinition, refExpr: ReferenceExpression) = when (refExpr) {
        is PropertyReferenceExpression -> checkPropertyReferenceExpression(contextType, ref, refExpr)
        is CollectionReferenceExpression -> checkCollectionReferenceExpression(contextType, ref, refExpr)
        else -> error("subtype of 'ReferenceExpression' not handled: '${refExpr::class.simpleName}'")
    }

    private fun checkCollectionReferenceExpression(
        contextType: TypeDefinition,
        ref: ReferenceDefinition,
        refExpr: CollectionReferenceExpression
    ) {
        refExpr.ofType?.let {
            val type = _grammarNamespace?.findTypeNamed(it)
            when (type) {
                null -> raiseError(it, "For reference in '${ref.inTypeName}' referred to type '$it' not found")
            }
        }

        when (contextType) {
            is DataType -> {
                val collTypeInstance = refExpr.navigation.propertyDeclarationFor(contextType)?.typeInstance
                when (collTypeInstance?.type) {
                    null -> TODO()
                    is CollectionType -> {
                        val loopVarType = collTypeInstance.typeArguments[0].type
                        val filteredLoopVarType = refExpr.ofType?.let {
                            val ofType = _grammarNamespace?.findTypeNamed(it)
                            when {
                                null == ofType -> error("Should not happen, checked above.")
                                loopVarType is DataType && ofType is DataType && ofType.allSuperTypes.any { it.type == loopVarType } -> ofType //no error
                                else -> {
                                    raiseError(ref, "The of-type '${ofType.name}' is not a subtype of the loop variable type '${loopVarType.name}'")
                                    null
                                }
                            }
                        } ?: loopVarType
                        for (re in refExpr.referenceExpressionList) {
                            checkReferenceExpression(filteredLoopVarType, ref, re)
                        }
                    }

                    else -> TODO()
                }
            }

            else -> TODO()
        }
    }

    private fun checkPropertyReferenceExpression(contextType: TypeDefinition, ref: ReferenceDefinition, refExpr: PropertyReferenceExpression) {
        //propertyReferenceExpression = 'property' navigation 'refers-to' typeReferences from? ;
        //from = 'from' navigation ;
        val x = refExpr.referringPropertyNavigation.propertyDeclarationFor(contextType)
        if (null == x) {
            raiseError(
                AglScopesSyntaxAnalyser.PropertyValue(refExpr, "propertyReference"),
                "For reference in '${ref.inTypeName}' referring property '${refExpr.referringPropertyNavigation}' not found"
            )
        }

        refExpr.refersToTypeName.forEachIndexed { i, n ->
            if (null == _grammarNamespace?.findOwnedTypeNamed(n)) {
                raiseError(
                    AglScopesSyntaxAnalyser.PropertyValue(ref, "typeReferences[$i]"),
                    "For reference in '${ref.inTypeName}' referred to type '$n' not found"
                )
            }
        }
    }


}