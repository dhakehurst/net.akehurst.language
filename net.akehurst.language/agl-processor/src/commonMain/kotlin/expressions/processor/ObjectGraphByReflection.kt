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

import net.akehurst.kotlinx.reflect.reflect
import net.akehurst.language.asm.simple.AsmPrimitiveSimple
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.collections.ListSeparated
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.expressions.processor.ObjectGraph
import net.akehurst.language.expressions.processor.TypedObject
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.typemodel.api.CollectionType
import net.akehurst.language.typemodel.api.DataType
import net.akehurst.language.typemodel.api.EnumType
import net.akehurst.language.typemodel.api.InterfaceType
import net.akehurst.language.typemodel.api.MethodDeclaration
import net.akehurst.language.typemodel.api.MethodName
import net.akehurst.language.typemodel.api.PrimitiveType
import net.akehurst.language.typemodel.api.PropertyDeclaration
import net.akehurst.language.typemodel.api.PropertyName
import net.akehurst.language.typemodel.api.SingletonType
import net.akehurst.language.typemodel.api.SpecialType
import net.akehurst.language.typemodel.api.StructuredType
import net.akehurst.language.typemodel.api.TupleType
import net.akehurst.language.typemodel.api.TypeArgumentNamed
import net.akehurst.language.typemodel.api.TypeDefinition
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.api.UnionType
import net.akehurst.language.typemodel.api.ValueType
import net.akehurst.language.typemodel.asm.PropertyDeclarationDerived
import net.akehurst.language.typemodel.asm.PropertyDeclarationPrimitive
import net.akehurst.language.typemodel.asm.PropertyDeclarationStored
import net.akehurst.language.typemodel.asm.StdLibDefault
import net.akehurst.language.typemodel.asm.TypeArgumentNamedSimple

object StdLibPrimitiveExecutionsForReflection {
    val property = mapOf<TypeDefinition, Map<PropertyDeclaration, ((Any, PropertyDeclaration) -> Any)>>(
        StdLibDefault.List to mapOf(
            StdLibDefault.List.findAllPropertyOrNull(PropertyName("size"))!! to { self, prop ->
                check(self is List<*>) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmPrimitiveSimple.stdInteger(self.size)
            },
            StdLibDefault.List.findAllPropertyOrNull(PropertyName("first"))!! to { self, prop ->
                check(self is List<*>) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.first() as Any
            },
            StdLibDefault.List.findAllPropertyOrNull(PropertyName("last"))!! to { self, prop ->
                check(self is List<*>) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.last() as Any
            },
            StdLibDefault.List.findAllPropertyOrNull(PropertyName("back"))!! to { self, prop ->
                check(self is List<*>) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.drop(1)
            },
            StdLibDefault.List.findAllPropertyOrNull(PropertyName("front"))!! to { self, prop ->
                check(self is List<*>) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.dropLast(1)
            },
            StdLibDefault.List.findAllPropertyOrNull(PropertyName("join"))!! to { self, prop ->
                check(self is List<*>) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.joinToString(separator = "") {
                    when (it) {
                        is TypedObject<*> -> it.self.toString()
                        else -> it.toString()
                    }
                }
            },
            StdLibDefault.List.findAllPropertyOrNull(PropertyName("asMap"))!! to { self, prop ->
                check(self is List<*>) { "Method '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.associate {
                    val el = when (it) {
                        is TypedObject<*> -> it.self
                        else -> it
                    }
                    when (el) {
                        is Pair<*, *> -> el
                        is Map<*, *> -> when {
                            el.containsKey("key") && el.containsKey("value") -> Pair(el["key"], el["value"])
                            else -> error("To convert a List<Map> via 'asMap' there must be a 'key' and a 'value' entry")
                        }

                        else -> error("To convert a List via 'asMap' the elements must be either Pairs or Maps with key and value entries")
                    }
                }
            },
        ),
        StdLibDefault.ListSeparated to mapOf(
            StdLibDefault.ListSeparated.findAllPropertyOrNull(PropertyName("items"))!! to { self, prop ->
                check(self is ListSeparated<*, *, *>) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.items
            },
            StdLibDefault.ListSeparated.findAllPropertyOrNull(PropertyName("separators"))!! to { self, prop ->
                check(self is ListSeparated<*, *, *>) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.separators
            },
        )
    )

    val method = mapOf<TypeDefinition, Map<MethodDeclaration, ((Any, MethodDeclaration, List<TypedObject<Any>>) -> Any)>>(
        StdLibDefault.List to mapOf(
            StdLibDefault.List.findAllMethodOrNull(MethodName("get"))!! to { self, meth, args ->
                check(self is List<*>) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' has wrong number of argument, expecting 1, received ${args.size}" }
                check(args[0].self is Int) { "Method '${meth.name}' takes an ${StdLibDefault.Integer.qualifiedTypeName} as its argument, received ${args[0].type.qualifiedTypeName}" }
                check(StdLibDefault.Integer.qualifiedTypeName == args[0].type.qualifiedTypeName) { "Method '${meth.name}' takes an ${StdLibDefault.Integer.qualifiedTypeName} as its argument, received ${args[0].type.qualifiedTypeName}" }
                val idx = args[0].self as Int
                self[idx] as Any
            },

            /*
            StdLibDefault.Collection.findAllMethodOrNull(MethodName("map"))!! to { self, meth, args ->
                check(self is List<*>) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' takes 1 lambda argument got ${args.size} arguments." }
                check(args[0].self is Function<*>) { "Method '${meth.name}' first argument must be a lambda, got '${args[0].self::class.simpleName}'." }
                val lambda = args[0].self as Function<*>
                (self as List<Any>).map {
                    val args = mapOf("it" to it)
                    lambda.invoke(args)
                }
            }
             */
        ),
    )
}

class TypedObjectAny<SelfType : Any>(
    override val type: TypeInstance,
    override val self: SelfType
) : TypedObject<SelfType> {

    override fun asString(): String = self.toString()

    override fun hashCode(): Int = self.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypedObject<*> -> false
        else -> self == other.self
    }

    override fun toString(): String = "$self : ${type.qualifiedTypeName}"
}

open class ObjectGraphByReflection<SelfType : Any>(
    override var typeModel: TypeModel,
    val issues: IssueHolder
) : ObjectGraph<SelfType> {

    //fun Any.toTypedObject(type: TypeInstance) = TypedObjectByReflection(type, this)

    override fun typeFor(obj: SelfType?): TypeInstance {
        return when (obj) {
            null -> StdLibDefault.NothingType
            is Boolean -> StdLibDefault.Boolean
            is Int -> StdLibDefault.Integer
            is String -> StdLibDefault.String
            is Double -> StdLibDefault.Real
            is List<*> -> StdLibDefault.List.type(listOf(StdLibDefault.AnyType.asTypeArgument))
            is Map<*, *> -> {
                val me = obj.entries.firstOrNull()
                when (me) {
                    null -> StdLibDefault.Map.type(listOf(StdLibDefault.AnyType.asTypeArgument, StdLibDefault.AnyType.asTypeArgument))
                    else -> when (me.key) {
                        is String -> {
                            val ttargs = obj.map { (k, v) -> TypeArgumentNamedSimple(PropertyName(k as String), typeFor(v as SelfType)) }
                            StdLibDefault.TupleType.type(ttargs)
                        }

                        else -> StdLibDefault.Map.type(listOf(StdLibDefault.AnyType.asTypeArgument, StdLibDefault.AnyType.asTypeArgument))
                    }
                }
            }

            else -> {
                val tp = typeModel.findFirstDefinitionByNameOrNull(SimpleName(obj::class.simpleName!!)) //TODO: use qualified name when kotlin-common supports it
                when (tp) {
                    null -> {
                        issues.error(null, "ObjectGraphByReflection cannot get type for ${obj::class.simpleName}")
                        StdLibDefault.AnyType
                    }

                    else -> tp.type()
                }
            }
        }
    }

    override fun toTypedObject(obj: SelfType?): TypedObject<SelfType> = when {
        obj is TypedObject<*> -> obj as TypedObject<SelfType>
        else -> obj?.let { TypedObjectAny(typeFor(obj), obj) } ?: nothing()
    }

    override fun nothing(): TypedObject<SelfType> = TypedObjectAny(StdLibDefault.NothingType, Unit) as TypedObject<SelfType>
    override fun any(value: Any): TypedObject<SelfType> = TypedObjectAny(StdLibDefault.AnyType, value) as TypedObject<SelfType>

    override fun createPrimitiveValue(qualifiedTypeName: QualifiedName, value: Any) = toTypedObject(value as SelfType?)

    override fun createTupleValue(typeArgs: List<TypeArgumentNamed>): TypedObject<SelfType> {
        val tupleType = StdLibDefault.TupleType
        val tuple = mutableMapOf<String, Any>()
        return TypedObjectAny(tupleType.type(typeArgs), tuple) as TypedObject<SelfType>
    }

    override fun createStructureValue(possiblyQualifiedTypeName: PossiblyQualifiedName, constructorArgs: Map<String, TypedObject<SelfType>>): TypedObject<SelfType> {
        val typeDef = typeModel.findFirstDefinitionByPossiblyQualifiedNameOrNull(possiblyQualifiedTypeName)
            ?: error("Cannot createStructureValue, no type found for '$possiblyQualifiedTypeName'")
        val obj = when (typeDef) {
            is SingletonType -> typeDef.objectInstance()
            is StructuredType -> when (typeDef) {
                is DataType -> typeDef.constructDataType(*(constructorArgs.values.map{it.self}.toTypedArray<Any>()))
                is ValueType -> typeDef.constructValueType(constructorArgs.values.map{it.self}.first()) //TODO: special method
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
        return TypedObjectAny(typeDef.type(), obj) as TypedObject<SelfType>
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

    override fun createLambdaValue(lambda: (it: TypedObject<SelfType>) -> TypedObject<SelfType>): TypedObject<SelfType> {
        val lambdaType = StdLibDefault.Lambda //TODO: typeargs like tuple
        val lmb = { it: Any -> lambda.invoke(toTypedObject(it as SelfType)).self }
        return TypedObjectAny(lambdaType, lmb) as TypedObject<SelfType>
    }

    override fun valueOf(value: TypedObject<SelfType>): Any = value.self

    override fun getIndex(tobj: TypedObject<SelfType>, index: Any): TypedObject<SelfType> {
        val idx = when (index) {
            is TypedObject<*> -> {
                index.self
            }

            else -> index
        }
        val self = tobj.self
        return when (self) {
            is List<*> -> when (idx) {
                is Int -> {
                    val el = self.getOrNull(idx)
                    when (el) {
                        null -> {
                            issues.error(null, "In getIndex argument index '$index' out of range")
                            nothing()
                        }

                        else -> toTypedObject(el as SelfType)

                    }
                }

                else -> {
                    issues.error(null, "In getIndex argument 'index' must be an Int for Lists")
                    nothing()
                }
            }

            else -> {
                issues.error(null, "getIndex not supported on type '${tobj.type.typeName}'")
                nothing()
            }
        }
    }

    override fun getProperty(tobj: TypedObject<SelfType>, propertyName: String): TypedObject<SelfType> {
        return when {
            StdLibDefault.TupleType == tobj.type.resolvedDeclaration -> {
                when (tobj.self) {
                    is Map<*, *> -> {
                        val value = (tobj.self as Map<String, Any>)[propertyName]
                        value?.let { toTypedObject(it as SelfType?) } ?: nothing()
                    }

                    else -> nothing()
                }
            }

            else -> {
                val propRes = tobj.type.allResolvedProperty[PropertyName(propertyName)]
                when (propRes) {
                    null -> {
                        val obj = tobj.self
                        val value = obj.reflect().getProperty(propertyName)
                        value?.let { toTypedObject(it as SelfType?) } ?: nothing()
                    }

                    else -> when (propRes.original) {
                        is PropertyDeclarationDerived -> TODO()
                        is PropertyDeclarationPrimitive -> {
                            val type = tobj.type.resolvedDeclaration
                            val typeProps = StdLibPrimitiveExecutionsForReflection.property[type]
                                ?: error("StdLibPrimitiveExecutions not found for TypeDeclaration '${type.qualifiedName}'")
                            val propExec = typeProps[propRes.original]
                                ?: error("StdLibPrimitiveExecutionsForReflection not found for property '${propertyName}' of TypeDeclaration '${type.qualifiedName}'")
                            toTypedObject(propExec.invoke(tobj.self, propRes) as SelfType?)
                        }

                        is PropertyDeclarationStored -> {
                            val obj = tobj.self
                            val value = obj.reflect().getProperty(propertyName)
                            value?.let { toTypedObject(it as SelfType?) } ?: nothing()
                        }

                        else -> error("Subtype of PropertyDeclaration not handled: '${this::class.simpleName}'")
                    }
                }
            }
        }
    }

    override fun setProperty(tobj: TypedObject<SelfType>, propertyName: String, value: TypedObject<SelfType>) {
        when {
            StdLibDefault.TupleType == tobj.type.resolvedDeclaration -> {
                when (tobj.self) {
                    is MutableMap<*, *> -> {
                        (tobj.self as MutableMap<String, Any>)[propertyName] = value.self
                    }
                }
            }

            else -> {
                val obj = tobj.self
                obj.reflect().setProperty(propertyName, value.self)
            }
        }
    }

    override fun executeMethod(tobj: TypedObject<SelfType>, methodName: String, args: List<TypedObject<SelfType>>): TypedObject<SelfType> {
        val obj = tobj.self
        val arguments = args.map { it.self }
        val value = obj.reflect().call(methodName, arguments)
        return value?.let { toTypedObject(it as SelfType?) } ?: nothing()
    }

    override fun cast(tobj: TypedObject<SelfType>, newType: TypeInstance): TypedObject<SelfType> {
        return TypedObjectAny(newType, tobj.self)
    }

}