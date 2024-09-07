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

package net.akehurst.language.agl.language.asmTransform

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.default.Grammar2TransformRuleSet
import net.akehurst.language.agl.default.Grammar2TypeModelMapping
import net.akehurst.language.agl.default.GrammarModel2TransformModel
import net.akehurst.language.agl.language.base.ModelAbstract
import net.akehurst.language.agl.language.base.NamespaceAbstract
import net.akehurst.language.agl.language.expressions.asm.*
import net.akehurst.language.agl.language.grammar.ContextFromGrammar
import net.akehurst.language.agl.language.grammar.asm.GrammarModelDefault
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.api.language.asmTransform.*
import net.akehurst.language.api.language.base.Indent
import net.akehurst.language.api.language.base.PossiblyQualifiedName
import net.akehurst.language.api.language.base.QualifiedName
import net.akehurst.language.api.language.base.SimpleName
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.language.grammar.Grammar
import net.akehurst.language.api.language.grammar.GrammarModel
import net.akehurst.language.api.language.grammar.GrammarNamespace
import net.akehurst.language.api.language.grammar.GrammarRuleName
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.simple.TypeModelSimple

class TransformModelDefault(
    override val name: SimpleName,
    override val typeModel: TypeModel?,
    namespace: List<TransformNamespace>
) : TransformModel, ModelAbstract<TransformNamespace, TransformRuleSet>(namespace) {

    companion object {
        fun fromString(context: ContextFromGrammar, transformStr: String): ProcessResult<TransformModel> {
            val proc = Agl.registry.agl.asmTransform.processor ?: error("Asm-Transform language not found!")
            return proc.process(
                sentence = transformStr,
                Agl.options {
                    semanticAnalysis { context(context) }
                }
            )
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

        fun fromGrammar(
            grammar: Grammar,
            typeModel: TypeModel = TypeModelSimple(grammar.name),
            configuration: Grammar2TypeModelMapping? = Grammar2TransformRuleSet.defaultConfiguration
        ): ProcessResult<TransformModel> {
            val grammarModel = GrammarModelDefault(grammar.name, listOf(grammar.namespace as GrammarNamespace))
            return fromGrammarModel(grammarModel, typeModel, configuration)
        }

    }

    override val namespace: List<TransformNamespace>
        get() = super.namespace

}

internal class TransformNamespaceDefault(
    qualifiedName: QualifiedName,
) : TransformNamespace, NamespaceAbstract<TransformRuleSet>(qualifiedName) {

}

internal class TransformRuleSetDefault(
    override val namespace: TransformNamespace,
    override val name: SimpleName,
    _rules: List<TransformationRule>
) : TransformRuleSet {

    override val qualifiedName: QualifiedName get() = namespace.qualifiedName.append(name)

    override val rules: Map<GrammarRuleName, TransformationRule> = _rules.associateBy(TransformationRule::grammarRuleName).toMutableMap()

    override val createObjectRules: List<CreateObjectRule> get() = rules.values.filterIsInstance<CreateObjectRule>()
    override val modifyObjectRules: List<ModifyObjectRule> get() = rules.values.filterIsInstance<ModifyObjectRule>()

    override fun findTrRuleForGrammarRuleNamedOrNull(grmRuleName: GrammarRuleName): TransformationRule? =
        rules[grmRuleName]

    fun addRule(tr: TransformationRule) {
        (rules as MutableMap)[tr.grammarRuleName] = tr
    }

    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        sb.append("grammar-transform {\n")
        val rulesStr = rules.map {
            "${it.key}: ${it.value}"
        }.joinToString(separator = "\n")
        sb.append("rulesStr")
        sb.append("}")
        return sb.toString()
    }
}

internal abstract class TransformationRuleAbstract : TransformationRule {

    companion object {
        val CHILD_0 = ExpressionSelfStatementSimple(
            NavigationSimple(
                start = RootExpressionSimple("child"),
                parts = listOf(IndexOperationSimple(listOf(LiteralExpressionSimple(LiteralExpressionSimple.INTEGER, 0))))
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

internal class TransformationRuleDefault(
    override val possiblyQualifiedTypeName: PossiblyQualifiedName,
    override val expression: Expression
) : TransformationRuleAbstract() {

    override fun asString(indent: Indent): String {
        return "$indent${grammarRuleName}: ${expression.asString(indent)} as $possiblyQualifiedTypeName"
    }

    override fun toString(): String = "${expression} as $possiblyQualifiedTypeName"
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