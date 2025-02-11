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

package net.akehurst.language.expressions.processor

import net.akehurst.language.asm.api.*
import net.akehurst.language.asm.simple.*
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.*

object StdLibPrimitiveExecutions {
    val property = mapOf<TypeDefinition, Map<PropertyDeclaration, ((AsmValue, PropertyDeclaration) -> AsmValue)>>(
        StdLibDefault.List to mapOf(
            StdLibDefault.List.findAllPropertyOrNull(PropertyName("size"))!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmPrimitiveSimple.stdInteger(self.elements.size)
            },
            StdLibDefault.List.findAllPropertyOrNull(PropertyName("first"))!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.elements.first()
            },
            StdLibDefault.List.findAllPropertyOrNull(PropertyName("last"))!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.elements.last()
            },
            StdLibDefault.List.findAllPropertyOrNull(PropertyName("back"))!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmListSimple(self.elements.drop(1))
            },
            StdLibDefault.List.findAllPropertyOrNull(PropertyName("front"))!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmListSimple(self.elements.dropLast(1))
            },
            StdLibDefault.List.findAllPropertyOrNull(PropertyName("join"))!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmPrimitiveSimple.stdString(self.elements.joinToString(separator = "") { it.asString() })
            }
        ),
        StdLibDefault.ListSeparated to mapOf(
            StdLibDefault.ListSeparated.findAllPropertyOrNull(PropertyName("items"))!! to { self, prop ->
                check(self is AsmListSeparated) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmListSimple(self.elements.items)
            },
            StdLibDefault.ListSeparated.findAllPropertyOrNull(PropertyName("separators"))!! to { self, prop ->
                check(self is AsmListSeparated) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmListSimple(self.elements.separators)
            },
        )
    )

    val method = mapOf<TypeDefinition, Map<MethodDeclaration, ((AsmValue, MethodDeclaration, List<TypedObject<AsmValue>>) -> AsmValue)>>(
        StdLibDefault.List to mapOf(
            StdLibDefault.List.findAllMethodOrNull(MethodName("get"))!! to { self, meth, args ->
                check(self is AsmList) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' has wrong number of argument, expecting 1, received ${args.size}" }
                check(args[0].self is AsmPrimitive) { "Method '${meth.name}' takes an ${StdLibDefault.Integer.qualifiedTypeName} as its argument, received ${args[0].type.qualifiedTypeName}" }
                check(StdLibDefault.Integer.qualifiedTypeName == args[0].type.qualifiedTypeName) { "Method '${meth.name}' takes an ${StdLibDefault.Integer.qualifiedTypeName} as its argument, received ${args[0].type.qualifiedTypeName}" }
                val arg1 = args[0].self as AsmPrimitive
                val idx = arg1.value as Int
                self.elements.get(idx)
            },
            StdLibDefault.Collection.findAllMethodOrNull(MethodName("map"))!! to { self, meth, args ->
                check(self is AsmList) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' takes 1 lambda argument got ${args.size} arguments." }
                check(args[0].self is AsmLambda) { "Method '${meth.name}' first argument must be a lambda, got '${args[0].self::class.simpleName}'." }
                val lambda = args[0].self as AsmLambda
                val mapped = self.elements.map {
                    val args = mapOf("it" to it)
                    lambda.invoke(args)
                }
                AsmListSimple(mapped)
            }
        )
    )
}

class TypedObjectAsmValue(
    override val type: TypeInstance,
    override val self: AsmValue
) : TypedObject<AsmValue> {

    override fun asString(): String = self.asString()

    override fun hashCode(): Int = self.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypedObject<*> -> false
        else -> self == other.self
    }

    override fun toString(): String = "$self : ${type.qualifiedTypeName}"
}


class ObjectGraphAsmSimple(
    override val typeModel: TypeModel,
    val issues: IssueHolder
) : ObjectGraph<AsmValue> {

    fun AsmValue.toTypedObject() = TypedObjectAsmValue(typeFor(this), this)

    override fun typeFor(obj: AsmValue?): TypeInstance {
        return obj?.let {
            typeModel.findByQualifiedNameOrNull(it.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
        } ?: StdLibDefault.NothingType
    }

    override fun toTypedObject(obj: AsmValue?): TypedObject<AsmValue> = obj?.toTypedObject() ?: nothing()

    override fun nothing() = AsmNothingSimple.toTypedObject()

    override fun createPrimitiveValue(qualifiedTypeName: QualifiedName, value: Any) = when (qualifiedTypeName) {
        StdLibDefault.Boolean.qualifiedTypeName -> AsmPrimitiveSimple(qualifiedTypeName, value).toTypedObject()
        StdLibDefault.Integer.qualifiedTypeName -> AsmPrimitiveSimple(qualifiedTypeName, value).toTypedObject()
        StdLibDefault.Real.qualifiedTypeName -> AsmPrimitiveSimple(qualifiedTypeName, value).toTypedObject()
        StdLibDefault.String.qualifiedTypeName -> AsmPrimitiveSimple(qualifiedTypeName, value).toTypedObject()
        else -> error("should not happen")
    }

    override fun createTupleValue(typeArgs: List<TypeArgumentNamed>): TypedObject<AsmValue> {
        val tupleType = StdLibDefault.TupleType
        val tuple = AsmStructureSimple(AsmPathSimple(""), tupleType.qualifiedName)
        return TypedObjectAsmValue(tupleType.type(typeArgs), tuple)
    }

    override fun createStructureValue(possiblyQualifiedTypeName: PossiblyQualifiedName, constructorArgs: Map<String, TypedObject<AsmValue>>): TypedObject<AsmValue> {
        val typeDecl = typeModel.findFirstDefinitionByPossiblyQualifiedNameOrNull(possiblyQualifiedTypeName)
            ?: error("Type not found ${possiblyQualifiedTypeName}")
        val asmPath = AsmPathSimple("??") //TODO:
        val obj = AsmStructureSimple(asmPath, typeDecl.qualifiedName)
        constructorArgs.forEach { (k, v) -> obj.setProperty(PropertyValueName(k), v.self, obj.property.size) }
        return TypedObjectAsmValue(typeDecl.type(), obj)
    }

    override fun createLambdaValue(lambda: (it: TypedObject<AsmValue>) -> TypedObject<AsmValue>): TypedObject<AsmValue> {
        val lambdaType = StdLibDefault.Lambda //TODO: typeargs like tuple
        val lmb = AsmLambdaSimple { lambda.invoke(it.toTypedObject()).self }
        return TypedObjectAsmValue(lambdaType, lmb)
    }

    override fun valueOf(value: TypedObject<AsmValue>): Any = value.self.raw

    override fun getIndex(tobj: TypedObject<AsmValue>, index: Any): TypedObject<AsmValue> {
        val asmValue = tobj.self
        return when (asmValue) {
            is AsmList -> when (index) {
                is Int -> {
                    val el = asmValue.elements.getOrNull(index)
                    when (el) {
                        null -> {
                            issues.error(null, "in getIndex argument index '$index' out of range")
                            nothing()
                        }

                        else -> el.toTypedObject()
                    }
                }

                else -> {
                    issues.error(null, "in getIndex argument 'index' must be an Int for Lists")
                    nothing()
                }
            }

            else -> {
                issues.error(null, "getIndex not supported on type '${tobj.type.typeName}'")
                nothing()
            }
        }
    }

    override fun getProperty(tobj: TypedObject<AsmValue>, propertyName: String): TypedObject<AsmValue> {
        val asmValue = tobj.self
        val propRes = tobj.type.allResolvedProperty[PropertyName(propertyName)]
        return when (propRes) {
            null -> when {
                asmValue is AsmStructure -> {
                    val pv = asmValue.property[PropertyValueName(propertyName)]
                    pv?.let { pv.value.toTypedObject() } ?: nothing()
                }

                else -> {
                    issues.error(null, "getProperty property '$propertyName' not found on object of type '${tobj.type.typeName}'")
                    nothing()
                }
            }

            else -> when (propRes.original) {
                is PropertyDeclarationDerived -> TODO()
                is PropertyDeclarationPrimitive -> {
                    val type = tobj.type.resolvedDeclaration
                    val typeProps = StdLibPrimitiveExecutions.property[type]
                        ?: error("StdLibPrimitiveExecutions not found for TypeDeclaration '${type.qualifiedName}'")
                    val propExec = typeProps[propRes.original]
                        ?: error("StdLibPrimitiveExecutions not found for property '${propertyName}' of TypeDeclaration '${type.qualifiedName}'")
                    propExec.invoke(asmValue, propRes).toTypedObject()
                }

                is PropertyDeclarationStored -> when (asmValue) {
                    is AsmStructure -> {
                        val pv = asmValue.property[PropertyValueName(propertyName)]
                        pv?.let { pv.value.toTypedObject() } ?: nothing()
                    }

                    else -> error("Cannot evaluate property '${propertyName}' on object of type '${tobj::class.simpleName}'")
                }

                else -> error("Subtype of PropertyDeclaration not handled: '${this::class.simpleName}'")
            }
        }
    }

    override fun setProperty(tobj: TypedObject<AsmValue>, propertyName: String, value: TypedObject<AsmValue>) {
        val obj = tobj.self as AsmStructure
        obj.setProperty(PropertyValueName(propertyName), value.self, obj.property.size)
    }

    override fun executeMethod(tobj: TypedObject<AsmValue>, methodName: String, args: List<TypedObject<AsmValue>>): TypedObject<AsmValue> {
        val methRes = tobj.type.allResolvedMethod[MethodName(methodName)]!!
        val type = tobj.type.resolvedDeclaration
        val stdMeths = StdLibPrimitiveExecutions.method[type]
        val ao = when (stdMeths) {
            null -> TODO()
            else -> {
                val methExec = stdMeths[methRes.original]
                    ?: error("StdLibPrimitiveExecutions not found for method '${methRes.name.value}' of TypeDeclaration '${type.qualifiedName}'")
                val self = tobj.self
                // val arguments = args.map { it.asmValue }
                methExec.invoke(self, methRes, args)
            }
        }
        return TypedObjectAsmValue(methRes.returnType, ao)
    }

    override fun cast(tobj: TypedObject<AsmValue>, newType: TypeInstance): TypedObject<AsmValue> {
        val rtd = newType.resolvedDeclaration
        return when (rtd) {
            is TupleType -> {
                val targs = (tobj.self as AsmStructure).property.map {
                    val n = PropertyName(it.key.value)
                    val t = StdLibDefault.AnyType //TODO: can do better!
                    TypeArgumentNamedSimple(n, t)
                }
                val tp = rtd.typeTuple(targs)
                TypedObjectAsmValue(tp, tobj.self)
            }

            else -> tobj.self.toTypedObject()
        }
    }
}