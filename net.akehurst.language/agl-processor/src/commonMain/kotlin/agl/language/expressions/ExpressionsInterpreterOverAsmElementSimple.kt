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

import net.akehurst.language.agl.asm.AsmListSimple
import net.akehurst.language.agl.asm.AsmNothingSimple
import net.akehurst.language.agl.asm.AsmPrimitiveSimple
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.api.asm.*
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.language.expressions.NavigationExpression
import net.akehurst.language.api.language.expressions.RootExpression
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.typemodel.api.PropertyDeclaration
import net.akehurst.language.typemodel.api.TypeDeclaration
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.simple.PropertyDeclarationDerived
import net.akehurst.language.typemodel.simple.PropertyDeclarationPrimitive
import net.akehurst.language.typemodel.simple.PropertyDeclarationStored
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib

class ExpressionsInterpreterOverAsmSimple(
    val typeModel: TypeModel
) {

    private val _issues = IssueHolder(LanguageProcessorPhase.INTERPRET)

    fun evaluateStr(self: AsmValue, expression: String): AsmValue {
        val result = Agl.registry.agl.expressions.processor!!.process(expression)
        check(result.issues.errors.isEmpty()) { result.issues.toString() }
        val asm = result.asm!!
        return this.evaluateExpression(self, asm)
    }

    fun evaluateExpression(self: AsmValue, expression: Expression): AsmValue = when (expression) {
        is RootExpression -> this.evaluateRootExpression(self, expression)
        is NavigationExpression -> this.evaluateNavigation(self, expression)
        else -> error("Subtype of Expression not handled in 'evaluateFor'")
    }

    private fun evaluateRootExpression(self: AsmValue, expression: RootExpression): AsmValue = when {
        expression.isNothing -> AsmNothingSimple
        expression.isSelf -> when (self) {
            is AsmNothing -> self
            is AsmPrimitive -> self
            else -> {
                _issues.error(null, "evaluation of 'self' only works if self is a String, got an object of type '${self::class.simpleName}'")
                AsmNothingSimple
            }
        }

        else -> {
            _issues.error(null, "evaluateFor RootExpression not handled")
            AsmNothingSimple
        }
    }

    private fun evaluateNavigation(self: AsmValue, expression: NavigationExpression): AsmValue =
        expression.value.fold(self) { acc, it ->
            val type = typeModel.typeOf(acc)
            val pd = type.declaration.findPropertyOrNull(it)
            when (pd) {
                null -> {
                    _issues.error(null, "evaluateFor Navigation from object of type '${self::class.simpleName}' not handled")
                    AsmNothingSimple
                }

                else -> evaluatePropertyDeclaration(acc, pd)
            }
        }

    fun evaluatePropertyDeclaration(self: AsmValue, propertyDeclaration: PropertyDeclaration): AsmValue = when (propertyDeclaration) {
        is PropertyDeclarationDerived -> TODO()
        is PropertyDeclarationPrimitive -> {
            val type = typeModel.typeOf(self).declaration
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

    private fun TypeModel.typeOf(self: AsmValue): TypeInstance =
        this.findByQualifiedNameOrNull(self.qualifiedTypeName)?.type()
            ?: SimpleTypeModelStdLib.AnyType

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