/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.parser.leftcorner.listSeparated

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test

/**
 * A list that is an item of itself (as in MScript: infixExpression = [ expression / infixOperator ]2+ ).
 * Without a preference rule every nesting is a valid parse; the parser keeps the flat list
 * (it does not start a nested list where the item can continue the outer one), so long lists stay fast.
 */
class test_selfNesting : test_LeftCornerParserAbstract() {

    private companion object {
        // S = E ; E = L | 'a' ; L = [E / '+']2+
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("E") }
            choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("L")
                literal("a")
            }
            sList("L", 2, -1, "E", "'+'")
            literal("'+'", "+")
        }
        const val goal = "S"
    }

    @Test
    fun a() {
        super.test_pass(rrs, goal, "a", 1, "S { E { 'a' } }")
    }

    @Test
    fun aPa() {
        super.test_pass(rrs, goal, "a+a", 1, "S { E { L { E { 'a' } '+' E { 'a' } } } }")
    }

    @Test
    fun aPaPaPa_flat() {
        super.test_pass(rrs, goal, "a+a+a+a", 1, "S { E { L { E { 'a' } '+' E { 'a' } '+' E { 'a' } '+' E { 'a' } } } }")
    }

    @Test
    fun a_100() {
        val sentence = "a" + "+a".repeat(100)
        val expected = "S { E { L { E { 'a' } " + "'+' E { 'a' } ".repeat(100) + "} } }"
        super.test_pass(rrs, goal, sentence, 1, expected)
    }
}
