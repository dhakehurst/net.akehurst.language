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
import net.akehurst.language.types.test.TypesDomainTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_ExpressionsLanguage {

    private companion object {
        val sentences = listOf(
            // root
            $$"$self",
            $$"$nothing",
            $$"$group",
            $$"$alternative",
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
            "a.method(1)",
            "a.method(1,true,'a')",
//TODO            "a.method(a.b.c, 4+5, a.b*(2+y))",
            "a.method() { e }",
            "a.method(a,b,c) { e }",
            "a[1]",
            "a.b[1]",
            "a.b.c[1]",
            "a.b[1].c",
            "a[1].b.c",
            $$"tup.$group",
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
            $$"tuple { a:= 1 b:=$self }",
            "tuple { a:= 1 b:=x.y.x c:= a[1].f().z }",
            $$"tuple { $group:= 'a' }",
            // object
            "A()",
            "A(v:=true) { a:= 1 }",
            $$"A(s:='d' o:=x.y.z v:=$self) { a:= 1 b:=$self }",
            $$"A(o:=a[1].f() v:=$self.f() b:=true) { a:= 1 b:=x.y.x c:= a[1].f().z }",
            // with
            "with(1) true",
            $$"with(a[1].f().z) A(s:='d' o:=x.y.z v:=$self) { a:= 1 b:=$self }",
            // when
            "when { true -> 1 else -> false }",
            "when{1+1->2 else->3}",
            "when { z==1 -> 2  x!=2 -> x.y  a[7].f() -> x.y().d[9] else -> x}",
            "when { a+b-c -> 2 else -> 5 }",
            // cast
            "a as B",
            "a.b.c as D",
            "a.b.c as D<A>",
            "a.b.c as D<A,B,C>",
            "a.b.c as D<A,B<F>,C<H,H>>",
            //group
            "(a)",
            "(a+b)-c",
            "(a+b)-(c.fun(d))",
            "(a+b) as C",
            "(a as C).f"
        )
    }

    @Test
    fun check_grammar() {
        val proc = Agl.registry.agl.expressions.processor
        assertTrue(Agl.registry.agl.expressions.issues.errors.isEmpty(), Agl.registry.agl.expressions.issues.toString())
        assertNotNull(proc)
    }

    @Ignore
    @Test
    fun check_typeModel() {
        val proc = Agl.registry.agl.expressions.processor!!
        val actual = proc.typesDomain
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
            dataFor("navigation", "Navigation")
            dataFor("propertyReference", "")
            dataFor("qualifiedName", "")
        }
        TypesDomainTest.tmAssertEquals(expected, actual)
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
            assertTrue(result.allIssues.errors.isEmpty(), "'$s'\n${result.allIssues}")
        }
    }
}