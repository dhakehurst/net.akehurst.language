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

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.LanguageObjectAbstract
import net.akehurst.language.agl.simple.SemanticAnalyserSimple
import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.CompletionProvider
import net.akehurst.language.api.processor.Formatter
import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.api.processor.LanguageObject
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.ProcessOptions
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.automaton.api.Automaton
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.automaton.leftcorner.aut
import net.akehurst.language.format.processor.FormatterOverAsmSimple
import net.akehurst.language.formatter.api.AglFormatDomain
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammar.api.RuleItem
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.parser.api.ParseOptions
import net.akehurst.language.parser.api.RuleSet
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.style.api.AglStyleDomain
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.builder.typesDomain

// sample
object GeneratedGrammar_Simple : LanguageObjectAbstract<Asm, ContextWithScope<Any, Any>>() {

    val issues = IssueHolder(LanguageProcessorPhase.ALL)

    override val identity = LanguageIdentity("Test")

    override val extends: List<LanguageObject<Any, ContextWithScope<Any, Any>>> = emptyList()

    override val grammarString = """
        namespace test
        grammar Test {
          S = 'a' ;
        }
     """.trimIndent()

    override val typesString: String = ""
    override val kompositeString: String = ""
    override val asmTransformString: String = ""
    override val crossReferenceString: String = ""
    override val styleString: String = ""
    override val formatString: String = ""

    override val grammarDomain: GrammarDomain get() = TODO("not implemented")
    override val typesDomain: TypesDomain = typesDomain("test", true) {
        TODO("build type model")
    }
    override val kompositeDomain: TypesDomain get() = typesDomain
    override val asmTransformDomain: AsmTransformDomain get() = TODO()
    override val crossReferenceDomain: CrossReferenceDomain get() = TODO("builder for cross reference model")
    override val styleDomain: AglStyleDomain get() = TODO("not implemented")
    override val formatDomain: AglFormatDomain get() = TODO("not implemented")
    override val defaultTargetGrammar: Grammar
        get() = TODO("not implemented")
    override val defaultTargetGoalRule: String
        get() = TODO("not implemented")

    override val targetRuleSet: RuleSet = ruleSet("Test") {
        concatenation("S") { literal("a") }
    }

    private val automaton_S = aut(targetRuleSet, AutomatonKind.LOOKAHEAD_1, "S", false) {
        // 0: G = . S
        state(GOAL_RULE, OP_NONE, SR)
        // 1: G = S .
        state(GOAL_RULE, OP_NONE, ER)
        // 2: S = a .
        state(0, OP_NONE, ER)
        // 3: a.
        state(1, OP_NONE, ER)

        transition(WIDTH) { source(0); target(3) }
    }

    val defaultGoalRuleName: String = "S"
    override val mapToGrammar: (Int, Int) -> RuleItem get() = { _, _ -> TODO() }

    override val syntaxAnalyser: SyntaxAnalyser<Asm> get() = TODO()

    // SyntaxAnalyserDefault(grammar.qualifiedName, TypeModelFromGrammar.create(grammar), asmTransformModel)
    override val semanticAnalyser: SemanticAnalyser<Asm, ContextWithScope<Any, Any>> = SemanticAnalyserSimple(typesDomain, crossReferenceDomain)
    override val completionProvider: CompletionProvider<Asm, ContextWithScope<Any, Any>>?
        get() = TODO("not implemented")
    val formatter: Formatter<Asm> = FormatterOverAsmSimple(formatDomain, typesDomain, this.issues)
    override val automata: Map<String, Automaton> = mapOf(
        "S" to automaton_S
    )

    val processor: LanguageProcessor<Asm, ContextWithScope<Any, Any>> by lazy { Agl.processorFromLanguageObject(this) }

    fun parse(sentence: String, options: ParseOptions? = null) = processor.parse(sentence, options)

    fun process(sentence: String, options: ProcessOptions<Asm, ContextWithScope<Any, Any>>? = null) = processor.process(sentence, options)
}