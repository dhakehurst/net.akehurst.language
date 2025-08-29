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


import net.akehurst.language.grammarTypemodel.api.GrammarTypesNamespace
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.api.TypesNamespace
import net.akehurst.language.types.test.TypesDomainTest
import kotlin.test.fail

object GrammarTypeModelTest {

    fun tmAssertEquals(expected: TypesDomain?, actual: TypesDomain?) {
        kotlin.test.assertEquals(expected?.asString(), actual?.asString())
        when {
            (expected == null && actual == null) -> Unit // pass
            expected == null -> fail()
            actual == null -> fail()
            else -> {
                kotlin.test.assertEquals(expected.namespace.size, actual.namespace.size, "number of namespaces in model is different")
                val expSorted = expected.namespace.sortedBy { it.qualifiedName.value }
                val actSorted = expected.namespace.sortedBy { it.qualifiedName.value }
                for (k in expSorted.indices) {
                    val expEl = expSorted[k]
                    val actEl = actSorted[k]
                    when {
                        expEl is GrammarTypesNamespace && actEl is GrammarTypesNamespace -> GrammarTypeModelTest.tmAssertEquals(expEl, actEl, "GrammarTypeNamespace")
                        else -> TypesDomainTest.tmAssertEquals(expEl, actEl, "TypeNamespace")
                    }

                }
            }
        }
    }

    fun tmAssertEquals(expected: GrammarTypesNamespace?, actual: GrammarTypesNamespace?, source: String) {
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
                    TypesDomainTest.tmAssertEquals(expEl, actEl, "TypesDomain")
                }
                TypesDomainTest.tmAssertEquals(expected as TypesNamespace?, actual as TypesNamespace?, "")
            }
        }
    }

}