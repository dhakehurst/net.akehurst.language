package net.akehurst.language.agl.generators

import net.akehurst.language.automaton.api.Automaton
import net.akehurst.language.automaton.api.AutomatonState
import net.akehurst.language.automaton.api.AutomatonTransition
import net.akehurst.language.automaton.api.LookaheadGuard
import net.akehurst.language.automaton.api.ParseAction
import net.akehurst.language.automaton.api.StateNumber
import net.akehurst.language.automaton.api.TransitionContext
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.parser.api.Rule
import net.akehurst.language.parser.api.RulePosition
import net.akehurst.language.parser.api.RuleSet
import net.akehurst.language.types.builder.typesDomain

class GenerateAutomatonDslForTests : GeneratorAbstract<Automaton>() {

    companion object {
        val format = $$"""
namespace net.akehurst.language.automaton

format RuleDecl {
  RuleSet -> "$[rule sep $EOL]"
  Rule -> when {
    isTerminal -> "private val _t$number = rrs.rule[$number]  // $tag"
    else -> "private val $tag = rrs.rule[$number]  // $tag"
  }
}

format RP {
    AutomatonState -> "$[rulePosition sep ', ']"
    RulePosition -> when {
      0 == position -> "RP(${rule}, $option, SR)"
      -1 == position -> "RP(${rule}, $option, ER)"
      else -> when {
        rule.isListSeparated -> when {
          1 == position -> "RP(${rule}, $option, pSS)"
          2 == position -> "RP(${rule}, $option, pSI)"
          else -> "§ERROR"
        }
        else -> "RP(${rule.tag}, $option, ${position})"
      }
    }
    Rule -> when {
       '<GOAL>'==tag -> 'rG'
       '<EOT>'==tag -> 'EOT'
       '<RT>'==tag -> 'RT'
       '<EMPTY>'==tag -> 'EMPTY'
       '<EMPTY_LIST>'==tag -> 'EMPTY_LIST'
       isTerminal -> "_t$number"
       else -> tag
    }
    OptionNum -> when {
      (-1) == value -> 'oN'
      (-2) == value -> 'oOI'
      (-3) == value -> 'oOE'
      (-4) == value -> 'oLI'
      (-5) == value -> 'oLE'
      (-6) == value -> 'oSI'
      (-7) == value -> 'oSE'
      else -> "o$value"
    }
}

format Trans {
    AutomatonState -> {
      when {
         1 == rulePosition.size -> {
           with(rulePosition.first) when {
             0 == position -> "${rule via RP}, $option, SR"
             -1 == position -> "${rule via RP}, $option, ER"
             else -> "${rule via RP}, $option, ${position}"
           }
         }
         else -> "$[rulePosition sep ', ' via RP])"
      }
    }
}

format AutomatonDsl {
    Automaton -> "
      ${ruleSet via RuleDecl}
      private val rG = rrs.goalRuleFor[S]

      automaton(rrs, AutomatonKind.LOOKAHEAD_1, 'S', false) {
        $[state sep $EOL]
        
        $[transition sep $EOL]
      }
    "

    StateNumber -> value
    
    AutomatonState -> {
      when {
         1 == rulePosition.size -> {
           rp := rulePosition.first
           when{
             0 == rp.position -> "state(${rp.rule via RP}, ${rp.option}, SR)   // ${rp.asString}"
             -1 == rp.position -> "state(${rp.rule via RP}, ${rp.option}, ER)   // ${rp.asString}"
             else -> "state(${rp.rule via RP}, ${rp.option}, ${rp.position})   // ${rp.asString}"
           }
         }
         else -> "state($[rulePosition sep ', ' via RP])"
      }
    }
   
    AutomatonTransition -> when {
      transContext.isEmpty -> when {
        // WIDTH / EMBED — incomplete source state, no prevPrev needed; use legacy ctx(...) form.
        1 == prev.size -> "trans($action) { src(${source via Trans}); tgt(${target via Trans}); $[lookahead sep ' '] ctx(${prev via Trans}) }"
        else -> "trans($action) { src(${source via Trans}); tgt(${target via Trans}); $[lookahead sep ' '] ctx($[prev sep ',' via RP]) }"
      }
      // HEIGHT / GRAFT / GOAL — emit one prevPair(...) per atomic (prevPrev, prev) pair
      else -> "trans($action) { src(${source via Trans}); tgt(${target via Trans}); $[lookahead sep ' ']  $[transContext sep '; '] }"
    }

    TransitionContext -> when {
      ((1 == prevPrev.rulePosition.size) and (1 == prev.rulePosition.size)) ->
        "prevPair(${prevPrev via RP}, ${prev via RP})"
      else ->
        "prevPair(setOf($[prevPrev.rulePosition sep ',' via RP]), setOf($[prev.rulePosition sep ',' via RP]))"
    }

    LookaheadGuard -> when {
      0 == up.size -> when {
        1 == guard.size -> "lhg($[guard sep ',' via RP]);"
        else -> "lhg(setOf($[guard sep ',' via RP]));"
      }
      else -> "lhg(setOf($[guard sep ',' via RP]), setOf($[up sep ',' via RP]));"
    }
}
        """.trimIndent()
    }

    override val formatString get() = format
    override val formatSetQualifiedName = "net.akehurst.language.automaton.AutomatonDsl".asQualifiedName
    override val inputTypesDomain = typesDomain("Automaton", true) {
        namespace("net.akehurst.language.automaton.api") {
            interface_("Rule", implementation = Rule::class) {
                propertyOfWithBinding(setOf(CMP, VAL), "ruleSetNumber", "Integer", accessor = Rule::ruleSetNumber)
                propertyOfWithBinding(setOf(CMP, VAL), "number", "Integer", accessor = Rule::number)
                propertyOfWithBinding(setOf(DER, VAL), "isTerminal", "Boolean", accessor = Rule::isTerminal)
            }
            interface_("RulePosition", implementation = RulePosition::class) {
                propertyOfWithBinding(setOf(CMP, VAL), "rule", "Rule", accessor = RulePosition::rule)
                propertyOfWithBinding(setOf(CMP, VAL), "option", "Integer", accessor = RulePosition::option)
                propertyOfWithBinding(setOf(CMP, VAL), "position", "Integer", accessor = RulePosition::position)
                propertyOfWithBinding(setOf(DER, VAL), "asString", "String", accessor = RulePosition::asString)
            }
            interface_("RuleSet", implementation = RuleSet::class) {
                propertyOfWithBinding(setOf(CMP, VAL), "rule", "List", accessor = RuleSet::rule) { typeArgument("Rule") }
            }
            interface_("Automaton", implementation = Automaton::class) {
                propertyOfWithBinding(setOf(CMP, VAL), "ruleSet", "RuleSet", accessor = Automaton::ruleSet)
                propertyOfWithBinding(setOf(CMP, VAL), "state", "Set", accessor = Automaton::state) { typeArgument("AutomatonState") }
                propertyOfWithBinding(setOf(CMP, VAL), "transition", "Set", accessor = Automaton::transition) { typeArgument("AutomatonTransition") }
            }
            interface_("AutomatonState", implementation = AutomatonState::class) {
                propertyOfWithBinding(setOf(CMP, VAL), "number", "StateNumber", accessor = AutomatonState::number)
                propertyOfWithBinding(setOf(CMP, VAL), "rulePosition", "List", accessor = AutomatonState::rulePosition) { typeArgument("RulePosition") }
            }
            enum("ParseAction", listOf("HEIGHT", "GRAFT", "WIDTH", "GOAL", "EMBED"), implementation = ParseAction::class)
            interface_("AutomatonTransition", implementation = AutomatonTransition::class) {
                propertyOfWithBinding(setOf(VAL), "action", "ParseAction", accessor = AutomatonTransition::action)
                propertyOfWithBinding(setOf(VAL), "source", "AutomatonState", accessor = AutomatonTransition::source)
                propertyOfWithBinding(setOf(VAL), "target", "AutomatonState", accessor = AutomatonTransition::target)
                propertyOfWithBinding(setOf(VAL), "lookahead", "Set", accessor = AutomatonTransition::lookahead) { typeArgument("LookaheadGuard") }
                propertyOfWithBinding(setOf(VAL), "prev", "Set", accessor = AutomatonTransition::prev) { typeArgument("AutomatonState") }
                propertyOfWithBinding(setOf(VAL), "prevPrev", "Set", accessor = AutomatonTransition::prevPrev) { typeArgument("AutomatonState") }
                propertyOfWithBinding(setOf(VAL), "transContext", "Set", accessor = AutomatonTransition::transContext) { typeArgument("TransitionContext") }
            }
            interface_("TransitionContext", implementation = TransitionContext::class) {
                propertyOfWithBinding(setOf(CMP, VAL), "prevPrev", "AutomatonState", accessor = TransitionContext::prevPrev)
                propertyOfWithBinding(setOf(CMP, VAL), "prev", "AutomatonState", accessor = TransitionContext::prev)
            }
            interface_("LookaheadGuard") {
                propertyOfWithBinding(setOf(CMP, VAL), "guard", "Set", accessor = LookaheadGuard::guard) { typeArgument("Rule") }
                propertyOfWithBinding(setOf(CMP, VAL), "up", "Set", accessor = LookaheadGuard::up) { typeArgument("Rule") }
            }
            data("StateNumber", implementation = StateNumber::class) {
                constructor_ {
                    parameter(setOf(REF, VAL), "value", "Integer")
                }
            }
            data("OptionNum", implementation = StateNumber::class) {
                constructor_ {
                    parameter(setOf(REF, VAL), "value", "Integer")
                }
            }
        }
    }

}