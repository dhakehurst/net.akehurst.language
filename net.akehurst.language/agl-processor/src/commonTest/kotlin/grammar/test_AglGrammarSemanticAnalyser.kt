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

package net.akehurst.language.grammar.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.grammar.processor.*
import net.akehurst.language.grammar.asm.GrammarModelDefault
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.agl.processor.SyntaxAnalysisResultDefault
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.grammar.api.*
import net.akehurst.language.api.processor.ProcessOptions
import net.akehurst.language.api.processor.ProcessResult
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.processor.SyntaxAnalysisResult
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import net.akehurst.language.sppt.api.SharedPackedParseTree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class test_AglGrammarSemanticAnalyser {

    private companion object {

        init {
            // grammars are registered in a registry when semantically analysed,
            // thus need to analyse Base grammar first
            val context = ContextFromGrammarRegistry(Agl.registry)
            val gm = GrammarModelDefault(SimpleName("Test"), namespace =  listOf(AglBase.grammar.namespace as GrammarNamespace))
            semanticAnalysis(
                SyntaxAnalysisResultDefault(
                    gm,
                    IssueHolder(LanguageProcessorPhase.SYNTAX_ANALYSIS),
                    emptyMap()
                ),
                Agl.options { }
            )
        }

        fun parse(sentence: String): SharedPackedParseTree {
            val conv = ConverterToRuntimeRules(AglGrammar.grammar)
            val rrs = conv.runtimeRuleSet
            val scanner = ScannerOnDemand(RegexEnginePlatform, rrs.terminals)
            val parser = LeftCornerParser(scanner, rrs)
            val res = parser.parse(sentence, Agl.parseOptions { goalRuleName(AglGrammar.goalRuleName) })
            assertTrue(res.issues.isEmpty(), res.issues.toString())
            return res.sppt!!
        }

        fun syntaxAnalysis(sppt: SharedPackedParseTree): SyntaxAnalysisResult<GrammarModel> {
            val sut = AglGrammarSyntaxAnalyser()
            val res = sut.transform(sppt, { _, _ -> TODO() })
            return res
        }

        fun semanticAnalysis(
            asmRes: SyntaxAnalysisResult<GrammarModel>,
            options: ProcessOptions<GrammarModel, ContextFromGrammarRegistry>
        ): SemanticAnalysisResult {
            val semanticAnalyser = AglGrammarSemanticAnalyser()
            val context = ContextFromGrammarRegistry(Agl.registry)
            return semanticAnalyser.analyse(asmRes.asm!!, asmRes.locationMap, context, options.semanticAnalysis)
        }

        fun test(
            grammarStr: String,
            expected: Set<LanguageIssue>,
            options: ProcessOptions<GrammarModel, ContextFromGrammarRegistry> = Agl.options { }
        ): ProcessResult<GrammarModel> {
            val sppt = parse(grammarStr)
            val asmRes = syntaxAnalysis(sppt)
            val res = semanticAnalysis(asmRes, options)
            assertEquals(expected, res.issues.all)
            return ProcessResultDefault(asmRes.asm, res.issues)
        }

    }

    @Test
    fun nonTerminalNotFound() {
        val grammarStr = """
            namespace test
            grammar Test {
                a = b ;
            }
        """.trimIndent()

        val expected = setOf(
            LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(38, 9, 3, 1), "GrammarRule 'b' not found in grammar 'Test'")
        )

        test(grammarStr, expected, Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })
    }

    @Test
    fun rule_nonTerminalQualified() {
        val grammarStr = """
            namespace ns.test
            grammar Base {
                A = 'a' ;
            }
            grammar Test {
              S = Base.A ;
            }
        """.trimIndent()
        val res = test(grammarStr, emptySet(), Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })
        val gBase = res.asm!!.allDefinitions[0]
        val gTest = res.asm!!.allDefinitions[1]
        assertTrue(gTest.findAllResolvedGrammarRule(GrammarRuleName("S")) != null)
        val rS = gTest.findAllResolvedGrammarRule(GrammarRuleName("S")) as NormalRule
        assertTrue(rS.rhs is NonTerminal)
        assertEquals(SimpleName("Base"), (rS.rhs as NonTerminal).referencedRuleOrNull(gTest)?.grammar?.name)
    }

    @Test
    fun duplicateRule() {
        val grammarStr = """
            namespace test
            grammar Test {
                a = b ;
                b = 'a' ;
                b = 'b' ;
            }
        """.trimIndent()

        val expected = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(position = 60, column = 5, line = 5, length = 9),
                "More than one rule named 'b' found in grammar 'Test'"
            ),
//            LanguageIssue(
//                LanguageIssueKind.ERROR,
//                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
//                InputLocation(position = 38, column = 9, line = 3, length = 1),
//                "More than one rule named 'b' found in grammar 'Test'"
//            ),
        )

        test(grammarStr, expected, Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })
    }

    @Test
    fun ruleUnused() {
        val grammarStr = """
            namespace test
            grammar Test {
                a = 'b' ;
                c = 'd' ;
            }
        """.trimIndent()
        val expected = setOf(
            LanguageIssue(LanguageIssueKind.WARNING, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(48, 5, 4, 9), "Rule 'c' is not used in grammar Test.")
        )
        test(grammarStr, expected, Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })
    }

    @Test
    fun ambiguity() {
        val grammarStr = """
            namespace test
            grammar Test {
                a = b1 | b2 ;
                b1 = 'b' ;
                b2 = 'b' ;
            }
        """.trimIndent()
        val expected = setOf(
            LanguageIssue(
                LanguageIssueKind.WARNING,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(57, 10, 4, 3),
                "Ambiguity: [HEIGHT/HEIGHT] conflict from 'b1' into 'b1/b2' on [<EOT>]"
            ),
            LanguageIssue(
                LanguageIssueKind.WARNING,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(72, 10, 5, 3),
                "Ambiguity: [HEIGHT/HEIGHT] conflict from 'b1' into 'b2/b1' on [<EOT>]"
            ),
        )
        test(grammarStr, expected, Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })
    }

    @Test
    fun extends_one_reuse_leaf_from_base() {
        val grammarStr = """
            namespace ns.test
            grammar Base {
                leaf A = 'a' ;
            }
            
            grammar Test : Base {
              S = A ;
            }
        """.trimIndent()

        val res = test(grammarStr, emptySet(), Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })

        val asm = res.asm!!
        assertEquals(2, asm.allDefinitions.size)
        val baseG = asm.allDefinitions[0]
        val testG = asm.allDefinitions[1]

        assertTrue(baseG.findAllResolvedGrammarRule(GrammarRuleName("A")) != null)
        assertTrue(testG.findAllResolvedGrammarRule(GrammarRuleName("A")) != null)
        assertEquals(1, testG.grammarRule.size)
        assertEquals(2, testG.allResolvedGrammarRule.size)

        val proc = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
        assertTrue(proc.parse("a").issues.errors.isEmpty())
    }

    @Test
    fun extends_two_reuse_leaves_from_bases() {
        val grammarStr = """
            namespace ns.test
            grammar Base1 {
                leaf A = 'a' ;
            }
            grammar Base2 {
                leaf B = 'b' ;
            }
            grammar Test : Base1, Base2 {
              S = A B ;
            }
        """.trimIndent()

        val res = test(grammarStr, emptySet(), Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })

        val asm = res.asm!!
        assertEquals(3, asm.allDefinitions.size)
        val base1G = asm.allDefinitions[0]
        val base2G = asm.allDefinitions[1]
        val testG = asm.allDefinitions[2]

        assertTrue(base1G.findAllResolvedGrammarRule(GrammarRuleName("A")) != null)
        assertTrue(base2G.findAllResolvedGrammarRule(GrammarRuleName("B")) != null)
        assertTrue(testG.findAllResolvedGrammarRule(GrammarRuleName("A")) != null)
        assertTrue(testG.findAllResolvedGrammarRule(GrammarRuleName("B")) != null)
        assertEquals(1, testG.grammarRule.size)
        assertEquals(3, testG.allResolvedGrammarRule.size)

        val proc = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
        assertTrue(proc.parse("ab").issues.errors.isEmpty())
    }

    @Test
    fun extends_one_override_leaf_not_override() {
        val grammarStr = """
            namespace ns.test
            grammar Base {
                leaf A = 'a' ;
            }
            
            grammar Test : Base {
              S = A ;
              A = 'aa' ;
            }
        """.trimIndent()

        val expected = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(37, 5, 3, 14),
                "More than one rule named 'A' found in grammar 'Test'"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(89, 3, 8, 10),
                "More than one rule named 'A' found in grammar 'Test'"
            )
        )

        test(grammarStr, expected, Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })
    }

    @Test
    fun extends_one_override_leaf_from_base() {
        val grammarStr = """
            namespace ns.test
            grammar Base {
                leaf A = 'a' ;
            }
            
            grammar Test : Base {
              S = A ;
              override A = 'aa' ;
            }
        """.trimIndent()

        val res = test(grammarStr, emptySet(), Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })

        val asm = res.asm!!
        assertEquals(2, asm.allDefinitions.size)
        val baseG = asm.allDefinitions[0]
        val testG = asm.allDefinitions[1]

        assertTrue(baseG.findAllResolvedGrammarRule(GrammarRuleName("A")) != null)
        assertTrue(testG.findAllResolvedGrammarRule(GrammarRuleName("A")) != null)
        assertEquals(2, testG.grammarRule.size)
        assertEquals(2, testG.allResolvedGrammarRule.size)

        val proc = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
        assertTrue(proc.parse("aa").issues.errors.isEmpty())
    }

    @Test
    fun extends_two_override_leaves_from_bases() {
        val grammarStr = """
            namespace ns.test
            grammar Base1 {
                leaf A = 'a' ;
            }
            grammar Base2 {
                leaf B = 'b' ;
            }
            grammar Test : Base1, Base2 {
              S = A B ;
              override leaf A = 'aa' ;
              override leaf B = 'bb' ;
            }
        """.trimIndent()

        val res = test(grammarStr, emptySet(), Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })

        val asm = res.asm!!
        assertEquals(3, asm.allDefinitions.size)
        val base1G = asm.allDefinitions[0]
        val base2G = asm.allDefinitions[1]
        val testG = asm.allDefinitions[2]

        assertTrue(base1G.findAllResolvedGrammarRule(GrammarRuleName("A")) != null)
        assertTrue(base2G.findAllResolvedGrammarRule(GrammarRuleName("B")) != null)
        assertTrue(testG.findAllResolvedGrammarRule(GrammarRuleName("A")) != null)
        assertTrue(testG.findAllResolvedGrammarRule(GrammarRuleName("B")) != null)
        assertEquals(3, testG.grammarRule.size)
        assertEquals(3, testG.allResolvedGrammarRule.size)

        val proc = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
        assertTrue(proc.parse("aabb").issues.errors.isEmpty())
    }

    @Test
    fun extends_two_same_rule_name_and_rhs_in_bases_fails() {
        val grammarStr = """
            namespace ns.test
            grammar Base1 {
                leaf A = 'a' ;
            }
            grammar Base2 {
                leaf A = 'a' ;
            }
            grammar Test : Base1, Base2 {
              S = A ;
            }
        """.trimIndent()

        val expected = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(75, 5, 6, 14),
                "More than one rule named 'A' found in grammar 'Test'"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(38, 5, 3, 14),
                "More than one rule named 'A' found in grammar 'Test'"
            ),
        )

        test(grammarStr, expected, Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })
    }

    @Test
    fun extends_diamond() {
        val grammarStr = """
            namespace ns.test
            grammar Base {
                leaf A = 'a' ;
            }
            grammar Mid1 : Base {
                leaf B = 'b' ;
            }
            grammar Mid2 : Base {
                leaf C = 'c' ;
            }
            grammar Test : Mid1, Mid2 {
              S = A B C;
            }
        """.trimIndent()

        val res = test(grammarStr, emptySet(), Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })

        val asm = res.asm!!
        assertEquals(4, asm.allDefinitions.size)
        val baseG = asm.allDefinitions[0]
        val mid1G = asm.allDefinitions[1]
        val mid2G = asm.allDefinitions[2]
        val testG = asm.allDefinitions[3]

        assertTrue(baseG.findAllResolvedGrammarRule(GrammarRuleName("A")) != null)
        assertTrue(mid1G.findAllResolvedGrammarRule(GrammarRuleName("B")) != null)
        assertTrue(mid1G.findAllResolvedGrammarRule(GrammarRuleName("A")) != null)
        assertTrue(mid2G.findAllResolvedGrammarRule(GrammarRuleName("C")) != null)
        assertTrue(mid1G.findAllResolvedGrammarRule(GrammarRuleName("A")) != null)
        assertTrue(testG.findAllResolvedGrammarRule(GrammarRuleName("A")) != null)
        assertTrue(testG.findAllResolvedGrammarRule(GrammarRuleName("B")) != null)
        assertTrue(testG.findAllResolvedGrammarRule(GrammarRuleName("C")) != null)
        assertEquals(1, baseG.allResolvedGrammarRule.size)
        assertEquals(2, mid1G.allResolvedGrammarRule.size)
        assertEquals(2, mid2G.allResolvedGrammarRule.size)
        assertEquals(4, testG.allResolvedGrammarRule.size)

        val proc = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
        assertTrue(proc.parse("abc").issues.errors.isEmpty())
    }

    @Test
    fun extends_one_override_appendChoice_to_base() {
        val grammarStr = """
            namespace ns.test
            grammar Base {
                C = 'a' | 'b' ;
            }
            
            grammar Test : Base {
              S = C ;
              override C +=| 'c' ;
            }
        """.trimIndent()

        val res = test(grammarStr, emptySet(), Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })

        val asm = res.asm!!
        assertEquals(2, asm.allDefinitions.size)
        val baseG = asm.allDefinitions[0]
        val testG = asm.allDefinitions[1]

        assertNotNull(baseG.findAllResolvedGrammarRule(GrammarRuleName("C")))
        assertNotNull(testG.findAllResolvedGrammarRule(GrammarRuleName("C")))
        assertEquals(2, testG.grammarRule.size)
        assertEquals(2, testG.allResolvedGrammarRule.size)
        assertTrue(baseG.findAllGrammarRuleList(GrammarRuleName("C"))[0].rhs is Choice)
        assertEquals(2, (baseG.findAllGrammarRuleList(GrammarRuleName("C"))[0].rhs as Choice).alternative.size)
        assertTrue(testG.findAllGrammarRuleList(GrammarRuleName("C"))[0].rhs is ChoiceLongest)
        assertEquals(3, (testG.findAllResolvedGrammarRule(GrammarRuleName("C"))!!.rhs as ChoiceLongest).alternative.size)

        val proc = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
        assertTrue(proc.parse("a").issues.errors.isEmpty())
        assertTrue(proc.parse("b").issues.errors.isEmpty())
        assertTrue(proc.parse("c").issues.errors.isEmpty())
    }

    @Test
    fun extends_diamond_repeat_no_override_fails() {
        val grammarStr = """
            namespace ns.test
            grammar Base {
                leaf A = 'a' ;
            }
            grammar Mid1 : Base {
                leaf B = 'b' ;
            }
            grammar Mid2 : Base {
                leaf A = 'c' ;
            }
            grammar Test : Mid1, Mid2 {
              S = A B;
            }
        """.trimIndent()

        val expected = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(123, 5, 9, 14),
                "More than one rule named 'A' found in grammar 'Mid2'"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(37, 5, 3, 14),
                "More than one rule named 'A' found in grammar 'Mid2'"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(123, 5, 9, 14),
                "More than one rule named 'A' found in grammar 'Test'"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(37, 5, 3, 14),
                "More than one rule named 'A' found in grammar 'Test'"
            ),
        )

        test(grammarStr, expected, Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })
    }

    @Test
    fun extends_diamond_repeat_with_override() {
        val grammarStr = """
            namespace ns.test
            grammar Base {
                leaf A = 'a' ;
            }
            grammar Mid1 : Base {
                leaf B = 'b' ;
            }
            grammar Mid2 : Base {
                leaf B = 'c' ;
            }
            grammar Test : Mid1, Mid2 {
              S = A B;
              override B = 'd' ;
            }
        """.trimIndent()


        test(grammarStr, emptySet(), Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })
    }

    @Test
    fun extends_diamond_repeat_with_override2_fails1() {
        val grammarStr = """
            namespace ns.test
            grammar Base {
                leaf A = 'a' ;
            }
            grammar Mid1 : Base {
                leaf B = 'b' ;
            }
            grammar Mid2 : Base {
                leaf A = 'c' ;
            }
            grammar Test : Mid1, Mid2 {
              S = B;
            }
        """.trimIndent()

        val expected = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(37, 5, 3, 14),
                "More than one rule named 'A' found in grammar 'Mid2'"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(123, 5, 9, 14),
                "More than one rule named 'A' found in grammar 'Mid2'"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(37, 5, 3, 14),
                "More than one rule named 'A' found in grammar 'Test'"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(123, 5, 9, 14),
                "More than one rule named 'A' found in grammar 'Test'"
            ),
        )

        test(grammarStr, expected, Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })
    }

    @Test
    fun extends_diamond_repeat_with_override2_fails2() {
        val grammarStr = """
            namespace ns.test
            grammar Base {
                leaf A = 'a' ;
            }
            grammar Mid1 : Base {
                leaf B = 'b' ;
            }
            grammar Mid2 : Base {
                override leaf A +=| 'c' ;
            }
            grammar Test : Mid1, Mid2 {
              S = A B;
            }
        """.trimIndent()

        val expected = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(37, 5, 3, 14),
                "More than one rule named 'A' found in grammar 'Test'"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(123, 5, 9, 25),
                "More than one rule named 'A' found in grammar 'Test'"
            ),
        )

        test(grammarStr, expected, Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })
    }

    @Test
    fun extends_diamond_repeat_with_override2() {
        val grammarStr = """
            namespace ns.test
            grammar Base {
                leaf A = 'a' ;
            }
            grammar Mid1 : Base {
                leaf B = 'b' ;
            }
            grammar Mid2 : Base {
                override leaf A +=| 'c' ;
            }
            grammar Test : Mid1, Mid2 {
                S = A B;
                override leaf A = Mid2.A ;
            }
        """.trimIndent()

        test(grammarStr, emptySet(), Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })
    }

    @Test
    fun extends_diamond_repeat_with_override3() {
        val grammarStr = """
            namespace ns.test
            grammar Annotations {
                Annotation = 'annotation' ;
            }
            grammar Mid1 : Base {
                leaf B = 'b' ;
            }
            grammar Mid2 : Base {
                override leaf A +=| 'c' ;
            }
            grammar Test : Mid1, Mid2 {
                S = A B;
                override leaf A = Mid2.A ;
            }
        """.trimIndent()

        test(grammarStr, emptySet(), Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })
    }

    @Test
    fun extends_xxx_fails() {
        val grammarStr = """
            namespace ns.test
            
            grammar Base {
                leaf ID = "[a-z]+" ;
            }
            grammar Annotations : Base {
                Annotation = 'annotation' ;
            }
            grammar Relationships : Annotations {
                Relationship = 'relationship' ;
            }
            grammar Containers : Relationships {
                Container = 'container' ;
            }
            
            grammar LiteralExpressions : Base {
                LiteralExpression = 'literal-expression' ;
            }
            grammar Expressions : LiteralExpressions {
                Expression = '{' FunctionBodyPart '}' ;
                FunctionBodyPart = 'expr' ;
            }
            grammar Types : LiteralExpressions, Annotations {
                Type = 'type' ;
            }   
            grammar Classifiers : Types {
                Classifier = 'classifier' ;
            }
            grammar Features : Types, Expressions {
                Feature = 'feature' ;
            }
            
            grammar Connectors : Features {
                Connector = 'connector' ;
            }
            grammar Behaviors : Features, Classifiers {
                override FunctionBodyPart = 'function-body' ;
            }
            grammar Meta : Features, Classifiers, Containers {
                Meta = 'meta' ;
            }            
            grammar KerML : Connectors, Behaviors, Meta {
                KerML = 'KerML' ;
                override FunctionBodyPart = Behaviors.FunctionBodyPart ;
            }
        """.trimIndent()

        val expected = emptySet<LanguageIssue>()

        test(grammarStr, expected, Agl.options {
            semanticAnalysis {
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        })
    }
}