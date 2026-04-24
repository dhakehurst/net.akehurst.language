package net.akehurst.language.agl.generators

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand

fun main() {
    val rrs = ruleSet("Test") {
        choiceLongest("S") {
            ref("ABCZ")
            ref("ABC")
        }
        concatenation("ABCZ") { literal("a"); literal("b"); literal("c"); literal("z") }
        concatenation("ABC") { literal("a"); literal("b"); literal("c") }
    }
    //val automaton = rrs.automatonFor("S", AutomatonKind.LOOKAHEAD_1)

    val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
    val result = parser.parseForGoal("S", "abcz")
    val automaton = rrs.usedAutomatonFor("S")

    val generator = GenerateAutomatonDslForTests()
    val output = generator.generateFromAsm("Automaton", automaton)
    println(generator.issues.toString())
    println(output)
}