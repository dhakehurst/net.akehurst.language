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

package net.akehurst.language.parser.leftcorner.nesting

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test

class test_java_NavigableExpression : test_LeftCornerParserAbstract() {

    // NavigableExpression
    //   = MethodReference
    //   | GenericMethodInvocation
    //   ;
    // MethodReference = MethodInvocation '::' IDENTIFIER ;
    // GenericMethodInvocation = TypeArguments? MethodInvocation ;
    // MethodInvocation = IDENTIFIER '(' Expression ')' ;
    // TypeArguments = '<>' ;
    //
    // Expression = Postfix | IDENTIFIER ;
    // Postfix = Expression '++' ;
    //
    // leaf IDENTIFIER = "[A-Za-z]+" ;

    private companion object {
        val rrs = runtimeRuleSet {
            choice("NavigableExpression", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("MethodReference")
                ref("GenericMethodInvocation")
            }
            concatenation("MethodReference") { ref("MethodInvocation"); literal("::"); ref("IDENTIFIER") }
            concatenation("GenericMethodInvocation") { ref("optTypeArguments"); ref("MethodInvocation"); }
            concatenation("MethodInvocation") { ref("IDENTIFIER"); literal("("); ref("Expression"); literal(")") }
            optional("optTypeArguments", "TypeArguments")
            concatenation("TypeArguments") { literal("<>") }
            choice("Expression", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("Postfix")
                ref("IDENTIFIER")
            }
            concatenation("Postfix") { ref("Expression"); literal("++"); }
            pattern("IDENTIFIER", "[a-zA-Z]+")
        }
    }

    @Test
    fun MethodInvocation_IDENTIFIER() {
        val goal = "NavigableExpression"
        val sentence = "getUnchecked(i)"

        val expected = """
            NavigableExpression|1 { GenericMethodInvocation {
                optTypeArguments|1 { §empty }
                MethodInvocation {
                  IDENTIFIER : 'getUnchecked'
                  '('
                  Expression|1 { IDENTIFIER : 'i' }
                  ')'
                }
            } }
        """.trimIndent()

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun MethodInvocation_Postfix() {
//        rrs.buildFor("NavigableExpression",AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("NavigableExpression"))
        val goal = "NavigableExpression"
        val sentence = "f(i++)"

        val expected = """
            NavigableExpression|1 { GenericMethodInvocation {
                optTypeArguments|1 { §empty }
                MethodInvocation {
                  IDENTIFIER : 'f'
                  '('
                  Expression { Postfix {
                      Expression|1 { IDENTIFIER : 'i' }
                      '++'
                    } }
                  ')'
                }
            } }
        """.trimIndent()

        super.test_pass(
            rrs = rrs,
            goal = goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expectedTrees = arrayOf(expected)
        )
    }


}