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
import net.akehurst.language.agl.m2mTransform.testing.TransformTestCase
import net.akehurst.language.agl.m2mTransform.testing.TransformTestSuit
import net.akehurst.language.agl.m2mTransform.testing.m2mTransformTestSuits
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.processor.M2mTransformString
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.expressions.processor.ExternalGetterAsmSimple
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorAsmSimple
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.types.api.PropertyCharacteristic
import kotlin.test.Test
import kotlin.test.assertEquals

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
            // simple mapping
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
                    input("d1", "n1") {
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
            testSuit("mapping with nested match") {
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

            // simple relation
            testSuit("1 matching top relation to 1 String input 1 gives literal String result") {
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
                        top relation A12A2 {
                            domain d1 a1:String == 'Any'
                            domain d2 a2:String == 'Hello World!'
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
                testCase("Any <- Hello World!") {
                    input("d2") {
                        string("Hello World!")
                    }
                    target("d1") {
                        string("Any")
                    }
                }
            }
            testSuit("1 matching top relation to 2 String input 2 gives 2 literal String result") {
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
                        top relation A12A2 {
                            domain d1 a1:String {}
                            domain d2 a2:String == 'Hello ' + a1
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
            testSuit("2 matching top relation to 2 String input 2 give 4 results") {
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
                        top relation A {
                            domain d1 a1:String {}
                            domain d2 a2:String == 'Hello '+ a1 +' from A'
                        }
                        top relation B {
                            domain d1 a1:String {}
                            domain d2 a2:String == 'Hello '+ a1 +' from B'
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
            testSuit("2 matching top relation to one of each 2 String input 2 gives 2 literal String result") {
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
                        top relation A {
                            domain d1 a1:String == 'A'
                            domain d2 a2:String == 'Hello A!'
                        }
                        top relation B {
                            domain d1 a1:String == 'B'
                            domain d2 a2:String == 'Hello B!'
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
            testSuit("simple relation") {
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
                        top relation A12A2 {
                            domain d1 a1:A1 { prop1 == v }
                            domain d2 a2:A2 { prop2 == v }
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
                testCase("A1 <- A2") {
                    input("d2") {
                        element("A2") {
                            propertyString("prop2", "value1")
                        }
                    }
                    target("d1") {
                        element("A1") {
                            propertyString("prop1", "value1")
                        }
                    }
                }
            }
            testSuit("simple relation set from navigation") {
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
                        top relation A12A2 {
                            domain d1 a1:A1 {}
                            domain d2 a2:A2 { prop2 == a1.prop1 }
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
            testSuit("relation with nested match") {
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
                        top relation A12A2 {
                            domain d1 a1:A1 {
                               prop1 == B1 {
                                 prop2 == C1 {
                                   prop3 == v
                                 }
                               }
                            }
                            domain d2 a2:A2 { prop2 == v }
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
                            propertyString("prop1", "value2")
                        }
                    }
                    expectIssue(LanguageIssueKind.INFORMATION, "when clause evaluated to false for target domain ref 'd2' of rule 'A12A2'.")
                    target("d2") {
                    }
                }
            }

            // relation when
            testSuit("simple relation when expression") {
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
                        top relation A12A2 {
                            domain d1 a1:A1 { prop1 == v }
                            domain d2 a2:A2 { prop2 == v }
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
                testCase("A1 with value1 <- A2") {
                    input("d2") {
                        element("A2") {
                            propertyString("prop2", "value1")
                        }
                    }
                    target("d1") {
                        element("A1") {
                            propertyString("prop1", "value1")
                        }
                    }
                }
                testCase("A1 with value2 -> nothing") {
                    input("d1") {
                        element("A1") {
                            propertyString("prop1", "value2")
                        }
                    }
                    expectIssue(LanguageIssueKind.INFORMATION, "when clause evaluated to false for target domain ref 'd2' of rule 'A12A2'.")
                    target("d2") {
                    }
                }
            }

            // mapping where
            testSuit("simple mapping where map String via table") {
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
                              map StringConvert { d1 := s1 d2 := s2 }
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
                            propertyString("prop1", "value2")
                        }
                    }
                    expectIssue(LanguageIssueKind.WARNING, "In rule 'A1_to_A2' the 'where' clause matched nothing.")
                    target("d2") {
                        element("A2") {
                            propertyNothing("prop2")
                        }
                    }
                }
            }

            // relation where
            testSuit("simple relation where map String via table") {
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
                        top relation A1_to_A2 {
                            domain d1 a1:A1 { prop1 == s1 }
                            domain d2 a2:A2 { prop2 == s2 }
                            where {
                              map StringConvert{ d1:=s1 d2:=s2 }
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
                testCase("A1 with value1 <- A2") {
                    input("d2") {
                        element("A2") {
                            propertyString("prop2", "value2")
                        }
                    }
                    target("d1") {
                        element("A1") {
                            propertyString("prop1", "value1")
                        }
                    }
                }
                testCase("A1 with value2 -> nothing") {
                    input("d1") {
                        element("A1") {
                            propertyString("prop1", "value2")
                        }
                    }
                    expectIssue(LanguageIssueKind.WARNING, "In rule 'A1_to_A2' the 'where' clause matched nothing.")
                    target("d2") {
                        element("A2") {
                            propertyNothing("prop2")
                        }
                    }
                }
            }


            // mapping collections
            testSuit("mapping match 1 from Set") {
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
            testSuit("mapping match 2 from Set") {
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
            testSuit("mapping match exact Collection") {
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

            // relation collections
            testSuit("relation match 1 from Set") {
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
                        top relation A12A2 {
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
                            domain d2 a2:String == X + ' contains a1'
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
            testSuit("relation match 2 from Set") {
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
                        top relation A12A2 {
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
                            domain d2 a2:String == X + ' contains ' + Y
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
            testSuit("relation match exact Collection") {
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
                            top relation A12A2 {
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
                                domain d2 a2:String == c1.name + a3.name + a4.name
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
            //TODO: collections on target side !

            // relation qvt example
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
                            constructor_ {
                                this.parameter(setOf(), "name", "String")
                            }
                        }
                        data("Table") {
                            supertypes("RModelElement")
                            constructor_ {
                                parameter(setOf(), "schema", "Schema")
                                parameter(setOf(), "name", "String")
                            }
                        }
                        data("Column") {
                            supertypes("RModelElement")
                            constructor_ {
                                parameter(setOf(), "owner", "Table")
                                parameter(setOf(), "name", "String")
                            }
                            propertyOf(setOf(CMP,VAR), "type", "String")
                        }
                        data("Key") {
                            supertypes("RModelElement")
                            constructor_ {
                                parameter(setOf(), "owner", "Table")
                                parameter(setOf(), "name", "String")
                            }
                            propertyOf(setOf(CMP,VAR), "kind", "String")
                        }
                        data("ForeignKey") {
                            supertypes("RModelElement")
                        }
                        association {
                            end("Schema", setOf(REF, VAR), "schema")
                            end("Table", setOf(CMP, VAR), "table", false, "Set")
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
                            end("Column",setOf(REF, VAR), "column",false,"Set")
                            end("Key",setOf(REF, VAR),"key",false,"Set")
                        }
                        association {
                            end("Column", setOf(REF, VAR), "column", false, "Set")
                            end("ForeignKey", setOf(REF, VAR), "foreignKey", false, "Set")
                        }
                    }
                }
                crossReferenceDomain("uml", "SimpleUML") {
                    declarationsFor("uml") {
                        identify("Package", "name")
                        reference("Class") {
                            property("namespace", listOf("Package"), null)
                        }
                        scope("Package") {
                            identify("Class", "name")
                        }
                    }
                }
                crossReferenceDomain("rdbms", "SimpleRDBMS") {
                    declarationsFor("rdbms") {
                        identify("Schema", "name")
                        identify("Table", "schema.name+'.'+name")
                        identify("Column", "owner.schema.name+'.'+owner.name+'.'+name")
                        scope("Schema") {
                            identify("Table", "name")
                        }
                        scope("Table") {
                            identify("Column", "name")
                            identify("Key", "name")
                        }
                        reference("Table") {
                            property("schema", listOf("Schema"), null)
                        }
                        reference("Key") {
                            property("owner", listOf("Table"), null)
                            property("column", listOf("Column"), null)
                        }
                        reference("Column") {
                            property("owner", listOf("Table"), null)
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
                                relate all ClassToTable { uml := p_els rdbms:= s_tbl }
                            }
                        }
                        relation ClassToTable {
                            pivot cn: String
                            domain uml c:Class {
                                namespace==p
                                kind=='Persistent'
                                name==cn
                                attribute==c_atts
                            }
                            domain rdbms t:Table {
                                schema==s
                                name==cn
                                column==t_cols:[
                                  ...
                                  pk_col:Column {
                                    owner==t
                                    name=='_id'
                                    type=='NUMBER'
                                  }
                                ]
                                key==Key {
                                    owner==t
                                    name=='_pk'
                                    column==pk_col
                                    kind=='primary'
                                }
                            }
                            when { related PackageToSchema{ uml := p rdbms := s } }
                            where {
                                 relate all AttributeToColumn{ uml := c_atts rdbms := t_cols }
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
                                relate PrimitiveUmlTypeToSqlType {
                                  uml := at
                                  rdbms := ct
                                }
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
                                relate ComplexUmlTypeToSqlType {
                                  uml := at
                                  rdbms := ct
                                }
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
                    expectIssue(LanguageIssueKind.ERROR, "In 'where' clause of rule 'PackageToSchema' in 'umlRdbms', the all call to rule 'ClassToTable' is expecting a collection.")
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
                testCase("1 Class with no name, namespace, kind or attributes") {
                    input("uml") {
                        element("Package") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("elements") {
                                element("Class") {
                                }
                            }
                        }
                    }
                    // because the Class.kind is not set, the ClassToTable uml domain matches nothing, and the PackageToSchema.where will match nothing
                    expectIssue(LanguageIssueKind.WARNING, "In rule 'PackageToSchema' the 'where' clause matched nothing.")
                    target("rdbms") {
                        element("Schema") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("table") {}
                        }
                    }
                }
                testCase("1 Class with name but no namespace, kind or attributes") {
                    input("uml") {
                        element("Package") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("elements") {
                                element("Class") {
                                    propertyString("name", "Cls1")
                                }
                            }
                        }
                    }
                    // because the Class.kind is not set, the ClassToTable uml domain matches nothing, and the PackageToSchema.where will match nothing
                    expectIssue(LanguageIssueKind.WARNING, "In rule 'PackageToSchema' the 'where' clause matched nothing.")
                    target("rdbms") {
                        element("Schema") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("table") {}
                        }
                    }
                }
                testCase("1 Class with name & kind, but no namespace, or attributes") { //when clause of ClassToSchema fails
                    input("uml") {
                        element("Package") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("elements") {
                                element("Class") {
                                    propertyString("name", "Cls1")
                                    propertyString("kind", "Persistent")
                                }
                            }
                        }
                    }
                    // because the Class.namespace is not set, variable p will be $nothing, and the ClassToTable.when clause will fail
                    expectIssue(LanguageIssueKind.INFORMATION, "when clause evaluated to false for target domain ref 'rdbms' of rule 'ClassToTable'.")
                    // because the ClassToTable.when clause fails, the PackageToSchema.where will match nothing
                    expectIssue(LanguageIssueKind.WARNING, "In rule 'PackageToSchema' the 'where' clause matched nothing.")
                    target("rdbms") {
                        element("Schema") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("table") {}
                        }
                    }
                }
                testCase("1 Class with name, kind & namespace, but no attributes") {
                    input("uml", resolveReferences = true, context = contextAsmSimple(), sentenceId = 0) {
                        element("Package") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("elements") {
                                element("Class") {
                                    reference("namespace", "pkg1")
                                    propertyString("name", "Cls1")
                                    propertyString("kind", "Persistent")
                                }
                            }
                        }
                    }
                    // there are no Class attributes so expect this
                    expectIssue(LanguageIssueKind.ERROR, "In 'where' clause of rule 'ClassToTable' in 'umlRdbms', the all call to rule 'AttributeToColumn' is expecting a collection.")
                    target("rdbms", resolveReferences = true, failIfIssues = true, context = contextAsmSimple(), sentenceId = 0) {
                        element("Schema") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("table") {
                                element("Table") {
                                    reference("schema", "pkg1")
                                    propertyString("name", "Cls1")
                                    propertyElementExplicitType("key", "Key") {
                                        reference("owner", "pkg1.Cls1")
                                        propertyString("name", "_pk")
                                        reference("column", "pkg1.Cls1._id")
                                        propertyString("kind", "primary")
                                    }
                                    propertyListOfElement("column") {
                                        element("Column") {
                                            reference("owner", "pkg1.Cls1")
                                            propertyString("name", "_id")
                                            propertyString("type", "NUMBER")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                testCase("1 Class with name, kind & namespace and empty attributes") {
                    input("uml", resolveReferences = true, context = contextAsmSimple(), sentenceId = 0) {
                        element("Package") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("elements") {
                                element("Class") {
                                    reference("namespace", "pkg1")
                                    propertyString("kind", "Persistent")
                                    propertyString("name", "Cls1")
                                    propertyListOfElement("attribute") {}
                                }
                            }
                        }
                    }
                    target("rdbms", resolveReferences = true, failIfIssues = false, context = contextAsmSimple(), sentenceId = 0) {
                        element("Schema") {
                            propertyString("name", "pkg1")
                            propertyListOfElement("table") {
                                element("Table") {
                                    reference("schema", "pkg1")
                                    propertyString("name", "Cls1")
                                    propertyElementExplicitType("key", "Key") {
                                        reference("owner", "pkg1.Cls1")
                                        propertyString("name", "_pk")
                                        reference("column", "pkg1.Cls1._id")
                                        propertyString("kind", "primary")
                                    }
                                    propertyListOfElement("column") {
                                        element("Column") {
                                            reference("owner", "pkg1.Cls1")
                                            propertyString("name", "_id")
                                            propertyString("type", "NUMBER")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        fun doTest(suite: TransformTestSuit, case: TransformTestCase) {
            println("****** Suit '${suite.description}' : Case '${case.description}' ******")
//            suite.typeDomains.forEach { (k, v) ->
//                println("----- ${k.value} : ${v.name.value} -----")
//                println(v.asString())
//            }
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val transform = M2mTransformString(suite.transform)
            val accMuts = suite.typeDomains.entries.associate { (k, v) ->
                val cdr = suite.crossReferenceDomains[k]
                Pair(v.name, ObjectGraphAccessorMutatorAsmSimple(v, issues, LocationMapDefault(), ExternalGetterAsmSimple(v, cdr, issues, LocationMapDefault())))
            }
            val domains = case.input.entries.associate { (k, v) ->
                println("----- Source ${k.value} -----")
                val sourceObjects = v.root.map { obj ->
                    println(obj.asString())
                    val srcTypeDomain = suite.typeDomains[k]!!
                    accMuts[srcTypeDomain.name]!!.let { am ->
                        val td = srcTypeDomain.findByQualifiedNameOrNull(obj.qualifiedTypeName) ?: error("Can't find type ${obj.qualifiedTypeName}")
                        am.typedAs(obj, td.type())
                    }
                }
                Pair(k, sourceObjects)
            }
            val trRes = Agl.transform(
                transform,
                suite.typeDomains,
                accMuts as Map<SimpleName,ObjectGraphAccessorMutator>,
                domains,
                case.target!!
            )
            println("----- M2M Transform Result -----")
            trRes.targets.forEach { println(it.asString()) }
            println(trRes.asString())
            assertEquals(case.expectedIssues, trRes.issues.all, trRes.issues.toString())
            val expected = case.expected
            if (null != expected) {
                assertEquals(expected.root.size, trRes.targets.size)
                for (i in expected.root.indices) {
                    val exp = expected.root[i]
                    val act = trRes.targets[i]
                    assertEquals(exp.asString(), act.asString())
                }
            }
        }

        fun doTest2(suite: TransformTestSuit, case: TransformTestCase) {
            println("****** Suit '${suite.description}' : Case '${case.description}' ******")
            println(suite.transform)
//            suite.typeDomains.forEach { (k, v) ->
//                println("----- ${k.value} : ${v.name.value} -----")
//                println(v.asString())
//            }
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val context = SentenceContextAny()
            suite.typeDomains.forEach { (k, v) ->
                context.addToScope(null, listOf(v.name.value), QualifiedName("TypesDomain"), null, v)
            }
            val res = Agl.registry.agl.m2mTransform.processor!!.process(
                suite.transform,
                options = Agl.options {
                    semanticAnalysis {
                        sentenceContext(context)
                    }
                }
            )
            val m2m = res.let {
                check(it.allIssues.errors.isEmpty()) { it.allIssues.toString() }
                it.asm!!
            }
            val ogs = suite.typeDomains.entries.associate { (k, v) ->
                val cdr = suite.crossReferenceDomains[k]
                Pair(v.name, ObjectGraphAccessorMutatorAsmSimple(v, issues, LocationMapDefault(), ExternalGetterAsmSimple(v, cdr, issues, LocationMapDefault())))
            }
            val interpreter = M2mTransformInterpreter(m2m, ogs, issues)

            val source = case.input.entries.associate { (k, v) ->
                println("----- Source ${k.value} -----")
                val sourceObjects = v.root.map { obj ->
                    println(obj.asString())
                    val srcTypeDomain = suite.typeDomains[k]!!
                    ogs[srcTypeDomain.name]!!.let { am ->
                        val td = srcTypeDomain.findByQualifiedNameOrNull(obj.qualifiedTypeName) ?: error("Can't find type ${obj.qualifiedTypeName}")
                        am.typedAs(obj,td.type())
                    }
                }
                Pair(k, sourceObjects)
            }
            val tgtTransform = m2m.allTransformRuleSet.first()
            val trRes = interpreter.transform(tgtTransform, case.target!!, source)
            println("----- M2M Transform Result -----")
            trRes.targets.forEach { println(it.asString()) }
            println(trRes.asString())
            println(trRes.issues.toString())
            assertEquals(case.expectedIssues, trRes.issues.all, trRes.issues.toString())
            val expected = case.expected
            if (null != expected) {
                assertEquals(expected.root.size, trRes.targets.size)
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
        val total = testSuits.values.sumOf { it.testCase.size }
        var count = 0
        var passes = 0
        testSuits.values.forEach { suite ->
            suite.testCase.values.forEach { case ->
                count++
                try {
                    print("$count: ")
                    doTest2(suite, case)
                    passes++
                } catch (t: Throwable) {
                    println("Error at $count: ")
                    t.printStackTrace()
                }
            }
        }
        println("$passes / $total")
        assertEquals(total, passes)
    }

    @Test
    fun single() {
        val suite = testSuits["Full umlRdbms QVT example"]!!
        val case = suite.testCase["1 Class with name, kind & namespace and empty attributes"]!!
        doTest2(suite, case)
    }
}
