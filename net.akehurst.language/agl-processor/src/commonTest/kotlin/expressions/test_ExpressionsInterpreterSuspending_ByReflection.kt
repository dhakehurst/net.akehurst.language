/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.expressions.processor

import kotlinx.coroutines.test.runTest
import net.akehurst.language.agl.expressions.processor.ObjectGraphByReflectionSuspending
import net.akehurst.language.agl.expressions.processor.StdLibPrimitiveExecutionsForReflection
import net.akehurst.language.agl.expressions.processor.StdLibPrimitiveExecutionsForReflectionSuspending
import net.akehurst.language.agl.expressions.processor.TypedObjectAny
import net.akehurst.language.api.processor.EvaluationContext
import net.akehurst.language.asm.api.AsmValue
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.asm.simple.AsmListSimple
import net.akehurst.language.asm.simple.AsmNothingSimple
import net.akehurst.language.asm.simple.AsmPrimitiveSimple
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals

class test_ExpressionsInterpreterSuspending_ByReflection {

    companion object Companion {
        data class TestObj(
            val prop1: String
        )

        suspend fun test(typesDomain: TypesDomain, self: Any, selfTypeName: String, expression: String, expected: Any) {
            val st = typesDomain.findByQualifiedNameOrNull(selfTypeName.asQualifiedName)?.type() ?: StdLibDefault.AnyType
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val interpreter = ExpressionsInterpreterOverTypedObjectSuspending(ObjectGraphByReflectionSuspending(typesDomain, issues), issues)
            val actual = interpreter.evaluateStr(EvaluationContext.ofSelf(TypedObjectAny(st, self)), expression)
            assertEquals(expected, actual.self)
        }

        suspend fun test_fail(typesDomain: TypesDomain, self: Any, selfTypeName: String, expression: String, expected: List<LanguageIssue>) {
            val st = typesDomain.findByQualifiedNameOrNull(selfTypeName.asQualifiedName)?.type() ?: StdLibDefault.AnyType
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val interpreter = ExpressionsInterpreterOverTypedObjectSuspending(ObjectGraphByReflectionSuspending(typesDomain, issues), issues)
            val actual = interpreter.evaluateStr(EvaluationContext.ofSelf(TypedObjectAny(st, self)), expression)
            assertEquals(AsmNothingSimple, actual.self)
            assertEquals(expected, interpreter.issues.all.toList())
        }

        val executor = StdLibPrimitiveExecutionsForReflectionSuspending<Any>().also {
            val qn = AglBase.typesDomain.findFirstDefinitionByNameOrNull(SimpleName("QualifiedName"))!!
            val qn_v = qn.property.first { it.name.value == "value" }
            it.addPropertyExecution1(qn_v, QualifiedName::value)
        }
    }

    @Test
    fun nothing() = runTest {
        val expression = $$"$nothing"
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestObj") {
                    propertyByEvaluation("prop1", "String").also {
                        executor.addPropertyExecution1(it, TestObj::prop1)
                    }
                }
            }
        }
        val self = TestObj("strValue")

        test(tm, self, "ns.Test", expression, Unit)
    }

    @Test
    fun primitive_string() = runTest {
        val expression = $$"'Hello World!'"
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestObj") {
                    propertyByEvaluation("prop1", "String").also {
                        executor.addPropertyExecution1(it, TestObj::prop1)
                    }
                }
            }
        }
        val self = Unit

        test(tm, self, StdLibDefault.NothingType.qualifiedTypeName.value, expression, "Hello World!")
    }

    @Test
    fun Set_of_string() = runTest {
        val expression = $$"Set('Hello', 'World', '!')"
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestObj") {
                    propertyByEvaluation("prop1", "String").also {
                        executor.addPropertyExecution1(it, TestObj::prop1)
                    }
                }
            }
        }
        val self = Unit

        test(
            tm, self, StdLibDefault.NothingType.qualifiedTypeName.value, expression,
            listOf(
                "Hello",
                "World",
                "!"
            )

        )
    }

    @Test
    fun structure_self() = runTest {
        val expression = $$"$self"
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyString("prop1", "strValue")
            }
        }
        val self = asm.root[0]

        test(tm, self, "ns.Test", expression, self)
    }

    @Test
    fun structure_property__notfound() = runTest {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyString("prop1", "strValue")
            }
        }
        val self = asm.root[0]

        test(tm, self, "ns.Test", "prop2", AsmNothingSimple)
    }

    @Test
    fun structure_property__found() = runTest {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyString("prop1", "strValue")
            }
        }
        val self = asm.root[0]

        test(tm, self, "ns.Test", "prop1", AsmPrimitiveSimple.stdString("strValue"))
    }

    @Test
    fun structure_property_index__notIndexable() = runTest {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyString("prop1", "strValue")
            }
        }
        val self = asm.root[0]

        val expectedIssues = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.INTERPRET,
                null,
                "Index operation on non List value is not possible: 'strValue'"
            )
        )
        test_fail(tm, self, "ns.Test", "prop1[0]", expectedIssues)
    }

    @Test
    fun structure_property_index__onlyOneIndexValue() = runTest {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyListOfString("prop1", listOf("strValue"))
            }
        }
        val self = asm.root[0]

        val expectedIssues = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.INTERPRET,
                null,
                "Only one index value should be used for Lists"
            )
        )
        test_fail(tm, self, "ns.Test", "prop1[0,1,2]", expectedIssues)
    }

    @Test
    fun structure_property_index__mustBeInteger() = runTest {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyListOfString("prop1", listOf("strValue"))
            }
        }
        val self = asm.root[0]

        val expectedIssues = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.INTERPRET,
                null,
                "Index value must evaluate to an Integer for Lists"
            )
        )
        test_fail(tm, self, "ns.Test", "prop1['a']", expectedIssues)
    }

    @Test
    fun structure_property_index__outOfRange() = runTest {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyListOfString("prop1", listOf("strValue"))
            }
        }
        val self = asm.root[0]

        val expectedIssues = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.INTERPRET,
                null,
                "in getIndex argument index '4' out of range"
            )
        )
        test_fail(tm, self, "ns.Test", "prop1[4]", expectedIssues)
    }

    @Test
    fun structure_propertyListOfString_index_0() = runTest {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyListOfString("prop1", listOf("strValue"))
            }
        }
        val self = asm.root[0]

        val expected = AsmPrimitiveSimple.stdString("strValue")
        test(tm, self, "ns.Test", "prop1[0]", expected)
    }

    @Test
    fun structure_propertyListOfA_get() = runTest {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("aList", "A", false, 0)
                }
                data("A") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyListOfElement("aList") {
                    element("A") {
                        propertyString("prop1", "v1")
                    }
                    element("A") {
                        propertyString("prop1", "v2")
                    }
                    element("A") {
                        propertyString("prop1", "v3")
                    }
                }
            }
        }
        val self = asm.root[0]

        val expected = AsmPrimitiveSimple.stdString("v2")
        test(tm, self, "ns.Test", "aList.get(1).prop1", expected)
    }

    @Test
    fun structure_propertyListOfA_map() = runTest {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("aList", "A", false, 0)
                }
                data("A") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyListOfElement("aList") {
                    element("A") {
                        propertyString("prop1", "v1")
                    }
                    element("A") {
                        propertyString("prop1", "v2")
                    }
                    element("A") {
                        propertyString("prop1", "v3")
                    }
                }
            }
        }
        val self = asm.root[0]
        val expected = AsmListSimple(
            listOf(
                AsmPrimitiveSimple.stdString("v1"),
                AsmPrimitiveSimple.stdString("v2"),
                AsmPrimitiveSimple.stdString("v3")
            )
        )
        test(tm, self, "ns.Test", "aList.map() {it.prop1}", expected)
    }
}