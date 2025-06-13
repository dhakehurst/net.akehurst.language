/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.agl.processor.thirdparty.freon

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.sppt.api.SharedPackedParseTree
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class test_ProjectIT_singles {

    companion object {

        private val grammarStr = this::class.java.getResource("/projectIT/PiEditGrammar.agl")?.readText() ?: error("File not found")

        var processor = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!

    }

    private fun checkSPPT(expected: String, actual: SharedPackedParseTree) {
        val sppt = processor.spptParser
        val exp = sppt.parse(expected)
        assertEquals(exp.toStringAllWithIndent("  "), actual.toStringAllWithIndent("  "))
        assertEquals(exp, actual)
    }

    @Test
    fun expression_constant_string() {
        val goal = "expression"
        val sentence = """
            "hello"
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertNotNull(result.sppt, result.issues.toString())
        assertTrue(result.issues.isEmpty())
        checkSPPT(
            "expression { constant { string : '\"hello\"' } }",
            result.sppt!!
        )
    }

    @Test
    fun expression_constant_number() {
        val goal = "expression"
        val sentence = """
            12345
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertNotNull(result.sppt, result.issues.toString())
        assertTrue(result.issues.isEmpty())
        checkSPPT(
            "expression { constant { number : '12345' } }",
            result.sppt!!
        )
    }

    @Test
    fun expression_var() {
        val goal = "expression"
        val sentence = """
            variable
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertNotNull(result.sppt, result.issues.toString())
        assertTrue(result.issues.isEmpty())
        checkSPPT(
            "expression { navigationExpression { var : 'variable' } }",
            result.sppt!!
        )
    }

    @Test
    fun expression_instanceExpression() {
        val goal = "expression"
        val sentence = """
            var1 : var2
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertNotNull(result.sppt, result.issues.toString())
        assertTrue(result.issues.isEmpty())
        checkSPPT(
            """
                expression { instanceExpression {
                  var : 'var1' WHITESPACE : ' '
                  ':' WHITESPACE : ' '
                  var : 'var2'
                } }
            """.trimIndent(),
            result.sppt!!
        )
    }

    @Test
    fun expression_functionExpression() {
        val goal = "expression"
        val sentence = """
            func1(a,d,v)
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertNotNull(result.sppt, result.issues.toString())
        assertTrue(result.issues.isEmpty())
        checkSPPT(
            """
                expression { functionExpression {
                  var : 'func1'
                  '('
                  argList {
                    expression { navigationExpression { var : 'a' } }
                    ','
                    expression { navigationExpression { var : 'd' } }
                    ','
                    expression { navigationExpression { var : 'v' } }
                  }
                  ')'
                } }
            """.trimIndent(),
            result.sppt!!
        )
    }

    @Test
    fun expression_navigationExpression() {
        val goal = "expression"
        val sentence = """
            a.b.c.d
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertNotNull(result.sppt, result.issues.toString())
        assertTrue(result.issues.isEmpty())
        checkSPPT(
            """
                expression { navigationExpression {
                  var : 'a'
                  '.'
                  var : 'b'
                  '.'
                  var : 'c'
                  '.'
                  var : 'd'
                } }
            """.trimIndent(),
            result.sppt!!
        )
    }

    @Test
    fun expression_listExpression() {
        val goal = "expression"
        val sentence = """
            list a.b.c horizontal separator [,]
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertNotNull(result.sppt, result.issues.toString())
        assertTrue(result.issues.isEmpty())
        checkSPPT(
            """
                expression { listExpression {
                  'list' WHITESPACE : ' '
                  navigationExpression {
                    var : 'a'
                    '.'
                    var : 'b'
                    '.'
                    var : 'c' WHITESPACE : ' '
                  }
                  listInfo {
                    §listInfo§opt1 {
                      listDirection : 'horizontal' WHITESPACE : ' '
                    }
                    §listInfo§opt2 { §listInfo§group1 {
                      listInfoType : 'separator' WHITESPACE : ' '
                      '['
                      literal : ','
                      ']'
                    } }
                  }
                } }
            """.trimIndent(),
            result.sppt!!
        )
    }

    @Test
    fun expression_tableExpression() {
        val goal = "expression"
        val sentence = """
            table a.b.c rows
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertNotNull(result.sppt, result.issues.toString())
        assertTrue(result.issues.isEmpty())
        checkSPPT(
            """
                expression { tableExpression {
                  'table' WHITESPACE : ' '
                  navigationExpression {
                    var : 'a'
                    '.'
                    var : 'b'
                    '.'
                    var : 'c' WHITESPACE : ' '
                  }
                  tableInfo { §tableInfo§choice1 { 'rows' } }
                } }
            """.trimIndent(),
            result.sppt!!
        )
    }

    @Test
    fun templateText_literal() {
        val goal = "templateText"
        val sentence = """
          Insurance Product
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertNotNull(result.sppt, result.issues.toString())
        assertTrue(result.issues.isEmpty())
        checkSPPT(
            "templateText { Template::text { textItem { literal : 'Insurance Product' } } }",
            result.sppt!!
        )
    }

    @Test
    fun literal_literal_fails_close_bracket() {
        val goal = "literal"
        val sentence = """
          Insurance Product
        ]
        """.trimIndent()

        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))

        assertEquals(
            setOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(20, 1, 2, 1, null), "^]", setOf("<EOT>"))
            ),
            result.issues.all
        )
    }

    @Test
    fun templateText_literal_fails_close_bracket() {
        val goal = "templateText"
        val sentence = """
          Insurance Product
        ]
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertEquals(
            setOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(20, 1, 2, 1, null), "^]", setOf("<EOT>", "literal", "escapedChar", "'\${'"))
            ),
            result.issues.all
        )
    }

    @Test
    fun projection_literal() {
        val goal = "projection"
        val sentence = """
        [
          Insurance Product
        ]
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertNotNull(result.sppt, result.issues.toString())
        assertTrue(result.issues.isEmpty())
        checkSPPT(
            """
                projection {
                  '['
                  WHITESPACE : '⏎  '
                  projectionContent { templateText {
                    Template::text { textItem { literal : 'Insurance Product⏎' } }
                  } }
                  ']'
                }
            """.trimIndent(),
            result.sppt!!
        )
    }

    @Test
    fun projection_lit_var_lit() {
        val goal = "projection"
        val sentence = """
        [
          Insurance Product ${"$"}{name} USES
        ]
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt, "No SPPT !")
        checkSPPT(
            """
            projection {
              '[' WHITESPACE : '⏎  '
              projectionContent { templateText { Template::text {
                textItem { literal : 'Insurance Product ' }
                textItem { embeddedExpression {
                  '${'$'}{'
                      §Expressions§expression§embedded1 { Expressions::expression { navigationExpression {
                        var : 'name'
                      } } }
                  '}'
                } }
                textItem { literal : ' USES⏎' }
              } } }
              ']'
            }
            """.trimIndent(),
            result.sppt!!
        )
    }

    @Test
    fun projection_lit_list_lit() {
        val goal = "projection"
        val sentence = """
        [
          Insurance Product ${"$"}{list name} USES
        ]
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt, "No SPPT !")
        checkSPPT(
            """
            projection {
              '[' WHITESPACE : '⏎  '
              projectionContent { templateText { Template::text {
                textItem { literal : 'Insurance Product ' }
                textItem { embeddedExpression {
                  '${'$'}{'
                      §Expressions§expression§embedded1 { Expressions::expression { listExpression {
                        'list' WHITESPACE : ' '
                        navigationExpression { var : 'name' }
                        listInfo {
                          §listInfo§opt1 { §empty }
                          §listInfo§opt2 { §empty }
                        }
                      } } }
                  '}'
                } }
                textItem { literal : ' USES⏎' }
              } } }
              ']'
            }
            """.trimIndent(),
            result.sppt!!
        )
    }

    @Test
    fun projection_1() {
        val goal = "projection"
        val sentence = """
        [
          ${"$"}{list a.b.c }
        ]
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt, "No SPPT !")
        checkSPPT(
            """
            projection {
              '[' WHITESPACE : '⏎  '
              projectionContent { templateText { Template::text {
                textItem { embeddedExpression {
                  '${'$'}{'
                      §Expressions§expression§embedded1 { Expressions::expression { listExpression {
                        'list' WHITESPACE : ' '
                        navigationExpression {
                          var : 'a'
                          '.'
                          var : 'b'
                          '.'
                          var : 'c' WHITESPACE : ' '
                        }
                        listInfo {
                          §listInfo§opt1 { §empty }
                          §listInfo§opt2 { §empty }
                        }
                      } } }
                  '}'
                } }
                textItem { literal : '⏎' }
              } } }
              ']'
            }
            """.trimIndent(),
            result.sppt!!
        )
    }

    @Test
    fun projection_2() {
        val goal = "projection"
        val sentence = """
        [
          Insurance Product ${"$"}{name} ( public name: ${"$"}{productName} ) USES ${"$"}{list basedOn horizontal separator[, ]}
        ]
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goalRuleName = goal))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt, "No SPPT !")
        checkSPPT(
            """
            projection {
              '['
              WHITESPACE : '⏎  '
              projectionContent { templateText { Template::text {
                textItem { literal : 'Insurance Product ' }
                textItem { embeddedExpression {
                  '${'$'}{'
                    §Expressions§expression§embedded1 { Expressions::expression { navigationExpression { var : 'name' } } }
                    '}'
                  } }
                textItem { literal : ' ( public name: ' }
                textItem { embeddedExpression {
                  '${'$'}{'
                    §Expressions§expression§embedded1 { Expressions::expression { navigationExpression { var : 'productName' } } }
                    '}'
                  } }
                textItem { literal : ' ) USES ' }
                textItem { embeddedExpression {
                  '${'$'}{'
                    §Expressions§expression§embedded1 { Expressions::expression { listExpression {
                        'list'
                        WHITESPACE : ' '
                        navigationExpression {
                            var : 'basedOn'
                            WHITESPACE : ' '
                        }
                        listInfo {
                            §listInfo§opt1 {
                            listDirection : 'horizontal'
                            WHITESPACE : ' '
                        }
                        §listInfo§opt2 { §listInfo§group1 {
                            listInfoType : 'separator'
                            '['
                            literal : ', '
                            ']'
                        } }
                        }
                    } } }
                    '}'
                  } }
                textItem { literal : '⏎' }
              } } }
              ']'
            }
            """.trimIndent(),
            result.sppt!!
        )
    }

}
