package net.akehurst.language.parser.scanondemand.choicePriority

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_ifThenElse_NoWS_Inverted : test_ScanOnDemandParserAbstract() {

    // invert the dangling else

    // S =  expr ;
    // ifthenelse = 'if' expr 'then' expr 'else' expr ;
    // ifthen = 'if' expr 'then' expr ;
    // expr = var < conditional ;
    // conditional = ifthenelse < ifthen;
    // var = 'W' | 'X' | 'Y' | 'Z' ;
    val rrs = runtimeRuleSet {
        concatenation("S") { ref("expr") }
        choice("expr",RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
            ref("var")
            ref("conditional")
        }
        choice("conditional",RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
            ref("ifthenelse")
            ref("ifthen")
        }
        concatenation("ifthen") { literal("if"); ref("expr"); literal("then"); ref("expr") }
        concatenation("ifthenelse") { literal("if"); ref("expr"); literal("then"); ref("expr"); literal("else"); ref("expr") }
        choice("var",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            literal("W")
            literal("X")
            literal("Y")
            literal("Z")
        }
    }


    @Test
    fun empty_fails() {
        val goal = "S"
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence,1)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
    }

    @Test
    fun ifthenelse() {
        val goal = "S"
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
        val goal = "S"
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
        val goal = "S"
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
        val goal = "S"
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
