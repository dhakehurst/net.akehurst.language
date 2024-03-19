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
import net.akehurst.language.agl.default.Grammar2TypeModelMapping
import net.akehurst.language.agl.default.GrammarNamespaceAndAsmTransformBuilderFromGrammar
import net.akehurst.language.agl.default.TypeModelFromGrammar
import net.akehurst.language.agl.language.expressions.*
import net.akehurst.language.agl.language.grammar.ContextFromGrammar
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.api.language.asmTransform.*
import net.akehurst.language.api.language.expressions.AssignmentStatement
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.language.grammar.Grammar
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.simple.UnnamedSupertypeTypeSimple


class AsmTransformModelSimple(
    override val qualifiedName: String
) : AsmTransformModel {

    companion object {
        fun fromString(context: ContextFromGrammar, transformStr: String): ProcessResult<List<AsmTransformModel>> {
            val proc = Agl.registry.agl.asmTransform.processor ?: error("Asm-Transform language not found!")
            return proc.process(
                sentence = transformStr,
                Agl.options {
                    semanticAnalysis { context(context) }
                }
            )
        }

        fun fromGrammar(
            grammar: Grammar,
            typeModel: TypeModel,
            configuration: Grammar2TypeModelMapping? = TypeModelFromGrammar.defaultConfiguration
        ): ProcessResult<List<AsmTransformModel>> {
            val atfg = GrammarNamespaceAndAsmTransformBuilderFromGrammar(typeModel, grammar, configuration)
            atfg.build()
            return ProcessResultDefault<List<AsmTransformModel>>(listOf(atfg.transformModel), atfg.issues)
        }
    }

    override val name: String get() = this.qualifiedName.split(".").last()

    override var typeModel: TypeModel? = null

    override val rules get() = _rules

    override val createObjectRules: List<CreateObjectRule> get() = rules.values.filterIsInstance<CreateObjectRule>()
    override val modifyObjectRules: List<ModifyObjectRule> get() = rules.values.filterIsInstance<ModifyObjectRule>()

    override fun findTrRuleForGrammarRuleNamedOrNull(grmRuleName: String): TransformationRule? =
        rules[grmRuleName]

    // GrammarRuleName -> TransformationRule
    private val _rules = mutableMapOf<String, TransformationRule>()

    fun addRule(tr: TransformationRule) {
        _rules[tr.grammarRuleName] = tr
    }

    override fun asString(indent: String, increment: String): String {
        val ni = indent + increment
        val rulesStr = rules.values.sortedBy { it.grammarRuleName }.joinToString(separator = "\n") { it.asString(ni, increment) }
        val sb = StringBuilder()
        sb.append("${indent}namespace $qualifiedName\n")
        sb.append("${indent}transform $qualifiedName {\n")
        sb.append("$rulesStr\n")
        sb.append("${indent}}")
        return sb.toString()
    }
}

abstract class TransformationRuleAbstract : TransformationRule {

    companion object {
        val CHILD_0 = ExpressionSelfStatementSimple(
            NavigationSimple(
                start = RootExpressionSimple("child"),
                parts = listOf(IndexOperationSimple(listOf(LiteralExpressionSimple(LiteralExpressionSimple.INTEGER, 0))))
            )
        )
    }

    override lateinit var grammarRuleName: String
    override val resolvedType: TypeInstance get() = _resolvedType

    private lateinit var _resolvedType: TypeInstance

    fun resolveTypeAs(type: TypeInstance) {
        _resolvedType = type
    }

    override val modifyStatements = mutableListOf<AssignmentStatement>()

    fun appendAssignment(lhsPropertyName: String, rhs: Expression) {
        val ass = AssignmentStatementSimple(lhsPropertyName, rhs)
        modifyStatements.add(ass)
    }

    override fun asString(indent: String, increment: String): String {
        return "$indent${grammarRuleName}: ${this}"
    }

    abstract override fun toString(): String
}

class CreateObjectRuleSimple(
    override val qualifiedTypeName: String
) : TransformationRuleAbstract(), CreateObjectRule {

    override val selfStatement: SelfStatement = ConstructObjectSelfStatementSimple(qualifiedTypeName)

    override fun asString(indent: String, increment: String): String {
        val ni = indent + increment
        val sb = StringBuilder()
        sb.append("$indent${grammarRuleName}: $qualifiedTypeName {\n")
        sb.append("${modifyStatements.joinToString(separator = "\n") { it.asString(ni, increment) }}\n")
        sb.append("$indent}")
        return sb.toString()
    }

    override fun toString(): String = "$qualifiedTypeName { ... }"
}

class ModifyObjectRuleSimple(
    override val qualifiedTypeName: String
) : TransformationRuleAbstract(), ModifyObjectRule {

    override val selfStatement: SelfStatement = LambdaSelfStatementSimple(qualifiedTypeName)

    override fun asString(indent: String, increment: String): String {
        val ni = indent + increment
        val sb = StringBuilder()
        sb.append("$indent${grammarRuleName}: { $qualifiedTypeName ->\n")
        sb.append("$ni${modifyStatements.joinToString(separator = "\n$ni") { it.asString(ni, increment) }}\n")
        sb.append("$indent}")
        return sb.toString()
    }

    override fun toString(): String = "{ $qualifiedTypeName  -> ... }"
}

class SubtypeTransformationRuleSimple(
    override val qualifiedTypeName: String
) : TransformationRuleAbstract(), SubtypeTransformationRule {

    override val selfStatement: SelfStatement = CHILD_0

    override fun toString(): String = "child[0] as $qualifiedTypeName //subtype"
}

class UnnamedSubtypeTransformationRuleSimple() : TransformationRuleAbstract(), SubtypeTransformationRule {

    override val qualifiedTypeName: String get() = UnnamedSupertypeTypeSimple.NAME

    override val selfStatement: SelfStatement = TransformationRuleAbstract.CHILD_0

    override fun toString(): String = "child[0] as $qualifiedTypeName //UnnamedSubtype"
}

class NothingTransformationRuleSimple() : TransformationRuleAbstract(), NoActionTransformationRule {

    init {
        super.resolveTypeAs(SimpleTypeModelStdLib.NothingType)
    }

    override val qualifiedTypeName: String get() = SimpleTypeModelStdLib.NothingType.qualifiedTypeName
    override val selfStatement: SelfStatement = ExpressionSelfStatementSimple(RootExpressionSimple(RootExpressionSimple.NOTHING))

    override fun toString(): String = "\$nothing //no action"
}

class OptionalItemTransformationRuleSimple(
    override val qualifiedTypeName: String
) : TransformationRuleAbstract(), NoActionTransformationRule {

    override val selfStatement: SelfStatement = CHILD_0

    override fun toString(): String = "child[0] as $qualifiedTypeName // optional"
}

class Child0AsStringTransformationRuleSimple() : TransformationRuleAbstract(), SelfAssignChild0TransformationRule {
    override val qualifiedTypeName: String get() = SimpleTypeModelStdLib.String.qualifiedTypeName

    override val selfStatement: SelfStatement = CHILD_0

    override fun toString(): String = "child[0] as $qualifiedTypeName // self-assign"
}

class LeafAsStringTransformationRuleSimple() : TransformationRuleAbstract(), SelfAssignChild0TransformationRule {
    override val qualifiedTypeName: String get() = SimpleTypeModelStdLib.String.qualifiedTypeName

    override val selfStatement: SelfStatement get() = TODO()

    override fun toString(): String = "leaf as std.String"
}

class SelfTransformationRuleSimple(
    override val qualifiedTypeName: String
) : TransformationRuleAbstract(), ListTransformationRule {

    override val selfStatement: SelfStatement = ExpressionSelfStatementSimple(RootExpressionSimple(RootExpressionSimple.SELF))

    override fun toString(): String = "self"
}

class ListTransformationRuleSimple() : TransformationRuleAbstract(), ListTransformationRule {
    override val qualifiedTypeName: String get() = SimpleTypeModelStdLib.List.type().qualifiedTypeName

    override val selfStatement: SelfStatement = ExpressionSelfStatementSimple(RootExpressionSimple("children"))

    override fun toString(): String = "children as $qualifiedTypeName // list"
}

class SepListItemsTransformationRuleSimple() : TransformationRuleAbstract(), ListTransformationRule {
    override val qualifiedTypeName: String get() = SimpleTypeModelStdLib.List.type().qualifiedTypeName

    override val selfStatement: SelfStatement = ExpressionSelfStatementSimple(
        NavigationSimple(
            start = RootExpressionSimple("children"),
            parts = listOf(PropertyCallSimple("items"))
        )
    )

    override fun toString(): String = "children.items as $qualifiedTypeName // SepList"
}

class CreateTupleTransformationRuleSimple(
    override val qualifiedTypeName: String
) : TransformationRuleAbstract() {

    override val selfStatement: SelfStatement = ConstructTupleSelfStatementSimple(qualifiedTypeName)

    override fun toString(): String = "children as $qualifiedTypeName // Tuple"
}

abstract class TransformationStatementAbstract

abstract class SelfStatementAbstract : TransformationStatementAbstract(), SelfStatement

class ConstructObjectSelfStatementSimple(
    val qualifiedTypeName: String
) : SelfStatementAbstract() {

}

class ConstructTupleSelfStatementSimple(
    val qualifiedTypeName: String
) : SelfStatementAbstract() {

}

class LambdaSelfStatementSimple(
    val qualifiedTypeName: String
) : SelfStatementAbstract() {

}

class ExpressionSelfStatementSimple(
    val expression: Expression
) : SelfStatementAbstract() {

}