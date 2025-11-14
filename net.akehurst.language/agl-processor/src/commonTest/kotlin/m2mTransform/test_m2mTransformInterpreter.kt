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
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorAsmSimple
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
            val typeDomains = mutableMapOf<DomainReference, TypesDomain>()
            var transform: String = ""
            val input = mutableMapOf<DomainReference, Asm>()
            var target: DomainReference? = null
            var expected: Asm? = null
        }

        val testSuit = listOf(
            TestData("1 matching top mapping to 1 String input 1 gives literal String result").also {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("Domain1", true) {
                    namespace("n1") {
                    }
                }
                val tm2 = typesDomain("Domain2", true) {
                    namespace("n2") {
                    }
                }
                it.typeDomains[dr1] = tm1
                it.typeDomains[dr2] = tm2
                it.transform = $$"""
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A12A2 {
                            domain d1 a1:String == 'Any'
                            domain d2 a2:String := 'Hello World!'
                        }
                    }
                """
                it.input[dr1] = asmSimple(tm1) {
                    string("Any")
                }
                it.target = dr2
                it.expected = asmSimple(tm2) {
                    string("Hello World!")
                }
            },
            TestData("1 matching top mapping to 2 String input 2 gives 2 literal String result").also {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("Domain1", true) {
                    namespace("n1") {
                    }
                }
                val tm2 = typesDomain("Domain2", true) {
                    namespace("n2") {
                    }
                }
                it.typeDomains[dr1] = tm1
                it.typeDomains[dr2] = tm2
                it.transform = """
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A12A2 {
                            domain d1 a1:String {}
                            domain d2 a2:String := 'Hello World! ' + a1
                        }
                    }
                """
                it.input[dr1] = asmSimple(tm1) {
                    string("Any1")
                    string("Any2")
                }
                it.target = dr2
                it.expected = asmSimple(tm2) {
                    string("Hello World! Any1")
                    string("Hello World! Any2")
                }
            },
            TestData("2 matching top mapping to 2 String input 2 give 4 results").also {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("Domain1", true) {
                    namespace("n1") {
                    }
                }
                val tm2 = typesDomain("Domain2", true) {
                    namespace("n2") {
                    }
                }
                it.typeDomains[dr1] = tm1
                it.typeDomains[dr2] = tm2
                it.transform = """
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A {
                            domain d1 a1:String {}
                            domain d2 a2:String := 'Hello World! from A '+a1
                        }
                        top mapping B {
                            domain d1 a1:String {}
                            domain d2 a2:String := 'Hello World! from B '+a1
                        }
                    }
                """
                it.input[dr1] = asmSimple(tm1) {
                    string("Any1")
                    string("Any2")
                }
                it.target = dr2
                it.expected = asmSimple(tm2) {
                    string("Hello World! from A Any1")
                    string("Hello World! from A Any2")
                    string("Hello World! from B Any1")
                    string("Hello World! from B Any2")
                }
            },
            TestData("2 matching top mapping to one of each 2 String input 2 gives 2 literal String result").also {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("Domain1", true) {
                    namespace("n1") {
                    }
                }
                val tm2 = typesDomain("Domain2", true) {
                    namespace("n2") {
                    }
                }
                it.typeDomains[dr1] = tm1
                it.typeDomains[dr2] = tm2
                it.transform = """
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A {
                            domain d1 a1:String == 'A'
                            domain d2 a2:String := 'Hello World A!'
                        }
                        top mapping B {
                            domain d1 a1:String == 'B'
                            domain d2 a2:String := 'Hello World B!'
                        }
                    }
                """
                it.input[dr1] = asmSimple(tm1) {
                    string("A")
                    string("B")
                }
                it.target = dr2
                it.expected = asmSimple(tm2) {
                    string("Hello World A!")
                    string("Hello World B!")
                }
            },
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
                            domain d1 a1:A1 { prop1 == v }
                            domain d2 a2:A2 := A2() { prop2 := v }
                        }
                    }
                """
                it.input[dr1] = asmSimple(tm1) {
                    element("A1") {
                        propertyString("prop1", "value1")
                    }
                }
                it.target = dr2
                it.expected = asmSimple(tm2) {
                    element("A2") {
                        propertyString("prop2", "value1")
                    }
                }
            },
            TestData("simple mapping set from navigation").also {
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
                            domain d1 a1:A1 {}
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
                it.expected = asmSimple(tm2) {
                    element("A2") {
                        propertyString("prop2", "value1")
                    }
                }
            },
            TestData("nested match").also {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("Domain1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "B1")
                        }
                        data("B1") {
                            propertyOf(emptySet(), "prop1", "C1")
                        }
                        data("C1") {
                            propertyOf(emptySet(), "prop3", "String")
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
                            domain d1 a1:A1 {
                               prop1 == B1 {
                                 prop2 == C1 {
                                   prop3 == v
                                 }
                               }
                            }
                            domain d2 a2:A2 := A2() { prop2 := v }
                        }
                    }
                """
                it.input[dr1] = asmSimple(tm1) {
                    element("A1") {
                        propertyElementExplicitType("prop1", "B1") {
                            propertyElementExplicitType("prop2", "C1") {
                                propertyString("prop3", "value")
                            }
                        }
                    }
                }
                it.target = dr2
                it.expected = asmSimple(tm2) {
                    element("A2") {
                        propertyString("prop2", "value")
                    }
                }
            },
            // collections
            TestData("match 1 from Set").also {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("Domain1", true) {
                    namespace("n1") {
                        data("Class") {
                            propertyOf(emptySet(), "name", "String")
                            propertyOf(emptySet(), "kind", "String")
                        }
                        data("Attribute") {
                            propertyOf(emptySet(), "name", "String")
                        }
                        association {
                            end("Class", emptySet(), "class")
                            end("Attribute", emptySet(), "attribtute")
                        }
                    }
                }
                val tm2 = typesDomain("Domain2", true) {
                    namespace("n2") {
                    }
                }
                it.typeDomains[dr1] = tm1
                it.typeDomains[dr2] = tm2
                it.transform = """
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A12A2 {
                            domain d1 c1:Class {
                               name == X
                               kind == 'P'
                               attribute == [
                                 ...
                                 a1:Attribute {
                                   name == 'a1'
                                 }
                               ]
                            }
                            domain d2 a2:String := X + ' contains a1'
                        }
                    }
                """
                it.input[dr1] = asmSimple(tm1) {
                    element("Class") {
                        propertyString("name", "c1")
                        propertyString("kind", "P")
                        propertyListOfElement("attribute") {
                            element("Attribute") {
                                propertyString("name", "a1")
                            }
                            element("Attribute") {
                                propertyString("name", "a2")
                            }
                        }
                    }
                    element("Class") {
                        propertyString("name", "c2")
                        propertyString("kind", "T")
                        propertyListOfElement("attribute") {
                            element("Attribute") {
                                propertyString("name", "a3")
                            }
                            element("Attribute") {
                                propertyString("name", "a4")
                            }
                        }
                    }
                }
                it.target = dr2
                it.expected = asmSimple(tm2) {
                    string("c1 contains a1")
                }
            },
            TestData("match 2 from Set").also {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("Domain1", true) {
                    namespace("n1") {
                        data("Class") {
                            propertyOf(emptySet(), "name", "String")
                            propertyOf(emptySet(), "kind", "String")
                        }
                        data("Attribute") {
                            propertyOf(emptySet(), "name", "String")
                        }
                        association {
                            end("Class", emptySet(), "class")
                            end("Attribute", emptySet(), "attribtute")
                        }
                    }
                }
                val tm2 = typesDomain("Domain2", true) {
                    namespace("n2") {
                    }
                }
                it.typeDomains[dr1] = tm1
                it.typeDomains[dr2] = tm2
                it.transform = """
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A12A2 {
                            domain d1 c1:Class {
                               name == X
                               kind == 'P'
                               attribute == [
                                 ...
                                 Attribute {
                                   name == Y
                                 }
                               ]
                            }
                            domain d2 a2:String := X + ' contains ' + Y
                        }
                    }
                """
                it.input[dr1] = asmSimple(tm1) {
                    element("Class") {
                        propertyString("name", "c1")
                        propertyString("kind", "P")
                        propertyListOfElement("attribute") {
                            element("Attribute") {
                                propertyString("name", "a1")
                            }
                            element("Attribute") {
                                propertyString("name", "a2")
                            }
                        }
                    }
                    element("Class") {
                        propertyString("name", "c2")
                        propertyString("kind", "T")
                        propertyListOfElement("attribute") {
                            element("Attribute") {
                                propertyString("name", "a3")
                            }
                            element("Attribute") {
                                propertyString("name", "a4")
                            }
                        }
                    }
                }
                it.target = dr2
                it.expected = asmSimple(tm2) {
                    string("c1 contains a1")
                    string("c1 contains a2")
                }
            },
            TestData("match exact Collection").also {
                val dr1 = DomainReference("d1")
                val dr2 = DomainReference("d2")
                val tm1 = typesDomain("Domain1", true) {
                    namespace("n1") {
                        data("Class") {
                            propertyOf(emptySet(), "name", "String")
                            propertyOf(emptySet(), "kind", "String")
                        }
                        data("Attribute") {
                            propertyOf(emptySet(), "name", "String")
                        }
                        association {
                            end("Class", emptySet(), "class")
                            end("Attribute", emptySet(), "attribtute")
                        }
                    }
                }
                val tm2 = typesDomain("Domain2", true) {
                    namespace("n2") {
                    }
                }
                it.typeDomains[dr1] = tm1
                it.typeDomains[dr2] = tm2
                it.transform = """
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A12A2 {
                            domain d1 c1:Class {
                               name == X
                               attribute == [
                                 a3:Attribute {
                                   name == 'a3'
                                 }
                                 a4:Attribute {
                                   name == 'a4'
                                 }
                               ]
                            }
                            domain d2 a2:String := c1.name + a3.name + a4.name
                        }
                    }
                """
                it.input[dr1] = asmSimple(tm1) {
                    element("Class") {
                        propertyString("name", "c1")
                        propertyString("kind", "P")
                        propertyListOfElement("attribute") {
                            element("Attribute") {
                                propertyString("name", "a1")
                            }
                            element("Attribute") {
                                propertyString("name", "a2")
                            }
                        }
                    }
                    element("Class") {
                        propertyString("name", "c2")
                        propertyString("kind", "T")
                        propertyListOfElement("attribute") {
                            element("Attribute") {
                                propertyString("name", "a3")
                            }
                            element("Attribute") {
                                propertyString("name", "a4")
                            }
                        }
                    }
                }
                it.target = dr2
                it.expected = asmSimple(tm2) {
                    string("c2a3a4")
                }
            },
            // qvt example
            TestData("umlRdbms QVT example - PackageToSchema").also {
                val dr1 = DomainReference("uml")
                val dr2 = DomainReference("rdbms")
                val tm1 = typesDomain("SimpleUML", true) {
                    namespace("uml") {
                        data("UmlModelElement") {
                            propertyOf(setOf(CMP, VAR), "name", "String")
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
                            propertyOf(setOf(CMP, VAR), "kind", "String")
                        }
                        data("PrimitiveDataType") {
                            supertypes("Classifier")

                        }
                        data("Association") {
                            supertypes("PackageElement")
                        }
                        association {
                            end("Package", setOf(REF, VAR), "namespace")
                            end("PackageElement", setOf(CMP, VAR), "elements", false, "Set")
                        }
                        association {
                            end("Classifier", setOf(REF, VAR), "type")
                            end("Attribute", setOf(REF, VAR), "typeOpposite", false, "Set")
                        }
                        association {
                            end("Class", setOf(REF, VAR), "owner")
                            end("Attribute", setOf(CMP, VAR), "attribute", false, "Set")
                        }
                        association {
                            end("Class", setOf(REF, VAR), "general", false, "Set")
                            end("Class", setOf(REF, VAR), "generalOpposite", false, "Set")
                        }
                        association {
                            end("Class", setOf(REF, VAR), "source")
                            end("Association", setOf(REF, VAR), "reverse", false, "Set")
                        }
                        association {
                            end("Class", setOf(REF, VAR), "destination")
                            end("Association", setOf(REF, VAR), "forward", false, "Set")
                        }
                    }
                }
                val tm2 = typesDomain("SimpleRDBMS", true) {
                    namespace("rdbms") {
                        data("RModelElement") {
                            propertyOf(setOf(PropertyCharacteristic.READ_WRITE), "name", "String")
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
                            end("Schema", setOf(REF, VAR), "schema")
                            end("Table", setOf(CMP, VAR), "tables", false, "Set")
                        }
                        association {
                            end("Table", setOf(REF, VAR), "owner")
                            end("Column", setOf(CMP, VAR), "column", false, "Set")
                        }
                        association {
                            end("Table", setOf(REF, VAR), "owner")
                            end("Key", setOf(CMP, VAR), "key", false, "Set")
                        }
                        association {
                            end("Table", setOf(REF, VAR), "owner")
                            end("ForeignKey", setOf(CMP, VAR), "foreignKey", false, "Set")
                        }
                        association {
                            end("Key", setOf(REF, VAR), "refersTo", false, "Set")
                            //TODO: refersToOpposite is not navigable!
                            end("ForeignKey", setOf(REF, VAR), "refersToOpposite", false, "Set")
                        }
                        association {
                            end("Column", setOf(REF, VAR), "column", false, "Set")
                            end("ForeignKey", setOf(REF, VAR), "foreignKey", false, "Set")
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
                it.expected = asmSimple(tm2) {
                    element("Schema") {
                        propertyString("name", "pkg1")
                    }
                }
            },
            TestData("umlRdbms QVT example - top table PrimitiveUmlTypeToSqlType").also {
                val dr1 = DomainReference("uml")
                val dr2 = DomainReference("rdbms")
                val tm1 = typesDomain("SimpleUML", true) {
                    namespace("uml") {
                        data("UmlModelElement") {
                            propertyOf(setOf(CMP, VAR), "name", "String")
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
                            propertyOf(setOf(CMP, VAR), "kind", "String")
                        }
                        data("PrimitiveDataType") {
                            supertypes("Classifier")

                        }
                        data("Association") {
                            supertypes("PackageElement")
                        }
                        association {
                            end("Package", setOf(REF, VAR), "namespace")
                            end("PackageElement", setOf(CMP, VAR), "elements", false, "Set")
                        }
                        association {
                            end("Classifier", setOf(REF, VAR), "type")
                            end("Attribute", setOf(REF, VAR), "typeOpposite", false, "Set")
                        }
                        association {
                            end("Class", setOf(REF, VAR), "owner")
                            end("Attribute", setOf(CMP, VAR), "attribute", false, "Set")
                        }
                        association {
                            end("Class", setOf(REF, VAR), "general", false, "Set")
                            end("Class", setOf(REF, VAR), "generalOpposite", false, "Set")
                        }
                        association {
                            end("Class", setOf(REF, VAR), "source")
                            end("Association", setOf(REF, VAR), "reverse", false, "Set")
                        }
                        association {
                            end("Class", setOf(REF, VAR), "destination")
                            end("Association", setOf(REF, VAR), "forward", false, "Set")
                        }
                    }
                }
                val tm2 = typesDomain("SimpleRDBMS", true) {
                    namespace("rdbms") {
                        data("RModelElement") {
                            propertyOf(setOf(PropertyCharacteristic.READ_WRITE), "name", "String")
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
                            end("Schema", setOf(REF, VAR), "schema")
                            end("Table", setOf(CMP, VAR), "tables", false, "Set")
                        }
                        association {
                            end("Table", setOf(REF, VAR), "owner")
                            end("Column", setOf(CMP, VAR), "column", false, "Set")
                        }
                        association {
                            end("Table", setOf(REF, VAR), "owner")
                            end("Key", setOf(CMP, VAR), "key", false, "Set")
                        }
                        association {
                            end("Table", setOf(REF, VAR), "owner")
                            end("ForeignKey", setOf(CMP, VAR), "foreignKey", false, "Set")
                        }
                        association {
                            end("Key", setOf(REF, VAR), "refersTo", false, "Set")
                            //TODO: refersToOpposite is not navigable!
                            end("ForeignKey", setOf(REF, VAR), "refersToOpposite", false, "Set")
                        }
                        association {
                            end("Column", setOf(REF, VAR), "column", false, "Set")
                            end("ForeignKey", setOf(REF, VAR), "foreignKey", false, "Set")
                        }
                    }
                }
                it.typeDomains[dr1] = tm1
                it.typeDomains[dr2] = tm2
                it.transform = """
                    namespace test
                    transform umlRdbms(uml : SimpleUML, rdbms : SimpleRDBMS) {
                        top table PrimitiveUmlTypeToSqlType {
                            domain  uml :PrimitiveDataType                /**/ domain rdbms :String
                            /*===================================================================*/ 
                            values PrimitiveDataType(){ name := 'Int'}     to  'NUMBER'
                            values PrimitiveDataType(){ name := 'Boolean'} to  'BOOLEAN'
                            values PrimitiveDataType(){ name := 'String'}  to  'VARCHAR'
                        }
                    }
                """.trimIndent()
                it.input[dr1] = asmSimple(tm1) {
                    element("PrimitiveDataType") {
                        propertyString("name", "Boolean")
                    }
                }
                it.target = dr2
                it.expected = asmSimple(tm2) {
                    string("BOOLEAN")
                }
            },
            TestData("umlRdbms QVT example - abstract PrimitiveUmlTypeToSqlType").also {
                val dr1 = DomainReference("uml")
                val dr2 = DomainReference("rdbms")
                val tm1 = typesDomain("SimpleUML", true) {
                    namespace("uml") {
                        data("UmlModelElement") {
                            propertyOf(setOf(CMP, VAR), "name", "String")
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
                            propertyOf(setOf(CMP, VAR), "kind", "String")
                        }
                        data("PrimitiveDataType") {
                            supertypes("Classifier")

                        }
                        data("Association") {
                            supertypes("PackageElement")
                        }
                        association {
                            end("Package", setOf(REF, VAR), "namespace")
                            end("PackageElement", setOf(CMP, VAR), "elements", false, "Set")
                        }
                        association {
                            end("Classifier", setOf(REF, VAR), "type")
                            end("Attribute", setOf(REF, VAR), "typeOpposite", false, "Set")
                        }
                        association {
                            end("Class", setOf(REF, VAR), "owner")
                            end("Attribute", setOf(CMP, VAR), "attribute", false, "Set")
                        }
                        association {
                            end("Class", setOf(REF, VAR), "general", false, "Set")
                            end("Class", setOf(REF, VAR), "generalOpposite", false, "Set")
                        }
                        association {
                            end("Class", setOf(REF, VAR), "source")
                            end("Association", setOf(REF, VAR), "reverse", false, "Set")
                        }
                        association {
                            end("Class", setOf(REF, VAR), "destination")
                            end("Association", setOf(REF, VAR), "forward", false, "Set")
                        }
                    }
                }
                val tm2 = typesDomain("SimpleRDBMS", true) {
                    namespace("rdbms") {
                        data("RModelElement") {
                            propertyOf(setOf(PropertyCharacteristic.READ_WRITE), "name", "String")
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
                            end("Schema", setOf(REF, VAR), "schema")
                            end("Table", setOf(CMP, VAR), "tables", false, "Set")
                        }
                        association {
                            end("Table", setOf(REF, VAR), "owner")
                            end("Column", setOf(CMP, VAR), "column", false, "Set")
                        }
                        association {
                            end("Table", setOf(REF, VAR), "owner")
                            end("Key", setOf(CMP, VAR), "key", false, "Set")
                        }
                        association {
                            end("Table", setOf(REF, VAR), "owner")
                            end("ForeignKey", setOf(CMP, VAR), "foreignKey", false, "Set")
                        }
                        association {
                            end("Key", setOf(REF, VAR), "refersTo", false, "Set")
                            //TODO: refersToOpposite is not navigable!
                            end("ForeignKey", setOf(REF, VAR), "refersToOpposite", false, "Set")
                        }
                        association {
                            end("Column", setOf(REF, VAR), "column", false, "Set")
                            end("ForeignKey", setOf(REF, VAR), "foreignKey", false, "Set")
                        }
                    }
                }
                it.typeDomains[dr1] = tm1
                it.typeDomains[dr2] = tm2
                it.transform = """
                    namespace test
                    transform umlRdbms(uml : SimpleUML, rdbms : SimpleRDBMS) {
                        abstract top rule PrimitiveUmlTypeToSqlType {
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
                it.expected = asmSimple(tm2) {
                    string("BOOLEAN")
                }
            },
            TestData("umlRdbms QVT example").also {
                val dr1 = DomainReference("uml")
                val dr2 = DomainReference("rdbms")
                val tm1 = typesDomain("SimpleUML", true) {
                    namespace("uml") {
                        data("UmlModelElement") {
                            propertyOf(setOf(CMP, VAR), "name", "String")
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
                            propertyOf(setOf(CMP, VAR), "kind", "String")
                        }
                        data("PrimitiveDataType") {
                            supertypes("Classifier")

                        }
                        data("Association") {
                            supertypes("PackageElement")
                        }
                        association {
                            end("Package", setOf(REF, VAR), "namespace")
                            end("PackageElement", setOf(CMP, VAR), "elements", false, "Set")
                        }
                        association {
                            end("Classifier", setOf(REF, VAR), "type")
                            end("Attribute", setOf(REF, VAR), "typeOpposite", false, "Set")
                        }
                        association {
                            end("Class", setOf(REF, VAR), "owner")
                            end("Attribute", setOf(CMP, VAR), "attribute", false, "Set")
                        }
                        association {
                            end("Class", setOf(REF, VAR), "general", false, "Set")
                            end("Class", setOf(REF, VAR), "generalOpposite", false, "Set")
                        }
                        association {
                            end("Class", setOf(REF, VAR), "source")
                            end("Association", setOf(REF, VAR), "reverse", false, "Set")
                        }
                        association {
                            end("Class", setOf(REF, VAR), "destination")
                            end("Association", setOf(REF, VAR), "forward", false, "Set")
                        }
                    }
                }
                val tm2 = typesDomain("SimpleRDBMS", true) {
                    namespace("rdbms") {
                        data("RModelElement") {
                            propertyOf(setOf(PropertyCharacteristic.READ_WRITE), "name", "String")
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
                            end("Schema", setOf(REF, VAR), "schema")
                            end("Table", setOf(CMP, VAR), "tables", false, "Set")
                        }
                        association {
                            end("Table", setOf(REF, VAR), "owner")
                            end("Column", setOf(CMP, VAR), "column", false, "Set")
                        }
                        association {
                            end("Table", setOf(REF, VAR), "owner")
                            end("Key", setOf(CMP, VAR), "key", false, "Set")
                        }
                        association {
                            end("Table", setOf(REF, VAR), "owner")
                            end("ForeignKey", setOf(CMP, VAR), "foreignKey", false, "Set")
                        }
                        association {
                            end("Key", setOf(REF, VAR), "refersTo", false, "Set")
                            //TODO: refersToOpposite is not navigable!
                            end("ForeignKey", setOf(REF, VAR), "refersToOpposite", false, "Set")
                        }
                        association {
                            end("Column", setOf(REF, VAR), "column", false, "Set")
                            end("ForeignKey", setOf(REF, VAR), "foreignKey", false, "Set")
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
                        table PrimitiveUmlTypeToSqlType {
                            domains uml :PrimitiveDataType                | rdbms :String
                            map     PrimitiveDataType{ name := 'Int'}     | 'NUMBER'
                            map     PrimitiveDataType{ name := 'Boolean'} | 'BOOLEAN'
                            map     PrimitiveDataType{ name := 'String'}  | 'VARCHAR'
                        }
                    }
                """.trimIndent()
                it.input[dr1] = asmSimple(tm1) {
                    element("Package") {
                        propertyString("name", "pkg1")
                    }
                }
                it.target = dr2
                it.expected = asmSimple(tm2) {
                    element("Schema") {
                        propertyString("name", "pkg1")
                    }
                }
            }
        )

        fun doTest(index:Int) {
            val testData = testSuit[index]!!
            println("****** [$index] ${testData.description} ******")
            testData.typeDomains.forEach { (k, v) ->
                println("----- ${k.value} : ${v.name.value} -----")
                println(v.asString())
            }
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val context = SentenceContextAny()
            testData.typeDomains.forEach { (k, v) ->
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
            val ogs = testData.typeDomains.entries.associate { (k, v) ->
                Pair(v.name, ObjectGraphAccessorMutatorAsmSimple(v, issues))
            }
            val interpreter = M2mTransformInterpreter(m2m, ogs, issues)

            val source = testData.input.entries.associate { (k, v) ->
                println("----- Source ${k.value} -----")
                val sourceObjects = v.root.map { obj ->
                    println(obj.asString())
                    val srcTypeDomain = testData.typeDomains[k]!!
                    ogs[srcTypeDomain.name]!!.let {
                        val td = srcTypeDomain.findByQualifiedNameOrNull(obj.qualifiedTypeName) ?: error("Can't find type ${obj.qualifiedTypeName}")
                        TypedObjectAsmValue(td.type(), obj)
                    }
                }
                Pair(k, sourceObjects)
            }
            val tgtTransform = m2m.allTransformRuleSet.first()
            val trRes = interpreter.transform(tgtTransform, testData.target!!, source)
            println("----- M2M Transform Result -----")
            println(trRes.asString())
            assertTrue(trRes.issues.isEmpty(), trRes.issues.toString())
            val expected = testData.expected
            if (null!=expected) {
                for (i in expected.root.indices) {
                    val exp = expected.root[i]
                    val act = trRes.targets[i]
                    assertEquals(exp.asString(), act.asString())
                }
            }
        }
    }

    @Test
    fun testAll() {
        testSuit.forEachIndexed { idx, it ->
            doTest(idx)
        }
    }

    @Test
    fun single() {
        doTest(11)
    }
}