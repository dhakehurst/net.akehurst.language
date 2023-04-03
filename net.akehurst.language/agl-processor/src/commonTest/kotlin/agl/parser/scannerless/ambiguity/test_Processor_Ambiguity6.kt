/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.scanondemand.ambiguity

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.scanondemand.embedded.test_embeddedSupersetSkip
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_Processor_Ambiguity6 : test_ScanOnDemandParserAbstract() {
    /**
     * Expression = PrimaryExpression | ... | AssignmentExpression ;
     * AssignmentExpression = Expression '=' Expression ;
     * PrimaryExpression = ... | FeatureCall | ... | EventValueReferenceExpression ;
     * EventValueReferenceExpression = 'valueof' '(' FeatureCall ')' ;
     * FeatureCall = RootElement | FunctionCall | ... ;
     * RootElement = ID ;
     * FunctionCall = ID ArgumentList ;
     * ArgumentList = '(' Arguments? ')' ;
     * Arguments = [Argument / ',']+ ;
     * Argument = (ID '=')?  Expression ;
     */

    /*
     * Ex = Pr | As               // Expression = PrimaryExpression | ... | AssignmentExpression ;
     * As = Ex '=' E x            // AssignmentExpression = Expression '=' Expression ;
     * Pr = Fc | Ev               // PrimaryExpression = ... | FeatureCall | ... | EventValueReferenceExpression ;
     * Ev = 'vo' '(' Fc ')'       // EventValueReferenceExpression = 'valueof' '(' FeatureCall ')' ;
     * Fc = Rt | Mc               // FeatureCall = RootElement | FunctionCall | ... ;
     * Rt = ID                    // RootElement = ID ;
     * Mc = ID Al                 // FunctionCall = ID ArgumentList ;
     * Al = '(' Go ')             // ArgumentList = '(' Arguments? ')' ;
     * Go = Gl?                   // Arguments?
     * Gl = [Ag / ',']+            // Arguments = [Argument / ',']+ ;
     * Ag = No Ex                  // Argument = (ID '=')?  Expression ;
     * No = N?                    // (ID '=')?
     * N = ID '='                 // ID '='
     * ID = "[a-z]+"
     */
    private companion object {
        val rrs = runtimeRuleSet {
            choice("Ex", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { ref("Pr"); ref("As") }
            concatenation("As") { ref("Ex"); literal("="); ref("Ex") }
            choice("Pr", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { ref("Fc"); ref("Ev") }
            concatenation("Ev") { literal("vo"); literal("("); ref("Fc"); literal(")") }
            choice("Fc", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { ref("Rt"); ref("Mc") }
            concatenation("Rt") { ref("ID") }
            concatenation("Mc") { ref("ID"); ref("Al") }
            concatenation("Al") { literal("("); ref("Go"); literal(")") }
            multi("Go",0,1,"Gl")
            multi("Gl",1,-1,"Ag")
            concatenation("Ag") { ref("No"); ref("Ex") }
            multi("No",0,1,"N")
            concatenation("N") { ref("ID"); literal("=") }
            pattern("ID","[a-z]+")
        }
        val goal = "Ex"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1), "^", setOf("ID","'vo'"))
            ), issues.error
        )
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            Ex { Pr { Fc { Rt { ID:'a' } } } }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun a_eq_b() {
        val sentence = "a=b"

        val expected = """
            Ex { As {
              Ex { Pr { Fc { Rt { ID:'a' } } } }
              '='
              Ex { Pr { Fc { Rt { ID:'b' } } } }
            } }
        """.trimIndent()

        super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun vo_a() {
        val sentence = "vo(a)"

        val expected = """
            Ex { Pr { Ev {
              'vo'
              '('
              Fc { Rt { ID : 'a' } }
              ')'
            } } }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 2,
            expectedTrees = arrayOf(expected)
        ) ?: error("error")

        val Ex = actual.root.asBranch
        val Pr = Ex.nonSkipChildren[0].asBranch
        val Ev = Pr.nonSkipChildren[0].asBranch

        assertEquals("Ex", Ex.name)
        assertEquals("Pr", Pr.name)
        assertEquals("Ev", Ev.name)

        assertEquals(0, Ex.option)
        assertEquals(1, Pr.option)
        assertEquals(0, Ev.option)
    }

    @Test
    fun a_eq_vo_b() {
        val sentence = "a=vo(b)"

        val expected = """
            Ex { As {
              Ex { Pr { Fc { Rt { ID : 'a' } } } }
              '='
              Ex { Pr { Ev {
                'vo'
                '('
                Fc { Rt { ID : 'b' } }
                ')'
              } } }
            } }
        """.trimIndent()

        val actual = super.test(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 2,
            expectedTrees = arrayOf(expected)
        ) ?: error("error")

        actual.root
    }
}