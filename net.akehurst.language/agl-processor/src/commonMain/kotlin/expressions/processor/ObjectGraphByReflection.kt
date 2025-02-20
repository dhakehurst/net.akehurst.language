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
import net.akehurst.language.expressions.processor.ObjectGraph
import net.akehurst.language.expressions.processor.TypedObject
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.typemodel.api.PropertyName
import net.akehurst.language.typemodel.api.TypeArgumentNamed
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.StdLibDefault
import net.akehurst.language.typemodel.asm.TypeArgumentNamedSimple

class TypedObjectByReflection(
    override val type: TypeInstance,
    override val self: Any
) : TypedObject<Any> {

    override fun asString(): String = self.toString()

    override fun hashCode(): Int = self.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypedObject<*> -> false
        else -> self == other.self
    }

    override fun toString(): String = "$self : ${type.qualifiedTypeName}"
}


class ObjectGraphByReflection(
    override var typeModel: TypeModel,
    val issues: IssueHolder
) : ObjectGraph<Any> {

    //fun Any.toTypedObject(type: TypeInstance) = TypedObjectByReflection(type, this)

    override fun typeFor(obj: Any?): TypeInstance {
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
                            val ttargs = obj.map { (k, v) -> TypeArgumentNamedSimple(PropertyName(k as String), typeFor(v)) }
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

    override fun toTypedObject(obj: Any?): TypedObject<Any> = when {
        obj is TypedObject<*> -> obj as TypedObject<Any>
        else ->  obj?.let { TypedObjectByReflection(typeFor(obj), obj) } ?: nothing()
    }

    override fun nothing() = TypedObjectByReflection(StdLibDefault.NothingType,Unit)
    override fun any(value: Any)= TypedObjectByReflection(StdLibDefault.AnyType,value)

    override fun createPrimitiveValue(qualifiedTypeName: QualifiedName, value: Any) = toTypedObject(value)

    override fun createTupleValue(typeArgs: List<TypeArgumentNamed>): TypedObject<Any> {
        val tupleType = StdLibDefault.TupleType
        val tuple = mutableMapOf<String, Any>()
        return TypedObjectByReflection(tupleType.type(typeArgs), tuple)
    }

    override fun createStructureValue(possiblyQualifiedTypeName: PossiblyQualifiedName, constructorArgs: Map<String, TypedObject<Any>>): TypedObject<Any> {
        TODO("not implemented")
    }

    override fun createCollection(qualifiedTypeName: QualifiedName, collection: Iterable<TypedObject<Any>>): TypedObject<Any> {
        TODO("not implemented")
    }

    override fun createLambdaValue(lambda: (it: TypedObject<Any>) -> TypedObject<Any>): TypedObject<Any> {
        val lambdaType = StdLibDefault.Lambda //TODO: typeargs like tuple
        val lmb = { it: Any -> lambda.invoke(toTypedObject(it)).self }
        return TypedObjectByReflection(lambdaType, lmb)
    }

    override fun valueOf(value: TypedObject<Any>): Any = value.self

    override fun getIndex(tobj: TypedObject<Any>, index: Any): TypedObject<Any> {
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

                        else -> toTypedObject(el)

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

    override fun getProperty(tobj: TypedObject<Any>, propertyName: String): TypedObject<Any> {
        return when {
            StdLibDefault.TupleType == tobj.type.resolvedDeclaration -> {
                when (tobj.self) {
                    is Map<*, *> -> {
                        val value = (tobj.self as Map<String, Any>)[propertyName]
                        value?.let { toTypedObject(it) } ?: nothing()
                    }

                    else -> nothing()
                }
            }

            else -> {
                val obj = tobj.self
                val value = obj.reflect().getProperty(propertyName)
                value?.let { toTypedObject(it) } ?: nothing()
            }
        }
    }

    override fun setProperty(tobj: TypedObject<Any>, propertyName: String, value: TypedObject<Any>) {
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

    override fun executeMethod(tobj: TypedObject<Any>, methodName: String, args: List<TypedObject<Any>>): TypedObject<Any> {
        val obj = tobj.self
        val arguments = args.map { it.self }
        val value = obj.reflect().call(methodName, arguments)
        return value?.let { toTypedObject(it) } ?: nothing()
    }

    override fun cast(tobj: TypedObject<Any>, newType: TypeInstance): TypedObject<Any> {
        return TypedObjectByReflection(newType, tobj.self)
    }

}