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
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.collections.ListSeparated
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.collections.transitiveClosure
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.*
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.*
import kotlin.reflect.KProperty1

class StdLibPrimitiveExecutionsForReflectionSuspending<T : Any>(
    val issues: IssueHolder = IssueHolder(LanguageProcessorPhase.INTERPRET)
) : PrimitiveExecutorSuspending<T> {

    private val _property = mutableMapOf<TypeDefinition, MutableMap<PropertyDeclaration, ((Any, PropertyDeclaration) -> Any?)>>(
        StdLibDefault.Collection to mutableMapOf(
            StdLibDefault.Collection.findAllPropertyOrNull(PropertyName("size"))!! to { self, prop ->
                check(self is Collection<*>) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.size.toLong()
            },
            StdLibDefault.Collection.findAllPropertyOrNull(PropertyName("asMap"))!! to { self, prop ->
                check(self is Collection<*>) { "Method '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
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
        StdLibDefault.List to mutableMapOf(
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
        ),
        StdLibDefault.ListSeparated to mutableMapOf(
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

    private val _method = mutableMapOf<TypeDefinition, MutableMap<MethodDeclaration, (suspend (Any, MethodDeclaration, List<TypedObject<Any>>) -> Any?)>>(
        StdLibDefault.String.resolvedDeclaration to mutableMapOf(
            StdLibDefault.String.resolvedDeclaration.findAllMethodOrNull(MethodName("toBoolean"))!! to { self, meth, args ->
                check(self is String) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.toBooleanStrictOrNull() ?: Unit
            },
            StdLibDefault.String.resolvedDeclaration.findAllMethodOrNull(MethodName("toInteger"))!! to { self, meth, args ->
                check(self is String) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.toLongOrNull() ?: Unit
            },
            StdLibDefault.String.resolvedDeclaration.findAllMethodOrNull(MethodName("toReal"))!! to { self, meth, args ->
                check(self is String) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.toDoubleOrNull() ?: Unit
            },
            StdLibDefault.String.resolvedDeclaration.findAllMethodOrNull(MethodName("removeSurrounding"))!! to { self, meth, args ->
                check(self is String) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                val arg1 = args[0].self as String
                self.removeSurrounding(arg1)
            }
        ),

        StdLibDefault.Collection to mutableMapOf(
            StdLibDefault.Collection.findAllMethodOrNull(MethodName("map"))!! to { self, meth, args ->
                check(self is Collection<*>) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' takes 1 lambda argument got ${args.size} arguments." }
                check(args[0].self is Function2<*, *, *>) { "Method '${meth.name}' first argument must be a lambda, got '${args[0].self::class.simpleName}'." }
                val lambda: suspend (Any) -> Any = args[0].self as suspend (Any) -> Any
                (self as Collection<Any>).map {
                    //val args = mapOf("it" to it)
                    lambda.invoke(it)
                }
            },
            StdLibDefault.Collection.findAllMethodOrNull(MethodName("filter"))!! to { self, meth, args ->
                check(self is Collection<*>) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' takes 1 lambda argument got ${args.size} arguments." }
                check(args[0].self is Function2<*, *, *>) { "Method '${meth.name}' first argument must be a lambda, got '${args[0].self::class.simpleName}'." }
                val lambda: suspend (Any) -> Boolean = args[0].self as suspend (Any) -> Boolean
                (self as Collection<Any>).filter {
                    //val args = mapOf("it" to it)
                    lambda.invoke(it)
                }
            }
        ),

        StdLibDefault.List to mutableMapOf(
            StdLibDefault.List.findAllMethodOrNull(MethodName("get"))!! to { self, meth, args ->
                check(self is List<*>) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' has wrong number of argument, expecting 1, received ${args.size}" }
                check(args[0].self is Long) { "Method '${meth.name}' takes an ${StdLibDefault.Integer.qualifiedTypeName} as its argument, received ${args[0].type.qualifiedTypeName}" }
                check(StdLibDefault.Integer.qualifiedTypeName == args[0].type.qualifiedTypeName) { "Method '${meth.name}' takes an ${StdLibDefault.Integer.qualifiedTypeName} as its argument, received ${args[0].type.qualifiedTypeName}" }
                val idx = args[0].self as Long
                self[idx.toInt()] as Any
            },
            StdLibDefault.List.findAllMethodOrNull(MethodName("separate"))!! to { self, meth, args ->
                check(self is List<*>) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(0 == args.size) { "Method '${meth.name}' has wrong number of argument, expecting 0, received ${args.size}" }
                self.toSeparatedList()
            },
            StdLibDefault.List.findAllMethodOrNull(MethodName("map"))!! to { self, meth, args ->
                check(self is List<*>) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' takes 1 lambda argument got ${args.size} arguments." }
                // Lambda has extra paramter for coroutine (becasue it is a suspend function)
                check(args[0].self is Function2<*, *, *>) { "Method '${meth.name}' first argument must be a lambda, got '${args[0].self::class.simpleName}'." }
                val lambda: suspend (Any) -> Any = args[0].self as suspend (Any) -> Any
                (self as List<Any>).map {
                    //val args = mapOf("it" to it)
                    lambda.invoke(it)
                }
            },
            StdLibDefault.List.findAllMethodOrNull(MethodName("filter"))!! to { self, meth, args ->
                check(self is List<*>) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' takes 1 lambda argument got ${args.size} arguments." }
                // Lambda has extra paramter for coroutine (becasue it is a suspend function)
                check(args[0].self is Function2<*, *, *>) { "Method '${meth.name}' first argument must be a lambda, got '${args[0].self::class.simpleName}'." }
                val lambda: suspend (Any) -> Boolean = args[0].self as suspend (Any) -> Boolean
                (self as List<Any>).filter {
                    //val args = mapOf("it" to it)
                    lambda.invoke(it)
                }
            },
            StdLibDefault.List.findAllMethodOrNull(MethodName("transitiveClosure"))!! to { self, meth, args ->
                check(self is List<*>) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' takes 1 lambda argument got ${args.size} arguments." }
                // Lambda has extra paramter for coroutine (becasue it is a suspend function)
                check(args[0].self is Function2<*, *, *>) { "Method '${meth.name}' first argument must be a lambda, got '${args[0].self::class.simpleName}'." }
                val lambda: suspend (Any) -> List<Any> = args[0].self as suspend (Any) -> List<Any>
                (self as List<Any>).transitiveClosure {
                    //val args = mapOf("it" to it)
                    lambda.invoke(it)
                }
            },
        ),
    )

    override fun propertyValue(obj: T, typeDef: TypeDefinition, property: PropertyDeclaration): ExecutionResult? =
        propertyValueDirectOrSuperType(obj, typeDef, property)

    override suspend fun methodCall(obj: T, typeDef: TypeDefinition, method: MethodDeclaration, args: List<TypedObject<T>>): ExecutionResult? =
        methodDirectOrSuperType(obj, typeDef, method, args)

    override fun functionCall(functionName: String, args: List<TypedObject<T>>): ExecutionResult? {
        return when (functionName) {
            "Pair" -> {
                check(2 == args.size) { "The Pair function only takes 2 arguments." }
                ExecutionResult(Pair(args[0], args[1]))
            }
            "Set" -> ExecutionResult(args.map { it.self }.toSet())
            "List" -> ExecutionResult(args.map { it.self })
            else -> error("StdLibPrimitiveExecutionsForReflectionSuspending, unsupported function '$functionName'")
        }
    }

    fun <S> addPropertyExecution1(property: PropertyDeclaration, exec: KProperty1<S, Any?>) {
        addPropertyExecution2(property) { obj, pd -> exec.get(obj as S) }
    }

    fun addPropertyExecution2(property: PropertyDeclaration, execution: (Any, PropertyDeclaration) -> Any?) {
        var typeMap = this._property[property.owner]
        if (null == typeMap) {
            typeMap = mutableMapOf()
            this._property[property.owner] = typeMap
        }
        typeMap[property] = execution
    }

    /** returns null if execution is not found for the given property on the typeDef or its supertypes */
    private fun propertyValueDirectOrSuperType(obj: T, typeDef: TypeDefinition, property: PropertyDeclaration): ExecutionResult? {
        val result = propertyValueDirect(obj, typeDef, property)
        return if (null != result) {
            result
        } else {
            // try supertypes
            typeDef.supertypes.firstNotNullOfOrNull { superType -> propertyValueDirectOrSuperType(obj, superType.resolvedDeclaration, property) }
        }
    }

    /** returns null if execution is not found for the given property on the typeDef */
    private fun propertyValueDirect(obj: T, typeDef: TypeDefinition, property: PropertyDeclaration): ExecutionResult? {
        val propExec = property.execution ?: property.executionSuspend
        return when {
            null == propExec -> {
                val typeProps = this._property[typeDef]
                typeProps?.let {
                    val propExec = typeProps[property]
                    propExec?.let {
                        val v = propExec.invoke(obj, property) as T
                        ExecutionResult(v)
                    }
                }
            }

            propExec !is KProperty1<*, *> -> {
                issues.error(null, "'$property' is not a KProperty1<*,*>, cannot get it")
                null
            }

            else -> try {
                ExecutionResult((propExec as KProperty1<Any, Any?>).get(obj))
            } catch (t: Throwable) {
                issues.error(null, t.message ?: "Error while invoking property execution", t)
                null
            }
        }
    }

    private suspend fun methodDirectOrSuperType(obj: T, typeDef: TypeDefinition, method: MethodDeclaration, args: List<TypedObject<T>>): ExecutionResult? {
        val result = methodDirect(obj, typeDef, method, args)
        return if (null != result) {
            result
        } else {
            // try supertypes
            typeDef.supertypes.firstNotNullOfOrNull { superType -> methodDirectOrSuperType(obj, superType.resolvedDeclaration, method, args) }
        }
    }

    private suspend fun methodDirect(obj: T, typeDef: TypeDefinition, method: MethodDeclaration, args: List<TypedObject<T>>): ExecutionResult? {
        val typeMeths = this._method[typeDef]
        return typeMeths?.let {
            val methExec = typeMeths[method]
            methExec?.let {
                val v = methExec.invoke(obj, method, args) as T
                ExecutionResult(v)
            }
        }
    }
}

class ExternalGetterByReflectionSuspending<SelfType : Any>(
    val typesDomain: TypesDomain,
    val issues: IssueHolder,
) : ExternalGetterSuspending<SelfType> {

    override fun typeFor(obj: SelfType): TypeInstance {
        val tp = typesDomain.findFirstDefinitionByNameOrNull(SimpleName(obj::class.simpleName!!)) //TODO: use qualified name when kotlin-common supports it
        return when (tp) {
            null -> {
                issues.error(null, "ObjectGraphByReflection cannot get type for ${obj::class.simpleName}")
                StdLibDefault.AnyType
            }

            else -> tp.type()
        }
    }

    override suspend fun createStructure(qualifiedName: QualifiedName, constructorArgs: Map<String, Any>): Any? {
        val typeDef = typesDomain.findByQualifiedNameOrNull(qualifiedName)
        return when (typeDef) {
            null -> error("Type Definition for '${qualifiedName.value}' not found.")
            is DataType -> typeDef.constructDataType(*(constructorArgs.values.toTypedArray<Any>()))
            is ValueType -> typeDef.constructValueType(constructorArgs.values.first()) //TODO: special method
            is CollectionType -> error("use 'createCollection' for CollectionType")
            is InterfaceType -> error("Should not create an instance of a InterfaceType")
            else -> error("Unsupported subtype of StructuredType: '${typeDef::class.simpleName}'")
        }
    }

    override suspend fun getProperty(obj: SelfType, propertyName: String): Any? {
        return try {
            obj.reflect().getProperty(propertyName)
        } catch (t: Throwable) {
            issues.error(null, "Unable to evaluate property '$propertyName': ${t.message}")
            null
        }
    }

}

open class ObjectGraphAccessorMutatorSuspendingByReflection(
    typesDomain: TypesDomain,
    issues: IssueHolder,
    override val externalGetter: ExternalGetterSuspending<Any> = ExternalGetterByReflectionSuspending(typesDomain, issues),
    override val primitiveExecutor: PrimitiveExecutorSuspending<Any> = StdLibPrimitiveExecutionsForReflectionSuspending()
) : ObjectGraphAccessorMutatorCommonByReflectionAbstract<Any>(typesDomain, issues), ObjectGraphAccessorMutatorSuspending<Any> {

    override fun typeFor(obj: Any?): TypeInstance {
        return when (obj) {
            null -> StdLibDefault.NothingType
            is Boolean -> StdLibDefault.Boolean
            is Long -> StdLibDefault.Integer
            is String -> StdLibDefault.String
            is Double -> StdLibDefault.Real
            is List<*> -> StdLibDefault.List.type(listOf(StdLibDefault.AnyType.asTypeArgument))
            is Set<*> -> StdLibDefault.Set.type(listOf(StdLibDefault.AnyType.asTypeArgument))
            is Map<*, *> -> {
                val me = obj.entries.firstOrNull()
                when (me) {
                    null -> StdLibDefault.Map.type(listOf(StdLibDefault.AnyType.asTypeArgument, StdLibDefault.AnyType.asTypeArgument))
                    else -> when (me.key) {
                        is String -> {
                            val ttargs = obj.map { (k, v) -> TypeArgumentNamedSimple(PropertyName(k as String), typeFor(v)) }
                            StdLibDefault.TupleType.type(ttargs)
                        }

                        else -> StdLibDefault.Map.type(listOf(StdLibDefault.AnyType.asTypeArgument, StdLibDefault.AnyType.asTypeArgument))
                    }
                }
            }

            else -> externalGetter.typeFor(obj)
        }
    }

    override fun createLambdaValue(lambda: suspend (it: TypedObject<Any>) -> TypedObject<Any>): TypedObject<Any> {
        val lambdaType = StdLibDefault.Lambda //TODO: typeargs like tuple
        val lmb: suspend (Any) -> Any = { it: Any -> untyped(lambda.invoke(toTypedObject(it))) }
        return TypedObjectAny(lambdaType, lmb)
    }

    override suspend fun executeMethod(tobj: TypedObject<Any>, methodName: String, args: List<TypedObject<Any>>): TypedObject<Any> {
        val meth = tobj.type.allResolvedMethod[MethodName(methodName)]
        return when (meth) {
            null -> {
                //try reflection on untyped object, FIXME: will not work currently for JS and wasmJS
                val obj = untyped(tobj)
                val arguments = args.map { untyped(it) }.toTypedArray()
                val value = obj.reflect().call(methodName, *arguments)
                value?.let { toTypedObject(it) } ?: nothing()
            }

            else -> {
                // first try execution
                val type = tobj.type.resolvedDeclaration
                val execResult = primitiveExecutor.methodCall(untyped(tobj), type, meth.original, args)
                when (execResult) {
                    null -> when (meth.original) {
                        is MethodDeclarationDerived -> TODO()
                        is MethodDeclarationPrimitive -> {
                            // should have found execution if there is one
                            issues.error(null, "using StdLibPrimitiveExecutionsForReflection not found for method '${meth}'")
                            nothing()
                        }

                        else -> error("Subtype of MethodDeclaration not handled: '${this::class.simpleName}'")
                    }

                    else -> toTypedObject(execResult.value)
                }
            }
        }
    }

    override fun callFunction(functionName: String, args: List<TypedObject<Any>>): TypedObject<Any> {
        return primitiveExecutor.functionCall(functionName, args)?.let {
            toTypedObject(it.value)
        } ?: nothing()
    }

    override suspend fun createStructureValue(possiblyQualifiedTypeName: PossiblyQualifiedName, constructorArgs: Map<String, TypedObject<Any>>): TypedObject<Any> {
        val typeDef = typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(possiblyQualifiedTypeName)
            ?: error("Cannot createStructureValue, no type found for '$possiblyQualifiedTypeName'")
        val obj = when (typeDef) {
            is SingletonType -> typeDef.objectInstance()
            is StructuredType -> externalGetter.createStructure(typeDef.qualifiedName, constructorArgs.map {(k,v) -> Pair(k,v.self)}.toMap()) ?: Unit

            is SpecialType -> error("Should not create an instance of a SpecialType")
            is PrimitiveType -> error("use 'createPrimitiveValue' for PrimitiveType")
            is EnumType -> error("use '??' for EnumType")
            is TupleType -> error("use 'createTupleValue' for TupleType")
            is UnionType -> error("Should not create an instance of a UnionType")
            else -> error("Unsupported subtype of TypeDefinition: '${typeDef::class.simpleName}'")
        }
        val type = typeDef.type()
        addCreatedStructure(type, obj)
        return TypedObjectAny(type, obj)
    }

    /**
     * when {
     *   type is TupleType -> property from tuple or $nothing
     *   type does not have resolved property -> try reflection
     *   type has resolved property -> try primitiveExecutor or reflection if executor fails
     * }
     */
    override suspend fun getProperty(tobj: TypedObject<Any>, propertyName: String): TypedObject<Any> {
        return when {
            StdLibDefault.TupleType == tobj.type.resolvedDeclaration -> {
                val obj = untyped(tobj)
                when (obj) {
                    is Map<*, *> -> {
                        val value = (obj as Map<String, Any>)[propertyName]
                        value?.let { toTypedObject(it as Any?) } ?: nothing()
                    }

                    else -> nothing()
                }
            }

            else -> {
                val propRes = tobj.type.allResolvedProperty[PropertyName(propertyName)]
                when (propRes) {
                    null -> {
                        //try reflection on untyped object, FIXME: will not work currently for JS and wasmJS
                        val obj = tobj.self
                        val value = externalGetter.getProperty(obj, propertyName)
                        value?.let { toTypedObject(value) } ?: nothing()
                    }

                    else -> {
                        when {
                            null!=propRes.original.execution -> {
                                val value =  propRes.original.execution!!.invoke(tobj.self)
                                value?.let { toTypedObject(value) } ?: nothing()
                            }
                            null!=propRes.original.executionSuspend -> {
                                val value =  propRes.original.executionSuspend!!.invoke(tobj.self)
                                value?.let { toTypedObject(value) } ?: nothing()
                            }
                            else -> {
                                val type = tobj.type.resolvedDeclaration
                                val execResult = primitiveExecutor.propertyValue(untyped(tobj), type, propRes.original)
                                when (execResult) {
                                    null -> when (propRes.original) {
                                        is PropertyDeclarationDerived -> TODO()
                                        is PropertyDeclarationPrimitive -> {
                                            // should have found execution if there is one
                                            //issues.error(null, "using StdLibPrimitiveExecutionsForReflection not found for property '${propRes}'")
                                            val obj = tobj.self
                                            val value = externalGetter.getProperty(obj, propertyName)
                                            value?.let { toTypedObject(value) } ?: nothing()
                                        }

                                        is PropertyDeclarationStored -> {
                                            //try reflection
                                            val obj = tobj.self
                                            val value = externalGetter.getProperty(obj, propertyName)
                                            value?.let { toTypedObject(value) } ?: nothing()
                                        }

                                        else -> error("Subtype of PropertyDeclaration not handled: '${this::class.simpleName}'")
                                    }

                                    else -> toTypedObject(execResult.value)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun setProperty(tobj: TypedObject<Any>, propertyName: String, value: TypedObject<Any>) {
        when {
            StdLibDefault.TupleType == tobj.type.resolvedDeclaration -> {
                when (tobj.self) {
                    is MutableMap<*, *> -> {
                        (tobj.self as MutableMap<String, Any>)[propertyName] = untyped(value)
                    }
                }
            }

            else -> {
                try {
                    val obj = tobj.self
                    obj.reflect().setProperty(propertyName, untyped(value))
                } catch (t:Throwable){
                    issues.error(null, "Could not set property $propertyName to $value")
                }
            }
        }
    }

}
