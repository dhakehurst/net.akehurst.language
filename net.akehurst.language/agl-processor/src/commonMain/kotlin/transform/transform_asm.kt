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

package net.akehurst.language.asmTransform.asm

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.agl.simple.ContextFromGrammarAndTypeModel
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.agl.simple.Grammar2TransformRuleSet
import net.akehurst.language.agl.simple.Grammar2TypeModelMapping
import net.akehurst.language.agl.simple.GrammarModel2TransformModel
import net.akehurst.language.api.processor.*
import net.akehurst.language.base.api.*
import net.akehurst.language.base.asm.DefinitionAbstract
import net.akehurst.language.base.asm.ModelAbstract
import net.akehurst.language.base.asm.NamespaceAbstract
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.grammar.api.*
import net.akehurst.language.asmTransform.api.*
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.TypeModelSimple

class AsmTransformDomainDefault(
    override val name: SimpleName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    namespace: List<AsmTransformNamespace> = emptyList()
) : AsmTransformDomain, ModelAbstract<AsmTransformNamespace, AsmTransformRuleSet>(namespace,options) {

    companion object Companion {
        fun fromString(context: ContextWithScope<Any, Any>, transformStr: TransformString): ProcessResult<AsmTransformDomain> {
            val proc = Agl.registry.agl.transform.processor ?: error("Asm-Transform language not found!")
            val res = proc.process(
                sentence = transformStr.value,
                Agl.options {
                    semanticAnalysis { context(context) }
                }
            )
            return when {
                res.allIssues.errors.isEmpty() -> res
                else -> error(res.allIssues.toString())
            }
        }

        fun fromGrammarModel(
            grammarModel: GrammarModel,
            typeModel: TypeModel = TypeModelSimple(grammarModel.allDefinitions.last().name),
            configuration: Grammar2TypeModelMapping? = Grammar2TransformRuleSet.defaultConfiguration
        ): ProcessResult<AsmTransformDomain> {
            val atfg = GrammarModel2TransformModel(typeModel, grammarModel, configuration)
            val trModel = atfg.build()
            return ProcessResultDefault<AsmTransformDomain>(trModel, processIssues=atfg.issues)
        }

    }

    override var typeModel: TypeModel? = null

    override fun findOrCreateNamespace(qualifiedName: QualifiedName, imports: List<Import>): AsmTransformNamespace {
        val existing = findNamespaceOrNull(qualifiedName)
        return when(existing) {
            null -> {
                val ns = AsmTransformNamespaceDefault(qualifiedName = qualifiedName, import = imports)//, imports)
                addNamespace(ns)
                ns
            }
            else -> existing as AsmTransformNamespace
        }
    }

    override fun findTypeForGrammarRule(grammarQualifiedName: QualifiedName, ruleName: GrammarRuleName): TypeInstance? {
        val ruleSet = this.findDefinitionByQualifiedNameOrNull(grammarQualifiedName)
        return ruleSet?.findAllTrRuleForGrammarRuleNamedOrNull(ruleName)?.resolvedType
    }

}

class AsmTransformNamespaceDefault(
    override val qualifiedName: QualifiedName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    import: List<Import> = mutableListOf()
) : AsmTransformNamespace, NamespaceAbstract<AsmTransformRuleSet>(options,import) {

    override fun createOwnedTransformRuleSet(name: SimpleName, extends: List<AsmTransformRuleSetReference>, options: OptionHolder): AsmTransformRuleSet = AsmTransformRuleSetDefault(
        namespace = this,
        name = name,
        argExtends = extends,
        options = options,
        _rules = emptyList()
    )

    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        sb.append("namespace $qualifiedName\n")
        val newIndent = indent.inc
        if (import.isNotEmpty()) {
            val importStr = import
                .sortedBy { it.value }
                .joinToString(separator = "\n") { "${newIndent}import ${it.value}" }
            sb.append(importStr)
            sb.append("\n")
        }
        val defs = definition
            .sortedBy { it.name.value }
            .joinToString(separator = "\n") { "$newIndent${it.asString(newIndent)}" }
        sb.append(defs)
        return sb.toString()
    }
}

data class AsmTransformRuleSetReferenceDefault(
    override val localNamespace: AsmTransformNamespace,
    override val nameOrQName: PossiblyQualifiedName
) : AsmTransformRuleSetReference {
    override var resolved: AsmTransformRuleSet? = null
    override fun resolveAs(resolved: AsmTransformRuleSet) {
        this.resolved = resolved
    }

    override fun cloneTo(ns: AsmTransformNamespace): AsmTransformRuleSetReference {
        return AsmTransformRuleSetReferenceDefault(ns, nameOrQName).also {
            val resolved = this.resolved
            if (null!=resolved) it.resolveAs(resolved)
        }
    }
}

class AsmTransformRuleSetDefault(
    override val namespace: AsmTransformNamespace,
    override val name: SimpleName,
    argExtends: List<AsmTransformRuleSetReference> = emptyList(),
    override val options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    _rules: List<AsmTransformationRule>
) : AsmTransformRuleSet, DefinitionAbstract<AsmTransformRuleSet>() {

    override val extends: List<AsmTransformRuleSetReference> = argExtends.toMutableList() //clone the list so it can be modified

    override val importTypes: List<Import> = mutableListOf()

    override val rules: Map<GrammarRuleName, AsmTransformationRule> = _rules.associateBy(AsmTransformationRule::grammarRuleName).toMutableMap()

    override val createObjectRules: List<CreateObjectRule> get() = rules.values.filterIsInstance<CreateObjectRule>()
    override val modifyObjectRules: List<ModifyObjectRule> get() = rules.values.filterIsInstance<ModifyObjectRule>()

    init {
        namespace.addDefinition(this)
    }

    override fun addImportType(value:Import) {
        if (this.importTypes.contains(value).not()) {
            (this.importTypes as MutableList).add(value)
        }
    }

    override fun findOwnedTrRuleForGrammarRuleNamedOrNull(grmRuleName: GrammarRuleName): AsmTransformationRule? =
        rules[grmRuleName]

    override fun findAllTrRuleForGrammarRuleNamedOrNull(grmRuleName: GrammarRuleName): AsmTransformationRule? {
        return findOwnedTrRuleForGrammarRuleNamedOrNull(grmRuleName)
            ?: this.extends.firstNotNullOfOrNull {
                val res = it.resolved ?: error("Should be resolved!")
                res.findAllTrRuleForGrammarRuleNamedOrNull(grmRuleName)
            }
    }

    override fun cloneTo(ns: AsmTransformNamespace): AsmTransformRuleSet {
        val rrs = AsmTransformRuleSetDefault(
            namespace = ns,
            name = this.name,
            argExtends = this.extends.map { it.cloneTo(ns) },
            options = this.options,
            _rules = this.rules.values.toMutableList()//should clone content, rules should be ok to share, they do not ref the RuleSet and are immutable
        )
        this.importTypes.forEach { rrs.addImportType(it) }
        return rrs
    }

    fun addExtends(other:AsmTransformRuleSetReference) {
        (this.extends as MutableList).add(other)
    }

    override fun setRule(rule: AsmTransformationRule) {
        (rules as MutableMap)[rule.grammarRuleName] = rule
    }

    override fun asString(indent: Indent): String {
        val sb = StringBuilder()
        val extStr = when {
            this.extends.isEmpty() -> " "
            else -> {
                val refsStr = extends.map {
                    when {
                        it.localNamespace == this.namespace -> it.nameOrQName.simpleName.value
                        else -> it.nameOrQName.value
                    }
                }
                " : ${refsStr.joinToString(separator = ", ") { it }}" //TODO import affect!
            }
        }
        sb.append("transform $name$extStr{\n")
        val newIndent = indent.inc
        if (importTypes.isNotEmpty()) {
            val importStr = importTypes
                .sortedBy { it.value }
                .joinToString(separator = "\n") { "${newIndent}import-types ${it.value}" }
            sb.append(importStr)
            sb.append("\n")
        }
        val rulesStr = rules
            .entries.sortedBy {
                it.key.value
            }.map {
                it.value.asString(newIndent, importTypes)
            }.joinToString(separator = "\n")
        sb.append(rulesStr)
        sb.append("\n${indent}}")
        return sb.toString()
    }

    override fun toString(): String  = "transform ${this.qualifiedName.value} {...}"
}

class AsmTransformationRuleDefault(
    override val expression: Expression
) : AsmTransformationRule {

    override var grammarRuleName: GrammarRuleName = GrammarRuleName("<unset>")
    override val isResolved: Boolean get() = null!=this._resolvedType
    override val resolvedType: TypeInstance get() = _resolvedType ?: error("TransformationRule for '${grammarRuleName.value}' has not been resolved")

    private var _resolvedType: TypeInstance? = null

    override fun resolveTypeAs(type: TypeInstance) {
        _resolvedType = type
    }

    override fun asString(indent: Indent, imports: List<Import>): String {
        return "$indent${grammarRuleName}: ${expression.asString(indent, imports)}"
    }

    override fun toString(): String  = "${grammarRuleName.value}: ${expression}"
}

internal fun asmTransformationRule(type: TypeInstance, expression: Expression): AsmTransformationRuleDefault {
    return AsmTransformationRuleDefault(
        expression
    ).also {
        it.resolveTypeAs(type)
    }
}

internal abstract class AsmTransformationStatementAbstract

internal abstract class SelfStatementAbstract : AsmTransformationStatementAbstract(), SelfStatement
/*
internal class LambdaSelfStatementSimple(
    val qualifiedTypeName: String
) : SelfStatementAbstract() {
    override fun toString(): String = "{ -> }"
}
*/
internal class ExpressionSelfStatementSimple(
    val expression: Expression
) : SelfStatementAbstract() {
    override fun toString(): String = "$expression"
}