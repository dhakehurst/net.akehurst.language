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

import net.akehurst.kotlinx.utils.Indent
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.expressions.api.FunctionDefinition
import net.akehurst.language.expressions.api.FunctionDefinitionFloating
import net.akehurst.language.expressions.api.TypeReference
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.types.api.*

interface TypedObject {
    val accessor: ObjectGraphAccessorMutatorCommon
    val self: Any
    val type: TypeInstance

    fun getProperty(name:String): TypedObject
    suspend fun getPropertySuspend(name:String) : TypedObject

    fun setProperty(name:String, value: TypedObject)
    suspend fun setPropertySuspend(name:String, value: TypedObject)

    fun executeMethod(name: String, argValues: List<TypedObject>): TypedObject
    suspend fun executeMethodSuspend(name: String, argValues: List<TypedObject>): TypedObject

    fun asString(indent: Indent = Indent()): String
}

class EvaluationContext(
    val parent: EvaluationContext?,
    initialNamedValues: Map<String, TypedObject>
) {
    companion object {
        fun of(namedValues: Map<String, TypedObject>, parent: EvaluationContext? = null) = EvaluationContext(parent, namedValues)
        fun ofSelf(
            self: TypedObject,
            namedValues: Map<String, TypedObject> = emptyMap(),
            parent: EvaluationContext? = null
        ): EvaluationContext {
            val env = namedValues.toMutableMap()
            env[RootExpressionDefault.SELF.name] = self
            return of(env, parent = parent)
        }
    }

    val namedValues: Map<String, TypedObject> = initialNamedValues.toMutableMap()
    val self get() = getOrInParent(RootExpressionDefault.SELF.name)

    val executionTrace: List<String> = mutableListOf()

    fun getOrInParent(name: String): TypedObject? = namedValues[name] ?: parent?.getOrInParent(name)

    fun child(namedValues: Map<String, TypedObject> = emptyMap()) = of(namedValues, this)

    fun childSelf(self: TypedObject, namedValues: Map<String, TypedObject> = emptyMap()) = ofSelf(self, namedValues, parent = this)

    fun setNamedValue(name:String, value: TypedObject): EvaluationContext {
        (namedValues as MutableMap)[name] = value
        return this
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

interface PrimitiveExecutor {
    fun propertyValue(obj: Any, typeDef: TypeDefinition, property: PropertyDeclaration): ExecutionResult?
    fun methodCall(obj: Any, typeDef: TypeDefinition, method: MethodDefinition, args: List<*>): ExecutionResult?
    fun functionCall(functionName: String, args: List<*>): ExecutionResult?

    suspend fun methodCallSuspend(obj: Any, typeDef: TypeDefinition, method: MethodDefinition, args: List<*>): ExecutionResult?

}

interface ObjectGraph {
    val nodes: Set<TypedObject>
    val edges: Set<ObjectGraphEdge>
}

interface ObjectGraphEdge {
    val source: TypedObject
    val target: TypedObject
    val property: PropertyDeclaration
}

interface ObjectGraphAccessorMutatorCommon {
    var typesDomain: TypesDomain
    val createdStructuresByType: Map<TypeInstance, List<Any>>

    fun typeFor(obj: Any?, ifNotFound: TypeInstance): TypeInstance
    fun toTypedObject(obj: Any?, ifNotFound: TypeInstance): TypedObject
    fun untyped(typedObj: TypedObject): Any
    fun typedAs(obj:Any, type:TypeInstance): TypedObject

    fun isNothing(obj: TypedObject): Boolean
    fun equalTo(lhs: TypedObject, rhs: TypedObject): Boolean

    fun nothing(): TypedObject
    fun any(value: Any): TypedObject

    /**
     * kotlin value of value (as opposed to untyped which returns something of type SelfType)
     */
    fun valueOf(value: TypedObject): Any

    // would like to use Long as index to be compatible with Integer implemented as Long - but index in underlying kotlin is always an Int
    fun getFromListWithIndex(tobj: TypedObject, index: Int): TypedObject
    fun getFromMapWithKey(tobj: TypedObject, key: TypedObject): TypedObject
    fun forEachIndexed(tobj: TypedObject, body: (index: Int, value: TypedObject) -> Unit)

    fun callFunction(functionName: String, args: List<TypedObject>, typeReferenceResolver:(TypeReference)-> TypeInstance): TypedObject
    fun cast(tobj: TypedObject, newType: TypeInstance): TypedObject

    fun createPrimitiveValue(qualifiedTypeName: QualifiedName, value: Any): TypedObject
    fun createTupleValue(typeArgs: List<TypeArgumentNamed>): TypedObject
    fun createCollection(collectionType: TypeInstance, collection: Iterable<TypedObject>): TypedObject
    fun createCollectionFromQualifiedName(qualifiedTypeName: QualifiedName, collection: Iterable<TypedObject>): TypedObject
    fun collectionUnion(collection1: TypedObject, collection2: TypedObject): TypedObject

    fun getCompositeGraphFrom(resultGraphIdentity: String, roots: List<TypedObject>): ObjectGraph
}

interface ExternalGetter {
    fun typeFor(obj: Any, ifNotFound:TypeInstance): TypeInstance
    fun createStructure(qualifiedName: QualifiedName, constructorArgs: Map<String, Any>): Any?
    fun getProperty(obj: Any, propertyName: String): Any?
    fun setProperty(obj: Any, propertyName: String, value: Any?)

    fun createStructureSuspend(qualifiedName: QualifiedName, constructorArgs: Map<String, Any>): Any?
    suspend fun getPropertySuspend(obj: Any, propertyName: String): Any?
}

interface FunctionLib {
    val declaration: Map<String, FunctionDefinitionFloating>

    fun findFirstFunctionNamed(functionName:String): FunctionDefinitionFloating?
}

interface ObjectGraphAccessorMutator : ObjectGraphAccessorMutatorCommon {
    val primitiveExecutor: PrimitiveExecutor
    val externalGetter: ExternalGetter
    val functionLib: FunctionLib

    fun createLambdaValue(lambda: (it: TypedObject) -> TypedObject): TypedObject

    fun createStructureValue(possiblyQualifiedTypeName: PossiblyQualifiedName, constructorArgs: Map<String, TypedObject>): TypedObject

    /**
     * value of the given PropertyDeclaration or Nothing if no such property exists
     */
    fun getProperty(tobj: TypedObject, propertyName: String): TypedObject

    fun executeMethod(tobj: TypedObject, methodName: String, args: List<TypedObject>): TypedObject

    fun setProperty(tobj: TypedObject, propertyName: String, value: TypedObject)

    suspend fun createLambdaValueSuspend(lambda: suspend (it: TypedObject) -> TypedObject): TypedObject
    suspend fun createStructureValueSuspend(possiblyQualifiedTypeName: PossiblyQualifiedName, constructorArgs: Map<String, TypedObject>): TypedObject
    suspend fun getPropertySuspend(tobj: TypedObject, propertyName: String): TypedObject
    suspend fun setPropertySuspend(tobj: TypedObject, propertyName: String, value: TypedObject)
    suspend fun executeMethodSuspend(tobj: TypedObject, methodName: String, args: List<TypedObject>): TypedObject

}

interface ExternalGetterSuspending {
    fun typeFor(obj: Any): TypeInstance
    suspend fun createStructure(qualifiedName: QualifiedName, constructorArgs: Map<String, Any>): Any?
    suspend fun getProperty(obj: Any, propertyName: String): Any?
}

interface PrimitiveExecutorSuspending {
    fun propertyValue(obj: Any, typeDef: TypeDefinition, property: PropertyDeclaration): ExecutionResult?
    suspend fun methodCall(obj: Any, typeDef: TypeDefinition, method: MethodDefinition, args: List<*>): ExecutionResult?
    fun functionCall(functionName: String, args: List<*>): ExecutionResult?
}
/*
interface ObjectGraphAccessorMutator : ObjectGraphAccessorMutatorCommon {
    val primitiveExecutor: PrimitiveExecutorSuspending
    val externalGetter: ExternalGetterSuspending

    fun createLambdaValue(lambda: suspend (it: TypedObject) -> TypedObject): TypedObject

    suspend fun createStructureValue(possiblyQualifiedTypeName: PossiblyQualifiedName, constructorArgs: Map<String, TypedObject>): TypedObject

    /**
     * value of the given PropertyDeclaration or Nothing if no such property exists
     */
    suspend fun getProperty(tobj: TypedObject, propertyName: String): TypedObject

    suspend fun executeMethod(tobj: TypedObject, methodName: String, args: List<TypedObject>): TypedObject

    suspend fun setProperty(tobj: TypedObject, propertyName: String, value: TypedObject)

}
*/