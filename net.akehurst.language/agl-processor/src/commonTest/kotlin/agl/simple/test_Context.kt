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

package net.akehurst.language.agl.simple

import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals


class test_Context {

    @Test
    fun isEmpty() {
        val sut = ContextWithScope<Any,Any>()
        val actual = sut.isEmpty
        assertEquals(true, actual)
    }

    @Test
    fun asString__empty() {
        val sut = ContextWithScope<Any,Any>()
        val actual = sut.asString()
        val expected = "context { }"
        assertEquals(expected, actual)
    }

    @Test
    fun asString__1_sentence_3_items() {
        val sut = ContextWithScope<Any,Any>()
        sut.addToScope(1, listOf("item","qual","1"), QualifiedName("Type"), null, "TheItemStored_1")
        sut.addToScope(1, listOf("item","qual","2"), QualifiedName("Type"), null, "TheItemStored_2")
        sut.addToScope(1, listOf("item","qual","3"), QualifiedName("Type"), null, "TheItemStored_3")
        val actual = sut.asString()
        val expected = """
            context {
              sentence 1 = {
                scope 'item' {
                  scope 'qual' {
                    item '1' {
                      '1': Type -> (null, TheItemStored_1)
                    }
                    item '2' {
                      '2': Type -> (null, TheItemStored_2)
                    }
                    item '3' {
                      '3': Type -> (null, TheItemStored_3)
                    }
                  }
                }
              }
            }
        """.trimIndent()
        assertEquals(expected, actual)
    }

    @Test
    fun asString__3_sentence_1_item() {
        val sut = ContextWithScope<Any,Any>()
        sut.addToScope(1, listOf("item","qual","1"), QualifiedName("Type"), null, "TheItemStored_1")
        sut.addToScope(2, listOf("item","qual","2"), QualifiedName("Type"), null, "TheItemStored_2")
        sut.addToScope(3, listOf("item","qual","3"), QualifiedName("Type"), null, "TheItemStored_3")
        val actual = sut.asString()
        val expected = """
            context {
              sentence 1 = {
                scope 'item' {
                  scope 'qual' {
                    item '1' {
                      '1': Type -> (null, TheItemStored_1)
                    }
                  }
                }
              }
              sentence 2 = {
                scope 'item' {
                  scope 'qual' {
                    item '2' {
                      '2': Type -> (null, TheItemStored_2)
                    }
                  }
                }
              }
              sentence 3 = {
                scope 'item' {
                  scope 'qual' {
                    item '3' {
                      '3': Type -> (null, TheItemStored_3)
                    }
                  }
                }
              }
            }
        """.trimIndent()
        assertEquals(expected, actual)
    }

    @Test
    fun addToScope() {
        val sut = ContextWithScope<Any,Any>()
        sut.addToScope(1, listOf("item","qual","name"), QualifiedName("Type"), InputLocation(1,2,3,4,5), "TheItemStored")

        val expected = contextAsmSimple() {
            forSentence(1) {
                scope("item", "??") {
                    scope("qual", "??") {
                        item("name", "Type", InputLocation(1, 2, 3, 4, 5), "TheItemStored")
                    }
                }
            }
        }

        assertEquals(expected.asString(), sut.asString())
        val expectedString = """
            context {
              sentence 1 = {
                scope 'item' {
                  scope 'qual' {
                    item 'name' {
                      'name': Type -> (InputLocation(position=1, column=2, line=3, length=4, sentenceIdentity=5), TheItemStored)
                    }
                  }
                }
              }
            }
        """.trimIndent()
        assertEquals(expectedString, sut.asString())
    }

}