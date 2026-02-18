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
import net.akehurst.language.agl.expressions.processor.ObjectGraphAccessorMutatorSuspendingByReflection
import net.akehurst.language.agl.expressions.processor.StdLibPrimitiveExecutionsForReflectionSuspending
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals

class test_StdLibPrimitiveExecutionsForReflectionSuspending_eval {

    private companion object Companion {
        data class TestObj(
            val list: List<String>
        )

        data class TestObjSet(
            val set: Set<String>
        )

        data class TestContainer(
            val id: String,
            val list: List<TestContainer>
        )

        suspend fun test(typesDomain: TypesDomain, self: Any, selfType: String, expression: String, expected: Any) {
            val st = typesDomain.findByQualifiedNameOrNull(selfType.asQualifiedName)?.type() ?: StdLibDefault.AnyType
            val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
            val og = ObjectGraphAccessorMutatorSuspendingByReflection(typesDomain, issues, primitiveExecutor = StdLibPrimitiveExecutionsForReflectionSuspending(issues))
            val interpreter = ExpressionsInterpreterOverTypedObjectSuspending(og, issues)
            val actual = interpreter.evaluateStr(EvaluationContext.ofSelf(og.typedAs(self, st)), expression)
            assertEquals(expected, og.untyped(actual))
        }
    }

    @Test
    fun collection_List_size__empty() = runTest {
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
    fun collection_List_size() = runTest {
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
    fun collection_List_size__missing_prop_name() = runTest {
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
    fun collection_List_first() = runTest {
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
    fun collection_List_last() = runTest {
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
    fun collection_List_back() = runTest {
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
    fun collection_List_front() = runTest {
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
    fun collection_List_join() = runTest {
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

    @Test
    fun collection_List_map() = runTest {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("list", StdLibDefault.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val self = TestObj(listOf("A", "B", "C", "D"))
        test(tm, self, "ns.TestObj", "list.map({ it + '1' })", listOf("A1", "B1", "C1", "D1"))
    }

    @Test
    fun collection_Set_map() = runTest {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestObjSet") {
                    propertyListTypeOf("list", StdLibDefault.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val self = TestObjSet(setOf("A", "B", "C", "D"))
        test(tm, self, "ns.TestObjSet", "set.map({ it + '1' })", listOf("A1", "B1", "C1", "D1"))
    }

    @Test
    fun collection_List_filter() = runTest {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("Test") {
                    propertyListTypeOf("list", StdLibDefault.String.qualifiedTypeName.value, false, 0)
                }
            }
        }
        val self = TestObj(listOf("A", "B", "C", "D"))
        test(tm, self, "ns.TestObj", "list.filter({ it != 'B' })", listOf("A", "C", "D"))
    }

    @Test
    fun collection_List_transitiveClosure() = runTest {
        val tm = typesDomain("test", true) {
            namespace("ns") {
                data("TestContainer") {
                    propertyOf(setOf(VAR), "list", "TestContainer", false, TestContainer::list)
                }
            }
        }
        val self = TestContainer(
            "1",
            listOf(
                TestContainer(
                    "1.1",
                    listOf(TestContainer("1.1.1", emptyList()))
                ),
                TestContainer(
                    "1.2",
                    emptyList()
                ),
                TestContainer(
                    "1.3",
                    listOf(TestContainer("1.3.1", emptyList()))
                )
            )
        )
        val expected = listOf("1.1", "1.2", "1.3", "1.1.1", "1.3.1")
        test(tm, self, "ns.TestContainer", "list.transitiveClosure({ it.list }).map({it.id})", expected)
    }
}