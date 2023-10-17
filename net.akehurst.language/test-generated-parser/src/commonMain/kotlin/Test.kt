/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import net.akehurst.language.agl.api.generator.GeneratedLanguageProcessorAbstract
import net.akehurst.language.agl.api.runtime.RuleSet
import net.akehurst.language.agl.default.SemanticAnalyserDefault
import net.akehurst.language.agl.default.SyntaxAnalyserDefault
import net.akehurst.language.agl.default.TypeModelFromGrammar
import net.akehurst.language.agl.formatter.FormatterSimple
import net.akehurst.language.agl.grammar.grammar.ContextFromGrammar
import net.akehurst.language.agl.grammar.scopes.ScopeModelAgl
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.semanticAnalyser.ContextSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.automaton.Automaton
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.semanticAnalyser.ScopeModel
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser

// sample
object GeneratedGrammar_Simple : GeneratedLanguageProcessorAbstract<AsmSimple, ContextSimple>() {

    override val grammarString = """
        namespace test
        grammar Test {
          S = 'a' ;
        }
     """.trimIndent()

    override val scopeModelString = """
    """

    override val ruleSet: RuleSet = RuleSet.build("Test") {
        concatenation("S") { literal("a") }
    }

    private val automaton_S = Automaton.build(ruleSet, AutomatonKind.LOOKAHEAD_1, "S", false) {
        // 0: G = . S
        state(GOAL_RULE, 0, SR)
        // 1: G = S .
        state(GOAL_RULE, 0, ER)
        // 2: S = a .
        state(0, 0, ER)
        // 3: a.
        state(1, 0, ER)

        transition(WIDTH) { source(0); target(3) }
    }

    override val defaultGoalRuleName: String = "S"
    override val mapToGrammar: (Int, Int) -> RuleItem get() = { _, _ -> TODO() }
    override val scopeModel: ScopeModel by lazy {
        val res = ScopeModelAgl.fromString(ContextFromGrammar(grammar), scopeModelString)
        val asm = res.asm ?: error("Error creating ScopeModel.\n${res.issues}")
        asm
    }

    override val syntaxAnalyser: SyntaxAnalyser<AsmSimple> = SyntaxAnalyserDefault(grammar.qualifiedName, TypeModelFromGrammar.create(grammar), scopeModel)
    override val semanticAnalyser: SemanticAnalyser<AsmSimple, ContextSimple> = SemanticAnalyserDefault(scopeModel)
    override val formatter: Formatter<AsmSimple> = FormatterSimple(null)
    override val automata: Map<String, Automaton> = mapOf(
        "S" to automaton_S
    )

    val processor: LanguageProcessor<AsmSimple, ContextSimple> by lazy { Agl.processorFromGeneratedCode(this) }

    fun parse(sentence: String, options: ParseOptions? = null) = processor.parse(sentence, options)

    fun process(sentence: String, options: ProcessOptions<AsmSimple, ContextSimple>? = null) = processor.process(sentence, options)
}