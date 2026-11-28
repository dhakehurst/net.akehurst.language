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

import kotlinx.coroutines.test.runTest
import net.akehurst.kotlinx.utils.Indent
import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.asPossiblyQualifiedName
import net.akehurst.language.expressions.api.*
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.types.api.TypeInstance
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Systematic tests of the expression language, its type resolver and its interpreter (sync and suspend),
 * one feature at a time. Only behaviour that is documented (5_Documentation/dsl/expressions.md) or otherwise agreed is asserted.
 */
class test_Expressions_systematic {

    private val tm = typesDomain("test", true) {
        namespace("ns") {
            data("Test") {
                propertyPrimitiveType("num", "Integer", false, 0)
                propertyPrimitiveType("real", "Real", false, 1)
                propertyPrimitiveType("str", "String", false, 2)
                propertyPrimitiveType("flag", "Boolean", false, 3)
                propertyPrimitiveType("opt", "String", true, 4)
                propertyListTypeOf("aList", "A", false, 5)
                propertyListTypeOf("empty", "A", false, 6)
                propertyDataTypeOf("one", "A", false, 7)
            }
            data("A") {
                propertyPrimitiveType("prop1", "String", false, 0)
            }
            data("B") {
                supertype("A")
                propertyPrimitiveType("prop2", "Integer", false, 1)
            }
        }
    }
    private val self = asmSimple(typesDomain = tm) {
        element("Test") {
            propertyInteger("num", 3L)
            propertyReal("real", 2.5)
            propertyString("str", "abc")
            propertyBoolean("flag", true)
            propertyNothing("opt")
            propertyListOfElement("aList") {
                element("A") { propertyString("prop1", "a1") }
                element("B") { propertyString("prop1", "b1"); propertyInteger("prop2", 7L) }
            }
            propertyListOfElement("empty") { }
            propertyElementExplicitType("one", "A") { propertyString("prop1", "o1") }
        }
    }.root[0]
    private val selfType = tm.findFirstDefinitionByPossiblyQualifiedNameOrNull("ns.Test".asPossiblyQualifiedName)!!

    private class Result(val value: Any?, val type: String, val issues: List<Pair<LanguageIssueKind, String>>)

    private suspend fun eval(expression: String, suspending: Boolean): Result {
        val issues = IssueHolder(LanguageProcessorPhase.INTERPRET)
        val interpreter = ExpressionsInterpreterOverTypedObjectCombined(objectGraphSimpleAsm(tm, null, issues, LocationMapDefault()))
        val evc = EvaluationContext.ofSelf(interpreter.objectGraph.typedAs(self, selfType.type()))
        val r = when {
            suspending -> interpreter.evaluateStrSuspend(evc, expression)
            else -> interpreter.evaluateStr(evc, expression)
        }
        return Result(r.untyped, r.type.qualifiedTypeName.value, issues.all.map { Pair(it.kind, it.message) })
    }

    /** evaluates sync and suspend, checks both give 'expected' with no issues */
    private suspend fun assertValue(expected: Any?, expression: String) {
        for (s in listOf(false, true)) {
            val r = eval(expression, s)
            assertEquals(emptyList(), r.issues, "issues of '$expression' (suspend=$s)")
            assertEquals(expected, r.value, "value of '$expression' (suspend=$s)")
        }
    }

    /** evaluates sync and suspend, checks both give $nothing and an issue of 'kind' */
    private suspend fun assertNothingWithIssue(kind: LanguageIssueKind, expression: String) {
        for (s in listOf(false, true)) {
            val r = eval(expression, s)
            assertEquals(StdLibDefault.NothingType.qualifiedTypeName.value, r.type, "type of '$expression' (suspend=$s)")
            assertTrue(r.issues.any { it.first == kind }, "expected a $kind for '$expression' (suspend=$s), got ${r.issues}")
        }
    }

    private fun typeOf(expression: String): TypeInstance {
        val issues = IssueHolder(LanguageProcessorPhase.ALL)
        val ns = tm.findNamespaceOrNull(QualifiedName("ns"))!!
        return ExpressionTypeResolver(tm, ns, issues).typeOfExpressionStr(expression, selfType)
    }

    private fun parse(expression: String): Expression {
        val result = Agl.registry.agl.expressions.processor!!.process(expression)
        assertEquals(emptyList(), result.allIssues.errors.toList(), expression)
        return result.asm!!
    }

    // ---------------- language ----------------

    @Test
    fun language_asm_kind_per_construct() {
        val cases: List<Pair<String, KClass<*>>> = listOf(
            "\$self" to RootExpression::class,
            "prop" to RootExpression::class,
            "\$nothing" to RootExpression::class,
            "1" to LiteralExpression::class,
            "-1" to LiteralExpression::class,
            "1.5" to LiteralExpression::class,
            "'x'" to LiteralExpression::class,
            "true" to LiteralExpression::class,
            "a.b" to NavigationExpression::class,
            "a.m(1)" to NavigationExpression::class,
            "a[1]" to NavigationExpression::class,
            "f(1)" to FunctionCall::class,
            "a ? b : c" to TernaryConditionExpression::class,
            "a + b" to InfixExpression::class,
            "tuple { a := 1 }" to CreateTupleExpression::class,
            "A() { a := 1 }" to CreateObjectExpression::class,
            "with(a) b" to WithExpression::class,
            "when { a -> 1 else -> 2 }" to WhenExpression::class,
            "a as B" to CastExpression::class,
            "a is B" to TypeTestExpression::class,
            "{ x -> x }" to LambdaExpression::class,
            "(a)" to GroupExpression::class,
            "{ x := 1 x }" to StatementBlockExpression::class,
        )
        for ((s, k) in cases) {
            val asm = parse(s)
            assertTrue(k.isInstance(asm), "'$s' gives ${asm::class.simpleName}, expected ${k.simpleName}")
        }
    }

    @Test
    fun language_asString_round_trip() {
        // single-line constructs are written exactly as given
        listOf(
            "\$self", "a.b.c", "a.m(1, 'x')", "a[1].b", "f(a, b)", "a ? b : c", "a + b * c", "(a + b) * c",
            "with(a) b", "a as B", "a is B", "a.&b", "a.\$type.name",
        ).forEach { s ->
            assertEquals(s, parse(s).asString(Indent(), emptyList()), s)
        }
        // multi-line constructs: asString parses back to the same asString
        listOf(
            "when { a -> 1 b -> 2 else -> 3 }", "tuple { a := 1 b := 'x' }", "A() { a := 1 }", "{ x := 1 y := x x + y }",
        ).forEach { s ->
            val s1 = parse(s).asString(Indent(), emptyList())
            val s2 = parse(s1).asString(Indent(), emptyList())
            assertEquals(s1, s2, s)
        }
    }

    @Test
    fun language_invalid_sentences_rejected() {
        val processor = Agl.registry.agl.expressions.processor!!
        listOf(
            "1 +", "a.", "a[", "a[1", "(a", "a)", "a ? b", "a ? b :", "when { }", "when { a -> 1 }",
            "tuple {", "A() {", "a as", "a is", "{ x -> }", "f(", "'abc", "a b",
        ).filter { s ->
            processor.parse(s, Agl.parseOptions { goalRuleName("expression") }).issues.errors.isEmpty()
        }.let { parsed -> assertEquals(emptyList(), parsed, "should not parse") }
    }

    // ---------------- type resolver ----------------

    @Test
    fun type_per_construct() {
        val cases = listOf(
            "1" to StdLibDefault.Integer,
            "1.5" to StdLibDefault.Real,
            "'x'" to StdLibDefault.String,
            "true" to StdLibDefault.Boolean,
            "\$nothing" to StdLibDefault.NothingType,
            "num" to StdLibDefault.Integer,
            "\$self.str" to StdLibDefault.String,
            "one.prop1" to StdLibDefault.String,
            "aList.size" to StdLibDefault.Integer,
            "str.length" to StdLibDefault.Integer,
            "1 + 2" to StdLibDefault.Integer,
            "'a' + 'b'" to StdLibDefault.String,
            "1 < 2" to StdLibDefault.Boolean,
            "true and false" to StdLibDefault.Boolean,
            "true xor false" to StdLibDefault.Boolean,
            "flag ? 1 : 2" to StdLibDefault.Integer,
            "when { flag -> 1 else -> 2 }" to StdLibDefault.Integer,
            "with(one) prop1" to StdLibDefault.String,
            "(num)" to StdLibDefault.Integer,
            "num.\$type.name" to StdLibDefault.String,
        )
        for ((e, expected) in cases) assertEquals(expected, typeOf(e), e)
    }

    @Test
    fun type_of_navigation_into_list() {
        assertEquals("ns.A", typeOf("aList[0]").qualifiedTypeName.value)
        assertEquals("ns.A", typeOf("aList.get(0)").qualifiedTypeName.value)
        assertEquals(StdLibDefault.String, typeOf("aList[0].prop1"))
    }

    // ---------------- interpreter ----------------

    @Test
    fun eval_literals_and_root() = runTest {
        assertValue(1L, "1")
        assertValue(-1L, "-1")
        assertValue(1.5, "1.5")
        assertValue("x", "'x'")
        assertValue(true, "true")
        assertValue(false, "false")
        assertValue(Unit, "\$nothing")
        assertValue(3L, "num")
        assertValue(3L, "\$self.num")
    }

    @Test
    fun eval_unset_nullable_property_is_nothing() = runTest {
        assertValue(Unit, "opt")
        assertValue(true, "opt == \$nothing")
    }

    @Test
    fun eval_unknown_property_is_nothing_with_warning() = runTest {
        assertNothingWithIssue(LanguageIssueKind.WARNING, "nonExist")
        assertNothingWithIssue(LanguageIssueKind.WARNING, "str.unknown")
        assertNothingWithIssue(LanguageIssueKind.WARNING, "opt.length")
    }

    @Test
    fun eval_arithmetic_integer() = runTest {
        assertValue(3L, "1 + 2")
        assertValue(-1L, "1 - 2")
        assertValue(6L, "2 * 3")
        assertValue(3L, "7 / 2")
        assertValue(1L, "7 % 3")
        assertValue(2L, "1 - -1")
        assertValue(9L, "num * num")
    }

    @Test
    fun eval_arithmetic_real() = runTest {
        assertValue(4.0, "1.5 + 2.5")
        assertValue(-1.0, "1.5 - 2.5")
        assertValue(3.75, "1.5 * 2.5")
        assertValue(5.0, "real * 2.0")
    }

    @Test
    fun eval_string_concatenation() = runTest {
        assertValue("ab", "'a' + 'b'")
        assertValue("abcd", "str + 'd'")
        assertValue("a1", "'a' + 1")
    }

    @Test
    fun eval_unsupported_operand_types_is_nothing_with_error() = runTest {
        assertNothingWithIssue(LanguageIssueKind.ERROR, "'a' - 'b'")
        assertNothingWithIssue(LanguageIssueKind.ERROR, "1 + 'a'")
        assertNothingWithIssue(LanguageIssueKind.ERROR, "1 < 'a'")
        assertNothingWithIssue(LanguageIssueKind.ERROR, "true < false")
        assertNothingWithIssue(LanguageIssueKind.ERROR, "1 and true")
        assertNothingWithIssue(LanguageIssueKind.ERROR, "true and 1")
    }

    @Test
    fun eval_comparison() = runTest {
        assertValue(true, "1 == 1")
        assertValue(false, "1 != 1")
        assertValue(true, "1 < 2")
        assertValue(true, "2 <= 2")
        assertValue(false, "1 > 2")
        assertValue(true, "2 >= 2")
        assertValue(true, "1.5 < 2.5")
        assertValue(true, "'a' == 'a'")
        assertValue(true, "'abc' < 'abd'")
        assertValue(true, "\$nothing == \$nothing")
        assertValue(true, "aList[0] == aList[0]")
        assertValue(false, "aList[0] == aList[1]")
    }

    @Test
    fun eval_logic() = runTest {
        for ((a, b) in listOf(true to true, true to false, false to true, false to false)) {
            assertValue(a && b, "$a and $b")
            assertValue(a || b, "$a or $b")
            assertValue(a xor b, "$a xor $b")
        }
    }

    @Test
    fun eval_precedence_and_associativity() = runTest {
        assertValue(7L, "1 + 2 * 3")
        assertValue(7L, "2 * 3 + 1")
        assertValue(5L, "1 + 2 * 3 - 4 / 2")
        assertValue(5L, "10 - 2 - 3")
        assertValue(2L, "8 / 2 / 2")
        assertValue(1L, "7 % 4 % 2")
        assertValue(9L, "(1 + 2) * 3")
        assertValue(13L, "2 * (3 + 4) - 1")
        assertValue(true, "1 + 2 == 3")
        assertValue(true, "1 < 2 == true")
        assertValue(true, "true or false and false")
        assertValue(true, "false and true or true")
        assertValue(true, "true xor false or false")
        assertValue(false, "true xor true and true")
    }

    @Test
    fun eval_ternary() = runTest {
        assertValue(1L, "flag ? 1 : 2")
        assertValue(2L, "flag == false ? 1 : 2")
        assertValue("many", "num > 2 ? 'many' : 'few'")
        assertValue("x", "true ? 'x' : 1")
        assertNothingWithIssue(LanguageIssueKind.ERROR, "1 ? 1 : 2")
        assertNothingWithIssue(LanguageIssueKind.ERROR, "\$nothing ? 1 : 2")
    }

    @Test
    fun eval_when() = runTest {
        assertValue(2L, "when { false -> 1 else -> 2 }")
        assertValue(1L, "when { true -> 1 else -> 2 }")
        // first true condition wins
        assertValue(2L, "when { false -> 1 num == 3 -> 2 num > 0 -> 3 else -> 4 }")
        assertValue("three", "when { num == 3 -> 'three' else -> 'other' }")
    }

    @Test
    fun eval_with() = runTest {
        assertValue("o1", "with(one) prop1")
        assertValue("b1", "with(aList[1]) prop1")
        assertValue(Unit, "with(\$nothing) prop1")
        assertNothingWithIssue(LanguageIssueKind.WARNING, "with(one) num")
    }

    @Test
    fun eval_type_test() = runTest {
        assertValue(true, "aList[0] is A")
        assertValue(false, "aList[0] is B")
        assertValue(true, "aList[1] is A")
        assertValue(true, "aList[1] is B")
        assertValue(false, "\$nothing is A")
        assertValue(true, "num is Integer")
        assertValue(false, "num is String")
    }

    @Test
    fun eval_cast_to_actual_type() = runTest {
        assertValue(7L, "(aList[1] as B).prop2")
        assertValue("b1", "(aList[1] as A).prop1")
    }

    @Test
    fun eval_navigation() = runTest {
        assertValue("o1", "one.prop1")
        assertValue(2L, "aList.size")
        assertValue(0L, "empty.size")
        assertValue(true, "empty.isEmpty")
        assertValue(false, "aList.isEmpty")
        assertValue("a1", "aList[0].prop1")
        assertValue("b1", "aList.get(1).prop1")
        assertValue("a1", "aList.first.prop1")
        assertValue("b1", "aList.last.prop1")
        assertValue(3L, "str.length")
    }

    @Test
    fun eval_index_out_of_range_is_nothing_with_error() = runTest {
        assertNothingWithIssue(LanguageIssueKind.ERROR, "aList[2]")
        assertNothingWithIssue(LanguageIssueKind.ERROR, "aList[-1]")
    }

    @Test
    fun eval_lambdas() = runTest {
        assertValue(listOf("a1", "b1"), "aList.map({ x -> x.prop1 })")
        assertValue(listOf("a1", "b1"), "aList.map({ it -> it.prop1 })")
        assertValue(1L, "aList.filter({ x -> x is B }).size")
        assertValue(0L, "empty.map({ x -> x.prop1 }).size")
        assertValue("a1b1", "aList.foldRight('', { x, acc -> x.prop1 + acc })")
    }

    @Test
    fun eval_tuple() = runTest {
        assertValue(1L, "(tuple { a := 1 b := 'x' }).a")
        assertValue("x", "(tuple { a := 1 b := 'x' }).b")
        assertValue(3L, "(tuple { a := num }).a")
    }

    @Test
    fun eval_create_object() = runTest {
        assertValue("n", "(A() { prop1 := 'n' }).prop1")
        assertValue(4L, "(B() { prop1 := 'n' prop2 := 4 }).prop2")
        assertValue(true, "(B() { prop1 := 'n' prop2 := 4 }) is A")
        assertNothingWithIssue(LanguageIssueKind.ERROR, "Unknown() { }")
    }

    @Test
    fun eval_block() = runTest {
        assertValue(6L, "{ x := 2 x * 3 }")
        assertValue(3L, "{ x := 2 y := x + 1 y }")
        assertValue(2L, "{ x := 2 { y := x y } }")
    }

    @Test
    fun eval_unknown_function_is_nothing_with_error() = runTest {
        assertNothingWithIssue(LanguageIssueKind.ERROR, "unknownFunc()")
    }

    @Test
    fun eval_reflection_type() = runTest {
        assertValue("Integer", "num.\$type.name")
        assertValue("B", "aList[1].\$type.name")
        assertValue("B", "(aList[1] as A).\$type.name")
    }

    // ---------------- EP3: errors give an issue and $nothing, not an exception ----------------

    @Test
    fun ep3_A1_real_division() = runTest {
        assertValue(2.5, "5.0 / 2.0")
        assertNothingWithIssue(LanguageIssueKind.ERROR, "7.0 / 2")
    }

    @Test
    fun ep3_A2_integer_division_by_zero() = runTest {
        assertNothingWithIssue(LanguageIssueKind.ERROR, "num / 0")
        assertNothingWithIssue(LanguageIssueKind.ERROR, "num % 0")
    }

    @Test
    fun ep3_A3_when_condition_not_boolean() = runTest {
        assertNothingWithIssue(LanguageIssueKind.ERROR, "when { 1 -> 1 else -> 2 }")
        assertNothingWithIssue(LanguageIssueKind.ERROR, "when { \$nothing -> 1 else -> 2 }")
    }

    @Test
    fun ep3_A4_unknown_type_in_is_and_as() = runTest {
        assertNothingWithIssue(LanguageIssueKind.ERROR, "aList[0] is Unknown")
        assertNothingWithIssue(LanguageIssueKind.ERROR, "aList[0] as Unknown")
    }

    @Test
    fun ep3_A5_unknown_method() = runTest {
        assertNothingWithIssue(LanguageIssueKind.WARNING, "aList.unknownMethod()")
    }

    @Test
    fun ep3_A6_method_argument_not_a_lambda() = runTest {
        assertNothingWithIssue(LanguageIssueKind.ERROR, "aList.map(1)")
        assertNothingWithIssue(LanguageIssueKind.ERROR, "aList.filter('x')")
    }

    @Test
    fun ep3_B_type_resolver() {
        assertEquals(StdLibDefault.Boolean, typeOf("aList[0] is B"))
        assertEquals(StdLibDefault.Integer, typeOf("{ x := 2 x * 3 }"))
        assertEquals(StdLibDefault.Integer, typeOf("{ x := 2 y := x + 1 y }"))
        assertEquals(StdLibDefault.Integer, typeOf("{ x := 2 { y := x y } }"))
        assertEquals(StdLibDefault.String, typeOf("{ x := 'a' x }"))
        assertEquals("ns.B", typeOf("aList[1] as B").qualifiedTypeName.value)
        assertEquals(StdLibDefault.Integer, typeOf("(aList[1] as B).prop2"))
    }

    // ---------------- EP4: and/or/xor are three-valued, $nothing is unknown ----------------

    @Test
    fun ep4_logic_with_nothing() = runTest {
        val n = "\$nothing"
        assertValue(false, "false and $n")
        assertValue(false, "$n and false")
        assertValue(Unit, "true and $n")
        assertValue(Unit, "$n and true")
        assertValue(Unit, "$n and $n")
        assertValue(true, "true or $n")
        assertValue(true, "$n or true")
        assertValue(Unit, "false or $n")
        assertValue(Unit, "$n or false")
        assertValue(Unit, "$n or $n")
        assertValue(Unit, "true xor $n")
        assertValue(Unit, "$n xor false")
        assertValue(Unit, "$n xor $n")
    }
}
