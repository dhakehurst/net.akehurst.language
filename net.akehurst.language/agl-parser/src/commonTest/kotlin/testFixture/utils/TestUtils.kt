package testFixture.utils

import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.sentence.common.SentenceDefault

fun parseError(location: InputLocation, sentence: String, tryingFor:Set<String>, expected: Set<String>): LanguageIssue {
    val failed = tryingFor.sorted().joinToString(separator = " | ")
    val posIndication = SentenceDefault(sentence, null).contextInText(location.position)
    val message = "Failed to match {$failed} at: $posIndication"
    return LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, location, message, expected)
}