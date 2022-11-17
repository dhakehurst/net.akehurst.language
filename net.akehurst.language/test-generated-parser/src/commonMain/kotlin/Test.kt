import net.akehurst.language.agl.api.automaton.Automaton
import net.akehurst.language.agl.api.generator.GeneratorConstants
import net.akehurst.language.agl.api.runtime.RuleSet
import net.akehurst.language.api.processor.AutomatonKind

// sample
object GrammarName_GoalRuleName : GeneratorConstants() {

    val asString = """
        namespace test
        grammar Test {
          S = a ;
        }
     """.trimIndent()

    // G = S
    // S = a

    val runtimeRuleSet: RuleSet = RuleSet.build {
        concatenation("S") { literal("a") }
    }

    val automaton_S = Automaton.build(runtimeRuleSet, AutomatonKind.LOOKAHEAD_1, "S", false) {
        // G = . S
        state(0, 0, SR)
        // G = S .
        state(1, 0, ER)
        // S = a .
        state(2, 0, ER)
        // a.
        state(3, 0, ER)

        transition(WIDTH) { src(0); tgt(3) }
    }
}