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

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.asm.AsmElementSimple
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.language.expressions.Navigation
import net.akehurst.language.api.language.expressions.RootExpression
import net.akehurst.language.typemodel.api.PropertyDeclaration
import net.akehurst.language.typemodel.simple.PropertyDeclarationDerived
import net.akehurst.language.typemodel.simple.PropertyDeclarationPrimitive
import net.akehurst.language.typemodel.simple.PropertyDeclarationStored
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib

fun AsmElementSimple.evaluateStr(expression: String): Any? {
    val result = Agl.registry.agl.expressions.processor!!.process(expression)
    check(result.issues.errors.isEmpty()) { result.issues.toString() }
    val asm = result.asm!!
    return asm.evaluateFor(this)
}

fun Expression.evaluateFor(self: Any?) = when (this) {
    is RootExpression -> this.evaluateFor(self)
    is Navigation -> this.evaluateFor(self)
    else -> error("Subtype of Expression not handled in 'evaluateFor'")
}

fun RootExpression.evaluateFor(self: Any?): String? = when {
    this.isNothing -> null
    this.isSelf -> when (self) {
        null -> null
        is String -> self
        else -> error("evaluation of 'self' only works if self is a String, got an object of type '${self::class.simpleName}'")
    }

    else -> error("evaluateFor RootExpression not handled")
}

fun Navigation.evaluateFor(self: Any?) = when (self) {
    null -> error("Cannot navigate from 'null'")
    is AsmElementSimple -> {
        this.value.fold(self as Any?) { acc, it ->
            when (acc) {
                null -> null
                is AsmElementSimple -> acc.getPropertyOrNull(it)
                is List<*> -> SimpleTypeModelStdLib.List.findPropertyOrNull(it)?.evaluateFor(acc)
                else -> error("Cannot evaluate $this on object of type '${acc::class.simpleName}'")
            }
        }
    }

    else -> error("evaluateFor Navigation from object of type '${self::class.simpleName}' not handled")
}

fun PropertyDeclaration.evaluateFor(self: Any): Any? = when (this) {
    is PropertyDeclarationDerived -> TODO()
    is PropertyDeclarationPrimitive -> this.expression.invoke(self)
    is PropertyDeclarationStored -> when (self) {
        is AsmElementSimple -> self.properties[this.name]?.value
        else -> error("Cannot evaluate property '${this.name}' on object of type '${self::class.simpleName}'")
    }

    else -> error("Subtype of PropertyDeclaration not handled: '${this::class.simpleName}'")
}