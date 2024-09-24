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

package net.akehurst.language.agl.default_

import net.akehurst.language.asm.simple.AsmNothingSimple
import net.akehurst.language.asm.simple.AsmPrimitiveSimple
import net.akehurst.language.asm.simple.asValueName
import net.akehurst.language.asm.simple.isStdString
import net.akehurst.language.expressions.processor.EvaluationContext
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.expressions.processor.asmValue
import net.akehurst.language.expressions.processor.toTypedObject
import net.akehurst.language.reference.asm.CollectionReferenceExpressionDefault
import net.akehurst.language.reference.asm.PropertyReferenceExpressionDefault
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.asm.api.*
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.expressions.api.NavigationExpression
import net.akehurst.language.expressions.api.PropertyCall
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.reference.api.ReferenceExpression
import net.akehurst.language.scope.api.Scope
import net.akehurst.language.collections.mutableStackOf
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.typemodel.api.TypeDeclaration
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib

typealias ResolveFunction = (ref: AsmPath) -> AsmValue?

data class ReferenceExpressionContext(
    val element: AsmValue,
    val scope: Scope<AsmPath>
)

/**
 * will check and resolve (if resolveFunction is not null) references.
 * Properties in the asm that are references
 */
class ReferenceResolverDefault(
    val typeModel: TypeModel,
    val scopeModel: CrossReferenceModel,
    val rootScope: Scope<AsmPath>,
    val resolveFunction: ResolveFunction?,
    private val _locationMap: Map<Any, InputLocation>,
    private val _issues: IssueHolder
) : AsmTreeWalker {

    private val scopeStack = mutableStackOf(rootScope)
    private val scopeForElement = mutableMapOf<AsmStructure, Scope<AsmPath>>()
    private val _interpreter = ExpressionsInterpreterOverTypedObject(typeModel)

    private fun raiseError(element: Any, message: String) {
        _issues.error(
            _locationMap.get(element),//TODO: should be property location
            message
        )
    }

    override fun beforeRoot(root: AsmValue) {
        when (root) {
            is AsmStructure -> {
                val elScope = rootScope.rootScope.scopeMap[root.path] ?: rootScope
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
        val elScope = parentScope.rootScope.scopeMap[value.path] ?: parentScope
        scopeStack.push(elScope)
        scopeForElement[value] = elScope

        val references = scopeModel.referencesFor(value.typeName)
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

    private fun handleReferenceExpression(refExpr: ReferenceExpression, context: ReferenceExpressionContext, self: AsmValue) {
        when (refExpr) {
            is PropertyReferenceExpressionDefault -> handlePropertyReferenceExpression(refExpr, context, self)
            is CollectionReferenceExpressionDefault -> handleCollectionReferenceExpression(refExpr, context, self)
            else -> error("subtype of 'ReferenceExpression' not handled: '${refExpr::class.simpleName}'")
        }
    }

    private fun handlePropertyReferenceExpression(refExpr: PropertyReferenceExpressionDefault, context: ReferenceExpressionContext, self: AsmValue) {
        // 'in' typeReference '{' referenceExpression* '}'
        // 'property' navigation 'refers-to' typeReferences from? ;
        //check referred to item exists
        // resolve reference & convert property value to reference
        val scope = when (refExpr.fromNavigation) {
            null -> scopeStack.peek()
            else -> {
                //scope for result of navigation
                val elType = typeModel.findByQualifiedNameOrNull(context.element.qualifiedTypeName)?.type() ?: SimpleTypeModelStdLib.AnyType
                val fromEl = _interpreter.evaluateExpression(
                    EvaluationContext.ofSelf(context.element.toTypedObject(elType)), refExpr.fromNavigation
                )
                when (fromEl) {
                    is AsmNothing -> error("Cannot get scope for result of '${context.element}.${refExpr.fromNavigation}' in is ${AsmNothingSimple}")
                    is AsmStructure -> scopeForElement[fromEl]!!
                    is AsmReference -> {
                        val v = fromEl.value ?: error("'${fromEl.reference}' not resolved, can't get its scope")
                        scopeForElement[v] ?: error("Scope for '${v}' not found !")
                    }

                    else -> error("Cannot get scope for result of '${context.element}.${refExpr.fromNavigation}' in is not an AsmElementSimple, rather it is a '${fromEl::class.simpleName}'")
                }
            }
        }
        val elType = typeModel.findByQualifiedNameOrNull(self.qualifiedTypeName)?.type() ?: SimpleTypeModelStdLib.AnyType
        var referringValue = _interpreter.evaluateExpression(EvaluationContext.ofSelf(self.toTypedObject(elType)), refExpr.referringPropertyNavigation).asmValue
        if (referringValue is AsmReference) {
            referringValue = AsmPrimitiveSimple.stdString(referringValue.reference)
        }
        when {
            referringValue is AsmPrimitive -> {
                val referringStr = referringValue.value as String
                val referredToTypes = refExpr.refersToTypeName.mapNotNull { this.typeModel.findFirstByPossiblyQualifiedOrNull(it) }
                val targets = referredToTypes.flatMap { td ->
                    scope.findItemsNamedConformingTo(referringStr) {
                        val itemType = typeModel.findFirstByPossiblyQualifiedOrNull(it) ?: SimpleTypeModelStdLib.NothingType.declaration
                        itemType.conformsTo(td)
                    }
                }
                when {
                    targets.isEmpty() -> {
                        raiseError(self, "No target of type(s) ${refExpr.refersToTypeName} found for referring value '${referringValue.value}' in scope of element '$self'")
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
                            referred.isExternal -> {
                                // cannot resolve, intentionally external, refer to null
                                val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                                referringProperty.convertToReferenceTo(null)
                            }

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
                val referredToTypes = refExpr.refersToTypeName.mapNotNull { this.typeModel.findFirstByPossiblyQualifiedOrNull(it) }
                val targets = referredToTypes.flatMap { td ->
                    scope.rootScope.findItemsByQualifiedNameConformingTo(list) {
                        val itemType = typeModel.findFirstByPossiblyQualifiedOrNull(it) ?: SimpleTypeModelStdLib.NothingType.declaration
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
                            referred.isExternal -> {
                                // cannot resolve, intentionally external, refer to null
                                val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                                referringProperty.convertToReferenceTo(null)
                            }

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

    private fun handleCollectionReferenceExpression(refExpr: CollectionReferenceExpressionDefault, context: ReferenceExpressionContext, self: AsmValue) {
        val elType = typeModel.findByQualifiedNameOrNull(self.qualifiedTypeName)?.type() ?: SimpleTypeModelStdLib.AnyType
        val coll = _interpreter.evaluateExpression(EvaluationContext.ofSelf(self.toTypedObject(elType)), refExpr.expression)
        for (re in refExpr.referenceExpressionList) {
            when (coll.asmValue) {
                is AsmNothing -> Unit //do nothing
                is AsmList -> {
                    for (el in (coll.asmValue as AsmList).elements) {
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
        val type = typeModel.findFirstByPossiblyQualifiedOrNull(typeName) ?: SimpleTypeModelStdLib.NothingType.declaration
        val selfType = typeModel.typeOf(this)
        return selfType.conformsTo(type)
    }

    private fun TypeModel.typeOf(self: AsmValue): TypeDeclaration =
        typeModel.findByQualifiedNameOrNull(self.qualifiedTypeName)
            ?: error("Type '${self.qualifiedTypeName}' not found in type model '${this.name}'")

    private fun NavigationExpression.propertyFor(root: AsmValue): AsmStructureProperty {
        return when {
            root is AsmNothing -> error("Cannot navigate '$this' from '$root' value")
            else -> {
                val front = this.parts.dropLast(1)
                var v = root
                for (pn in front) {
                    val pd = typeModel.typeOf(v).findPropertyOrNull((pn as PropertyCall).propertyName)
                    v = when (pd) {
                        null -> error("Cannot navigate '$pn' from null value")
                        else -> {
                            val elType = typeModel.findByQualifiedNameOrNull(v.qualifiedTypeName)?.type() ?: SimpleTypeModelStdLib.AnyType
                            v.toTypedObject(elType).getPropertyValue(pd).asmValue
                        }
                    }
                }
                val lastProp = (this.parts.last() as PropertyCall).propertyName
                when (v) {
                    is AsmStructure -> {
                        v.property[lastProp.asValueName] ?: error("Cannot navigate '$this' from null value")
                    }

                    else -> error("Cannot navigate '$this' from null value")
                }
            }
        }
    }
}
