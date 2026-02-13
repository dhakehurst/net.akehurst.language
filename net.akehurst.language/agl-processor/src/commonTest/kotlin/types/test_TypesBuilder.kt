/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.types.builder

import net.akehurst.language.agl.expressions.processor.ObjectGraphByReflection
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.EvaluationContext
import kotlin.test.Test
import kotlin.test.assertEquals


class test_TypesBuilder {
    companion object {
        data class DataClass(val id:String) {

        }
    }
    @Test
    fun derivedPropertyWithKotlinExection() {

        val types = typesDomain("Test", true) {
            namespace("test") {
                data("DataClass") {
                    derivedPropertyOf("derProp", "String", false, execution = { "Hello World!" })
                }
            }
        }
        val issues = IssueHolder()
        val interpret = ExpressionsInterpreterOverTypedObject(ObjectGraphByReflection(types, issues),issues)

        val actual = interpret.evaluateStr(EvaluationContext.ofSelf(interpret.objectGraph.toTypedObject(DataClass("id1"))),$$"$self.derProp")
        assertEquals("Hello World!", actual.self)
    }
}