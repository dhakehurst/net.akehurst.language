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

package net.akehurst.language.transform.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.expressions.processor.ObjectGraphAsmSimple
import net.akehurst.language.expressions.processor.TypedObjectAsmValue
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.m2mTransform.api.DomainReference
import net.akehurst.language.m2mTransform.processor.M2mTransformInterpreter
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.builder.typeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_transformInterpreter {
    private companion object {
        data class TestData(
            val description: String = "",
        ) {
            val typeDomains = mutableListOf<TypeModel>()
            var transform: String = ""
            val input = mutableMapOf<DomainReference, Asm>()
            val expected = mutableMapOf<DomainReference, Asm>()
        }

        val testSuit = listOf(
            TestData("just a namespace").also {
                it.typeDomains.add(typeModel("Domain1", true) {

                })
                it.typeDomains.add(typeModel("Domain2", true) {

                })
                it.transform = """
                    namespace test
                """
            },
            TestData("just a transform, no rules").also {
                it.typeDomains.add(typeModel("Domain1", true) {

                })
                it.typeDomains.add(typeModel("Domain2", true) {

                })
                it.transform = """
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        
                    }
                """
            },
            TestData("simple mapping").also {
                val d1 = typeModel("Domain1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val d2 = typeModel("Domain2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                it.typeDomains.add(d1)
                it.typeDomains.add(d2)
                it.transform = """
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A12A2 {
                            domain d1 a1:A1 := A1() { prop1 := a2.prop2 }
                            domain d2 a2:A2 := A2() { prop2 := a1.prop1 }
                        }
                    }
                """
                it.input[DomainReference("d1")] = asmSimple(d1) {
                    element("A1") {
                        propertyString("prop1", "value1")
                    }
                }
                it.expected[DomainReference("d2")] = asmSimple(d2) {
                    element("A2") {
                        propertyString("prop2", "value1")
                    }
                }
            },
            TestData("umlRdbms QVT example").also {
                it.typeDomains.add(typeModel("SimpleUML", true) {
                    namespace("uml") {
                        data("Package")
                    }
                })
                it.typeDomains.add(typeModel("SimpleRDBMS", true) {
                    namespace("rdbms") {
                        data("Schema")
                    }
                })
                it.transform = """
                    namespace test
                    transform umlRdbms(uml : SimpleUML, rdbms : SimpleRDBMS) {
                        /* map each package to a schema */
                        top relation PackageToSchema {
                            pivot pn: String
                            domain uml p:Package { name==pn }
                            domain rdbms s:Schema { name==pn }
                        }
                    }
                """.trimIndent()
                it.input[DomainReference("uml")] = asmSimple {
                    element("Package") {}
                }
                it.expected[DomainReference("uml")] = asmSimple {

                }
            }
        )

        fun doTest(testData: TestData) {
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val context = ContextWithScope<Any, Any>()
            testData.typeDomains.forEach {
                context.addToScope(null, listOf(it.name.value), QualifiedName("TypeModel"), null, it)
            }
            val res = Agl.registry.agl.m2mTransform.processor!!.process(
                testData.transform,
                options = Agl.options {
                    semanticAnalysis {
                        context(context)
                    }
                }
            )
            val m2m = res.let {
                check(it.allIssues.errors.isEmpty()) { it.allIssues.toString() }
                it.asm!!
            }
            val ogs = testData.typeDomains.associate {
                it.name to ObjectGraphAsmSimple(it, issues)
            }
            val interpreter = M2mTransformInterpreter(m2m, ogs, issues)

            val srcTypeDomain = testData.typeDomains.first()
            for ((dr, asm) in testData.input) {
                for(obj in asm.root) {
                    val tobj = ogs[srcTypeDomain.name]?.let {
                        val td = srcTypeDomain.findByQualifiedNameOrNull(obj.qualifiedTypeName)!!
                        TypedObjectAsmValue(td.type(),obj)
                    }!!
                    val res = interpreter.transform(dr, tobj)
                    assertTrue(res.issues.isEmpty(), res.issues.toString())
                    assertEquals(testData.expected.size, res.objects.size, "number of outputs is different")
                    for(dr in testData.expected.keys) {
                        val exp = testData.expected[dr]!!
                        val act = res.objects[dr]!!
                        assertEquals(exp.asString(), act.asString())
                    }
                }
            }
        }
    }

    @Test
    fun testAll() {
        testSuit.forEach {
            println("****** ${it.description} ******")
            doTest(it)
        }
    }

    @Test
    fun single() {
        doTest(testSuit[3])
    }
}