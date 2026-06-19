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
                propertyOf(setOf(CMP, VAL), "state", "Set", execution = Automaton::state) { typeArgument("AutomatonState") }
                propertyOf(setOf(CMP, VAL), "transition", "Set", execution = Automaton::transition) { typeArgument("AutomatonTransition") }
            }
            interface_("AutomatonState", implementation = AutomatonState::class) {
                propertyOf(setOf(CMP, VAL), "number", "StateNumber", execution = AutomatonState::number)
                propertyOf(setOf(CMP, VAL), "rulePosition", "List", execution = AutomatonState::rulePosition) { typeArgument("RulePosition") }
            }
            enum("ParseAction", listOf("HEIGHT", "GRAFT", "WIDTH", "GOAL", "EMBED"), implementation = ParseAction::class)
            interface_("AutomatonTransition", implementation = AutomatonTransition::class) {
                propertyOf(setOf(CMP, VAL), "action", "ParseAction", execution = AutomatonTransition::action)
                propertyOf(setOf(REF, VAL), "source", "AutomatonState", execution = AutomatonTransition::source)
                propertyOf(setOf(REF, VAL), "target", "AutomatonState", execution = AutomatonTransition::target)
                propertyOf(setOf(REF, VAL), "lookahead", "Set", execution = AutomatonTransition::lookahead) { typeArgument("LookaheadGuard") }
            }
            interface_("LookaheadGuard") {
                propertyOf(setOf(CMP, VAL), "guard", "Set", execution = LookaheadGuard::guard) { typeArgument("Rule") }
                propertyOf(setOf(CMP, VAL), "up", "Set", execution = LookaheadGuard::up) { typeArgument("Rule") }
            }
            interface_("RulePosition", implementation = RulePosition::class) {
                propertyOf(setOf(CMP, VAL), "rule", "Rule", execution = RulePosition::rule)
                propertyOf(setOf(CMP, VAL), "option", "Integer", execution = RulePosition::option)
                propertyOf(setOf(CMP, VAL), "position", "Integer", execution = RulePosition::position)
                propertyOf(setOf(DER, VAL), "asString", "String", execution = RulePosition::asString)
            }
            interface_("Rule", implementation = Rule::class) {
                propertyOf(setOf(CMP, VAL), "ruleSetNumber", "Integer", execution = Rule::ruleSetNumber)
                propertyOf(setOf(CMP, VAL), "number", "Integer", execution = Rule::number)
            }
            data("StateNumber", implementation = StateNumber::class) {
                constructor_ {
                    parameter(setOf(REF, VAL), "value", "Integer")
                }
            }
        }
    }

}