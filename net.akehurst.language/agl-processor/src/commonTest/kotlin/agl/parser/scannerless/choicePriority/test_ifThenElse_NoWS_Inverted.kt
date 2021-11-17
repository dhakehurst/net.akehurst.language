package net.akehurst.language.parser.scanondemand.choicePriority

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_ifThenElse_NoWS_Inverted : test_ScanOnDemandParserAbstract() {

    // invert the dangling else

    // S =  expr ;
    // ifthenelse = 'if' expr 'then' expr 'else' expr ;
    // ifthen = 'if' expr 'then' expr ;
    // expr = var < conditional ;
    // conditional = ifthenelse < ifthen;
    // var = 'W' | 'X' | 'Y' | 'Z' ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("expr") }
            choice("expr", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("var")
                ref("conditional")
            }
            choice("conditional", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("ifthenelse")
                ref("ifthen")
            }
            concatenation("ifthen") { literal("if"); ref("expr"); literal("then"); ref("expr") }
            concatenation("ifthenelse") { literal("if"); ref("expr"); literal("then"); ref("expr"); literal("else"); ref("expr") }
            choice("var", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("W")
                literal("X")
                literal("Y")
                literal("Z")
            }
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {

        val sentence = ""

        val (sppt,issues) = super.testFail(rrs,goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0,1,1,1),"^", setOf("'W'","'X'","'Y'","'Z'","'if'"))
        ),issues)
    }

    @Test
    fun ifthenelse() {
        val sentence = "ifWthenXelseY"

        val expected = """
            S {
              expr|1 {
                conditional {
                    ifthenelse {
                      'if'
                      expr { var { 'W' } }
                      'then'
                      expr { var|1 { 'X' } }
                      'else'
                      expr { var|2 { 'Y' } }
                    }
                }
              }
            }
        """.trimIndent()

        //NOTE: season 35, long expression is dropped in favour of the shorter one!

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun ifthen() {
        val sentence = "ifWthenX"

        val expected = """
            S {
              expr|1 {
                conditional|1 {
                    ifthen {
                      'if'
                      expr { var { 'W' } }
                      'then'
                      expr { var|1 { 'X' } }
                    }
                }
              }
            }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun ifthenelseifthen() {
        val sentence = "ifWthenXelseifYthenZ"

        val expected = """
            S {
              expr|1 {
                conditional {
                    ifthenelse {
                      'if'
                      expr { var { 'W' } }
                      'then'
                      expr { var|1 { 'X' } }
                      'else'
                      expr|1 {
                        conditional|1 {
                            ifthen {
                              'if'
                              expr { var|2 { 'Y'} }
                              'then'
                              expr { var|3 { 'Z' } }
                            }
                        }
                      }
                    }
                }
              }
            }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = *arrayOf(expected)
        )
    }

    @Test
    fun ifthenifthenelse() {
        val sentence = "ifWthenifXthenYelseZ"

        val expected = """
         S { expr|1 { conditional { ifthenelse {
                'if'
                expr { var { 'W' } }
                'then'
                expr|1 { conditional|1 { ifthen {
                      'if'
                      expr { var|1 { 'X' } }
                      'then'
                      expr { var|2 { 'Y' } }
                    } } }
                'else'
                expr { var|3 { 'Z' } }
              } } } }
        """.trimIndent()

        val actual = super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 2, //TODO: can it be 1
                expectedTrees = *arrayOf(expected)
        )
    }


}
