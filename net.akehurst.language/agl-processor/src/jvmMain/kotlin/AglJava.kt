/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl

import kotlinx.coroutines.runBlocking
import net.akehurst.language.api.processor.M2mTransformString
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.m2mTransform.api.DomainReference
import net.akehurst.language.m2mTransform.processor.M2MTransformResult
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.TypesDomain

object AglJava {

    @JvmStatic
    fun executeExpressionSuspendBlocking(accessorMutator: ObjectGraphAccessorMutator, self: Any?, expression: String): TypedObject = runBlocking {
        Agl.executeExpressionSuspend(accessorMutator, self, expression)
    }

    @JvmStatic
    fun executeExpressionWithEvaluationContextSuspend(accessorMutator: ObjectGraphAccessorMutator, evc: EvaluationContext, expression: String): TypedObject = runBlocking {
        Agl.executeExpressionWithEvaluationContextSuspend(accessorMutator, evc, expression)
    }

    @JvmStatic
    fun transformSuspendBlocking(
        m2m: M2mTransformString,
        typeDomains: Map<DomainReference, TypesDomain>,
        accessorMutators: Map<SimpleName, ObjectGraphAccessorMutator>,
        domains: Map<DomainReference, List<TypedObject>>,
        targetDomainReference: DomainReference
    ): M2MTransformResult = runBlocking {
        Agl.transformSuspend(m2m, typeDomains, accessorMutators, domains, targetDomainReference)
    }
}