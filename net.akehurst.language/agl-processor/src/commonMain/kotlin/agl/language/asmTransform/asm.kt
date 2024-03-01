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
import net.akehurst.language.agl.language.expressions.IndexOperationDefault
import net.akehurst.language.agl.language.expressions.LiteralExpressionDefault
import net.akehurst.language.agl.language.expressions.NavigationDefault
import net.akehurst.language.agl.language.expressions.RootExpressionDefault
import net.akehurst.language.agl.language.grammar.ContextFromGrammar
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.api.language.asmTransform.*
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.language.grammar.Grammar
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.typemodel.api.PropertyDeclaration
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
}

abstract class TransformationRuleAbstract : TransformationRule {

    companion object {
        val CHILD_0 = ExpressionSelfStatementSimple(
            NavigationDefault(
                start = RootExpressionDefault("child"),
                parts = listOf(IndexOperationDefault(listOf(LiteralExpressionDefault(LiteralExpressionDefault.INTEGER, 0))))
            )
        )
    }

    override lateinit var grammarRuleName: String
    override val resolvedType: TypeInstance get() = _resolvedType

    private lateinit var _resolvedType: TypeInstance

    fun resolveTypeAs(type: TypeInstance) {
        _resolvedType = type
    }

    override val modifyStatements = mutableListOf<AssignmentTransformationStatement>()

    fun appendAssignment(lhsPropertyName: String, rhs: Expression) {
        val ass = AssignmentTransformationStatementSimple(lhsPropertyName, rhs)
        modifyStatements.add(ass)
    }

    abstract override fun toString(): String
}

class CreateObjectRuleSimple(
    override val typeName: String
) : TransformationRuleAbstract(), CreateObjectRule {

    override val selfStatement: SelfStatement = ConstructSelfStatementSimple(typeName)

    override fun toString(): String = "$typeName { ... }"
}

class ModifyObjectRuleSimple(
    override val typeName: String
) : TransformationRuleAbstract(), ModifyObjectRule {

    override val selfStatement: SelfStatement = LambdaSelfStatementSimple(typeName)

    override fun toString(): String = "{ $typeName  -> ... }"
}

class SubtypeTransformationRuleSimple(
    override val typeName: String
) : TransformationRuleAbstract(), SubtypeTransformationRule {

    override val selfStatement: SelfStatement = CHILD_0

    override fun toString(): String = "child[0] as $typeName //subtype"
}

class UnnamedSubtypeTransformationRuleSimple() : TransformationRuleAbstract(), SubtypeTransformationRule {

    override val typeName: String get() = UnnamedSupertypeTypeSimple.NAME

    override val selfStatement: SelfStatement = TransformationRuleAbstract.CHILD_0

    override fun toString(): String = "child[0] as $typeName //UnnamedSubtype"
}

class NothingTransformationRuleSimple() : TransformationRuleAbstract(), NoActionTransformationRule {

    init {
        super.resolveTypeAs(SimpleTypeModelStdLib.NothingType)
    }

    override val typeName: String get() = SimpleTypeModelStdLib.NothingType.qualifiedTypeName
    override val selfStatement: SelfStatement = ExpressionSelfStatementSimple(RootExpressionDefault(RootExpressionDefault.NOTHING))

    override fun toString(): String = "\$nothing //no action"
}

class OptionalItemTransformationRuleSimple(
    override val typeName: String
) : TransformationRuleAbstract(), NoActionTransformationRule {

    override val selfStatement: SelfStatement = CHILD_0

    override fun toString(): String = "child[0] as $typeName // optional"
}

class Child0AsStringTransformationRuleSimple() : TransformationRuleAbstract(), SelfAssignChild0TransformationRule {
    override val typeName: String get() = SimpleTypeModelStdLib.String.qualifiedTypeName

    override val selfStatement: SelfStatement = CHILD_0

    override fun toString(): String = "child[0] as std.String // self-assign"
}

class ListTransformationRuleSimple() : TransformationRuleAbstract(), ListTransformationRule {
    override val typeName: String get() = SimpleTypeModelStdLib.List.type().qualifiedTypeName

    override val selfStatement: SelfStatement = ExpressionSelfStatementSimple(RootExpressionDefault("children"))

    override fun toString(): String = "children as $typeName // list"
}

abstract class TransformationStatementAbstract

abstract class SelfStatementAbstract : TransformationStatementAbstract(), SelfStatement

class ConstructSelfStatementSimple(
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

class AssignmentTransformationStatementSimple(
    override val lhsPropertyName: String,
    override val rhs: Expression
) : TransformationStatementAbstract(), AssignmentTransformationStatement {

    val resolvedLhs get() = _resolvedLhs

    private lateinit var _resolvedLhs: PropertyDeclaration

    fun resolveLhsAs(propertyDeclaration: PropertyDeclaration) {
        _resolvedLhs = propertyDeclaration
    }

}