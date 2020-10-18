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

package net.akehurst.language.agl.grammar.grammar

import net.akehurst.language.agl.parser.Parser
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.sppt.SPPTParser
import net.akehurst.language.api.sppt.SharedPackedParseTree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_AglGrammar_item {

    companion object {
        private val converterToRuntimeRules: ConverterToRuntimeRules = ConverterToRuntimeRules(AglGrammarGrammar())
        private val parser: Parser = ScanOnDemandParser(converterToRuntimeRules.transform())
    }

    private val spptParser = SPPTParser(converterToRuntimeRules.builder.ruleSet())

    private fun parse(goalRule: String, inputText: CharSequence): SharedPackedParseTree {
        return parser.parse(goalRule, inputText)
    }

    private fun sppt(treeString: String): SharedPackedParseTree {
        return spptParser.addTree(treeString)
    }

    @Test
    fun SINGLE_LINE_COMMENT() {
        val text = """
            // a single line comment
            a
        """.trimIndent()
        val actual = parse("nonTerminal", text)
        val expected = this.sppt("""
            nonTerminal {
                SINGLE_LINE_COMMENT : '// a single line comment'
                WHITESPACE : '⏎'
                qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'a' } }
            }
        """)
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun SINGLE_LINE_COMMENT_EMPTY() {
        val text = """
            //
            a
        """.trimIndent()
        val actual = parse("nonTerminal", text)
        val expected = this.sppt("""
            nonTerminal {
                SINGLE_LINE_COMMENT : '//'
                WHITESPACE : '⏎'
                qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'a' } }
            }
        """)
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun MULTI_LINE_COMMENT() {
        val text = """
            /* a single line comment
            sfgh
            */
            a
        """.trimIndent()
        val actual = parse("nonTerminal", text)
        //TODO("what is 9166 ?")
        val expected = this.sppt("""
            nonTerminal {
                MULTI_LINE_COMMENT : '/* a single line comment⏎sfgh⏎*/'
                WHITESPACE : '⏎'
                qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'a' } }
            }
        """)
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun IDENTIFIER() {
        val actual = parse("IDENTIFIER", "a")

        val expected = this.sppt("IDENTIFIER  : 'a' ")
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun LITERAL() {
        val actual = parse("LITERAL", "'a'")
        val expected = this.sppt("LITERAL  : '\\'a\\'' ")
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun LITERAL_backslash() {
        val text = """
            '\\'
            """.trimIndent()
        val actual = parse("LITERAL", text)
        val expected = this.sppt("""
            LITERAL  : '\'\\\''
        """.trimIndent())
        //converted to single backslash by SyntaxAnalyser
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun PATTERN() {
        val actual = parse("PATTERN", "\"[a-c]\"")
        val expected = this.sppt("PATTERN  : '\"[a-c]\"' ")
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun PATTERN2() {
        val actual = parse("PATTERN", "\"([^\\\"\\\\]|\\.)*\"")
        val expected = this.sppt("PATTERN  : '\"([^\\\"\\\\]|\\.)*\"' ")
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun PATTERN3() {
        val text = """
            "[\\]"
        """.trimIndent()
        val actual = parse("PATTERN", text)
        val expected = this.sppt("PATTERN  : '\"[\\\\]\"' ")
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun terminal_literal() {
        val actual = parse("terminal", "'a'")
        val expected = this.sppt("terminal { LITERAL :'\\'a\\'' }")
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun terminal_pattern() {
        val actual = parse("terminal", "\"[a-c]\"")
        val expected = this.sppt("terminal|1 { PATTERN  : '\"[a-c]\"'  }")
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun qualifiedName_1() {
        val actual = parse("qualifiedName", "a")
        val expected = this.sppt("qualifiedName { §qualifiedName§sList0 { IDENTIFIER  : 'a'  } }")
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun qualifiedName_2() {
        val actual = parse("qualifiedName", "a.b")
        val expected = this.sppt("""
            qualifiedName {
                §qualifiedName§sList0 {
                    IDENTIFIER  : 'a' 
                    '.'
                    IDENTIFIER  : 'b' 
                }
            }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun qualifiedName_3() {
        val actual = parse("qualifiedName", "a.b.c")
        val expected = this.sppt("""
            qualifiedName {
                §qualifiedName§sList0 {
                    IDENTIFIER  : 'a' 
                    '.'
                    IDENTIFIER  : 'b' 
                    '.'
                    IDENTIFIER : 'c' 
                }
            }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun simpleItem_literal() {
        val actual = parse("simpleItem", "'a'")
        val expected = this.sppt("""
            simpleItem { terminal { LITERAL  : '\'a\''  } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun simpleItem_nonTerminal() {
        val actual = parse("simpleItem", "a")
        val expected = this.sppt("""
            simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'a' } } } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun multiplicity_0_n() {
        val actual = parse("multiplicity", "*")
        val expected = this.sppt("""
            multiplicity { '*' : '*' }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun multiplicity_0_1() {
        val actual = parse("multiplicity", "?")
        val expected = this.sppt("""
            multiplicity|2 { '?' : '?' }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun multiplicity_1_n() {
        val actual = parse("multiplicity", "+")
        val expected = this.sppt("""
            multiplicity|1 { '+' : '+' }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun multiplicity_2_n() {
        val actual = parse("multiplicity", "2+")
        val expected = this.sppt("""
            multiplicity|3 { §multiplicity§choice0 {
              POSITIVE_INTEGER  : '2' 
              '+'
             } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun multiplicity_2_5() {
        val actual = parse("multiplicity", "2..5")
        val expected = this.sppt("""
            multiplicity|4 { §multiplicity§choice1 {
              POSITIVE_INTEGER  : '2' 
              '..'
              POSITIVE_INTEGER  : '5' 
            } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun multi_literal() {
        val actual = parse("multi", "'a'*")
        val expected = this.sppt("""
            multi {
                simpleItem { terminal { LITERAL  : '\'a\''  } }
                multiplicity { '*' : '*' }
            }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun multi_nonTerminal() {
        val actual = parse("multi", "a*")
        val expected = this.sppt("""
            multi {
                simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'a' } } } }
                multiplicity { '*' }
            }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun concatenationItem_literal() {
        val actual = parse("concatenationItem", "'a'")
        assertNotNull(actual)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun concatenationItem_nonTermianal() {
        val actual = parse("concatenationItem", "a")
        assertNotNull(actual)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun concatenation_literal_1() {
        val actual = parse("concatenation", "'a'")
        val expected = this.sppt("""
            concatenation {
                §concatenation§multi6 { concatenationItem { simpleItem { terminal { LITERAL  : '\'a\''  } } } }
            }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun concatenation_literal_3() {
        val actual = parse("concatenation", "'a' 'b' 'c'")
        val expected = this.sppt("""
            concatenation { §concatenation§multi6 {
                concatenationItem { simpleItem { terminal { LITERAL  : '\'a\''  WHITESPACE  : ' ' } } } 
                concatenationItem { simpleItem { terminal { LITERAL  : '\'b\''  WHITESPACE  : ' ' } } } 
                concatenationItem { simpleItem { terminal { LITERAL  : '\'c\''  } } 
            } } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun concatenation_nonTerminal_1() {
        val actual = parse("concatenation", "a")
        val expected = this.sppt("""
             concatenation { §concatenation§multi6 { concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'a' } } } } } } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun concatenation_nonTerminal_3() {
        val actual = parse("concatenation", "a b c")
        val expected = this.sppt("""
            concatenation { §concatenation§multi6 {
                concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                    IDENTIFIER : 'a' WHITESPACE  : ' ' 
                } } } } }
                concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                    IDENTIFIER : 'b' WHITESPACE  : ' ' 
                } } } } }
                concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'c' } } } } }
            } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun priorityChoice_nonTerminal_3() {
        val actual = parse("priorityChoice", "a < b < c")
        val expected = this.sppt("""
            priorityChoice { §priorityChoice§sList2 {
                concatenation { §concatenation§multi6 { concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                    IDENTIFIER : 'a' WHITESPACE  : ' ' 
                } } } } } } }
                '<' WHITESPACE  : ' ' 
                concatenation { §concatenation§multi6 { concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                    IDENTIFIER : 'b' WHITESPACE  : ' ' 
                } } } } } } }
                '<' WHITESPACE  : ' ' 
                concatenation { §concatenation§multi6 { concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'c' } } } } } } }
            } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun simpleChoice_nonTerminal_3() {
        val actual = parse("simpleChoice", "a | b | c")
        val expected = this.sppt("""
             simpleChoice { §simpleChoice§sList4 {
                concatenation { §concatenation§multi6 { concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                IDENTIFIER : 'a'
                WHITESPACE : ' '
                } } } } } } }
                '|'
                WHITESPACE : ' '
                concatenation { §concatenation§multi6 { concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                IDENTIFIER : 'b'
                WHITESPACE : ' '
                } } } } } } }
                '|'
                WHITESPACE : ' '
                concatenation { §concatenation§multi6 { concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'c' } } } } } } }
            } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun choice_priority_nonTerminal_3() {
        val actual = parse("choice", "a < b < c")
        val expected = this.sppt("""
            choice { priorityChoice { §priorityChoice§sList2 {
                concatenation { §concatenation§multi6 { concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                    IDENTIFIER : 'a' WHITESPACE  : ' ' 
                } } } } } } }
                '<' WHITESPACE  : ' ' 
                concatenation { §concatenation§multi6 { concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                    IDENTIFIER : 'b' WHITESPACE  : ' ' 
                } } } } } } }
                '<' WHITESPACE  : ' ' 
                concatenation { §concatenation§multi6 { concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'c' } } } } } } }
            } } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun group1() {
        val gstr = """
            r=e?;
            e=w? a*;
            s=(s i?);
        """.trimIndent()
        val actual = parse("rules", gstr)
        val expected = this.sppt("""
            group {
            concatenation {
                concatenationItem { simpleItem { terminal { LITERAL  : '\'a\''  } } }
            }
            }
        """.trimIndent())
        assertNotNull(actual)
       // assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun normalRule_priorityChoice_nonTerminal_3() {
        val actual = parse("rule", "r = a < b < c ;")
        val expected = this.sppt("""
            rule {
                ruleTypeLabels {
                    isSkip { §isSkip§multi3|1 { §empty } }
                    isLeaf { §isLeaf§multi4|1 { §empty } }
                }
                IDENTIFIER : 'r' WHITESPACE  : ' ' 
                '=' WHITESPACE  : ' ' 
                choice { priorityChoice { §priorityChoice§sList2 {
                    concatenation { §concatenation§multi6 { concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                        IDENTIFIER : 'a' WHITESPACE  : ' ' 
                    } } } } } } }
                    '<' WHITESPACE  : ' ' 
                    concatenation { §concatenation§multi6 { concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                        IDENTIFIER : 'b' WHITESPACE  : ' ' 
                    } } } } } } }
                    '<' WHITESPACE  : ' ' 
                    concatenation { §concatenation§multi6 { concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                        IDENTIFIER : 'c' WHITESPACE  : ' ' 
                    } } } } } } }
                } } }
                ';'
            }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun HEX_FLOAT_LITERAL() {
        val sentence = """
            HEX_FLOAT_LITERAL =  '0' "[xX]" (HexDigits '.'? | HexDigits? '.' HexDigits) "[pP]" "[+-]"? Digits "[fFdD]"? ;
        """.trimIndent()
        val actual = parse("rule", sentence)

        assertNotNull(actual)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun skip_rule() {
        val sentence = """
            skip r = a ;
        """.trimIndent()
        val actual = parse("rule", sentence)
        val expected = this.sppt("""
            rule {
                ruleTypeLabels {
                    isOverride { §isOverride§multi3|1 { §empty } }
                    isSkip { §isSkip§multi4 {
                        'skip'
                        WHITESPACE : ' '
                    } }
                    isLeaf { §isLeaf§multi5|1 { §empty } }
                }
                IDENTIFIER : 'r'
                WHITESPACE : ' '
                '='
                WHITESPACE : ' '
                choice|2 { simpleChoice { §simpleChoice§sList4 { concatenation { §concatenation§multi6 { concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                    IDENTIFIER : 'a'
                    WHITESPACE : ' '
                } } } } } } } } } }
                ';'
            }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun leaf_rule() {
        val sentence = """
            leaf r = a ;
        """.trimIndent()
        val actual = parse("rule", sentence)
        val expected = this.sppt("""
            rule {
                ruleTypeLabels {
                    isOverride { §isOverride§multi3|1 { §empty } }
                    isSkip { §isSkip§multi4|1 { §empty } }
                    isLeaf { §isLeaf§multi5 { 'leaf' WHITESPACE : ' ' } }
                }
                IDENTIFIER : 'r'
                WHITESPACE : ' '
                '='
                WHITESPACE : ' '
                choice|2 { simpleChoice { §simpleChoice§sList4 { concatenation { §concatenation§multi6 { concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                    IDENTIFIER : 'a'
                    WHITESPACE : ' '
                } } } } } } } } } }
                ';'
            }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }

    @Test
    fun skip_leaf_rule() {
        val sentence = """
            skip leaf r = a ;
        """.trimIndent()
        val actual = parse("rule", sentence)
        val expected = this.sppt("""
            rule {
                ruleTypeLabels {
                    isSkip { §isSkip§multi3 { 'skip' WHITESPACE : ' ' } }
                    isLeaf { §isLeaf§multi4 { 'leaf' WHITESPACE : ' ' } }
                }
                IDENTIFIER : 'r'
                WHITESPACE : ' '
                '='
                WHITESPACE : ' '
                choice|2 { simpleChoice { §simpleChoice§sList4 { concatenation { §concatenation§multi6 { concatenationItem { simpleItem|1 { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                    IDENTIFIER : 'a'
                    WHITESPACE : ' '
                } } } } } } } } } }
                ';'
            }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
        assertEquals(1,actual.maxNumHeads)
    }
}
