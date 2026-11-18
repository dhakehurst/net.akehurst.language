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

package net.akehurst.language.m2mTransformTest.api

import net.akehurst.language.base.api.Definition
import net.akehurst.language.base.api.Domain
import net.akehurst.language.base.api.Namespace
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.m2mTransform.api.DomainReference
import net.akehurst.language.m2mTransform.api.M2mTransformRuleSet
import net.akehurst.language.m2mTransform.api.M2mTransformTestCase

/**
 * a unit of the M2mTransformTest language: tests of m2m transforms
 */
interface M2mTransformTestDomain : Domain<M2mTransformTestNamespace, M2mTransformTestDefinition> {
    val allTransformTest: List<M2mTransformTestDefinition>
}

interface M2mTransformTestNamespace : Namespace<M2mTransformTestDefinition>

/**
 * 'transform-test Name for Transform { test-case* }'
 */
interface M2mTransformTestDefinition : Definition<M2mTransformTestDefinition> {
    override val namespace: M2mTransformTestNamespace

    /** the transform under test */
    val transform: TransformUnderTestReference

    /** the domain parameters of the transform under test, empty until it is resolved */
    val domainParameters: Map<DomainReference, SimpleName>

    val testCase: Map<SimpleName, M2mTransformTestCase>
}

interface TransformUnderTestReference {
    val nameOrQName: PossiblyQualifiedName
    val resolved: M2mTransformRuleSet?

    fun resolveAs(resolved: M2mTransformRuleSet)
}
