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
import kotlin.collections.set

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

}

class M2mTransformNamespaceDefault(
    override val qualifiedName: QualifiedName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    import: List<Import> = mutableListOf()
) : M2mTransformNamespace, NamespaceAbstract<M2MTransformDefinition>(options, import) {
    override fun createOwnedTransformRuleSet(
        name: SimpleName,
        extends: List<M2MTransformDefinition>,
        options: OptionHolder
    ): M2mTransformRuleSet {
        TODO("not implemented")
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

    init {
        namespace.addDefinition(this)
    }

    override fun addImportType(value: Import) {
        if (this.importTypes.contains(value).not()) {
            (this.importTypes as MutableList).add(value)
        }
    }

    override fun setRule(value: M2mTransformRule) {
        (this.rule as MutableMap)[value.name] = value
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

    override fun setIdentifier(value: SimpleName) {
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
    override fun setIdentifier(value: SimpleName) {
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
    override fun setIdentifier(value: SimpleName) {
        this.identifier = value
    }
}


data class M2MTransformTestDefault(
    override val namespace: M2mTransformNamespace,
    override val name: SimpleName,
    override val domainParameters: Map<DomainReference, SimpleName>,
    override val options: OptionHolder = OptionHolderDefault(null, emptyMap()),
) : M2mTransformTest, DefinitionAbstract<M2MTransformDefinition>() {

    override val domain: Map<DomainReference, Expression> = mutableMapOf()
}
