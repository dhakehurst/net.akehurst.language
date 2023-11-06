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

import net.akehurst.language.agl.asm.AsmNothingSimple
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.api.asm.*
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.language.expressions.Navigation
import net.akehurst.language.api.language.expressions.RootExpression
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.typemodel.api.PropertyDeclaration
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.simple.PropertyDeclarationDerived
import net.akehurst.language.typemodel.simple.PropertyDeclarationPrimitive
import net.akehurst.language.typemodel.simple.PropertyDeclarationStored
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib

class ExpressionsInterpreterOverAsmSimple(
    //val typeModel: TypeModel
) {

    private val _issues = IssueHolder(LanguageProcessorPhase.INTERPRETER)

    fun evaluateStr(self: AsmValue, expression: String): AsmValue {
        val result = Agl.registry.agl.expressions.processor!!.process(expression)
        check(result.issues.errors.isEmpty()) { result.issues.toString() }
        val asm = result.asm!!
        return this.evaluateExpression(self, asm)
    }

    fun evaluateExpression(self: AsmValue, expression: Expression): AsmValue = when (expression) {
        is RootExpression -> this.evaluateRootExpression(self, expression)
        is Navigation -> this.evaluateNavigation(self, expression)
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

    private fun evaluateNavigation(self: AsmValue, expression: Navigation): AsmValue = when (self) {
        is AsmNothing -> {
            _issues.error(null, "Cannot navigate from '$AsmNothingSimple'")
            AsmNothingSimple
        }

        is AsmStructure -> {
            expression.value.fold(self as AsmValue) { acc, it ->
                when (acc) {
                    is AsmNothing -> acc
                    is AsmStructure -> acc.getProperty(it)
                    is AsmList -> {
                        val pd = SimpleTypeModelStdLib.List.findPropertyOrNull(it)
                        pd?.let { evaluatePropertyDeclaration(acc, it) } ?: AsmNothingSimple
                    }

                    is AsmListSeparated -> {
                        val pd = SimpleTypeModelStdLib.List.findPropertyOrNull(it)
                        pd?.let { evaluatePropertyDeclaration(acc, it) } ?: AsmNothingSimple
                    }

                    else -> error("Cannot evaluate $this on object of type '${acc::class.simpleName}'")
                }
            }
        }

        else -> {
            _issues.error(null, "evaluateFor Navigation from object of type '${self::class.simpleName}' not handled")
            AsmNothingSimple
        }
    }

    fun evaluatePropertyDeclaration(self: AsmValue, propertyDeclaration: PropertyDeclaration): AsmValue = when (propertyDeclaration) {
        is PropertyDeclarationDerived -> TODO()
        is PropertyDeclarationPrimitive -> propertyDeclaration.expression.invoke(self) as AsmValue
        is PropertyDeclarationStored -> when (self) {
            is AsmStructure -> self.getProperty(propertyDeclaration.name)
            else -> error("Cannot evaluate property '${propertyDeclaration.name}' on object of type '${self::class.simpleName}'")
        }

        else -> error("Subtype of PropertyDeclaration not handled: '${this::class.simpleName}'")
    }

    private fun TypeModel.typeOf(self: AsmValue): TypeInstance =
        this.findByQualifiedNameOrNull(self.qualifiedTypeName)?.instance()
            ?: SimpleTypeModelStdLib.AnyType

}