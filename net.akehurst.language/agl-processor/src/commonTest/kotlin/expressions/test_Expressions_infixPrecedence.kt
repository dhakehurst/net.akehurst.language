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

package net.akehurst.language.expressions.processor

import net.akehurst.language.agl.Agl
import net.akehurst.kotlinx.utils.Indent
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.GroupExpression
import net.akehurst.language.expressions.api.InfixExpression
import net.akehurst.language.expressions.asm.groupedByPrecedence
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

/**
 * Operator precedence is semantic: the ASM keeps an infix chain flat, as written,
 * and the interpreter and type resolver group it by precedence.
 */
class test_Expressions_infixPrecedence {

    private fun parse(expression: String): Expression {
        val result = Agl.registry.agl.expressions.processor!!.process(expression)
        assertEquals(emptyList(), result.allIssues.errors.toList(), expression)
        return result.asm!!
    }

    /** fully parenthesised form of the grouped chain */
    private fun grouping(e: Expression): String = when (e) {
        is InfixExpression -> {
            val g = e.groupedByPrecedence
            when {
                g !== e -> grouping(g)
                else -> "(" + grouping(e.expressions[0]) + " " + e.operators[0] + " " + grouping(e.expressions[1]) + ")"
            }
        }

        else -> e.asString(Indent(), emptyList())
    }

    @Test
    fun asm_keeps_chain_flat() {
        val asm = parse("1 + 2 * 3 - 4")
        assertIs<InfixExpression>(asm)
        assertEquals(listOf("+", "*", "-"), asm.operators)
        assertEquals(4, asm.expressions.size)
        asm.expressions.forEach { kotlin.test.assertFalse(it is InfixExpression, "$it") }
    }

    @Test
    fun asString_is_as_written() {
        listOf(
            "1 + 2 * 3",
            "a or b and c == d",
            "(1 + 2) * 3",
            "a < b and (c or d)",
        ).forEach {
            assertEquals(it, parse(it).asString(Indent(), emptyList()))
        }
    }

    @Test
    fun group_stays_its_own_node() {
        val asm = parse("(1 + 2) * 3")
        assertIs<InfixExpression>(asm)
        assertEquals(listOf("*"), asm.operators)
        assertIs<GroupExpression>(asm.expressions[0])
    }

    @Test
    fun single_operator_is_not_regrouped() {
        val asm = parse("a == b")
        assertIs<InfixExpression>(asm)
        assertSame(asm, asm.groupedByPrecedence)
    }

    @Test
    fun grouping_is_computed_once() {
        val asm = parse("1 + 2 * 3")
        assertIs<InfixExpression>(asm)
        assertSame(asm.groupedByPrecedence, asm.groupedByPrecedence)
    }

    @Test
    fun grouping_by_precedence() {
        listOf(
            "1 + 2 * 3" to "(1 + (2 * 3))",
            "2 * 3 + 1" to "((2 * 3) + 1)",
            "10 - 2 - 3" to "((10 - 2) - 3)",
            "a or b and c" to "(a or (b and c))",
            "a and b or c" to "((a and b) or c)",
            "a xor b or c and d" to "((a xor b) or (c and d))",
            "1 + 2 == 3" to "((1 + 2) == 3)",
            "a < b == c > d" to "((a < b) == (c > d))",
            "(1 + 2) * 3" to "((1 + 2) * 3)",
        ).forEach { (e, expected) ->
            assertEquals(expected, grouping(parse(e)), e)
        }
    }

    @Test
    fun type_resolver_types_grouped_chain() {
        val tm = typesDomain("Test", true) {
            namespace("test") {
                data("Test") {
                    propertyPrimitiveType("num", "Integer", false, 0)
                }
            }
        }
        val dt = tm.findFirstDefinitionByPossiblyQualifiedNameOrNull("test.Test".asPossiblyQualifiedName)!!
        val typeResolver = ExpressionTypeResolver(tm, StdLibDefault, IssueHolder(LanguageProcessorPhase.ALL))
        // with the current operator typing ("+" etc. give the lhs type) grouping does not change the type,
        // this checks the grouped chain is typed
        assertEquals(StdLibDefault.Boolean, typeResolver.typeOfExpressionStr("num + 1 == 2", dt))
        assertEquals(StdLibDefault.Boolean, typeResolver.typeOfExpressionStr("num == 1 + 2", dt))
        assertEquals(StdLibDefault.Integer, typeResolver.typeOfExpressionStr("num + 1 * 2", dt))
    }
}
