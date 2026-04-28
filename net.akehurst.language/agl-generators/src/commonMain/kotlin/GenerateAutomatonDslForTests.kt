package net.akehurst.language.agl.generators

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.language.agl.generators.GenerateGrammarDomainBuild.Companion.generatedFormat
import net.akehurst.language.agl.processor.contextFromRegistryGrammars
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.automaton.api.Automaton
import net.akehurst.language.automaton.api.AutomatonState
import net.akehurst.language.automaton.api.AutomatonTransition
import net.akehurst.language.automaton.api.LookaheadGuard
import net.akehurst.language.automaton.api.ParseAction
import net.akehurst.language.automaton.api.StateNumber
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.base.api.asQualifiedName
import net.akehurst.language.format.processor.FormatterOverTypedObject
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammar.processor.AglGrammar
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
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
          1 == position -> "RP(${rule}, $option, LIST_SEPARATOR)"
          2 == position -> "RP(${rule}, $option, LIST_ITEM)"
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
      (-2) == value -> 'OI'
      (-3) == value -> 'OE'
      (-4) == value -> 'LI'
      (-5) == value -> 'LE'
      (-6) == value -> 'SI'
      (-7) == value -> 'SE'
      else -> value
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
      prevPrev.isEmpty -> when {
        1 == prev.size -> "trans($action) { src(${source via Trans}); tgt(${target via Trans}); $[lookahead sep ' '] ctx(${prev via Trans}) }"
        else -> "trans($action) { src(${source via Trans}); tgt(${target via Trans}); $[lookahead sep ' '] ctx($[prev sep ',' via RP]) }"
      }
      1 == prevPrev.size -> when {
        1 == prev.size -> "trans($action) { src(${source via Trans}); tgt(${target via Trans}); $[lookahead sep ' '] ctx(${prev via Trans}); pctx(${prevPrev via Trans}) }"
        else -> "trans($action) { src(${source via Trans}); tgt(${target via Trans}); $[lookahead sep ' '] ctx($[prev sep ',' via RP]); pctx(${prevPrev via Trans}) }"
      }
      else -> when {
        1 == prev.size -> "trans($action) { src(${source via Trans}); tgt(${target via Trans}); $[lookahead sep ' '] ctx(${prev via Trans}); pctx($[prevPrev sep ',' via RP]) }"
        else -> "trans($action) { src(${source via Trans}); tgt(${target via Trans}); $[lookahead sep ' '] ctx($[prev sep ',' via RP]); pctx($[prevPrev sep ',' via RP]) }"
      }
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
                propertyOf(setOf(CMP, VAL), "ruleSetNumber", "Integer", execution = Rule::ruleSetNumber)
                propertyOf(setOf(CMP, VAL), "number", "Integer", execution = Rule::number)
                propertyOf(setOf(DER, VAL), "isTerminal", "Boolean", execution = Rule::isTerminal)
            }
            interface_("RulePosition", implementation = RulePosition::class) {
                propertyOf(setOf(CMP, VAL), "rule", "Rule", execution = RulePosition::rule)
                propertyOf(setOf(CMP, VAL), "option", "Integer", execution = RulePosition::option)
                propertyOf(setOf(CMP, VAL), "position", "Integer", execution = RulePosition::position)
                propertyOf(setOf(DER, VAL), "asString", "String", execution = RulePosition::asString)
            }
            interface_("RuleSet", implementation = RuleSet::class) {
                propertyOf(setOf(CMP, VAL), "rule", "List", execution = RuleSet::rule) { typeArgument("Rule") }
            }
            interface_("Automaton", implementation = Automaton::class) {
                propertyOf(setOf(CMP, VAL), "ruleSet", "RuleSet", execution = Automaton::ruleSet)
                propertyOf(setOf(CMP, VAL), "state", "Set", execution = Automaton::state) { typeArgument("AutomatonState") }
                propertyOf(setOf(CMP, VAL), "transition", "Set", execution = Automaton::transition) { typeArgument("AutomatonTransition") }
            }
            interface_("AutomatonState", implementation = AutomatonState::class) {
                propertyOf(setOf(CMP, VAL), "number", "StateNumber", execution = AutomatonState::number)
                propertyOf(setOf(CMP, VAL), "rulePosition", "List", execution = AutomatonState::rulePosition) { typeArgument("RulePosition") }
            }
            enum("ParseAction", listOf("HEIGHT", "GRAFT", "WIDTH", "GOAL", "EMBED"), implementation = ParseAction::class)
            interface_("AutomatonTransition", implementation = AutomatonTransition::class) {
                propertyOf(setOf(VAL), "action", "ParseAction", execution = AutomatonTransition::action)
                propertyOf(setOf(VAL), "source", "AutomatonState", execution = AutomatonTransition::source)
                propertyOf(setOf(VAL), "target", "AutomatonState", execution = AutomatonTransition::target)
                propertyOf(setOf(VAL), "lookahead", "Set", execution = AutomatonTransition::lookahead) { typeArgument("LookaheadGuard") }
                propertyOf(setOf(VAL), "prev", "Set", execution = AutomatonTransition::prev) { typeArgument("AutomatonState") }
                propertyOf(setOf(VAL), "prevPrev", "Set", execution = AutomatonTransition::prevPrev) { typeArgument("AutomatonState") }
            }
            interface_("LookaheadGuard") {
                propertyOf(setOf(CMP, VAL), "guard", "Set", execution = LookaheadGuard::guard) { typeArgument("Rule") }
                propertyOf(setOf(CMP, VAL), "up", "Set", execution = LookaheadGuard::up) { typeArgument("Rule") }
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