/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.objectgraph.api

import net.akehurst.language.base.api.Indent
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.types.api.*

interface TypedObject<out SelfType : Any> {
    val self: SelfType
    val type: TypeInstance
    fun asString(indent: Indent = Indent()): String
}

class EvaluationContext<SelfType : Any>(
    val parent: EvaluationContext<SelfType>?,
    initialNamedValues: Map<String, TypedObject<SelfType>>
) {
    companion object {
        fun <SelfType : Any> of(namedValues: Map<String, TypedObject<SelfType>>, parent: EvaluationContext<SelfType>? = null) = EvaluationContext(parent, namedValues)
        fun <SelfType : Any> ofSelf(
            self: TypedObject<SelfType>,
            namedValues: Map<String, TypedObject<SelfType>> = emptyMap(),
            parent: EvaluationContext<SelfType>? = null
        ): EvaluationContext<SelfType> {
            val env = namedValues.toMutableMap()
            env[RootExpressionDefault.SELF.name] = self
            return of(env, parent = parent)
        }
    }

    val namedValues: Map<String, TypedObject<SelfType>> = initialNamedValues.toMutableMap()
    val self = namedValues[RootExpressionDefault.SELF.name]

    val executionTrace: List<String> = mutableListOf()

    fun getOrInParent(name: String): TypedObject<SelfType>? = namedValues[name] ?: parent?.getOrInParent(name)

    fun child(namedValues: Map<String, TypedObject<SelfType>>) = of(namedValues, this)

    fun childSelf(self: TypedObject<SelfType>, namedValues: Map<String, TypedObject<SelfType>> = emptyMap()) = ofSelf(self, namedValues, parent = this)

    fun setNamedValue(name:String, value: TypedObject<SelfType>) {
        (namedValues as MutableMap)[name] = value
    }

    fun addExecutionTrace(trace: String) {
        (executionTrace as MutableList).add(trace)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        this.parent?.let {
            sb.append(it.toString())
            sb.append("----------\n")
        } ?: run {
            sb.append("\n")
        }
        this.namedValues.forEach {
            sb.append("  ")
            sb.append(it.key)
            sb.append(" := ")
            sb.append(it.value.toString())
            sb.append("\n")
        }
        return sb.toString()
    }
}

data class ExecutionResult(val value: Any?)

interface PrimitiveExecutor<T : Any> {
    fun propertyValue(obj: T, typeDef: TypeDefinition, property: PropertyDeclaration): ExecutionResult?
    fun methodCall(obj: T, typeDef: TypeDefinition, method: MethodDeclaration, args: List<TypedObject<T>>): ExecutionResult?
    fun functionCall(functionName: String, args: List<TypedObject<T>>): ExecutionResult?
}

interface ObjectGraph<SelfType : Any> {
    val nodes: Set<TypedObject<SelfType>>
    val edges: Set<ObjectGraphEdge<SelfType>>
}

interface ObjectGraphEdge<SelfType : Any> {
    val source: TypedObject<SelfType>
    val target: TypedObject<SelfType>
    val property: PropertyDeclaration
}

interface ObjectGraphAccessorMutatorCommon<SelfType : Any> {
    var typesDomain: TypesDomain
    val createdStructuresByType: Map<TypeInstance, List<SelfType>>

    fun typeFor(obj: SelfType?): TypeInstance
    fun toTypedObject(obj: SelfType?): TypedObject<SelfType>
    fun untyped(typedObj: TypedObject<SelfType>): Any

    fun isNothing(obj: TypedObject<SelfType>): Boolean
    fun equalTo(lhs: TypedObject<SelfType>, rhs: TypedObject<SelfType>): Boolean

    fun nothing(): TypedObject<SelfType>
    fun any(value: Any): TypedObject<SelfType>

    /**
     * kotlin value of value (as opposed to untyped which returns something of type SelfType)
     */
    fun valueOf(value: TypedObject<SelfType>): Any

    // would like to use Long as index to be compatible with Integer implemented as Long - but index in underlying kotlin is always an Int
    fun getFromListWithIndex(tobj: TypedObject<SelfType>, index: Int): TypedObject<SelfType>
    fun getFromMapWithKey(tobj: TypedObject<Any>, key: TypedObject<Any>): TypedObject<SelfType>
    fun forEachIndexed(tobj: TypedObject<SelfType>, body: (index: Int, value: TypedObject<SelfType>) -> Unit)

    fun callFunction(functionName: String, args: List<TypedObject<SelfType>>): TypedObject<SelfType>
    fun cast(tobj: TypedObject<SelfType>, newType: TypeInstance): TypedObject<SelfType>

    fun createPrimitiveValue(qualifiedTypeName: QualifiedName, value: Any): TypedObject<SelfType>
    fun createTupleValue(typeArgs: List<TypeArgumentNamed>): TypedObject<SelfType>
    fun createCollection(qualifiedTypeName: QualifiedName, collection: Iterable<TypedObject<SelfType>>): TypedObject<SelfType>

    fun getCompositeGraphFrom(resultGraphIdentity: String, roots: List<TypedObject<SelfType>>): ObjectGraph<SelfType>
}

interface ExternalGetter<T : Any> {
    fun typeFor(obj: T): TypeInstance
    fun createStructure(qualifiedName: QualifiedName, constructorArgs: Map<String, Any>): T?
    fun getProperty(obj: T, propertyName: String): Any?
    fun setProperty(obj: T, propertyName: String, value: Any?)
}

interface ObjectGraphAccessorMutator<SelfType : Any> : ObjectGraphAccessorMutatorCommon<SelfType> {
    val primitiveExecutor: PrimitiveExecutor<SelfType>
    val externalGetter: ExternalGetter<SelfType>

    fun createLambdaValue(lambda: (it: TypedObject<SelfType>) -> TypedObject<SelfType>): TypedObject<SelfType>

    fun createStructureValue(possiblyQualifiedTypeName: PossiblyQualifiedName, constructorArgs: Map<String, TypedObject<SelfType>>): TypedObject<SelfType>

    /**
     * value of the given PropertyDeclaration or Nothing if no such property exists
     */
    fun getProperty(tobj: TypedObject<SelfType>, propertyName: String): TypedObject<SelfType>

    fun executeMethod(tobj: TypedObject<SelfType>, methodName: String, args: List<TypedObject<SelfType>>): TypedObject<SelfType>

    fun setProperty(tobj: TypedObject<SelfType>, propertyName: String, value: TypedObject<SelfType>)

}

interface ExternalGetterSuspending<T : Any> {
    fun typeFor(obj: T): TypeInstance
    suspend fun createStructure(qualifiedName: QualifiedName, constructorArgs: Map<String, Any>): Any?
    suspend fun getProperty(obj: T, propertyName: String): Any?
}

interface PrimitiveExecutorSuspending<T : Any> {
    fun propertyValue(obj: T, typeDef: TypeDefinition, property: PropertyDeclaration): ExecutionResult?
    suspend fun methodCall(obj: T, typeDef: TypeDefinition, method: MethodDeclaration, args: List<TypedObject<T>>): ExecutionResult?
    fun functionCall(functionName: String, args: List<TypedObject<T>>): ExecutionResult?
}

interface ObjectGraphAccessorMutatorSuspending<SelfType : Any> : ObjectGraphAccessorMutatorCommon<SelfType> {
    val primitiveExecutor: PrimitiveExecutorSuspending<SelfType>
    val externalGetter: ExternalGetterSuspending<SelfType>

    fun createLambdaValue(lambda: suspend (it: TypedObject<SelfType>) -> TypedObject<SelfType>): TypedObject<SelfType>

    suspend fun createStructureValue(possiblyQualifiedTypeName: PossiblyQualifiedName, constructorArgs: Map<String, TypedObject<Any>>): TypedObject<SelfType>

    /**
     * value of the given PropertyDeclaration or Nothing if no such property exists
     */
    suspend fun getProperty(tobj: TypedObject<SelfType>, propertyName: String): TypedObject<SelfType>

    suspend fun executeMethod(tobj: TypedObject<SelfType>, methodName: String, args: List<TypedObject<SelfType>>): TypedObject<SelfType>

    suspend fun setProperty(tobj: TypedObject<SelfType>, propertyName: String, value: TypedObject<SelfType>)

}
