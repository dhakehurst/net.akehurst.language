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

import net.akehurst.language.agl.expressions.processor.ExpressionTypeResolver
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.builder.typeModel
import kotlin.test.Test
import kotlin.test.assertEquals

class test_ExpressionsSemantics {

    @Test
    fun typeOfExpressionStr__list() {

        val tm = typeModel("Test", true) {
            namespace("test") {
                dataType("Test") {
                    propertyListType("list", false, 0) {
                        this.ref("String")
                    }
                }
            }
        }
        val dt = tm.findFirstByNameOrNull(SimpleName("Test"))!!

        val expression = "list"
        val typeResolver = ExpressionTypeResolver(tm)
        val actual = typeResolver.typeOfExpressionStr(expression,dt)

        assertEquals(SimpleTypeModelStdLib.List.type(listOf(SimpleTypeModelStdLib.String.asTypeArgument)), actual)
    }

    @Test
    fun typeOfExpressionStr__list_front() {

        val tm = typeModel("Test", true) {
            namespace("test") {
                dataType("Test") {
                    propertyListType("list", false, 0) {
                        this.ref("String")
                    }
                }
            }
        }
        val dt = tm.findFirstByNameOrNull(SimpleName("Test"))!!

        val expression = "list.front"

        val typeResolver = ExpressionTypeResolver(tm)
        val actual = typeResolver.typeOfExpressionStr(expression,dt)

        assertEquals(SimpleTypeModelStdLib.List.type(listOf(SimpleTypeModelStdLib.String.asTypeArgument)), actual)
    }

    @Test
    fun typeOfExpressionStr__list_front_join() {

        val tm = typeModel("Test", true) {
            namespace("test") {
                dataType("Test") {
                    propertyListType("list", false, 0) {
                        this.ref("String")
                    }
                }
            }
        }
        val dt = tm.findFirstByNameOrNull(SimpleName("Test"))!!

        val expression = "list.front.join"

        val typeResolver = ExpressionTypeResolver(tm)
        val actual = typeResolver.typeOfExpressionStr(expression,dt)

        assertEquals(SimpleTypeModelStdLib.String, actual)
    }

}