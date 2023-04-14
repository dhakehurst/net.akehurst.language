import net.akehurst.language.api.automaton.Automaton
import net.akehurst.language.agl.api.generator.GeneratedLanguageProcessorAbstract
import net.akehurst.language.agl.api.runtime.RuleSet
import net.akehurst.language.agl.formatter.FormatterSimple
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.semanticAnalyser.SemanticAnalyserSimple
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.agl.syntaxAnalyser.SyntaxAnalyserSimple
import net.akehurst.language.agl.syntaxAnalyser.TypeModelFromGrammar
import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.analyser.SyntaxAnalyser
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.processor.*

// sample
object GeneratedGrammar_Simple : GeneratedLanguageProcessorAbstract<AsmSimple, ContextSimple>() {

    override val grammarString = """
        namespace test
        grammar Test {
          S = 'a' ;
        }
     """.trimIndent()

    override val ruleSet: RuleSet = RuleSet.build {
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
    override val scopeModel: ScopeModel?
        get() = TODO("not implemented")

    override val syntaxAnalyser: SyntaxAnalyser<AsmSimple> = SyntaxAnalyserSimple(TypeModelFromGrammar(grammar), scopeModel)
    override val semanticAnalyser: SemanticAnalyser<AsmSimple, ContextSimple> = SemanticAnalyserSimple(scopeModel)
    override val formatter: Formatter<AsmSimple> = FormatterSimple(null)
    override val automata: Map<String, Automaton> = mapOf(
        "S" to automaton_S
    )

    val processor: LanguageProcessor<AsmSimple, ContextSimple> by lazy { Agl.processorFromGeneratedCode(this) }

    fun parse(sentence: String, options: ParseOptions? = null) = processor.parse(sentence,options)

    fun process(sentence: String,options: ProcessOptions<AsmSimple, ContextSimple>? = null) = processor.process(sentence, options)
}