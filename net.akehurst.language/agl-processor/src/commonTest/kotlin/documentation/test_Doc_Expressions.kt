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

package net.akehurst.language.documentation

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.documentation.test_Doc_GettingStarted.Companion.SHAPES_SENTENCE
import net.akehurst.language.documentation.test_Doc_GettingStarted.Companion.shapesProcessor
import net.akehurst.language.expressions.processor.objectGraphSimpleAsm
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The examples in 5_Documentation/dsl/expressions.md.
 */
class test_Doc_Expressions {

    @Test
    fun evaluate() {
        // expressions.md: "Example"
        val asm = shapesProcessor.process(SHAPES_SENTENCE).asm!!
        val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        val objectGraph = objectGraphSimpleAsm(shapesProcessor.typesDomain, null, issues, LocationMapDefault())
        val unit = asm.root[0]

        fun eval(expression: String) = Agl.executeExpression(objectGraph, unit, expression).untyped

        assertEquals(3L, eval("item.size"))
        assertEquals("c", eval("item[0].name"))
        assertEquals("5", eval("item[0].radius.number"))
        assertEquals("1", eval("item[1].point[0].number"))
        assertEquals(true, eval("item[0] is Circle"))
        assertEquals("c->s", eval("item[2].source + '->' + item[2].target"))
        assertEquals(7L, eval("1 + 2 * 3"))
        assertEquals("three", eval("when { item.size == 3 -> 'three' else -> 'other' }"))
        assertEquals("s", eval("with(item[1]) name"))
        assertEquals(6L, eval("{ x := 2 x * 3 }"))
        assertEquals(unit, eval("\$self"))
        assertEquals("c", eval("item.first.name"))
        assertEquals("s", eval("item.last.target"))
        assertEquals(false, eval("item.isEmpty"))
        assertEquals("s", eval("item.get(1).name"))
        assertEquals(emptyList(), issues.all.toList())
    }

    @Test
    fun lambda_condition_precedence() {
        // expressions.md: "Example" (rows for lambdas, '? :' and 'and')
        val asm = shapesProcessor.process(SHAPES_SENTENCE).asm!!
        val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        val objectGraph = objectGraphSimpleAsm(shapesProcessor.typesDomain, null, issues, LocationMapDefault())
        val unit = asm.root[0]

        fun eval(expression: String) = Agl.executeExpression(objectGraph, unit, expression).untyped

        assertEquals(listOf("1", "3"), eval("item[1].point.map({ p -> p.number })"))
        assertEquals(2L, eval("item.filter({ i -> i is Shape }).size"))
        assertEquals("many", eval("item.size > 2 ? 'many' : 'few'"))
        assertEquals(true, eval("item.size > 2 and true"))
        assertEquals(emptyList(), issues.all.toList())
    }
}
