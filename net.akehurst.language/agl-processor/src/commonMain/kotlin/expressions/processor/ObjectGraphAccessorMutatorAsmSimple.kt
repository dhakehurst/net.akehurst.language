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

import net.akehurst.language.agl.simple.SemanticAnalyserSimple
import net.akehurst.language.asm.api.*
import net.akehurst.language.asm.simple.*
import net.akehurst.language.base.api.Indent
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.collections.toSeparatedList
import net.akehurst.language.collections.transitiveClosure
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.*
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.*

object StdLibPrimitiveExecutionsForAsmSimple : PrimitiveExecutor {
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
                        is AsmSet -> it.asString()
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

    val method = mapOf<TypeDefinition, Map<MethodDeclaration, ((AsmValue, MethodDeclaration, List<*>) -> AsmValue)>>(
        StdLibDefault.List to mapOf(
            StdLibDefault.List.findAllMethodOrNull(MethodName("get"))!! to { self, meth, args ->
                check(self is AsmList) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' has wrong number of argument, expecting 1, received ${args.size}" }
                check(args[0] is AsmPrimitive) { "Method '${meth.name}' takes an ${StdLibDefault.Integer.qualifiedTypeName} as its argument, received ${args[0]?.let {it::class.simpleName}}" }
                //check(StdLibDefault.Integer.qualifiedTypeName == args[0].qualifiedTypeName) { "Method '${meth.name}' takes an ${StdLibDefault.Integer.qualifiedTypeName} as its argument, received ${args[0].type.qualifiedTypeName}" }
                val arg1 = args[0] as AsmPrimitive
                val idx = arg1.value as Long
                self.elements.get(idx.toInt())
            },
            StdLibDefault.List.findAllMethodOrNull(MethodName("map"))!! to { self, meth, args ->
                check(self is AsmList) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' takes 1 lambda argument got ${args.size} arguments." }
                check(args[0] is AsmLambda) { "Method '${meth.name}' first argument must be a lambda, got '${args[0]?.let {it::class.simpleName}}'." }
                val lambda = args[0] as AsmLambda
                val mapped = self.elements.map {
                    val args = mapOf("it" to it)
                    lambda.invoke(args)
                }
                AsmListSimple(mapped)
            },
            StdLibDefault.List.findAllMethodOrNull(MethodName("filter"))!! to { self, meth, args ->
                check(self is AsmList) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' takes 1 lambda argument got ${args.size} arguments." }
                check(args[0] is AsmLambda) { "Method '${meth.name}' first argument must be a lambda, got '${args[0]?.let {it::class.simpleName}}'." }
                val lambda = args[0] as AsmLambda
                val mapped = self.elements.filter {
                    val args = mapOf("it" to it)
                    val v = lambda.invoke(args)
                    (v as AsmPrimitive).value as Boolean
                }
                AsmListSimple(mapped)
            },
            StdLibDefault.List.findAllMethodOrNull(MethodName("transitiveClosure"))!! to { self, meth, args ->
                check(self is AsmList) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' takes 1 lambda argument got ${args.size} arguments." }
                check(args[0] is AsmLambda) { "Method '${meth.name}' first argument must be a lambda, got '${args[0]?.let {it::class.simpleName}}'." }
                val lambda = args[0] as AsmLambda
                val mapped = self.elements.transitiveClosure {
                    val args = mapOf("it" to it)
                    val v = lambda.invoke(args)
                    (v as AsmList).elements
                }
                AsmListSimple(mapped)
            }
        ),
    )

    override fun propertyValue(obj: Any, typeDef: TypeDefinition, property: PropertyDeclaration): ExecutionResult? {
        val typeProps = this.property[typeDef] ?: error("StdLibPrimitiveExecutionsForAsmSimple not found for TypeDeclaration '${typeDef.qualifiedName.value}'")
        val propExec = typeProps[property] ?: error("StdLibPrimitiveExecutionsForAsmSimple not found for property '${property.name.value}' of TypeDeclaration '${typeDef.qualifiedName.value}'")
        return ExecutionResult(propExec.invoke(obj as AsmValue, property))
    }

    override fun methodCall(obj: Any, typeDef: TypeDefinition, method: MethodDeclaration, args: List<*>): ExecutionResult? {
        val methProps = this.method[typeDef] ?: error("StdLibPrimitiveExecutionsForAsmSimple not found for TypeDeclaration '${typeDef.qualifiedName.value}'")
        val methExec = methProps[method] ?: error("StdLibPrimitiveExecutionsForAsmSimple not found for method '${method.name.value}' of TypeDeclaration '${typeDef.qualifiedName.value}'")
        return ExecutionResult(methExec.invoke(obj as AsmValue, method, args) as AsmValue)
    }

    override fun functionCall(functionName: String, args: List<*>): ExecutionResult? {
        return when (functionName) {
            "List" -> {
                val elts = args
                ExecutionResult(AsmListSimple(elts as List<AsmValue>))
            }

            "Set" -> {
                val elts = args.toSet()
                ExecutionResult(AsmSetSimple(elts as Set<AsmValue>))
            }

            else -> error("Unknown function '$functionName'")
        }
    }

    override suspend fun methodCallSuspend(obj: Any, typeDef: TypeDefinition, method: MethodDeclaration, args: List<*>): ExecutionResult? =
        methodCall(obj, typeDef, method, args)
}

class ExternalGetterAsmSimple(
    val typesDomain: TypesDomain,
    val crossReferenceDomain: CrossReferenceDomain?,
    val issues: IssueHolder,
) : ExternalGetter {

    private val _interpreter = ExpressionsInterpreterOverTypedObject(ObjectGraphAccessorMutatorAsmSimple(typesDomain, issues, this), issues)

    override fun typeFor(obj: Any): TypeInstance {
        TODO("not implemented")
    }

    override fun createStructure(qualifiedName: QualifiedName, constructorArgs: Map<String, Any>): AsmValue {
        val obj = AsmStructureSimple(qualifiedName)
        constructorArgs.forEach { (k, v) ->
            obj.setProperty(PropertyValueName(k), v as AsmValue, obj.property.size)
        }

        crossReferenceDomain?.let {
            val id = SemanticAnalyserSimple.identifyingValueInFor(_interpreter, crossReferenceDomain, CrossReferenceDomainDefault.ROOT_SCOPE_TYPE_NAME.last, obj)
            when (id) {
                is String -> obj.setSemanticQualifiedPath(listOf(id)) //TODO: list of string returned from identifyingValueInFor
            }
        }

        return obj
    }

    override fun getProperty(obj: Any, propertyName: String): Any? {
        TODO("not implemented")
    }

    override fun setProperty(obj: Any, propertyName: String, value: Any?) {
        TODO("not implemented")
    }

    override fun createStructureSuspend(qualifiedName: QualifiedName, constructorArgs: Map<String, Any>): Any? = createStructure(qualifiedName, constructorArgs)

    override suspend fun getPropertySuspend(obj: Any, propertyName: String): Any? = getProperty(obj, propertyName)
}

private class TypedObjectAsmValue(
    override val accessor: ObjectGraphAccessorMutator,
    override val type: TypeInstance,
    override val self: AsmValue
) : TypedObject {

    override fun getProperty(name:String) = accessor.getProperty(this, name)
    override  suspend fun getPropertySuspend(name:String) = accessor.getPropertySuspend(this, name)

    override fun setProperty(name: String, value: TypedObject) = accessor .setProperty(this, name, value)
    override suspend fun setPropertySuspend(name: String, value: TypedObject) = accessor.setProperty(this, name, value)

    override  fun executeMethod(name: String, argValues: List<TypedObject>) = accessor.executeMethod(this, name, argValues)
    override suspend fun executeMethodSuspend(name: String, argValues: List<TypedObject>) = accessor.executeMethodSuspend(this, name, argValues)

    override fun asString(indent: Indent): String = self.asString(indent)

    override fun hashCode(): Int = self.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is TypedObjectAsmValue -> false
        else -> self == other.self
    }

    override fun toString(): String = "$self : ${type.qualifiedTypeName}"
}

open class ObjectGraphAccessorMutatorAsmSimple(
    override var typesDomain: TypesDomain,
    val issues: IssueHolder,
    override val externalGetter: ExternalGetter = ExternalGetterAsmSimple(typesDomain, null, issues),
    override val primitiveExecutor: PrimitiveExecutor = StdLibPrimitiveExecutionsForAsmSimple
) : ObjectGraphAccessorMutator {

    override val createdStructuresByType = mutableMapOf<TypeInstance, List<AsmValue>>()

    private fun AsmValue.asmToTypedObject() = typedAs(this, typeFor(this))

    override fun typeFor(obj: Any?): TypeInstance {
        return (obj as? AsmValue)?.let { o ->
            typesDomain.findByQualifiedNameOrNull(o.qualifiedTypeName)?.type() ?: let {
                issues.error(null, "Cannot find type definition '${o.qualifiedTypeName.value}'")
                StdLibDefault.AnyType
            }
        } ?: StdLibDefault.NothingType
    }

    override fun toTypedObject(obj: Any?): TypedObject = (obj as? AsmValue )?.asmToTypedObject() ?: nothing()
    override fun untyped(typedObj: TypedObject): Any {
        val self = typedObj.self
        return when (self) {
            is AsmPrimitive, is AsmAny, is AsmNothing, is AsmStructure -> self
            is AsmSet -> self.elements.map { el -> untyped(el.asmToTypedObject()) }.toSet()
            is AsmList -> self.elements.map { el -> untyped(el.asmToTypedObject()) }
            is AsmListSeparated -> self.elements.map { el -> untyped(el.asmToTypedObject()) }
            is AsmLambda -> self
            else -> error("Unsupported ${typedObj.self::class.simpleName}")
        }
    }

    override fun typedAs(obj: Any, type: TypeInstance): TypedObject = TypedObjectAsmValue(this,type, obj as AsmValue)

    override fun isNothing(obj: TypedObject): Boolean = obj.self == AsmNothingSimple
    override fun equalTo(lhs: TypedObject, rhs: TypedObject): Boolean {
        val lhsResolved = when (lhs.self) {
            is AsmReference -> (lhs.self as AsmReference).value ?: lhs.self
            else -> lhs.self
        }
        val rhsResolved = when (rhs.self) {
            is AsmReference -> (rhs.self as AsmReference).value ?: rhs.self
            else -> rhs.self
        }
        return (lhsResolved as AsmValue).equalTo(rhsResolved as AsmValue)
    }

    override fun nothing() = AsmNothingSimple.asmToTypedObject()
    override fun any(value: Any) = AsmAnySimple(value).asmToTypedObject()

    override fun createPrimitiveValue(qualifiedTypeName: QualifiedName, value: Any) = when (qualifiedTypeName) {
        StdLibDefault.Boolean.qualifiedTypeName -> AsmPrimitiveSimple.stdBoolean(value as Boolean).asmToTypedObject()
        StdLibDefault.Integer.qualifiedTypeName -> AsmPrimitiveSimple.stdInteger(value as Long).asmToTypedObject()
        StdLibDefault.Real.qualifiedTypeName -> AsmPrimitiveSimple.stdReal(value as Double).asmToTypedObject()
        StdLibDefault.String.qualifiedTypeName -> AsmPrimitiveSimple.stdString(value as String).asmToTypedObject()
        else -> error("should not happen")
    }

    override fun createTupleValue(typeArgs: List<TypeArgumentNamed>): TypedObject {
        val tupleType = StdLibDefault.TupleType
        val tuple = AsmStructureSimple(tupleType.qualifiedName)
        return typedAs(tuple, tupleType.type(typeArgs))
    }

    override fun createStructureValue(possiblyQualifiedTypeName: PossiblyQualifiedName, constructorArgs: Map<String, TypedObject>): TypedObject {
        val typeDecl = typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(possiblyQualifiedTypeName)
            ?: error("Type not found ${possiblyQualifiedTypeName}")
        val type = typeDecl.type()
        //val asmPath = AsmPathSimple("??") //TODO:
        val cargs = constructorArgs.map { (k, v) -> Pair(k, convertValue(type, k, v)) }.toMap()
        val obj = externalGetter.createStructure(possiblyQualifiedTypeName.asQualifiedName(null), cargs) ?: AsmNothingSimple
        addCreatedStructure(type, obj as AsmValue)
        return typedAs(obj,type)
    }

    override suspend fun createStructureValueSuspend(possiblyQualifiedTypeName: PossiblyQualifiedName, constructorArgs: Map<String, TypedObject>): TypedObject =
        createStructureValue(possiblyQualifiedTypeName, constructorArgs) // no need for anything suspend specific

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
                typedAs(AsmListSimple(collection.map { it.self as AsmValue }), type)
             }

            StdLibDefault.ListSeparated.qualifiedName -> {
                val elType = collectionType.typeArguments.getOrNull(0)?.type ?: StdLibDefault.AnyType
                val sepType = collectionType.typeArguments.getOrNull(1)?.type ?: StdLibDefault.AnyType
                val type = StdLibDefault.ListSeparated.type(listOf(elType.asTypeArgument, sepType.asTypeArgument))
                typedAs(AsmListSeparatedSimple(collection.map { it.self as AsmValue }.toSeparatedList()), type)
            }

            StdLibDefault.Set.qualifiedName -> {
                val elType = collectionType.typeArguments.firstOrNull()?.type ?: StdLibDefault.AnyType
                val type = StdLibDefault.Set.type(listOf(elType.asTypeArgument))
                typedAs(AsmSetSimple(collection.map { it.self as AsmValue }.toSet()), type)
            }

//            StdLibDefault.Map.qualifiedName -> {
//                val keyType = collectionType.typeArguments.getOrNull(0)?.type ?: StdLibDefault.AnyType
//                val valType = collectionType.typeArguments.getOrNull(1)?.type ?: StdLibDefault.AnyType
//                val map = collection.associate { it.self as Pair<Any, Any> }
//                typedAs(map, StdLibDefault.Map.type(listOf(keyType.asTypeArgument, valType.asTypeArgument)))
//            }
            else -> {
                issues.error(null,"Unsupported collection type: '${collectionType.qualifiedTypeName.value}'")
                nothing()
            }
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

    override fun valueOf(value: TypedObject): Any = (value.self as AsmValue).raw

    override fun getFromListWithIndex(tobj: TypedObject, index: Int): TypedObject {
        val asmValue = tobj.self
        return when (asmValue) {
            is AsmList -> {
                val el = asmValue.elements.getOrNull(index)
                when (el) {
                    null -> {
                        issues.error(null, "in getIndex argument index '$index' out of range")
                        nothing()
                    }

                    else -> el.asmToTypedObject()
                }
            }

            else -> {
                issues.error(null, "getIndex not supported on type '${tobj.type.typeName}'")
                nothing()
            }
        }
    }

    override fun getFromMapWithKey(tobj: TypedObject, key: TypedObject): TypedObject {
        TODO()
    }

    override fun forEachIndexed(tobj: TypedObject, body: (index: Int, value: TypedObject) -> Unit) {
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

    override fun cast(tobj: TypedObject, newType: TypeInstance): TypedObject {
        val rtd = newType.resolvedDeclaration
        return when (rtd) {
            is TupleType -> {
                val targs = (tobj.self as AsmStructure).property.map {
                    val n = PropertyName(it.key.value)
                    val t = StdLibDefault.AnyType //TODO: can do better!
                    TypeArgumentNamedSimple(n, t)
                }
                val tp = rtd.typeTuple(targs)
                typedAs(tobj.self, tp)
            }

            else -> (tobj.self as AsmValue).asmToTypedObject()
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

    override fun getCompositeGraphFrom(resultGraphIdentity: String, roots: List<TypedObject>): ObjectGraph {
        val nodes = mutableSetOf<TypedObject>()
        val edges = mutableSetOf<ObjectGraphEdge>()

        AsmSimple.traverseDepthFirst(roots.map { it.self as AsmValue}, object : AsmTreeWalker {
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
                    edges.add(edge as ObjectGraphEdge)
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

    //
    override fun createLambdaValue(lambda: (it: TypedObject) -> TypedObject): TypedObject {
        val lambdaType = StdLibDefault.Lambda //TODO: typeargs like tuple
        val lmb = AsmLambdaSimple { lambda.invoke((it as AsmValue).asmToTypedObject()).self as AsmValue }
        return typedAs(lmb, lambdaType)
    }

    override suspend fun createLambdaValueSuspend(lambda: suspend (it: TypedObject) -> TypedObject): TypedObject =
        error("Should not be called") //TODO: can we combine this with above using crossinline ?

    override fun getProperty(tobj: TypedObject, propertyName: String): TypedObject {
        //TODO: use executor
        val asmValue = tobj.self as AsmValue
        val propRes = tobj.type.allResolvedProperty[PropertyName(propertyName)]
        return when (propRes) {
            null -> when {
                asmValue is AsmStructure -> {
                    val pv = asmValue.property[PropertyValueName(propertyName)]
                    pv?.let { pv.value.asmToTypedObject() } ?: nothing()
                }

                else -> {
                    issues.warn(null, $$"in getProperty, property '$$propertyName' not found on object of type '$${tobj.type.typeName}', using value '$nothing'")
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
                    propExec.invoke(asmValue, propRes).asmToTypedObject()
                }

                is PropertyDeclarationStored -> when (asmValue) {
                    is AsmStructure -> {
                        val pv = asmValue.property[PropertyValueName(propertyName)]
                        pv?.let { pv.value.asmToTypedObject() } ?: nothing()
                    }

                    is AsmReference -> asmValue.value?.let { it.property[PropertyValueName(propertyName)]?.value }?.asmToTypedObject() ?: nothing()
                    else -> error("Cannot evaluate property '${propertyName}' on object of type '${tobj::class.simpleName}'")
                }

                else -> error("Subtype of PropertyDeclaration not handled: '${this::class.simpleName}'")
            }
        }
    }

    override suspend fun getPropertySuspend(tobj: TypedObject, propertyName: String): TypedObject =
        getProperty(tobj,propertyName) // no need for anything suspend specific

    override fun setProperty(tobj: TypedObject, propertyName: String, value: TypedObject) {
        val obj = tobj.self as AsmStructure
        // if property is a reference && value is a structure && semanticPath is available THEN set a reference rather than composition
        val v = convertValue(tobj.type, propertyName, value)
        //TODO: use executor
        obj.setProperty(PropertyValueName(propertyName), v, obj.property.size)
    }

    override fun executeMethod(tobj: TypedObject, methodName: String, args: List<TypedObject>): TypedObject {
        //TODO: use executor
        val methRes = tobj.type.allResolvedMethod[MethodName(methodName)]
        return when (methRes) {
            null -> {
                issues.warn(null, $$"in executeMethod, Method '$$methodName' not found on object of type '$${tobj.type.typeName}', using value '$nothing'")
                nothing()
            }

            else -> {
                val type = tobj.type.resolvedDeclaration
                val stdMeths = StdLibPrimitiveExecutionsForAsmSimple.method[type]
                val ao = when (stdMeths) {
                    null -> TODO()
                    else -> {
                        val methExec = stdMeths[methRes.original]
                            ?: error("StdLibPrimitiveExecutionsForAsmSimple, not found for method '${methRes.name.value}' of TypeDeclaration '${type.qualifiedName}'")
                        val self = tobj.self as AsmValue
                         val arguments = args.map { it.self }
                        methExec.invoke(self, methRes, arguments)
                    }
                }
                typedAs(ao,methRes.returnType)
            }
        }
    }

    override suspend fun executeMethodSuspend(tobj: TypedObject, methodName: String, args: List<TypedObject>): TypedObject =
        executeMethod(tobj, methodName, args) // no need for anything suspend specific

    override fun callFunction(functionName: String, args: List<TypedObject>): TypedObject {
        val arguments = args.map { untyped(it) }
        return primitiveExecutor.functionCall(functionName, arguments)?.let {
            toTypedObject(it.value as AsmValue)
        } ?: nothing()
    }

    private fun convertValue(ownerType: TypeInstance, propertyName: String, value: TypedObject): AsmValue {
        val asmValue = value.self as AsmValue
        return when (asmValue) {
            is AsmStructureSimple -> {
                val prop = ownerType.allResolvedProperty[PropertyName(propertyName)]
                val refStr = asmValue.semanticQualifiedPath?.joinToString(separator = ".")
                when (prop) {
                    null -> asmValue
                    else -> when {
                        prop.isReference && null != refStr -> AsmReferenceSimple(refStr, asmValue)
                        else -> asmValue
                    }
                }
            }

            else -> asmValue
        }
    }

}

class ObjectGraphAsmSimple(
    val identity: String,
    override val nodes: Set<TypedObject>,
    override val edges: Set<ObjectGraphEdge>
) : ObjectGraph {

    override fun hashCode(): Int = identity.hashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is ObjectGraphAsmSimple -> false
        else -> identity == other.identity
    }

    override fun toString(): String = "ObjectGraphAsmSimple(identity='$identity')"
}

data class ObjectGraphEdgeSimple(
    override val source: TypedObject,
    override val target: TypedObject,
    override val property: PropertyDeclaration
) : ObjectGraphEdge

