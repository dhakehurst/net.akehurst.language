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

package net.akehurst.language.agl.grammarTypeModel


import net.akehurst.language.typemodel.asm.test.TypeModelTest
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.api.TypeNamespace
import kotlin.test.fail

object GrammarTypeModelTest {

    fun tmAssertEquals(expected: TypeModel?, actual: TypeModel?) {
        kotlin.test.assertEquals(expected?.asString(), actual?.asString())
        when {
            (expected == null && actual == null) -> Unit // pass
            expected == null -> fail()
            actual == null -> fail()
            else -> {
                kotlin.test.assertEquals(expected.allNamespace.size, actual.allNamespace.size, "number of namespaces in model is different")
                for (k in expected.allNamespace.indices) {
                    val expEl = expected.allNamespace[k]
                    val actEl = actual.allNamespace[k]
                    when {
                        expEl is GrammarTypeNamespace && actEl is GrammarTypeNamespace -> GrammarTypeModelTest.tmAssertEquals(expEl, actEl, "GrammarTypeNamespace")
                        else -> TypeModelTest.tmAssertEquals(expEl, actEl, "TypeNamespace")
                    }

                }
            }
        }
    }

    fun tmAssertEquals(expected: GrammarTypeNamespace?, actual: GrammarTypeNamespace?, source: String) {
        kotlin.test.assertEquals(expected?.asString(), actual?.asString())
        when {
            (expected == null && actual == null) -> Unit // pass
            expected == null -> fail()
            actual == null -> fail()
            else -> {
                kotlin.test.assertEquals(expected.allRuleNameToType.size, actual.allRuleNameToType.size, "number of types in model is different")
                for (k in expected.allRuleNameToType.keys) {
                    val expEl = expected.allRuleNameToType[k]
                    val actEl = actual.allRuleNameToType[k]
                    TypeModelTest.tmAssertEquals(expEl, actEl, "TypeModel")
                }
                TypeModelTest.tmAssertEquals(expected as TypeNamespace?, actual as TypeNamespace?, "")
            }
        }
    }

}