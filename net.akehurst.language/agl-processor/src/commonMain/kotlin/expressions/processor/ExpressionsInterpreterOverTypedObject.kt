/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.expressions.processor.ExpressionTypeResolver
import net.akehurst.language.asm.simple.*
import net.akehurst.language.expressions.asm.RootExpressionSimple
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.asm.api.*
import net.akehurst.language.expressions.api.*
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.transform.processor.AsmTransformInterpreter
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.*

data class EvaluationContext(
    val parent: EvaluationContext?,
    val namedValues: Map<String, TypedObject>
) {
    companion object {
        fun of(namedValues: Map<String, TypedObject>, parent: EvaluationContext? = null) = EvaluationContext(parent, namedValues)
        fun ofSelf(self: TypedObject) = of(mapOf(RootExpressionSimple.SELF.name to self))
    }

    val self = namedValues[RootExpressionSimple.SELF.name]

    fun getOrInParent(name: String): TypedObject? = namedValues[name] ?: parent?.getOrInParent(name)

    fun child(namedValues: Map<String, TypedObject>) = of(namedValues, this)

    override fun toString(): String {
        val sb = StringBuilder()
        this.parent?.let {
            sb.append(it.toString())
            sb.append("----------\n")
        }
        this.namedValues.forEach {
            sb.append(it.key)
            sb.append(" := ")
            sb.append(it.value.toString())
            sb.append("\n")
        }
        return sb.toString()
    }
}

interface TypedObject {
    val type: TypeInstance

    fun getPropertyValue(propertyDeclaration: PropertyDeclaration): TypedObject

    fun callMethod(methodDeclaration: MethodDeclaration, arguments: List<TypedObject>): TypedObject

    fun asString(): String
}

class TypedObjectAsmValue(
    override val type: TypeInstance,
    val self: AsmValue
) : TypedObject {

    override fun getPropertyValue(propertyDeclaration: PropertyDeclaration): TypedObject {
        val propRes = this.type.allResolvedProperty[propertyDeclaration.name]!!
        val ao = when (propertyDeclaration) {
            is PropertyDeclarationDerived -> TODO()
            is PropertyDeclarationPrimitive -> {
                val type = this.type.resolvedDeclaration
                val typeProps = StdLibPrimitiveExecutions.property[type]
                    ?: error("StdLibPrimitiveExecutions not found for TypeDeclaration '${type.qualifiedName}'")
                val propExec = typeProps[propertyDeclaration]
                    ?: error("StdLibPrimitiveExecutions not found for property '${propertyDeclaration.name}' of TypeDeclaration '${type.qualifiedName}'")
                propExec.invoke(self, propRes)
            }

            is PropertyDeclarationStored -> when (self) {
                is AsmStructure -> self.getProperty(propertyDeclaration.name.asValueName)
                else -> error("Cannot evaluate property '${propertyDeclaration.name}' on object of type '${self::class.simpleName}'")
            }

            else -> error("Subtype of PropertyDeclaration not handled: '${this::class.simpleName}'")
        }
        return TypedObjectAsmValue(propRes.typeInstance, ao)
    }

    override fun callMethod(methodDeclaration: MethodDeclaration, arguments: List<TypedObject>): TypedObject {
        val methRes = this.type.allResolvedMethod[methodDeclaration.name]!!
        val ao = when (methodDeclaration) {
            is MethodDeclarationPrimitiveSimple -> {
                val type = this.type.resolvedDeclaration
                val typeMeths = StdLibPrimitiveExecutions.method[type]
                    ?: error("StdLibPrimitiveExecutions not found for TypeDeclaration '${type.qualifiedName}'")
                val methExec = typeMeths[methodDeclaration]
                    ?: error("StdLibPrimitiveExecutions not found for method '${methodDeclaration.name}' of TypeDeclaration '${type.qualifiedName}'")
                methExec.invoke(self, methRes, arguments)
            }

            is MethodDeclarationDerivedSimple -> {
                TODO()
            }

            else -> error("Subtype of MethodDeclaration not handled: '${this::class.simpleName}'")
        }
        return TypedObjectAsmValue(methRes.returnType, ao)
    }

    override fun asString(): String = self.asString()

    override fun toString(): String = "$self : ${type.qualifiedTypeName}"
}

fun AsmValue.toTypedObject(type: TypeInstance) = TypedObjectAsmValue(type, this)
val TypedObject.asmValue
    get() = when (this) {
        is TypedObjectAsmValue -> this.self
        else -> error("Not possible to convert ${this::class.simpleName} to AsmValue")
    }

class ExpressionsInterpreterOverTypedObject(
    val typeModel: TypeModel
) {

    val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
    val typeResolver = ExpressionTypeResolver(typeModel, issues)

    /**
     * if more than one value is to be passed in as an 'evaluation-context'
     * self can contain a 'tuple' of all the necessary named values
     */
    fun evaluateStr(evc: EvaluationContext, expression: String): TypedObject {
        val result = Agl.registry.agl.expressions.processor!!.process(expression)
        check(result.issues.errors.isEmpty()) { result.issues.toString() }
        val asm = result.asm!!
        return this.evaluateExpression(evc, asm)
    }

    /**
     * if more than one value is to be passed in as an 'evaluation-context'
     * self can contain a 'tuple' of all the necessary named values
     */
    fun evaluateExpression(evc: EvaluationContext, expression: Expression): TypedObject = when (expression) {
        is RootExpression -> this.evaluateRootExpression(evc, expression)
        is LiteralExpression -> this.evaluateLiteralExpression(expression)
        is CreateObjectExpression -> this.evaluateCreateObject(evc, expression)
        is CreateTupleExpression -> this.evaluateCreateTuple(evc, expression)
        is OnExpression -> TODO()
        is NavigationExpression -> this.evaluateNavigation(evc, expression)
        is LambdaExpression -> this.evaluateLambda(evc, expression)
        is WithExpression -> this.evaluateWith(evc, expression)
        is WhenExpression -> this.evaluateWhen(evc, expression)
        is InfixExpression -> this.evaluateInfix(evc, expression)
        is CastExpression -> this.evaluateCast(evc, expression)
        is GroupExpression -> this.evaluateGroup(evc, expression)
        else -> error("Subtype of Expression not handled in 'evaluateFor'")
    }

    private fun evaluateRootExpression(evc: EvaluationContext, expression: RootExpression): TypedObject {
        return when {
            expression.isNothing -> AsmNothingSimple.toTypedObject(StdLibDefault.NothingType)
            expression.isSelf -> {
                //_issues.error(null, "evaluation of 'self' only works if self is a String, got an object of type '${self::class.simpleName}'")
                evc.self
                    ?: error("No '\$self' value defined in Evaluation Context: $evc")
            }

            expression.name.startsWith("\$") -> evaluateSpecial(evc, expression.name)
            else -> evc.getOrInParent(expression.name)
                ?: evc.self?.let { evaluatePropertyName(it, PropertyName(expression.name)) }
                ?: error("Evaluation Context does not contain '${expression.name}' and there is no 'self' object with that property name")
        }
    }

    private fun evaluateSpecial(evc: EvaluationContext, name: String): TypedObject {
        // the name must exist as a property of the self which must be a tuple
        return evc.getOrInParent(name)
            ?: evc.self?.let { evaluatePropertyName(it, PropertyName(name)) }
            ?: error("Evaluation Context does not contain '$name' and there is no 'self' object with that property name")
    }

    private fun evaluateLiteralExpression(expression: LiteralExpression): TypedObject = when (expression.qualifiedTypeName) {
        StdLibDefault.Boolean.qualifiedTypeName -> AsmPrimitiveSimple(expression.qualifiedTypeName, expression.value).toTypedObject(StdLibDefault.Boolean)
        StdLibDefault.Integer.qualifiedTypeName -> AsmPrimitiveSimple(expression.qualifiedTypeName, expression.value).toTypedObject(StdLibDefault.Integer)
        StdLibDefault.Real.qualifiedTypeName -> AsmPrimitiveSimple(expression.qualifiedTypeName, expression.value).toTypedObject(StdLibDefault.Real)
        StdLibDefault.String.qualifiedTypeName -> AsmPrimitiveSimple(expression.qualifiedTypeName, expression.value).toTypedObject(StdLibDefault.String)
        else -> error("should not happen")
    }

    private fun evaluateNavigation(evc: EvaluationContext, expression: NavigationExpression): TypedObject {
        // start should be a RootExpression or LiteralExpression
        val st = expression.start
        val start = evaluateExpression(evc, st)
        val result = expression.parts.fold(start) { acc, it ->
            when (it) {
                is PropertyCall -> evaluatePropertyName(acc, PropertyName(it.propertyName))
                is MethodCall -> evaluateMethodCall(evc, acc, MethodName(it.methodName), it.arguments)
                is IndexOperation -> evaluateIndexOperation(evc, acc, it.indices)
                else -> error("should not happen")
            }
        }
        return result
    }

    private fun evaluatePropertyName(obj: TypedObject, propertyName: PropertyName): TypedObject {
        val type = obj.type
        val pd = type.resolvedDeclaration.findAllPropertyOrNull(propertyName)
        return when (pd) {
            null -> when {
                obj.asmValue is AsmStructure -> {
                    // try with no type
                    val p = (obj.asmValue as AsmStructure).property[propertyName.asValueName]
                    when (p) {
                        null -> {
                            issues.error(null, "Property '$propertyName' not found on type '${obj.type.typeName}'")
                            AsmNothingSimple.toTypedObject(StdLibDefault.NothingType)
                        }

                        else -> {
                            val pv = p.value
                            val tp = typeModel.findByQualifiedNameOrNull(pv.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
                            TypedObjectAsmValue(tp, pv)
                        }
                    }

                }

                else -> {
                    issues.error(null, "Property '$propertyName' not found on type '${obj.type.typeName}'")
                    AsmNothingSimple.toTypedObject(StdLibDefault.NothingType)
                }
            }

            else -> obj.getPropertyValue(pd)
        }
    }

    private fun evaluateMethodCall(evc: EvaluationContext, obj: TypedObject, methodName: MethodName, args: List<Expression>): TypedObject {
        val type = obj.type
        val md = type.resolvedDeclaration.findAllMethodOrNull(methodName)
        return when (md) {
            null -> {
                issues.error(null, "Method '$methodName' not found on type '${obj.type.typeName}'")
                AsmNothingSimple.toTypedObject(StdLibDefault.NothingType)
            }

            else -> {
                val argValues = args.map {
                    evaluateExpression(evc, it)
                }
                obj.callMethod(md, argValues)
            }
        }
    }

    private fun evaluateIndexOperation(evc: EvaluationContext, obj: TypedObject, indices: List<Expression>): TypedObject {
        return when {
            obj.type.resolvedDeclaration.conformsTo(StdLibDefault.List) -> {
                when (indices.size) {
                    1 -> {
                        val idx = evaluateExpression(evc, indices[0])
                        when {
                            idx.type.conformsTo(StdLibDefault.Integer) -> {
                                val listElementType = obj.type.typeArguments.getOrNull(0)?.type
                                    ?: StdLibDefault.AnyType
                                val i = (idx.asmValue as AsmPrimitive).value as Int
                                val elem = (obj.asmValue as AsmList).elements.getOrNull(i)
                                when {
                                    null == elem -> {
                                        issues.error(null, "Index '$i' out of range")
                                        AsmNothingSimple.toTypedObject(StdLibDefault.NothingType)
                                    }

                                    elem.isNothing -> elem.toTypedObject(StdLibDefault.NothingType)
                                    else -> {
                                        val elemType = typeModel.findByQualifiedNameOrNull(elem.qualifiedTypeName)?.let {
                                            when (it) {
                                                is TupleType -> {
                                                    val targs = (elem as AsmStructure).property.map {
                                                        val n = PropertyName(it.key.value)
                                                        val t = StdLibDefault.AnyType //TODO: can do better!
                                                        TypeArgumentNamedSimple(n, t)
                                                    }
                                                    it.typeTuple(targs)
                                                }

                                                else -> it.type()
                                            }
                                        }
                                        when {
                                            null == elemType -> {
                                                issues.error(null, "Cannot find type '${elem.qualifiedTypeName}' of List element '$elem'")
                                                elem.toTypedObject(listElementType)
                                            }

                                            elemType.conformsTo(listElementType) -> elem.toTypedObject(elemType)
                                            else -> {
                                                issues.error(
                                                    null,
                                                    "List element '$elem' of type '${elem.qualifiedTypeName}' does not conform to the expected List element type of '${listElementType}'"
                                                )
                                                elem.toTypedObject(elemType)
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                issues.error(null, "Index value must evaluate to an Integer for Lists")
                                AsmNothingSimple.toTypedObject(StdLibDefault.NothingType)
                            }
                        }
                    }

                    else -> {
                        issues.error(null, "Only one index value should be used for Lists")
                        AsmNothingSimple.toTypedObject(StdLibDefault.NothingType)
                    }
                }
            }

            else -> {
                issues.error(null, "Index operation on non List value is not possible: ${obj.asString()}")
                AsmNothingSimple.toTypedObject(StdLibDefault.NothingType)
            }
        }
    }

    private fun evaluateInfix(evc: EvaluationContext, expression: InfixExpression): TypedObject {
        //TODO: Operator precedence
        var result = evaluateExpression(evc, expression.expressions.first())
        for (i in expression.operators.indices) {
            val op = expression.operators[i]
            val rhsExpr = expression.expressions[i + 1]
            val rhs = evaluateExpression(evc, rhsExpr)
            result = evaluateInfixOperator(result, op, rhs)
        }
        return result
    }

    //TODO: add operator functions to StdLib
    private fun evaluateInfixOperator(lhs: TypedObject, op: String, rhs: TypedObject): TypedObject = when (op) {
        "==" -> when {
            lhs.type == rhs.type -> {
                val lhsv = lhs.asmValue
                val rhsv = rhs.asmValue
                if (lhsv == rhsv) {
                    AsmPrimitiveSimple.stdBoolean(true).toTypedObject(StdLibDefault.Boolean)
                } else {
                    AsmPrimitiveSimple.stdBoolean(false).toTypedObject(StdLibDefault.Boolean)
                }
            }

            else -> error("'$op' must have same type for lhs and rhs")
        }

        "+" -> when {
            lhs.type.conformsTo(StdLibDefault.String) && rhs.type.conformsTo(StdLibDefault.String) -> {
                val lhsValue = (lhs.asmValue as AsmPrimitive).value as String
                val rhsValue = (rhs.asmValue as AsmPrimitive).value as String
                AsmPrimitiveSimple.stdString(lhsValue + rhsValue).toTypedObject(StdLibDefault.String)
            }

            else -> error("'$op' not supported for '${lhs.type.qualifiedTypeName}' && '${rhs.type.qualifiedTypeName}'")
        }

        else -> error("Unsupported Operator '$op'")
    }

    private fun evaluateWith(evc: EvaluationContext, expression: WithExpression): TypedObject {
        val newSelf = evaluateExpression(evc, expression.withContext)
        return when {
            newSelf.asmValue is AsmNothing -> newSelf
            else -> {
                val newEvc = evc.child(mapOf(RootExpressionSimple.SELF.name to newSelf))
                val result = evaluateExpression(newEvc, expression.expression)
                result
            }
        }
    }

    private fun evaluateWhen(evc: EvaluationContext, expression: WhenExpression): TypedObject {
        for (opt in expression.options) {
            val condValue = evaluateExpression(evc, opt.condition)
            when (condValue.type) {
                StdLibDefault.Boolean -> {
                    if ((condValue.asmValue as AsmPrimitive).value as Boolean) {
                        val result = evaluateExpression(evc, opt.expression)
                        return result
                    } else {
                        //condition not true
                    }
                }

                else -> error("Conditions/Options in a when expression must result in a Boolean value")
            }
        }
        return AsmNothingSimple.toTypedObject(StdLibDefault.NothingType)
    }

    private fun evaluateCreateTuple(evc: EvaluationContext, expression: CreateTupleExpression): TypedObject {
        //val ns = typeModel.findOrCreateNamespace(QualifiedName("\$interpreter"), listOf(Import(SimpleTypeModelStdLib.qualifiedName.value)))
        val tupleType = StdLibDefault.TupleType //ns.createTupleType()
        val tuple = AsmStructureSimple(AsmPathSimple(""), tupleType.qualifiedName)
        val typeArgs = mutableListOf<TypeArgumentNamed>()
        expression.propertyAssignments.forEach {
            val value = evaluateExpression(evc, it.rhs)
            tuple.setProperty(PropertyName(it.lhsPropertyName).asValueName, value.asmValue, tuple.property.size)
            //tupleType.appendPropertyStored(it.lhsPropertyName, value.type, setOf(PropertyCharacteristic.COMPOSITE, PropertyCharacteristic.READ_WRITE, PropertyCharacteristic.STORED))
            //val selfType = evc.self?.type ?: error("No self Type")
            //val exprType =  typeResolver.typeFor( it.rhs,selfType) ?: error("Cannot get type for expression '${it.rhs}' over type '$selfType'")
            typeArgs.add(TypeArgumentNamedSimple(PropertyName(it.lhsPropertyName), value.type))
        }
        return tuple.toTypedObject(tupleType.type(typeArgs))
    }

    private fun evaluateCreateObject(evc: EvaluationContext, expression: CreateObjectExpression): TypedObject {
        val typeDecl = typeModel.findFirstByPossiblyQualifiedOrNull(expression.possiblyQualifiedTypeName)
            ?: error("Type not found ${expression.possiblyQualifiedTypeName}")
        val asmPath = evaluateRootExpression(evc, RootExpressionSimple(AsmTransformInterpreter.PATH.value)) //FIXME: don't like this import on AsmTransformInterpreter
        val obj = AsmStructureSimple((asmPath.asmValue as AsmAny).value as AsmPath, typeDecl.qualifiedName)

        val args = expression.arguments.map { evaluateExpression(evc, it) }
        val consProps = typeDecl.property.filter { it.characteristics.contains(PropertyCharacteristic.CONSTRUCTOR) }
        if (consProps.size != args.size) error("Wrong number of constructor arguments for ${typeDecl.qualifiedName}")
        consProps.forEachIndexed { idx, pd ->
            val value = args[idx]
            obj.setProperty(pd.name.asValueName, value.asmValue, obj.property.size)
        }

        expression.propertyAssignments.forEach {
            val value = evaluateExpression(evc, it.rhs)
            obj.setProperty(PropertyName(it.lhsPropertyName).asValueName, value.asmValue, obj.property.size)
        }
        return obj.toTypedObject(typeDecl.type())
    }

    private fun evaluateLambda(evc: EvaluationContext, expression: LambdaExpression): TypedObject {
        val lambdaType = StdLibDefault.Lambda //TODO: typeargs like tuple
        return AsmLambdaSimple({ it ->
            val newEvc = evc.child(mapOf("it" to it.toTypedObject(StdLibDefault.AnyType)))
            evaluateExpression(newEvc, expression.expression).asmValue
        }).toTypedObject(lambdaType)
    }

    private fun evaluateCast(evc: EvaluationContext, expression: CastExpression): TypedObject {
        //TODO: do we need a type check? or can we assume it is already done in semantic analysis!
        val exprResult = evaluateExpression(evc, expression.expression)
        val tgtType = evaluateTypeReference(expression.targetType)
        return exprResult.asmValue.toTypedObject(tgtType)
    }

    private fun evaluateGroup(evc: EvaluationContext, expression: GroupExpression): TypedObject {
        return evaluateExpression(evc, expression.expression)
    }

    private fun evaluateTypeReference(typeReference: TypeReference): TypeInstance {
        //TODO: issues rather than exceptions!
        val decl = typeModel.findFirstByPossiblyQualifiedOrNull(typeReference.possiblyQualifiedName) ?: error("Type not found ${typeReference.possiblyQualifiedName}")
        val targs = typeReference.typeArguments.map { evaluateTypeReference(it).asTypeArgument }
        return decl.type(targs, typeReference.isNullable)
    }

}

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

    val method = mapOf<TypeDefinition, Map<MethodDeclaration, ((AsmValue, MethodDeclaration, List<TypedObject>) -> AsmValue)>>(
        StdLibDefault.List to mapOf(
            StdLibDefault.List.findAllMethodOrNull(MethodName("get"))!! to { self, meth, args ->
                check(self is AsmList) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' has wrong number of argument, expecting 1, received ${args.size}" }
                check(args[0].asmValue is AsmPrimitive) { "Method '${meth.name}' takes an ${StdLibDefault.Integer.qualifiedTypeName} as its argument, received ${args[0].type.qualifiedTypeName}" }
                check(StdLibDefault.Integer.qualifiedTypeName == args[0].type.qualifiedTypeName) { "Method '${meth.name}' takes an ${StdLibDefault.Integer.qualifiedTypeName} as its argument, received ${args[0].type.qualifiedTypeName}" }
                val arg1 = args[0].asmValue as AsmPrimitive
                val idx = arg1.value as Int
                self.elements.get(idx)
            },
            StdLibDefault.Collection.findAllMethodOrNull(MethodName("map"))!! to { self, meth, args ->
                check(self is AsmList) { "Method '${meth.name}' is not applicable to '${self::class.simpleName}' objects." }
                check(1 == args.size) { "Method '${meth.name}' takes 1 lambda argument got ${args.size} arguments." }
                check(args[0].asmValue is AsmLambda) { "Method '${meth.name}' first argument must be a lambda, got '${args[0].asmValue::class.simpleName}'." }
                val lambda = args[0].asmValue as AsmLambda
                val mapped = self.elements.map {
                    val args = mapOf("it" to it)
                    lambda.invoke(args)
                }
                AsmListSimple(mapped)
            }
        )
    )
}