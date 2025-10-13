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

import net.akehurst.language.api.processor.EvaluationContext
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.expressions.processor.ObjectGraph
import net.akehurst.language.expressions.processor.TypedObject
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.*
import net.akehurst.language.types.api.PropertyName

data class M2MTransformResult<OT : Any>(
    val issues: IssueHolder,
    val objects: Map<DomainReference, TypedObject<OT>>
)

data class PatternMatchResult<OT : Any>(
    val isMatch: Boolean,
    val value: TypedObject<OT>,
    val issues: IssueHolder
)

class M2mTransformInterpreter<OT : Any>(
    val m2m: M2mTransformDomain,
    val objectGraph: Map<SimpleName, ObjectGraph<OT>>,
    val _issues: IssueHolder = IssueHolder(LanguageProcessorPhase.INTERPRET)
) {

    fun transform(targetDomainRef: DomainReference, root: Map<DomainReference, TypedObject<OT>>): M2MTransformResult<OT> {
        val pair = m2m.allTransformRuleSet.firstNotNullOfOrNull {
            val dp = it.domainParameters // domainRef -> Domain-Name
            val tr = it.topRule.firstOrNull { tr ->
                val srcDomainRefs = tr.domainItem.filterKeys { k -> k != targetDomainRef }
                srcDomainRefs.keys == root.keys
                        && root.entries.all { (k, v) ->
                    tr.domainItem[k]?.let { di ->
                        di.variable.type.let { dt ->
                            v.type.conformsTo(dt)
                        }
                    } ?: false
                }
            }
            Pair(dp, tr)
        }
        val topRule = pair?.second
        val result = when (topRule) {
            null -> {
                _issues.error(null, "No conforming top rule found for target domain '${targetDomainRef.value}'")
                emptyMap<DomainReference, TypedObject<OT>>()
            }

            is M2mRelation -> {
                val domainParameters = pair.first
                executeRelation(topRule, domainParameters, targetDomainRef, root)
            }

            is M2mMapping -> {
                val domainParameters = pair.first
                executeMapping(topRule, domainParameters, targetDomainRef, root)
            }

            else -> error("")
        }

        return M2MTransformResult<OT>(_issues, result)
    }

    private fun executeMapping(
        rule: M2mMapping,
        domainParameters: Map<DomainReference, SimpleName>,
        targetDomainRef: DomainReference,
        source: Map<DomainReference, TypedObject<OT>>
    ): Map<DomainReference, TypedObject<OT>> {
        val result = mutableMapOf<DomainReference, TypedObject<OT>>()

        val srcObjs = source.entries.associate { (k, v) ->
            val di = rule.domainItem[k] ?: error("No domain item found for source domain ref '$k'")
            Pair(di.variable.name.value, v)
        }

        val expression = rule.expression[targetDomainRef]
        when (expression) {
            null -> _issues.error(null, "No expression found for target domain ref '$targetDomainRef'")
            else -> {
                val dtn = domainParameters[targetDomainRef] ?: error("DomainParameter not found for target domain ref '$targetDomainRef'")
                val tgtOg = objectGraph[dtn] ?: error("ObjectGraph not found for domain '$dtn'")
                val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(tgtOg, _issues)
                val evc = EvaluationContext.of(srcObjs)
                val target = exprInterp.evaluateExpression(evc, expression)
                result[targetDomainRef] = target
            }
        }

        return result
    }

    private fun executeRelation(
        rule: M2mRelation,
        domainParameters: Map<DomainReference, SimpleName>,
        targetDomainRef: DomainReference,
        source: Map<DomainReference, TypedObject<OT>>
    ): Map<DomainReference, TypedObject<OT>> {
        val result = mutableMapOf<DomainReference, TypedObject<OT>>()
        val variables = mutableMapOf<String, TypedObject<OT>>()

        val srcDomainRefs = rule.domainItem.filterKeys { k -> k != targetDomainRef }
        val isMatch = srcDomainRefs.all { (srcDomainRef, srcDomainItem) ->
            val srcDtn = domainParameters[srcDomainRef]
            val srcOg = objectGraph[srcDtn] ?: error("ObjectGraph not found for domain '$srcDomainRef'")
            val srcObjPat = rule.objectPattern[srcDomainRef] ?: error("No object pattern found for domain '$srcDomainRef'")
            // match variables from source domain
            val src = source[srcDomainRef] ?: error("No source object found for domain '$srcDomainRef'")
            val res = matchVariablesFromRhs(variables, srcObjPat, srcOg, src)
            if (res.isMatch) {
                variables[srcDomainItem.variable.name.value] = res.value
                true
            } else {
                false
            }
        }

        if (isMatch) {
            val dtn = domainParameters[targetDomainRef] ?: error("DomainParameter not found for domain '$targetDomainRef'")
            val tgtOg = objectGraph[dtn] ?: error("ObjectGraph not found for domain '$dtn'")
            val objPat = rule.objectPattern[targetDomainRef] ?: error("No object pattern found for domain '$targetDomainRef'")
            val obj = createFromRhs(variables, objPat, tgtOg)
            result[targetDomainRef] = obj

        } else {
            // no match, do nothing
        }
        return result
    }

    fun matchVariablesFromRhs(variables: MutableMap<String, TypedObject<OT>>, rhs: PropertyPatternRhs, srcObjectGraph: ObjectGraph<OT>, src: TypedObject<OT>): PatternMatchResult<OT> =
        when (rhs) {
            is PropertyPatternExpression -> matchVariablesFromPropertyPatternExpression(variables, rhs, srcObjectGraph, src)
            is ObjectPattern -> matchVariablesFromObjectPattern(variables, rhs, srcObjectGraph, src)
            else -> error("Unknown rhs type ${rhs::class}")
        }

    /**
     * always returns isMatch==true
     */
    fun matchVariablesFromPropertyPatternExpression(
        variables: MutableMap<String, TypedObject<OT>>,
        ppe: PropertyPatternExpression,
        srcObjectGraph: ObjectGraph<OT>,
        src: TypedObject<OT>
    ): PatternMatchResult<OT> {
        val expr = ppe.expression
        val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(srcObjectGraph, issues)
        val evc = EvaluationContext.of(variables)
        val value = exprInterp.evaluateExpression(evc, expr)
        val isMatch = srcObjectGraph.isNothing(value).not()
        return PatternMatchResult(isMatch, value, issues)
    }

    fun matchVariablesFromObjectPattern(
        variables: MutableMap<String, TypedObject<OT>>,
        objectPattern: ObjectPattern,
        srcObjectGraph: ObjectGraph<OT>,
        src: TypedObject<OT>
    ): PatternMatchResult<OT> {
        val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        val isMatch = objectPattern.propertyPattern.all { (k, v) ->
            val rhsPat = v.rhs
            val res = matchVariablesFromRhs(variables, rhsPat, srcObjectGraph, src)
            when {
                res.isMatch -> {
                    val sv = srcObjectGraph.getProperty(src, k.value)
                    srcObjectGraph.equalTo(sv, res.value)
                }

                srcObjectGraph.isNothing(res.value) -> {
                    // rhs variable not set, so set it
                    val varName = when {
                        rhsPat is PropertyPatternExpression -> when {
                            rhsPat.expression is RootExpression -> {
                                val n = (rhsPat.expression as RootExpression).name
                                SimpleName(n)
                            }

                            else -> error("rhs expression is not a pivot variable name, cannot set it")
                        }

                        else -> error("rhs is not a PropertyPatternExpression, cannot set it")
                    }
                    val value = srcObjectGraph.getProperty(src, k.value)
                    variables[varName.value] = value
                    true
                }

                else -> {
                    false
                }
            }
        }

        // if the pattern has a name, assign it
        objectPattern.identifier?.let { variables[it.value] = src }
        return PatternMatchResult(isMatch, src, issues)
    }

    fun createFromRhs(variables: MutableMap<String, TypedObject<OT>>, rhs: PropertyPatternRhs, tgtObjectGraph: ObjectGraph<OT>): TypedObject<OT> = when (rhs) {
        is PropertyPatternExpression -> createFromPropertyPatternExpression(variables, rhs, tgtObjectGraph)
        is ObjectPattern -> createFromObjectPattern(variables, rhs, tgtObjectGraph)
        else -> error("Unknown rhs type ${rhs::class}")
    }

    /**
     * returns value of expression evaluated in context of provided variables
     */
    fun createFromPropertyPatternExpression(variables: MutableMap<String, TypedObject<OT>>, ppe: PropertyPatternExpression, tgtObjectGraph: ObjectGraph<OT>): TypedObject<OT> {
        val expr = ppe.expression
        val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(tgtObjectGraph, _issues)
        val evc = EvaluationContext.of(variables)
        val value = exprInterp.evaluateExpression(evc, expr)
        return value
    }

    fun createFromObjectPattern(variables: MutableMap<String, TypedObject<OT>>, objectPattern: ObjectPattern, tgtObjectGraph: ObjectGraph<OT>): TypedObject<OT> {
        val propValues = mutableMapOf<String, TypedObject<OT>>()
        objectPattern.propertyPattern.forEach { (k, v) ->
            val value = createFromRhs(variables, v.rhs, tgtObjectGraph)
            propValues[k.value] = value
        }
        objectPattern.resolveType(tgtObjectGraph.typesDomain)
        val obj = tgtObjectGraph.createStructureValue(objectPattern.type.qualifiedTypeName, propValues)
        propValues.forEach { (k, v) ->
            val pn = PropertyName(k)
            if (true == objectPattern.type.allResolvedProperty[pn]?.isReadWrite) {
                tgtObjectGraph.setProperty(obj, k, v)
            }
        }
        return obj
    }


}