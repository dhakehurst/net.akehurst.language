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

import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserByMethodRegistrationAbstract
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.asm.OptionHolderDefault
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.processor.ExpressionsSyntaxAnalyser
import net.akehurst.language.m2mTransform.api.DomainReference
import net.akehurst.language.m2mTransform.api.M2mTransformTestCase
import net.akehurst.language.m2mTransform.asm.M2mTransformTestCaseDefault
import net.akehurst.language.m2mTransformTest.api.M2mTransformTestDefinition
import net.akehurst.language.m2mTransformTest.api.M2mTransformTestDomain
import net.akehurst.language.m2mTransformTest.api.M2mTransformTestNamespace
import net.akehurst.language.m2mTransformTest.asm.M2mTransformTestDefinitionDefault
import net.akehurst.language.m2mTransformTest.asm.M2mTransformTestDomainDefault
import net.akehurst.language.m2mTransformTest.asm.M2mTransformTestNamespaceDefault
import net.akehurst.language.m2mTransformTest.asm.TransformUnderTestReferenceDefault
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sppt.api.SpptDataNodeInfo

class M2mTransformTestSyntaxAnalyser : SyntaxAnalyserByMethodRegistrationAbstract<M2mTransformTestDomain>() {

    override val extendsSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = mapOf(
        QualifiedName("Expressions") to ExpressionsSyntaxAnalyser()
    )

    override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<*>> = emptyMap()

    override fun registerHandlers() {
        super.register(this::unit)
        super.register(this::namespace)
        super.register(this::definition)
        super.register(this::transformTest)
        super.register(this::testCase)
        super.register(this::testDomain)
    }

    // unit = option* namespace* ;
    private fun unit(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mTransformTestDomain {
        val options = children[0] as List<Pair<String, Any>>
        val namespace = children[1] as List<M2mTransformTestNamespace>
        val optHolder = OptionHolderDefault(null, options.toMap())
        namespace.forEach { (it.options as OptionHolderDefault).parent = optHolder }
        return M2mTransformTestDomainDefault(SimpleName("Unit"), optHolder, namespace)
    }

    // namespace = 'namespace' possiblyQualifiedName option* import* definition* ;
    private fun namespace(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): M2mTransformTestNamespace {
        val pqn = children[1] as PossiblyQualifiedName
        val options = children[2] as List<Pair<String, Any>>
        val import = children[3] as List<Import>
        val definition = children[4] as List<Pair<(ns: M2mTransformTestNamespace) -> M2mTransformTestDefinition, SpptDataNodeInfo>>

        val optHolder = OptionHolderDefault(null, options.toMap())
        val ns = M2mTransformTestNamespaceDefault(pqn.asQualifiedName(null), optHolder, import)
        definition.forEach { (create, defNode) ->
            val def = create.invoke(ns)
            when {
                null != ns.findOwnedDefinitionOrNull(def.name) -> issues.error(
                    locationForNode(sentence, defNode.node),
                    "namespace '${ns.qualifiedName.value}' already contains a transform-test named '${def.name.value}'."
                )

                else -> ns.addDefinition(def)
            }
        }
        return ns
    }

    // override definition = transformTest ;
    private fun definition(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<(ns: M2mTransformTestNamespace) -> M2mTransformTestDefinition, SpptDataNodeInfo> =
        Pair(children[0] as ((ns: M2mTransformTestNamespace) -> M2mTransformTestDefinition), nodeInfo)

    // transformTest = 'transform-test' IDENTIFIER 'for' possiblyQualifiedName '{' testCase* '}' ;
    private fun transformTest(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): (ns: M2mTransformTestNamespace) -> M2mTransformTestDefinition {
        val name = SimpleName(children[1] as String)
        val transform = children[3] as PossiblyQualifiedName
        val testCases = children[5] as List<Pair<M2mTransformTestCase, SpptDataNodeInfo>>
        return { ns ->
            M2mTransformTestDefinitionDefault(ns, name, TransformUnderTestReferenceDefault(transform)).also { def ->
                setLocationFor(def, nodeInfo, sentence)
                testCases.forEach { (tc, tcNode) ->
                    when {
                        def.testCase.containsKey(tc.name) -> issues.error(
                            locationForNode(sentence, tcNode.node),
                            "transform-test '${name.value}' already contains a test-case named '${tc.name.value}'."
                        )

                        else -> def.addTestCase(tc)
                    }
                }
            }
        }
    }

    // testCase = 'test-case' IDENTIFIER '{' testDomain{2+} '}' ;
    private fun testCase(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<M2mTransformTestCase, SpptDataNodeInfo> {
        val name = SimpleName(children[1] as String)
        val testDomains = children[3] as List<Pair<DomainReference, Expression>>
        val tc = M2mTransformTestCaseDefault(name)
        setLocationFor(tc, nodeInfo, sentence)
        testDomains.forEach { (dr, exp) ->
            when {
                tc.domain.containsKey(dr) -> issues.error(
                    locationForNode(sentence, nodeInfo.node),
                    "test-case '${name.value}' gives domain '${dr.value}' more than once."
                )

                else -> tc.domain[dr] = exp
            }
        }
        return Pair(tc, nodeInfo)
    }

    // testDomain = 'domain' domainReference ':=' expression ;
    private fun testDomain(nodeInfo: SpptDataNodeInfo, children: List<Any?>, sentence: Sentence): Pair<DomainReference, Expression> {
        val dr = DomainReference(children[1] as String)
        val exp = children[3] as Expression
        return Pair(dr, exp)
    }
}
