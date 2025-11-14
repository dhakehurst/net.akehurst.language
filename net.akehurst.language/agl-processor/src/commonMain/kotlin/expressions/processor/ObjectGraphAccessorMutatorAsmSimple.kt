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
import net.akehurst.language.base.api.Indent
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.*
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.*

object StdLibPrimitiveExecutionsForAsmSimple : PrimitiveExecutor<AsmValue> {
    val property = mapOf<TypeDefinition, Map<PropertyDeclaration, ((AsmValue, PropertyDeclaration) -> AsmValue)>>(
        StdLibDefault.List to mapOf(
            StdLibDefault.List.findAllPropertyOrNull(PropertyName("size"))!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmPrimitiveSimple.stdInteger(self.elements.size.toLong())
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
                AsmPrimitiveSimple.stdString(self.elements.joinToString(separator = "") {
                    when (it) {
                        is AsmNothing -> ""
                        is AsmAny -> it.value.toString()
                        is AsmPrimitive -> it.value.toString()
                        is AsmStructure -> it.asString()
                        is AsmList -> it.asString()
                        is AsmLambda -> it.asString()
                        else -> error("Unsupported ${it::class.simpleName}")
                    }
                })
            },
            StdLibDefault.List.findAllPropertyOrNull(PropertyName("asMap"))!! to { self, prop ->
                check(self is AsmList) { "Method '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                val map = self.elements.associate {
                    check(it.raw is Pair<*, *>) { }
                    it.raw as Pair<*, *>
                }
                TODO("No AsmMap object ? maybe use tuple !")
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
                val idx = arg1.value as Long
                self.elements.get(idx.toInt())
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
        ),
    )

    override fun propertyValue(obj: AsmValue, typeDef: TypeDefinition, property: PropertyDeclaration): ExecutionResult? {
        val typeProps = this.property[typeDef] ?: error("StdLibPrimitiveExecutionsForAsmSimple not found for TypeDeclaration '${typeDef.qualifiedName.value}'")
        val propExec = typeProps[property] ?: error("StdLibPrimitiveExecutionsForAsmSimple not found for property '${property.name.value}' of TypeDeclaration '${typeDef.qualifiedName.value}'")
        return ExecutionResult(propExec.invoke(obj, property))
    }

    override fun methodCall(obj: AsmValue, typeDef: TypeDefinition, method: MethodDeclaration, args: List<TypedObject<AsmValue>>): ExecutionResult? {
        val methProps = this.method[typeDef] ?: error("StdLibPrimitiveExecutionsForAsmSimple not found for TypeDeclaration '${typeDef.qualifiedName.value}'")
        val methExec = methProps[method] ?: error("StdLibPrimitiveExecutionsForAsmSimple not found for method '${method.name.value}' of TypeDeclaration '${typeDef.qualifiedName.value}'")
        return ExecutionResult(methExec.invoke(obj, method, args) as AsmValue)
    }

    override fun functionCall(functionName: String, args: List<TypedObject<AsmValue>>): ExecutionResult? {
        return when (functionName) {
            "List" -> {
                val elts = args.map { it.self }
                 ExecutionResult(AsmListSimple(elts))
            }
            "Set" -> {
                val elts = args.map { it.self }.toSet().toList()
                ExecutionResult(AsmListSimple(elts))
            }
            else -> error("Unknown function '$functionName'")
        }
    }
}

class TypedObjectAsmValue(
    override val type: TypeInstance,
    override val self: AsmValue
) : TypedObject<AsmValue> {

    override fun asString(indent: Indent): String = self.asString(indent)

    override fun hashCode(): Int = self.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypedObject<*> -> false
        else -> self == other.self
    }

    override fun toString(): String = "$self : ${type.qualifiedTypeName}"
}

open class ObjectGraphAccessorMutatorAsmSimple(
    override var typesDomain: TypesDomain,
    val issues: IssueHolder,
    override val primitiveExecutor: PrimitiveExecutor<AsmValue> = StdLibPrimitiveExecutionsForAsmSimple
) : ObjectGraphAccessorMutator<AsmValue> {

    override val createdStructuresByType = mutableMapOf<TypeInstance, List<AsmValue>>()

    fun AsmValue.toTypedObject() = TypedObjectAsmValue(typeFor(this), this)

    override fun typeFor(obj: AsmValue?): TypeInstance {
        return obj?.let { o->
            typesDomain.findByQualifiedNameOrNull(o.qualifiedTypeName)?.type() ?: let {
                issues.error(null, "Cannot find type definition '${o.qualifiedTypeName.value}'")
                StdLibDefault.AnyType
            }
        } ?: StdLibDefault.NothingType
    }

    override fun toTypedObject(obj: AsmValue?): TypedObject<AsmValue> = obj?.toTypedObject() ?: nothing()

    override fun isNothing(obj: TypedObject<AsmValue>): Boolean = obj.self == AsmNothingSimple
    override fun equalTo(lhs: TypedObject<AsmValue>, rhs: TypedObject<AsmValue>): Boolean =
        lhs.self.equalTo(rhs.self)

    override fun nothing() = AsmNothingSimple.toTypedObject()
    override fun any(value: Any) = AsmAnySimple(value).toTypedObject()

    override fun createPrimitiveValue(qualifiedTypeName: QualifiedName, value: Any) = when (qualifiedTypeName) {
        StdLibDefault.Boolean.qualifiedTypeName -> AsmPrimitiveSimple.stdBoolean(value as Boolean).toTypedObject()
        StdLibDefault.Integer.qualifiedTypeName -> AsmPrimitiveSimple.stdInteger(value as Long).toTypedObject()
        StdLibDefault.Real.qualifiedTypeName -> AsmPrimitiveSimple.stdReal(value as Double).toTypedObject()
        StdLibDefault.String.qualifiedTypeName -> AsmPrimitiveSimple.stdString(value as String).toTypedObject()
        else -> error("should not happen")
    }

    override fun createTupleValue(typeArgs: List<TypeArgumentNamed>): TypedObject<AsmValue> {
        val tupleType = StdLibDefault.TupleType
        val tuple = AsmStructureSimple(tupleType.qualifiedName)
        return TypedObjectAsmValue(tupleType.type(typeArgs), tuple)
    }

    override fun createStructureValue(possiblyQualifiedTypeName: PossiblyQualifiedName, constructorArgs: Map<String, TypedObject<AsmValue>>): TypedObject<AsmValue> {
        val typeDecl = typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(possiblyQualifiedTypeName)
            ?: error("Type not found ${possiblyQualifiedTypeName}")
        //val asmPath = AsmPathSimple("??") //TODO:
        val obj = AsmStructureSimple(typeDecl.qualifiedName)
        constructorArgs.forEach { (k, v) -> obj.setProperty(PropertyValueName(k), v.self, obj.property.size) }
        val type = typeDecl.type()
        addCreatedStructure(type, obj)
        return TypedObjectAsmValue(type, obj)
    }

    override fun createCollection(qualifiedTypeName: QualifiedName, collection: Iterable<TypedObject<AsmValue>>): TypedObject<AsmValue> {
        return when (qualifiedTypeName) {
            StdLibDefault.List.qualifiedName -> {
                val elType = collection.firstOrNull()?.type ?: StdLibDefault.AnyType
                TypedObjectAsmValue(StdLibDefault.List.type(listOf(elType.asTypeArgument)), AsmListSimple(collection.map { it.self }))
            }

            StdLibDefault.ListSeparated.qualifiedName -> {
                val list = collection.toList()
                val elType = list.getOrNull(0)?.type ?: StdLibDefault.AnyType
                val sepType = list.getOrNull(1)?.type ?: StdLibDefault.AnyType
                TypedObjectAsmValue(StdLibDefault.ListSeparated.type(listOf(elType.asTypeArgument, sepType.asTypeArgument)), AsmListSeparatedSimple(collection.map { it.self }.toSeparatedList()))
            }

            else -> nothing()
        }
    }

    override fun createLambdaValue(lambda: (it: TypedObject<AsmValue>) -> TypedObject<AsmValue>): TypedObject<AsmValue> {
        val lambdaType = StdLibDefault.Lambda //TODO: typeargs like tuple
        val lmb = AsmLambdaSimple { lambda.invoke(it.toTypedObject()).self }
        return TypedObjectAsmValue(lambdaType, lmb)
    }

    override fun valueOf(value: TypedObject<AsmValue>): Any = value.self.raw

    override fun getIndex(tobj: TypedObject<AsmValue>, index: Int): TypedObject<AsmValue> {
        val asmValue = tobj.self
        return when (asmValue) {
            is AsmList -> {
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
                issues.error(null, "getIndex not supported on type '${tobj.type.typeName}'")
                nothing()
            }
        }
    }

    override fun forEachIndexed(tobj: TypedObject<AsmValue>, body: (index: Int, value: TypedObject<AsmValue>) -> Unit) {
        val asmValue = tobj.self
        when (asmValue) {
            is AsmList -> {
                asmValue.elements.forEachIndexed { index, el -> body(index, toTypedObject(el)) }
            }

            else -> {
                issues.error(null, "forEachIndexed not supported on type '${tobj.type.typeName}'")
                nothing()
            }
        }
    }

    override fun getProperty(tobj: TypedObject<AsmValue>, propertyName: String): TypedObject<AsmValue> {
        //TODO: use executor
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
                    val typeProps = StdLibPrimitiveExecutionsForAsmSimple.property[type]
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
        //TODO: use executor
        val obj = tobj.self as AsmStructure
        obj.setProperty(PropertyValueName(propertyName), value.self, obj.property.size)
    }

    override fun executeMethod(tobj: TypedObject<AsmValue>, methodName: String, args: List<TypedObject<AsmValue>>): TypedObject<AsmValue> {
        //TODO: use executor
        val methRes = tobj.type.allResolvedMethod[MethodName(methodName)]!!
        val type = tobj.type.resolvedDeclaration
        val stdMeths = StdLibPrimitiveExecutionsForAsmSimple.method[type]
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

    override fun callFunction(functionName: String, args: List<TypedObject<AsmValue>>): TypedObject<AsmValue> {
       return primitiveExecutor.functionCall(functionName, args) ?.let {
           toTypedObject(it.value as AsmValue)
       } ?: nothing()
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

    private fun addCreatedStructure(type: TypeInstance, obj: AsmValue) {
        var list = this.createdStructuresByType[type]
        if (null == list) {
            list = mutableListOf(obj)
            this.createdStructuresByType[type] = list
        } else {
            (list as MutableList).add(obj)
        }
    }

    override fun getCompositeGraphFrom(resultGraphIdentity:String, roots: List<TypedObject<AsmValue>>): ObjectGraph<AsmValue> {
        val nodes = mutableSetOf<TypedObject<AsmValue>>()
        val edges = mutableSetOf<ObjectGraphEdge<AsmValue>>()

        AsmSimple.traverseDepthFirst(roots.map { it.self }, object : AsmTreeWalker {
            override fun beforeRoot(root: AsmValue) {}
            override fun afterRoot(root: AsmValue) {}

            override fun onNothing(owningProperty: AsmStructureProperty?, value: AsmNothing) {}

            override fun onPrimitive(owningProperty: AsmStructureProperty?, value: AsmPrimitive) {}

            override fun beforeStructure(owningProperty: AsmStructureProperty?, value: AsmStructure) {}

            override fun onProperty(owner: AsmStructure, property: AsmStructureProperty) {
                val src = toTypedObject(owner)
                val tgt = toTypedObject(property.value)
                val ownerTypeDef = src.type.resolvedDeclaration
                val propDef = ownerTypeDef.findAllPropertyOrNull(PropertyName(property.name.value))
                if (null == propDef) {
                    issues.error(null, "Cannot find property '${property.name.value}' on type definition '${ownerTypeDef.qualifiedName.value}'")
                } else {
                    val edge = ObjectGraphEdgeSimple(src, tgt, propDef)
                    edges.add(edge)
                }
            }

            override fun afterStructure(owningProperty: AsmStructureProperty?, value: AsmStructure) {
                val node = toTypedObject(value)
                nodes.add(node)
            }

            override fun beforeList(owningProperty: AsmStructureProperty?, value: AsmList) {}

            override fun afterList(owningProperty: AsmStructureProperty?, value: AsmList) {}

        })
        return ObjectGraphAsmSimple(resultGraphIdentity, nodes, edges)
    }
}

class ObjectGraphAsmSimple(
    val identity:String,
    override val nodes: Set<TypedObject<AsmValue>>,
    override val edges: Set<ObjectGraphEdge<AsmValue>>
) : ObjectGraph<AsmValue> {

    override fun hashCode(): Int = identity.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is ObjectGraphAsmSimple -> false
        else -> identity == other.identity
    }
    override fun toString(): String = "ObjectGraphAsmSimple(identity='$identity')"
}

data class ObjectGraphEdgeSimple(
    override val source: TypedObject<AsmValue>,
    override val target: TypedObject<AsmValue>,
    override val property: PropertyDeclaration
) : ObjectGraphEdge<AsmValue>