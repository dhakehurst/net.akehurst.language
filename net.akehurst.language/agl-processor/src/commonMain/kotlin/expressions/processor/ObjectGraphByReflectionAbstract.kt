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

abstract class ObjectGraphByReflectionAbstract<SelfType : Any>(
    override var typesDomain: TypesDomain,
    val issues: IssueHolder,
) : ObjectGraphAccessorMutatorCommon<SelfType> {

    override val createdStructuresByType = mutableMapOf<TypeInstance, List<SelfType>>()

    fun untyped(typedObj: TypedObject<SelfType>): Any {
        val obj = typedObj.self
        return when (obj) {
            is List<*> -> obj.map { untypedAny(it) }
            is Map<*, *> -> obj.entries.associate { Pair(untypedAny(it.key), untypedAny(it.value)) }
            else -> obj
        }
    }

    fun untypedAny(possiblyTypedObject: Any?): Any {
        return when (possiblyTypedObject) {
            null -> Unit
            is TypedObject<*> -> untyped(possiblyTypedObject as TypedObject<SelfType>)
            is List<*> -> possiblyTypedObject.map { untypedAny(it) }
            is Map<*, *> -> possiblyTypedObject.entries.associate { Pair(untypedAny(it.key), untypedAny(it.value)) }
            else -> possiblyTypedObject
        }
    }

    override fun toTypedObject(obj: SelfType?): TypedObject<SelfType> = when {
        obj is TypedObject<*> -> obj as TypedObject<SelfType>
        else -> obj?.let { TypedObjectAny(typeFor(obj), obj) } ?: nothing()
    }

    override fun isNothing(obj: TypedObject<SelfType>): Boolean = obj.self == Unit
    override fun equalTo(lhs: TypedObject<SelfType>, rhs: TypedObject<SelfType>): Boolean = lhs.self == rhs.self

    override fun nothing(): TypedObject<SelfType> = TypedObjectAny(StdLibDefault.NothingType, Unit) as TypedObject<SelfType>
    override fun any(value: Any): TypedObject<SelfType> = TypedObjectAny(StdLibDefault.AnyType, value) as TypedObject<SelfType>

    override fun createPrimitiveValue(qualifiedTypeName: QualifiedName, value: Any) = toTypedObject(value as SelfType?)

    override fun createTupleValue(typeArgs: List<TypeArgumentNamed>): TypedObject<SelfType> {
        val tupleType = StdLibDefault.TupleType
        val tuple = mutableMapOf<String, Any>()
        return TypedObjectAny(tupleType.type(typeArgs), tuple) as TypedObject<SelfType>
    }

    override fun createStructureValue(possiblyQualifiedTypeName: PossiblyQualifiedName, constructorArgs: Map<String, TypedObject<SelfType>>): TypedObject<SelfType> {
        val typeDef = typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(possiblyQualifiedTypeName)
            ?: error("Cannot createStructureValue, no type found for '$possiblyQualifiedTypeName'")
        val obj = when (typeDef) {
            is SingletonType -> typeDef.objectInstance()
            is StructuredType -> when (typeDef) {
                is DataType -> typeDef.constructDataType(*(constructorArgs.values.map { untyped(it) }.toTypedArray<Any>()))
                is ValueType -> typeDef.constructValueType(constructorArgs.values.map { untyped(it) }.first()) //TODO: special method
                is CollectionType -> error("use 'createCollection' for CollectionType")
                is InterfaceType -> error("Should not create an instance of a InterfaceType")
                else -> error("Unsupported subtype of StructuredType: '${typeDef::class.simpleName}'")
            }

            is SpecialType -> error("Should not create an instance of a SpecialType")
            is PrimitiveType -> error("use 'createPrimitiveValue' for PrimitiveType")
            is EnumType -> error("use '??' for EnumType")
            is TupleType -> error("use 'createTupleValue' for TupleType")
            is UnionType -> error("Should not create an instance of a UnionType")
            else -> error("Unsupported subtype of TypeDefinition: '${typeDef::class.simpleName}'")
        }
        val type = typeDef.type()
        addCreatedStructure(type, obj as SelfType)
        return TypedObjectAny(type, obj) as TypedObject<SelfType>
    }

    override fun createCollection(qualifiedTypeName: QualifiedName, collection: Iterable<TypedObject<SelfType>>): TypedObject<SelfType> {
        return when (qualifiedTypeName) {
            StdLibDefault.List.qualifiedName -> {
                val elType = collection.firstOrNull()?.type ?: StdLibDefault.AnyType //TODO: should really take comon supertype !
                val type = StdLibDefault.List.type(listOf(elType.asTypeArgument))
                TypedObjectAny(type, collection.toList()) as TypedObject<SelfType>
            }

            StdLibDefault.ListSeparated.qualifiedName -> {
                val list = collection.toList()
                val elType = list.getOrNull(0)?.type ?: StdLibDefault.AnyType
                val sepType = list.getOrNull(1)?.type ?: StdLibDefault.AnyType
                TypedObjectAny(StdLibDefault.ListSeparated.type(listOf(elType.asTypeArgument, sepType.asTypeArgument)), list.toSeparatedList()) as TypedObject<SelfType>
            }

            StdLibDefault.Set.qualifiedName -> {
                val elType = collection.firstOrNull()?.type ?: StdLibDefault.AnyType //TODO: should really take comon supertype !
                val type = StdLibDefault.Set.type(listOf(elType.asTypeArgument))
                TypedObjectAny(type, collection.toList()) as TypedObject<SelfType>
            }

            StdLibDefault.Map.qualifiedName -> {
                val fstElType = collection.firstOrNull()?.type ?: StdLibDefault.Pair.type(listOf(StdLibDefault.AnyType.asTypeArgument, StdLibDefault.AnyType.asTypeArgument))
                val keyType = fstElType.typeArguments[0]
                val valType = fstElType.typeArguments[1]
                val map = collection.associate { it as Pair<Any, Any> }
                TypedObjectAny(StdLibDefault.Map.type(listOf(keyType, valType)), map) as TypedObject<SelfType>
            }

            else -> error("Unsupported collection type: '${qualifiedTypeName.value}'")
        }
    }

    override fun valueOf(value: TypedObject<SelfType>): Any = untyped(value)

    override fun getIndex(tobj: TypedObject<SelfType>, index: Int): TypedObject<SelfType> {
        val self = untyped(tobj)
        return when (self) {
            is List<*> -> {
                val el = self.getOrNull(index.toInt())
                when (el) {
                    null -> {
                        issues.error(null, "In getIndex argument index '$index' out of range")
                        nothing()
                    }

                    else -> toTypedObject(el as SelfType)
                }
            }

            else -> {
                issues.error(null, "getIndex not supported on type '${tobj.type.typeName}'")
                nothing()
            }
        }
    }

    override fun forEachIndexed(tobj: TypedObject<SelfType>, body: (index: Int, value: TypedObject<SelfType>) -> Unit) {
        val self = untyped(tobj)
        when (self) {
            is List<*> -> {
                self.forEachIndexed { index, el -> body(index, toTypedObject(el as SelfType?)) }
            }

            else -> {
                issues.error(null, "forEachIndexed not supported on type '${tobj.type.typeName}'")
                nothing()
            }
        }
    }

    override fun cast(tobj: TypedObject<SelfType>, newType: TypeInstance): TypedObject<SelfType> {
        return TypedObjectAny(newType, tobj.self)
    }

    private fun addCreatedStructure(type: TypeInstance, obj: SelfType) {
        var list = this.createdStructuresByType[type]
        if (null == list) {
            list = mutableListOf(obj)
            this.createdStructuresByType[type] = list
        } else {
            (list as MutableList).add(obj)
        }
    }

    override fun getCompositeGraphFrom(resultGraphIdentity: String, roots: List<TypedObject<SelfType>>): ObjectGraph<SelfType> {
        val nodes = mutableSetOf<TypedObject<SelfType>>()
        val edges = mutableSetOf<ObjectGraphEdge<SelfType>>()

        TODO("Needs a Komposite walker!")
        return ObjectGraphAny(nodes, edges)
    }
}

