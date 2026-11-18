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

package net.akehurst.language.m2mTransformTest.asm

import net.akehurst.kotlinx.utils.Indent
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.OptionHolder
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.asm.DefinitionAbstract
import net.akehurst.language.base.asm.DomainAbstract
import net.akehurst.language.base.asm.NamespaceAbstract
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.m2mTransform.api.DomainReference
import net.akehurst.language.m2mTransform.api.M2mTransformRuleSet
import net.akehurst.language.m2mTransform.api.M2mTransformTestCase
import net.akehurst.language.m2mTransformTest.api.*

class M2mTransformTestDomainDefault(
    override val name: SimpleName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    namespace: List<M2mTransformTestNamespace> = emptyList()
) : M2mTransformTestDomain, DomainAbstract<M2mTransformTestNamespace, M2mTransformTestDefinition>(namespace, options) {
    override val allTransformTest: List<M2mTransformTestDefinition> get() = allDefinitions
}

class M2mTransformTestNamespaceDefault(
    override val qualifiedName: QualifiedName,
    options: OptionHolder = OptionHolderDefault(null, emptyMap()),
    import: List<Import> = emptyList()
) : M2mTransformTestNamespace, NamespaceAbstract<M2mTransformTestDefinition>(options, import)

class TransformUnderTestReferenceDefault(
    override val nameOrQName: PossiblyQualifiedName
) : TransformUnderTestReference {
    override var resolved: M2mTransformRuleSet? = null
        private set

    override fun resolveAs(resolved: M2mTransformRuleSet) {
        this.resolved = resolved
    }
}

class M2mTransformTestDefinitionDefault(
    override val namespace: M2mTransformTestNamespace,
    override val name: SimpleName,
    override val transform: TransformUnderTestReference,
    override val options: OptionHolder = OptionHolderDefault(null, emptyMap()),
) : M2mTransformTestDefinition, DefinitionAbstract<M2mTransformTestDefinition>() {

    override val domainParameters: Map<DomainReference, SimpleName> get() = transform.resolved?.domainParameters ?: emptyMap()

    override val testCase: Map<SimpleName, M2mTransformTestCase> = linkedMapOf() //order is important

    fun addTestCase(value: M2mTransformTestCase) {
        (testCase as MutableMap)[value.name] = value
    }

    override fun asString(indent: Indent, imports: List<Import>): String {
        val sb = StringBuilder()
        sb.append("transform-test ${name.value} for ${transform.nameOrQName.value} {\n")
        val tcIndent = indent.inc
        val dIndent = tcIndent.inc
        testCase.values.forEach { tc ->
            sb.append("${tcIndent}test-case ${tc.name.value} {\n")
            tc.domain.forEach { (dr, exp) ->
                sb.append("${dIndent}domain ${dr.value} := ${exp.asString(dIndent, imports)}\n")
            }
            sb.append("${tcIndent}}\n")
        }
        sb.append("${indent}}")
        return sb.toString()
    }
}
