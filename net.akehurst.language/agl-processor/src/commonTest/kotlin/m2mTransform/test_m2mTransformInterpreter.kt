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

import io.kotest.core.spec.style.FunSpec
import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.m2mTransform.testing.TransformTestCase
import net.akehurst.language.agl.m2mTransform.testing.TransformTestSuit
import net.akehurst.language.agl.m2mTransform.testing.m2mTransformTestSuits
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorAsmSimple
import net.akehurst.language.expressions.processor.TypedObjectAsmValue
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.api.PropertyCharacteristic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/* can't debug this way!
class test_m2mTransformInterpreter : FunSpec({
    testSuits.forEach { (_, suite) ->
        context(suite.description) {
            suite.testCase.forEach { (_, case) ->
                test(case.description) {
                    doTest2(suite, case)
                }
            }
        }
    }

    test("single") {
        val suite = testSuits["Full umlRdbms QVT example"]
        val case = suite!!.testCase.values.first()
        doTest2(suite, case)
    }

}) {*/

class test_m2mTransformInterpreter {
    private companion object Companion {

        val testSuits = m2mTransformTestSuits {
            // mapping
            testSuit("1 matching top mapping to 1 String input 1 gives literal String result") {
                typesDomain("d1", "Domain1", true) {
                    namespace("n1") {
                    }
                }
                typesDomain("d2", "Domain2", true) {
                    namespace("n2") {
                    }
                }
                transform(
                    $$"""
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A12A2 {
                            domain d1 a1:String == 'Any'
                            domain d2 a2:String := 'Hello World!'
                        }
                    }
                """
                )
                testCase("Any -> Hello World!") {
                    input("d1") {
                        string("Any")
                    }
                    target("d2") {
                        string("Hello World!")
                    }
                }
            }
            testSuit("1 matching top mapping to 2 String input 2 gives 2 literal String result") {
                typesDomain("d1", "Domain1", true) {
                    namespace("n1") {
                    }
                }
                typesDomain("d2", "Domain2", true) {
                    namespace("n2") {
                    }
                }
                transform(
                    $$"""
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A12A2 {
                            domain d1 a1:String {}
                            domain d2 a2:String := 'Hello ' + a1
                        }
                    }
                """
                )
                testCase("Any1 -> Hello Any1 && Any2 -> Hello Any2") {
                    input("d1") {
                        string("Any1")
                        string("Any2")
                    }
                    target("d2") {
                        string("Hello Any1")
                        string("Hello Any2")
                    }
                }
            }
            testSuit("2 matching top mapping to 2 String input 2 give 4 results") {
                typesDomain("d1", "Domain1", true) {
                    namespace("n1") {
                    }
                }
                typesDomain("d2", "Domain2", true) {
                    namespace("n2") {
                    }
                }
                transform(
                    $$"""
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A {
                            domain d1 a1:String {}
                            domain d2 a2:String := 'Hello '+ a1 +' from A'
                        }
                        top mapping B {
                            domain d1 a1:String {}
                            domain d2 a2:String := 'Hello '+ a1 +' from B'
                        }
                    }
                """
                )
                testCase("2x AnyN -> 4x Hello AnyN from X") {
                    input("d1") {
                        string("Any1")
                        string("Any2")
                    }
                    target("d2") {
                        string("Hello Any1 from A")
                        string("Hello Any2 from A")
                        string("Hello Any1 from B")
                        string("Hello Any2 from B")
                    }
                }
            }
            testSuit("2 matching top mapping to one of each 2 String input 2 gives 2 literal String result") {
                typesDomain("d1", "Domain1", true) {
                    namespace("n1") {
                    }
                }
                typesDomain("d2", "Domain2", true) {
                    namespace("n2") {
                    }
                }
                transform(
                    $$"""
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A {
                            domain d1 a1:String == 'A'
                            domain d2 a2:String := 'Hello A!'
                        }
                        top mapping B {
                            domain d1 a1:String == 'B'
                            domain d2 a2:String := 'Hello B!'
                        }
                    }
                """
                )
                testCase("'A' -> Hello A!") {
                    input("d1") {
                        string("A")
                    }
                    target("d2") {
                        string("Hello A!")
                    }
                }
                testCase("'B' -> HelloBA!") {
                    input("d1") {
                        string("B")
                    }
                    target("d2") {
                        string("Hello B!")
                    }
                }
                testCase("['A', 'B'] -> [Hello A!,  Hello B!]") {
                    input("d1") {
                        string("A")
                        string("B")
                    }
                    target("d2") {
                        string("Hello A!")
                        string("Hello B!")
                    }
                }
            }
            testSuit("simple mapping") {
                typesDomain("d1", "Domain1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                typesDomain("d2", "Domain2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                transform(
                    $$"""
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A12A2 {
                            domain d1 a1:A1 { prop1 == v }
                            domain d2 a2:A2 := A2() { prop2 := v }
                        }
                    }
                """
                )
                testCase("A1 -> A2") {
                    input("d1") {
                        element("A1") {
                            propertyString("prop1", "value1")
                        }
                    }
                    target("d2") {
                        element("A2") {
                            propertyString("prop2", "value1")
                        }
                    }
                }
            }
            testSuit("simple mapping set from navigation") {
                typesDomain("d1", "Domain1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                typesDomain("d2", "Domain2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                transform(
                    $$"""
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A12A2 {
                            domain d1 a1:A1 {}
                            domain d2 a2:A2 := A2() { prop2 := a1.prop1 }
                        }
                    }
                """
                )
                testCase("A1 -> A2") {
                    input("d1") {
                        element("A1") {
                            propertyString("prop1", "value1")
                        }
                    }
                    target("d2") {
                        element("A2") {
                            propertyString("prop2", "value1")
                        }
                    }
                }
            }
            testSuit("nested match") {
                typesDomain("d1", "Domain1", true) {
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
                typesDomain("d2", "Domain2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                transform(
                    $$"""
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
                )
                testCase("A1.B1.C1 -> A2") {
                    input("d1") {
                        element("A1") {
                            propertyElementExplicitType("prop1", "B1") {
                                propertyElementExplicitType("prop2", "C1") {
                                    propertyString("prop3", "value")
                                }
                            }
                        }
                    }
                    target("d2") {
                        element("A2") {
                            propertyString("prop2", "value")
                        }
                    }
                }
            }

            // mapping when
            testSuit("simple mapping when expression") {
                typesDomain("d1", "Domain1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                typesDomain("d2", "Domain2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                transform(
                    $$"""
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A12A2 {
                            domain d1 a1:A1 { prop1 == v }
                            domain d2 a2:A2 := A2() { prop2 := v }
                            when {
                              v == 'value1'
                            }
                        }
                    }
                """
                )
                testCase("A1 with value1 -> A2") {
                    input("d1") {
                        element("A1") {
                            propertyString("prop1", "value1")
                        }
                    }
                    target("d2") {
                        element("A2") {
                            propertyString("prop2", "value1")
                        }
                    }
                }
                testCase("A1 with value2 -> nothing") {
                    input("d1") {
                        element("A1") {
                            propertyString("prop1", "value1")
                        }
                    }
                    target("d2") {
                        element("A2") {
                            propertyString("prop2", "value1")
                        }
                    }
                }
            }

            // mapping where
            testSuit("simple mapping where map String vai table") {
                typesDomain("d1", "Domain1", true) {
                    namespace("n1") {
                        data("A1") {
                            propertyOf(emptySet(), "prop1", "String")
                        }
                    }
                }
                typesDomain("d2", "Domain2", true) {
                    namespace("n2") {
                        data("A2") {
                            propertyOf(emptySet(), "prop2", "String")
                        }
                    }
                }
                transform(
                    $$"""
                    namespace test
                    transform Test(d1:Domain1, d2:Domain2) {
                        top mapping A1_to_A2 {
                            domain d1 a1:A1 { prop1 == s1 }
                            domain d2 a2:A2 := A2() { prop2 := s2 }
                            where {
                              map StringConvert(s1,s2)
                            }
                        }
                        table StringConvert {
                            domain  d1 :String  /**/ domain d2 :String
                            /*=======================================*/ 
                            values 'value1'      to   'value2'
                        } 
                    }
                """
                )
                testCase("A1 with value1 -> A2") {
                    input("d1") {
                        element("A1") {
                            propertyString("prop1", "value1")
                        }
                    }
                    target("d2") {
                        element("A2") {
                            propertyString("prop2", "value2")
                        }
                    }
                }
                testCase("A1 with value2 -> nothing") {
                    input("d1") {
                        element("A1") {
                            propertyString("prop1", "value1")
                        }
                    }
                    target("d2") {
                        element("A2") {
                            propertyString("prop2", "value1")
                        }
                    }
                }
            }

            // collections
            testSuit("match 1 from Set") {
                typesDomain("d1", "Domain1", true) {
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
                typesDomain("d2", "Domain2", true) {
                    namespace("n2") {
                    }
                }
                transform(
                    $$"""
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
                )
                testCase("c1 or c2 contains e1?") {
                    input("d1") {
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
                    target("d2") {
                        string("c1 contains a1")
                    }
                }
            }
            testSuit("match 2 from Set") {
                typesDomain("d1", "Domain1", true) {
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
                typesDomain("d2", "Domain2", true) {
                    namespace("n2") {
                    }
                }
                transform(
                    $$"""
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
                )
                testCase("c1 contains both") {
                    input("d1") {
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
                    target("d2") {
                        string("c1 contains a1")
                        string("c1 contains a2")
                    }
                }
            }
            testSuit("match exact Collection") {
                typesDomain("d1", "Domain1", true) {
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
                typesDomain("d2", "Domain2", true) {
                    namespace("n2") {
                    }
                }
                transform(
                    $$"""
                        namespace test
                        transform Test(d1:Domain1, d2:Domain2) {
                            top mapping A12A2 {
                                domain d1 c1:Class {
                                    name == X
                                    attribute == [
                                        a3:Attribute {
                                            name == 'a3'
                                        },
                                        a4:Attribute {
                                            name == 'a4'
                                        }
                                    ]
                                }
                                domain d2 a2:String := c1.name + a3.name + a4.name
                            }
                        }
                    """
                )
                testCase("c2 contains both") {
                    input("d1") {
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
                    target("d2") {
                        string("c2a3a4")
                    }
                }
            }

            // qvt example
            testSuit("umlRdbms QVT example - PackageToSchema") {
                typesDomain("uml", "SimpleUML", true) {
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
                typesDomain("rdbms", "SimpleRDBMS", true) {
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
                transform(
                    $$"""
                    namespace test
                    transform umlRdbms(uml : SimpleUML, rdbms : SimpleRDBMS) {
                        /* map each package to a schema */
                        top relation PackageToSchema {
                            pivot pn: String
                            domain uml p:Package { name==pn }
                            domain rdbms s:Schema { name==pn }
                        }
                    }
                """
                )
                testCase("Pkg okg1 -> Schm pkg1") {
                    input("uml") {
                        element("Package") {
                            propertyString("name", "pkg1")
                        }
                    }
                    target("rdbms") {
                        element("Schema") {
                            propertyString("name", "pkg1")
                        }
                    }
                }
            }
            testSuit("umlRdbms QVT example - top table PrimitiveUmlTypeToSqlType") {
                typesDomain("uml", "SimpleUML", true) {
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
                typesDomain("rdbms", "SimpleRDBMS", true) {
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
                transform(
                    $$"""
                    namespace test
                    transform umlRdbms(uml : SimpleUML, rdbms : SimpleRDBMS) {
                        top table PrimitiveUmlTypeToSqlType {
                            domain  uml :PrimitiveDataType                /**/ domain rdbms :String
                            /*===================================================================*/ 
                            values uml.PrimitiveDataType(){ name := 'Int'}     to  'NUMBER'
                            values uml.PrimitiveDataType(){ name := 'Boolean'} to  'BOOLEAN'
                            values uml.PrimitiveDataType(){ name := 'String'}  to  'VARCHAR'
                        }
                    }
                """
                )
                testCase("primitive Int -> NUMBER") {
                    input("uml") {
                        element("PrimitiveDataType") {
                            propertyString("name", "Int")
                        }
                    }
                    target("rdbms") {
                        string("NUMBER")
                    }
                }
                testCase("primitive Boolean -> BOOLEAN") {
                    input("uml") {
                        element("PrimitiveDataType") {
                            propertyString("name", "Boolean")
                        }
                    }
                    target("rdbms") {
                        string("BOOLEAN")
                    }
                }
                testCase("primitive String -> VARCHAR") {
                    input("uml") {
                        element("PrimitiveDataType") {
                            propertyString("name", "String")
                        }
                    }
                    target("rdbms") {
                        string("VARCHAR")
                    }
                }
            }
            testSuit("umlRdbms QVT example - abstract PrimitiveUmlTypeToSqlType") {
                typesDomain("uml", "SimpleUML", true) {
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
                typesDomain("rdbms", "SimpleRDBMS", true) {
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
                transform(
                    $$"""
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
                        relation PrimitiveUmlTypeToSqlTypeString : PrimitiveUmlTypeToSqlType {
                            domain uml pt:PrimitiveDataType{ name == 'String'}
                            domain rdbms ct:String == 'VARCHAR'
                        }
                    }
                """
                )
                testCase("primitive Int -> NUMBER") {
                    input("uml") {
                        element("PrimitiveDataType") {
                            propertyString("name", "Int")
                        }
                    }
                    target("rdbms") {
                        string("NUMBER")
                    }
                }
                testCase("primitive Boolean -> BOOLEAN") {
                    input("uml") {
                        element("PrimitiveDataType") {
                            propertyString("name", "Boolean")
                        }
                    }
                    target("rdbms") {
                        string("BOOLEAN")
                    }
                }
                testCase("primitive String -> VARCHAR") {
                    input("uml") {
                        element("PrimitiveDataType") {
                            propertyString("name", "String")
                        }
                    }
                    target("rdbms") {
                        string("VARCHAR")
                    }
                }
            }
            testSuit("Full umlRdbms QVT example") {
                typesDomain("uml", "SimpleUML", true) {
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
                typesDomain("rdbms", "SimpleRDBMS", true) {
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
                transform(
                    $$"""
                    namespace test
                    transform umlRdbms(uml : SimpleUML, rdbms : SimpleRDBMS) {
                        /* map each package to a schema */
                        top relation PackageToSchema {
                            pivot pn: String
                            domain uml p:Package {
                              name == pn
                              elements == p_els
                            }
                            domain rdbms s:Schema {
                              name == pn
                              table == s_tbl
                            }
                            where {
                                relate all ClassToTable(p_els, s_tbl)
                            }
                        }
                        relation ClassToTable {
                            pivot cn: String
                            pivot prefix:String
                            domain uml c:Class {
                                namespace==p:Package {}
                                kind=='Persistent'
                                name==cn
                            }
                            domain rdbms t:Table {
                                schema==s:Schema {}
                                name==cn
                                column==cl:Column {
                                    name=='_id'
                                    type=='NUMBER'
                                }
                                key==k:Key {
                                    name=='_pk'
                                    column==cl
                                    kind=='primary'
                                }
                            }
                            when {  related PackageToSchema(p, s)  }
                            where {
                                 relate all AttributeToColumn(c.attribute, t.column)
                            }
                        }
                        abstract rule AttributeToColumn {
                            domain uml a:Attribute
                            domain rdbms c:Column
                        }
                        relation AttributeToColumnPrimitive {
                            pivot n:String
                            domain uml a:Attribute {
                                name==n
                                type==at:PrimitiveDataType{}
                            }
                            domain rdbms c:Column {
                                name==n
                                type==ct:String{}
                            }
                            where {
                                relate PrimitiveUmlTypeToSqlType(at, ct)
                            }
                        }
                        relation AttributeToColumnComplex {
                            pivot n:String
                            domain uml a:Attribute {
                                name==n
                                type==at:Class{}
                            }
                            domain rdbms c:Column {
                                name==n
                                type=='NUMBER'
                            }
                            where {
                                relate ComplexUmlTypeToSqlType(at, ct)
                            }
                        }                        
                        table PrimitiveUmlTypeToSqlType {
                            domain  uml :PrimitiveDataType                    /**/ domain rdbms :String
                            /*=======================================================================*/ 
                            values uml.PrimitiveDataType(){ name := 'Int'}     to  'NUMBER'
                            values uml.PrimitiveDataType(){ name := 'Boolean'} to  'BOOLEAN'
                            values uml.PrimitiveDataType(){ name := 'String'}  to  'VARCHAR'
                        }
                    }
                """
                )
                testCase("Package with no elements property") {
                    input("uml") {
                        element("Package") {
                            propertyString("name", "pkg1")
                        }
                    }
                    target("rdbms") {
                        element("Schema") {
                            propertyString("name", "pkg1")
                            propertyNothing("table")
                        }
                    }
                }
                testCase("Package with empty List elements") {
                    input("uml") {
                        element("Package") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("elements") {}
                        }
                    }
                    target("rdbms") {
                        element("Schema") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("table") {}
                        }
                    }
                }
                testCase("1 Class with no name or kind or properties") {
                    input("uml") {
                        element("Package") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("elements") {
                                element("Class") {

                                }
                            }
                        }
                    }
                    target("rdbms") {
                        element("Schema") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("table") {}
                        }
                    }
                }
                testCase("1 Class with no name or properties") {
                    input("uml") {
                        element("Package") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("elements") {
                                element("Class") {
                                    propertyString("kind","Persistent")
                                }
                            }
                        }
                    }
                    target("rdbms") {
                        element("Schema") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("table") {}
                        }
                    }
                }
                testCase("1 Class with no properties") {
                    input("uml") {
                        element("Package") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("elements") {
                                element("Class") {
                                    reference("namespace","pkg1")
                                    propertyString("kind","Persistent")
                                    propertyString("name","Cls1")
                                }
                            }
                        }
                    }
                    target("rdbms") {
                        element("Schema") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("table") {
                                element("Table") {
                                    reference("schema","pkg1")
                                }
                            }
                        }
                    }
                }
            }
        }

        fun doTest2(suite: TransformTestSuit, case: TransformTestCase) {
            println("****** ${suite.description} : ${case.description} ******")
            suite.typeDomains.forEach { (k, v) ->
                println("----- ${k.value} : ${v.name.value} -----")
                println(v.asString())
            }
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val context = SentenceContextAny()
            suite.typeDomains.forEach { (k, v) ->
                context.addToScope(null, listOf(v.name.value), QualifiedName("TypesDomain"), null, v)
            }
            val res = Agl.registry.agl.m2mTransform.processor!!.process(
                suite.transform,
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
            val ogs = suite.typeDomains.entries.associate { (k, v) ->
                Pair(v.name, ObjectGraphAccessorMutatorAsmSimple(v, issues))
            }
            val interpreter = M2mTransformInterpreter(m2m, ogs, issues)

            val source = case.input.entries.associate { (k, v) ->
                println("----- Source ${k.value} -----")
                val sourceObjects = v.root.map { obj ->
                    println(obj.asString())
                    val srcTypeDomain = suite.typeDomains[k]!!
                    ogs[srcTypeDomain.name]!!.let {
                        val td = srcTypeDomain.findByQualifiedNameOrNull(obj.qualifiedTypeName) ?: error("Can't find type ${obj.qualifiedTypeName}")
                        TypedObjectAsmValue(td.type(), obj)
                    }
                }
                Pair(k, sourceObjects)
            }
            val tgtTransform = m2m.allTransformRuleSet.first()
            val trRes = interpreter.transform(tgtTransform, case.target!!, source)
            println("----- M2M Transform Result -----")
            println(trRes.asString())
            assertTrue(trRes.issues.errors.isEmpty(), trRes.issues.toString())
            val expected = case.expected
            if (null != expected) {
                for (i in expected.root.indices) {
                    val exp = expected.root[i]
                    val act = trRes.targets[i]
                    assertEquals(exp.asString(), act.asString())
                }
            }
        }

/*
        fun doTest(index: Int) {
            val testData = testSuitList[index]!!
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
            assertTrue(trRes.issues.errors.isEmpty(), trRes.issues.toString())
            val expected = testData.expected
            if (null != expected) {
                for (i in expected.root.indices) {
                    val exp = expected.root[i]
                    val act = trRes.targets[i]
                    assertEquals(exp.asString(), act.asString())
                }
            }
        }
*/

    }

    @Test
    fun testAll() {
        testSuits.values.forEach { suite ->
            suite.testCase.values.forEach { case ->
                doTest2(suite, case)
            }
        }
    }

    @Test
    fun single() {
        val suite = testSuits["simple mapping where map String vai table"]!!
        val case = suite.testCase["A1 with value1 -> A2"]!!
        doTest2(suite, case)
    }
}
