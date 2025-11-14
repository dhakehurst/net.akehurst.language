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
import net.akehurst.language.api.processor.EvaluationContext
import net.akehurst.language.api.processor.ResolvedReference
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.asm.api.*
import net.akehurst.language.asm.simple.AsmNothingSimple
import net.akehurst.language.asm.simple.AsmPrimitiveSimple
import net.akehurst.language.asm.simple.isStdString
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.api.*
import net.akehurst.language.expressions.asm.NavigationExpressionDefault
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.expressions.processor.*
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.api.ReferenceExpression
import net.akehurst.language.reference.asm.ReferenceExpressionCollectionDefault
import net.akehurst.language.reference.asm.ReferenceExpressionPropertyDefault
import net.akehurst.language.scope.api.Scope
import net.akehurst.language.types.api.TypeDefinition
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault


data class ReferenceExpressionContext<ItemInScopeType>(
    val element: AsmValue,
    val scope: Scope<ItemInScopeType>
)

/**
 * will check and resolve (if resolveFunction is not null) references.
 * Properties in the asm that are references
 */
class ReferenceResolverSimple<ItemInScopeType : Any>(
    val typesDomain: TypesDomain,
    val crossReferenceDomain: CrossReferenceDomain,
    val context: ContextWithScope<Any,ItemInScopeType>, //TODO: use interface or something more abstract
    val sentenceIdentity: Any?,
    val identifyingValueInFor: (inTypeName: SimpleName, item: AsmStructure) -> Any?,
    val resolveFunction: ResolveScopedItem<Any, ItemInScopeType>?,
    private val _locationMap: LocationMap,
    private val _issues: IssueHolder
) : AsmTreeWalker {

    private val scopeStack = mutableStackOf(context.getScopeForSentenceOrNull(sentenceIdentity) ?: context.newScopeForSentence(sentenceIdentity))
    private val scopeForElement = mutableMapOf<AsmStructure, Scope<ItemInScopeType>>()
    private val _interpreter = ExpressionsInterpreterOverTypedObject(ObjectGraphAccessorMutatorAsmSimple(typesDomain, _issues), _issues)

    val resolvedReferences = mutableListOf<ResolvedReference>()

    private fun raiseError(element: Any, message: String) {
        _issues.error(
            _locationMap.get(element),//TODO: should be property location
            message
        )
    }

    override fun beforeRoot(root: AsmValue) {
        when (root) {
            is AsmStructure -> {
                //val ref = createReferableFunction.invoke()
                //val elScope = rootScope//.rootScope.scopeMap[ref] ?: rootScope
                // scopeStack.push(elScope)
                scopeForElement[root] = scopeStack.peek()
            }

            else -> Unit // do nothing
        }
    }

    override fun afterRoot(root: AsmValue) {

    }

    override fun onNothing(owningProperty: AsmStructureProperty?, value: AsmNothing) {}

    override fun onPrimitive(owningProperty: AsmStructureProperty?, value: AsmPrimitive) {}

    override fun beforeStructure(owningProperty: AsmStructureProperty?, value: AsmStructure) {
        val parentScope = scopeStack.peek()
        val inTypeName = parentScope.forTypeName.last
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

        val references = crossReferenceDomain.referencesFor(value.typeName)
        for (refExpr in references) {
            handleReferenceExpression(refExpr, ReferenceExpressionContext(value, elScope), value)
        }
    }

    override fun onProperty(owner: AsmStructure, property: AsmStructureProperty) {

    }

    override fun afterStructure(owningProperty: AsmStructureProperty?, value: AsmStructure) {
        scopeStack.pop()
    }

    override fun beforeList(owningProperty: AsmStructureProperty?, value: AsmList) {}

    override fun afterList(owningProperty: AsmStructureProperty?, value: AsmList) {}

    private fun handleReferenceExpression(refExpr: ReferenceExpression, context: ReferenceExpressionContext<ItemInScopeType>, self: AsmValue) {
        when (refExpr) {
            is ReferenceExpressionPropertyDefault -> handlePropertyReferenceExpression(refExpr, context, self)
            is ReferenceExpressionCollectionDefault -> handleCollectionReferenceExpression(refExpr, context, self)
            else -> error("subtype of 'ReferenceExpression' not handled: '${refExpr::class.simpleName}'")
        }
    }

    private fun handlePropertyReferenceExpression(refExpr: ReferenceExpressionPropertyDefault, exprContext: ReferenceExpressionContext<ItemInScopeType>, self: AsmValue) {
        // 'in' typeReference '{' referenceExpression* '}'
        // 'property' navigation 'refers-to' typeReferences from? ;
        //check referred to item exists
        // resolve reference & convert property value to reference
        val scope = when (refExpr.fromNavigation) {
            null -> scopeStack.peek()
            else -> {
                //scope for result of navigation
                val elType = typesDomain.findByQualifiedNameOrNull(exprContext.element.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
                val fromEl = _interpreter.evaluateExpression(
                    EvaluationContext.ofSelf(TypedObjectAsmValue(elType, exprContext.element)), refExpr.fromNavigation
                ).self
                when (fromEl) {
                    is AsmNothing -> error("Cannot get scope for result of '${exprContext.element}.${refExpr.fromNavigation}' in is ${AsmNothingSimple}")
                    is AsmStructure -> {
                        val fromElId =
                            identifyingValueInFor.invoke(exprContext.scope.forTypeName.last, fromEl) as String? ?: error("'${fromEl}' not identifiable, therefore cannot determine it's scope")
                        val elScope = exprContext.scope.getChildScopeOrNull(fromElId) ?: error("Scope for '${fromElId}' not found")
                        elScope
                        //scopeForElement[fromEl]!!
                    }

                    is AsmReference -> {
                        val v = fromEl.value // ?: error("'${fromEl.reference}' not resolved, therefore cannot determine it's scope")
                        if(null==v) {
                            raiseError(self,"Cannot resolve reference to ${fromEl.reference}")
                            null
                        } else {
                            val fromElId =
                                identifyingValueInFor.invoke(exprContext.scope.forTypeName.last, v) as String? ?: error("'${fromEl.reference}' not identifiable, therefore cannot determine it's scope")
                            val elScope = exprContext.scope.getChildScopeOrNull(fromElId) ?: error("Scope for '${fromElId}' not found")
                            elScope
                            //scopeForElement[v] ?: error("Scope for '${v}' not found !")
                        }
                    }

                    else -> error("Cannot get scope for result of '${exprContext.element}.${refExpr.fromNavigation}' in is not an AsmStructure, rather it is a '${fromEl::class.simpleName}'")
                }
            }
        }
        if(null!=scope) {
            val elType = typesDomain.findByQualifiedNameOrNull(self.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
            var referringValue = _interpreter.evaluateExpression(EvaluationContext.ofSelf(TypedObjectAsmValue(elType, self)), refExpr.referringPropertyNavigation).self
            if (referringValue is AsmReference) {
                referringValue = AsmPrimitiveSimple.stdString(referringValue.reference)
            }
            when {
                referringValue is AsmPrimitive -> {
                    //TODO: extract to function to avoid repetition
                    val referringStr = referringValue.value as String
                    val referredToTypes = refExpr.refersToTypeName.mapNotNull { this.typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(it) }
                    val targets = referredToTypes.flatMap { td ->
                        // Use context (not scope) because the reference could have been created from a different sentence
                        context.findItemsByQualifiedNameConformingTo(scope.scopePath + referringStr) {
                            val itemType = typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(it) ?: StdLibDefault.NothingType.resolvedDeclaration
                            itemType.conformsTo(td)
                        }
                    }
                    when {
                        targets.isEmpty() -> {
                            val selfId = when (self) {
                                is AsmStructure -> identifyingValueInFor.invoke(scope.forTypeName.last, self)
                                else -> self.toString()
                            }
                            raiseError(self, "Reference '$referringStr' not resolved, to type(s) ${refExpr.refersToTypeName} in scope '${scope.scopeIdentity}'")//of element '$selfId'")
                            //raiseError(self, "No target of type(s) ${refExpr.refersToTypeName} found for referring value '$referringStr' in scope of element '$self'")
                            val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                            createReference(self,referringProperty, null)
                        }

                        1 < targets.size -> {
                            val msg = "Multiple target of type(s) ${refExpr.refersToTypeName} found for referring value '${referringValue.value}' in scope of element '$self': $targets"
                            raiseError(self, msg)
                            val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                            createReference(self,referringProperty, null)
                        }

                        else -> {
                            val referred = targets.first().item // already checked for empty and > 1, so must be only one
                            when {
                                // referred.isExternal -> {
                                //     // cannot resolve, intentionally external, refer to null
                                //     val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                                //     createReference(referringProperty, null)
                                // }

                                null != resolveFunction -> {
                                    val ref = resolveFunction.invoke(referred)
                                    when (ref) {
                                        null -> this.raiseError(
                                            self,
                                            "Asm does not contain element '$referred' as reference for '${self.typeName}.${refExpr.referringPropertyNavigation}'"
                                        )

                                        is AsmStructure -> {
                                            val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                                            createReference(self,referringProperty, ref)
                                        }

                                        else -> this.raiseError(self, "Asm element '$referred' is not a reference to a structured element of the asm")
                                    }
                                }

                                else -> {
                                    // no resolve function so do not resolve, maybe intentional so do not warn or error
                                    // create reference but do not resolve it
                                    val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                                    createReference(self,referringProperty, null)
                                }
                            }
                        }
                    }
                }

                referringValue is AsmList && referringValue.elements.all { (it is AsmPrimitive) && it.isStdString } -> {
                    val qname = referringValue.elements.map { (it as AsmPrimitive).value as String }
                    val referredToTypes = refExpr.refersToTypeName.mapNotNull { this.typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(it) }
                    val targets = referredToTypes.flatMap { td ->
                        // Use context (not scope) because the reference could have been created from a different sentence
                        context.findItemsByQualifiedNameConformingTo(qname) {
                            val itemType = typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(it) ?: StdLibDefault.NothingType.resolvedDeclaration
                            itemType.conformsTo(td)
                        }
                    }
                    when {
                        targets.isEmpty() -> {
                            val selfId = when (self) {
                                is AsmStructure -> identifyingValueInFor.invoke(scope.forTypeName.last, self)
                                else -> self.toString()
                            }
                            raiseError(self, "Reference '$qname' not resolved, to type(s) ${refExpr.refersToTypeName} in scope '${scope.scopeIdentity}'")//of element '$selfId'")
                            //raiseError(self, "No target of type(s) ${refExpr.refersToTypeName} found for referring value '${list}' in scope of element '$self'")
                            val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                            createReference(self,referringProperty, null)
                        }

                        1 < targets.size -> {
                            val msg = "Multiple target of type(s) ${refExpr.refersToTypeName} found for referring value '${qname}' in scope of element '$self': $targets"
                            raiseError(self, msg)
                            val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                            createReference(self,referringProperty, null)
                        }

                        else -> {
                            val referred = targets.first().item // already checked for empty and > 1, so must be only one
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
                                            "Asm does not contain element '$referred' as reference for '${self.typeName}.${refExpr.referringPropertyNavigation}'"
                                        )

                                        is AsmStructure -> {
                                            val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                                            createReference(self,referringProperty, ref)
                                        }

                                        else -> this.raiseError(self, "Asm element '$referred' is not a reference to a structured element of the asm")
                                    }
                                }

                                else -> {
                                    // no resolve function so do not resolve, maybe intentional so do not warn or error
                                    // create reference but do not resolve it
                                    val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                                    createReference(self,referringProperty, null)
                                }
                            }
                        }
                    }
                }

                else -> raiseError(self, "Referring value '${self.typeName}.${refExpr.referringPropertyNavigation}=$referringValue' on element $self is not a String or List<String>")
            }
        } else {
            //issue already raised ?
        }
    }

    private fun handleCollectionReferenceExpression(refExpr: ReferenceExpressionCollectionDefault, context: ReferenceExpressionContext<ItemInScopeType>, self: AsmValue) {
        val elType = typesDomain.findByQualifiedNameOrNull(self.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
        val coll = _interpreter.evaluateExpression(EvaluationContext.ofSelf(TypedObjectAsmValue(elType, self)), refExpr.expression)
        for (re in refExpr.referenceExpressionList) {
            when (coll.self) {
                is AsmNothing -> Unit //do nothing
                is AsmList -> {
                    for (el in (coll.self as AsmList).elements) {
                        when {
                            el is AsmNothing -> Unit //do nothing
                            null == refExpr.ofType -> handleReferenceExpression(re, context, el)
                            el.conformsToType(refExpr.ofType) -> handleReferenceExpression(re, context, el)
                            else -> Unit // el is filtered out by the 'ofType'
                        }
                    }
                }

                else -> error("result of navigation should be a List<AsmElementSimple>, got '${coll::class.simpleName}'")
            }
        }
    }

    private fun createReference(source:AsmValue, referringProperty: AsmStructureProperty, refersTo: AsmStructure?) {
        referringProperty.convertToReferenceTo(refersTo)
        if (null!=refersTo) {
            val srcLocation = _locationMap[source]
            val tgtLocation = _locationMap[refersTo]
            resolvedReferences.add(ResolvedReference(
                source = source,
                sourceLocation = srcLocation,
                target = refersTo,
                targetLocation = tgtLocation
            ))
        }
    }

    private fun AsmValue.conformsToType(typeName: PossiblyQualifiedName): Boolean {
        val type = typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(typeName) ?: StdLibDefault.NothingType.resolvedDeclaration
        val selfType = typesDomain.typeOf(this)
        return selfType.conformsTo(type)
    }

    private fun TypesDomain.typeOf(self: AsmValue): TypeDefinition =
        typesDomain.findByQualifiedNameOrNull(self.qualifiedTypeName)
            ?: error("Type '${self.qualifiedTypeName}' not found in types domain '${this.name}'")

    private fun NavigationExpression.propertyFor(root: AsmValue): AsmStructureProperty {
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
                val selfType = typesDomain.typeOf(root).type()
                val front = NavigationExpressionDefault(this.start, this.parts.dropLast(1))
                val evc = EvaluationContext(null, mapOf(RootExpressionDefault.SELF.name to TypedObjectAsmValue(selfType, root)))
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
