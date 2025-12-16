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

package net.akehurst.language.agl.expressions.processor

import net.akehurst.language.base.api.Indent
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.*
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.*

class TypedObjectAny<SelfType : Any>(
    override val type: TypeInstance,
    override val self: SelfType
) : TypedObject<SelfType> {

    override fun asString(indent: Indent): String = "$indent$self"

    override fun hashCode(): Int = self.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypedObject<*> -> false
        else -> self == other.self
    }

    override fun toString(): String = "$self : ${type.qualifiedTypeName}"
}

class ObjectGraphAny<SelfType : Any>(
    override val nodes: Set<TypedObject<SelfType>>,
    override val edges: Set<ObjectGraphEdge<SelfType>>
) : ObjectGraph<SelfType> {

}

data class ObjectGraphEdgeAny<SelfType : Any>(
    override val source: TypedObject<SelfType>,
    override val target: TypedObject<SelfType>,
    override val property: PropertyDeclaration
) : ObjectGraphEdge<SelfType>

abstract class ObjectGraphByReflectionAbstract<StructureType : Any>(
    override var typesDomain: TypesDomain,
    val issues: IssueHolder,
) : ObjectGraphAccessorMutatorCommon<Any> {

    override val createdStructuresByType = mutableMapOf<TypeInstance, List<StructureType>>()

    fun untypedAny(possiblyTypedObject: Any?): Any {
        return when (possiblyTypedObject) {
            null -> Unit
            is TypedObject<*> -> untyped(possiblyTypedObject as TypedObject<Any>)
            is List<*> -> possiblyTypedObject.map { untypedAny(it) }
            is Set<*> -> possiblyTypedObject.map { untypedAny(it) }.toSet()
            is Map<*, *> -> possiblyTypedObject.entries.associate { Pair(untypedAny(it.key), untypedAny(it.value)) }
            else -> possiblyTypedObject
        }
    }

    override fun toTypedObject(obj: Any?): TypedObject<Any> = when {
        null == obj -> nothing()
        Unit == obj -> nothing()
        obj is TypedObject<*> -> obj as TypedObject<Any>
        else -> when (obj) {
            is Boolean -> TypedObjectAny(StdLibDefault.Boolean, obj)
            is Int -> TypedObjectAny(StdLibDefault.Integer, obj)
            is Long -> TypedObjectAny(StdLibDefault.Integer, obj)
            is Float -> TypedObjectAny(StdLibDefault.Real, obj)
            is Double -> TypedObjectAny(StdLibDefault.Real, obj)
            is String -> TypedObjectAny(StdLibDefault.String, obj)
            is List<*> -> createCollection(StdLibDefault.List.qualifiedName, obj.map { toTypedObject(it) })
            is Set<*> -> createCollection(StdLibDefault.Set.qualifiedName, obj.map { toTypedObject(it) }.toSet())
            is Map<*, *> -> createCollection(
                StdLibDefault.Map.qualifiedName,
                obj.map { (k, v) ->
                    val key = toTypedObject(k)
                    val value = toTypedObject(v)
                    val p = Pair(key, value)
                    TypedObjectAny(StdLibDefault.Pair.type(listOf(key.type.asTypeArgument, value.type.asTypeArgument)), p)
                }
            )

            else -> TypedObjectAny(typeFor(obj), obj)
        }
    }

    override fun untyped(typedObj: TypedObject<Any>): Any {
        return untypedAny(typedObj.self)
    }

    override fun isNothing(obj: TypedObject<Any>): Boolean = obj.self == Unit
    override fun equalTo(lhs: TypedObject<Any>, rhs: TypedObject<Any>): Boolean = lhs.self == rhs.self

    override fun nothing(): TypedObject<Any> = TypedObjectAny(StdLibDefault.NothingType, Unit)
    override fun any(value: Any): TypedObject<Any> = TypedObjectAny(StdLibDefault.AnyType, value)

    override fun createPrimitiveValue(qualifiedTypeName: QualifiedName, value: Any) = toTypedObject(value)

    override fun createTupleValue(typeArgs: List<TypeArgumentNamed>): TypedObject<Any> {
        val tupleType = StdLibDefault.TupleType
        val tuple = mutableMapOf<String, Any>()
        return TypedObjectAny(tupleType.type(typeArgs), tuple)
    }

    override fun createCollection(qualifiedTypeName: QualifiedName, collection: Iterable<TypedObject<Any>>): TypedObject<Any> {
        return when (qualifiedTypeName) {
            StdLibDefault.List.qualifiedName -> {
                val elType = collection.firstOrNull()?.type ?: StdLibDefault.AnyType //TODO: should really take comon supertype !
                val type = StdLibDefault.List.type(listOf(elType.asTypeArgument))
                TypedObjectAny(type, collection.toList())
            }

            StdLibDefault.ListSeparated.qualifiedName -> {
                val list = collection.toList()
                val elType = list.getOrNull(0)?.type ?: StdLibDefault.AnyType
                val sepType = list.getOrNull(1)?.type ?: StdLibDefault.AnyType
                TypedObjectAny(StdLibDefault.ListSeparated.type(listOf(elType.asTypeArgument, sepType.asTypeArgument)), list.toSeparatedList())
            }

            StdLibDefault.Set.qualifiedName -> {
                val elType = collection.firstOrNull()?.type ?: StdLibDefault.AnyType //TODO: should really take comon supertype !
                val type = StdLibDefault.Set.type(listOf(elType.asTypeArgument))
                TypedObjectAny(type, collection.toSet())
            }

            StdLibDefault.Map.qualifiedName -> {
                val fstElType = collection.firstOrNull()?.type ?: StdLibDefault.Pair.type(listOf(StdLibDefault.AnyType.asTypeArgument, StdLibDefault.AnyType.asTypeArgument))
                val keyType = fstElType.typeArguments[0]
                val valType = fstElType.typeArguments[1]
                val map = collection.associate { it.self as Pair<Any, Any> }
                TypedObjectAny(StdLibDefault.Map.type(listOf(keyType, valType)), map)
            }

            else -> error("Unsupported collection type: '${qualifiedTypeName.value}'")
        }
    }

    override fun valueOf(value: TypedObject<Any>): Any = untyped(value)

    override fun getIndex(tobj: TypedObject<Any>, index: Int): TypedObject<Any> {
        val self = untyped(tobj)
        return when (self) {
            is List<*> -> {
                val el = self.getOrNull(index.toInt())
                when (el) {
                    null -> {
                        issues.error(null, "In getIndex argument index '$index' out of range")
                        nothing()
                    }

                    else -> toTypedObject(el)
                }
            }

            else -> {
                issues.error(null, "getIndex not supported on type '${tobj.type.typeName}'")
                nothing()
            }
        }
    }

    override fun forEachIndexed(tobj: TypedObject<Any>, body: (index: Int, value: TypedObject<Any>) -> Unit) {
        val self = untyped(tobj)
        when (self) {
            is List<*> -> {
                self.forEachIndexed { index, el -> body(index, toTypedObject(el)) }
            }

            else -> {
                issues.error(null, "forEachIndexed not supported on type '${tobj.type.typeName}'")
                nothing()
            }
        }
    }

    override fun cast(tobj: TypedObject<Any>, newType: TypeInstance): TypedObject<Any> {
        return TypedObjectAny(newType, tobj.self)
    }

    protected fun addCreatedStructure(type: TypeInstance, obj: StructureType) {
        var list = this.createdStructuresByType[type]
        if (null == list) {
            list = mutableListOf(obj)
            this.createdStructuresByType[type] = list
        } else {
            (list as MutableList).add(obj)
        }
    }

    override fun getCompositeGraphFrom(resultGraphIdentity: String, roots: List<TypedObject<Any>>): ObjectGraph<Any> {
        val nodes = mutableSetOf<TypedObject<Any>>()
        val edges = mutableSetOf<ObjectGraphEdge<Any>>()

        TODO("Needs a Komposite walker!")
        return ObjectGraphAny(nodes, edges)
    }
}

