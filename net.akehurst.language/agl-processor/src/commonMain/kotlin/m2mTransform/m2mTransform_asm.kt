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
import net.akehurst.language.m2mTransform.api.*
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.api.TypesDomain

class M2mTransformDomainDefault(
    override val name: SimpleName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    namespace: List<M2mTransformNamespace> = emptyList()
) : M2mTransformDomain, DomainAbstract<M2mTransformNamespace, M2MTransformDefinition>(namespace, options) {

    override val allTransformRuleSet: List<M2mTransformRuleSet> get() = allDefinitions.filterIsInstance<M2mTransformRuleSet>()
    override val allTransformTest: List<M2mTransformTest> get() = allDefinitions.filterIsInstance<M2mTransformTest>()

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
) : M2mTransformNamespace, NamespaceAbstract<M2MTransformDefinition>(options, import) {

    override val testDefinition: List<M2mTransformTest>  get() = _testDefinition.values.toList()
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
}

data class M2mTransformRuleSetReferenceDefault(
    override val localNamespace: M2mTransformNamespace,
    override val nameOrQName: PossiblyQualifiedName
) : M2mTransformRuleSetReference {

    override var resolved: M2MTransformDefinition? = null

    override fun resolveAs(resolved: M2MTransformDefinition) {
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
) : M2mTransformRuleSet, DefinitionAbstract<M2MTransformDefinition>() {

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

    override fun setRule(value: M2mTransformRule) {
        (this.rule as MutableMap)[value.name] = value
    }

    override fun resolveDomainParameter(ref: DomainReference, typesDomain: TypesDomain) {
        _domainParameterResolved[ref] = typesDomain
    }
}

data class M2MTransformAbstractRuleDefault(
    override val isTop: Boolean,
    override val name: SimpleName
) : M2mTransformAbstractRule {
    override val primitiveDomains: List<VariableDefinition> = mutableListOf()
    override val domainSignature: Map<DomainReference, DomainSignature> = mutableMapOf()
}

data class M2MTransformRelationDefault(
    override val isTop: Boolean,
    override val name: SimpleName
) : M2MTransformRelation {
    override val primitiveDomains: List<VariableDefinition> = mutableListOf()
    override val domainSignature: Map<DomainReference, DomainSignature> = mutableMapOf()

    override val pivot: Map<SimpleName, VariableDefinition> = mutableMapOf()
    override val domainTemplate: Map<DomainReference, ObjectTemplate> = mutableMapOf()
}

data class M2MTransformMappingDefault(
    override val isTop: Boolean,
    override val name: SimpleName
) : M2MTransformMapping {
    override val primitiveDomains: List<VariableDefinition> = mutableListOf()
    override val domainSignature: Map<DomainReference, DomainSignature> = mutableMapOf()

    override val pivot: Map<SimpleName, VariableDefinition> = mutableMapOf()
    override val domainTemplate: Map<DomainReference, ObjectTemplate> = mutableMapOf()
    override val expression: Map<DomainReference, Expression?> = mutableMapOf()
}

data class M2MTransformTableDefault(
    override val isTop: Boolean,
    override val name: SimpleName
) : M2MTransformTable {
    override val primitiveDomains: List<VariableDefinition> = mutableListOf()
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

data class ObjectTemplateDefault(
    val typeRef: TypeReference,
    override val propertyTemplate: Map<SimpleName, PropertyTemplate>
) : ObjectTemplate {

    override var identifier: SimpleName? = null

    private var _resolvedType: TypeInstance? = null
    override val type: TypeInstance get() = _resolvedType ?: error("Type not resolved for '$this'")

    override fun setIdentifierValue(value: SimpleName) {
        this.identifier = value
    }

    override fun resolveType(tm: TypesDomain) {
        val td = tm.findFirstDefinitionByPossiblyQualifiedNameOrNull(this.typeRef.possiblyQualifiedName)
        _resolvedType = td?.type()
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

}

data class M2mTransformTestCaseDefault(
    override val name: SimpleName
) : M2mTransformTestCase {
    override val domain = mutableMapOf<DomainReference, Expression>()
}