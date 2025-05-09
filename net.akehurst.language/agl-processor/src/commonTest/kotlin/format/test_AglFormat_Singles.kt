package format

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.format.processor.AglFormat
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_AglFormat_Singles {
    private companion object {
        fun processor(targetGrammar: String): LanguageProcessor<Asm, ContextWithScope<Any, Any>> {
            Agl.registry.agl.expressions
            return Agl.processorFromGrammar(
                AglFormat.grammarModel,
                Agl.configuration(base = Agl.configurationSimple()) {
                    targetGrammarName(targetGrammar)
                }
            )
        }

        private fun test_process(targetGrammar: String, goal: String, sentence: String, expectedAsm: Asm? = null) {
            //val proc = processor(targetGrammar)
            val proc = Agl.registry.agl.format.processor!!
            val result = proc.process(
                sentence,
                Agl.options {
                    parse {
                        goalRuleName(goal)
                    }
                }
            )
            assertNotNull(result.asm, result.issues.toString())
            assertTrue(result.issues.errors.isEmpty(), "'${sentence}'\n${result.issues}")
            expectedAsm?.let {
                TODO()
            }
        }
    }

    @Test
    fun Format_unit_namespace_empty() {
        val targetGrammar = "Format"
        val goal = "unit"
        val sentence = """
            namespace test
        """.trimIndent()
        val asm = null
        test_process(targetGrammar, goal, sentence, asm)
    }

    @Test
    fun Format_unit_format_empty() {
        val targetGrammar = "Format"
        val goal = "unit"
        val sentence = """
            namespace test
            format F { }
        """.trimIndent()
        val asm = null
        test_process(targetGrammar, goal, sentence, asm)
    }

    @Test
    fun Format_unit_template_empty() {
        val targetGrammar = "Format"
        val goal = "unit"
        val sentence = """
            namespace test
            format F {
                Type -> "" 
            }
        """.trimIndent()
        val asm = null
        test_process(targetGrammar, goal, sentence, asm)
    }

    @Test
    fun Format_unit_template_with_text() {
        val targetGrammar = "Format"
        val goal = "unit"
        val sentence = """
            namespace test
            format F {
                Type -> "he said" 
            }
        """.trimIndent()
        val asm = null
        test_process(targetGrammar, goal, sentence, asm)
    }

    @Test
    fun Format_unit_template_with_text_single_quote() {
        val targetGrammar = "Format"
        val goal = "unit"
        val sentence = """
            namespace test
            format F {
                Type -> "he said 'hello' to me!" 
            }
        """.trimIndent()
        val asm = null
        test_process(targetGrammar, goal, sentence, asm)
    }

    @Test
    fun Format_unit_template_with_text_escaped_double_quote() {
        val targetGrammar = "Format"
        val goal = "unit"
        val sentence = """
            namespace test
            format F {
                Type -> "he said \"hello\" to me!" 
            }
        """.trimIndent()
        val asm = null
        test_process(targetGrammar, goal, sentence, asm)
    }

    @Test
    fun Format_unit_template_with_text_prop_text() {
        val targetGrammar = "Format"
        val goal = "unit"
        val sentence = $$"""
            namespace test
            format F {
                Type -> "he said $greet to me!" 
            }
        """.trimIndent()
        val asm = null
        test_process(targetGrammar, goal, sentence, asm)
    }

    @Test
    fun Format_unit_template_with_text_list_text() {
        val targetGrammar = "Format"
        val goal = "unit"
        val sentence = $$"""
            namespace test
            format F {
                Type -> "he said $[greetings / ','] to me!" 
            }
        """.trimIndent()
        val asm = null
        test_process(targetGrammar, goal, sentence, asm)
    }

    @Test
    fun Format_unit_template_with_text_fmtEpr_text() {
        val targetGrammar = "Format"
        val goal = "unit"
        val sentence = $$"""
            namespace test
            format F {
                Type -> "he said ${a+b} to me!" 
            }
        """.trimIndent()
        val asm = null
        test_process(targetGrammar, goal, sentence, asm)
    }

    @Test
    fun Template_RAW_TEXT() {
        val targetGrammar = "Template"
        val goal = "RAW_TEXT"
        val sentence = ""
        val asm = null
        test_process(targetGrammar, goal, sentence, asm)
    }

}