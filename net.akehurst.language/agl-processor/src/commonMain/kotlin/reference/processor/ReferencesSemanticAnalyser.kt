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

package net.akehurst.language.reference.processor

import net.akehurst.language.agl.expressions.processor.ExpressionTypeResolver
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.api.NavigationExpression
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.reference.api.ReferenceExpression
import net.akehurst.language.reference.asm.*
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.typemodel.api.*

class ReferencesSemanticAnalyser(
) : SemanticAnalyser<CrossReferenceModel, ContextFromTypeModel> {

    private val issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private var _locationMap: Map<Any, InputLocation> = emptyMap()

    private var _grammarNamespace: GrammarTypeNamespace? = null

    private var _context:ContextFromTypeModel? = null

    override fun clear() {
        _grammarNamespace = null
        _locationMap = emptyMap()
        _context = null
        issues.clear()
    }

    override fun analyse(
        asm: CrossReferenceModel,
        locationMap: Map<Any, InputLocation>?,
        context: ContextFromTypeModel?,
        options: SemanticAnalysisOptions<CrossReferenceModel, ContextFromTypeModel>
    ): SemanticAnalysisResult {
        this._locationMap = locationMap ?: mapOf()
        _context = context
        if (null != context) {
            asm.declarationsForNamespace.values.forEach {
                val importedNamespaces = it.importedNamespaces.mapNotNull { impNs ->
                    val impNs = context.typeModel.findNamespaceOrNull(impNs.asQualifiedName)
                    when (impNs) {
                        null -> raiseError(it, "Namespace to import not found")
                    }
                    impNs
                }

                val ns = context.typeModel.findNamespaceOrNull(it.qualifiedName)
                when (ns) {
                    null -> issues.raise(
                        LanguageIssueKind.ERROR,
                        LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                        null,
                        "Namespace '${it.qualifiedName}' not found in typeModel."
                    )

                    else -> {
                        _grammarNamespace = ns as GrammarTypeNamespace
                        it.scopeDefinition.values.forEach {
                            checkScopeDefinition(it as ScopeDefinitionDefault)
                        }
                        it.references.forEach { ref ->
                            checkReferenceDefinition(asm, ref as ReferenceDefinitionDefault, importedNamespaces)
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
        val msgStart = if (CrossReferenceModelDefault.ROOT_SCOPE_TYPE_NAME.last == scopeDef.scopeForTypeName) {
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
            "In scope for type '${scopeDef.scopeForTypeName}'"
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
                identifiedBy is NavigationExpression -> {
                    // only check this if the typeName is valid - else it is always invalid
                    //TODO: check this in context of typeName GrammarRule
                    val typeResolver = ExpressionTypeResolver(_context!!.typeModel)
                    val identifyingProperty = typeResolver.typeFor(identifiedBy, identifiedType.type())
                    //val identifyingProperty = identifiedBy.typeOfNavigationExpressionFor(identifiedType.type())
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

    private fun checkReferenceDefinition(crossReferenceModel: CrossReferenceModel, ref: ReferenceDefinitionDefault, importedNamespaces: List<TypeNamespace>) {
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
                    checkReferenceExpression(crossReferenceModel, contextType, ref, refExpr, importedNamespaces)
                }
//            }
            }
        }
    }

    private fun checkReferenceExpression(
        crossReferenceModel: CrossReferenceModel,
        contextType: TypeDeclaration,
        ref: ReferenceDefinitionDefault,
        refExpr: ReferenceExpression,
        importedNamespaces: List<TypeNamespace>
    ) =
        when (refExpr) {
            is PropertyReferenceExpressionDefault -> checkPropertyReferenceExpression(crossReferenceModel, contextType, ref, refExpr, importedNamespaces)
            is CollectionReferenceExpressionDefault -> checkCollectionReferenceExpression(crossReferenceModel, contextType, ref, refExpr, importedNamespaces)
            else -> error("subtype of 'ReferenceExpression' not handled: '${refExpr::class.simpleName}'")
        }

    private fun checkCollectionReferenceExpression(
        crossReferenceModel: CrossReferenceModel,
        contextType: TypeDeclaration,
        ref: ReferenceDefinitionDefault,
        refExpr: CollectionReferenceExpressionDefault,
        importedNamespaces: List<TypeNamespace>
    ) {
        refExpr.ofType?.let {
            val type = _grammarNamespace?.findTypeNamed(it)
            when (type) {
                null -> raiseError(it, "For references in '${ref.inTypeName}', forall '${refExpr.expression}', the of-type '$it' is not found")
            }
        }
        val typeResolver = ExpressionTypeResolver(_context!!.typeModel)
        val collTypeInstance = typeResolver.typeFor( refExpr.expression,contextType.type())
        when (collTypeInstance.declaration) {
            null -> TODO()
            is CollectionType -> {
                val loopVarType = (collTypeInstance.typeArguments[0] as TypeInstance).declaration
                val filteredLoopVarType = refExpr.ofType?.let { ofTypeName ->
                    val ofType = _grammarNamespace?.findTypeNamed(ofTypeName)
                    when {
                        null == ofType -> error("Should not happen, checked above.")
                        //TODO: needs a conforms to to check transitive closure of supertypes
                        loopVarType is DataType && ofType is DataType && ofType.allSuperTypes.any { it.declaration == loopVarType } -> ofType //no error
                        loopVarType is UnnamedSupertypeType && loopVarType.subtypes.any { it.declaration == ofType } -> ofType
                        else -> {
                            raiseError(ref, "The of-type '${ofType.name}' is not a subtype of the loop variable type '${loopVarType.name}'")
                            null
                        }
                    }
                } ?: loopVarType
                for (re in refExpr.referenceExpressionList) {
                    checkReferenceExpression(crossReferenceModel, filteredLoopVarType, ref, re, importedNamespaces)
                }
            }

            else -> TODO()
        }

    }

    private fun checkPropertyReferenceExpression(
        crossReferenceModel: CrossReferenceModel,
        contextType: TypeDeclaration,
        ref: ReferenceDefinitionDefault,
        refExpr: PropertyReferenceExpressionDefault,
        importedNamespaces: List<TypeNamespace>
    ) {
        //propertyReferenceExpression = 'property' navigation 'refers-to' typeReferences from? ;
        //from = 'from' navigation ;
        val typeResolver = ExpressionTypeResolver(_context!!.typeModel)
        val prop = typeResolver.lastPropertyDeclarationFor(refExpr.referringPropertyNavigation, contextType.type())
        //val prop = refExpr.referringPropertyNavigation.lastPropertyDeclarationFor(contextType.type())
        if (null == prop) {
            raiseError(
                ReferencesSyntaxAnalyser.PropertyValue(refExpr, "propertyReference"),
                "For references in '${ref.inTypeName}' referring property '${refExpr.referringPropertyNavigation}' not found"
            )
        }

        refExpr.refersToTypeName.forEachIndexed { i, n ->
            if (null == findReferredToType(n, importedNamespaces)) {
                raiseError(
                    ReferencesSyntaxAnalyser.PropertyValue(ref, "typeReferences[$i]"),
                    "For references in '${ref.inTypeName}', referred to type '$n' not found"
                )
            } else {
                //OK
            }
        }

        if (null != prop) {
            val qualifiedTypeNames = refExpr.refersToTypeName.mapNotNull { findReferredToType(it, importedNamespaces)?.qualifiedName }
            (crossReferenceModel as CrossReferenceModelDefault).addRecordReferenceForProperty(prop.owner.qualifiedName, prop.name, qualifiedTypeNames)
        }
    }

    fun findReferredToType(name: PossiblyQualifiedName, importedNamespaces: List<TypeNamespace>): TypeDeclaration? {
        return _grammarNamespace?.findTypeNamed(name)
            ?: importedNamespaces.firstNotNullOfOrNull { it.findOwnedTypeNamed(name as SimpleName) }
    }

}