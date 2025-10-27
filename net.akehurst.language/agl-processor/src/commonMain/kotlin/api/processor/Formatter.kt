/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.api.processor

import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.formatter.api.AglFormatDomain
import net.akehurst.language.objectgraph.api.TypedObject

data class EvaluationContext<SelfType:Any>(
    val parent: EvaluationContext<SelfType>?,
    val namedValues: Map<String, TypedObject<SelfType>>
) {
    companion object {
        fun <SelfType:Any> of(namedValues: Map<String, TypedObject<SelfType>>, parent: EvaluationContext<SelfType>? = null) = EvaluationContext(parent, namedValues)
        fun <SelfType:Any> ofSelf(self: TypedObject<SelfType>, namedValues: Map<String, TypedObject<SelfType>> = emptyMap()):EvaluationContext<SelfType> {
            val env = namedValues.toMutableMap()
            env[RootExpressionDefault.SELF.name] = self
            return of(env)
        }
    }

    val self = namedValues[RootExpressionDefault.SELF.name]

    fun getOrInParent(name: String): TypedObject<SelfType>? = namedValues[name] ?: parent?.getOrInParent(name)

    fun child(namedValues: Map<String, TypedObject<SelfType>>) = of(namedValues, this)

    fun childSelf(self: TypedObject<SelfType>) = ofSelf(self, this.namedValues)

    override fun toString(): String {
        val sb = StringBuilder()
        this.parent?.let {
            sb.append(it.toString())
            sb.append("----------\n")
        }
        this.namedValues.forEach {
            sb.append(it.key)
            sb.append(" := ")
            sb.append(it.value.toString())
            sb.append("\n")
        }
        return sb.toString()
    }
}

interface Formatter<SelfType:Any> {
    val formatDomain: AglFormatDomain
    fun formatSelf(formatSetName: PossiblyQualifiedName, self:SelfType): FormatResult
    fun format(formatSetName: PossiblyQualifiedName, evc: EvaluationContext<SelfType>): FormatResult
}