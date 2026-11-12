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

import net.akehurst.kotlinx.collections.mutableStackOf
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.api.processor.ResolvedReference
import net.akehurst.language.api.semanticAnalyser.ResolveScopedItem
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.asm.api.*
import net.akehurst.language.asm.simple.AsmNothingSimple
import net.akehurst.language.asm.simple.AsmPrimitiveSimple
import net.akehurst.language.asm.simple.isStdString
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.expressions.api.*
import net.akehurst.language.expressions.asm.NavigationExpressionDefault
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.expressions.processor.*
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.api.ReferenceExpression
import net.akehurst.language.reference.api.ReferenceExpressionProperty
import net.akehurst.language.reference.api.ScopeImport
import net.akehurst.language.reference.api.ScopeImportKind
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import net.akehurst.language.reference.asm.ReferenceExpressionCollectionDefault
import net.akehurst.language.reference.asm.ReferenceExpressionPropertyDefault
import net.akehurst.language.scope.api.ItemInScope
import net.akehurst.language.scope.api.Scope
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.TypeDefinition
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault


data class ReferenceExpressionContext(
    val element: AsmValue,
    val scope: Scope,
    /** inside a 'forall', the element before 'element' in the collection ('\$previous'), null for the first element or outside a 'forall' */
    val previous: AsmStructure? = null
)

/**
 * will check and resolve (if resolveFunction is not null) references.
 * Properties in the asm that are references
 */
class ReferenceResolverSimple(
    val typesDomain: TypesDomain,
    val crossReferenceDomain: CrossReferenceDomain,
    val sentenceContext: SentenceContext,
    val sentenceIdentity: Any?,
    val identifyingValueInFor: (inTypeName: QualifiedName, item: AsmStructure) -> Any?,
    val resolveFunction: ResolveScopedItem?,
    private val _locationMap: LocationMap,
    private val _accessor: ObjectGraphAccessorMutator
) : AsmTreeWalker {

    private val scopeStack = mutableStackOf(sentenceContext.getScopeForSentenceOrNull(sentenceIdentity) ?: sentenceContext.newScopeForSentence(sentenceIdentity))
    private val scopeForElement = mutableMapOf<AsmStructure, Scope>()
    private val _issues = _accessor.issues
    private val _interpreter = ExpressionsInterpreterOverTypedObject(_accessor)

    val resolvedReferences = mutableListOf<ResolvedReference>()

    private fun raiseError(element: Any, message: String) {
        _issues.error(
            _locationMap.get(element),//TODO: should be property location
            message
        )
    }

    override fun beforeRoot(root: Any) {
        when (root) {
            is AsmStructure -> {
                recordContainers(root)
                //val ref = createReferableFunction.invoke()
                //val elScope = rootScope//.rootScope.scopeMap[ref] ?: rootScope
                // scopeStack.push(elScope)
                scopeForElement[root] = scopeStack.peek()
            }

            else -> Unit // do nothing
        }
    }

    override fun afterRoot(root: Any) {

    }

    override fun onNothing(owningProperty: AsmStructureProperty?, value: Unit) {}

    override fun onPrimitive(owningProperty: AsmStructureProperty?, value: Any) {}

    override fun beforeStructure(owningProperty: AsmStructureProperty?, value: AsmStructure) {
        val parentScope = scopeStack.peek()
        val inTypeName = parentScope.forTypeName
        val refInParent = identifyingValueInFor.invoke(inTypeName, value)
        val elScope = when (refInParent) {
            null -> parentScope
            is String -> parentScope.getChildScopeOrNull(refInParent) ?: parentScope
            is List<*> -> when {
                refInParent.all { it is String } -> {
                    var lastScope = parentScope
                    for (r in refInParent) {
                        lastScope = lastScope.getChildScopeOrNull(r as String) ?: parentScope
                    }
                    lastScope
                }

                else -> TODO()
            }

            else -> TODO()
        }
        scopeStack.push(elScope)
        scopeForElement[value] = elScope

        resolveReferencesOf(value, elScope)
    }

    // elements whose references have been handled, or are being handled (to stop cycles)
    private val _referencesHandled = mutableSetOf<AsmStructure>()

    /**
     * handle the reference expressions for 'value', at most once per element.
     * Called by the tree walk, and on demand when a lookup needs a reference of an element (e.g. the target of an import) before the walk reaches it.
     */
    private fun resolveReferencesOf(value: AsmStructure, elScope: Scope) {
        if (_referencesHandled.contains(value)) return
        _referencesHandled.add(value)
        val references = crossReferenceDomain.referencesFor(typesDomain, value.qualifiedTypeName)
        val type = typesDomain.findByQualifiedNameOrNull(value.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
        for (match in references) {
            when {
                match.isAmbiguous && isConditionalChoice(match.candidates) -> {
                    match.candidates.firstOrNull { conditionHolds(it, value) }?.let { refExpr ->
                        handleReferenceExpression(refExpr, ReferenceExpressionContext(value, elScope), value, type)
                    }
                }

                match.isAmbiguous -> raiseError(
                    value,
                    "Ambiguous references for type '${value.qualifiedTypeName.value}', candidates: ${match.candidates.joinToString(separator = ", ") { "'${it.asString().trim()}'" }}"
                )

                else -> match.singleOrNull?.takeIf { conditionHolds(it, value) }?.let { refExpr ->
                    handleReferenceExpression(refExpr, ReferenceExpressionContext(value, elScope), value, type)
                }
            }
        }
    }

    override fun onProperty(owner: AsmStructure, property: AsmStructureProperty) {}

    override fun afterStructure(owningProperty: AsmStructureProperty?, value: AsmStructure) {
        scopeStack.pop()
    }

    override fun beforeList(owningProperty: AsmStructureProperty?, value: Collection<*>) {}

    override fun afterList(owningProperty: AsmStructureProperty?, value: Collection<*>) {}

    private fun handleReferenceExpression(refExpr: ReferenceExpression, sentenceContext: ReferenceExpressionContext, self: Any, type: TypeInstance) {
        when (refExpr) {
            is ReferenceExpressionPropertyDefault -> handlePropertyReferenceExpression(refExpr, sentenceContext, self, type)
            is ReferenceExpressionCollectionDefault -> handleCollectionReferenceExpression(refExpr, sentenceContext, self, type)
            else -> error("subtype of 'ReferenceExpression' not handled: '${refExpr::class.simpleName}'")
        }
    }

    private fun handlePropertyReferenceExpression(refExpr: ReferenceExpressionPropertyDefault, exprContext: ReferenceExpressionContext, self: Any, type: TypeInstance) {
        // 'in' typeReference '{' referenceExpression* '}'
        // 'property' navigation 'refers-to' typeReferences from? ;
        //check referred to item exists
        // resolve reference & convert property value to reference
        val fromNav = refExpr.fromNavigation
        val isFromPrevious = null != fromNav && fromNav.start is RootExpression && (fromNav.start as RootExpression).name == SCOPE_PREVIOUS
        // 'from \$previous...' : resolve in the scope of the target of the reference reached from the previous element of the 'forall'
        // for the first element (no previous) the normal lookup is used
        val chain = when {
            isFromPrevious && null != exprContext.previous -> ChainLookup(previousTargetOf(exprContext.previous, fromNav!!))
            else -> null
        }
        val fromScopes = when {
            null != fromNav && fromNav.start is RootExpression && (fromNav.start as RootExpression).name in SCOPE_ROOTS -> scopesFor(fromNav, exprContext.scope)
            else -> null
        }
        val scope = when {
            null != fromScopes -> exprContext.scope
            isFromPrevious -> exprContext.scope
            null == refExpr.fromNavigation -> exprContext.scope
            else -> {
                //scope for result of navigation
                val elType = typesDomain.findByQualifiedNameOrNull(exprContext.element.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
                val fromEl = _interpreter.evaluateExpression(
                    EvaluationContext.ofSelf(_interpreter.objectGraph.typedAs(exprContext.element, elType)), refExpr.fromNavigation
                ).self
                when (fromEl) {
                    is AsmNothing -> error("Cannot get scope for result of '${exprContext.element}.${refExpr.fromNavigation}' in is ${AsmNothingSimple}")
                    is AsmStructure -> {
                        val fromElId =
                            identifyingValueInFor.invoke(exprContext.scope.forTypeName, fromEl) as String? ?: error("'${fromEl}' not identifiable, therefore cannot determine it's scope")
                        val elScope = exprContext.scope.getChildScopeOrNull(fromElId) ?: error("Scope for '${fromElId}' not found")
                        elScope
                        //scopeForElement[fromEl]!!
                    }

                    is AsmReference -> {
                        val v = fromEl.value // ?: error("'${fromEl.reference}' not resolved, therefore cannot determine it's scope")
                        if (null == v) {
                            raiseError(self, "Cannot resolve reference to ${fromEl.reference}")
                            null
                        } else {
                            val fromElId =
                                identifyingValueInFor.invoke(exprContext.scope.forTypeName, v) as String? ?: error("'${fromEl.reference}' not identifiable, therefore cannot determine it's scope")
                            val elScope = exprContext.scope.getChildScopeOrNull(fromElId) ?: error("Scope for '${fromElId}' not found")
                            elScope
                            //scopeForElement[v] ?: error("Scope for '${v}' not found !")
                        }
                    }

                    else -> error("Cannot get scope for result of '${exprContext.element}.${refExpr.fromNavigation}' in is not an AsmStructure, rather it is a '${fromEl::class.simpleName}'")
                }
            }
        }
        if (null != scope) {
            val elType = _accessor.typeFor(self, StdLibDefault.AnyType)
            var referringValue = _interpreter.evaluateExpression(EvaluationContext.ofSelf(_interpreter.objectGraph.typedAs(self, elType)), refExpr.referringPropertyNavigation).self
            if (referringValue is AsmReference) {
                referringValue = referringValue.reference
            }
            if (referringValue is List<*>) {
                // elements of a List<String> property arrive wrapped as TypedObject
                referringValue = referringValue.map { if (it is TypedObject) it.self else it }
            }
            when {
                referringValue is String -> {
                    val qname = referringValue.split(".")
                    handleCreateReference(scope, fromScopes, chain, self, elType, refExpr, qname)
                }

                referringValue is List<*> && referringValue.isNotEmpty() && referringValue.all { it is String } -> {
                    val qname = referringValue.map { it as String }
                    handleCreateReference(scope, fromScopes, chain, self, elType, refExpr, qname)
                }
                // Deprecated
                referringValue is AsmPrimitive -> {
                    val qname = (referringValue.value as String).split(".")
                    handleCreateReference(scope, fromScopes, chain, self, elType, refExpr, qname)
                }
                // Deprecated
                referringValue is AsmList && referringValue.elements.isNotEmpty() && referringValue.elements.all { (it is AsmPrimitive) && it.isStdString } -> {
                    val qname = referringValue.elements.map { (it as AsmPrimitive).value as String }
                    handleCreateReference(scope, fromScopes, chain, self, elType, refExpr, qname)
                }

                else -> raiseError(self, "Referring value '${elType.typeName.value}.${refExpr.referringPropertyNavigation}=$referringValue' on element $self is not a String or List<String>")
            }
        } else {
            //issue already raised ?
        }
    }

    private fun handleCreateReference(scope: Scope, fromScopes: List<Scope>?, chain: ChainLookup?, self: Any, type: TypeInstance, refExpr: ReferenceExpressionPropertyDefault, qName: List<String>) {
        if (null != chain && null == chain.previousTarget) {
            // the previous element's reference is not resolved (its error is already reported), so this one cannot be resolved either; no further error
            val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self, type)
            createReference(self, referringProperty, null)
            return
        }
        val chainScope = chain?.previousTarget?.let { scopeOfElement(it) }
        val referredToTypes = refExpr.refersToTypeName.mapNotNull { this.typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(it) }
        val conforms = { typeName: QualifiedName ->
            val itemType = typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(typeName) ?: StdLibDefault.NothingType.resolvedDefinition
            referredToTypes.any { td -> itemType.conformsTo(td) }
        }
        val found = when {
            // 'from \$previous...' : members of the previous target, visible from outside
            null != chain -> when (chainScope) {
                null -> emptyList()
                else -> resolveMembersOf(chainScope, qName)
            }
            // Use context (not scope) because the reference could have been created from a different sentence
            null == fromScopes -> sentenceContext.scopeForSentence.values.flatMap { rootScope -> resolveQualifiedName(chainFrom(rootScope, scope.scopePath), qName) }
            // 'from $scope...' : try each start scope in turn, the first that gives a result is used
            else -> fromScopes.firstNotNullOfOrNull { st -> resolveQualifiedName(chainFrom(st.rootScope, st.scopePath), qName).ifEmpty { null } } ?: emptyList()
        }
        val targets = found
            .flatMap { aliasTargets(it, mutableListOf()) }
            .distinctBy { Pair(it.typeName, it.item) }
            .filter { conforms.invoke(it.typeName) }
            .map { ItemInScope(it.name, it.typeName, null, it.item!!) }
        when {
            targets.isEmpty() -> {
                /*
                val selfId = when (self) {
                    is AsmStructure -> identifyingValueInFor.invoke(scope.forTypeName, self)
                    else -> self.toString()
                }
                 */
                raiseError(self, "Reference '${qName.joinToString(".")}' not resolved, to type(s) ${refExpr.refersToTypeName} in scope '${(chainScope ?: scope).scopeIdentity}'")//of element '$selfId'")
                //raiseError(self, "No target of type(s) ${refExpr.refersToTypeName} found for referring value '${list}' in scope of element '$self'")
                val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self, type)
                createReference(self, referringProperty, null)
            }

            1 < targets.size -> {
                val msg = "Multiple target of type(s) ${refExpr.refersToTypeName} found for referring value '${qName.joinToString(".")}' in scope of element '$self': $targets"
                raiseError(self, msg)
                val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self, type)
                createReference(self, referringProperty, null)
            }

            else -> {
                val referred = targets.first().item // already checked for empty and > 1, so must be only one
                if (self is AsmStructure) {
                    elementOf(referred)?.let { _targetOf.getOrPut(self) { mutableMapOf() }[refExpr.referringPropertyNavigation.asString()] = it }
                }
                when {
                    //  referred.isExternal -> {
                    //      // cannot resolve, intentionally external, refer to null
                    //      val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                    //      createReference(referringProperty, null)
                    //  }

                    null != resolveFunction -> {
                        val ref = resolveFunction.invoke(referred)
                        when (ref) {
                            null -> this.raiseError(
                                self,
                                "Asm does not contain element '$referred' as reference for '${type.typeName.value}.${refExpr.referringPropertyNavigation}'"
                            )

                            is AsmStructure -> {
                                val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self,type)
                                createReference(self, referringProperty, ref)
                            }

                            else -> this.raiseError(self, "Asm element '$referred' is not a reference to a structured element of the asm")
                        }
                    }

                    else -> {
                        // no resolve function so do not resolve, maybe intentional so do not warn or error
                        // create reference but do not resolve it
                        val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self,type)
                        createReference(self, referringProperty, null)
                    }
                }
            }
        }
    }

    // ---- name lookup ----

    private companion object {
        const val SCOPE_THIS = "\$scope"
        const val SCOPE_ROOT = "\$root"
        const val SCOPE_PREVIOUS = "\$previous"
        val SCOPE_ROOTS = setOf(SCOPE_THIS, SCOPE_ROOT)
        const val CONTAINER = "\$container"
        const val PUBLIC = "public"
        const val PROTECTED = "protected"
        const val PRIVATE = "private"
        const val MAX_ALIAS_DEPTH = 100
    }

    /** an item found by name; 'item' is null for a child scope that has no item of the same name */
    private data class Found(val name: String, val typeName: QualifiedName, val item: Any?, val owner: Scope)

    /** from where a scope is looked into, decides which visibilities are visible */
    private enum class Access { INSIDE, SUBTYPE, OUTSIDE }

    private fun isVisible(visibility: String, access: Access) = when (access) {
        Access.INSIDE -> true
        Access.SUBTYPE -> visibility != PRIVATE
        Access.OUTSIDE -> visibility == PUBLIC
    }

    /** the scopes from 'rootScope' along 'scopePath', as far as they exist, innermost last */
    private fun chainFrom(rootScope: Scope, scopePath: List<String>): List<Scope> {
        val chain = mutableListOf(rootScope)
        for (seg in scopePath) {
            chain.add(chain.last().getChildScopeOrNull(seg) ?: break)
        }
        return chain
    }

    /**
     * KerML style name resolution (8.2.3.5):
     * the first part of the name is looked up in the innermost scope of 'chain', then outwards. The nearest scope with that name is used.
     * Looking up a name in one scope finds owned items first, then inherited items, then imported items.
     * The remaining parts are looked up only inside the scope found for the previous part, and only items visible from outside are found.
     */
    private fun resolveQualifiedName(chain: List<Scope>, qName: List<String>): List<Found> {
        if (qName.isEmpty()) return emptyList()
        val first = qName.first()
        var found = emptyList<Found>()
        for (s in chain.asReversed()) {
            found = localResolve(s, first, Access.INSIDE, mutableListOf())
            if (found.isEmpty() && qName.size > 1) {
                // a child scope with that name, without an item
                s.getChildScopeOrNull(first)?.let { found = listOf(Found(first, it.forTypeName, null, s)) }
            }
            if (found.isNotEmpty()) break
        }
        found = resolveRest(found, qName.drop(1))
        return found.filter { null != it.item }
    }

    /** the remaining parts of a qualified name, each looked up only inside the scope found for the part before, and only items visible from outside are found */
    private fun resolveRest(start: List<Found>, rest: List<String>): List<Found> {
        var found = start
        for (seg in rest) {
            found = found.flatMap { f ->
                val ns = f.owner.getChildScopeOrNull(f.name)
                when (ns) {
                    null -> emptyList()
                    else -> localResolve(ns, seg, Access.OUTSIDE, mutableListOf()).ifEmpty {
                        // a child scope without an item; not when there is an item that is not visible
                        when {
                            null != ns.items[seg] -> emptyList()
                            else -> ns.getChildScopeOrNull(seg)?.let { listOf(Found(seg, it.forTypeName, null, ns)) } ?: emptyList()
                        }
                    }
                }
            }
        }
        return found
    }

    /** items named 'name' in scope 's': owned, else inherited, else imported */
    private fun localResolve(s: Scope, name: String, access: Access, visited: MutableList<Scope>): List<Found> {
        if (visited.any { it === s }) return emptyList()
        visited.add(s)
        val owned = s.items[name]?.map { (typeName, p) -> Found(name, typeName, p.second, s) }?.filter { isVisible(visibilityOf(it), access) } ?: emptyList()
        if (owned.isNotEmpty()) return owned
        val ownerEl = ownerElementOf(s) ?: return emptyList()
        val scopeImports = crossReferenceDomain.scopeImportsFor(typesDomain, ownerEl.qualifiedTypeName)
        if (scopeImports.isEmpty()) return emptyList()
        val inheritAccess = if (access == Access.OUTSIDE) Access.OUTSIDE else Access.SUBTYPE
        val inherited = scopeImports.filter { it.kind == ScopeImportKind.INHERIT }.flatMap { si ->
            importElementsOf(si, ownerEl).flatMap { src -> targetsOf(si, src, s) }.flatMap { t -> scopeOfElement(t)?.let { localResolve(it, name, inheritAccess, visited) } ?: emptyList() }
        }.distinct()
        if (inherited.isNotEmpty()) return inherited
        val imported = scopeImports.filter { it.kind != ScopeImportKind.INHERIT }.flatMap { si ->
            importElementsOf(si, ownerEl).flatMap { impEl ->
                when {
                    access == Access.OUTSIDE && normaliseVisibility(evaluate(si.visibility, impEl)) != PUBLIC -> emptyList()
                    else -> {
                        val recursive = evaluate(si.recursive, impEl) == true
                        targetsOf(si, impEl, s).flatMap { t ->
                            val direct = when (si.kind) {
                                ScopeImportKind.IMPORT_ELEMENT -> foundForElement(t).filter { it.name == name }
                                else -> scopeOfElement(t)?.let { localResolve(it, name, Access.OUTSIDE, visited) } ?: emptyList()
                            }
                            val nested = when {
                                recursive -> scopeOfElement(t)?.let { ts ->
                                    descendantScopes(ts).flatMap { localResolve(it, name, Access.OUTSIDE, visited) }
                                        .let { if (si.kind == ScopeImportKind.IMPORT_ELEMENT) localResolve(ts, name, Access.OUTSIDE, visited) + it else it }
                                } ?: emptyList()

                                else -> emptyList()
                            }
                            direct + nested
                        }
                    }
                }
            }
        }
        return imported.distinct()
    }

    private fun descendantScopes(s: Scope): List<Scope> = s.childScopes.values.flatMap { listOf(it) + descendantScopes(it) }

    /** lookup for 'from \$previous...': 'previousTarget' is what the previous element's reference resolved to, null if it is not resolved */
    private class ChainLookup(val previousTarget: AsmStructure?)

    /** the element each resolved reference refers to, per referring element and referring property (as written in the reference definition) */
    private val _targetOf = mutableMapOf<AsmStructure, MutableMap<String, AsmStructure>>()

    /**
     * the target of the reference reached by 'nav' ('\$previous.a.b') from 'previous':
     * all but the last property are navigated from 'previous', the last must be a reference property resolved by this resolver
     */
    private fun previousTargetOf(previous: AsmStructure, nav: NavigationExpression): AsmStructure? {
        val props = nav.parts.map { (it as? PropertyCall)?.propertyName ?: return null }
        if (props.isEmpty()) return null
        var el: AsmStructure = previous
        for (pn in props.dropLast(1)) {
            el = structuresIn(evaluate(RootExpressionDefault(pn), el)).singleOrNull() ?: return null
        }
        val last = props.last()
        return _targetOf[el]?.get(last) ?: referredElements(evaluate(RootExpressionDefault(last), el)).singleOrNull()
    }

    /** items for 'qName' among the members of scope 's' (owned, then inherited, then imported) as seen from outside of 's' */
    private fun resolveMembersOf(s: Scope, qName: List<String>): List<Found> {
        if (qName.isEmpty()) return emptyList()
        val first = qName.first()
        val found = localResolve(s, first, Access.OUTSIDE, mutableListOf()).ifEmpty {
            s.getChildScopeOrNull(first)?.let { listOf(Found(first, it.forTypeName, null, s)) } ?: emptyList()
        }
        return resolveRest(found, qName.drop(1)).filter { null != it.item }
    }

    /** the scopes for 'from \$scope...' or 'from \$root...' */
    private fun scopesFor(nav: NavigationExpression, current: Scope): List<Scope> {
        var scopes = when ((nav.start as RootExpression).name) {
            SCOPE_ROOT -> listOf(current.rootScope)
            else -> listOf(current)
        }
        for (part in nav.parts) {
            val pn = (part as? PropertyCall)?.propertyName
            scopes = when (pn) {
                "owner" -> scopes.mapNotNull { ownerScopeOf(it) }
                "inherited" -> scopes.flatMap { inheritedScopesOf(it) }
                else -> {
                    _issues.error(null, "In 'from ${nav.asString()}', '$pn' is not a property of a scope, expected 'owner' or 'inherited'")
                    emptyList()
                }
            }
        }
        return scopes
    }

    private fun ownerScopeOf(s: Scope): Scope? = when {
        s.scopePath.isEmpty() -> null
        else -> chainFrom(s.rootScope, s.scopePath.dropLast(1)).let { ch -> if (ch.size == s.scopePath.size) ch.last() else null }
    }

    private fun inheritedScopesOf(s: Scope): List<Scope> {
        val ownerEl = ownerElementOf(s) ?: return emptyList()
        return crossReferenceDomain.scopeImportsFor(typesDomain, ownerEl.qualifiedTypeName)
            .filter { it.kind == ScopeImportKind.INHERIT }
            .flatMap { si -> importElementsOf(si, ownerEl).flatMap { src -> targetsOf(si, src, s) }.mapNotNull { scopeOfElement(it) } }
    }

    private fun elementOf(item: Any?): AsmStructure? = item?.let { sentenceContext.resolveScopedItem.invoke(it) as? AsmStructure }

    /** the element that created scope 's', i.e. the item in the owning scope with the scope's name and type */
    private fun ownerElementOf(s: Scope): AsmStructure? {
        val owner = ownerScopeOf(s) ?: return null
        val name = s.scopePath.last()
        val items = owner.items[name] ?: return null
        return (items[s.forTypeName]?.second?.let { elementOf(it) }) ?: items.values.firstNotNullOfOrNull { elementOf(it.second) }
    }

    /** the scope created by element 'el', found via its semantic path */
    private fun scopeOfElement(el: AsmStructure): Scope? {
        val path = el.semanticQualifiedPath ?: return null
        if (path.isEmpty()) return null
        return sentenceContext.scopeForSentence.values.firstNotNullOfOrNull { root ->
            val ch = chainFrom(root, path)
            when {
                ch.size != path.size + 1 -> null
                ownerElementOf(ch.last()) === el -> ch.last()
                else -> null
            }
        }
    }

    /** where element 'el' is an item in a scope */
    private fun foundForElement(el: AsmStructure): List<Found> {
        val path = el.semanticQualifiedPath ?: return emptyList()
        if (path.isEmpty()) return emptyList()
        val name = path.last()
        return sentenceContext.scopeForSentence.values.flatMap { root ->
            val ch = chainFrom(root, path.dropLast(1))
            when {
                ch.size != path.size -> emptyList()
                else -> ch.last().items[name]?.filter { (_, p) -> elementOf(p.second) === el }?.map { (typeName, p) -> Found(name, typeName, p.second, ch.last()) } ?: emptyList()
            }
        }
    }

    private fun visibilityOf(f: Found): String {
        val el = elementOf(f.item) ?: return PUBLIC
        val id = crossReferenceDomain.identifyingExpressionFor(typesDomain, f.owner.forTypeName, el.qualifiedTypeName).singleOrNull ?: return PUBLIC
        return normaliseVisibility(evaluate(id.visibility, el))
    }

    private fun normaliseVisibility(v: Any?): String = when (v) {
        PROTECTED, PRIVATE -> v as String
        else -> PUBLIC
    }

    /** if the found item is an alias, the items for what it is an alias for (transitively), else the item itself */
    private fun aliasTargets(f: Found, seen: MutableList<AsmStructure>): List<Found> {
        val el = elementOf(f.item) ?: return listOf(f)
        val aliasFor = crossReferenceDomain.identifyingExpressionFor(typesDomain, f.owner.forTypeName, el.qualifiedTypeName).singleOrNull?.aliasFor ?: return listOf(f)
        if (seen.any { it === el } || seen.size > MAX_ALIAS_DEPTH) return emptyList()
        seen.add(el)
        resolveReferencesOf(el, f.owner)
        return referredElements(evaluate(aliasFor, el)).flatMap { t -> foundForElement(t).flatMap { aliasTargets(it, seen) } }
    }

    /** the elements of the scope owner, selected by an import/inherit declaration */
    private fun importElementsOf(si: ScopeImport, ownerEl: AsmStructure): List<AsmStructure> {
        val ofType = si.ofType?.let { typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(it) }
        return structuresIn(evaluate(si.elements, ownerEl)).filter { el ->
            when (ofType) {
                null -> true
                else -> typesDomain.findByQualifiedNameOrNull(el.qualifiedTypeName)?.conformsTo(ofType) ?: false
            }
        }
    }

    /** the elements referred to by the 'target' (a reference property) of an import/inherit declaration, evaluated on 'sourceEl', which is in scope 's' */
    private fun targetsOf(si: ScopeImport, sourceEl: AsmStructure, s: Scope): List<AsmStructure> {
        resolveReferencesOf(sourceEl, s)
        return referredElements(evaluate(si.target, sourceEl))
    }

    private fun referredElements(v: Any?): List<AsmStructure> = when (v) {
        null -> emptyList()
        is AsmReference -> listOfNotNull(v.value)
        is AsmStructure -> listOf(v)
        is TypedObject -> referredElements(v.self)
        is List<*> -> v.flatMap { referredElements(it) }
        is AsmList -> v.elements.flatMap { referredElements(it) }
        else -> emptyList()
    }

    private fun structuresIn(v: Any?): List<AsmStructure> = when (v) {
        null -> emptyList()
        is AsmStructure -> listOf(v)
        is TypedObject -> structuresIn(v.self)
        is List<*> -> v.flatMap { structuresIn(it) }
        is AsmList -> v.elements.flatMap { structuresIn(it) }
        else -> emptyList()
    }

    // ---- containing elements, for '\$container' ----

    /** the element that contains each element of the sentence (in the ASM, not the scope owner) */
    private val _containerOf = mutableMapOf<AsmStructure, AsmStructure>()

    private fun recordContainers(owner: AsmStructure) {
        for (p in owner.property.values) {
            if (p.isReference) continue
            for (child in structuresIn(p.value)) {
                if (child !== owner && !_containerOf.containsKey(child)) {
                    _containerOf[child] = owner
                    recordContainers(child)
                }
            }
        }
    }

    private fun typedOf(el: AsmStructure?) = when (el) {
        null -> _interpreter.objectGraph.nothing()
        else -> _interpreter.objectGraph.typedAs(el, typesDomain.findByQualifiedNameOrNull(el.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType)
    }

    /**
     * evaluates an expression of an 'identify', 'import' or 'inherit' declaration on 'self'.
     * '\$container' is the element that contains 'self' in the ASM ('\$nothing' for the root, or an element of another sentence)
     */
    private fun evaluate(expr: Expression?, self: AsmStructure): Any? = when (expr) {
        null -> null
        else -> {
            val evc = EvaluationContext.ofSelf(typedOf(self), mapOf(CONTAINER to typedOf(_containerOf[self])))
            val v = _interpreter.evaluateExpression(evc, expr).self
            when (v) {
                is AsmPrimitive -> v.value
                else -> v
            }
        }
    }

    /** several 'property' lines for the same property, where all but the last have a 'when' condition */
    private fun isConditionalChoice(candidates: List<ReferenceExpression>): Boolean =
        candidates.all { it is ReferenceExpressionProperty }
                && candidates.map { (it as ReferenceExpressionProperty).referringPropertyNavigation.asString() }.distinct().size == 1
                && candidates.dropLast(1).all { null != (it as ReferenceExpressionProperty).condition }

    private fun conditionHolds(refExpr: ReferenceExpression, self: Any): Boolean {
        val cond = (refExpr as? ReferenceExpressionProperty)?.condition ?: return true
        return when (self) {
            is AsmStructure -> evaluate(cond, self) == true
            else -> false
        }
    }

    /** for each property, the first of its 'property' lines whose condition holds; other reference expressions unchanged */
    private fun applicable(list: List<ReferenceExpression>, self: Any): List<ReferenceExpression> {
        val done = mutableSetOf<String>()
        return list.filter { re ->
            when (re) {
                is ReferenceExpressionProperty -> {
                    val key = re.referringPropertyNavigation.asString()
                    when {
                        done.contains(key) -> false
                        conditionHolds(re, self) -> {
                            done.add(key); true
                        }

                        else -> false
                    }
                }

                else -> true
            }
        }
    }

    private fun handleCollectionReferenceExpression(refExpr: ReferenceExpressionCollectionDefault, sentenceContext: ReferenceExpressionContext, self: Any, type: TypeInstance) {
         val coll = _interpreter.evaluateExpression(EvaluationContext.ofSelf(_interpreter.objectGraph.typedAs(self, type)), refExpr.expression)
        val elType = coll.type.typeArguments.getOrNull(0)?.type ?: StdLibDefault.AnyType
        val obj = coll.untyped
        val elements = when (obj) {
            is Unit -> emptyList()
            is List<*> -> obj.filter { null != it && it !is Unit && it !is AsmNothing }
            //Deprecated
            is AsmNothing -> emptyList()
            is AsmList -> obj.elements.filter { it !is AsmNothing }
            else -> error("result of navigation should be a List<AsmElementSimple>, got '${coll::class.simpleName}'")
        }
        // when 'from \$previous' is used, the elements are resolved only here, their own reference definitions are not used
        val usesPrevious = refExpr.referenceExpressionList.any { usesPrevious(it) }
        var previous: AsmStructure? = null
        for (el in elements) {
            el!!
            val selected = when {
                null == refExpr.ofType -> true
                else -> el.conformsToType(elType, refExpr.ofType) // else el is filtered out by the 'ofType'
            }
            if (selected) {
                val elStr = (el as? TypedObject)?.self as? AsmStructure ?: el as? AsmStructure
                if (usesPrevious && null != elStr) _referencesHandled.add(elStr)
                val elContext = ReferenceExpressionContext(sentenceContext.element, sentenceContext.scope, previous)
                for (re in applicable(refExpr.referenceExpressionList, el)) {
                    handleReferenceExpression(re, elContext, el, elType)
                }
                previous = elStr
            }
        }
    }

    private fun usesPrevious(re: ReferenceExpression): Boolean = when (re) {
        is ReferenceExpressionProperty -> (re.fromNavigation?.start as? RootExpression)?.name == SCOPE_PREVIOUS
        else -> false
    }

    private fun createReference(source: Any, referringProperty: AsmStructureProperty, refersTo: AsmStructure?) {
        referringProperty.convertToReferenceTo(refersTo)
        if (null != refersTo) {
            val srcLocation = _locationMap[source]
            val tgtLocation = _locationMap[refersTo]
            resolvedReferences.add(
                ResolvedReference(
                    source = source,
                    sourceLocation = srcLocation,
                    target = refersTo,
                    targetLocation = tgtLocation
                )
            )
        }
    }

    private fun Any.conformsToType(selfType:TypeInstance, typeName: PossiblyQualifiedName): Boolean {
        val type = typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(typeName)?.type() ?: StdLibDefault.NothingType
        return selfType.conformsTo(type)
    }

    private fun TypesDomain.typeOf(self: AsmValue): TypeDefinition =
        typesDomain.findByQualifiedNameOrNull(self.qualifiedTypeName)
            ?: error("Type '${self.qualifiedTypeName}' not found in types domain '${this.name}'")

    private fun NavigationExpression.propertyFor(root: Any, type: TypeInstance): AsmStructureProperty {
        return when {
            root is AsmNothing -> error("Cannot navigate '$this' from '$root' value")
            this.parts.isEmpty() -> {
                when (root) {
                    is AsmStructure -> {
                        val exp = this.start
                        val pn = when (exp) {
                            is RootExpression -> PropertyValueName(exp.name)
                            is PropertyCall -> PropertyValueName(exp.propertyName)
                            else -> error("Unsupoorted")
                        }
                        root.property[pn] ?: error("Cannot navigate '$this' from null value")
                    }

                    else -> error("Cannot navigate '$this' from null value")
                }
            }

            else -> {
                //val exprEval = ExpressionsInterpreterOverTypedObject(typeModel)
                val selfType = type
                val front = NavigationExpressionDefault(this.start, this.parts.dropLast(1))
                val evc = EvaluationContext(null, mapOf(RootExpressionDefault.SELF.name to _interpreter.objectGraph.typedAs(root, selfType)))
                val v = _interpreter.evaluateExpression(evc, front).self
                val lastProp = (this.parts.last() as PropertyCall).propertyName
                when (v) {
                    is AsmStructure -> {
                        v.property[PropertyValueName(lastProp)] ?: error("Cannot navigate '$this' from null value")
                    }

                    else -> error("Cannot navigate '$this' from null value")
                }
            }
        }
    }
}
