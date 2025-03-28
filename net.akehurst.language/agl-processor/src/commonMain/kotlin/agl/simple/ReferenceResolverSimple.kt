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

import net.akehurst.language.asm.api.*
import net.akehurst.language.asm.simple.AsmNothingSimple
import net.akehurst.language.asm.simple.AsmPrimitiveSimple
import net.akehurst.language.asm.simple.isStdString
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.collections.mutableStackOf
import net.akehurst.language.expressions.api.*
import net.akehurst.language.expressions.asm.NavigationExpressionDefault
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.expressions.processor.*
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.reference.api.ReferenceExpression
import net.akehurst.language.reference.asm.ReferenceExpressionCollectionDefault
import net.akehurst.language.reference.asm.ReferenceExpressionPropertyDefault
import net.akehurst.language.scope.api.Scope
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.typemodel.api.TypeDefinition
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.StdLibDefault


data class ReferenceExpressionContext<ItemInScopeType>(
    val element: AsmValue,
    val scope: Scope<ItemInScopeType>
)

/**
 * will check and resolve (if resolveFunction is not null) references.
 * Properties in the asm that are references
 */
class ReferenceResolverSimple<ItemInScopeType>(
    val typeModel: TypeModel,
    val crossReferenceModel: CrossReferenceModel,
    val rootScope: Scope<ItemInScopeType>,
    val identifyingValueInFor: (inTypeName: SimpleName, item: AsmStructure) -> Any?,
    val resolveFunction: ((ref: ItemInScopeType) -> AsmStructure?)?,
    private val _locationMap: Map<Any, InputLocation>,
    private val _issues: IssueHolder
) : AsmTreeWalker {

    private val scopeStack = mutableStackOf(rootScope)
    private val scopeForElement = mutableMapOf<AsmStructure, Scope<ItemInScopeType>>()
    private val _interpreter = ExpressionsInterpreterOverTypedObject(ObjectGraphAsmSimple(typeModel, _issues),_issues)

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
                val elScope = rootScope//.rootScope.scopeMap[ref] ?: rootScope
                scopeStack.push(elScope)
                scopeForElement[root] = elScope
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

        val references = crossReferenceModel.referencesFor(value.typeName)
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

    private fun handlePropertyReferenceExpression(refExpr: ReferenceExpressionPropertyDefault, context: ReferenceExpressionContext<ItemInScopeType>, self: AsmValue) {
        // 'in' typeReference '{' referenceExpression* '}'
        // 'property' navigation 'refers-to' typeReferences from? ;
        //check referred to item exists
        // resolve reference & convert property value to reference
        val scope = when (refExpr.fromNavigation) {
            null -> scopeStack.peek()
            else -> {
                //scope for result of navigation
                val elType = typeModel.findByQualifiedNameOrNull(context.element.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
                val fromEl = _interpreter.evaluateExpression(
                    EvaluationContext.ofSelf(TypedObjectAsmValue(elType, context.element)), refExpr.fromNavigation
                ).self
                when (fromEl) {
                    is AsmNothing -> error("Cannot get scope for result of '${context.element}.${refExpr.fromNavigation}' in is ${AsmNothingSimple}")
                    is AsmStructure -> {
                        val fromElId = identifyingValueInFor.invoke(context.scope.forTypeName.last, fromEl) as String? ?: error("'${fromEl}' not identifiable, therefore cannot determine it's scope")
                        val elScope = context.scope.getChildScopeOrNull(fromElId) ?: error("Scope for '${fromElId}' not found")
                        elScope
                        //scopeForElement[fromEl]!!
                    }
                    is AsmReference -> {
                        val v = fromEl.value ?: error("'${fromEl.reference}' not resolved, therefore cannot determine it's scope")
                        val fromElId = identifyingValueInFor.invoke(context.scope.forTypeName.last, v) as String? ?: error("'${fromEl.reference}' not identifiable, therefore cannot determine it's scope")
                        val elScope = context.scope.getChildScopeOrNull(fromElId) ?: error("Scope for '${fromElId}' not found")
                        elScope
                        //scopeForElement[v] ?: error("Scope for '${v}' not found !")
                    }

                    else -> error("Cannot get scope for result of '${context.element}.${refExpr.fromNavigation}' in is not an AsmStructure, rather it is a '${fromEl::class.simpleName}'")
                }
            }
        }
        val elType = typeModel.findByQualifiedNameOrNull(self.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
        var referringValue = _interpreter.evaluateExpression(EvaluationContext.ofSelf(TypedObjectAsmValue(elType,self)), refExpr.referringPropertyNavigation).self
        if (referringValue is AsmReference) {
            referringValue = AsmPrimitiveSimple.stdString(referringValue.reference)
        }
        when {
            referringValue is AsmPrimitive -> {
                val referringStr = referringValue.value as String
                val referredToTypes = refExpr.refersToTypeName.mapNotNull { this.typeModel.findFirstDefinitionByPossiblyQualifiedNameOrNull(it) }
                val targets = referredToTypes.flatMap { td ->
                    scope.findItemsNamedConformingTo(referringStr) {
                        val itemType = typeModel.findFirstDefinitionByPossiblyQualifiedNameOrNull(it) ?: StdLibDefault.NothingType.resolvedDeclaration
                        itemType.conformsTo(td)
                    }
                }
                when {
                    targets.isEmpty() -> {
                        context.element
                        raiseError(self, "No target of type(s) ${refExpr.refersToTypeName} found for referring value '$referringStr' in scope of element '$self'")
                        val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                        referringProperty.convertToReferenceTo(null)
                    }

                    1 < targets.size -> {
                        val msg = "Multiple target of type(s) ${refExpr.refersToTypeName} found for referring value '${referringValue.value}' in scope of element '$self': $targets"
                        raiseError(self, msg)
                        val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                        referringProperty.convertToReferenceTo(null)
                    }

                    else -> {
                        val referred = targets.first().item // already checked for empty and > 1, so must be only one
                        when {
                            // referred.isExternal -> {
                            //     // cannot resolve, intentionally external, refer to null
                            //     val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                            //     referringProperty.convertToReferenceTo(null)
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
                                        referringProperty.convertToReferenceTo(ref)
                                    }

                                    else -> this.raiseError(self, "Asm element '$referred' is not a reference to a structured element of the asm")
                                }
                            }

                            else -> Unit // no resolve function so do not resolve, maybe intentional so do not warn or error
                        }
                    }
                }
            }

            referringValue is AsmList && referringValue.elements.all { (it is AsmPrimitive) && it.isStdString } -> {
                val list = referringValue.elements.map { (it as AsmPrimitive).value as String }
                val referredToTypes = refExpr.refersToTypeName.mapNotNull { this.typeModel.findFirstDefinitionByPossiblyQualifiedNameOrNull(it) }
                val targets = referredToTypes.flatMap { td ->
                    scope.rootScope.findItemsByQualifiedNameConformingTo(list) {
                        val itemType = typeModel.findFirstDefinitionByPossiblyQualifiedNameOrNull(it) ?: StdLibDefault.NothingType.resolvedDeclaration
                        itemType.conformsTo(td)
                    }
                }
                when {
                    targets.isEmpty() -> {
                        raiseError(self, "No target of type(s) ${refExpr.refersToTypeName} found for referring value '${list}' in scope of element '$self'")
                        val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                        referringProperty.convertToReferenceTo(null)
                    }

                    1 < targets.size -> {
                        val msg = "Multiple target of type(s) ${refExpr.refersToTypeName} found for referring value '${list}' in scope of element '$self': $targets"
                        raiseError(self, msg)
                        val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                        referringProperty.convertToReferenceTo(null)
                    }

                    else -> {
                        val referred = targets.first().item // already checked for empty and > 1, so must be only one
                        when {
                            //  referred.isExternal -> {
                            //      // cannot resolve, intentionally external, refer to null
                            //      val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                            //      referringProperty.convertToReferenceTo(null)
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
                                        referringProperty.convertToReferenceTo(ref)
                                    }

                                    else -> this.raiseError(self, "Asm element '$referred' is not a reference to a structured element of the asm")
                                }
                            }

                            else -> Unit // no resolve function so do not resolve, maybe intentional so do not warn or error
                        }
                    }
                }
            }

            else -> raiseError(self, "Referring value '${self.typeName}.${refExpr.referringPropertyNavigation}=$referringValue' on element $self is not a String or List<String>")
        }
    }

    private fun handleCollectionReferenceExpression(refExpr: ReferenceExpressionCollectionDefault, context: ReferenceExpressionContext<ItemInScopeType>, self: AsmValue) {
        val elType = typeModel.findByQualifiedNameOrNull(self.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
        val coll = _interpreter.evaluateExpression(EvaluationContext.ofSelf(TypedObjectAsmValue(elType,self)), refExpr.expression)
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

    private fun AsmValue.conformsToType(typeName: PossiblyQualifiedName): Boolean {
        val type = typeModel.findFirstDefinitionByPossiblyQualifiedNameOrNull(typeName) ?: StdLibDefault.NothingType.resolvedDeclaration
        val selfType = typeModel.typeOf(this)
        return selfType.conformsTo(type)
    }

    private fun TypeModel.typeOf(self: AsmValue): TypeDefinition =
        typeModel.findByQualifiedNameOrNull(self.qualifiedTypeName)
            ?: error("Type '${self.qualifiedTypeName}' not found in type model '${this.name}'")

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
                val selfType = typeModel.typeOf(root).type()
                val front = NavigationExpressionDefault(this.start, this.parts.dropLast(1))
                val evc = EvaluationContext(null, mapOf(RootExpressionDefault.SELF.name to TypedObjectAsmValue(selfType,root)))
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
