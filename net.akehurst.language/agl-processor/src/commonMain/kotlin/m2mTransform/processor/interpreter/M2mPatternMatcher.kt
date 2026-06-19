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

package net.akehurst.language.agl.m2mTransform.processor.interpreter

import net.akehurst.kotlinx.collections.topologicalSort
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.CollectionTemplate
import net.akehurst.language.m2mTransform.api.M2mTransformRule
import net.akehurst.language.m2mTransform.api.ObjectTemplate
import net.akehurst.language.m2mTransform.api.PropertyTemplateExpression
import net.akehurst.language.m2mTransform.api.PropertyTemplateRhs
import net.akehurst.language.m2mTransform.processor.M2mTransformExecution
import net.akehurst.language.m2mTransform.processor.M2mTransformInterpreter.Companion.cartesianProduct
import net.akehurst.language.m2mTransform.processor.M2mTransformInterpreter.Companion.findCoveringSubsets2
import net.akehurst.language.m2mTransform.processor.TemplateMatchAlternatives
import net.akehurst.language.m2mTransform.processor.TemplateMatchResult
import net.akehurst.language.m2mTransform.processor.TemplateMatchResult.Companion.merge
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import kotlin.collections.component1
import kotlin.collections.component2

class M2mPatternMatcher(
    val issues: IssueHolder,
    val accessorMutator: ObjectGraphAccessorMutator
) {

    fun matchVariablesFromRhs(
        variables: Map<String, TypedObject>,
        src: TypedObject,
        rhs: PropertyTemplateRhs
    ): TemplateMatchAlternatives {



        return when (rhs) {
            is PropertyTemplateExpression -> {
                val mr = matchVariablesFromPropertyTemplateExpression(variables, rhs, src)
                // only keep the result if it is a match
                mr.takeIf { it.isMatch }?.let { TemplateMatchAlternatives(listOf(it)) } ?: TemplateMatchAlternatives(emptyList())
            }

            is ObjectTemplate -> matchVariablesFromObjectTemplate(variables, rhs, src)
            is CollectionTemplate -> matchVariablesFromCollectionTemplate(variables, rhs, src)
            else -> error("Unknown rhs type ${rhs::class}")
        }.let { tma ->
            rhs.identifier?.let { id ->
                tma.withVariable(id.value, src)
            } ?: tma
        }
    }



    /**
     * always returns isMatch==true
     */
    fun matchVariablesFromPropertyTemplateExpression(
        variables: Map<String, TypedObject>,
        template: PropertyTemplateExpression,
        src: TypedObject
    ): TemplateMatchResult {
        // either :
        // 1) rhs is a free-variable with no value set, in which case set it to the lhs
        // 2) rhs is an expression with a value that matches the lhs
        val expr = template.expression
        return when {
            expr is RootExpression && variables.contains(expr.name).not() -> {
                TemplateMatchResult(true, mapOf(expr.name to src))
            }

            else -> {
                val exprInterp = ExpressionsInterpreterOverTypedObject(accessorMutator)
                val evc = EvaluationContext.of(variables)
                val value = exprInterp.evaluateExpression(evc, expr)
                val isMatch = accessorMutator.equalTo(src, value)
                TemplateMatchResult(isMatch, emptyMap())
            }
        }
    }

    fun matchVariablesFromObjectTemplate(
        variables: Map<String, TypedObject>,
        template: ObjectTemplate,
        src: TypedObject
    ): TemplateMatchAlternatives {
        return when {
            template.propertyTemplate.isEmpty() -> TemplateMatchAlternatives(listOf(TemplateMatchResult.EMPTY()))
            else -> {
                val propTemplateMatches = template.propertyTemplate.map { (k, v) ->
                    val rhsPat = v.rhs
                    val lhs = src.getProperty(k.value)
                    matchVariablesFromRhs(variables, lhs, rhsPat)
                } //TODO: determine no match before cartesianProduct
                val result = propTemplateMatches.map { it.alternatives }.cartesianProduct()
                val alts = result.map { l -> l.merge() }.filter { it.isMatch }
                TemplateMatchAlternatives(alts)
            }
        }
    }

    fun matchVariablesFromCollectionTemplate(
        variables: Map<String, TypedObject>,
        template: CollectionTemplate,
        src: TypedObject
    ): TemplateMatchAlternatives {
        val result = when {
            src.type.isCollection -> {
                // TODO: maybe a faster way to do it if NOT isSubset!
                val elements = mutableListOf<TypedObject>()
                accessorMutator.forEachIndexed(src) { idx, el -> elements.add(el) } //TODO: find a way not to 'collect' the list
                if (template.isSubset.not() && elements.size != template.elements.size) {
                    // TODO: should return a no match not error!
                    error("Collection size does not match template size: ${elements.size} != ${template.elements.size} ")
                }
                matchVariablesFromCollectionTemplateToElementsIn(variables, template, elements)
            }

            else -> error("src is not a collection")
        }
        return TemplateMatchAlternatives(result)
    }

    // get the different alternative combinations of objects from elements that (cover) match the defined templates
    // Set(  List(element-match per template)  )
    fun matchVariablesFromCollectionTemplateToElementsIn(
        variables: Map<String, TypedObject>,
        collectionTemplate: CollectionTemplate,
        elements: List<TypedObject>
    ): List<TemplateMatchResult> {
        val options = findCoveringSubsets2(
            cover = collectionTemplate.elements,
            bySubsetsOf = elements
        ) { tp, el ->
            val mr = matchVariablesFromRhs(variables, el, tp)
            Pair(mr.alternatives.isNotEmpty(), mr)
        }
        val res = options.map { opt ->
            opt.flatMap { it.alternatives }.merge()
        }
        return res
    }


}