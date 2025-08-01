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

package net.akehurst.language.m2mTransform.processor

import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.processor.EvaluationContext
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.expressions.processor.ObjectGraph
import net.akehurst.language.expressions.processor.TypedObject
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.*

data class M2MTransformResult<OT : Any>(
    val issues: IssueHolder,
    val objects: Map<DomainReference,TypedObject<OT>>
)

class M2mTransformInterpreter<OT : Any>(
    val m2m: M2mTransformDomain,
    val objectGraph: Map<SimpleName, ObjectGraph<OT>>,
    val _issues: IssueHolder = IssueHolder(LanguageProcessorPhase.INTERPRET)
) {

    fun transform(fromDomainRef: DomainReference, root: TypedObject<OT>): M2MTransformResult<OT> {
        val pair = m2m.allDefinitions.firstNotNullOfOrNull {
            val dp = it.domainParameters // domainRef -> Domain-Name
            val tr = it.topRule.firstOrNull { tr ->
                tr.domainItem[fromDomainRef]
                    ?.let { di ->
                        di.variable.type.let { dt ->
                            root.type.conformsTo(dt)
                        }
                    } ?: false
            }
            Pair(dp, tr)
        }
        val topRule = pair?.second
        val result = when (topRule) {
            null -> {
                _issues.error(null, "No top rule found with 'domain $fromDomainRef ?:${root.type}'")
                emptyMap<DomainReference, TypedObject<OT>>()
            }
            is M2mRelation -> {
                //TODO
                emptyMap<DomainReference, TypedObject<OT>>()
            }
            is M2mMapping -> {
                val domainParameters = pair.first
                executeMapping(topRule, domainParameters, fromDomainRef, root)
            }

            else -> error("")
        }

        return M2MTransformResult<OT>(_issues, result)
    }

    private fun executeMapping(rule: M2mMapping, domainParameters: Map<DomainReference, SimpleName>, sourceDomainName: DomainReference, source: TypedObject<OT>): Map<DomainReference, TypedObject<OT>> {
        val result = mutableMapOf<DomainReference, TypedObject<OT>>()
        val srcDomainItem = rule.domainItem[sourceDomainName] ?: error("No domain item found for domain '$sourceDomainName'")
        rule.domainItem.forEach { (dn, targetDomain) ->
            if (dn == sourceDomainName) {
                // source, so do nothing
            } else {
                val expression = rule.expression[dn]
                when (expression) {
                    null -> _issues.error(null, "No expression found for domain name '$dn'")
                    else -> {
                        val dtn = domainParameters[dn] ?: error("DomainParameter not found for domain '$dn'")
                        val tgtOg = objectGraph[dtn] ?: error("ObjectGraph not found for domain '$dtn'")
                        val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(tgtOg, _issues)
                        val evc = EvaluationContext.of(mapOf(srcDomainItem.variable.name.value to source))
                        val target = exprInterp.evaluateExpression(evc, expression)
                        result[dn] = target
                    }
                }
            }
        }
        return result
    }

}