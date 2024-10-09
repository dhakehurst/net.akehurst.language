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

import net.akehurst.language.agl.Agl
import net.akehurst.language.grammarTypemodel.builder.grammarTypeModel
import net.akehurst.language.typemodel.test.TypeModelTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_ExpressionsLanguage {

    private companion object {
        val sentences = listOf(
            // root
            "\$self",
            "\$nothing",
            "\$group",
            "\$alternative",
            "prop",
            // literal
            "true",
            "false",
            "0",
            "456",
            "3.141",
            "'hello world!'",
            // navigation
            "aaa.bbb.ccc.ddd.ee",
            "a.method()",
            "a.b.method()",
            "a.method().b",
            "a.method().a.b",
            "a[1]",
            "a.b[1]",
            "a.b.c[1]",
            "a.b[1].c",
            "a[1].b.c",
            "tup.\$group",
            // infix
            "0 == 0",
            "0==0",
            "0!=true",
            "a>=0",
            "a<=b",
            "1.34!=0",
            "a<b",
            "a>b",
            "a+b+c-d",
            "a*b/c%d+e-d",
            // tuple
            "tuple { a:= 1 }",
            "tuple { a:= 1 b:=\$self }",
            "tuple { a:= 1 b:=x.y.x c:= a[1].f().z }",
            "tuple { \$group:= 'a' }",
            // object
            "A()",
            "A(true) { a:= 1 }",
            "A('d',x.y.z,\$self) { a:= 1 b:=\$self }",
            "A(a[1].f(), \$self.f(), true) { a:= 1 b:=x.y.x c:= a[1].f().z }",
            // with
            "with(1) true",
            "with(a[1].f().z) A('d',x.y.z,\$self) { a:= 1 b:=\$self }",
            // when
            "when { true -> 1 }",
            "when{1+1->2}",
            "when { z==1 -> 2  x!=2 -> x.y  a[7].f() -> x.y().d[9] }",
            "when { a+b-c -> 2 }"
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

        val expected = grammarTypeModel("net.akehurst.language.agl.Expressions", "Expressions") {
            stringTypeFor("BOOLEAN")
            stringTypeFor("IDENTIFIER")
            stringTypeFor("INFIX_OPERATOR")
            stringTypeFor("INTEGER")
            stringTypeFor("SPECIAL")
            stringTypeFor("REAL")
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