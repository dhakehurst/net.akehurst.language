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
import net.akehurst.language.base.api.Indent
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.collections.ListSeparated
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.*
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.*
import kotlin.reflect.KProperty1

class StdLibPrimitiveExecutionsForReflectionSuspending<T : Any>(
    val issues: IssueHolder = IssueHolder(LanguageProcessorPhase.INTERPRET)
) : PrimitiveExecutor<T> {

    private val _property = mutableMapOf<TypeDefinition, MutableMap<PropertyDeclaration, ((Any, PropertyDeclaration) -> Any?)>>(
        StdLibDefault.List to mutableMapOf(
            StdLibDefault.List.findAllPropertyOrNull(PropertyName("size"))!! to { self, prop ->
                check(self is List<*>) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.size.toLong()
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

    private val _method = mutableMapOf<TypeDefinition, MutableMap<MethodDeclaration, ((Any, MethodDeclaration, List<TypedObject<Any>>) -> Any?)>>(
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
                check(self is List<*>) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' takes 1 lambda argument got ${args.size} arguments." }
                check(args[0].self is Function1<*, *>) { "Method '${meth.name}' first argument must be a lambda, got '${args[0].self::class.simpleName}'." }
                val lambda = args[0].self as Function1<Any, *>
                (self as List<Any>).map {
                    //val args = mapOf("it" to it)
                    lambda.invoke(it)
                }
            }
        ),

        StdLibDefault.List to mutableMapOf(
            StdLibDefault.List.findAllMethodOrNull(MethodName("map"))!! to { self, meth, args ->
                check(self is List<*>) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' takes 1 lambda argument got ${args.size} arguments." }
                check(args[0].self is Function1<*, *>) { "Method '${meth.name}' first argument must be a lambda, got '${args[0].self::class.simpleName}'." }
                val lambda = args[0].self as Function1<Any, *>
                (self as List<Any>).map {
                    //val args = mapOf("it" to it)
                    lambda.invoke(it)
                }
            },
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
        ),
    )

    override fun propertyValue(obj: T, typeDef: TypeDefinition, property: PropertyDeclaration): ExecutionResult? = propertyValueDirectOrSuperType(obj, typeDef, property)

    override fun methodCall(obj: T, typeDef: TypeDefinition, method: MethodDeclaration, args: List<TypedObject<T>>): ExecutionResult? {
        val methProps = this._method[typeDef] ?: error("StdLibPrimitiveExecutions not found for TypeDeclaration '${typeDef.qualifiedName.value}'")
        val methExec = methProps[method] ?: error("StdLibPrimitiveExecutionsForReflection not found for method '${method.name.value}' of TypeDeclaration '${typeDef.qualifiedName.value}'")
        val res = methExec.invoke(obj, method, args)
        return ExecutionResult(res)
    }

    override fun functionCall(functionName: String, args: List<TypedObject<T>>): ExecutionResult? {
        TODO("not implemented")
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
        val propExec = property.execution
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
}

open class ObjectGraphByReflectionSuspending<SelfType : Any>(
    typesDomain: TypesDomain,
    issues: IssueHolder,
    primitiveExecutor: PrimitiveExecutor<SelfType> = StdLibPrimitiveExecutionsForReflection<SelfType>()
) : ObjectGraphByReflectionAbstract<SelfType>(typesDomain, issues, primitiveExecutor), ObjectGraphAccessorMutatorSuspending<SelfType> {

    override fun createLambdaValue(lambda: suspend (it: TypedObject<SelfType>) -> TypedObject<SelfType>): TypedObject<SelfType> {
        val lambdaType = StdLibDefault.Lambda //TODO: typeargs like tuple
        val lmb: suspend (Any) -> Any = { it: Any -> untyped(lambda.invoke(toTypedObject(it as SelfType))) }
        return TypedObjectAny(lambdaType, lmb) as TypedObject<SelfType>
    }

    /**
     * when {
     *   type is TupleType -> property from tuple or $nothing
     *   type does not have resolved property -> try reflection
     *   type has resolved property -> try primitiveExecutor or reflection if executor fails
     * }
     */
    override suspend fun getProperty(tobj: TypedObject<SelfType>, propertyName: String): TypedObject<SelfType> {
        return when {
            StdLibDefault.TupleType == tobj.type.resolvedDeclaration -> {
                val obj = untyped(tobj)
                when (obj) {
                    is Map<*, *> -> {
                        val value = (obj as Map<String, Any>)[propertyName]
                        value?.let { toTypedObject(it as SelfType?) } ?: nothing()
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
                        val value = obj.reflect().getProperty(propertyName)
                        value?.let { toTypedObject(it as SelfType?) } ?: nothing()
                    }

                    else -> {
                        // first try execution
                        val type = tobj.type.resolvedDeclaration
                        val execResult = primitiveExecutor.propertyValue(untyped(tobj) as SelfType, type, propRes.original)
                        when (execResult) {
                            null -> when (propRes.original) {
                                is PropertyDeclarationDerived -> TODO()
                                is PropertyDeclarationPrimitive -> {
                                    // should have found execution if there is one
                                    issues.error(null, "using StdLibPrimitiveExecutionsForReflection not found for property '${propRes}'")
                                    nothing()
                                }

                                is PropertyDeclarationStored -> {
                                    //try reflection
                                    val obj = tobj.self
                                    val value = obj.reflect().getProperty(propertyName)
                                    value?.let { toTypedObject(it as SelfType?) } ?: nothing()
                                }

                                else -> error("Subtype of PropertyDeclaration not handled: '${this::class.simpleName}'")
                            }

                            else -> toTypedObject(execResult.value as SelfType?)
                        }
                    }
                }
            }
        }
    }

    override suspend fun setProperty(tobj: TypedObject<SelfType>, propertyName: String, value: TypedObject<SelfType>) {
        when {
            StdLibDefault.TupleType == tobj.type.resolvedDeclaration -> {
                when (tobj.self) {
                    is MutableMap<*, *> -> {
                        (tobj.self as MutableMap<String, Any>)[propertyName] = untyped(value)
                    }
                }
            }

            else -> {
                val obj = tobj.self
                obj.reflect().setProperty(propertyName, untyped(value))
            }
        }
    }

}
