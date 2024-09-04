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

package net.akehurst.language.agl.language.expressions

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.asm.*
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.api.asm.*
import net.akehurst.language.api.language.base.Import
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.language.expressions.*
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.simple.PropertyDeclarationDerived
import net.akehurst.language.typemodel.simple.PropertyDeclarationPrimitive
import net.akehurst.language.typemodel.simple.PropertyDeclarationStored
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib

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

    fun asString(): String
}

class TypedObjectAsmValue(
    override val type: TypeInstance,
    val self: AsmValue
) : TypedObject {

    override fun getPropertyValue(propertyDeclaration: PropertyDeclaration): TypedObject {
        val ao = when (propertyDeclaration) {
            is PropertyDeclarationDerived -> TODO()
            is PropertyDeclarationPrimitive -> {
                val type = this.type.declaration
                val typeProps = StdLibPrimitiveExecutions.property[type] ?: error("StdLibPrimitiveExecutions not found for TypeDeclaration '${type.qualifiedName}'")
                val propExec = typeProps[propertyDeclaration]
                    ?: error("StdLibPrimitiveExecutions not found for property '${propertyDeclaration.name}' of TypeDeclaration '${type.qualifiedName}'")
                propExec.invoke(self, propertyDeclaration)
            }

            is PropertyDeclarationStored -> when (self) {
                is AsmStructure -> self.getProperty(propertyDeclaration.name)
                else -> error("Cannot evaluate property '${propertyDeclaration.name}' on object of type '${self::class.simpleName}'")
            }

            else -> error("Subtype of PropertyDeclaration not handled: '${this::class.simpleName}'")
        }
        return TypedObjectAsmValue(propertyDeclaration.typeInstance, ao)
    }

    override fun asString(): String = self.asString()

    override fun toString(): String = "$self : ${type.qualifiedTypeName}"
}

fun AsmValue.toTypedObject(type: TypeInstance) = TypedObjectAsmValue(type, this)
val TypedObject.asm
    get() = when (this) {
        is TypedObjectAsmValue -> this.self
        else -> error("Not possible to convert ${this::class.simpleName} to AsmValue")
    }

class ExpressionsInterpreterOverTypedObject(
    val typeModel: TypeModel
) {

    val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)

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
        is NavigationExpression -> this.evaluateNavigation(evc, expression)
        is InfixExpression -> this.evaluateInfix(evc, expression)
        is CreateTupleExpression -> this.evaluateCreateTuple(evc, expression)
        is CreateObjectExpression -> this.evaluateCreateObject(evc, expression)
        is WithExpression -> this.evaluateWith(evc, expression)
        is WhenExpression -> this.evaluateWhen(evc, expression)
        else -> error("Subtype of Expression not handled in 'evaluateFor'")
    }

    private fun evaluateRootExpression(evc: EvaluationContext, expression: RootExpression): TypedObject {
        return when {
            expression.isNothing -> AsmNothingSimple.toTypedObject(SimpleTypeModelStdLib.NothingType)
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
        LiteralExpressionSimple.BOOLEAN -> AsmPrimitiveSimple(SimpleTypeModelStdLib.Boolean.qualifiedTypeName, expression.value).toTypedObject(SimpleTypeModelStdLib.Boolean)
        LiteralExpressionSimple.INTEGER -> AsmPrimitiveSimple(SimpleTypeModelStdLib.Integer.qualifiedTypeName, expression.value).toTypedObject(SimpleTypeModelStdLib.Integer)
        LiteralExpressionSimple.REAL -> AsmPrimitiveSimple(SimpleTypeModelStdLib.Real.qualifiedTypeName, expression.value).toTypedObject(SimpleTypeModelStdLib.Real)
        LiteralExpressionSimple.STRING -> AsmPrimitiveSimple(SimpleTypeModelStdLib.String.qualifiedTypeName, expression.value).toTypedObject(SimpleTypeModelStdLib.String)
        else -> error("should not happen")
    }

    private fun evaluateNavigation(evc: EvaluationContext, expression: NavigationExpression): TypedObject {
        // start should be a RootExpression or LiteralExpression
        val st = expression.start
        val start = when (st) {
            is LiteralExpression -> evaluateLiteralExpression(st)
            is RootExpression -> evaluateRootExpression(evc, st)
            else -> error("should not happen")
        }
        val result =
            expression.parts.fold(start) { acc, it ->
                when (it) {
                    is PropertyCall -> evaluatePropertyName(acc, it.propertyName)
                    is MethodCall -> TODO()
                    is IndexOperation -> evaluateIndexOperation(evc, acc, it.indices)
                    else -> error("should not happen")
                }
            }
        return result
    }

    private fun evaluatePropertyName(obj: TypedObject, propertyName: PropertyName): TypedObject {
        val type = obj.type
        val pd = type.declaration.findPropertyOrNull(propertyName)
        return when (pd) {
            null -> when {
                obj.asm is AsmStructure -> {
                    // try with no type
                    val p = (obj.asm as AsmStructure).property[propertyName]
                    when (p) {
                        null -> {
                            issues.error(null, "Property '$propertyName' not found on type '${obj.type.typeName}'")
                            AsmNothingSimple.toTypedObject(SimpleTypeModelStdLib.NothingType)
                        }

                        else -> {
                            val pv = p.value
                            val tp = typeModel.findByQualifiedNameOrNull(pv.qualifiedTypeName)?.type() ?: SimpleTypeModelStdLib.AnyType
                            TypedObjectAsmValue(tp, pv)
                        }
                    }

                }

                else -> {
                    issues.error(null, "Property '$propertyName' not found on type '${obj.type.typeName}'")
                    AsmNothingSimple.toTypedObject(SimpleTypeModelStdLib.NothingType)
                }
            }

            else -> obj.getPropertyValue(pd)
        }
    }

    private fun evaluateIndexOperation(evc: EvaluationContext, obj: TypedObject, indices: List<Expression>): TypedObject {
        return when {
            obj.type.declaration.conformsTo(SimpleTypeModelStdLib.List) -> {
                when (indices.size) {
                    1 -> {
                        val idx = evaluateExpression(evc, indices[0])
                        when {
                            idx.type.conformsTo(SimpleTypeModelStdLib.Integer) -> {
                                val listElementType = obj.type.typeArguments.getOrNull(0) ?: SimpleTypeModelStdLib.AnyType
                                val i = (idx.asm as AsmPrimitive).value as Int
                                (obj.asm as AsmList).elements.getOrNull(i)?.toTypedObject(listElementType) ?: run {
                                    issues.error(null, "Index '$i' out of range")
                                    AsmNothingSimple.toTypedObject(SimpleTypeModelStdLib.NothingType)
                                }
                            }

                            else -> {
                                issues.error(null, "Index value must evaluate to an Integer for Lists")
                                AsmNothingSimple.toTypedObject(SimpleTypeModelStdLib.NothingType)
                            }
                        }
                    }

                    else -> {
                        issues.error(null, "Only one index value should be used for Lists")
                        AsmNothingSimple.toTypedObject(SimpleTypeModelStdLib.NothingType)
                    }
                }
            }

            else -> {
                issues.error(null, "Index operation on non List value is not possible: ${obj.asString()}")
                AsmNothingSimple.toTypedObject(SimpleTypeModelStdLib.NothingType)
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

    private fun evaluateInfixOperator(lhs: TypedObject, op: String, rhs: TypedObject): TypedObject = when (op) {
        "==" -> when {
            lhs.type == rhs.type -> {
                val lhsv = lhs.asm
                val rhsv = rhs.asm
                if (lhsv == rhsv) {
                    AsmPrimitiveSimple.stdBoolean(true).toTypedObject(SimpleTypeModelStdLib.Boolean)
                } else {
                    AsmPrimitiveSimple.stdBoolean(false).toTypedObject(SimpleTypeModelStdLib.Boolean)
                }
            }

            else -> error("'$op' must have same type for lhs and rhs")
        }

        else -> error("Unsupported Operator '$op'")
    }

    private fun evaluateWith(evc: EvaluationContext, expression: WithExpression): TypedObject {
        val newSelf = evaluateExpression(evc, expression.withContext)
        return when {
            newSelf.asm is AsmNothing -> newSelf
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
                SimpleTypeModelStdLib.Boolean -> {
                    if ((condValue.asm as AsmPrimitive).value as Boolean) {
                        val result = evaluateExpression(evc, opt.expression)
                        return result
                    } else {
                        //condition not true
                    }
                }

                else -> error("Conditions/Options in a when expression must result in a Boolean value")
            }
        }
        return AsmNothingSimple.toTypedObject(SimpleTypeModelStdLib.NothingType)
    }

    private fun evaluateCreateTuple(evc: EvaluationContext, expression: CreateTupleExpression): TypedObject {
        val ns = typeModel.findOrCreateNamespace(QualifiedName("\$interpreter"), listOf(Import(SimpleTypeModelStdLib.qualifiedName.value)))
        val tuple = AsmStructureSimple(AsmPathSimple(""), TupleType.NAME)
        val tupleType = ns.createTupleType()
        expression.propertyAssignments.forEach {
            val value = evaluateExpression(evc, it.rhs)
            tuple.setProperty(it.lhsPropertyName, value.asm, tuple.property.size)
            tupleType.appendPropertyStored(it.lhsPropertyName, value.type, setOf(PropertyCharacteristic.COMPOSITE, PropertyCharacteristic.MEMBER))
        }
        return tuple.toTypedObject(tupleType.type())
    }

    private fun evaluateCreateObject(evc: EvaluationContext, expression: CreateObjectExpression): TypedObject {
        val typeDecl = typeModel.findFirstByPossiblyQualifiedOrNull(expression.possiblyQualifiedTypeName)
            ?: error("Type not found ${expression.possiblyQualifiedTypeName}")
        val obj = AsmStructureSimple(AsmPathSimple(""), typeDecl.qualifiedName)

        val args = expression.arguments.map { evaluateExpression(evc, it) }
        val consProps = typeDecl.property.filter { it.characteristics.contains(PropertyCharacteristic.CONSTRUCTOR) }
        if (consProps.size != args.size) error("Wrong number of constructor arguments for ${typeDecl.qualifiedName}")
        consProps.forEachIndexed { idx, pd ->
            val value = args[idx]
            obj.setProperty(pd.name, value.asm, obj.property.size)
        }

        expression.propertyAssignments.forEach {
            val value = evaluateExpression(evc, it.rhs)
            obj.setProperty(it.lhsPropertyName, value.asm, obj.property.size)
        }
        return obj.toTypedObject(typeDecl.type())
    }
}

object StdLibPrimitiveExecutions {

    val property = mapOf<TypeDeclaration, Map<PropertyDeclaration, ((AsmValue, PropertyDeclaration) -> AsmValue)>>(
        SimpleTypeModelStdLib.List to mapOf(
            SimpleTypeModelStdLib.List.findPropertyOrNull(PropertyName("size"))!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmPrimitiveSimple.stdInteger(self.elements.size)
            },
            SimpleTypeModelStdLib.List.findPropertyOrNull(PropertyName("first"))!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.elements.first()
            },
            SimpleTypeModelStdLib.List.findPropertyOrNull(PropertyName("last"))!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                self.elements.last()
            },
            SimpleTypeModelStdLib.List.findPropertyOrNull(PropertyName("back"))!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmListSimple(self.elements.drop(1))
            },
            SimpleTypeModelStdLib.List.findPropertyOrNull(PropertyName("front"))!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmListSimple(self.elements.dropLast(1))
            },
            SimpleTypeModelStdLib.List.findPropertyOrNull(PropertyName("join"))!! to { self, prop ->
                check(self is AsmList) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmPrimitiveSimple.stdString(self.elements.joinToString(separator = "") { it.asString() })
            }
        ),
        SimpleTypeModelStdLib.ListSeparated to mapOf(
            SimpleTypeModelStdLib.ListSeparated.findPropertyOrNull(PropertyName("items"))!! to { self, prop ->
                check(self is AsmListSeparated) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmListSimple(self.elements.items)
            },
            SimpleTypeModelStdLib.ListSeparated.findPropertyOrNull(PropertyName("separators"))!! to { self, prop ->
                check(self is AsmListSeparated) { "Property '${prop.name}' is not applicable to '${self::class.simpleName}' objects." }
                AsmListSimple(self.elements.separators)
            },
        )
    )

}