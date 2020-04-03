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

import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.agl.grammar.runtime.ConverterToRuntimeRules
import net.akehurst.language.agl.parser.Parser
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.sppt.SPPTParser

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
//TODO("what is 9166 ?")
        val expected = this.sppt("""
            nonTerminal {
                SINGLE_LINE_COMMENT : '// a single line comment' 
                WHITESPACE : '9166' 
                qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'a' } }
            }
        """)
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
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
                MULTI_LINE_COMMENT  : '/* a single line comment9166sfgh9166*/'  WHITESPACE  : '9166' 
                qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'a' } }
            }
        """)
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }
    @Test
    fun IDENTIFIER() {
        val actual = parse("IDENTIFIER", "a")

        val expected = this.sppt("IDENTIFIER  : 'a' ")
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun LITERAL() {
        val actual = parse("LITERAL", "'a'")
        val expected = this.sppt("LITERAL  : '\\'a\\'' ")
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun PATTERN() {
        val actual = parse("PATTERN", "\"[a-c]\"")
        val expected = this.sppt("PATTERN  : '\"[a-c]\"' ")
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun terminal_literal() {
        val actual = parse("terminal", "'a'")
        val expected = this.sppt("terminal { LITERAL :'\\'a\\'' }")
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun terminal_pattern() {
        val actual = parse("terminal", "\"[a-c]\"")
        val expected = this.sppt("terminal { PATTERN  : '\"[a-c]\"'  }")
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun qualifiedName_1() {
        val actual = parse("qualifiedName", "a")
        val expected = this.sppt("qualifiedName { §qualifiedName§sList0 { IDENTIFIER  : 'a'  } }")
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
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
    }

    @Test
    fun simpleItem_literal() {
        val actual = parse("simpleItem", "'a'")
        val expected = this.sppt("""
            simpleItem { terminal { LITERAL  : '\'a\''  } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun simpleItem_nonTerminal() {
        val actual = parse("simpleItem", "a")
        val expected = this.sppt("""
            simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'a' } } } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun multiplicity_0_n() {
        val actual = parse("multiplicity", "*")
        val expected = this.sppt("""
            multiplicity { '*' : '*' }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun multiplicity_0_1() {
        val actual = parse("multiplicity", "?")
        val expected = this.sppt("""
            multiplicity { '?' : '?' }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun multiplicity_1_n() {
        val actual = parse("multiplicity", "+")
        val expected = this.sppt("""
            multiplicity { '+' : '+' }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun multiplicity_2_n() {
        val actual = parse("multiplicity", "2+")
        val expected = this.sppt("""
            multiplicity { §multiplicity§choice0 {
              POSITIVE_INTEGER  : '2' 
              '+'
             } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun multiplicity_2_5() {
        val actual = parse("multiplicity", "2..5")
        val expected = this.sppt("""
            multiplicity { §multiplicity§choice1 {
              POSITIVE_INTEGER  : '2' 
              '..'
              POSITIVE_INTEGER  : '5' 
            } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
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
    }

    @Test
    fun multi_nonTerminal() {
        val actual = parse("multi", "a*")
        val expected = this.sppt("""
            multi {
                simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'a' } } } }
                multiplicity { '*' }
            }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun concatenationItem_literal() {
        val actual = parse("concatenationItem", "'a'")
        assertNotNull(actual)
    }

    @Test
    fun concatenationItem_nonTermianal() {
        val actual = parse("concatenationItem", "a")
        assertNotNull(actual)
    }

    @Test
    fun concatenation_literal_1() {
        val actual = parse("concatenation", "'a'")
        val expected = this.sppt("""
            concatenation {
                §concatenation§multi5 { concatenationItem { simpleItem { terminal { LITERAL  : '\'a\''  } } } }
            }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun concatenation_literal_3() {
        val actual = parse("concatenation", "'a' 'b' 'c'")
        val expected = this.sppt("""
            concatenation { §concatenation§multi5 {
                concatenationItem { simpleItem { terminal { LITERAL  : '\'a\''  WHITESPACE  : ' ' } } } 
                concatenationItem { simpleItem { terminal { LITERAL  : '\'b\''  WHITESPACE  : ' ' } } } 
                concatenationItem { simpleItem { terminal { LITERAL  : '\'c\''  } } 
            } } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun concatenation_nonTerminal_1() {
        val actual = parse("concatenation", "a")
        val expected = this.sppt("""
             concatenation { §concatenation§multi5 { concatenationItem { simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'a' } } } } } } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun concatenation_nonTerminal_3() {
        val actual = parse("concatenation", "a b c")
        val expected = this.sppt("""
            concatenation { §concatenation§multi5 {
                concatenationItem { simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                    IDENTIFIER : 'a' WHITESPACE  : ' ' 
                } } } } }
                concatenationItem { simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                    IDENTIFIER : 'b' WHITESPACE  : ' ' 
                } } } } }
                concatenationItem { simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'c' } } } } }
            } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun priorityChoice_nonTerminal_3() {
        val actual = parse("priorityChoice", "a < b < c")
        val expected = this.sppt("""
            priorityChoice { §priorityChoice§sList2 {
                concatenation { §concatenation§multi5 { concatenationItem { simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                    IDENTIFIER : 'a' WHITESPACE  : ' ' 
                } } } } } } }
                '<' WHITESPACE  : ' ' 
                concatenation { §concatenation§multi5 { concatenationItem { simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                    IDENTIFIER : 'b' WHITESPACE  : ' ' 
                } } } } } } }
                '<' WHITESPACE  : ' ' 
                concatenation { §concatenation§multi5 { concatenationItem { simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'c' } } } } } } }
            } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun simpleChoice_nonTerminal_3() {
        val actual = parse("simpleChoice", "a | b | c")
        val expected = this.sppt("""
            simpleChoice { §simpleChoice§sList3 {
                concatenation { §concatenation§multi5 { concatenationItem { simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                    IDENTIFIER : 'a' WHITESPACE  : ' ' 
                } } } } } } }
                '|' WHITESPACE  : ' ' 
                concatenation { §concatenation§multi5 { concatenationItem { simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                    IDENTIFIER : 'b' WHITESPACE  : ' ' 
                } } } } } } }
                '|' WHITESPACE  : ' ' 
                concatenation { §concatenation§multi5 { concatenationItem { simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'c' } } } } } } }
            } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun choice_priority_nonTerminal_3() {
        val actual = parse("choice", "a < b < c")
        val expected = this.sppt("""
            choice { priorityChoice { §priorityChoice§sList2 {
                concatenation { §concatenation§multi5 { concatenationItem { simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                    IDENTIFIER : 'a' WHITESPACE  : ' ' 
                } } } } } } }
                '<' WHITESPACE  : ' ' 
                concatenation { §concatenation§multi5 { concatenationItem { simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                    IDENTIFIER : 'b' WHITESPACE  : ' ' 
                } } } } } } }
                '<' WHITESPACE  : ' ' 
                concatenation { §concatenation§multi5 { concatenationItem { simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 { IDENTIFIER : 'c' } } } } } } }
            } } }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
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
    }

    @Test
    fun normalRule_priorityChoice_nonTerminal_3() {
        val actual = parse("rule", "r = a < b < c ;")
        val expected = this.sppt("""
            rule {
                ruleTypeLabels {
                    isSkip { §isSkip§multi3 { §empty } }
                    isLeaf { §isLeaf§multi4 { §empty } }
                }
                IDENTIFIER : 'r' WHITESPACE  : ' ' 
                '=' WHITESPACE  : ' ' 
                choice { priorityChoice { §priorityChoice§sList2 {
                    concatenation { §concatenation§multi5 { concatenationItem { simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                        IDENTIFIER : 'a' WHITESPACE  : ' ' 
                    } } } } } } }
                    '<' WHITESPACE  : ' ' 
                    concatenation { §concatenation§multi5 { concatenationItem { simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                        IDENTIFIER : 'b' WHITESPACE  : ' ' 
                    } } } } } } }
                    '<' WHITESPACE  : ' ' 
                    concatenation { §concatenation§multi5 { concatenationItem { simpleItem { nonTerminal { qualifiedName { §qualifiedName§sList0 {
                        IDENTIFIER : 'c' WHITESPACE  : ' ' 
                    } } } } } } }
                } } }
                ';'
            }
        """.trimIndent())
        assertNotNull(actual)
        assertEquals(expected.toStringAll, actual.toStringAll)
    }

    @Test
    fun HEX_FLOAT_LITERAL() {
        val sentence = """
            HEX_FLOAT_LITERAL =  '0' "[xX]" (HexDigits '.'? | HexDigits? '.' HexDigits) "[pP]" "[+-]"? Digits "[fFdD]"? ;
        """.trimIndent()
        val actual = parse("rule", "r = a < b < c ;")

        assertNotNull(actual)

    }
}
