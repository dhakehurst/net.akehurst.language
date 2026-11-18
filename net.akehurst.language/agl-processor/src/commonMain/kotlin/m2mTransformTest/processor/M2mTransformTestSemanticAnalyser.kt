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

package net.akehurst.language.m2mTransformTest.processor

import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.M2mTransformDomain
import net.akehurst.language.m2mTransform.api.M2mTransformRuleSet
import net.akehurst.language.m2mTransformTest.api.M2mTransformTestDefinition
import net.akehurst.language.m2mTransformTest.api.M2mTransformTestDomain

/**
 * a sentence context holding the transforms of [m2m], for the semantic analysis of transform tests
 */
fun contextFromM2mTransformDomain(m2m: M2mTransformDomain): SentenceContext = SentenceContextAny().also { ctx ->
    m2m.allTransformRuleSet.forEach { rs ->
        ctx.addToScope(null, rs.qualifiedName.parts.map { it.value }, M2mTransformTest.transformRuleSetItemTypeName, null, rs)
    }
}

/**
 * resolves the transform under test of each transform-test, and checks that each domain of a test-case is a domain of that transform
 */
class M2mTransformTestSemanticAnalyser : SemanticAnalyser<M2mTransformTestDomain, SentenceContext> {

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private var _locationMap: LocationMap = LocationMapDefault()

    override fun clear() {
        _issues.clear()
    }

    override fun analyse(
        sentenceIdentity: Any?,
        asm: M2mTransformTestDomain,
        locationMap: LocationMap?,
        options: SemanticAnalysisOptions<SentenceContext>
    ): SemanticAnalysisResult {
        _locationMap = locationMap ?: LocationMapDefault()
        val sentenceContext = options.sentenceContext
        when (sentenceContext) {
            null -> _issues.warn(null, "No sentence context provided, either provide one (e.g. contextFromM2mTransformDomain) or switch off Semantic Analysis.")
            else -> asm.allTransformTest.forEach { test ->
                val transform = findTransform(sentenceContext, test)
                when (transform) {
                    null -> _issues.error(_locationMap[test], "Transform '${test.transform.nameOrQName.value}' not found for transform-test '${test.name.value}'.")
                    else -> {
                        test.transform.resolveAs(transform)
                        checkTestCases(test, transform)
                    }
                }
            }
        }
        return SemanticAnalysisResultDefault(emptyList(), _issues)
    }

    /**
     * a qualified name as it is; a simple name in the namespace of the test, then in each imported namespace (first import first)
     */
    private fun findTransform(sentenceContext: SentenceContext, test: M2mTransformTestDefinition): M2mTransformRuleSet? {
        val pqn = test.transform.nameOrQName
        val candidates = when (pqn) {
            is QualifiedName -> listOf(pqn)
            is SimpleName -> listOf(test.namespace.qualifiedName.append(pqn)) + test.namespace.import.map { QualifiedName(it.value).append(pqn) }
        }
        return candidates.firstNotNullOfOrNull { qn ->
            sentenceContext.findItemsByQualifiedNameConformingTo(qn.parts.map { it.value }) { it == M2mTransformTest.transformRuleSetItemTypeName }
                .firstNotNullOfOrNull { it.item as? M2mTransformRuleSet }
        }
    }

    private fun checkTestCases(test: M2mTransformTestDefinition, transform: M2mTransformRuleSet) {
        test.testCase.values.forEach { tc ->
            tc.domain.keys.forEach { dr ->
                if (transform.domainParameters.containsKey(dr).not()) {
                    _issues.error(
                        _locationMap[tc],
                        "In test-case '${tc.name.value}' of transform-test '${test.name.value}', '${dr.value}' is not a domain of transform '${transform.qualifiedName.value}'."
                    )
                }
            }
        }
    }
}
