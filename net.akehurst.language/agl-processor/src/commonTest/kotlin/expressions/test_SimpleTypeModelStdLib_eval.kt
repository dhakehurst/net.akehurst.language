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

import net.akehurst.language.asm.simple.AsmListSimple
import net.akehurst.language.asm.simple.AsmNothingSimple
import net.akehurst.language.asm.simple.AsmPrimitiveSimple
import net.akehurst.language.asm.api.AsmValue
import net.akehurst.language.asm.simple.asmSimple
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.builder.typeModel
import kotlin.test.Test
import kotlin.test.assertEquals

class test_SimpleTypeModelStdLib_eval {

    companion object {
        fun test(typeModel: TypeModel, self: AsmValue, expression: String, expected: AsmValue) {
            val st = typeModel.findByQualifiedNameOrNull(self.qualifiedTypeName)?.type() ?: SimpleTypeModelStdLib.AnyType
            val interpreter = ExpressionsInterpreterOverTypedObject(typeModel)
            val actual = interpreter.evaluateStr(EvaluationContext.ofSelf(self.toTypedObject(st)), expression)
            assertEquals(expected, actual.asmValue)
        }
    }

    @Test
    fun collection_List_size__empty() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("Test") {
                    propertyListTypeOf("list", "std.String", false, 0)
                }
            }
        }
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf())
            }
        }
        val self = asm.root[0]

        test(tm, self, "list.size", AsmPrimitiveSimple.stdInteger(0))
    }

    @Test
    fun collection_List_size() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("Test") {
                    propertyListTypeOf("list", SimpleTypeModelStdLib.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val self = asm.root[0]
        test(tm, self, "list.size", AsmPrimitiveSimple(SimpleTypeModelStdLib.Integer.qualifiedTypeName, 4))
    }

    @Test
    fun collection_List_size__missing_prop_name() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("Test") {
                    propertyListTypeOf("list", SimpleTypeModelStdLib.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val self = asm.root[0]
        test(tm, self, "list2.size", AsmNothingSimple)
    }

    @Test
    fun collection_List_first() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("Test") {
                    propertyListTypeOf("list", SimpleTypeModelStdLib.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val self = asm.root[0]
        test(tm, self, "list.first", AsmPrimitiveSimple(SimpleTypeModelStdLib.String.qualifiedTypeName, "A"))
    }

    @Test
    fun collection_List_last() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("Test") {
                    propertyListTypeOf("list", SimpleTypeModelStdLib.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val self = asm.root[0]
        test(tm, self, "list.last", AsmPrimitiveSimple(SimpleTypeModelStdLib.String.qualifiedTypeName, "D"))
    }

    @Test
    fun collection_List_back() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("Test") {
                    propertyListTypeOf("list", SimpleTypeModelStdLib.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val self = asm.root[0]
        test(tm, self, "list.back", AsmListSimple(listOf("B", "C", "D").map { AsmPrimitiveSimple(SimpleTypeModelStdLib.String.qualifiedTypeName, it) }))
    }

    @Test
    fun collection_List_front() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("Test") {
                    propertyListTypeOf("list", SimpleTypeModelStdLib.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val self = asm.root[0]
        test(tm, self, "list.front", AsmListSimple(listOf("A", "B", "C").map { AsmPrimitiveSimple(SimpleTypeModelStdLib.String.qualifiedTypeName, it) }))
    }

    @Test
    fun collection_List_join() {
        val tm = typeModel("test", true) {
            namespace("ns") {
                dataType("Test") {
                    propertyListTypeOf("list", SimpleTypeModelStdLib.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val asm = asmSimple {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val self = asm.root[0]
        test(tm, self, "list.join", AsmPrimitiveSimple(SimpleTypeModelStdLib.String.qualifiedTypeName, "ABCD"))
    }
}