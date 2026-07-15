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

import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.asm.api.AsmValue
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.asm.simple.AsmListSimple
import net.akehurst.language.asm.simple.AsmNothingSimple
import net.akehurst.language.asm.simple.AsmPrimitiveSimple
import net.akehurst.language.asm.simple.AsmSetSimple
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_ExpressionsInterpreterOverTypedObject_AsmSimple {

    companion object Companion {
        fun test(typesDomain: TypesDomain, self: Any, pqn:String, expression: String, expected: Any) {
            val st = typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(pqn.asPossiblyQualifiedName)?.type() ?: StdLibDefault.AnyType
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val interpreter = ExpressionsInterpreterOverTypedObject(objectGraphSimpleAsm(typesDomain, null,issues, LocationMapDefault()))
            val actual = interpreter.evaluateStr(EvaluationContext.ofSelf(interpreter.objectGraph.typedAs(self, st)), expression)
            assertEquals(expected, actual.untyped, "Expected != Actual:\n$expected\n$actual")
        }

        fun test_fail(typesDomain: TypesDomain, self: Any, pqn:String, expression: String, expected: List<LanguageIssue>) {
            val st = typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(pqn.asPossiblyQualifiedName)?.type() ?: StdLibDefault.AnyType
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val interpreter = ExpressionsInterpreterOverTypedObject(objectGraphSimpleAsm(typesDomain, null,issues, LocationMapDefault()))
            val actual = interpreter.evaluateStr(EvaluationContext.ofSelf(interpreter.objectGraph.typedAs(self, st)), expression)
            assertEquals(Unit, actual.untyped)
            assertEquals(expected, interpreter.issues.all.toList())
        }
    }

    @Test
    fun nothing() {
        val expression = $$"$nothing"
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

        test(tm, self, "ns.Test", expression, Unit)
    }

    @Test
    fun primitive_string() {
        val expression = $$"'Hello World!'"
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        val self = Unit

        test(tm, self, "Nothing", expression, "Hello World!")
    }

    @Test
    fun Set_of_string() {
        val expression = $$"Set('Hello', 'World', '!')"
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        val self = Unit

        test(tm, self, "Nothing",expression, setOf("Hello", "World", "!"))
    }

    @Test
    fun structure_self() {
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

        test(tm, self, "ns.Test",expression, self)
    }

    @Test
    fun structure_property__notfound() {
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

        test(tm, self, "ns.Test","prop2", Unit)
    }

    @Test
    fun structure_property__found() {
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

        test(tm, self, "ns.Test","prop1", "strValue")
    }

    @Test
    fun structure_property_index__notIndexable() {
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
        test_fail(tm, self, "ns.Test","prop1[0]", expectedIssues)
    }

    @Test
    fun structure_property_index__onlyOneIndexValue() {
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
        test_fail(tm, self, "ns.Test","prop1[0,1,2]", expectedIssues)
    }

    @Test
    fun structure_property_index__mustBeInteger() {
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
        test_fail(tm, self, "ns.Test","prop1['a']", expectedIssues)
    }

    @Test
    fun structure_property_index__outOfRange() {
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
                "In getFromListWithIndex argument index '4' out of range"
            )
        )
        test_fail(tm, self, "ns.Test","prop1[4]", expectedIssues)
    }

    @Test
    fun structure_propertyListOfString_index_0() {
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

        test(tm, self, "ns.Test","prop1[0]", "strValue")
    }

    @Test
    fun structure_propertyListOfA_get() {
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

        test(tm, self, "ns.Test","aList.get(1).prop1", "v2")
    }

    @Test
    fun structure_propertyListOfA_map() {
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
        val expected =listOf("v1", "v2", "v3")
        test(tm, self, "ns.Test","aList.map({it -> it.prop1})", expected)
    }

    @Test
    fun ternaryConditional_true() {
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
                propertyString("x", "a")
            }
        }
        val self = asm.root[0]
        test(tm, self, "ns.Test","x=='a' ? true : false", true)
    }
    @Test
    fun ternaryConditional_false() {
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
                propertyString("x", "a")
            }
        }
        val self = asm.root[0]
        test(tm, self, "ns.Test","x==7 ? 'yes' : 'no'", "no")
    }
}