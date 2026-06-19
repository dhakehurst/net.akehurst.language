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

import net.akehurst.kotlinx.collections.OrderedSet
import net.akehurst.kotlinx.collections.toOrderedSet
import net.akehurst.kotlinx.utils.Indent
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.collections.ListSeparated
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.*
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.*
import kotlin.time.Instant

private class TypedObjectAny(
    override val accessor: ObjectGraphAccessorMutator,
    override val type: TypeInstance,
    override val self: Any
) : TypedObject {

    override fun getProperty(name: String) = accessor.getProperty(this, name)
    override suspend fun getPropertySuspend(name: String) = accessor.getPropertySuspend(this, name)

    override fun setProperty(name: String, value: TypedObject) = accessor.setProperty(this, name, value)
    override suspend fun setPropertySuspend(name: String, value: TypedObject) = accessor.setProperty(this, name, value) //TODO: Suspend version

    override fun executeMethod(name: String, argValues: List<TypedObject>) = accessor.executeMethod(this, name, argValues)
    override suspend fun executeMethodSuspend(name: String, argValues: List<TypedObject>) = accessor.executeMethodSuspend(this, name, argValues)

    override fun asString(indent: Indent): String = "$indent$self"

    override fun hashCode(): Int = self.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypedObject -> false
        else -> self == other.self
    }

    override fun toString(): String = "$self : ${type.qualifiedTypeName}"
}

class ObjectGraphAny(
    override val nodes: Set<TypedObject>,
    override val edges: Set<ObjectGraphEdge>
) : ObjectGraph {

}

data class ObjectGraphEdgeAny(
    override val source: TypedObject,
    override val target: TypedObject,
    override val property: PropertyDeclaration
) : ObjectGraphEdge

abstract class ObjectGraphAccessorMutatorCommonByReflectionAbstract<StructureType : Any>(
    override var typesDomain: TypesDomain,
    override val issues: IssueHolder,
    override val locationMap: LocationMap
) : ObjectGraphAccessorMutator {

    override val createdStructuresByType = mutableMapOf<TypeInstance, List<StructureType>>()

    fun issueError(obj: Any?, message: String) {
        val loc = obj?.let {  locationMap[obj] }
        issues.error(loc, message)
    }

    fun issueErrorReturnNothing(obj: Any?, message: String): TypedObject {
        issueError(obj,message)
        return nothing()
    }

    fun issueWarningReturnNothing(obj: Any?, message: String): TypedObject {
        val loc = obj?.let {  locationMap[obj] }
        issues.warn(loc, message)
        return nothing()
    }

    fun untypedAny(possiblyTypedObject: Any?): Any {
        return when (possiblyTypedObject) {
            null -> Unit
            is TypedObject -> untyped(possiblyTypedObject as TypedObject)
            is List<*> -> possiblyTypedObject.map { untypedAny(it) }
            is Set<*> -> possiblyTypedObject.map { untypedAny(it) }.toSet()
            is Map<*, *> -> possiblyTypedObject.entries.associate { Pair(untypedAny(it.key), untypedAny(it.value)) }
            else -> possiblyTypedObject
        }
    }

    override fun toTypedObject(obj: Any?, ifNotFound: TypeInstance): TypedObject = when {
        null == obj -> nothing()
        Unit == obj -> nothing()
        obj is TypedObject -> obj as TypedObject
        else -> when (obj) {
            is Boolean -> typedAs(obj, StdLibDefault.Boolean)
            is Byte -> typedAs(obj.toLong(), StdLibDefault.Integer)
            is Short -> typedAs(obj.toLong(), StdLibDefault.Integer)
            is Int -> typedAs(obj.toLong(), StdLibDefault.Integer)
            is Long -> typedAs(obj, StdLibDefault.Integer)
            is Float -> typedAs(obj.toDouble(), StdLibDefault.Real)
            is Double -> typedAs(obj, StdLibDefault.Real)
            is Char -> typedAs(obj.toString(), StdLibDefault.String) //TODO: do we need an explicit Char type ?
            is String -> typedAs(obj, StdLibDefault.String)
            is Instant -> typedAs(obj, StdLibDefault.Timestamp)
            is Throwable -> typedAs(obj, StdLibDefault.Exception)
            is Pair<*, *> -> {
                val ifFstTypeNotFound = when {
                    StdLibDefault.Pair == ifNotFound.resolvedDefinition && 0 < ifNotFound.typeArguments.size -> ifNotFound.typeArguments[0].type
                    else -> StdLibDefault.AnyType
                }
                val ifSndTypeNotFound = when {
                    StdLibDefault.Pair == ifNotFound.resolvedDefinition && 1 < ifNotFound.typeArguments.size -> ifNotFound.typeArguments[1].type
                    else -> StdLibDefault.AnyType
                }
                val fst = toTypedObject(obj.first, ifFstTypeNotFound)
                val snd = toTypedObject(obj.second, ifSndTypeNotFound)
                when {
                    StdLibDefault.Pair == ifNotFound.resolvedDefinition -> typedAs(obj, ifNotFound)
                    else -> typedAs(Pair(fst, snd), typeFor(obj, ifNotFound))
                }
            }

            is OrderedSet<*> -> toTypedCollection(obj.toList(), StdLibDefault.OrderedSet, ifNotFound) { it.toOrderedSet() }
            is ListSeparated<*, *, *> -> {
                TODO()
            }

            is Array<*> -> toTypedCollection(obj.toList(), StdLibDefault.List, ifNotFound) { it.toList() }
            //TODO: special array types IntArray, LongArray, etc
            is List<*> -> toTypedCollection(obj, StdLibDefault.List, ifNotFound) { it.toList() }
            is Set<*> -> toTypedCollection(obj, StdLibDefault.Set, ifNotFound) { it.toSet() }

            is Map<*, *> -> {
                val ifKeyTypeNotFound = when {
                    StdLibDefault.Map == ifNotFound.resolvedDefinition && 0 < ifNotFound.typeArguments.size -> ifNotFound.typeArguments[0].type
                    else -> StdLibDefault.AnyType
                }
                val ifValueTypeNotFound = when {
                    StdLibDefault.Map == ifNotFound.resolvedDefinition && 1 < ifNotFound.typeArguments.size -> ifNotFound.typeArguments[1].type
                    else -> StdLibDefault.AnyType
                }
                val entries = obj.map { (k, v) ->
                    val key = toTypedObject(k, ifKeyTypeNotFound)
                    val value = toTypedObject(v, ifValueTypeNotFound)
                    val p = Pair(key, value)
                    typedAs(p, StdLibDefault.Pair.type(listOf(key.type.asTypeArgument, value.type.asTypeArgument)))
                }
                when {
                    StdLibDefault.Map == ifNotFound.resolvedDefinition -> createCollection(ifNotFound, entries)
                    else -> createCollection(StdLibDefault.Map.type(listOf(ifKeyTypeNotFound.asTypeArgument, ifValueTypeNotFound.asTypeArgument)), entries)
                }
            }

            is Iterator<*> -> toTypedCollection(obj.asSequence().toList(), StdLibDefault.List, ifNotFound) { it.toList() }
            is Function<*> -> typedAs(obj, StdLibDefault.Lambda)
            else -> typedAs(obj, typeFor(obj, ifNotFound))
        }
    }

    override fun untyped(typedObj: TypedObject): Any {
        return untypedAny(typedObj.self)
    }

    override fun typedAs(obj: Any, type: TypeInstance): TypedObject = TypedObjectAny(this, type, obj)

    override fun isNothing(obj: TypedObject): Boolean = obj.self == Unit
    override fun equalTo(lhs: TypedObject, rhs: TypedObject): Boolean = lhs.self == rhs.self

    override fun nothing(): TypedObject = typedAs(Unit, StdLibDefault.NothingType)
    override fun any(value: Any): TypedObject = typedAs(value, StdLibDefault.AnyType)

    override fun createPrimitiveValue(qualifiedTypeName: QualifiedName, value: Any) = toTypedObject(value, StdLibDefault.AnyType)

    override fun createTupleValue(typeArgs: List<TypeArgumentNamed>): TypedObject {
        val tupleType = StdLibDefault.TupleType
        val tuple = mutableMapOf<String, Any>()
        return typedAs(tuple, tupleType.type(typeArgs))
    }

    override fun createCollection(collectionType: TypeInstance, collection: Iterable<TypedObject>): TypedObject {
        return when (collectionType.qualifiedTypeName) {
            StdLibDefault.List.qualifiedName -> {
                val elTypeArg = collectionType.typeArguments.firstOrNull()
                /* this computes the 'real' list type, but it is expensive !
               val elType = when (elTypeArg) {
                    null -> {
                        val first = collection.firstOrNull()
                        when (first) {
                            null -> StdLibDefault.AnyType
                            else -> collection
                                .runningFold(first.type) { acc, it -> acc.commonSuperType(it.type) }
                                .takeWhile { it != StdLibDefault.AnyType }
                                .last()
                        }
                    }

                    else -> elTypeArg.type
                }*/
                val type = StdLibDefault.List.type(listOf(elTypeArg ?: StdLibDefault.AnyType.asTypeArgument))
                typedAs(collection.toList(), type)
            }

            StdLibDefault.ListSeparated.qualifiedName -> {
                val list = collection.toList()
                val elType = collectionType.typeArguments.getOrNull(0)?.type ?: StdLibDefault.AnyType
                val sepType = collectionType.typeArguments.getOrNull(1)?.type ?: StdLibDefault.AnyType
                typedAs(list.toSeparatedList(), StdLibDefault.ListSeparated.type(listOf(elType.asTypeArgument, sepType.asTypeArgument)))
            }

            StdLibDefault.Set.qualifiedName -> {
                val elType = collectionType.typeArguments.firstOrNull()?.type ?: StdLibDefault.AnyType
                val type = StdLibDefault.Set.type(listOf(elType.asTypeArgument))
                typedAs(collection.toSet(), type)
            }

            StdLibDefault.Map.qualifiedName -> {
                val keyType = collectionType.typeArguments.getOrNull(0)?.type ?: StdLibDefault.AnyType
                val valType = collectionType.typeArguments.getOrNull(1)?.type ?: StdLibDefault.AnyType
                val map = collection.associate { it.self as Pair<Any, Any> }
                typedAs(map, StdLibDefault.Map.type(listOf(keyType.asTypeArgument, valType.asTypeArgument)))
            }

            else -> error("Unsupported collection type: '${collectionType.qualifiedTypeName.value}'")
        }
    }

    override fun createCollectionFromQualifiedName(qualifiedTypeName: QualifiedName, collection: Iterable<TypedObject>): TypedObject {
        val colType = when (qualifiedTypeName) {
            StdLibDefault.List.qualifiedName -> StdLibDefault.List.type(listOf(StdLibDefault.AnyType.asTypeArgument))
            StdLibDefault.ListSeparated.qualifiedName -> StdLibDefault.ListSeparated.type(listOf(StdLibDefault.AnyType.asTypeArgument))
            StdLibDefault.Set.qualifiedName -> StdLibDefault.Set.type(listOf(StdLibDefault.AnyType.asTypeArgument))
            StdLibDefault.Map.qualifiedName -> StdLibDefault.Map.type(listOf(StdLibDefault.AnyType.asTypeArgument, StdLibDefault.AnyType.asTypeArgument))
            else -> error("Unsupported collection type: '${qualifiedTypeName.value}'")
        }
        return createCollection(colType, collection)
    }

    override fun valueOf(value: TypedObject): Any = untyped(value)

    override fun getFromListWithIndex(tobj: TypedObject, index: Int): TypedObject {
        val self = untyped(tobj)
        return when (self) {
            is List<*> -> {
                val el = self.getOrNull(index.toInt())
                when (el) {
                    null -> {
                        issueError(null, "In getFromListWithIndex argument index '$index' out of range")
                        nothing()
                    }

                    else -> toTypedObject(el, StdLibDefault.AnyType)
                }
            }

            else -> {
                issueError(null, "getFromListWithIndex not supported on type '${tobj.type.typeName}'")
                nothing()
            }
        }
    }

    override fun getFromMapWithKey(tobj: TypedObject, key: TypedObject): TypedObject {
        val self = untyped(tobj)
        val k = untyped(key)
        return when (self) {
            is Map<*, *> -> {
                val el = self.get(k)
                when (el) {
                    null -> nothing()
                    else -> toTypedObject(el, StdLibDefault.AnyType)
                }
            }

            else -> {
                issueError(null, "getFromMapWithKey not supported on type '${tobj.type.typeName}'")
                nothing()
            }
        }
    }

    override fun forEachIndexed(tobj: TypedObject, body: (index: Int, value: TypedObject) -> Unit) {
        val self = untyped(tobj)
        when (self) {
            is List<*> -> {
                self.forEachIndexed { index, el -> body(index, toTypedObject(el, StdLibDefault.AnyType)) }
            }

            else -> {
                issueError(null, "forEachIndexed not supported on type '${tobj.type.typeName}'")
                nothing()
            }
        }
    }

    override fun collectionUnion(collection1: TypedObject, collection2: TypedObject): TypedObject {
        //TODO: this is inefficient
        val col1 = untyped(collection1) as Iterable<Any>
        val col2 = untyped(collection2) as Iterable<Any>
        val union = toTypedObject(col1 + col2, StdLibDefault.Collection.type(listOf(StdLibDefault.AnyType.asTypeArgument)))
        return union
    }

    override fun cast(tobj: TypedObject, newType: TypeInstance): TypedObject {
        return typedAs(tobj.self, newType)
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

    override fun getCompositeGraphFrom(resultGraphIdentity: String, roots: List<TypedObject>): ObjectGraph {
        val nodes = mutableSetOf<TypedObject>()
        val edges = mutableSetOf<ObjectGraphEdge>()

        TODO("Needs a Komposite walker!")
        return ObjectGraphAny(nodes, edges)
    }

    private fun toTypedCollection(obj: Iterable<*>, baseCollType: TypeDefinition, ifNotFound: TypeInstance, func: (Iterable<TypedObject>) -> Iterable<TypedObject>): TypedObject {
        val ifElementTypeNotFound = when {
            baseCollType == ifNotFound.resolvedDefinition && 0 < ifNotFound.typeArguments.size -> ifNotFound.typeArguments[0].type
            else -> StdLibDefault.AnyType
        }
        val els = obj.map { toTypedObject(it, ifElementTypeNotFound) }
        val elements = func.invoke(els)
        return when {
            baseCollType == ifNotFound.resolvedDefinition -> createCollection(ifNotFound, elements)
            else -> createCollection(baseCollType.type(listOf(ifElementTypeNotFound.asTypeArgument)), elements)
        }
    }
}

