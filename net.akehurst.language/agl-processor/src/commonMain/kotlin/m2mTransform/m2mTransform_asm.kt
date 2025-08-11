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

import net.akehurst.language.asmTransform.api.AsmTransformRuleSet
import net.akehurst.language.asmTransform.api.AsmTransformationRule
import net.akehurst.language.base.api.*
import net.akehurst.language.base.asm.DefinitionAbstract
import net.akehurst.language.base.asm.ModelAbstract
import net.akehurst.language.base.asm.NamespaceAbstract
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.m2mTransform.api.*
import net.akehurst.language.m2mTransform.asm.ObjectPatternDefault
import net.akehurst.language.m2mTransform.asm.PropertyPatternExpressionDefault
import net.akehurst.language.typemodel.api.TypeInstance
import net.akehurst.language.typemodel.api.TypeModel
import kotlin.collections.set

class M2mTransformDomainDefault(
    override val name: SimpleName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    namespace: List<M2mTransformNamespace> = emptyList()
) : M2mTransformDomain, ModelAbstract<M2mTransformNamespace, M2mTransformRuleSet>(namespace, options) {

    override fun findOrCreateNamespace(qualifiedName: QualifiedName, imports: List<Import>): M2mTransformNamespace {
        TODO("not implemented")
    }

}

class M2mTransformNamespaceDefault(
    override val qualifiedName: QualifiedName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    import: List<Import> = mutableListOf()
) : M2mTransformNamespace, NamespaceAbstract<M2mTransformRuleSet>(options, import) {
    override fun createOwnedTransformRuleSet(
        name: SimpleName,
        extends: List<M2mTransformRuleSetReference>,
        options: OptionHolder
    ): M2mTransformRuleSet {
        TODO("not implemented")
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

data class M2mRelationDefault(
    override val isTop: Boolean,
    override val name: SimpleName
) : M2mRelation {
    override val pivot: Map<SimpleName, VariableDefinition> = mutableMapOf()
    override val domainItem: Map<DomainReference, DomainItem> = mutableMapOf()
    override val objectPattern: Map<DomainReference, ObjectPattern> = mutableMapOf()
}

data class M2mMappingDefault(
    override val isTop: Boolean,
    override val name: SimpleName
) : M2mMapping {
    override val domainItem: Map<DomainReference, DomainItem> = mutableMapOf()
    override val expression: Map<DomainReference, Expression> = mutableMapOf()
}

data class DomainItemDefault(
    override val domainRef: DomainReference,
    override val variable: VariableDefinition
) : DomainItem {

}

data class VariableDefinitionDefault(
    override val name: SimpleName,
    val typeRef: PossiblyQualifiedName,
) : VariableDefinition {
    private var _resolvedType: TypeInstance? = null

    override val type: TypeInstance get() = _resolvedType ?: error("Type not resolved for '$this'")

    override fun resolveType(tm: TypeModel) {
        val td = tm.findFirstDefinitionByPossiblyQualifiedNameOrNull(this.typeRef)
        _resolvedType = td?.type()
    }
}

data class ObjectPatternDefault(
    val typeRef: PossiblyQualifiedName,
    override val propertyPattern: Map<SimpleName, PropertyPattern>
) : ObjectPattern {

    override var identifier: SimpleName? = null

    private var _resolvedType: TypeInstance? = null
    override val type: TypeInstance get() = _resolvedType ?: error("Type not resolved for '$this'")

    override fun setIdentifier(value: SimpleName) {
        this.identifier = value
    }

    override fun resolveType(tm: TypeModel) {
        val td = tm.findFirstDefinitionByPossiblyQualifiedNameOrNull(this.typeRef)
        _resolvedType = td?.type()
    }
}

data class PropertyPatternDefault(
    override val propertyName: SimpleName,
    override val rhs: PropertyPatternRhs
) : PropertyPattern {

}

class PropertyPatternExpressionDefault(
    override val expression: Expression
) : PropertyPatternExpression