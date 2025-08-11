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
import net.akehurst.language.typemodel.api.PropertyCharacteristic
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
            val typeDomains = mutableMapOf<DomainReference,TypeModel>()
            var transform: String = ""
            val input = mutableMapOf<DomainReference, Asm>()
            var target: DomainReference? = null
            val expected = mutableMapOf<DomainReference, Asm>()
        }

        val testSuit = listOf(
            TestData("just a namespace").also {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typeModel("Domain1", true) {}
                val tm2 = typeModel("Domain2", true) {}
                it.typeDomains[dr1] = tm1
                it.typeDomains[dr2] = tm2
                it.transform = """
                    namespace test
                """
                it.target = dr2
            },
            TestData("just a transform, no rules").also {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typeModel("Domain1", true) {}
                val tm2 = typeModel("Domain2", true) {}
                it.typeDomains[dr1] = tm1
                it.typeDomains[dr2] = tm2
                it.transform = """
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        
                    }
                """
                it.target = dr2
            },
            TestData("simple mapping").also {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typeModel("Domain1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typeModel("Domain2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                it.typeDomains[dr1] = tm1
                it.typeDomains[dr2] = tm2
                it.transform = """
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A12A2 {
                            domain d1 a1:A1 := A1() { prop1 := a2.prop2 }
                            domain d2 a2:A2 := A2() { prop2 := a1.prop1 }
                        }
                    }
                """
                it.input[dr1] = asmSimple(tm1) {
                    element("A1") {
                        propertyString("prop1", "value1")
                    }
                }
                it.target = dr2
                it.expected[dr2] = asmSimple(tm2) {
                    element("A2") {
                        propertyString("prop2", "value1")
                    }
                }
            },
            TestData("umlRdbms QVT example").also {
                val dr1 = DomainReference("uml")
                val dr2 = DomainReference("rdbms")
                val tm1 = typeModel("SimpleUML", true) {
                    namespace("uml") {
                        data("UmlModelElement") {
                            propertyOf(setOf(PropertyCharacteristic.READ_WRITE),"name","String")
                        }
                        data("Package") {
                            supertypes("UmlModelElement")
                            propertyOf(setOf(PropertyCharacteristic.READ_WRITE),"elements","Set") {
                                typeArgument("PackageElement")
                            }
                        }
                        data("Attribute") {
                            supertypes("UmlModelElement")
                            propertyOf(setOf(PropertyCharacteristic.READ_WRITE),"type","Classifier")
                            propertyOf(setOf(PropertyCharacteristic.READ_WRITE),"owner","Class")
                        }
                        data("PackageElement") {
                            supertypes("UmlModelElement")
                            propertyOf(setOf(PropertyCharacteristic.READ_WRITE),"namespace","Package")
                        }
                        data("Classifier") {
                            supertypes("PackageElement")

                        }
                        data("Class") {
                            supertypes("Classifier")

                        }
                        data("PrimitiveDataType") {
                            supertypes("Classifier")

                        }
                        data("Association") {
                            supertypes("PackageElement")

                        }
                    }
                }
                val tm2 = typeModel("SimpleRDBMS", true) {
                    namespace("rdbms") {
                        data("Schema") {
                            propertyOf(setOf(PropertyCharacteristic.READ_WRITE),"name","String")
                        }
                    }
                }
                it.typeDomains[dr1] = tm1
                it.typeDomains[dr2] = tm2
                it.transform = """
                    namespace test
                    transform umlRdbms(uml : SimpleUML, rdbms : SimpleRDBMS) {
                        /* map each package to a schema */
                        top relation PackageToSchema {
                            pivot pn: String
                            domain uml p:Package { name==pn }
                            domain rdbms s:Schema { name==pn }
                        }
                    }
                """.trimIndent()
                it.input[dr1] = asmSimple(tm1) {
                    element("Package") {
                        propertyString("name", "pkg1")
                    }
                }
                it.target = dr2
                it.expected[dr2] = asmSimple(tm2) {
                    element("Schema") {
                        propertyString("name", "pkg1")
                    }
                }
            }
        )

        fun doTest(testData: TestData) {
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val context = ContextWithScope<Any, Any>()
            testData.typeDomains.forEach { (k,v) ->
                context.addToScope(null, listOf(v.name.value), QualifiedName("TypeModel"), null, v)
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
            val ogs = testData.typeDomains.entries.associate { (k,v) ->
                Pair(v.name , ObjectGraphAsmSimple(v, issues))
            }
            val interpreter = M2mTransformInterpreter(m2m, ogs, issues)

            val source = testData.input.entries.associate { (k,v) ->
                //TODO: for all roots!
                val obj = v.root.first()
                val srcTypeDomain = testData.typeDomains[k]!!
                val tobj = ogs[srcTypeDomain.name]?.let {
                    val td = srcTypeDomain.findByQualifiedNameOrNull(obj.qualifiedTypeName) ?: error("Can't find type ${obj.qualifiedTypeName}")
                    TypedObjectAsmValue(td.type(),obj)
                }!!
                Pair(k,tobj)
            }
            val trRes = interpreter.transform(testData.target!!, source)
            assertTrue(trRes.issues.isEmpty(), trRes.issues.toString())
            assertEquals(testData.expected.size, trRes.objects.size, "number of outputs is different")
            for(dr in testData.expected.keys) {
                val exp = testData.expected[dr]!!
                val act = trRes.objects[dr]!!
                assertEquals(exp.asString(), act.asString())
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