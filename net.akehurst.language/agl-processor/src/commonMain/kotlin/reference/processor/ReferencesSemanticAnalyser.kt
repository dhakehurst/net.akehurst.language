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
import net.akehurst.language.grammarTypemodel.api.GrammarTypeNamespace
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.reference.api.ReferenceExpression
import net.akehurst.language.reference.asm.*
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib

class ReferencesSemanticAnalyser(
) : SemanticAnalyser<CrossReferenceModel, ContextFromTypeModel> {

    private val issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private var _locationMap: Map<Any, InputLocation> = emptyMap()

    private var _grammarNamespace: GrammarTypeNamespace? = null

    private var _context: ContextFromTypeModel? = null
    private var _typeResolver: ExpressionTypeResolver? = null

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
        _typeResolver = _context?.let { ExpressionTypeResolver(it.typeModel, issues) }

        if (null != context) {
            asm.declarationsForNamespace.values.forEach {
                val importedNamespaces = it.importedNamespaces.mapNotNull { impNs ->
                    val ns = context.typeModel.findNamespaceOrNull(impNs.asQualifiedName)
                    when (ns) {
                        null -> raiseError(it, "Namespace to import not found")
                    }
                    ns
                }

                val ns = context.typeModel.findNamespaceOrNull(it.namespace.qualifiedName)
                when (ns) {
                    null -> issues.raise(
                        LanguageIssueKind.ERROR,
                        LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                        null,
                        "Namespace '${it.namespace.qualifiedName}' not found in typeModel."
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
                raiseError(ReferencesSyntaxAnalyser.PropertyValue(scopeDef, "typeReference"), "Scope type '${scopeDef.scopeForTypeName}' not found")
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
                    raiseError(ReferencesSyntaxAnalyser.PropertyValue(identifiable, "simpleTypeName"), "Type to identify '${identifiable.typeName}' not found")
                }

                else -> {
                    val identifyingProperty = _typeResolver!!.typeFor(identifiedBy, identifiedType.type())
                    when (identifyingProperty) {
                        SimpleTypeModelStdLib.NothingType -> {
                            raiseError(
                                ReferencesSyntaxAnalyser.PropertyValue(identifiable, "expression"),
                                "$msgStart, '${identifiable.identifiedBy}' not found for identifying property of '${identifiable.typeName}'"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkReferenceDefinition(crossReferenceModel: CrossReferenceModel, ref: ReferenceDefinitionDefault, importedNamespaces: List<TypeNamespace>) {
        val contextType = _grammarNamespace?.findOwnedTypeNamed(ref.inTypeName)
        when {
            (null == contextType) -> {
                raiseError(
                    ReferencesSyntaxAnalyser.PropertyValue(ref, "in"),
                    "Referring type '${ref.inTypeName}' not found"
                )
            }

            else -> {
                for (refExpr in ref.referenceExpressionList) {
                    checkReferenceExpression(crossReferenceModel, contextType, ref, refExpr, importedNamespaces)
                }
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
            is ReferenceExpressionPropertyDefault -> checkPropertyReferenceExpression(crossReferenceModel, contextType, ref, refExpr, importedNamespaces)
            is ReferenceExpressionCollectionDefault -> checkCollectionReferenceExpression(crossReferenceModel, contextType, ref, refExpr, importedNamespaces)
            else -> error("subtype of 'ReferenceExpression' not handled: '${refExpr::class.simpleName}'")
        }

    private fun checkCollectionReferenceExpression(
        crossReferenceModel: CrossReferenceModel,
        contextType: TypeDeclaration,
        ref: ReferenceDefinitionDefault,
        refExpr: ReferenceExpressionCollectionDefault,
        importedNamespaces: List<TypeNamespace>
    ) {
        refExpr.ofType?.let {
            val type = _grammarNamespace?.findTypeNamed(it)
            when (type) {
                null -> raiseError(it, "For references in '${ref.inTypeName}', forall '${refExpr.expression}', the of-type '$it' is not found")
            }
        }
        val collTypeInstance = _typeResolver!!.typeFor(refExpr.expression, contextType.type())
        when (collTypeInstance.declaration) {
            SimpleTypeModelStdLib.NothingType -> {
                TODO()
            }

            is CollectionType -> {
                val loopVarType = collTypeInstance.typeArguments[0].type.declaration
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
        refExpr: ReferenceExpressionPropertyDefault,
        importedNamespaces: List<TypeNamespace>
    ) {
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

        val prop = _typeResolver!!.lastPropertyDeclarationFor(refExpr.referringPropertyNavigation, contextType.type())
        when (prop) {
            null -> {
                raiseError(
                    ReferencesSyntaxAnalyser.PropertyValue(refExpr, "propertyReference"),
                    "For references in '${ref.inTypeName}' referring property '${refExpr.referringPropertyNavigation}' not found"
                )
            }

            else -> {
                val qualifiedTypeNames = refExpr.refersToTypeName.mapNotNull { findReferredToType(it, importedNamespaces)?.qualifiedName }
                (crossReferenceModel as CrossReferenceModelDefault).addRecordReferenceForProperty(prop.owner.qualifiedName, prop.name.value, qualifiedTypeNames)
            }
        }
    }

    private fun findReferredToType(name: PossiblyQualifiedName, importedNamespaces: List<TypeNamespace>): TypeDeclaration? {
        return _grammarNamespace?.findTypeNamed(name)
            ?: importedNamespaces.firstNotNullOfOrNull { it.findOwnedTypeNamed(name as SimpleName) }
    }

}