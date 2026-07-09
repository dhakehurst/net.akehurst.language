/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.m2mTransform.builder

import net.akehurst.language.agl.processor.SemanticAnalysisOptionsDefault
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.TypeReference
import net.akehurst.language.expressions.asm.TypeReferenceDefault
import net.akehurst.language.regex.api.UnescapedPattern
import net.akehurst.language.m2mTransform.api.*
import net.akehurst.language.m2mTransform.asm.*
import net.akehurst.language.m2mTransform.processor.M2mTransformSemanticAnalyser
import kotlin.collections.set

@DslMarker
annotation class M2mModelDslMarker

fun m2mDomain(name: String, sentenceContext: SentenceContext, init: M2mDomainBuilder.() -> Unit): M2mTransformDomain {
    val b = M2mDomainBuilder(SimpleName(name))
    b.init()
    val styles = b.build()

    val sa = M2mTransformSemanticAnalyser()
    val opts = SemanticAnalysisOptionsDefault(sentenceContext = sentenceContext)
    sa.analyse(null, styles, LocationMapDefault(), opts)

    return styles
}


@M2mModelDslMarker
class M2mDomainBuilder(
    private val _name: SimpleName
) {

    private val _namespaces = mutableListOf<M2mTransformNamespace>()

    fun namespace(qualifiedName: String, init: M2mTransformNamespaceBuilder.() -> Unit) {
        val b = M2mTransformNamespaceBuilder(QualifiedName(qualifiedName))
        b.init()
        _namespaces.add(b.build())
    }

    fun build(): M2mTransformDomain = M2mTransformDomainDefault(_name).also { mdl ->
        _namespaces.forEach { namespace -> mdl.addNamespace(namespace) }
    }
}

@M2mModelDslMarker
class M2mTransformNamespaceBuilder internal constructor(
    qualifiedName: QualifiedName
) {
    private val _namespace = M2mTransformNamespaceDefault(qualifiedName)

    fun rules(name: String, init: RuleSetBuilder.() -> Unit) {
        val b = RuleSetBuilder(_namespace, SimpleName(name))
        b.init()
        b.build()
    }

    fun build() = _namespace
}

@M2mModelDslMarker
class RuleSetBuilder internal constructor(
    private val _namespace: M2mTransformNamespace,
    private val _name: SimpleName
) {

    private val _extends = mutableListOf<M2mTransformRuleSetReference>()
    private val _domainParams = mutableMapOf<DomainReference, SimpleName>()
    private val _rules = mutableListOf<M2mTransformRule>()

    fun extends(ruleSetName: String) {
        _extends.add(M2mTransformRuleSetReferenceDefault(_namespace, ruleSetName.asPossiblyQualifiedName))
    }

    fun domainParameter(domainReference: String, domainName: String) {
        _domainParams[DomainReference(domainReference)] = SimpleName(domainName)
    }

    fun table(name: String, init: M2MTransformTableBuilder.() -> Unit) {
        val b = M2MTransformTableBuilder(SimpleName(name))
        b.init()
        val rule = b.build()
        _rules.add(rule)
    }

    fun mapping(name: String, init: M2MTransformMappingBuilder.() -> Unit) {
        val b = M2MTransformMappingBuilder(SimpleName(name))
        b.init()
        val rule = b.build()
        _rules.add(rule)

    }

    fun relation(name: String, init: M2MTransformRelationBuilder.() -> Unit) {
        val b = M2MTransformRelationBuilder(SimpleName(name))
        b.init()
        val rule = b.build()
        _rules.add(rule)
    }

    fun build() = M2mTransformRuleSetDefault(_namespace, _name, _domainParams, _extends).also { rs ->
        _rules.forEach { rs.setRule(it) }
    }
}

@M2mModelDslMarker
class M2MTransformTableBuilder internal constructor(
    private val _name: SimpleName
) {
    private var _isTop: Boolean = false
    private val _parameters = mutableListOf<VariableDefinition>()
    private val _extends = mutableListOf<M2mTransformRuleReference>()
    private val _sigDomains = mutableListOf<DomainSignature>()
    private val _values = mutableListOf<List<Expression>>()

    fun isTop() {
        _isTop = true
    }

    fun parameter(name: String, typeName: String) {
        val tr = TypeReferenceDefault(typeName.asPossiblyQualifiedName, emptyList(), false)
        val vd = VariableDefinitionDefault(SimpleName(name), tr)
        _parameters.add(vd)
    }

    fun signatures(domainReference: String, name: String, typeName: String) { //TODO handle type args - via typeArgBuilder
        val dr = DomainReference(domainReference)
        val tr = TypeReferenceDefault(typeName.asPossiblyQualifiedName, emptyList(), false)
        val vd = VariableDefinitionDefault(SimpleName(name), tr)
        val ds = DomainSignatureDefault(dr, vd)
        _sigDomains.add(ds)
    }

    fun values(vararg values: Expression) {
        _values.add(values.toList())
    }

    fun build() = M2MTransformTableDefault(_isTop, _name).also { tbl ->
        (tbl.parameters as MutableList).addAll(_parameters)
        (tbl.extends as MutableList).addAll(_extends)
        _sigDomains.forEach { rd ->
            (tbl.domainSignature as MutableMap)[rd.domainRef] = rd
        }
        val valsMap = _values.map { vs ->
            vs.mapIndexed { i, v -> Pair(_sigDomains[i].domainRef, v) }.toMap()
        }
        (tbl.values as MutableList).addAll(valsMap)
    }
}

@M2mModelDslMarker
class M2MTransformMappingBuilder internal constructor(
    private val _name: SimpleName
) {
    private var _isTop: Boolean = false

    fun build() = M2MTransformMappingDefault(_isTop, _name).also { mapping ->
        //TODO
    }

}

@M2mModelDslMarker
class M2MTransformRelationBuilder internal constructor(
    private val _name: SimpleName
) {
    private var _isTop: Boolean = false

    fun build() = M2MTransformRelationDefault(_isTop, _name).also { rel ->
        //TODO
    }

}

@M2mModelDslMarker
class ObjectTemplateBuilder internal constructor(

) {
    private val _typeRef: TypeReference? = null
    private val _propTemplate = mutableMapOf<SimpleName,PropertyTemplate>()

    fun build() = ObjectTemplateDefault(_typeRef!!, _propTemplate).also {

    }
}