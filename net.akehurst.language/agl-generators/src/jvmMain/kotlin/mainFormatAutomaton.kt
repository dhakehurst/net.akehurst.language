package net.akehurst.language.agl.generators

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand

fun main() {
    val rrs = ruleSet("Test") {
        concatenation("S") { ref("E") }
        choiceLongest("E") {
            ref("E1")
            ref("T")
        }
        concatenation("E1") { ref("E"); literal("a"); ref("T") }
        choiceLongest("T") {
            ref("T1")
            ref("F")
        }
        concatenation("T1") { ref("T"); literal("m"); ref("F") }
        choiceLongest("F") {
            literal("v")
            ref("F2")
        }
        concatenation("F2") { literal("("); ref("E"); literal(")") }
    }
    val automaton = rrs.automatonFor("S", AutomatonKind.LOOKAHEAD_1)

//    val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
//    val sentences = listOf("bcd", "abcd", "bced", "abced", "bcefed", "abcefed")
//    sentences.forEach { parser.parseForGoal("S", it) }
//    val automaton = rrs.usedAutomatonFor("S")

    val generator = GenerateAutomatonDslForTests()
    val output = generator.generateFromAsm("Automaton", automaton)
    println(generator.issues.toString())
    println(output)
}