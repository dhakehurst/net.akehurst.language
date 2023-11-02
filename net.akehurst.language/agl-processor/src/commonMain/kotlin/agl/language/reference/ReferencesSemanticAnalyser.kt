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

package net.akehurst.language.agl.language.reference

import net.akehurst.language.agl.language.expressions.propertyDeclarationFor
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.api.language.expressions.Navigation
import net.akehurst.language.api.language.expressions.RootExpression
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.typemodel.api.CollectionType
import net.akehurst.language.typemodel.api.DataType
import net.akehurst.language.typemodel.api.TypeDeclaration
import net.akehurst.language.typemodel.api.UnnamedSupertypeType

class ReferencesSemanticAnalyser(
) : SemanticAnalyser<CrossReferenceModelDefault, SentenceContext<String>> {

    private val issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private var _locationMap: Map<Any, InputLocation> = emptyMap()

    private var _grammarNamespace: GrammarTypeNamespace? = null

    override fun clear() {
        _grammarNamespace = null
        _locationMap = emptyMap()
        issues.clear()
    }

    override fun analyse(
        asm: CrossReferenceModelDefault,
        locationMap: Map<Any, InputLocation>?,
        context: SentenceContext<String>?,
        options: SemanticAnalysisOptions<CrossReferenceModelDefault, SentenceContext<String>>
    ): SemanticAnalysisResult {
        this._locationMap = locationMap ?: mapOf()
        if (null != context) {
            asm.declarationsForNamespace.values.forEach {
                val ns = (context as ContextFromTypeModel).typeModel.namespace[it.qualifiedName]
                when (ns) {
                    null -> issues.raise(
                        LanguageIssueKind.ERROR,
                        LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                        null,
                        "Namespace '${it.qualifiedName}' not found in typeModel."
                    )

                    else -> {
                        _grammarNamespace = ns as GrammarTypeNamespace
                        it.scopes.values.forEach {
                            checkScopeDefinition(it as ScopeDefinitionDefault)
                        }
                        it.references.forEach { ref ->
                            checkReferenceDefinition(it.externalTypes, ref as ReferenceDefinitionDefault)
                        }
                    }
                }
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

    private fun checkScopeDefinition(scopeDef: ScopeDefinitionDefault) {
        val msgStart = if (CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME == scopeDef.scopeForTypeName) {
            //do nothing
            "In root scope"
        } else {
            val scopedType = _grammarNamespace?.findTypeNamed(scopeDef.scopeForTypeName)
            if (null == scopedType) {
                //if (context.rootScope.isMissing(scope.scopeFor, ContextFromTypeModel.TYPE_NAME_FOR_TYPES)) {
                raiseError(ReferencesSyntaxAnalyser.PropertyValue(scopeDef, "typeReference"), "Type '${scopeDef.scopeForTypeName}' not found")
            } else {
                //OK
            }
            "In scope for '${scopeDef.scopeForTypeName}'"
        }

        scopeDef.identifiables.forEach { identifiable ->
            val identifiedType = _grammarNamespace?.findTypeNamed(identifiable.typeName)
            //val typeScope = context.rootScope.childScopes[identifiable.typeName]
            val identifiedBy = identifiable.identifiedBy
            when {
                null == identifiedType -> {
                    raiseError(ReferencesSyntaxAnalyser.PropertyValue(identifiable, "typeReference"), "Type '${identifiable.typeName}' not found")
                }

                identifiedBy is RootExpression && identifiedBy.isNothing -> Unit
                identifiedBy is Navigation -> {
                    // only check this if the typeName is valid - else it is always invalid
                    //TODO: check this in context of typeName GrammarRule
                    val identifyingProperty = identifiedBy.propertyDeclarationFor(identifiedType.instance())
                    if (null == identifyingProperty) {
                        //if (typeScope.isMissing(part, ContextFromTypeModel.TYPE_NAME_FOR_PROPERTIES)) {
                        raiseError(
                            ReferencesSyntaxAnalyser.PropertyValue(identifiable, "propertyName"),
                            "$msgStart, '${identifiable.identifiedBy}' not found for identifying property of '${identifiable.typeName}'"
                        )
                    } else {
                        //OK
                    }
                }
            }
        }
    }

    private fun checkReferenceDefinition(externalTypes: List<String>, ref: ReferenceDefinitionDefault) {
        val contextType = _grammarNamespace?.findOwnedTypeNamed(ref.inTypeName)
        when {
            (null == contextType) -> {
                //if (context.rootScope.isMissing(ref.inTypeName, ContextFromTypeModel.TYPE_NAME_FOR_TYPES)) {
                raiseError(
                    ReferencesSyntaxAnalyser.PropertyValue(ref, "in"),
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
                    checkReferenceExpression(externalTypes, contextType, ref, refExpr)
                }
//            }
            }
        }
    }

    private fun checkReferenceExpression(externalTypes: List<String>, contextType: TypeDeclaration, ref: ReferenceDefinitionDefault, refExpr: ReferenceExpressionAbstract) =
        when (refExpr) {
            is PropertyReferenceExpressionDefault -> checkPropertyReferenceExpression(externalTypes, contextType, ref, refExpr)
            is CollectionReferenceExpressionDefault -> checkCollectionReferenceExpression(externalTypes, contextType, ref, refExpr)
            else -> error("subtype of 'ReferenceExpression' not handled: '${refExpr::class.simpleName}'")
        }

    private fun checkCollectionReferenceExpression(
        externalTypes: List<String>,
        contextType: TypeDeclaration,
        ref: ReferenceDefinitionDefault,
        refExpr: CollectionReferenceExpressionDefault
    ) {
        refExpr.ofType?.let {
            val type = _grammarNamespace?.findTypeNamed(it)
            when (type) {
                null -> raiseError(it, "For references in '${ref.inTypeName}', forall '${refExpr.navigation}', the of-type '$it' is not found")
            }
        }

        when (contextType) {
            is DataType -> {
                val collTypeInstance = refExpr.navigation.propertyDeclarationFor(contextType.instance())?.typeInstance
                when (collTypeInstance?.type) {
                    null -> TODO()
                    is CollectionType -> {
                        val loopVarType = collTypeInstance.typeArguments[0].type
                        val filteredLoopVarType = refExpr.ofType?.let { ofTypeName ->
                            val ofType = _grammarNamespace?.findTypeNamed(ofTypeName)
                            when {
                                null == ofType -> error("Should not happen, checked above.")
                                //TODO: needs a conforms to to check transitive closure of supertypes
                                loopVarType is DataType && ofType is DataType && ofType.allSuperTypes.any { it.type == loopVarType } -> ofType //no error
                                loopVarType is UnnamedSupertypeType && loopVarType.subtypes.any { it.type == ofType } -> ofType
                                else -> {
                                    raiseError(ref, "The of-type '${ofType.name}' is not a subtype of the loop variable type '${loopVarType.name}'")
                                    null
                                }
                            }
                        } ?: loopVarType
                        for (re in refExpr.referenceExpressionList) {
                            checkReferenceExpression(externalTypes, filteredLoopVarType, ref, re)
                        }
                    }

                    else -> TODO()
                }
            }

            else -> TODO()
        }
    }

    private fun checkPropertyReferenceExpression(
        externalTypes: List<String>,
        contextType: TypeDeclaration,
        ref: ReferenceDefinitionDefault,
        refExpr: PropertyReferenceExpressionDefault
    ) {
        //propertyReferenceExpression = 'property' navigation 'refers-to' typeReferences from? ;
        //from = 'from' navigation ;
        val x = refExpr.referringPropertyNavigation.propertyDeclarationFor(contextType.instance())
        if (null == x) {
            raiseError(
                ReferencesSyntaxAnalyser.PropertyValue(refExpr, "propertyReference"),
                "For references in '${ref.inTypeName}' referring property '${refExpr.referringPropertyNavigation}' not found"
            )
        }

        refExpr.refersToTypeName.forEachIndexed { i, n ->
            if (null == _grammarNamespace?.findTypeNamed(n) && externalTypes.contains(n).not()) {
                raiseError(
                    ReferencesSyntaxAnalyser.PropertyValue(ref, "typeReferences[$i]"),
                    "For references in '${ref.inTypeName}', referred to type '$n' not found"
                )
            } else {
                //OK
            }
        }
    }


}