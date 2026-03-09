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

package net.akehurst.language.m2mTransform.asm

import net.akehurst.language.base.api.*
import net.akehurst.language.base.asm.DefinitionAbstract
import net.akehurst.language.base.asm.DomainAbstract
import net.akehurst.language.base.asm.NamespaceAbstract
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.TypeReference
import net.akehurst.language.expressions.asm.TypeReferenceDefault
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.m2mTransform.api.*
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.TypesDomain
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.exp

class M2mTransformDomainDefault(
    override val name: SimpleName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    namespace: List<M2mTransformNamespace> = emptyList()
) : M2mTransformDomain, DomainAbstract<M2mTransformNamespace, M2mTransformRuleSet>(namespace, options) {

    override val allTransformRuleSet: List<M2mTransformRuleSet> get() = allDefinitions
    override val allTransformTest: List<M2mTransformTest> get() = namespace.flatMap { it.testDefinition }

    override fun findOrCreateNamespace(qualifiedName: QualifiedName, imports: List<Import>): M2mTransformNamespace {
        TODO("not implemented")
    }

    override fun findTestDefinitionByQualifiedNameOrNull(qualifiedName: QualifiedName): M2mTransformTest? {
        return (findNamespaceOrNull(qualifiedName.front) as? M2mTransformNamespace)?.findOwnedTestDefinitionOrNull(qualifiedName.last)
    }
}

class M2mTransformNamespaceDefault(
    override val qualifiedName: QualifiedName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    import: List<Import> = mutableListOf()
) : M2mTransformNamespace, NamespaceAbstract<M2mTransformRuleSet>(options, import) {

    override val testDefinition: List<M2mTransformTest> get() = _testDefinition.values.toList()
    override val testDefinitionByName: Map<SimpleName, M2mTransformTest> get() = _testDefinition

    protected val _testDefinition = linkedMapOf<SimpleName, M2mTransformTest>() //order is important

    override fun createOwnedTransformRuleSet(
        name: SimpleName,
        extends: List<M2MTransformDefinition>,
        options: OptionHolder
    ): M2mTransformRuleSet {
        TODO("not implemented")
    }

    override fun findOwnedTestDefinitionOrNull(simpleName: SimpleName): M2mTransformTest? {
        return _testDefinition[simpleName]
    }

    override fun addTestDefinition(value: M2mTransformTest) {
        _testDefinition[value.name] = value
    }

    override fun merge(value: Namespace<M2mTransformRuleSet>) {
        value.definition.forEach {
            if (_definition.containsKey(it.name)) {
                _definition[it.name]!!.merge(it)
            } else {
                addDefinition(it)
            }
        }

        (value as M2mTransformNamespace).testDefinition.forEach {
            if (_testDefinition.containsKey(it.name)) {
                _testDefinition[it.name]!!.merge(it)
            } else {
                addTestDefinition(it)
            }
        }
    }
}

data class M2mTransformRuleSetReferenceDefault(
    override val localNamespace: M2mTransformNamespace,
    override val nameOrQName: PossiblyQualifiedName
) : M2mTransformRuleSetReference {

    override var resolved: M2mTransformRuleSet? = null

    override fun resolveAs(resolved: M2mTransformRuleSet) {
        this.resolved = resolved
    }

    override fun cloneTo(ns: M2mTransformNamespace): M2mTransformRuleSetReference {
        return M2mTransformRuleSetReferenceDefault(ns, nameOrQName).also {
            val resolved = this.resolved
            if (null != resolved) it.resolveAs(resolved)
        }
    }
}

class M2mTransformRuleSetDefault(
    override val namespace: M2mTransformNamespace,
    override val name: SimpleName,
    override val domainParameters: Map<DomainReference, SimpleName>,
    argExtends: List<M2mTransformRuleSetReference> = emptyList(),
    override val options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    _rules: List<M2mTransformRule>
) : M2mTransformRuleSet, DefinitionAbstract<M2mTransformRuleSet>() {

    override val extends: List<M2mTransformRuleSetReference> = mutableListOf()
    override val importTypes: List<Import> = mutableListOf()
    override val topRule: List<M2mTransformRule> get() = rule.values.filter { it.isTop }
    override val rule: Map<SimpleName, M2mTransformRule> = mutableMapOf()
    override val domainParameterResolved: Map<DomainReference, TypesDomain> get() = _domainParameterResolved

    init {
        namespace.addDefinition(this)
    }

    private val _domainParameterResolved = mutableMapOf<DomainReference, TypesDomain>()

    override fun addImportType(value: Import) {
        if (this.importTypes.contains(value).not()) {
            (this.importTypes as MutableList).add(value)
        }
    }

    override fun setRule(rule: M2mTransformRule) {
        (this.rule as MutableMap)[rule.name] = rule
    }

    override fun resolveDomainParameter(ref: DomainReference, typesDomain: TypesDomain) {
        _domainParameterResolved[ref] = typesDomain
    }

    override fun merge(value: M2mTransformRuleSet) {
        check(this.domainParameters == value.domainParameters) { "Domain parameters must match" }
        (extends as MutableList).addAll(value.extends)
        (importTypes as MutableList).addAll(value.importTypes)
        value.rule.forEach {
            if (this.rule.containsKey(it.key)) {
                error("M2mTransformRuleSet '${qualifiedName}' already contains a rule named '${it.key}', cannot merge another")
            } else {
                setRule(it.value)
            }
        }
    }
}

data class M2mTransformRuleReferenceDefault(
    override val nameOrQName: PossiblyQualifiedName
) : M2mTransformRuleReference {
    override var resolved: M2mTransformRule? = null

    override fun resolveAs(resolved: M2mTransformRule) {
        this.resolved = resolved
    }
}

abstract class M2mTransformRuleAbstract(
) : M2mTransformRule {
    override val parameters: List<VariableDefinition> = mutableListOf()
    override val extends: List<M2mTransformRuleReference> = mutableListOf()

    override fun conformsTo(other: M2mTransformRule): Boolean = when {
        other === this -> true
        other == this -> true
        else -> this.extends.any { it.resolved?.conformsTo(other) ?: error("M2mTransformRule '${this.name}' is not Resolved") }
    }
}

data class M2MTransformAbstractRuleDefault(
    override val isTop: Boolean,
    override val name: SimpleName
) : M2mTransformAbstractRule, M2mTransformRuleAbstract() {
    override val domainSignature: Map<DomainReference, DomainSignature> = mutableMapOf()
}

data class M2MTransformRelationDefault(
    override val isTop: Boolean,
    override val name: SimpleName
) : M2MTransformRelation, M2mTransformRuleAbstract() {
    override val domainSignature: Map<DomainReference, DomainSignature> = mutableMapOf()

    override val pivot: Map<SimpleName, VariableDefinition> = mutableMapOf()
    override val domainTemplate: Map<DomainReference, ObjectTemplate> = mutableMapOf()

    override var when_: Expression? = null
    override val where = mutableListOf<RuleWhere>()
}

data class M2MTransformMappingDefault(
    override val isTop: Boolean,
    override val name: SimpleName
) : M2MTransformMapping, M2mTransformRuleAbstract() {
    override val domainSignature: Map<DomainReference, DomainSignature> = mutableMapOf()

    override val pivot: Map<SimpleName, VariableDefinition> = mutableMapOf()
    override val domainTemplate: Map<DomainReference, ObjectTemplate> = mutableMapOf()
    override val expression: Map<DomainReference, Expression?> = mutableMapOf()

    override var when_: Expression? = null
    override val where = mutableListOf<RuleWhere>()
}

data class M2MTransformTableDefault(
    override val isTop: Boolean,
    override val name: SimpleName
) : M2MTransformTable, M2mTransformRuleAbstract() {
    override val domainSignature: Map<DomainReference, DomainSignature> = mutableMapOf()
    override val values: List<Map<DomainReference, Expression>> = mutableListOf()
}

data class DomainSignatureDefault(
    override val domainRef: DomainReference,
    override val variable: VariableDefinition
) : DomainSignature {

}

data class VariableDefinitionDefault(
    override val name: SimpleName,
    override val typeRef: TypeReference,
) : VariableDefinition {
    private var _resolvedType: TypeInstance? = null

    override val type: TypeInstance get() = _resolvedType ?: error("Type not resolved for '$this'")

    override fun resolveType(tm: TypesDomain) {
        val td = tm.findFirstDefinitionByPossiblyQualifiedNameOrNull(this.typeRef.possiblyQualifiedName) //TODO typeargs
        _resolvedType = td?.type()
    }
}


abstract class RuleCallAbstract(
    override val ruleName: SimpleName,
    override val ruleArguments: Map<SimpleName, Expression>,
    override val domainArguments: Map<DomainReference, Expression>,
) : RuleCall {
    override var resolved: M2mTransformRule? = null

    override fun resolveAs(resolved: M2mTransformRule) {
        this.resolved = resolved
    }
}

class RuleWhenRelationHoldsDefault(
    ruleName: SimpleName,
    ruleArguments: Map<SimpleName, Expression>,
    domainArguments: Map<DomainReference, Expression>,
) : RuleWhenRelationHolds, Expression, RuleCallAbstract(ruleName, ruleArguments, domainArguments) {

    override fun asString(indent: Indent, imports: List<Import>): String = "related ${ruleName.value}(...)"
}

class RuleWhenRelationHoldsForAllDefault(
     ruleName: SimpleName,
     ruleArguments: Map<SimpleName, Expression>,
    domainArguments: Map<DomainReference, Expression>,
) : RuleWhenRelationHoldsForAll, Expression, RuleCallAbstract(ruleName, ruleArguments, domainArguments) {

    override fun asString(indent: Indent, imports: List<Import>): String = "related all ${ruleName.value}(...)"
}

class RuleWhenMappingHoldsDefault(
    ruleName: SimpleName,
    ruleArguments: Map<SimpleName, Expression>,
    domainArguments: Map<DomainReference, Expression>,
) : RuleWhenMappingHolds, Expression, RuleCallAbstract(ruleName, ruleArguments, domainArguments) {

    override fun asString(indent: Indent, imports: List<Import>): String = "mapped ${ruleName.value}(...)"
}

class RuleWhenMappingHoldsForAllDefault(
    ruleName: SimpleName,
    ruleArguments: Map<SimpleName, Expression>,
    domainArguments: Map<DomainReference, Expression>,
) : RuleWhenMappingHoldsForAll, Expression, RuleCallAbstract(ruleName, ruleArguments, domainArguments) {

    override fun asString(indent: Indent, imports: List<Import>): String = "mapped all ${ruleName.value}(...)"
}

class RuleWhereCallRelationDefault(
    override val ruleName: SimpleName,
    override val ruleArguments: Map<SimpleName, Expression>,
    override val domainArguments: Map<DomainReference, Expression>,
) : RuleWhereCallRelation, Expression, RuleCallAbstract(ruleName, ruleArguments, domainArguments) {

    override fun asString(indent: Indent, imports: List<Import>): String = "relate ${ruleName.value}(...)"
}

class RuleWhereCallRelationForAllDefault(
    ruleName: SimpleName,
    ruleArguments: Map<SimpleName, Expression>,
    domainArguments: Map<DomainReference, Expression>,
) : RuleWhereCallRelationForAll, Expression, RuleCallAbstract(ruleName, ruleArguments, domainArguments) {

    override fun asString(indent: Indent, imports: List<Import>): String = "relate all ${ruleName.value}(...)"
}

class RuleWhereCallMappingDefault(
    ruleName: SimpleName,
    ruleArguments: Map<SimpleName, Expression>,
    domainArguments: Map<DomainReference, Expression>,
) : RuleWhereCallMapping, Expression, RuleCallAbstract(ruleName, ruleArguments, domainArguments) {

    override fun asString(indent: Indent, imports: List<Import>): String = "map ${ruleName.value}(...)"
}

class RuleWhereCallMappingForAllDefault(
    ruleName: SimpleName,
    ruleArguments: Map<SimpleName, Expression>,
    domainArguments: Map<DomainReference, Expression>,
) : RuleWhereCallMappingForAll, Expression, RuleCallAbstract(ruleName, ruleArguments, domainArguments) {

    override fun asString(indent: Indent, imports: List<Import>): String = "map all ${ruleName.value}(...)"
}


data class ObjectTemplateDefault(
    val typeRef: TypeReference,
    override val propertyTemplate: Map<SimpleName, PropertyTemplate>
) : ObjectTemplate {

    // used when type is known
    constructor(type: TypeInstance, propertyTemplate: Map<SimpleName, PropertyTemplate>) : this(TypeReferenceDefault(type.qualifiedTypeName,emptyList(),type.isNullable),propertyTemplate) {
        _resolvedType = type
    }

    override var identifier: SimpleName? = null

    private var _resolvedType: TypeInstance? = null
    override val type: TypeInstance get() = _resolvedType ?: error("Type not resolved for '$this'")

    override fun setIdentifierValue(value: SimpleName) {
        this.identifier = value
    }

    override fun resolveTypes(tm: TypesDomain): List<LanguageIssue> {
        val td = tm.findFirstDefinitionByPossiblyQualifiedNameOrNull(this.typeRef.possiblyQualifiedName)
        val issues = if (null == td) {
            val msg = "In ObjectTemplate, cannot resolveType '${this.typeRef.possiblyQualifiedName.value}' in TypesDomain '${tm.name.value}'."
            listOf(LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS, null, msg, null))
        } else {
            _resolvedType = td.type()
            emptyList()
        }
        return issues + propertyTemplate.values.flatMap { it.rhs.resolveTypes(tm) }
    }

    override fun asString(indent: Indent): String {
        val id = identifier?.let { "${it.value}:" } ?: ""
        val propIndent = indent.inc
        val props = propertyTemplate.entries.joinToString(separator = "\n") { (k,v) -> "$propIndent${k.value}==${v.rhs.asString(propIndent)}" }
        return when {
            propertyTemplate.isEmpty() -> "$id$type { }"
            else -> """$id$type {
            $props
            $indent}
            """.trimIndent()
        }
    }
}

data class CollectionTemplateDefault(
    override val isSubset: Boolean,
    override val elements: List<PropertyTemplateRhs>
) : CollectionTemplate {
    override var identifier: SimpleName? = null
    override fun setIdentifierValue(value: SimpleName) {
        this.identifier = value
    }

    override fun resolveTypes(tm: TypesDomain): List<LanguageIssue> {
        return elements.flatMap { it.resolveTypes(tm) }
    }
    override fun asString(indent: Indent): String {
        val id = identifier?.let { "${it.value}:" } ?: ""
        val elIndent = indent.inc
        val elems = elements.joinToString(separator = "\n") { el -> "$elIndent${el.asString(elIndent)}" }
        return when{
            elements.isEmpty() -> "$id[ ]"
            else -> """$id[
            $elems
            $indent]
            """.trimIndent()
        }
    }
}

data class PropertyTemplateDefault(
    override val propertyName: SimpleName,
    override val rhs: PropertyTemplateRhs
) : PropertyTemplate {

}

class PropertyTemplateExpressionDefault(
    override val expression: Expression
) : PropertyTemplateExpression {
    override var identifier: SimpleName? = null
    override fun setIdentifierValue(value: SimpleName) {
        this.identifier = value
    }

    override fun resolveTypes(tm: TypesDomain): List<LanguageIssue> {
        //TODO:
        return emptyList()
    }

    override fun asString(indent: Indent): String {
        val id = identifier?.let { "${it.value}:" } ?: ""
        val exp = expression.asString(indent)
        return "$id$exp"
    }
}


data class M2MTransformTestDefault(
    override val namespace: M2mTransformNamespace,
    override val name: SimpleName,
    override val domainParameters: Map<DomainReference, SimpleName>,
    override val options: OptionHolder = OptionHolderDefault(null, emptyMap()),
) : M2mTransformTest {

    init {
        namespace.addTestDefinition(this)
    }

    override val qualifiedName: QualifiedName get() = namespace.qualifiedName.append(this.name)
    override val testCase = mutableMapOf<SimpleName, M2mTransformTestCase>()

    override fun merge(value: M2mTransformTest) {
        check(this.domainParameters == value.domainParameters) { "Domain parameters must match" }
        // (extends as MutableList).addAll(value.extends)
        // (importTypes as MutableList).addAll(value.importTypes)
        value.testCase.forEach {
            if (this.testCase.containsKey(it.key)) {
                error("M2mTransformTest '${qualifiedName}' already contains a test-case named '${it.key}', cannot merge another")
            } else {
                testCase[it.key] = (it.value)
            }
        }
    }
}

data class M2mTransformTestCaseDefault(
    override val name: SimpleName
) : M2mTransformTestCase {
    override val domain = mutableMapOf<DomainReference, Expression>()
}