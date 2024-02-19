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

package net.akehurst.language.agl.language.expressions

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.grammarTypeModel.grammarTypeModel
import net.akehurst.language.typemodel.test.TypeModelTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_ExpressionsLanguage {

    private companion object {
        val sentences = listOf(
            "\$self",
            "\$nothing",
            "true",
            "false",
            "0",
            "456",
            "3.141",
            "'hello world!'",
            "aaa.bbb.ccc.ddd.ee"
        )
    }

    @Test
    fun check_grammar() {
        val proc = Agl.registry.agl.expressions.processor
        assertTrue(Agl.registry.agl.expressions.issues.errors.isEmpty(), Agl.registry.agl.expressions.issues.toString())
        assertNotNull(proc)
    }

    @Test
    fun check_typeModel() {
        val proc = Agl.registry.agl.expressions.processor!!
        val actual = proc.typeModel
        assertTrue(Agl.registry.agl.expressions.issues.errors.isEmpty(), Agl.registry.agl.expressions.issues.toString())

        val expected = grammarTypeModel("net.akehurst.language.agl.Expressions", "Expressions", "") {
            stringTypeFor("BOOLEAN")
            stringTypeFor("IDENTIFIER")
            stringTypeFor("INTEGER")
            stringTypeFor("NOTHING")
            stringTypeFor("REAL")
            stringTypeFor("SELF")
            stringTypeFor("STRING")
            stringTypeFor("root")
            stringTypeFor("literal")
            dataType("navigation", "Navigation")
            dataType("propertyReference", "")
            dataType("qualifiedName", "")
        }
        TypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test
    fun parse() {
        val processor = Agl.registry.agl.expressions.processor!!
        for (s in sentences) {
            println("Parsing '$s'")
            val result = processor.parse(s)
            assertTrue(result.issues.errors.isEmpty(), "'$s'\n${result.issues}")
        }
    }

    @Test
    fun process() {
        val processor = Agl.registry.agl.expressions.processor!!
        for (s in sentences) {
            println("Processing '$s'")
            val result = processor.process(s)
            assertTrue(result.issues.errors.isEmpty(), "'$s'\n${result.issues}")
        }
    }
}