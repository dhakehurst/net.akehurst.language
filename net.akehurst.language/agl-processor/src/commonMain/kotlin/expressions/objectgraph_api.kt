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
import net.akehurst.language.types.api.*

interface TypedObject<out SelfType:Any> {
    val self: SelfType
    val type: TypeInstance
    fun asString(indent:Indent = Indent()): String
}

data class ExecutionResult(val value: Any?)

interface PrimitiveExecutor<T : Any> {
    fun propertyValue(obj:T, typeDef: TypeDefinition, property:PropertyDeclaration):ExecutionResult?
    fun methodCall(obj:T, typeDef: TypeDefinition, method: MethodDeclaration, args: List<TypedObject<T>>):ExecutionResult?
    fun functionCall(functionName: String, args: List<TypedObject<T>>):ExecutionResult?
}

interface ObjectGraphAccessorMutator<SelfType:Any> {
    var typesDomain: TypesDomain
    val primitiveExecutor:PrimitiveExecutor<SelfType>
    val createdStructuresByType:Map<TypeInstance, List<SelfType>>

    fun typeFor(obj: SelfType?): TypeInstance
    fun toTypedObject(obj:SelfType?) : TypedObject<SelfType>

    fun isNothing(obj: TypedObject<SelfType>): Boolean
    fun equalTo(lhs: TypedObject<SelfType>, rhs: TypedObject<SelfType>): Boolean

    fun nothing(): TypedObject<SelfType>
    fun any(value: Any): TypedObject<SelfType>

    fun valueOf(value: TypedObject<SelfType>): Any

    // would like to use Long as index to be compatible with Integer implemented as Long - but index in underlying kotlin is always an Int
    fun getIndex(tobj: TypedObject<SelfType>, index: Int): TypedObject<SelfType>
    fun forEachIndexed(tobj: TypedObject<SelfType>, body: (index: Int, value: TypedObject<SelfType>) -> Unit)

    /**
     * value of the given PropertyDeclaration or Nothing if no such property exists
     */
    fun getProperty(tobj: TypedObject<SelfType>, propertyName: String): TypedObject<SelfType>
    fun executeMethod(tobj: TypedObject<SelfType>, methodName: String, args: List<TypedObject<SelfType>>): TypedObject<SelfType>
    fun callFunction(functionName: String, args: List<TypedObject<SelfType>>): TypedObject<SelfType>
    fun cast(tobj: TypedObject<SelfType>, newType: TypeInstance): TypedObject<SelfType>

    fun createPrimitiveValue(qualifiedTypeName: QualifiedName, value: Any): TypedObject<SelfType>
    fun createLambdaValue(lambda: (it: TypedObject<SelfType>) -> TypedObject<SelfType>): TypedObject<SelfType>
    fun createTupleValue(typeArgs: List<TypeArgumentNamed>): TypedObject<SelfType>
    fun createCollection(qualifiedTypeName: QualifiedName, collection:Iterable<TypedObject<SelfType>>): TypedObject<SelfType>
    fun createStructureValue(possiblyQualifiedTypeName: PossiblyQualifiedName, constructorArgs: Map<String, TypedObject<SelfType>>): TypedObject<SelfType>
    fun setProperty(tobj: TypedObject<SelfType>, propertyName: String, value: TypedObject<SelfType>)

    fun getCompositeGraphFrom(resultGraphIdentity:String, roots: List<TypedObject<SelfType>>) : ObjectGraph<SelfType>
}

interface ObjectGraph<SelfType:Any> {
    val nodes: Set<TypedObject<SelfType>>
    val edges: Set<ObjectGraphEdge<SelfType>>
}

interface ObjectGraphEdge<SelfType:Any> {
    val source: TypedObject<SelfType>
    val target: TypedObject<SelfType>
    val property: PropertyDeclaration
}