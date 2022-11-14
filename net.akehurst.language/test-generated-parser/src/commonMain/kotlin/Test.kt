import net.akehurst.language.agl.api.generator.GeneratorConstants
import net.akehurst.language.agl.automaton.

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

    val runtimeRuleSet = runtimeRuleSet {
        concatenation("S") { literal("a") }
    }
    val userGoalRuleName = "S"
    val G = runtimeRuleSet.goalRuleFor[userGoalRuleName]

    val stateSetNumber = RuntimeRuleSet.nextRuntimeRuleSetNumber++
    val automaton_S = automaton(runtimeRuleSet, AutomatonKind.LOOKAHEAD_1, userGoalRuleName, stateSetNumber, false) {
        state(G, SR)
        state(G, ER)
    }
}