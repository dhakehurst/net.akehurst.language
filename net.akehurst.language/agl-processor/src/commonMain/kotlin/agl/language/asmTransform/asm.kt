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
import net.akehurst.language.agl.language.grammar.ContextFromGrammar
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.api.language.asmTransform.*
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.language.grammar.Grammar
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.typemodel.api.PropertyDeclaration
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.simple.TypeModelSimple


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

        fun fromGrammar(grammar: Grammar, configuration: Grammar2TypeModelMapping? = TypeModelFromGrammar.defaultConfiguration): ProcessResult<List<AsmTransformModel>> {
            val grmrTypeModel = TypeModelSimple(grammar.name)
            val atfg = GrammarNamespaceAndAsmTransformBuilderFromGrammar(grmrTypeModel, grammar, configuration)
            atfg.build()
            return ProcessResultDefault<List<AsmTransformModel>>(listOf(atfg.transformModel), atfg.issues)
        }
    }

    override val name: String get() = this.qualifiedName.split(".").last()

    override val rules get() = _rules

    override val createObjectRules: List<CreateObjectRule> get() = rules.values.filterIsInstance<CreateObjectRule>()
    override val modifyObjectRules: List<ModifyObjectRule> get() = rules.values.filterIsInstance<ModifyObjectRule>()

    // GrammarRuleName -> TransformationRule
    private val _rules = mutableMapOf<String, TransformationRule>()

    fun addRule(tr: TransformationRule) {
        _rules[tr.grammarRuleName] = tr
    }
}

abstract class TransformationRuleAbstract : TransformationRule {

    override lateinit var grammarRuleName: String
    override val resolvedType: TypeInstance get() = _resolvedType

    private lateinit var _resolvedType: TypeInstance

    fun resolveTypeAs(type: TypeInstance) {
        _resolvedType = type
    }
}

class CreateObjectRuleSimple(
    override val typeName: String
) : TransformationRuleAbstract(), CreateObjectRule {

    override val modifyStatements = mutableListOf<AssignmentTransformationStatement>()

    fun appendAssignment(lhsPropertyName: String, rhs: Expression) {
        val ass = AssignmentTransformationStatementSimple(lhsPropertyName, rhs)
        modifyStatements.add(ass)
    }

}

class ModifyObjectRuleSimple(
    override val typeName: String
) : TransformationRuleAbstract(), ModifyObjectRule {

    override val modifyStatements = mutableListOf<AssignmentTransformationStatement>()

    fun appendAssignment(lhsPropertyName: String, rhs: Expression) {
        val ass = AssignmentTransformationStatementSimple(lhsPropertyName, rhs)
        modifyStatements.add(ass)
    }

}

class SubtypeTransformationRuleSimple(
    override val typeName: String
) : TransformationRuleAbstract(), SubtypeTransformationRule {

}

class NoActionTransformationRuleSimple(
    override val typeName: String
) : TransformationRuleAbstract(), NoActionTransformationRule {

}

class StringActionTransformationRuleSimple() : TransformationRuleAbstract() {
    override val typeName: String get() = SimpleTypeModelStdLib.String.qualifiedTypeName
}

abstract class TransformationStatementAbstract

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