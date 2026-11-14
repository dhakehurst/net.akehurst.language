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

import kotlinx.coroutines.test.runTest
import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.m2mTransform.testing.m2mTransformTestSuits
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.processor.ExternalGetterAsmSimple
import net.akehurst.language.expressions.processor.objectGraphSimpleAsm
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.DomainReference
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.asm.StdLibDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Rule arguments for mapping rules without 'when', and issues of one transform not leaking into the next.
 * Runs the old sync interpreter and both modes of the combined one.
 */
class test_m2mTransformInterpreter_issuesAndArgs {

    private companion object {
        val suite = m2mTransformTestSuits {
            testSuit("rule argument used in mapping without when") {
                typesDomain("d1", "Domain1", true) { namespace("n1") {} }
                typesDomain("d2", "Domain2", true) { namespace("n2") {} }
                transform(
                    $$"""
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A12A2 {
                            domain d1 a1:String == 'Any'
                            domain d2 a2:String := 'Hello ' + $x
                        }
                    }
                """
                )
            }
        }.values.first()

        fun compile(transform: String) = run {
            val context = SentenceContextAny()
            suite.typeDomains.forEach { (_, v) -> context.addToScope(null, listOf(v.name.value), QualifiedName("TypesDomain"), null, v) }
            val res = Agl.registry.agl.m2mTransform.processor!!.process(transform, options = Agl.options { semanticAnalysis { sentenceContext(context) } })
            check(res.allIssues.errors.isEmpty()) { res.allIssues.toString() }
            res.asm!!
        }

        val m2m = compile(suite.transform)

        // 'unknownVar' is not defined, so evaluating the target expression raises a warning
        val m2mWithWarning = compile(
            $$"""
            namespace test
            transform Test(d1:Domain1, d2:Domain2) {
                top mapping A12A2 {
                    domain d1 a1:String == 'Any'
                    domain d2 a2:String := 'Hello ' + unknownVar
                }
            }
        """
        )

        fun ogs(issues: IssueHolder): Map<SimpleName, ObjectGraphAccessorMutator> = suite.typeDomains.entries.associate { (k, v) ->
            Pair(v.name, objectGraphSimpleAsm(v, null, issues, LocationMapDefault(), ExternalGetterAsmSimple(v, suite.crossReferenceDomains[k], issues, LocationMapDefault())))
        }

        val d1 = DomainReference("d1")
        val d2 = DomainReference("d2")

        fun str(og: ObjectGraphAccessorMutator, value: String): TypedObject = og.createPrimitiveValue(StdLibDefault.String.qualifiedTypeName, value)
    }

    private enum class Mode { OLD, SYNC, SUSPEND }

    private suspend fun run(mode: Mode, ogs: Map<SimpleName, ObjectGraphAccessorMutator>, issues: IssueHolder, input: String, interp: Any?, m2m: net.akehurst.language.m2mTransform.api.M2mTransformDomain = Companion.m2m): Pair<Any, M2MTransformResult> {
        val og1 = ogs[SimpleName("Domain1")]!!
        val og2 = ogs[SimpleName("Domain2")]!!
        val args = mapOf($$"$x" to str(og2, "World"))
        val source = mapOf(d1 to listOf(str(og1, input)))
        val tgt = m2m.allTransformRuleSet.first()
        return when (mode) {
            Mode.OLD -> {
                val i = (interp as? M2mTransformInterpreter) ?: M2mTransformInterpreter(m2m, ogs, issues)
                Pair(i, i.transform(tgt, d2, args, source))
            }

            Mode.SYNC -> {
                val i = (interp as? M2mTransformInterpreterCombined) ?: M2mTransformInterpreterCombined(m2m, ogs, issues)
                Pair(i, i.transform(tgt, d2, args, source))
            }

            Mode.SUSPEND -> {
                val i = (interp as? M2mTransformInterpreterCombined) ?: M2mTransformInterpreterCombined(m2m, ogs, issues)
                Pair(i, i.transformSuspend(tgt, d2, args, source))
            }
        }
    }

    private suspend fun ruleArgumentWithoutWhen(mode: Mode) {
        val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        val ogs = ogs(issues)
        val (_, res) = run(mode, ogs, issues, "Any", null)
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        assertEquals(listOf("Hello World"), res.targets.map { ogs[SimpleName("Domain2")]!!.valueOf(it) })
    }

    private suspend fun issuesOfEarlierTransformNotInResult(mode: Mode) {
        val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        val ogs = ogs(issues)
        // the accessor-mutators (and their issues) are shared by both transforms
        val (_, res1) = run(mode, ogs, issues, "Any", null, m2mWithWarning)
        assertTrue(res1.issues.isNotEmpty(), "expected an issue from the first transform")
        val (_, res2) = run(mode, ogs, issues, "Any", null)
        assertTrue(res2.issues.isEmpty(), res2.issues.toString())
    }

    @Test
    fun ruleArgumentWithoutWhen_old() = runTest { ruleArgumentWithoutWhen(Mode.OLD) }

    @Test
    fun ruleArgumentWithoutWhen_combined() = runTest { ruleArgumentWithoutWhen(Mode.SYNC) }

    @Test
    fun ruleArgumentWithoutWhen_combinedSuspend() = runTest { ruleArgumentWithoutWhen(Mode.SUSPEND) }

    @Test
    fun issuesOfEarlierTransformNotInResult_old() = runTest { issuesOfEarlierTransformNotInResult(Mode.OLD) }

    @Test
    fun issuesOfEarlierTransformNotInResult_combined() = runTest { issuesOfEarlierTransformNotInResult(Mode.SYNC) }

    @Test
    fun issuesOfEarlierTransformNotInResult_combinedSuspend() = runTest { issuesOfEarlierTransformNotInResult(Mode.SUSPEND) }
}
