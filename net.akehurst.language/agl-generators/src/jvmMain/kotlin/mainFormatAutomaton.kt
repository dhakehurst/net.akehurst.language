package net.akehurst.language.agl.generators

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand

fun main() {
    val rrsB = ruleSet("rrsB") {
        concatenation("B") { literal("b") }
    }
    val rrs = ruleSet("Test") {
        concatenation("S") { literal("a"); ref("gB"); literal("a"); }
        embedded("gB", rrsB, "B")
    }
//    val automaton = rrsB.automatonFor("B", AutomatonKind.LOOKAHEAD_1)

    val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
    val sentences = listOf("aba")
    sentences.forEach { parser.parseForGoal("S", it) }
    val automaton = rrs.usedAutomatonFor("S")

    val generator = GenerateAutomatonDslForTests()
    val output = generator.generateFromAsm("Automaton", automaton)
    println(generator.issues.toString())
    println(output)
}