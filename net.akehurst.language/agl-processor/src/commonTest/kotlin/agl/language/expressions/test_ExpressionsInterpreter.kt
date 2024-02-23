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

package net.akehurst.language.agl.language.expressions

import net.akehurst.language.agl.asm.AsmNothingSimple
import net.akehurst.language.agl.asm.AsmPrimitiveSimple
import net.akehurst.language.api.asm.AsmValue
import net.akehurst.language.api.asm.asmSimple
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.api.typeModel
import kotlin.test.Test
import kotlin.test.assertEquals

class test_ExpressionsInterpreter {

    companion object {
        fun test(typeModel: TypeModel, self: AsmValue, expression: String, expected: AsmValue) {
            val interpreter = ExpressionsInterpreterOverTypedObject(typeModel)
            val actual = interpreter.evaluateStr(self.toTypedObject(typeModel), expression)
            assertEquals(expected, actual.asm)
        }

        fun test_fail(typeModel: TypeModel, self: AsmValue, expression: String, expected: List<LanguageIssue>) {
            val interpreter = ExpressionsInterpreterOverTypedObject(typeModel)
            val actual = interpreter.evaluateStr(self.toTypedObject(typeModel), expression)
            assertEquals(AsmNothingSimple, actual.asm)
            assertEquals(expected, interpreter.issues.all.toList())
        }
    }

    @Test
    fun structure_nothing() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("Test") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple {
            element("Test") {
                propertyString("prop1", "strValue")
            }
        }
        val self = asm.root[0]

        val expectedIssues = listOf(
            LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.INTERPRET, null, "")
        )
        test_fail(tm, self, "\$nothing", expectedIssues)
    }

    @Test
    fun structure_self() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("Test") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple {
            element("Test") {
                propertyString("prop1", "strValue")
            }
        }
        val self = asm.root[0]

        test(tm, self, "\$self", self)
    }

    @Test
    fun structure_property__notfound() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("Test") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple {
            element("Test") {
                propertyString("prop1", "strValue")
            }
        }
        val self = asm.root[0]

        test(tm, self, "prop2", AsmNothingSimple)
    }

    @Test
    fun structure_property__found() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("Test") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple {
            element("Test") {
                propertyString("prop1", "strValue")
            }
        }
        val self = asm.root[0]

        test(tm, self, "prop1", AsmPrimitiveSimple.stdString("strValue"))
    }

    @Test
    fun structure_property_index__notIndexable() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("Test") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple {
            element("Test") {
                propertyString("prop1", "strValue")
            }
        }
        val self = asm.root[0]

        val expectedIssues = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.INTERPRET,
                null,
                "Index operation on non List value is not possible: strValue"
            )
        )
        test_fail(tm, self, "prop1[0]", expectedIssues)
    }

    @Test
    fun structure_property_index__onlyOneIndexValue() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("Test") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple {
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
        test_fail(tm, self, "prop1[0,1,2]", expectedIssues)
    }

    @Test
    fun structure_property_index__mustBeInteger() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("Test") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple {
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
        test_fail(tm, self, "prop1['a']", expectedIssues)
    }

    @Test
    fun structure_property_index__outOfRange() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("Test") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("prop1", listOf("strValue"))
            }
        }
        val self = asm.root[0]

        val expectedIssues = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.INTERPRET,
                null,
                "Index '4' out of range"
            )
        )
        test_fail(tm, self, "prop1[4]", expectedIssues)
    }

    @Test
    fun structure_propertyListOfString_index_0() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("Test") {
                    propertyPrimitiveType("prop1", "String", false, 0)
                }
            }
        }
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("prop1", listOf("strValue"))
            }
        }
        val self = asm.root[0]

        val expected = AsmPrimitiveSimple.stdString("strValue")
        test(tm, self, "prop1[0]", expected)
    }
}