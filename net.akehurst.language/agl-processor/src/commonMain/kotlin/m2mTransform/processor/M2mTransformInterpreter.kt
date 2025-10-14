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
import net.akehurst.language.types.api.CollectionType
import net.akehurst.language.types.api.PropertyName
import kotlin.collections.component1
import kotlin.collections.component2

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
                val srcDomainRefs = tr.domainSignature.filterKeys { k -> k != targetDomainRef }
                srcDomainRefs.keys == root.keys
                        && root.entries.all { (k, v) ->
                    tr.domainSignature[k]?.let { di ->
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
        val (isMatch, variables) = matchSourceVariables(domainParameters, rule, targetDomainRef, source)
        return when {
            isMatch -> {
                val expression = rule.expression[targetDomainRef]
                when (expression) {
                    null -> {
                        _issues.error(null, "No expression found for target domain ref '$targetDomainRef'")
                        emptyMap()
                    }

                    else -> {
                        val dtn = domainParameters[targetDomainRef] ?: error("DomainParameter not found for target domain ref '$targetDomainRef'")
                        val tgtOg = objectGraph[dtn] ?: error("ObjectGraph not found for domain '$dtn'")
                        val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(tgtOg, _issues)
                        val evc = EvaluationContext.of(variables)
                        val target = exprInterp.evaluateExpression(evc, expression)
                        val result = mutableMapOf<DomainReference, TypedObject<OT>>()
                        result[targetDomainRef] = target
                        result
                    }
                }
            }

            else -> emptyMap()
        }
    }

    private fun executeRelation(
        rule: M2mRelation,
        domainParameters: Map<DomainReference, SimpleName>,
        targetDomainRef: DomainReference,
        source: Map<DomainReference, TypedObject<OT>>
    ): Map<DomainReference, TypedObject<OT>> {
        val result = mutableMapOf<DomainReference, TypedObject<OT>>()
        val (isMatch, variables) = matchSourceVariables(domainParameters, rule, targetDomainRef, source)

        if (isMatch) {
            val dtn = domainParameters[targetDomainRef] ?: error("DomainParameter not found for domain '$targetDomainRef'")
            val tgtOg = objectGraph[dtn] ?: error("ObjectGraph not found for domain '$dtn'")
            val objPat = rule.objectTemplate[targetDomainRef] ?: error("No object pattern found for domain '$targetDomainRef'")
            val obj = createFromRhs(variables, objPat, tgtOg)
            result[targetDomainRef] = obj

        } else {
            // no match, do nothing
        }
        return result
    }

    private fun matchSourceVariables(
        domainParameters: Map<DomainReference, SimpleName>,
        rule: M2mTangibleRule,
        targetDomainRef: DomainReference,
        source: Map<DomainReference, TypedObject<OT>>
    ): Pair<Boolean, Map<String, TypedObject<OT>>> {
        val variables = mutableMapOf<String, TypedObject<OT>>()

        val srcDomainRefs = rule.domainSignature.filterKeys { k -> k != targetDomainRef }
        val isMatch = srcDomainRefs.all { (srcDomainRef, srcDomainItem) ->
            val srcDtn = domainParameters[srcDomainRef]
            val srcOg = objectGraph[srcDtn] ?: error("ObjectGraph not found for domain '$srcDomainRef'")
            val srcObjPat = rule.objectTemplate[srcDomainRef] ?: error("No object pattern found for domain '$srcDomainRef'")
            // match variables from source domain
            val src = source[srcDomainRef] ?: error("No source object found for domain '$srcDomainRef'")
            val res = matchVariablesFromRhs(variables, srcOg, src, srcObjPat)
            if (res.isMatch) {
                variables[srcDomainItem.variable.name.value] = res.value
                true
            } else {
                false
            }
        }
        return Pair(isMatch, variables.takeIf { isMatch } ?: emptyMap())
    }

    fun matchVariablesFromRhs(variables: MutableMap<String, TypedObject<OT>>, srcObjectGraph: ObjectGraph<OT>, src: TypedObject<OT>, rhs: PropertyTemplateRhs): PatternMatchResult<OT> =
        when (rhs) {
            is PropertyTemplateExpression -> matchVariablesFromPropertyTemplateExpression(variables, srcObjectGraph, src, rhs)
            is ObjectTemplate -> matchVariablesFromObjectTemplate(variables, rhs, srcObjectGraph, src)
            is CollectionTemplate -> matchVariablesFromCollectionTemplate(variables, rhs, srcObjectGraph, src)
            else -> error("Unknown rhs type ${rhs::class}")
        }

    /**
     * always returns isMatch==true
     */
    fun matchVariablesFromPropertyTemplateExpression(
        variables: MutableMap<String, TypedObject<OT>>,
        srcObjectGraph: ObjectGraph<OT>,
        lhs: TypedObject<OT>,
        rhs: PropertyTemplateExpression
    ): PatternMatchResult<OT> {
        // either :
        // 1) rhs is a free-variable with no value set, in which case set it to the lhs
        // 2) rhs is an expression with a value that matches the lhs
        val expr = rhs.expression
        return when {
            expr is RootExpression && variables.contains(expr.name).not() -> {
                val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
                variables[expr.name] = lhs
                PatternMatchResult(true, lhs, issues)
            }

            else -> {
                val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
                val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(srcObjectGraph, issues)
                val evc = EvaluationContext.of(variables)
                val value = exprInterp.evaluateExpression(evc, expr)
                val isMatch = srcObjectGraph.equalTo(lhs, value)
                PatternMatchResult(isMatch, value, issues)
            }
        }
    }

    fun matchVariablesFromObjectTemplate(
        variables: MutableMap<String, TypedObject<OT>>,
        objectTemplate: ObjectTemplate,
        srcObjectGraph: ObjectGraph<OT>,
        src: TypedObject<OT>
    ): PatternMatchResult<OT> {
        val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        val isMatch = objectTemplate.propertyTemplate.all { (k, v) ->
            val rhsPat = v.rhs
            val lhs = srcObjectGraph.getProperty(src, k.value)
            val res = matchVariablesFromRhs(variables, srcObjectGraph, lhs, rhsPat)
            when {
                res.isMatch -> {
                    val sv = srcObjectGraph.getProperty(src, k.value)
                    srcObjectGraph.equalTo(sv, res.value)
                }

                srcObjectGraph.isNothing(res.value) -> {
                    // rhs variable not set, so set it
                    val varName = when {
                        rhsPat is PropertyTemplateExpression -> when {
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
        objectTemplate.identifier?.let { variables[it.value] = src }
        return PatternMatchResult(isMatch, src, issues)
    }

    fun matchVariablesFromCollectionTemplate(
        variables: MutableMap<String, TypedObject<OT>>,
        collectionTemplate: CollectionTemplate,
        srcObjectGraph: ObjectGraph<OT>,
        src: TypedObject<OT>
    ): PatternMatchResult<OT> {
        val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        val result = when {
            src.type.isCollection -> {
                val list = mutableListOf<PatternMatchResult<OT>>()
                srcObjectGraph.forEachIndexed(src) { idx, el ->
                    collectionTemplate.elements.firstNotNullOfOrNull { elT ->
                        val matchedVars = mutableMapOf<String, TypedObject<OT>>()
                        val mr = matchVariablesFromRhs(matchedVars, srcObjectGraph, el, elT)
                        issues.addAll(mr.issues)
                        if (mr.isMatch) {
                            variables.putAll(matchedVars)
                            list.add(mr)
                        }
                    }?.let {

                    }
                }
                list
            }
            else -> error("src is not a collection")
        }
        return when {
            collectionTemplate.isSubset -> PatternMatchResult(isMatch, result, issues)
        }
    }

    fun createFromRhs(variables: Map<String, TypedObject<OT>>, rhs: PropertyTemplateRhs, tgtObjectGraph: ObjectGraph<OT>): TypedObject<OT> = when (rhs) {
        is PropertyTemplateExpression -> createFromPropertyPatternExpression(variables, rhs, tgtObjectGraph)
        is ObjectTemplate -> createFromObjectPattern(variables, rhs, tgtObjectGraph)
        else -> error("Unknown rhs type ${rhs::class}")
    }

    /**
     * returns value of expression evaluated in context of provided variables
     */
    fun createFromPropertyPatternExpression(variables: Map<String, TypedObject<OT>>, ppe: PropertyTemplateExpression, tgtObjectGraph: ObjectGraph<OT>): TypedObject<OT> {
        val expr = ppe.expression
        val exprInterp = ExpressionsInterpreterOverTypedObject<OT>(tgtObjectGraph, _issues)
        val evc = EvaluationContext.of(variables)
        val value = exprInterp.evaluateExpression(evc, expr)
        return value
    }

    fun createFromObjectPattern(variables: Map<String, TypedObject<OT>>, objectTemplate: ObjectTemplate, tgtObjectGraph: ObjectGraph<OT>): TypedObject<OT> {
        val propValues = mutableMapOf<String, TypedObject<OT>>()
        objectTemplate.propertyTemplate.forEach { (k, v) ->
            val value = createFromRhs(variables, v.rhs, tgtObjectGraph)
            propValues[k.value] = value
        }
        objectTemplate.resolveType(tgtObjectGraph.typesDomain)
        val obj = tgtObjectGraph.createStructureValue(objectTemplate.type.qualifiedTypeName, propValues)
        propValues.forEach { (k, v) ->
            val pn = PropertyName(k)
            if (true == objectTemplate.type.allResolvedProperty[pn]?.isReadWrite) {
                tgtObjectGraph.setProperty(obj, k, v)
            }
        }
        return obj
    }


}