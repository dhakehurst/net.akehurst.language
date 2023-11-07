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
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.api.typeModel
import kotlin.test.Test
import kotlin.test.assertEquals

class test_ExpressionsInterpreter {

    companion object {
        fun test(typeModel: TypeModel, self: AsmValue, expression: String, expected: AsmValue) {
            val interpreter = ExpressionsInterpreterOverAsmSimple(typeModel)
            val actual = interpreter.evaluateStr(self, expression)
            assertEquals(expected, actual)
        }
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

        test(tm, self, "prop1", AsmPrimitiveSimple("String", "strValue"))
    }
}