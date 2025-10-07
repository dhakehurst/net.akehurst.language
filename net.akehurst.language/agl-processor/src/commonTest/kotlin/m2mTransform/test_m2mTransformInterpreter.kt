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
import net.akehurst.language.types.api.PropertyCharacteristic
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_m2mTransformInterpreter {
    private companion object Companion {
        data class TestData(
            val description: String = "",
        ) {
            val typeDomains = mutableMapOf<DomainReference,TypesDomain>()
            var transform: String = ""
            val input = mutableMapOf<DomainReference, Asm>()
            var target: DomainReference? = null
            val expected = mutableMapOf<DomainReference, Asm>()
        }

        val testSuit = listOf(
//            TestData("just a namespace").also {
//                val dr1 = DomainReference("d1")
//                val dr2 = DomainReference("d2")
//                val tm1 = typeModel("Domain1", true) {}
//                val tm2 = typeModel("Domain2", true) {}
//                it.typeDomains[dr1] = tm1
//                it.typeDomains[dr2] = tm2
//                it.transform = """
//                    namespace test
//                """
//                it.target = dr2
//            },
//            TestData("just a transform, no rules").also {
//                val dr1 = DomainReference("d1")
//                val dr2 = DomainReference("d2")
//                val tm1 = typeModel("Domain1", true) {}
//                val tm2 = typeModel("Domain2", true) {}
//                it.typeDomains[dr1] = tm1
//                it.typeDomains[dr2] = tm2
//                it.transform = """
//                    namespace test
//                    transform Test(d1:Domain1, d2:Domain2) {
//
//                    }
//                """
//                it.target = dr2
//            },
            TestData("simple mapping").also {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("Domain1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("Domain2", true) {
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
            TestData("simple mapping one way").also {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("Domain1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                val tm2 = typesDomain("Domain2", true) {
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
                            domain d1 a1:A1
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
            TestData("umlRdbms QVT example - PackageToSchema").also {
                val dr1 = DomainReference("uml")
                val dr2 = DomainReference("rdbms")
                val tm1 = typesDomain("SimpleUML", true) {
                    namespace("uml") {
                        data("UmlModelElement") {
                            propertyOf(setOf(CMP, VAR),"name","String")
                        }
                        data("Package") {
                            supertypes("UmlModelElement")
                        }
                        data("Attribute") {
                            supertypes("UmlModelElement")
                        }
                        data("PackageElement") {
                            supertypes("UmlModelElement")
                        }
                        data("Classifier") {
                            supertypes("PackageElement")
                        }
                        data("Class") {
                            supertypes("Classifier")
                            propertyOf(setOf(CMP, VAR),"kind","String")
                        }
                        data("PrimitiveDataType") {
                            supertypes("Classifier")

                        }
                        data("Association") {
                            supertypes("PackageElement")
                        }
                        association {
                            end("Package",setOf(REF,VAR),"namespace")
                            end("PackageElement",setOf(CMP, VAR),"elements", false, "Set")
                        }
                        association {
                            end("Classifier",setOf(REF,VAR),"type")
                            end("Attribute",setOf(REF,VAR),"typeOpposite", false,"Set")
                        }
                        association {
                            end("Class",setOf(REF,VAR),"owner")
                            end("Attribute",setOf(CMP,VAR),"attribute", false,"Set")
                        }
                        association {
                            end("Class",setOf(REF,VAR),"general", false,"Set")
                            end("Class",setOf(REF,VAR),"generalOpposite", false,"Set")
                        }
                        association {
                            end("Class",setOf(REF,VAR),"source")
                            end("Association",setOf(REF,VAR),"reverse", false,"Set")
                        }
                        association {
                            end("Class",setOf(REF,VAR),"destination")
                            end("Association",setOf(REF,VAR),"forward", false,"Set")
                        }
                    }
                }
                val tm2 = typesDomain("SimpleRDBMS", true) {
                    namespace("rdbms") {
                        data("RModelElement") {
                            propertyOf(setOf(PropertyCharacteristic.READ_WRITE),"name","String")
                        }
                        data("Schema") {
                            supertypes("RModelElement")
                        }
                        data("Table") {
                            supertypes("RModelElement")
                        }
                        data("Column") {
                            supertypes("RModelElement")
                        }
                        data("Key") {
                            supertypes("RModelElement")
                        }
                        data("ForeignKey") {
                            supertypes("RModelElement")
                        }
                        association {
                            end("Schema",setOf(REF,VAR),"schema")
                            end("Table",setOf(CMP,VAR),"tables", false,"Set")
                        }
                        association {
                            end("Table",setOf(REF,VAR),"owner")
                            end("Column",setOf(CMP,VAR),"column", false,"Set")
                        }
                        association {
                            end("Table",setOf(REF,VAR),"owner")
                            end("Key",setOf(CMP,VAR),"key", false,"Set")
                        }
                        association {
                            end("Table",setOf(REF,VAR),"owner")
                            end("ForeignKey",setOf(CMP,VAR),"foreignKey", false,"Set")
                        }
                        association {
                            end("Key",setOf(REF,VAR),"refersTo", false,"Set")
                            //TODO: refersToOpposite is not navigable!
                            end("ForeignKey",setOf(REF,VAR),"refersToOpposite", false,"Set")
                        }
                        association {
                            end("Column",setOf(REF,VAR),"column", false,"Set")
                            end("ForeignKey",setOf(REF,VAR),"foreignKey", false,"Set")
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
            },
            TestData("umlRdbms QVT example - PrimitiveUmlTypeToSqlType").also {
                val dr1 = DomainReference("uml")
                val dr2 = DomainReference("rdbms")
                val tm1 = typesDomain("SimpleUML", true) {
                    namespace("uml") {
                        data("UmlModelElement") {
                            propertyOf(setOf(CMP, VAR),"name","String")
                        }
                        data("Package") {
                            supertypes("UmlModelElement")
                        }
                        data("Attribute") {
                            supertypes("UmlModelElement")
                        }
                        data("PackageElement") {
                            supertypes("UmlModelElement")
                        }
                        data("Classifier") {
                            supertypes("PackageElement")
                        }
                        data("Class") {
                            supertypes("Classifier")
                            propertyOf(setOf(CMP, VAR),"kind","String")
                        }
                        data("PrimitiveDataType") {
                            supertypes("Classifier")

                        }
                        data("Association") {
                            supertypes("PackageElement")
                        }
                        association {
                            end("Package",setOf(REF,VAR),"namespace")
                            end("PackageElement",setOf(CMP, VAR),"elements", false, "Set")
                        }
                        association {
                            end("Classifier",setOf(REF,VAR),"type")
                            end("Attribute",setOf(REF,VAR),"typeOpposite", false,"Set")
                        }
                        association {
                            end("Class",setOf(REF,VAR),"owner")
                            end("Attribute",setOf(CMP,VAR),"attribute", false,"Set")
                        }
                        association {
                            end("Class",setOf(REF,VAR),"general", false,"Set")
                            end("Class",setOf(REF,VAR),"generalOpposite", false,"Set")
                        }
                        association {
                            end("Class",setOf(REF,VAR),"source")
                            end("Association",setOf(REF,VAR),"reverse", false,"Set")
                        }
                        association {
                            end("Class",setOf(REF,VAR),"destination")
                            end("Association",setOf(REF,VAR),"forward", false,"Set")
                        }
                    }
                }
                val tm2 = typesDomain("SimpleRDBMS", true) {
                    namespace("rdbms") {
                        data("RModelElement") {
                            propertyOf(setOf(PropertyCharacteristic.READ_WRITE),"name","String")
                        }
                        data("Schema") {
                            supertypes("RModelElement")
                        }
                        data("Table") {
                            supertypes("RModelElement")
                        }
                        data("Column") {
                            supertypes("RModelElement")
                        }
                        data("Key") {
                            supertypes("RModelElement")
                        }
                        data("ForeignKey") {
                            supertypes("RModelElement")
                        }
                        association {
                            end("Schema",setOf(REF,VAR),"schema")
                            end("Table",setOf(CMP,VAR),"tables", false,"Set")
                        }
                        association {
                            end("Table",setOf(REF,VAR),"owner")
                            end("Column",setOf(CMP,VAR),"column", false,"Set")
                        }
                        association {
                            end("Table",setOf(REF,VAR),"owner")
                            end("Key",setOf(CMP,VAR),"key", false,"Set")
                        }
                        association {
                            end("Table",setOf(REF,VAR),"owner")
                            end("ForeignKey",setOf(CMP,VAR),"foreignKey", false,"Set")
                        }
                        association {
                            end("Key",setOf(REF,VAR),"refersTo", false,"Set")
                            //TODO: refersToOpposite is not navigable!
                            end("ForeignKey",setOf(REF,VAR),"refersToOpposite", false,"Set")
                        }
                        association {
                            end("Column",setOf(REF,VAR),"column", false,"Set")
                            end("ForeignKey",setOf(REF,VAR),"foreignKey", false,"Set")
                        }
                    }
                }
                it.typeDomains[dr1] = tm1
                it.typeDomains[dr2] = tm2
                it.transform = """
                    namespace test
                    transform umlRdbms(uml : SimpleUML, rdbms : SimpleRDBMS) {
                        abstract top relation PrimitiveUmlTypeToSqlType {
                            domain uml pt:PrimitiveDataType
                            domain rdbms ct:String
                        }
                        relation PrimitiveUmlTypeToSqlTypeBoolean : PrimitiveUmlTypeToSqlType {
                            domain uml pt:PrimitiveDataType{ name == 'Boolean'}
                            domain rdbms ct:String == 'BOOLEAN'
                        }
                        relation PrimitiveUmlTypeToSqlTypeInteger : PrimitiveUmlTypeToSqlType {
                            domain uml pt:PrimitiveDataType{ name == 'Int'}
                            domain rdbms ct:String == 'NUMBER'
                        }
                        relation PrimitiveUmlTypeToSqlTypeBoolean : PrimitiveUmlTypeToSqlType {
                            domain uml pt:PrimitiveDataType{ name == 'String'}
                            domain rdbms ct:String == 'VARCHAR'
                        }
                    }
                """.trimIndent()
                it.input[dr1] = asmSimple(tm1) {
                    element("PrimitiveDataType") {
                        propertyString("name", "Boolean")
                    }
                }
                it.target = dr2
                it.expected[dr2] = asmSimple(tm2) {
                    string("BOOLEAN")
                }
            },
            TestData("umlRdbms QVT example").also {
                val dr1 = DomainReference("uml")
                val dr2 = DomainReference("rdbms")
                val tm1 = typesDomain("SimpleUML", true) {
                    namespace("uml") {
                        data("UmlModelElement") {
                            propertyOf(setOf(CMP, VAR),"name","String")
                        }
                        data("Package") {
                            supertypes("UmlModelElement")
                        }
                        data("Attribute") {
                            supertypes("UmlModelElement")
                       }
                        data("PackageElement") {
                            supertypes("UmlModelElement")
                        }
                        data("Classifier") {
                            supertypes("PackageElement")
                        }
                        data("Class") {
                            supertypes("Classifier")
                            propertyOf(setOf(CMP, VAR),"kind","String")
                        }
                        data("PrimitiveDataType") {
                            supertypes("Classifier")

                        }
                        data("Association") {
                            supertypes("PackageElement")
                        }
                        association {
                            end("Package",setOf(REF,VAR),"namespace")
                            end("PackageElement",setOf(CMP, VAR),"elements", false, "Set")
                        }
                        association {
                            end("Classifier",setOf(REF,VAR),"type")
                            end("Attribute",setOf(REF,VAR),"typeOpposite", false,"Set")
                        }
                        association {
                            end("Class",setOf(REF,VAR),"owner")
                            end("Attribute",setOf(CMP,VAR),"attribute", false,"Set")
                        }
                        association {
                            end("Class",setOf(REF,VAR),"general", false,"Set")
                            end("Class",setOf(REF,VAR),"generalOpposite", false,"Set")
                        }
                        association {
                            end("Class",setOf(REF,VAR),"source")
                            end("Association",setOf(REF,VAR),"reverse", false,"Set")
                        }
                        association {
                            end("Class",setOf(REF,VAR),"destination")
                            end("Association",setOf(REF,VAR),"forward", false,"Set")
                        }
                    }
                }
                val tm2 = typesDomain("SimpleRDBMS", true) {
                    namespace("rdbms") {
                        data("RModelElement") {
                            propertyOf(setOf(PropertyCharacteristic.READ_WRITE),"name","String")
                        }
                        data("Schema") {
                            supertypes("RModelElement")
                        }
                        data("Table") {
                            supertypes("RModelElement")
                        }
                        data("Column") {
                            supertypes("RModelElement")
                        }
                        data("Key") {
                            supertypes("RModelElement")
                        }
                        data("ForeignKey") {
                            supertypes("RModelElement")
                        }
                        association {
                            end("Schema",setOf(REF,VAR),"schema")
                            end("Table",setOf(CMP,VAR),"tables", false,"Set")
                        }
                        association {
                            end("Table",setOf(REF,VAR),"owner")
                            end("Column",setOf(CMP,VAR),"column", false,"Set")
                        }
                        association {
                            end("Table",setOf(REF,VAR),"owner")
                            end("Key",setOf(CMP,VAR),"key", false,"Set")
                        }
                        association {
                            end("Table",setOf(REF,VAR),"owner")
                            end("ForeignKey",setOf(CMP,VAR),"foreignKey", false,"Set")
                        }
                        association {
                            end("Key",setOf(REF,VAR),"refersTo", false,"Set")
                            //TODO: refersToOpposite is not navigable!
                            end("ForeignKey",setOf(REF,VAR),"refersToOpposite", false,"Set")
                        }
                        association {
                            end("Column",setOf(REF,VAR),"column", false,"Set")
                            end("ForeignKey",setOf(REF,VAR),"foreignKey", false,"Set")
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
                            where {
                                ClassToTable(p.elements, s.table)
                            }
                        }
                        relation ClassToTable {
                            pivot cn: String
                            pivot prefix:String
                            domain uml c:Class {
                                namespace==p:Package
                                kind=='Persistent'
                                name==cn
                            }
                            domain rdbms t:Table {
                                schema==s:Schema
                                name==cn
                                column==cl:Column {
                                    name=='_id'
                                    type=='NUMBER'
                                }
                                key==k:Key {
                                    name=='_pk'
                                    column==cl
                                    kind==’primary’
                                }
                            }
                            when { PackageToSchema(p, s) }
                            where {
                                AttributeToColumn(c.attribute, t.column)
                            }
                        }
                        abstract relation AttributeToColumn {
                            domain uml a:Attribute
                            domain rdbms c:Column
                        }
                        relation AttributeToColumnPrimitive {
                            pivot n:String
                            domain uml a:Attribute {
                                name==n
                                type==at:PrimitiveDataType
                            }
                            domain rdbms c:Column {
                                name==n
                                type==ct:String
                            }
                            where {
                                PrimitiveUmlTypeToSqlType(at, ct)
                            }
                        }
                        relation AttributeToColumnComplex {
                            pivot n:String
                            domain uml a:Attribute {
                                name==n
                                type==at:Class
                            }
                            domain rdbms c:Column {
                                name==n
                                type=='NUMBER'
                            }
                            where {
                                ComplexUmlTypeToSqlType(at, ct)
                            }
                        }                        
                        abstract relation PrimitiveUmlTypeToSqlType {
                            domain uml pt:PrimitiveDataType
                            domain rdbms ct:String
                        }
                        relation PrimitiveUmlTypeToSqlTypeBoolean : PrimitiveUmlTypeToSqlType {
                            domain uml pt:PrimitiveDataType{ name == 'Boolean'}
                            domain rdbms ct:String == 'BOOLEAN'
                        }
                        relation PrimitiveUmlTypeToSqlTypeInteger : PrimitiveUmlTypeToSqlType {
                            domain uml pt:PrimitiveDataType{ name == 'Int'}
                            domain rdbms ct:String == 'NUMBER'
                        }
                        relation PrimitiveUmlTypeToSqlTypeBoolean : PrimitiveUmlTypeToSqlType {
                            domain uml pt:PrimitiveDataType{ name == 'String'}
                            domain rdbms ct:String == 'VARCHAR'
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
            testData.typeDomains.forEach { (k,v) ->
                println(v.asString())
            }
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val context = ContextWithScope<Any, Any>()
            testData.typeDomains.forEach { (k,v) ->
                context.addToScope(null, listOf(v.name.value), QualifiedName("TypesDomain"), null, v)
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
                println("Result domain ${dr.value} is ${act.asString()}")
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
        doTest(testSuit[1])
    }
}