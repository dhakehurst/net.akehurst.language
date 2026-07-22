package net.akehurst.language.agl.generators

import net.akehurst.language.automaton.api.Automaton
import net.akehurst.language.automaton.api.AutomatonState
import net.akehurst.language.automaton.api.AutomatonTransition
import net.akehurst.language.automaton.api.LookaheadGuard
import net.akehurst.language.automaton.api.ParseAction
import net.akehurst.language.automaton.api.StateNumber
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.parser.api.Rule
import net.akehurst.language.parser.api.RulePosition
import net.akehurst.language.types.builder.typesDomain

class GenerateAutomatonDsl : GeneratorAbstract<Automaton>() {

    companion object {
        val format = $$"""
namespace net.akehurst.language.automaton

format AutomatonDsl {
    fun ruleDecl(AutomatonState) {
       
    }

    Automaton -> "
      automaton(rrs, AutomatonKind.LOOKAHEAD_1, 'S', false) {
        $[state sep $EOL]
        
        $[transition sep $EOL]
      }
    "

    StateNumber -> value
    Rule -> number
    
    AutomatonState -> {
      when {
         1 == rulePosition.size -> {
           rp := rulePosition.first
           when{
             0 == rp.position -> "state(${rp.rule}, ${rp.option}, SR)   // ${rp.asString}"
             -1 == rp.position -> "state(${rp.rule}, ${rp.option}, ER)   // ${rp.asString}"
             else -> "state(${rp.rule}, ${rp.option}, ${rp.position})   // ${rp.asString}"
           }
         }
         else -> "state($[rulePosition sep ', '])"
      }
    }
    
    RulePosition -> when {
      0 == position -> "RP(${rule.tag}, $option, SR)"
      -1 == position -> "RP(${rule.tag}, $option, ER)"
      else -> "RP(${rule.tag}, $option, ${position})"
    }
   
    AutomatonTransition -> "trans($action) { src(${source.number}); tgt(${target.number}); $[lookahead sep ' '] ctx(?) }"

    LookaheadGuard -> when {
      0 == up.size -> "lh($[guard sep ',']);"
      else -> "lh(setOf($[guard sep ',']), setOf($[up sep ',']);"
    }
}
        """.trimIndent()
    }

    override val formatString get() = format
    override val formatSetQualifiedName = "net.akehurst.language.automaton.AutomatonDsl".asQualifiedName
    override val inputTypesDomain = typesDomain("Automaton", true) {
        namespace("net.akehurst.language.automaton.api") {
            interface_("Automaton", implementation = Automaton::class) {
                propertyOfWithBinding(setOf(CMP, VAL), "state", "Set", accessor = Automaton::state) { typeArgument("AutomatonState") }
                propertyOfWithBinding(setOf(CMP, VAL), "transition", "Set", accessor = Automaton::transition) { typeArgument("AutomatonTransition") }
            }
            interface_("AutomatonState", implementation = AutomatonState::class) {
                propertyOfWithBinding(setOf(CMP, VAL), "number", "StateNumber", accessor = AutomatonState::number)
                propertyOfWithBinding(setOf(CMP, VAL), "rulePosition", "List", accessor = AutomatonState::rulePosition) { typeArgument("RulePosition") }
            }
            enum("ParseAction", listOf("HEIGHT", "GRAFT", "WIDTH", "GOAL", "EMBED"), implementation = ParseAction::class)
            interface_("AutomatonTransition", implementation = AutomatonTransition::class) {
                propertyOfWithBinding(setOf(CMP, VAL), "action", "ParseAction", accessor = AutomatonTransition::action)
                propertyOfWithBinding(setOf(REF, VAL), "source", "AutomatonState", accessor = AutomatonTransition::source)
                propertyOfWithBinding(setOf(REF, VAL), "target", "AutomatonState", accessor = AutomatonTransition::target)
                propertyOfWithBinding(setOf(REF, VAL), "lookahead", "Set", accessor = AutomatonTransition::lookahead) { typeArgument("LookaheadGuard") }
            }
            interface_("LookaheadGuard") {
                propertyOfWithBinding(setOf(CMP, VAL), "guard", "Set", accessor = LookaheadGuard::guard) { typeArgument("Rule") }
                propertyOfWithBinding(setOf(CMP, VAL), "up", "Set", accessor = LookaheadGuard::up) { typeArgument("Rule") }
            }
            interface_("RulePosition", implementation = RulePosition::class) {
                propertyOfWithBinding(setOf(CMP, VAL), "rule", "Rule", accessor = RulePosition::rule)
                propertyOfWithBinding(setOf(CMP, VAL), "option", "Integer", accessor = RulePosition::option)
                propertyOfWithBinding(setOf(CMP, VAL), "position", "Integer", accessor = RulePosition::position)
                propertyOfWithBinding(setOf(DER, VAL), "asString", "String", accessor = RulePosition::asString)
            }
            interface_("Rule", implementation = Rule::class) {
                propertyOfWithBinding(setOf(CMP, VAL), "ruleSetNumber", "Integer", accessor = Rule::ruleSetNumber)
                propertyOfWithBinding(setOf(CMP, VAL), "number", "Integer", accessor = Rule::number)
            }
            data("StateNumber", implementation = StateNumber::class) {
                constructor_ {
                    parameter(setOf(REF, VAL), "value", "Integer")
                }
            }
        }
    }

}