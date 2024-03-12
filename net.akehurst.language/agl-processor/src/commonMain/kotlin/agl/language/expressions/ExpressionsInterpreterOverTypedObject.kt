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

package net.akehurst.language.agl.language.expressions

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.asm.*
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.api.asm.*
import net.akehurst.language.api.language.expressions.*
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.typemodel.api.PropertyDeclaration
import net.akehurst.language.typemodel.api.TypeDeclaration
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.simple.PropertyDeclarationDerived
import net.akehurst.language.typemodel.simple.PropertyDeclarationPrimitive
import net.akehurst.language.typemodel.simple.PropertyDeclarationStored
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib

interface TypedObject {
    val type: TypeInstance

    fun getPropertyValue(propertyDeclaration: PropertyDeclaration): TypedObject

    fun asString(): String
}

class TypedObjectAsmValue(
    val typeModel: TypeModel,
    val self: AsmValue
) : TypedObject {
    override val type
        get() = typeModel.findByQualifiedNameOrNull(self.qualifiedTypeName)?.type()
            ?: SimpleTypeModelStdLib.AnyType

    override fun getPropertyValue(propertyDeclaration: PropertyDeclaration): TypedObject {
        val ao = when (propertyDeclaration) {
            is PropertyDeclarationDerived -> TODO()
            is PropertyDeclarationPrimitive -> {
                val type = this.type.declaration
                val typeProps = StdLibPrimitiveExecutions.property[type] ?: error("StdLibPrimitiveExecutions not found for TypeDeclaration '${type.qualifiedName}'")
                val propExec = typeProps[propertyDeclaration]
                    ?: error("StdLibPrimitiveExecutions not found for property '${propertyDeclaration.name}' of TypeDeclaration '${type.qualifiedName}'")
                propExec.invoke(self, propertyDeclaration)
            }

            is PropertyDeclarationStored -> when (self) {
                is AsmStructure -> self.getProperty(propertyDeclaration.name)
                else -> error("Cannot evaluate property '${propertyDeclaration.name}' on object of type '${self::class.simpleName}'")
            }

            else -> error("Subtype of PropertyDeclaration not handled: '${this::class.simpleName}'")
        }
        return TypedObjectAsmValue(typeModel, ao)
    }

    override fun asString(): String = self.asString()
}

fun AsmValue.toTypedObject(typeModel: TypeModel) = TypedObjectAsmValue(typeModel, this)
val TypedObject.asm
    get() = when (this) {
        is TypedObjectAsmValue -> this.self
        else -> error("Not possible to convert ${this::class.simpleName} to AsmValue")
    }

class ExpressionsInterpreterOverTypedObject(
    val typeModel: TypeModel
) {

    val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)

    fun evaluateStr(self: TypedObject, expression: String): TypedObject {
        val result = Agl.registry.agl.expressions.processor!!.process(expression)
        check(result.issues.errors.isEmpty()) { result.issues.toString() }
        val asm = result.asm!!
        return this.evaluateExpression(self, asm)
    }

    fun evaluateExpression(self: TypedObject, expression: Expression): TypedObject = when (expression) {
        is RootExpression -> this.evaluateRootExpression(self, expression)
        is LiteralExpression -> this.evaluateLiteralExpression(expression)
        is NavigationExpression -> this.evaluateNavigation(self, expression)
        is CreateTupleExpression -> this.evaluateCreateTuple(self, expression)
        is WithExpression -> this.evaluateWith(self, expression)
        else -> error("Subtype of Expression not handled in 'evaluateFor'")
    }

    private fun evaluateRootExpression(self: TypedObject, expression: RootExpression): TypedObject {
        return when {
            expression.isNothing -> AsmNothingSimple.toTypedObject(typeModel)
            expression.isSelf -> {
                //_issues.error(null, "evaluation of 'self' only works if self is a String, got an object of type '${self::class.simpleName}'")
                self
            }

            else -> evaluatePropertyName(self, expression.name)
        }
    }

    private fun evaluateLiteralExpression(expression: LiteralExpression): TypedObject = when (expression.typeName) {
        LiteralExpressionSimple.BOOLEAN -> AsmPrimitiveSimple(SimpleTypeModelStdLib.Boolean.qualifiedTypeName, expression.value)
        LiteralExpressionSimple.INTEGER -> AsmPrimitiveSimple(SimpleTypeModelStdLib.Integer.qualifiedTypeName, expression.value)
        LiteralExpressionSimple.REAL -> AsmPrimitiveSimple(SimpleTypeModelStdLib.Real.qualifiedTypeName, expression.value)
        LiteralExpressionSimple.STRING -> AsmPrimitiveSimple(SimpleTypeModelStdLib.String.qualifiedTypeName, expression.value)
        else -> error("should not happen")
    }.toTypedObject(typeModel)

    private fun evaluateNavigation(self: TypedObject, expression: NavigationExpression): TypedObject {
        // start should be a RootExpression or LiteralExpression
        val st = expression.start
        val start = when (st) {
            is LiteralExpression -> evaluateLiteralExpression(st)
            is RootExpression -> evaluateRootExpression(self, st)
            else -> error("should not happen")
        }
        val result =
            expression.parts.fold(start) { acc, it ->
                when (it) {
                    is PropertyCall -> evaluatePropertyName(acc, it.propertyName)
                    is MethodCall -> TODO()
                    is IndexOperation -> evaluateIndexOperation(acc, it.indices)
                    else -> error("should not happen")
                }
            }
        return result
    }

    fun evaluatePropertyName(self: TypedObject, propertyName: String): TypedObject {
        val type = self.type
        val pd = type.declaration.findPropertyOrNull(propertyName)
        return when (pd) {
            null -> {
                issues.error(null, "Property '$propertyName' not found on type '${self.type.typeName}'")
                AsmNothingSimple.toTypedObject(typeModel)
            }

            else -> self.getPropertyValue(pd)
        }
    }

    fun evaluateIndexOperation(self: TypedObject, indices: List<Expression>): TypedObject {
        val asm = self.asm
        return when (asm) {
            is AsmList -> {
                when (indices.size) {
                    1 -> {
                        val idx = evaluateExpression(self, indices[0])
                        when {
                            idx.asm.isStdInteger -> {
                                val i = (idx.asm as AsmPrimitive).value as Int
                                asm.elements.getOrNull(i) ?: run {
                                    issues.error(null, "Index '$i' out of range")
                                    AsmNothingSimple
                                }
                            }

                            else -> {
                                issues.error(null, "Index value must evaluate to an Integer for Lists")
                                AsmNothingSimple
                            }
                        }
                    }

                    else -> {
                        issues.error(null, "Only one index value should be used for Lists")
                        AsmNothingSimple
                    }
                }
            }

            else -> {
                issues.error(null, "Index operation on non List value is not possible: ${self.asString()}")
                AsmNothingSimple
            }
        }.toTypedObject(typeModel)
    }

    fun evaluateWith(self: TypedObject, expression: WithExpression): TypedObject {
        val newSelf = evaluateExpression(self, expression.withContext)
        val result = evaluateExpression(newSelf, expression.expression)
        return result
    }

    fun evaluateCreateTuple(self: TypedObject, expression: CreateTupleExpression): TypedObject {
        val ns = typeModel.findOrCreateNamespace("interpreter")
        val tuple = AsmStructureSimple()
    }

}

object StdLibPrimitiveExecutions {

    val property = mapOf<TypeDeclaration, Map<PropertyDeclaration, ((AsmValue, PropertyDeclaration) -> AsmValue)>>(
        SimpleTypeModelStdLib.List to mapOf(
            SimpleTypeModelStdLib.List.findPropertyOrNull("size")!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmPrimitiveSimple.stdInteger(self.elements.size)
            },
            SimpleTypeModelStdLib.List.findPropertyOrNull("first")!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.elements.first()
            },
            SimpleTypeModelStdLib.List.findPropertyOrNull("last")!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.elements.last()
            },
            SimpleTypeModelStdLib.List.findPropertyOrNull("back")!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmListSimple(self.elements.drop(1))
            },
            SimpleTypeModelStdLib.List.findPropertyOrNull("front")!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmListSimple(self.elements.dropLast(1))
            },
            SimpleTypeModelStdLib.List.findPropertyOrNull("join")!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmPrimitiveSimple.stdString(self.elements.joinToString(separator = "") { it.asString() })
            }
        ),
        SimpleTypeModelStdLib.ListSeparated to mapOf(
            SimpleTypeModelStdLib.ListSeparated.findPropertyOrNull("items")!! to { self, prop ->
                check(self is AsmListSeparated) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmListSimple(self.elements.items)
            },
            SimpleTypeModelStdLib.ListSeparated.findPropertyOrNull("separators")!! to { self, prop ->
                check(self is AsmListSeparated) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmListSimple(self.elements.separators)
            },
        )
    )

}