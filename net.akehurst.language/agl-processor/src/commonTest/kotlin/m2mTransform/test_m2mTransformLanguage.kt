/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.asmTransform.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.grammarTypemodel.builder.grammarTypeModel
import net.akehurst.language.m2mTransform.api.DomainReference
import net.akehurst.language.m2mTransform.api.M2mTransformDomain
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.builder.typesDomain
import net.akehurst.language.types.test.TypesDomainTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_m2mTransformLanguage {

    companion object Companion {
        data class TestData(
            val testName: String,
            val sentence: String,
            val goalRuleName: String? = null,
            val expectedAsm: List<M2mTransformDomain> = emptyList()
        ) {
            val typeDomains = mutableMapOf<DomainReference,TypesDomain>()
        }

        val testData = listOf(
            // namespace
            TestData(
                testName = "empty",
                sentence = """
                    namespace test
                """.trimIndent()
            ),
            TestData(
                testName = "empty transform",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                    }
                """.trimIndent(),
            ),
            TestData(
                testName = "single line comment",
                sentence = """
                    // single line comment
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                    }
                """.trimIndent()
            ),
            TestData(
                testName = "multi line comment",
                sentence = """
                    /* multi
                       line
                       comment
                    */
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                    }
                """.trimIndent()
            ),
            // relations
            TestData(
                testName = "top relation",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      top relation Rel1 {
                        domain d1 x:X {}
                        domain d2 y:Y {}
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            TestData(
                testName = "non top relation",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      relation Rel1 {
                        domain d1 x:X {}
                        domain d2 y:Y {}
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            TestData(
                testName = "multiple relations",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      top relation Rel1 {
                        domain d1 x:X {}
                        domain d2 y:Y {}
                      }
                      relation Rel2 {
                        domain d1 x:X {}
                        domain d2 y:Y {}
                      }
                      relation Rel3 {
                        domain d1 x:X {}
                        domain d2 y:Y {}
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            // mapping
            TestData(
                testName = "top mapping",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      top mapping Map1 {
                        domain d1 x:X {}
                        domain d2 y:Y := A() {}
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            TestData(
                testName = "non top mapping",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      mapping Map1 {
                        domain d1 x:X {}
                        domain d2 y:Y := A() {}
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            TestData(
                testName = "multiple mapping",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      top mapping Map1 {
                        domain d1 x:X {}
                        domain d2 y:Y := A() {}
                      }
                      mapping Map2 {
                        domain d1 x:X {}
                        domain d2 y:Y := A() {}
                      }
                      mapping Map3 {
                        domain d1 x:X {}
                        domain d2 y:Y := A() {}
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            // table
            TestData(
                testName = "top table 2x1",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      top table Rel1 {
                        domain d1 :Int    domain d2 :Int
                        values  1    to   2
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) { namespace("n1") {} }
                val tm2 = typesDomain("D2", true) { namespace("n2") {} }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            TestData(
                testName = "top table 2x2",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      top table Rel1 {
                        domain d1 :Int    domain d2 :String
                        values  1    to   'a'
                        values  2    to   'b'
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) { namespace("n1") {} }
                val tm2 = typesDomain("D2", true) { namespace("n2") {} }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            TestData(
                testName = "top table 3x3",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2, d3:D3) {
                      top table Rel1 {
                        domain d1 :Int    domain d2 :String  domain d3 :Boolean
                        values  1    to   'a'    to           true
                        values  2    to   'b'    to           false
                        values  3    to   'c'    to           true
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val dr3 = DomainReference("d3")
                val tm1 = typesDomain("D1", true) { namespace("n1") {} }
                val tm2 = typesDomain("D2", true) { namespace("n2") {} }
                val tm3 = typesDomain("D3", true) { namespace("n3") {} }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
                typeDomains[dr3] = tm3
            },
            // where
            TestData(
                testName = "where",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      top relation Rel1 {
                        domain d1 x:X {}
                        domain d2 y:Y {}
                        where {
                          Rel2(x,y)
                        }
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            // Templates - primitive
            TestData(
                testName = "Pattern Template literal expression",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      relation Map1 {
                        domain d1 x:String == 'abc'
                        domain d2 y:Int == 3
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            TestData(
                testName = "Pattern Template complex expression",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      relation Map1 {
                        domain d1 x:String == 'abc' + 'def' + 'ghi'
                        domain d2 y:Int == x.y.z.size
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            // Templates - object
            TestData(
                testName = "Pattern Template property literal string ",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      mapping Map1 {
                        domain d1 x:X {
                          prop == 'abc'
                        }
                        domain d2 y:Y := A() {}
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            TestData(
                testName = "Pattern Template property free variable",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      mapping Map1 {
                        domain d1 x:X {
                          prop == v
                        }
                        domain d2 y:Y := A() {}
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            TestData(
                testName = "Pattern Template unnamed object template",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      mapping Map1 {
                        domain d1 x:X {
                          prop == Property { name == 'n' }
                        }
                        domain d2 y:Y := A() {}
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            TestData(
                testName = "Pattern Template named object template",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      mapping Map1 {
                        domain d1 x:X {
                          prop == p:Property { name == 'n' }
                        }
                        domain d2 y:Y := A() {}
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            // collection template
            TestData(
                testName = "Pattern Template collection template empty",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      mapping Map1 {
                        domain d1 x:X {
                          prop == []
                        }
                        domain d2 y:Y := A() {}
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            TestData(
                testName = "Pattern Template collection template complete literals",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      mapping Map1 {
                        domain d1 x:X {
                          prop == ['a' 'b' 'c']
                        }
                        domain d2 y:Y := A() {}
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            TestData(
                testName = "Pattern Template collection template complete objects",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      mapping Map1 {
                        domain d1 x:X {
                          prop == [
                            Object { p==b }
                            o2: Object { p==b }
                            Object { p==b }
                          ]
                        }
                        domain d2 y:Y := A() {}
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
            TestData(
                testName = "Pattern Template collection template subset objects",
                sentence = """
                    namespace test
                    transform Test(d1:D1, d2:D2) {
                      mapping Map1 {
                        domain d1 x:X {
                          prop == [
                            ...
                            o2: Object { p==b }
                            Object { p==b }
                          ]
                        }
                        domain d2 y:Y := A() {}
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },

            // transform-test
            TestData(
                testName = "tests",
                goalRuleName = "testUnit",
                sentence = """
                    namespace test
                    transform-test Test(d1:D1, d2:D2) {
                      test-case TC {
                        domain d1 := X() {}
                        domain d2 := Y() {}
                      }
                    }
                """.trimIndent()
            ).apply {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("D1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("D2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                typeDomains[dr1] = tm1
                typeDomains[dr2] = tm2
            },
        )

        private fun test_parse(data: TestData) {
            val proc = Agl.registry.agl.m2mTransform.processor!!
            val result = proc.parse(
                data.sentence,
                options = Agl.parseOptions {
                    data.goalRuleName?.let { goalRuleName(it) }
                }
            )
            assertTrue(result.issues.errors.isEmpty(), "'${data.sentence}'\n${result.issues}")
        }

        private fun test_process(i:Int) {
            val data = testData[i]
            println()
            println("--- ${data.testName} ---")
            println("Processing '${data.sentence}'")
            val context = SentenceContextAny()
            data.typeDomains.forEach { (k,v) ->
                context.addToScope(null, listOf(v.name.value), QualifiedName("TypesDomain"), null, v)
            }
            val result = Agl.registry.agl.m2mTransform.processor!!.process(
                data.sentence,
                options = Agl.options {
                    parse {
                        data.goalRuleName?.let {
                            goalRuleName(it)
                        }
                    }
                    semanticAnalysis {
                        context(context)
                    }
                }
            )
            assertNotNull(result.asm, result.allIssues.toString())
            assertTrue(result.allIssues.errors.isEmpty(), "'${data.sentence}'\n${result.allIssues}")
            data.expectedAsm.forEachIndexed { idx, it ->

            }
        }

    }

    @Test
    fun check_grammar() {
        val proc = Agl.registry.agl.asmTransform.processor
        assertTrue(Agl.registry.agl.asmTransform.issues.isEmpty(), Agl.registry.agl.asmTransform.issues.toString())
        assertNotNull(proc)
    }

    @Ignore
    @Test
    fun check_typeModel() {
        val actual = Agl.registry.agl.asmTransform.processor!!.typesDomain
        val expected = grammarTypeModel("net.akehurst.language.agl", "AsmTransform") {
            //unit = ruleList ;
            //ruleList = [formatRule]* ;
            //formatRule = typeReference '->' formatExpression ;
            //formatExpression
            // = stringExpression
            // | whenExpression
            // ;
        }

        TypesDomainTest.tmAssertEquals(expected, actual)
    }

    @Test
    fun parse() {
        val processor = Agl.registry.agl.m2mTransform.processor!!
        for (td in testData) {
            println()
            println("--- ${td.testName} ---")
            println("Parsing '${td.sentence}'")
           test_parse(td)
        }
    }

    @Test
    fun process() {
        for (i in testData.indices) {
            test_process(i)
        }
    }

}