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

import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.asm.api.AsmValue
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.asm.simple.AsmListSimple
import net.akehurst.language.asm.simple.AsmNothingSimple
import net.akehurst.language.asm.simple.AsmPrimitiveSimple
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals

class test_StdLibPrimitiveExecutionsForAsmSimple_eval {

    companion object Companion {
        fun test(typesDomain: TypesDomain, self: AsmValue, expression: String, expected: AsmValue) {
            val st = typesDomain.findByQualifiedNameOrNull(self.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val interpreter = ExpressionsInterpreterOverTypedObject(ObjectGraphAccessorMutatorAsmSimple(typesDomain, issues, primitiveExecutor = StdLibPrimitiveExecutionsForAsmSimple), issues)
            val actual = interpreter.evaluateStr(EvaluationContext.ofSelf(TypedObjectAsmValue(st, self)), expression)
            assertEquals(expected, actual.self)
        }
    }

    @Test
    fun collection_List_size__empty() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("list", "std.String", false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyListOfString("list", listOf())
            }
        }
        val self = asm.root[0]

        test(tm, self, "list.size", AsmPrimitiveSimple.stdInteger(0))
    }

    @Test
    fun collection_List_size() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("list", StdLibDefault.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val self = asm.root[0]
        test(tm, self, "list.size", AsmPrimitiveSimple.stdInteger(4L))
    }

    @Test
    fun collection_List_size__missing_prop_name() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("list", StdLibDefault.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val self = asm.root[0]
        test(tm, self, "list2.size", AsmNothingSimple)
    }

    @Test
    fun collection_List_first() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("list", StdLibDefault.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val self = asm.root[0]
        test(tm, self, "list.first", AsmPrimitiveSimple.stdString("A"))
    }

    @Test
    fun collection_List_last() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("list", StdLibDefault.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val self = asm.root[0]
        test(tm, self, "list.last", AsmPrimitiveSimple.stdString("D"))
    }

    @Test
    fun collection_List_back() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("list", StdLibDefault.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val self = asm.root[0]
        test(tm, self, "list.back", AsmListSimple(listOf("B", "C", "D").map { AsmPrimitiveSimple.stdString(it) }))
    }

    @Test
    fun collection_List_front() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("list", StdLibDefault.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val self = asm.root[0]
        test(tm, self, "list.front", AsmListSimple(listOf("A", "B", "C").map { AsmPrimitiveSimple.stdString(it) }))
    }

    @Test
    fun collection_List_join() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("list", StdLibDefault.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val self = asm.root[0]
        test(tm, self, "list.join", AsmPrimitiveSimple.stdString("ABCD"))
    }

    @Test
    fun collection_List_map() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("list", StdLibDefault.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val self = asm.root[0]
        test(tm, self, "list.map({ it + '1' })", AsmListSimple(listOf("A1", "B1", "C1", "D1").map { AsmPrimitiveSimple.stdString(it) }))
    }

    @Test
    fun collection_List_filter() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("list", StdLibDefault.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("Test") {
                propertyListOfString("list", listOf("A", "B", "C", "D"))
            }
        }
        val self = asm.root[0]
        test(tm, self, "list.filter({ it != 'B' })", AsmListSimple(listOf("A", "C", "D").map { AsmPrimitiveSimple.stdString(it) }))
    }

    @Test
    fun collection_List_transitiveClosure()  {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestContainer") {
                    propertyOf(setOf(VAR), "list", "TestContainer", false)
                }
            }
        }
        val asm = asmSimple(typesDomain = tm) {
            element("TestContainer") {
                propertyString("id", "1")
                propertyListOfElement("list") {
                    element("TestContainer") {
                        propertyString("id", "1.1")
                        propertyListOfElement("list") {
                            element("TestContainer") {
                                propertyString("id", "1.1.1")
                                propertyListOfElement("list") {}
                            }
                        }
                    }
                    element("TestContainer") {
                        propertyString("id", "1.2")
                        propertyListOfElement("list") { }
                    }
                    element("TestContainer") {
                        propertyString("id", "1.3")
                        propertyListOfElement("list") {
                            element("TestContainer") {
                                propertyString("id", "1.3.1")
                                propertyListOfElement("list") {}
                            }
                        }
                    }
                }
            }
        }
        val self = asm.root[0]
        val expected = AsmListSimple(listOf("1.1","1.2","1.3","1.1.1","1.3.1").map { AsmPrimitiveSimple.stdString(it) })
        test(tm, self, "list.transitiveClosure({ it.list }).map({it.id})", expected)
    }
}