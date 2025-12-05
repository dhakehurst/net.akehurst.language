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

import net.akehurst.language.agl.expressions.processor.ObjectGraphByReflection
import net.akehurst.language.agl.expressions.processor.StdLibPrimitiveExecutionsForReflection
import net.akehurst.language.agl.expressions.processor.TypedObjectAny
import net.akehurst.language.api.processor.EvaluationContext
import net.akehurst.language.asm.api.AsmValue
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.asm.simple.AsmListSimple
import net.akehurst.language.asm.simple.AsmNothingSimple
import net.akehurst.language.asm.simple.AsmPrimitiveSimple
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.ObjectGraphAccessorMutator
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals

class test_StdLibPrimitiveExecutionsForReflection_eval {

    companion object Companion {
        data class TestObj(
            val list: List<String>
        )

        fun test(typesDomain: TypesDomain, self: Any, selfType: String, expression: String, expected: Any) {
            val st = typesDomain.findByQualifiedNameOrNull(selfType.asQualifiedName)?.type() ?: StdLibDefault.AnyType
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val interpreter = ExpressionsInterpreterOverTypedObject(ObjectGraphByReflection(typesDomain, issues, primitiveExecutor = StdLibPrimitiveExecutionsForReflection(issues)), issues)
            val actual = interpreter.evaluateStr(EvaluationContext.ofSelf(TypedObjectAny(st, self)), expression)
            assertEquals(expected, actual.self)
        }
    }

    @Test
    fun collection_List_size__empty() {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestObj") {
                    propertyListTypeOf("list", "std.String", false, 0)
                }
            }
        }
        val self = TestObj(emptyList())

        test(tm, self, "ns.TestObj", "list.size", 0L)
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
        val self = TestObj(listOf("A", "B", "C", "D"))
        test(tm, self, "ns.TestObj", "list.size", 4L)
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
        val self = TestObj(listOf("A", "B", "C", "D"))
        test(tm, self, "ns.TestObj", "list2.size", Unit)
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
        val self = TestObj(listOf("A", "B", "C", "D"))
        test(tm, self, "ns.TestObj", "list.first", "A")
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
        val self = TestObj(listOf("A", "B", "C", "D"))
        test(tm, self, "ns.TestObj", "list.last", "D")
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
        val self = TestObj(listOf("A", "B", "C", "D"))
        test(tm, self, "ns.TestObj", "list.back", listOf("B", "C", "D"))
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
        val self = TestObj(listOf("A", "B", "C", "D"))
        test(tm, self, "ns.TestObj", "list.front", listOf("A", "B", "C"))
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
        val self = TestObj(listOf("A", "B", "C", "D"))
        test(tm, self, "ns.TestObj", "list.join", "ABCD")
    }
}