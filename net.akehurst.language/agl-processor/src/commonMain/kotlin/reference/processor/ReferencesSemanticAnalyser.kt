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

import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.processor.ResolvedReference
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.processor.ExpressionTypeResolver
import net.akehurst.language.grammarTypemodel.api.GrammarTypesNamespace
import net.akehurst.language.expressions.api.*
import net.akehurst.language.reference.api.ScopeImportKind
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.api.ReferenceExpression
import net.akehurst.language.reference.asm.*
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.StdLibDefault

class ReferencesSemanticAnalyser(
) : SemanticAnalyser<CrossReferenceDomain, SentenceContext> {

    private companion object {
        /** special values of expressions in references, given by the resolver; their types are not known here */
        val SPECIAL_VALUES = setOf("\$owner", "\$container", "\$root", "\$previous")
    }

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private val _resolvedReferences = mutableListOf<ResolvedReference>()
    // cached
    private var _locationMap: LocationMap? = null
    private var _grammarNamespace: TypesNamespace? = null
    /** type of the element of the 'in' block being checked ('from' and 'else' are evaluated on it, also inside a 'forall') */
    private var _inType: TypeDefinition? = null
    private var _typeResolver: ExpressionTypeResolver? = null
    private var _typesDomain: TypesDomain? = null

    override fun clear() {
        _grammarNamespace = null
        _typesDomain = null
        _locationMap = null
        _issues.clear()
        _resolvedReferences.clear()
    }

    override fun analyse(
        sentenceIdentity:Any?,
        asm: CrossReferenceDomain,
        locationMap: LocationMap?,
        options: SemanticAnalysisOptions<SentenceContext>
    ): SemanticAnalysisResult {
        this._locationMap = locationMap ?: LocationMapDefault()
        val typesDomainItem = options.sentenceContext?.findItemsConformingTo { it.value == TypesDomain::class.simpleName } //TODO: use qualified names when supported by kotlin

        if (typesDomainItem!=null && typesDomainItem.isNotEmpty() && typesDomainItem.first().item is TypesDomain) {
            val typesDomain = typesDomainItem.first().item as TypesDomain //TODO: maybe not the first ?
            _typesDomain = typesDomain
            asm.declarationsForNamespace.values.forEach {
                val importedNamespaces = it.importedNamespaces.mapNotNull { impNs ->
                    val ns = typesDomain.findNamespaceOrNull(impNs.asQualifiedName)
                    when(ns) {
                       null -> raiseError(it, "Namespace to import not found")
                    }
                    ns
                }
                val ns = typesDomain.findNamespaceOrNull(it.namespace.qualifiedName)
                when(ns)  {
                   null -> raiseError(null, "Namespace '${it.namespace.qualifiedName}' not found in typeModel.")

                    else -> {
                        _typeResolver = ExpressionTypeResolver(typesDomain, ns, _issues)
                        _grammarNamespace = ns
                        it.scopeDefinition.values.forEach {
                            checkScopeDefinition(it as ScopeDefinitionDefault, importedNamespaces)
                            checkScopeImports(it, importedNamespaces)
                        }
                        it.references.forEach { ref ->
                            checkReferenceDefinition(asm, ref as ReferenceDefinitionDefault, importedNamespaces)
                        }
                    }
                }
            }
        } else {
            _issues.raise(
                LanguageIssueKind.WARNING,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                null,
                "No sentence context provided, cannot perform semantic analysis. If you do not want to do semantic analysis then switch it off."
            )
        }

        return SemanticAnalysisResultDefault(_resolvedReferences,_issues)
    }

    private fun raiseError(obj: Any?, message: String) {
        val loc = obj?.let { _locationMap?.get(obj) }
        _issues.error(loc, message)
    }

    private fun raiseWarning(obj: Any?, message: String) {
        val loc = obj?.let { _locationMap?.get(obj) }
        _issues.warn(loc, message)
    }

    /** checks only the referred to type names ('refers-to'), e.g. when the type of the referring element is not known */
    private fun checkReferredToTypeNames(ref: ReferenceDefinitionDefault, list: List<ReferenceExpression>, importedNamespaces: List<TypesNamespace>) {
        for (re in list) {
            when (re) {
                is ReferenceExpressionPropertyDefault -> checkReferredToTypeNames(ref, re, importedNamespaces)
                is ReferenceExpressionCollectionDefault -> {
                    re.ofType?.let {
                        if (null == findReferredToType(it, importedNamespaces)) {
                            raiseError(it, "For references in '${ref.inTypeName}', forall '${re.expression}', the of-type '$it' is not found")
                        }
                    }
                    checkReferredToTypeNames(ref, re.referenceExpressionList, importedNamespaces)
                }
            }
        }
    }

    private fun checkReferredToTypeNames(ref: ReferenceDefinitionDefault, refExpr: ReferenceExpressionPropertyDefault, importedNamespaces: List<TypesNamespace>) {
        refExpr.refersToTypeName.forEach { n ->
            val referredTo = findReferredToType(n, importedNamespaces)
            when {
                null == referredTo -> raiseError(n, "For references in '${ref.inTypeName}', referred to type '$n' not found")
                referredTo.isReferableType.not() -> raiseError(n, "For references in '${ref.inTypeName}', referred to type '$n' is not a data type")
                else -> Unit //OK
            }
        }
    }

    private fun checkScopeDefinition(scopeDef: ScopeDefinitionDefault, importedNamespaces: List<TypesNamespace>) {
        val msgStart = if (CrossReferenceDomainDefault.ROOT_SCOPE_TYPE_NAME.last == scopeDef.scopeForTypeName) {
            //do nothing
            "In root scope"
        } else {
            val scopedType = findReferredToType(scopeDef.scopeForTypeName, importedNamespaces)
            if (null == scopedType) {
                //if (context.rootScope.isMissing(scope.scopeFor, ContextFromTypeModel.TYPE_NAME_FOR_TYPES)) {
                raiseError(ReferencesSyntaxAnalyser.PropertyValue(scopeDef, "typeReference"), "Scope type '${scopeDef.scopeForTypeName}' not found")
            } else {
                //OK
            }
            "In scope for type '${scopeDef.scopeForTypeName}'"
        }

        scopeDef.identifiables.forEach { identifiable ->
            val identifiedType = findReferredToType(identifiable.typeName, importedNamespaces)
            //val typeScope = context.rootScope.childScopes[identifiable.typeName]
            when {
                null == identifiedType -> {
                    raiseError(ReferencesSyntaxAnalyser.PropertyValue(identifiable, "simpleTypeName"), "Type to identify '${identifiable.typeName}' not found")
                }

                else -> {
                    checkNavigations(identifiable.visibility, identifiedType, "$msgStart, visibility of '${identifiable.typeName}'", identifiable)
                    checkNavigations(identifiable.aliasFor, identifiedType, "$msgStart, alias-for of '${identifiable.typeName}'", identifiable)
                }
            }
            when {
                null == identifiedType -> Unit
                else -> identifiable.identifiedByAlternatives.forEach { identifiedBy ->
                    // the properties navigated to must exist (a later one may be a property of a subtype, see missingProperties);
                    // the type resolver is used only to report why when one does not
                    when {
                        missingProperties(identifiedBy, identifiedType).isNotEmpty() -> {
                            _typeResolver!!.typeFor(identifiedBy, identifiedType.type())
                            raiseError(
                                ReferencesSyntaxAnalyser.PropertyValue(identifiable, "expression"),
                                "$msgStart, '${identifiedBy}' not found for identifying property of '${identifiable.typeName}'"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkScopeImports(scopeDef: ScopeDefinitionDefault, importedNamespaces: List<TypesNamespace>) {
        val isRoot = CrossReferenceDomainDefault.ROOT_SCOPE_TYPE_NAME.last == scopeDef.scopeForTypeName
        // the type of the element that owns the scope, null for the root scope (any root element)
        val ownerType = if (isRoot) null else findReferredToType(scopeDef.scopeForTypeName, importedNamespaces)
        scopeDef.scopeImports.forEach { si ->
            val ofType = si.ofType?.let {
                val t = findReferredToType(it, importedNamespaces)
                if (null == t) {
                    when {
                        si.intoScope -> raiseError(si, "For 'import-into-scope', the type '$it' is not found")
                        else -> raiseError(si, "In scope for type '${scopeDef.scopeForTypeName}', the of-type '$it' is not found")
                    }
                }
                t
            }
            val what = when {
                si.intoScope -> "For 'import-into-scope ${si.ofType?.value}'"
                si.kind == ScopeImportKind.INHERIT -> "In scope for type '${scopeDef.scopeForTypeName}', inherit"
                else -> "In scope for type '${scopeDef.scopeForTypeName}', import"
            }
            if (null == si.named) {
                // the type of each selected element: the of-type, else the (element) type of 'elements' evaluated on the scope owner
                val elementType: TypeDefinition? = when {
                    si.intoScope -> ofType
                    null != ofType -> {
                        if (null != ownerType) checkNavigations(si.elements, ownerType, "$what, forall", si)
                        ofType
                    }

                    null != ownerType -> {
                        checkNavigations(si.elements, ownerType, "$what, forall", si)
                        elementTypeOf(si.elements, ownerType)
                    }

                    else -> null
                }
                if (null != elementType) {
                    checkNavigations(si.target, elementType, what, si)
                    checkNavigations(si.recursive, elementType, "$what, recursive", si)
                    checkNavigations(si.visibility, elementType, "$what, visibility", si)
                    checkNavigations(si.condition, elementType, "$what, when", si)
                }
            } else {
                if (null != ownerType) checkNavigations(si.condition, ownerType, "$what named, when", si)
            }
        }
    }

    /** the type of the elements of 'expr' evaluated on 'selfType' (the element type of a collection), null if it cannot be determined */
    private fun elementTypeOf(expr: Expression, selfType: TypeDefinition): TypeDefinition? {
        if (usesSpecialValue(expr)) return null
        // its own issues: a property that is not found is already reported by checkNavigations
        val ti = ExpressionTypeResolver(_typesDomain!!, _grammarNamespace!!, IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)).typeFor(expr, selfType.type())
        val td = ti.resolvedDefinition
        return when {
            td is CollectionType -> ti.typeArguments.getOrNull(0)?.type?.resolvedDefinition
            td is SpecialType -> null
            else -> td
        }?.takeIf { it !is SpecialType }
    }

    private fun usesSpecialValue(expr: Expression): Boolean = when (expr) {
        is RootExpression -> expr.name in SPECIAL_VALUES
        is NavigationExpression -> usesSpecialValue(expr.start) || expr.parts.any { it is PropertyCall && it.propertyName.startsWith("\$") }
        else -> false
    }

    /**
     * checks that the properties navigated to in 'expr' (evaluated on an element of type 'selfType') exist, an error for each that does not.
     */
    private fun checkNavigations(expr: Expression?, selfType: TypeDefinition, what: String, location: Any) {
        missingProperties(expr, selfType).forEach { raiseError(location, "$what, $it") }
    }

    /**
     * the properties navigated to in 'expr' (evaluated on an element of type 'selfType') that do not exist, as messages.
     * Only navigations are checked (also those in infix, group, ternary and when expressions);
     * a navigation is checked up to the first part that is not a property, or a special value ('\$owner', '\$container', '\$root', '\$previous').
     * The first property must be a property of 'selfType' (or a supertype). A later property may also be a property of a subtype of the type before it
     * (e.g. of an alternative of a grammar choice, whose supertype has no properties); the check stops there.
     */
    private fun missingProperties(expr: Expression?, selfType: TypeDefinition): List<String> = when (expr) {
        null -> emptyList()
        is RootExpression -> listOfNotNull(missingInNavigation(expr, emptyList(), selfType, expr))
        is NavigationExpression -> when (val st = expr.start) {
            is RootExpression -> listOfNotNull(missingInNavigation(st, expr.parts, selfType, expr))
            else -> missingProperties(st, selfType)
        }

        is InfixExpression -> expr.expressions.flatMap { missingProperties(it, selfType) }
        is GroupExpression -> missingProperties(expr.expression, selfType)
        is TernaryConditionExpression -> listOf(expr.condition, expr.trueExpression, expr.falseExpression).flatMap { missingProperties(it, selfType) }
        is WhenExpression -> expr.options.flatMap { missingProperties(it.condition, selfType) + missingProperties(it.expression, selfType) } +
                missingProperties(expr.elseOption.expression, selfType)

        else -> emptyList() // not checked
    }

    private fun missingInNavigation(start: RootExpression, parts: List<NavigationPart>, selfType: TypeDefinition, whole: Expression): String? {
        if (start.isNothing || start.name in SPECIAL_VALUES) return null
        var type: TypeInstance = when {
            start.isSelf -> selfType.type()
            selfType !is StructuredType -> return null
            else -> propertyTypeOrNull(selfType.type(), start.name) ?: return "'${whole.asString()}': type '${selfType.name}' has no property '${start.name}'"
        }
        for (part in parts) {
            val pn = (part as? PropertyCall)?.propertyName ?: return null
            if (pn.startsWith("\$")) return null
            val td = type.resolvedDefinition
            if (td !is StructuredType) return null
            type = propertyTypeOrNull(type, pn) ?: return when {
                // the value may be of a subtype that has the property; its type is not known here, so the check stops
                allSubtypesOf(td).any { null != propertyTypeOrNull(it.type(), pn) } -> null
                else -> "'${whole.asString()}': type '${td.name}' has no property '$pn'"
            }
        }
        return null
    }

    /** the subtypes of 'type', transitively */
    private fun allSubtypesOf(type: TypeDefinition): List<TypeDefinition> {
        val result = mutableListOf<TypeDefinition>()
        val todo = ArrayDeque(listOf(type))
        while (todo.isNotEmpty()) {
            for (st in todo.removeFirst().subtypes.map { it.resolvedDefinition }) {
                if (result.none { it === st } && st !== type) {
                    result.add(st)
                    todo.addLast(st)
                }
            }
        }
        return result
    }

    private fun propertyTypeOrNull(type: TypeInstance, name: String): TypeInstance? = when (type.resolvedDefinition) {
        is StructuredType -> type.allResolvedProperty[PropertyName(name)]?.typeInstance
        else -> null
    }

    /**
     * a property has at most one 'property' line (or one 'property p when { ... }') in a block;
     * a choice between targets is written with 'property p when { c -> refers-to ... else -> refers-to ... }'
     */
    private fun checkPropertyOnce(ref: ReferenceDefinitionDefault, list: List<ReferenceExpression>, where: String) {
        list.filterIsInstance<ReferenceExpressionPropertyDefault>()
            .filter { it.whenOptionIndex <= 0 } // one per line or per 'when { }' block
            .groupBy { it.referringPropertyNavigation.asString() }
            .filter { it.value.size > 1 }
            .forEach { (prop, lines) ->
                raiseError(
                    lines[1],
                    "For references in '${ref.inTypeName}'$where, property '$prop' has more than one 'property' line; use one 'property $prop when { <condition> -> refers-to ... else -> refers-to ... }'"
                )
            }
    }

    private fun checkReferenceDefinition(crossReferenceDomain: CrossReferenceDomain, ref: ReferenceDefinitionDefault, importedNamespaces: List<TypesNamespace>) {
        checkPropertyOnce(ref, ref.referenceExpressionList, "")
        val contextType = findReferredToType(ref.inTypeName, importedNamespaces)
        when {
            (null == contextType) -> {
                raiseError(
                    ReferencesSyntaxAnalyser.PropertyValue(ref, "in"),
                    "Referring type '${ref.inTypeName}' not found"
                )
            }

            else -> {
                _inType = contextType
                for (refExpr in ref.referenceExpressionList) {
                    checkReferenceExpression(crossReferenceDomain, contextType, ref, refExpr, importedNamespaces)
                }
            }
        }
    }

    private fun checkReferenceExpression(
        crossReferenceDomain: CrossReferenceDomain,
        contextType: TypeDefinition,
        ref: ReferenceDefinitionDefault,
        refExpr: ReferenceExpression,
        importedNamespaces: List<TypesNamespace>
    ) =
        when (refExpr) {
            is ReferenceExpressionPropertyDefault -> checkPropertyReferenceExpression(crossReferenceDomain, contextType, ref, refExpr, importedNamespaces)
            is ReferenceExpressionCollectionDefault -> checkCollectionReferenceExpression(crossReferenceDomain, contextType, ref, refExpr, importedNamespaces)
            else -> error("subtype of 'ReferenceExpression' not handled: '${refExpr::class.simpleName}'")
        }

    private fun checkCollectionReferenceExpression(
        crossReferenceDomain: CrossReferenceDomain,
        contextType: TypeDefinition,
        ref: ReferenceDefinitionDefault,
        refExpr: ReferenceExpressionCollectionDefault,
        importedNamespaces: List<TypesNamespace>
    ) {
        checkPropertyOnce(ref, refExpr.referenceExpressionList, ", forall '${refExpr.expression}'")
        val ofType = refExpr.ofType?.let {
            val type = findReferredToType(it, importedNamespaces)
            when (type) {
                null -> raiseError(it, "For references in '${ref.inTypeName}', forall '${refExpr.expression}', the of-type '$it' is not found")
            }
            type
        }
        val collTypeInstance = _typeResolver!!.typeFor(refExpr.expression, contextType.type())
        val collType = collTypeInstance.resolvedDefinition
        when {
            // the type resolver has already reported why the expression has no type (e.g. a property not found)
            StdLibDefault.NothingType.resolvedDefinition == collType -> Unit

            // the type of the expression cannot be determined, the references are resolved on the actual objects;
            // only the referred to type names can be checked
            StdLibDefault.AnyType.resolvedDefinition == collType -> {
                raiseWarning(
                    refExpr,
                    "For references in '${ref.inTypeName}', the type of forall expression '${refExpr.expression}' cannot be determined, its references are not type checked"
                )
                checkReferredToTypeNames(ref, refExpr.referenceExpressionList, importedNamespaces)
            }

            collType is SpecialType -> raiseError(
                refExpr,
                "For references in '${ref.inTypeName}', forall expression '${refExpr.expression}' has type '${collType.name}', it must be a collection or a single value"
            )

            // of-type not found, error already reported
            null != refExpr.ofType && null == ofType -> Unit

            else -> {
                // 'forall' over a single value (not a collection) : the loop variable has the type of the value
                val loopVarType = when (collType) {
                    is CollectionType -> collTypeInstance.typeArguments[0].type.resolvedDefinition
                    else -> collType
                }
                val filteredLoopVarType = ofType?.let {
                    when {
                        //TODO: needs a conforms to to check transitive closure of supertypes
                        loopVarType is DataType && ofType is DataType && ofType.allSuperTypes.any { it.resolvedDefinition == loopVarType } -> ofType //no error
                        loopVarType is UnionType && loopVarType.alternatives.any { it.resolvedDefinition == ofType } -> ofType
                        else -> {
                            raiseError(ref, "The of-type '${ofType.name}' is not a subtype of the loop variable type '${loopVarType.name}'")
                            null
                        }
                    }
                } ?: loopVarType
                checkNavigations(refExpr.condition, filteredLoopVarType, "For references in '${ref.inTypeName}', forall '${refExpr.expression}', when", refExpr)
                for (re in refExpr.referenceExpressionList) {
                    checkReferenceExpression(crossReferenceDomain, filteredLoopVarType, ref, re, importedNamespaces)
                }
            }
        }

    }

    private fun checkPropertyReferenceExpression(
        crossReferenceDomain: CrossReferenceDomain,
        contextType: TypeDefinition,
        ref: ReferenceDefinitionDefault,
        refExpr: ReferenceExpressionPropertyDefault,
        importedNamespaces: List<TypesNamespace>
    ) {
        checkReferredToTypeNames(ref, refExpr, importedNamespaces)
        val what = "For references in '${ref.inTypeName}', property '${refExpr.referringPropertyNavigation}'"
        checkNavigations(refExpr.condition, contextType, "$what, when", refExpr)
        // 'from' and 'else' are evaluated on the element of the 'in' block
        val inType = _inType ?: contextType
        checkNavigations(refExpr.fromNavigation, inType, "$what, from", refExpr)
        refExpr.fromElseNavigation?.let { elseNav ->
            val fromStart = refExpr.fromNavigation?.start
            when {
                fromStart is RootExpression && fromStart.name == "\$previous" -> checkNavigations(elseNav, inType, "$what, else", refExpr)
                else -> raiseError(refExpr, "$what: 'else' is only used with 'from \$previous...', it is ignored here")
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
                (crossReferenceDomain as CrossReferenceDomainDefault).addRecordReferenceForProperty(prop.owner.qualifiedName, prop.name.value, qualifiedTypeNames)
            }
        }
    }

    /** a type name of the cross-reference definition, see findCrossReferenceTypeOrNull */
    private fun findReferredToType(name: PossiblyQualifiedName, importedNamespaces: List<TypesNamespace>): TypeDefinition? =
        _typesDomain?.findCrossReferenceTypeOrNull(_grammarNamespace, importedNamespaces, name)

}