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

import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.types.asm.StdLibDefault
import kotlin.test.Test
import kotlin.test.assertEquals


class test_TypesBuilder {
    companion object {
        data class DataClass(val id:String) {

        }
    }

    @Test
    fun derivedPropertyWithKotlinAccessor() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("DataClass") {
                    propertyPrimitive<DataClass,String>("derProp", "String", false, accessor = { "Hello World!" })
                }
            }
        }
        val issues = IssueHolder()
        val interpret = ExpressionsInterpreterOverTypedObject(ObjectGraphAccessorMutatorByReflection(types, issues, LocationMapDefault()))

        val actual = interpret.evaluateStr(EvaluationContext.ofSelf(interpret.objectGraph.toTypedObject(DataClass("id1"), StdLibDefault.AnyType)),$$"$self.derProp")
        assertEquals("Hello World!", actual.self)
    }


    @Test
    fun propertyPrimitive_via_lambda() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("DataClass") {
                    propertyPrimitive<DataClass,String>("derProp", "String", false, accessor = { "Hello World!" })
                }
            }
        }
        val issues = IssueHolder()
        val interpret = ExpressionsInterpreterOverTypedObject(ObjectGraphAccessorMutatorByReflection(types, issues, LocationMapDefault()))

        val actual = interpret.evaluateStr(EvaluationContext.ofSelf(interpret.objectGraph.toTypedObject(DataClass("id1"), StdLibDefault.AnyType)),$$"$self.derProp")
        assertEquals("Hello World!", actual.self)
    }

    @Test
    fun propertyPrimitive_accessor_via_property() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("DataClass") {
                    propertyPrimitive("derProp", "String", false, accessor = DataClass::id)
                }
            }
        }
    }

    @Test
    fun propertyOf_no_accessor() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("DataClass") {
                    propertyOf(setOf(),"derProp", "String", false)
                }
            }
        }
        val issues = IssueHolder()
        val interpret = ExpressionsInterpreterOverTypedObject(ObjectGraphAccessorMutatorByReflection(types, issues, LocationMapDefault()))

        val actual = interpret.evaluateStr(EvaluationContext.ofSelf(interpret.objectGraph.toTypedObject(DataClass("id1"), StdLibDefault.AnyType)),$$"$self.derProp")
        assertEquals("Hello World!", actual.self)
    }

    @Test
    fun propertyOf_vai_lambda() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("DataClass") {
                    propertyOfWithBinding<DataClass,String>(setOf(),"derProp", "String", false, accessor = { o ->  "Hello World!" })
                }
            }
        }
        val issues = IssueHolder()
        val interpret = ExpressionsInterpreterOverTypedObject(ObjectGraphAccessorMutatorByReflection(types, issues, LocationMapDefault()))

        val actual = interpret.evaluateStr(EvaluationContext.ofSelf(interpret.objectGraph.toTypedObject(DataClass("id1"), StdLibDefault.AnyType)),$$"$self.derProp")
        assertEquals("Hello World!", actual.self)
    }

    @Test
    fun propertyOf_vai_property() {
        val types = typesDomain("Test", true) {
            namespace("test") {
                data("DataClass") {
                    propertyOfWithBinding(setOf(),"derProp", "String", false, accessor = DataClass::id)
                }
            }
        }
        val issues = IssueHolder()
        val interpret = ExpressionsInterpreterOverTypedObject(ObjectGraphAccessorMutatorByReflection(types, issues, LocationMapDefault()))

        val actual = interpret.evaluateStr(EvaluationContext.ofSelf(interpret.objectGraph.toTypedObject(DataClass("id1"), StdLibDefault.AnyType)),$$"$self.derProp")
        assertEquals("Hello World!", actual.self)
    }
}