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

package net.akehurst.language.agl.language.grammar

import net.akehurst.language.agl.parser.LeftCornerParser
import net.akehurst.language.agl.scanner.ScannerOnDemand
import net.akehurst.language.agl.sppt.SPPTParserDefault
import net.akehurst.language.api.parser.Parser
import net.akehurst.language.api.processor.ParseResult
import net.akehurst.language.api.sppt.SharedPackedParseTree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_AglGrammar_item {

    private companion object {
        private val converterToRuntimeRules: ConverterToRuntimeRules = ConverterToRuntimeRules(AglGrammarGrammar)
        private val parser: Parser = LeftCornerParser(ScannerOnDemand(converterToRuntimeRules.runtimeRuleSet.nonSkipTerminals), converterToRuntimeRules.runtimeRuleSet)
    }

    private val spptParser = SPPTParserDefault(converterToRuntimeRules.runtimeRuleSet)

    private fun parse(goalRule: String, inputText: String): ParseResult {
        //parser.buildFor(goalRule,AutomatonKind.LOOKAHEAD_1)
        return parser.parseForGoal(goalRule, inputText)
    }

    private fun sppt(treeString: String): SharedPackedParseTree {
        return spptParser.addTree(treeString)
    }

    private fun test(sentence: String, goal: String, expected: String) {
        val result = parse(goal, sentence)
        val expSppt = sppt(expected)
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expSppt.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun nonTerminal__SINGLE_LINE_COMMENT() {
        val goal = "nonTerminal"
        val sentence = """
            // a single line comment
            a
        """.trimIndent()
        val expected = """
            nonTerminal {
                SINGLE_LINE_COMMENT : '// a single line comment'
                WHITESPACE : '⏎'
                qualifiedName { IDENTIFIER : 'a' }
            }
        """
        test(sentence, goal, expected)
    }

    @Test
    fun nonTerminal__SINGLE_LINE_COMMENT_EMPTY() {
        val text = """
            //
            a
        """.trimIndent()
        val result = parse("nonTerminal", text)
        val expected = this.sppt(
            """
            nonTerminal {
                SINGLE_LINE_COMMENT : '//'
                WHITESPACE : '⏎'
                qualifiedName { IDENTIFIER : 'a' }
            }
        """
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun nonTerminal__MULTI_LINE_COMMENT() {
        val text = """
            /* a single line comment
            sfgh
            */
            a
        """.trimIndent()
        val result = parse("nonTerminal", text)
        //TODO("what is 9166 ?")
        val expected = this.sppt(
            """
            nonTerminal {
                MULTI_LINE_COMMENT : '/* a single line comment⏎sfgh⏎*/'
                WHITESPACE : '⏎'
                qualifiedName { IDENTIFIER : 'a' }
            }
        """
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun IDENTIFIER__a() {
        val result = parse("IDENTIFIER", "a")

        val expected = this.sppt("IDENTIFIER  : 'a' ")
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun LITERAL__a() {
        val result = parse("LITERAL", "'a'")
        val expected = this.sppt("LITERAL  : '\\'a\\'' ")
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun LITERAL__backslash() {
        val text = """
            '\\'
            """.trimIndent()
        val result = parse("LITERAL", text)
        val expected = this.sppt(
            """
            LITERAL  : '\'\\\\\''
        """.trimIndent()
        )

        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun PATTERN__range_a2c() {
        val result = parse("PATTERN", "\"[a-c]\"")
        val expected = this.sppt("PATTERN  : '\"[a-c]\"' ")
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun PATTERN__double_quote_string() {
        val result = parse("PATTERN", "\"([^\\\"\\\\]|\\.)*\"")
        val expected = this.sppt("PATTERN  : '\"([^\\\"\\\\]|\\.)*\"' ")
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun PATTERN__backslash() {
        val text = """
            "[\\]"
        """.trimIndent()
        val result = parse("PATTERN", text)
        val expected = this.sppt("PATTERN  : '\"[\\\\]\"' ")
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun terminal__literal_a() {
        val result = parse("terminal", "'a'")
        val expected = this.sppt("terminal { LITERAL :'\\'a\\'' }")
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun terminal__pattern_range_a2c() {
        val result = parse("terminal", "\"[a-c]\"")
        val expected = this.sppt("terminal|1 { PATTERN  : '\"[a-c]\"'  }")
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun qualifiedName__a() {
        val result = parse("qualifiedName", "a")
        val expected = this.sppt("qualifiedName { IDENTIFIER  : 'a'  }")
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun qualifiedName__a_b() {
        val result = parse("qualifiedName", "a.b")
        val expected = this.sppt(
            """
            qualifiedName {
                IDENTIFIER  : 'a' 
                '.'
                IDENTIFIER  : 'b' 
            }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun qualifiedName__a_b_c() {
        val result = parse("qualifiedName", "a.b.c")
        val expected = this.sppt(
            """
            qualifiedName {
                IDENTIFIER  : 'a' 
                '.'
                IDENTIFIER  : 'b' 
                '.'
                IDENTIFIER : 'c' 
            }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun simpleItem__literal_a() {
        val result = parse("simpleItem", "'a'")
        val expected = this.sppt(
            """
            simpleItem { terminal { LITERAL  : '\'a\''  } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun simpleItem__nonTerminal_a() {
        val result = parse("simpleItem", "a")
        val expected = this.sppt(
            """
            simpleItem|1 { nonTerminal { qualifiedName { IDENTIFIER : 'a' } } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun simpleItem__nonTerminalQualified_a() {
        val result = parse("simpleItem", "z.y.x.B.a")
        val expected = this.sppt(
            """
            simpleItem { nonTerminal { qualifiedName {
              IDENTIFIER : 'z'
              '.'
              IDENTIFIER : 'y'
              '.'
              IDENTIFIER : 'x'
              '.'
              IDENTIFIER : 'B'
              '.'
              IDENTIFIER : 'a'
            } } } 
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun multiplicity__0_n() {
        val result = parse("multiplicity", "*")
        val expected = this.sppt(
            """
            multiplicity { '*' }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun multiplicity__0_1() {
        val result = parse("multiplicity", "?")
        val expected = this.sppt(
            """
            multiplicity|2 { '?' }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun multiplicity__1_n() {
        val result = parse("multiplicity", "+")
        val expected = this.sppt(
            """
            multiplicity|1 { '+' }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun multiplicity__unBraced_unBounded() {
        val result = parse("multiplicity", "2+")
        val expected = this.sppt(
            """
            multiplicity|3 { range {
              rangeUnBraced {
                POSITIVE_INTEGER : '2'
                rangeMaxOpt { rangeMax { rangeMaxUnbounded { '+' } } }
              }
            } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun multiplicity__braced_unBounded() {
        val result = parse("multiplicity", "{2+}")
        val expected = this.sppt(
            """
            multiplicity|3 { range {
              rangeBraced {
                '{'
                POSITIVE_INTEGER : '2'
                rangeMaxOpt { rangeMax { rangeMaxUnbounded { '+' } } }
                '}'
              }
            } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun multiplicity__unBraced_bounded() {
        val result = parse("multiplicity", "2..5")
        val expected = this.sppt(
            """
            multiplicity|3 { range {
              rangeUnBraced {
                POSITIVE_INTEGER : '2'
                rangeMaxOpt { rangeMax {
                  rangeMaxBounded {'..' POSITIVE_INTEGER_GT_ZERO  : '5' }
                } }
              }
            } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun multiplicity__braced_bounded() {
        val result = parse("multiplicity", "{2..5}")
        val expected = this.sppt(
            """
            multiplicity|3 { range {
              rangeBraced {
                '{'
                POSITIVE_INTEGER : '2'
                rangeMaxOpt { rangeMax {
                  rangeMaxBounded {'..' POSITIVE_INTEGER_GT_ZERO  : '5' }
                } }
                '}'
              }
            } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun simpleList__literal_a_0_n() {
        val result = parse("simpleList", "'a'*")
        val expected = this.sppt(
            """
            simpleList {
                simpleItemOrGroup { simpleItem { terminal { LITERAL  : '\'a\''  } } }
                multiplicity { '*' }
            }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun simpleList__nonTerminal_a_0_n() {
        val result = parse("simpleList", "a*")
        val expected = this.sppt(
            """
            simpleList {
                simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'a' } } } }
                multiplicity { '*' }
            }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun separatedList__literal_comma_0_n() {
        val result = parse("separatedList", "['a'/',']*")
        val expected = this.sppt(
            """
            separatedList {
              '['
              simpleItemOrGroup { simpleItem { terminal { LITERAL : '\'a\'' } } }
              '/'
              simpleItemOrGroup { simpleItem { terminal { LITERAL : '\',\'' } } }
              ']'
              multiplicity { '*' }
            }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun concatenationItem__literal_a() {
        val result = parse("concatenationItem", "'a'")
        val expected = this.sppt(
            """
            concatenationItem { simpleItemOrGroup { simpleItem { terminal { LITERAL : '\'a\'' } } } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun concatenationItem__nonTerminal_a() {
        val result = parse("concatenationItem", "a")
        val expected = this.sppt(
            """
            concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'a' } } } } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun concatenation__literal_a() {
        val result = parse("concatenation", "'a'")
        val expected = this.sppt(
            """
            concatenation {
               concatenationItem { simpleItemOrGroup { simpleItem { terminal { LITERAL  : '\'a\''  } } } }
            }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun concatenation__literals_a_b_c() {
        val result = parse("concatenation", "'a' 'b' 'c'")
        val expected = this.sppt(
            """
            concatenation {
                concatenationItem { simpleItemOrGroup { simpleItem { terminal { LITERAL  : '\'a\''  WHITESPACE  : ' ' } } } }
                concatenationItem { simpleItemOrGroup { simpleItem { terminal { LITERAL  : '\'b\''  WHITESPACE  : ' ' } } } }
                concatenationItem { simpleItemOrGroup { simpleItem { terminal { LITERAL  : '\'c\''  } } }
            } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun concatenation__nonTerminal_a() {
        val result = parse("concatenation", "a")
        val expected = this.sppt(
            """
             concatenation { concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'a' } } } } } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun concatenation__nonTerminals_a_b_c() {
        val result = parse("concatenation", "a b c")
        val expected = this.sppt(
            """
            concatenation {
                concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
                    IDENTIFIER : 'a' WHITESPACE  : ' ' 
                } } } } }
                concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
                    IDENTIFIER : 'b' WHITESPACE  : ' ' 
                } } } } }
                concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'c' } } } } }
            }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun priorityChoice__nonTerminals_a_b_c() {
        val result = parse("priorityChoice", "a < b < c")
        val expected = this.sppt(
            """
            priorityChoice {
                concatenation { concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
                    IDENTIFIER : 'a' WHITESPACE  : ' ' 
                } } } } } }
                '<' WHITESPACE  : ' ' 
                concatenation { concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
                    IDENTIFIER : 'b' WHITESPACE  : ' ' 
                } } } } } }
                '<' WHITESPACE  : ' ' 
                concatenation { concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'c' } } } } } }
            }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun simpleChoice__nonTerminals_a_b_c() {
        val result = parse("simpleChoice", "a | b | c")
        val expected = this.sppt(
            """
             simpleChoice {
                concatenation { concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
                IDENTIFIER : 'a' WHITESPACE : ' '
                } } } } } }
                '|' WHITESPACE : ' '
                concatenation { concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
                IDENTIFIER : 'b' WHITESPACE : ' '
                } } } } } }
                '|' WHITESPACE : ' '
                concatenation { concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'c' } } } } } }
            }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun choice__simple_nonTerminals_a_b() {
        val result = parse("choice", "a | b")
        val expected = this.sppt(
            """
            choice|2 { simpleChoice {
                concatenation { concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
                    IDENTIFIER : 'a'
                    WHITESPACE : ' '
                } } } } } }
                '|'
                WHITESPACE : ' '
                concatenation { concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'b' } } } } } }
            } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun choice__priority_nonTerminals_a_b_c() {
        val result = parse("choice", "a < b < c")
        val expected = this.sppt(
            """
            choice { priorityChoice {
                concatenation { concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
                    IDENTIFIER : 'a' WHITESPACE  : ' ' 
                } } } } } }
                '<' WHITESPACE  : ' ' 
                concatenation { concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
                    IDENTIFIER : 'b' WHITESPACE  : ' ' 
                } } } } } }
                '<' WHITESPACE  : ' ' 
                concatenation { concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'c' } } } } } }
            } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun rules__group_nonTerminal_b() {
        val gstr = """
            s=(b);
        """.trimIndent()
        val result = parse("rules", gstr)
        val expected = this.sppt(
            """
            rules { rule { grammarRule {
                ruleTypeLabels {
                    isSkip|1 { §empty }
                    isLeaf|1 { §empty }
                }
                IDENTIFIER : 's'
                '='
                rhs { concatenation { concatenationItem { simpleItemOrGroup { group {
                '('
                    groupedContent { concatenation { concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {IDENTIFIER : 'b' } } } } } } }
                ')'
                } } } } }
                ';'
            } } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun group__group_nonTerminals_a_b() {
        val gstr = "(b c)"
        val result = parse("group", gstr)
        val expected = this.sppt(
            """
            group {
              '('
              groupedContent { concatenation {
                concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'b' WHITESPACE : ' ' } } } } }
                concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'c' } } } } }
              } }
              ')'
            }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun concatenation__group_nonTerminals_a_b() {
        val gstr = "(b c)"
        val result = parse("concatenation", gstr)
        val expected = this.sppt(
            """
            concatenation { concatenationItem { simpleItemOrGroup { group { 
              '('
              groupedContent { concatenation {
                concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'b' WHITESPACE : ' ' } } } } }
                concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'c' } } } } }
              } }
              ')'
            } } } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun embeded() {
        val gstr = "X::y"
        val result = parse("embedded", gstr)
        val expected = this.sppt(
            """
             embedded {
              qualifiedName { IDENTIFIER : 'X' }
              '::'
              nonTerminal { qualifiedName { IDENTIFIER : 'y' } }
             }
            """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun rhs_embeded() {
        val gstr = "X::y"
        val result = parse("rhs", gstr)
        val expected = this.sppt(
            """
             rhs { concatenation { concatenationItem { simpleItemOrGroup { simpleItem { embedded {
              qualifiedName { IDENTIFIER : 'X' }
              '::'
              nonTerminal { qualifiedName { IDENTIFIER : 'y' } }
             } } } } } }
            """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun rhs__group_nonTerminals_a_b() {
        val gstr = "(b c)"
        val result = parse("rhs", gstr)
        val expected = this.sppt(
            """
            rhs { concatenation { concatenationItem { simpleItemOrGroup {  group {
              '('
              groupedContent { concatenation {
                concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'b' WHITESPACE : ' ' } } } } }
                concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'c' } } } } }
              } }
              ')'
            } } } } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun rule__priorityChoice_nonTerminal_a_b_c() {
        val result = parse("rule", "r = a < b < c ;")
        val expected = this.sppt(
            """
            rule { grammarRule {
                ruleTypeLabels {
                    isSkip { §empty }
                    isLeaf { §empty }
                }
                IDENTIFIER : 'r' WHITESPACE : ' '
                '=' WHITESPACE : ' '
                rhs|2 { choice { priorityChoice {
                    concatenation { concatenationItem { simpleItemOrGroup { simpleItem|1 { nonTerminal { qualifiedName {
                        IDENTIFIER : 'a' WHITESPACE : ' '
                    } } } } } }
                    '<'
                    WHITESPACE : ' '
                    concatenation { concatenationItem { simpleItemOrGroup { simpleItem|1 { nonTerminal { qualifiedName {
                        IDENTIFIER : 'b' WHITESPACE : ' '
                    } } } } } }
                    '<'
                    WHITESPACE : ' '
                    concatenation { concatenationItem { simpleItemOrGroup { simpleItem|1 { nonTerminal { qualifiedName {
                        IDENTIFIER : 'c' WHITESPACE : ' '
                    } } } } } }
                } } }
                ';'
            } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun rule__concatenation_a_optional_a() {
        val sentence = "r= 'a''a'? ;".trimIndent()
        val result = parse("rule", sentence)

        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        //assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun rule__HEX_FLOAT_LITERAL() {
        val sentence = """
            HEX_FLOAT_LITERAL = '0' "[xX]" (HexDigits '.'? | HexDigits? '.' HexDigits) "[pP]" "[+-]"? Digits "[fFdD]"? ;
        """.trimIndent()
        val result = parse("rule", sentence)

        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        //assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun rule__skip_rule() {
        val sentence = """
            skip r = a ;
        """.trimIndent()
        val result = parse("rule", sentence)
        val expected = this.sppt(
            """
rule { grammarRule {
  ruleTypeLabels {
    isSkip {
      'skip'
      WHITESPACE : ' '
    }
    isLeaf { §empty }
  }
  IDENTIFIER : 'r'
  WHITESPACE : ' '
  '='
  WHITESPACE : ' '
  rhs { concatenation { concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
    IDENTIFIER : 'a'
    WHITESPACE : ' '
  } } } } } } }
  ';'
} }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun rule__leaf_rule() {
        val sentence = """
            leaf r = a ;
        """.trimIndent()
        val result = parse("rule", sentence)
        val expected = this.sppt(
            """
            rule { grammarRule {
                ruleTypeLabels {
                    isSkip|1 {  §empty } 
                    isLeaf {  'leaf' WHITESPACE : ' ' } 
                }
                IDENTIFIER : 'r' WHITESPACE : ' '
                '=' WHITESPACE : ' '
                rhs|1 { concatenation {  concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
                    IDENTIFIER : 'a'
                    WHITESPACE : ' '
                } } } } } } }
                ';'
            } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun rule__skip_leaf_rule() {
        val sentence = """
            skip leaf r = a ;
        """.trimIndent()
        val result = parse("rule", sentence)
        val expected = this.sppt(
            """
rule { grammarRule {
  ruleTypeLabels {
    isSkip {
      'skip'
      WHITESPACE : ' '
    }
    isLeaf {
      'leaf'
      WHITESPACE : ' '
    }
  }
  IDENTIFIER : 'r'
  WHITESPACE : ' '
  '='
  WHITESPACE : ' '
  rhs { concatenation { concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
    IDENTIFIER : 'a'
    WHITESPACE : ' '
  } } } } } } }
  ';'
} }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun rule__simpleList_a_0_n() {
        val sentence = """
            list = a* ;
        """.trimIndent()
        val result = parse("rule", sentence)
        val expected = this.sppt(
            """
            rule { grammarRule {
              ruleTypeLabels {
                isSkip { §empty }
                isLeaf { §empty }
              }
              IDENTIFIER : 'list'
              WHITESPACE : ' '
              '='
              WHITESPACE : ' '
              rhs { concatenation { concatenationItem { listOfItems { simpleList {
                simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'a' } } } }
                multiplicity {
                  '*'
                  WHITESPACE : ' '
                }
              } } } } }
              ';'
            } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun rule__separatedList_a_c_0_n() {
        val sentence = """
            list=[a/c]*;
        """.trimIndent()
        val result = parse("rule", sentence)
        val expected = this.sppt(
            """
rule { grammarRule {
  ruleTypeLabels {
    isSkip { §empty }
    isLeaf { §empty }
  }
  IDENTIFIER : 'list'
  '='
  rhs { concatenation { concatenationItem { listOfItems { separatedList {
    '['
    simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'a' } } } }
    '/'
    simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'c' } } } }
    ']'
    multiplicity { '*' }
  } } } } }
  ';'
} }
            """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun rules__group_nonTerminals_a_b() {
        val gstr = """
            s=(b c);
        """.trimIndent()
        val result = parse("rules", gstr)
        val expected = this.sppt(
            """
            rules { rule { grammarRule {
              ruleTypeLabels {
                isSkip { §empty }
                isLeaf { §empty }
              }
              IDENTIFIER : 's'
              '='
              rhs { concatenation { concatenationItem { simpleItemOrGroup { group {
                '('
                groupedContent { concatenation {
                  concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
                    IDENTIFIER : 'b'
                    WHITESPACE : ' '
                  } } } } }
                  concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'c' } } } } }
                } }
                ')'
              } } } } }
              ';'
            } } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun rules__nonTermial_a_group_nonTerminal_a_optional_c() {
        val gstr = """
            s=a(b c?);
        """.trimIndent()
        val result = parse("rules", gstr)
        val expected = this.sppt(
            """
            rules { rule { grammarRule {
              ruleTypeLabels {
                isSkip { §empty }
                isLeaf { §empty }
              }
              IDENTIFIER : 's'
              '='
              rhs { concatenation {
                concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'a' } } } } }
                concatenationItem { simpleItemOrGroup { group {
                  '('
                  groupedContent { concatenation {
                    concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
                      IDENTIFIER : 'b'
                      WHITESPACE : ' '
                    } } } } }
                    concatenationItem { listOfItems { simpleList {
                      simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'c' } } } }
                      multiplicity { '?' }
                    } } }
                  } }
                  ')'
                } } }
              } }
              ';'
            } } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun rules__group_choice_a_b_OR_optional_c() {
        val gstr = """
            s = (a b | c?) ;
        """.trimIndent()
        val result = parse("rules", gstr)
        val expected = this.sppt(
            """
rules { rule { grammarRule {
  ruleTypeLabels {
    isSkip { §empty }
    isLeaf { §empty }
  }
  IDENTIFIER : 's'
  WHITESPACE : ' '
  '='
  WHITESPACE : ' '
  rhs { concatenation { concatenationItem { simpleItemOrGroup { group {
    '('
    groupedContent { choice { simpleChoice {
      concatenation {
        concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
          IDENTIFIER : 'a'
          WHITESPACE : ' '
        } } } } }
        concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
          IDENTIFIER : 'b'
          WHITESPACE : ' '
        } } } } }
      }
      '|'
      WHITESPACE : ' '
      concatenation { concatenationItem { listOfItems { simpleList {
        simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName { IDENTIFIER : 'c' } } } }
        multiplicity { '?' }
      } } } }
    } } }
    ')'
    WHITESPACE : ' '
  } } } } }
  ';'
} } }
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun grammar__grammar_nonTerminal_a() {
        val gstr = """
            grammar Test { s = a ; }
        """.trimIndent()
        val result = parse("grammar", gstr)
        val expected = this.sppt(
            """
grammar {
  'grammar'
  WHITESPACE : ' '
  IDENTIFIER : 'Test'
  WHITESPACE : ' '
  extendsOpt { §empty }
  '{'
  WHITESPACE : ' '
  options { <EMPTY> }
  rules { rule { grammarRule {
    ruleTypeLabels {
      isSkip { §empty }
      isLeaf { §empty }
    }
    IDENTIFIER : 's'
    WHITESPACE : ' '
    '='
    WHITESPACE : ' '
    rhs { concatenation { concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
      IDENTIFIER : 'a'
      WHITESPACE : ' '
    } } } } } } }
    ';'
    WHITESPACE : ' '
  } } }
  '}'
}
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun grammar__grammar_skip_rule_nonTerminal_a() {
        val gstr = """
            grammar Test { skip s = a ; }
        """.trimIndent()
        val result = parse("grammar", gstr)
        val expected = this.sppt(
            """
grammar {
  'grammar'
  WHITESPACE : ' '
  IDENTIFIER : 'Test'
  WHITESPACE : ' '
  extendsOpt { §empty }
  '{'
  WHITESPACE : ' '
  options { <EMPTY> }
  rules { rule { grammarRule {
    ruleTypeLabels {
      isSkip {
        'skip'
        WHITESPACE : ' '
      }
      isLeaf { §empty }
    }
    IDENTIFIER : 's'
    WHITESPACE : ' '
    '='
    WHITESPACE : ' '
    rhs { concatenation { concatenationItem { simpleItemOrGroup { simpleItem { nonTerminal { qualifiedName {
      IDENTIFIER : 'a'
      WHITESPACE : ' '
    } } } } } } }
    ';'
    WHITESPACE : ' '
  } } }
  '}'
}
        """.trimIndent()
        )
        assertNotNull(result.sppt, result.issues.joinToString(separator = "\n") { it.toString() })
        assertEquals(expected.toStringAll, result.sppt!!.toStringAll)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test
    fun preferenceRule() {
        val goal = "preferenceRule"
        val sentence = """
            preference s {
              x on 'a','b','c' left
            }
        """.trimIndent()
        val expected = """
            preferenceRule {
              'preference' WHITESPACE : ' '
              simpleItem { nonTerminal { qualifiedName {
                IDENTIFIER : 's' WHITESPACE : ' '
              } } }
              '{' WHITESPACE : '⏎  '
              preferenceOptionList { preferenceOption {
                nonTerminal { qualifiedName {
                  IDENTIFIER : 'x' WHITESPACE : ' '
                } }
                choiceNumber { §empty }
                'on' WHITESPACE : ' '
                terminalList {
                  simpleItem { terminal { LITERAL : '\'a\'' } }
                  ','
                  simpleItem { terminal { LITERAL : '\'b\'' } }
                  ','
                  simpleItem { terminal {
                    LITERAL : '\'c\'' WHITESPACE : ' '
                  } }
                }
                associativity {
                  'left' WHITESPACE : '⏎'
                }
              } }
              '}'
            }
        """
        test(sentence, goal, expected)
    }
}
