/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.transform.asm

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.TransformString
import net.akehurst.language.agl.simple.Grammar2TransformRuleSet
import net.akehurst.language.agl.simple.Grammar2TypeModelMapping
import net.akehurst.language.agl.simple.GrammarModel2TransformModel
import net.akehurst.language.expressions.asm.IndexOperationSimple
import net.akehurst.language.expressions.asm.LiteralExpressionSimple
import net.akehurst.language.expressions.asm.NavigationSimple
import net.akehurst.language.expressions.asm.RootExpressionSimple
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.agl.simple.ContextFromGrammarAndTypeModel
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.base.api.*
import net.akehurst.language.base.asm.ModelAbstract
import net.akehurst.language.base.asm.NamespaceAbstract
import net.akehurst.language.grammar.api.*
import net.akehurst.language.transform.api.*
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.asm.TypeModelSimple

class TransformModelDefault(
    override val name: SimpleName,
    override val options: List<Option>,
    namespace: List<TransformNamespace>
) : TransformModel, ModelAbstract<TransformNamespace, TransformRuleSet>() {

    companion object {
        fun fromString(context: ContextFromGrammarAndTypeModel, transformStr: TransformString): ProcessResult<TransformModel> {
            val proc = Agl.registry.agl.transform.processor ?: error("Asm-Transform language not found!")
            val res = proc.process(
                sentence = transformStr.value,
                Agl.options {
                    semanticAnalysis { context(context) }
                }
            )
            return when {
                res.issues.errors.isEmpty() -> res
                else -> error(res.issues.toString())
            }
        }

        fun fromGrammarModel(
            grammarModel: GrammarModel,
            typeModel: TypeModel = TypeModelSimple(grammarModel.allDefinitions.last().name),
            configuration: Grammar2TypeModelMapping? = Grammar2TransformRuleSet.defaultConfiguration
        ): ProcessResult<TransformModel> {
            val atfg = GrammarModel2TransformModel(typeModel, grammarModel, configuration)
            val trModel = atfg.build()
            return ProcessResultDefault<TransformModel>(trModel, atfg.issues)
        }

    }

    override var typeModel: TypeModel? = null

    private val _namespace: Map<QualifiedName, TransformNamespace> = linkedMapOf<QualifiedName, TransformNamespace>().also { map ->
        namespace.forEach { map[it.qualifiedName] = it }
    }
    override val namespace: List<TransformNamespace> get() = _namespace.values.toList()

    override fun findOrCreateNamespace(qualifiedName: QualifiedName, imports: List<Import>): TransformNamespace {
        return if (_namespace.containsKey(qualifiedName)) {
            _namespace[qualifiedName]!!
        } else {
            val ns = TransformNamespaceDefault(qualifiedName, emptyList(),imports)//, imports)
            addNamespace(ns)
            ns
        }
    }

    fun addNamespace(ns: TransformNamespace) {
        if (_namespace.containsKey(ns.qualifiedName)) {
            if (_namespace[ns.qualifiedName] === ns) {
                //same object, no need to add it
            } else {
                error("TypeModel '${this.name}' already contains a namespace '${ns.qualifiedName}'")
            }
        } else {
            (_namespace as MutableMap)[ns.qualifiedName] = ns
        }
    }
}

class TransformNamespaceDefault(
    override val qualifiedName: QualifiedName,
    override val options: List<Option>,
    override val import: List<Import>
) : TransformNamespace, NamespaceAbstract<TransformRuleSet>() {

}

data class TransformRuleSetReferenceDefault(
    override val localNamespace: TransformNamespace,
    override val nameOrQName: PossiblyQualifiedName
) : TransformRuleSetReference {
    override var resolved: TransformRuleSet? = null
    override fun resolveAs(resolved: TransformRuleSet) {
        this.resolved = resolved
    }
}

class TransformRuleSetDefault(
    override val namespace: TransformNamespace,
    override val name: SimpleName,
    argExtends: List<TransformRuleSetReference>,
    override val options: List<Option>,
    _rules: List<TransformationRule>
) : TransformRuleSet {

    override val qualifiedName: QualifiedName get() = namespace.qualifiedName.append(name)

    override val extends: List<TransformRuleSetReference> = argExtends.toMutableList() //clone the list so it can be modified

    override val rules: Map<GrammarRuleName, TransformationRule> = _rules.associateBy(TransformationRule::grammarRuleName).toMutableMap()

    override val createObjectRules: List<CreateObjectRule> get() = rules.values.filterIsInstance<CreateObjectRule>()
    override val modifyObjectRules: List<ModifyObjectRule> get() = rules.values.filterIsInstance<ModifyObjectRule>()

    override fun findTrRuleForGrammarRuleNamedOrNull(grmRuleName: GrammarRuleName): TransformationRule? =
        rules[grmRuleName]

    fun addExtends(other:TransformRuleSetReference) {
        (this.extends as MutableList).add(other)
    }

    override fun addRule(tr: TransformationRule) {
        (rules as MutableMap)[tr.grammarRuleName] = tr
    }

    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        sb.append("grammar-transform $name {\n")
        val newIndent = indent.inc
        val rulesStr = rules
            .entries.sortedBy {
                it.key.value
            }.map {
                it.value.asString(newIndent)
            }.joinToString(separator = "\n")
        sb.append(rulesStr)
        sb.append("\n${indent}}")
        return sb.toString()
    }
}

abstract class TransformationRuleAbstract : TransformationRule {

    companion object {
        internal val CHILD_0 = ExpressionSelfStatementSimple(
            NavigationSimple(
                start = RootExpressionSimple("child"),
                parts = listOf(IndexOperationSimple(listOf(LiteralExpressionSimple(SimpleTypeModelStdLib.Integer.qualifiedTypeName, 0))))
            )
        )
    }

    override var grammarRuleName: GrammarRuleName = GrammarRuleName("<unset>")
    override val resolvedType: TypeInstance get() = _resolvedType

    private lateinit var _resolvedType: TypeInstance

    fun resolveTypeAs(type: TypeInstance) {
        _resolvedType = type
    }

    //override val modifyStatements = mutableListOf<AssignmentStatement>()

//    fun appendAssignment(lhsPropertyName: String, rhs: Expression) {
//        val ass = AssignmentStatementSimple(lhsPropertyName, rhs)
//        modifyStatements.add(ass)
//    }

    override fun asString(indent: Indent): String {
        return "$indent${grammarRuleName}: ${this}"
    }

    abstract override fun toString(): String
}

class TransformationRuleDefault(
    override val possiblyQualifiedTypeName: PossiblyQualifiedName,
    override val expression: Expression
) : TransformationRuleAbstract() {

    override fun asString(indent: Indent): String {
        return "$indent${grammarRuleName}: ${expression.asString(indent)} as $possiblyQualifiedTypeName"
    }

    override fun toString(): String = "${expression} as $possiblyQualifiedTypeName"
}
internal fun transformationRuleUnresolved(qualifiedTypeName: QualifiedName, expression: Expression): TransformationRuleDefault {
    return TransformationRuleDefault(
        qualifiedTypeName,
        expression
    )
}
internal fun transformationRule(type: TypeInstance, expression: Expression): TransformationRuleDefault {
    return TransformationRuleDefault(
        type.qualifiedTypeName,
        expression
    ).also {
        it.resolveTypeAs(type)
    }
}

//class CreateObjectRuleSimple(
//    override val qualifiedTypeName: String
//) : TransformationRuleAbstract(), CreateObjectRule {
//
//    override val selfStatement: SelfStatement = ConstructObjectSelfStatementSimple(qualifiedTypeName)
//
//    override fun asString(indent: String, increment: String): String {
//        val ni = indent + increment
//        val sb = StringBuilder()
//        sb.append("$indent${grammarRuleName}: $qualifiedTypeName {\n")
////        sb.append("${modifyStatements.joinToString(separator = "\n") { it.asString(ni, increment) }}\n")
//        sb.append("$indent}")
//        return sb.toString()
//    }
//
//    override fun toString(): String = "$qualifiedTypeName { ... }"
//}

//class ModifyObjectRuleSimple(
//    override val qualifiedTypeName: String
//) : TransformationRuleAbstract(), ModifyObjectRule {
//
//    override val selfStatement: SelfStatement = LambdaSelfStatementSimple(qualifiedTypeName)
//
//    override fun asString(indent: String, increment: String): String {
//        val ni = indent + increment
//        val sb = StringBuilder()
//        sb.append("$indent${grammarRuleName}: { $qualifiedTypeName ->\n")
//        sb.append("$ni${modifyStatements.joinToString(separator = "\n$ni") { it.asString(ni, increment) }}\n")
//        sb.append("$indent}")
//        return sb.toString()
//    }
//
//    override fun toString(): String = "{ $qualifiedTypeName  -> ... }"
//}

//class SubtypeTransformationRuleSimple(
//    override val qualifiedTypeName: String
//) : TransformationRuleAbstract(), SubtypeTransformationRule {
//
//    override val selfStatement: SelfStatement = CHILD_0
//
//    override fun toString(): String = "child[0] as $qualifiedTypeName //subtype"
//}
//
//class UnnamedSubtypeTransformationRuleSimple() : TransformationRuleAbstract(), SubtypeTransformationRule {
//
//    override val qualifiedTypeName: String get() = UnnamedSupertypeTypeSimple.NAME
//
//    override val selfStatement: SelfStatement = TransformationRuleAbstract.CHILD_0
//
//    override fun toString(): String = "child[0] as $qualifiedTypeName //UnnamedSubtype"
//}
//
//class NothingTransformationRuleSimple() : TransformationRuleAbstract(), NoActionTransformationRule {
//
//    init {
//        super.resolveTypeAs(SimpleTypeModelStdLib.NothingType)
//    }
//
//    override val qualifiedTypeName: String get() = SimpleTypeModelStdLib.NothingType.qualifiedTypeName
//    override val selfStatement: SelfStatement = ExpressionSelfStatementSimple(RootExpressionSimple(RootExpressionSimple.NOTHING))
//
//    override fun toString(): String = "\$nothing //no action"
//}
//
//class OptionalItemTransformationRuleSimple(
//    override val qualifiedTypeName: String
//) : TransformationRuleAbstract(), NoActionTransformationRule {
//
//    override val selfStatement: SelfStatement = CHILD_0
//
//    override fun toString(): String = "child[0] as $qualifiedTypeName // optional"
//}
//
//class Child0AsStringTransformationRuleSimple() : TransformationRuleAbstract(), SelfAssignChild0TransformationRule {
//    override val qualifiedTypeName: String get() = SimpleTypeModelStdLib.String.qualifiedTypeName
//
//    override val selfStatement: SelfStatement = CHILD_0
//
//    override fun toString(): String = "child[0] as $qualifiedTypeName // self-assign"
//}
//
//class LeafAsStringTransformationRuleSimple() : TransformationRuleAbstract(), SelfAssignChild0TransformationRule {
//    override val qualifiedTypeName: String get() = SimpleTypeModelStdLib.String.qualifiedTypeName
//
//    override val selfStatement: SelfStatement get() = TODO()
//
//    override fun toString(): String = "leaf as std.String"
//}
//
//class SelfTransformationRuleSimple(
//    override val qualifiedTypeName: String
//) : TransformationRuleAbstract(), ListTransformationRule {
//
//    override val selfStatement: SelfStatement = ExpressionSelfStatementSimple(RootExpressionSimple(RootExpressionSimple.SELF))
//
//    override fun toString(): String = "self"
//}
//
//class ListTransformationRuleSimple() : TransformationRuleAbstract(), ListTransformationRule {
//    override val qualifiedTypeName: String get() = SimpleTypeModelStdLib.List.type().qualifiedTypeName
//
//    override val selfStatement: SelfStatement = ExpressionSelfStatementSimple(RootExpressionSimple("children"))
//
//    override fun toString(): String = "children as $qualifiedTypeName // list"
//}
//
//class SepListItemsTransformationRuleSimple() : TransformationRuleAbstract(), ListTransformationRule {
//    override val qualifiedTypeName: String get() = SimpleTypeModelStdLib.List.type().qualifiedTypeName
//
//    override val selfStatement: SelfStatement = ExpressionSelfStatementSimple(
//        NavigationSimple(
//            start = RootExpressionSimple("children"),
//            parts = listOf(PropertyCallSimple("items"))
//        )
//    )
//
//    override fun toString(): String = "children.items as $qualifiedTypeName // SepList"
//}

//class CreateTupleTransformationRuleSimple(
//    override val qualifiedTypeName: String
//) : TransformationRuleAbstract() {
//
//    override val selfStatement: SelfStatement = ExpressionSelfStatementSimple(CreateTupleExpressionSimple(super.modifyStatements))
//
//    override fun toString(): String = "children as $qualifiedTypeName // Tuple"
//}

internal abstract class TransformationStatementAbstract

internal abstract class SelfStatementAbstract : TransformationStatementAbstract(), SelfStatement

//class ConstructObjectSelfStatementSimple(
//    val qualifiedTypeName: String
//) : SelfStatementAbstract() {
//    override fun toString(): String = "${qualifiedTypeName}()"
//}

//class ConstructTupleSelfStatementSimple(
//    val qualifiedTypeName: String
//) : SelfStatementAbstract() {
//    override fun toString(): String = "tuple {}"
//}

internal class LambdaSelfStatementSimple(
    val qualifiedTypeName: String
) : SelfStatementAbstract() {
    override fun toString(): String = "{ -> }"
}

internal class ExpressionSelfStatementSimple(
    val expression: Expression
) : SelfStatementAbstract() {
    override fun toString(): String = "$expression"
}