/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.parser.leftcorner

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.automaton.api.ParseAction
import net.akehurst.language.automaton.leftcorner.LookaheadSet
import net.akehurst.language.automaton.leftcorner.ParserStateSet
import net.akehurst.language.issues.api.IssueCollection
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.parser.api.*
import net.akehurst.language.parsermessages.IssueMessage
import net.akehurst.language.scanner.api.Matchable
import net.akehurst.language.scanner.api.MatchableKind
import net.akehurst.language.scanner.api.Scanner
import net.akehurst.language.scanner.common.ScanOptionsDefault
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.sentence.api.Sentence
import net.akehurst.language.sentence.common.SentenceDefault
import net.akehurst.language.sppt.api.SharedPackedParseTree
import net.akehurst.language.sppt.treedata.SPPTFromTreeData
import kotlin.math.max

object SentenceIdentityFunctionNull : SentenceIdentityFunction {
    override fun invoke(): Any? = null
}

class ParseOptionsDefault(
    override var enabled: Boolean = true,
    override var goalRuleName: String? = null,
    override var sentenceIdentity: SentenceIdentityFunction = SentenceIdentityFunctionNull,
    override var reportErrors: Boolean = true,
    override var reportGrammarAmbiguities: Boolean = false,
    override var cacheSkip: Boolean = true,
    override var reverseFindWordStartRegex: String? = null,
    override var reverseFindWordStartBySkipRules: Boolean = true,
    override var expectedAtFromAllTokenStarts: Boolean = false
) : ParseOptions {
    override fun clone() = ParseOptionsDefault(
        enabled = enabled,
        goalRuleName = goalRuleName,
        sentenceIdentity = sentenceIdentity,
        reportErrors = reportErrors,
        reportGrammarAmbiguities = reportGrammarAmbiguities,
        cacheSkip = cacheSkip,
        reverseFindWordStartRegex = reverseFindWordStartRegex,
        reverseFindWordStartBySkipRules = reverseFindWordStartBySkipRules,
        expectedAtFromAllTokenStarts = expectedAtFromAllTokenStarts
    )
}

data class ParseResultDefault(
    override val sppt: SharedPackedParseTree?,
    override val issues: IssueCollection<LanguageIssue>
) : ParseResult

data class ParseFailureRuntime(
    override val failedSpines: List<RuntimeSpine>
) : ParseFailure {

    val expectedTerminals get() = failedSpines.flatMap { it.expectedNextTerminals }.toSet()
    val expected get() = expectedTerminals.map { it.tag }.toSet()

    fun message(sentence: Sentence, location: InputLocation): String {
        val p = failedSpines.map { it.elements.last().rule.tag }.joinToString(separator = " | ")
        val contextInText = sentence.contextInText(location.position)
        val message = "Failed to match {$p} at: $contextInText"
        return message
    }
}

data class ExpectedAtResultDefault(
    override val usedPosition: Int,
    override val spines: Set<RuntimeSpine>
) : ExpectedAtResult

class LeftCornerParser(
    val scanner: Scanner,
    ruleSet: RuleSet
) : Parser {

    val automatonKind = AutomatonKind.LOOKAHEAD_1 //TODO: make configuration arg
    override val ruleSet: RuleSet get() = runtimeRuleSet
    internal val runtimeRuleSet = ruleSet as RuntimeRuleSet

    // cached only so it can be interrupted
    private var runtimeParser: RuntimeParserAgl? = null

    private val _issues = IssueHolder(LanguageProcessorPhase.PARSE)

    val runtimeDataIsEmpty: Boolean get() = runtimeParser?.graph?.isEmpty ?: true

    override fun reset() {
        runtimeParser?.reset() //is this necessary?
        runtimeParser = null
    }

    override fun interrupt(message: String) {
        this.runtimeParser?.interrupt(message)
    }

    override fun buildFor(goalRuleName: String) {
        this.runtimeRuleSet.buildFor(goalRuleName, automatonKind)
    }

    override fun parseForGoal(goalRuleName: String, sentenceText: String): ParseResult =
        this.parse(
            sentenceText,
            ParseOptionsDefault(
                goalRuleName = goalRuleName
            )
        )

    override fun parse(sentenceText: String, options: ParseOptions): ParseResult {
        check(sentenceText.length < Int.MAX_VALUE) { "The parser can only handle a max sentence size < ${Int.MAX_VALUE} characters, requested size was ${sentenceText.length}" }
        val sentence = SentenceDefault(sentenceText, options.sentenceIdentity?.invoke())
        val goalRuleName = options.goalRuleName ?: error("Must define a goal rule in options")
        val reportErrors = options.reportErrors
        val reportGrammarAmbiguities = options.reportGrammarAmbiguities
        val cacheSkip = options.cacheSkip
        _issues.clear()
        scanner.reset()
        val rp = createRuntimeParser(sentence, goalRuleName, scanner, automatonKind, cacheSkip)
        this.runtimeParser = rp

        val possibleEndOfText = setOf(LookaheadSet.EOT)
        val parseArgs = RuntimeParserAgl.Companion.GrowArgs(
            true,
            false,
            false,
            false,
            reportErrors,
            reportGrammarAmbiguities,
            false,
            false
        )
        var seasons = 1
        var maxNumHeads = 0
        try {
            val startSkipFailures = rp.start(0, possibleEndOfText, parseArgs)
            maxNumHeads = rp.graph.numberOfHeads
            var totalWork = maxNumHeads

            while (rp.graph.hasNextHead && (rp.graph.goals.isEmpty() || rp.graph.goalMatchedAll.not())) {
                if (Debug.OUTPUT_RUNTIME) println("season $seasons ===================================")
                val steps = rp.grow3(possibleEndOfText, parseArgs)
                seasons += steps
                maxNumHeads = max(maxNumHeads, rp.graph.numberOfHeads)
                totalWork += rp.graph.numberOfHeads
            }
        } catch (e: ParserWontStopException) {
            val length = if (e.position < sentence.text.length) 1 else 0
            this._issues.error(sentence.locationFor(e.position, length), IssueMessage.PARSER_WONT_STOP)
            return ParseResultDefault(null, this._issues)
        }
        catch (e: ParserInterruptedException) {
            this._issues.error(sentence.locationFor(e.position, 0), IssueMessage.parserTerminated(e.reason))
            return ParseResultDefault(null, this._issues)
        }

        val match = rp.graph.treeData.complete

        return if (match.root != null) {
            val sppt = SPPTFromTreeData(match, sentence, seasons, maxNumHeads)
            ParseResultDefault(sppt, this._issues)
        } else {
            createParseIssuesFromFailures(sentence, options, rp)

            val sppt = null//TODO: provide best effort - SPPTFromTreeData(match, sentence, seasons, maxNumHeads)
            ParseResultDefault(sppt, this._issues)
        }
    }

    private fun createParseIssuesFromFailures(sentence: Sentence, options: ParseOptions, rp: RuntimeParserAgl) {
        if (options.reportErrors) {
            // need to include the 'startSkipFailures',
            val map1 = rp.failedReasons //no need to clone it as it will not be modified after this point
            //         startSkipFailures.forEach {
            //             val l = map[it.key] ?: mutableListOf()
            //             l.addAll(it.value)
            //             map[it.key] = l
            //         }

            // an embedded failure is reported at the position where the embedded parse stopped,
            // but in the context of the outer (embedding) rule, i.e. using the spine of the embedding head
            // (which also removes outer skip terminals that are not terminals of the embedded grammar)
            // Triple(position, isSkipFailure, spine)
            val failures: List<Triple<Int, Boolean, RuntimeSpineDefault>> = map1.values.flatten().flatMap { v ->
                when {
                    v is FailedParseReasonEmbedded && v.embededFailedParseReasons.isNotEmpty() -> v.embededFailedParseReasons
                        .groupBy { Pair(it.failedAtPosition, it.skipFailure) }
                        .map { (key, reasons) -> Triple(key.first, key.second, v.spineFor(reasons)) }

                    else -> listOf(Triple(v.failedAtPosition, v.skipFailure, v.spine))
                }
            }
            val map2 = failures.groupBy { it.first }

            if (map2.isNotEmpty()) {
                val failedAtPosition = map2.keys.max()
                val nextExpected = map2[failedAtPosition]?.filter { it.second.not() }?.map { it.third } ?: emptyList()
                val loc = sentence.locationFor(failedAtPosition, 0)
                addParseIssue(sentence, loc, nextExpected)
            }
        }
    }

    private fun createRuntimeParser(sentence: Sentence, goalRuleName: String, scanner: Scanner, automatonKind: AutomatonKind, cacheSkip: Boolean): RuntimeParserAgl {
        val goalRule = this.runtimeRuleSet.findRuntimeRule(goalRuleName)
        val s0 = runtimeRuleSet.fetchStateSetFor(goalRuleName, automatonKind).startState
        val skipStateSet = runtimeRuleSet.skipParserStateSet
        return RuntimeParserAgl(sentence, false, s0.stateSet, skipStateSet, cacheSkip, goalRule, scanner, _issues)
    }

    private fun addParseIssue(
        sentence: Sentence,
        lastLocation: InputLocation,
        nextExpected: List<RuntimeSpine>
    ) {
        val expectedTerminals = nextExpected.flatMap { it.expectedNextTerminals }.toSet()
        val expected = expectedTerminals.map { it.tag }.toSet()
        val errorPos = lastLocation.position + lastLocation.length
        val errorLength = 1 //TODO: determine a better length
        val location = sentence.locationFor(errorPos, errorLength)

        val p = nextExpected.map { it.elements.last().rule.tag }.toSet().sorted().joinToString(separator = " | ")
        val contextInText = sentence.contextInText(location.position)
        val message = "Failed to match {$p} at: $contextInText"

        this._issues.error(location, message, expected)
    }

    private fun findNextExpectedAfterError3(
        sentence: Sentence,
        options: ParseOptions,
        failedParseReasons: List<FailedParseReason>,
        automatonKind: AutomatonKind,
        stateSet: ParserStateSet,
        failedAtPosition: Int
    ): Pair<InputLocation, Set<RuntimeSpineDefault>> {

        val failedReasonsAtPos = failedParseReasons
        val maxReasons = failedReasonsAtPos.filter { fr ->
            when (fr) {
                is FailedParseReasonLookahead -> {
                    when (fr.attemptedAction) {
                        // don't use this unless the transition.to matches text at the current position
                        ParseAction.WIDTH -> {
                            val rhs = fr.transition.target.firstRule.rhs
                            when {
                                rhs.rule.isEmptyTerminal -> true
                                rhs.rule.isEmptyListTerminal -> true
                                else -> (rhs as RuntimeRuleRhsTerminal).matchable!!.isLookingAt(sentence, fr.position)
                            }
                        }

                        else -> true
                    }
                }

                else -> true
            }
        }
        val x: List<Pair<InputLocation, RuntimeSpineDefault>> = maxReasons.map { fr ->
            when (fr) {
                is FailedParseReasonWidthTo -> Pair(
                    sentence.locationFor(fr.failedAtPosition, 0),
                    RuntimeSpineDefault(fr.head, fr.gssSnapshot, setOf(fr.transition.target.firstRule), fr.head.numNonSkipChildren)
                )

                is FailedParseExpectedSkipAfter -> TODO()

                is FailedParseReasonGraftRTG -> {
                    val exp = fr.transition.runtimeGuard.expectedWhenFailed(fr.prevNumNonSkipChildren)
                    Pair(sentence.locationFor(fr.failedAtPosition, 0), RuntimeSpineDefault(fr.head, fr.gssSnapshot, exp, fr.head.numNonSkipChildren))
                }

                is FailedParseReasonLookahead -> {
                    val expected: Set<RuntimeRule> = fr.possibleEndOfText.flatMap { eot ->
                        fr.runtimeLhs.flatMap { rt ->
                            fr.transition.lookaheadGuard.flatMap { lh ->
                                lh.guardLookaheadSet.resolve(eot, rt).fullContent
                            }
                        }
                    }.toSet()
                    val terms = stateSet.usedTerminalRules
                    val embeddedSkipTerms = stateSet.embeddedRuntimeRuleSet.flatMap { it.skipTerminals }.toSet()
                    val exp = expected.minus(embeddedSkipTerms.minus(terms))
                    Pair(sentence.locationFor(fr.failedAtPosition, 0), RuntimeSpineDefault(fr.head, fr.gssSnapshot, exp, fr.head.numNonSkipChildren + 1))
                }

                is FailedParseReasonEmbedded -> {
                    // Outer skip terms are part of the 'possibleEndOfText' and thus could be in the expected terms
                    // if these skip terms are not part of the embedded 'normal' terms...remove them
                    val embeddedRhs = fr.transition.target.runtimeRules.first().rhs as RuntimeRuleRhsEmbedded // should only ever be one
                    val embeddedStateSet = embeddedRhs.embeddedRuntimeRuleSet.fetchStateSetFor(embeddedRhs.embeddedStartRule.tag, automatonKind)
                    val x = findNextExpectedAfterError3(sentence, options, fr.embededFailedParseReasons, automatonKind, embeddedStateSet, failedAtPosition)
                    val embeddedRuntimeRuleSet = embeddedRhs.embeddedRuntimeRuleSet
                    val embeddedTerms = embeddedRuntimeRuleSet.fetchStateSetFor(embeddedRhs.embeddedStartRule.tag, automatonKind).usedTerminalRules
                    val skipTerms = runtimeRuleSet.skipParserStateSet?.usedTerminalRules ?: emptySet()
                    val exp = x.second.flatMap { it.expectedNextTerminals }.minus(skipTerms.minus(embeddedTerms)).toSet()
                    Pair(x.first, RuntimeSpineDefault(fr.head, fr.gssSnapshot, exp, fr.head.numNonSkipChildren))
                }
            }
        }
        val y = x.groupBy { it.first.position }
        return y[failedAtPosition]?.let { Pair(it.first().first, it.map { it.second }.toSet()) }
            ?: Pair(InputLocation(failedAtPosition, 0, 0, 1, options.sentenceIdentity.invoke()), emptySet())

    }

    override fun expectedAt(sentenceText: String, position: Int, options: ParseOptions): ExpectedAtResult {
        return try {
            expectedAtUninterrupted(sentenceText, position, options)
        } catch (e: ParserInterruptedException) {
            // terminated on request: nothing is expected
            ExpectedAtResultDefault(position, emptySet())
        }
    }

    private fun expectedAtUninterrupted(sentenceText: String, position: Int, options: ParseOptions): ExpectedAtResult {
        // the text between a word start and position (if any) could be either
        //  1) an unfinished token - so expected terminals are those at the word start, that could start with the partial word
        //  2) a complete token - so expected terminals are those after the partial word
        // the result is the union of both, each spine records the position it was computed for
        val wordStarts = findWordStartPositions(sentenceText, position, options)
        val wordStartPosition = wordStarts.last()
        val unfinished = wordStarts.flatMap { ws ->
            val atWordStart = expectedSpinesAt(sentenceText, ws, options)
            if (ws == position) {
                atWordStart.map { RuntimeSpineAt(it, ws, it.expectedNextTerminals.filterIsInstance<RuntimeRule>().toSet()) }
            } else {
                val partial = sentenceText.substring(ws, position)
                atWordStart.mapNotNull { sp ->
                    val exp = sp.expectedNextTerminals.filterIsInstance<RuntimeRule>().filter { rr ->
                        when {
                            rr.isLiteral -> rr.unescapedTerminalValue.startsWith(partial)
                            rr.isPattern -> true // pattern might match the partial word, keep it
                            rr.isTerminal -> false // e.g. <EOT>, <EMPTY> cannot be the partial word
                            else -> true
                        }
                    }.toSet()
                    when {
                        exp.isEmpty() -> null
                        else -> RuntimeSpineAt(sp, ws, exp)
                    }
                }
            }
        }.toSet()
        val complete = when {
            wordStartPosition == position -> emptySet()
            else -> expectedSpinesAt(sentenceText, position, options).map { RuntimeSpineAt(it, position, it.expectedNextTerminals.filterIsInstance<RuntimeRule>().toSet()) }.toSet()
        }
        return ExpectedAtResultDefault(wordStartPosition, unfinished + complete)
    }

    /**
     * possible start positions (ascending) of a word that might be unfinished at 'position'.
     * When finding the word start by skip rules, the text after the last skip match (or from 0 if there are no skip rules)
     * is scanned into tokens, and the start of the last token (or of all tokens, see [ParseOptions.expectedAtFromAllTokenStarts]) is used.
     * The last entry is the start of the last token.
     */
    private fun findWordStartPositions(sentenceText: String, position: Int, options: ParseOptions): List<Int> {
        return when {
            options.reverseFindWordStartBySkipRules -> {
                val matchables = this.runtimeRuleSet.skipParserStateSet?.let {
                    it.usedTerminalRules.mapNotNull { (it.rhs as RuntimeRuleRhsTerminal).matchable }
                }
                val afterSkip = when (matchables) {
                    null -> 0
                    else -> position - (findLastMatchOf2(sentenceText.substring(0, position), matchables) ?: position)
                }
                val tokenStarts = when {
                    afterSkip >= position -> emptyList()
                    else -> {
                        scanner.reset()
                        val sr = scanner.scan(SentenceDefault(sentenceText.substring(0, position), options.sentenceIdentity.invoke()), ScanOptionsDefault(startAtPosition = afterSkip))
                        sr.allTokens.map { it.position }.filter { it in afterSkip until position }.distinct().sorted()
                    }
                }
                when {
                    tokenStarts.isEmpty() -> listOf(afterSkip)
                    options.expectedAtFromAllTokenStarts -> tokenStarts
                    else -> listOf(tokenStarts.last())
                }
            }

            else -> listOf(position - findReverseStartPosition(sentenceText, position, options))
        }
    }

    /**
     * parse the sentence from 0 up to 'usedPosition' (ignoring the rest of the text)
     * and return the spines of the parse that fail at that position, i.e. what is expected next.
     * If the text up to 'usedPosition' is a complete sentence, a spine expecting <EOT> is also returned.
     */
    private fun expectedSpinesAt(sentenceText: String, usedPosition: Int, options: ParseOptions): Set<RuntimeSpine> {
        val goalRuleName = options.goalRuleName ?: error("Must define a goal rule in options")
        val cacheSkip = options.cacheSkip
        val usedText = sentenceText.substring(0, usedPosition) // parse from start (0) to position - ignore rest of text
        scanner.reset()
        val rp = createRuntimeParser(SentenceDefault(usedText, options.sentenceIdentity.invoke()), goalRuleName, scanner, automatonKind, cacheSkip)
        this.runtimeParser = rp

        val possibleEndOfText = setOf(LookaheadSet.EOT)
        val parseArgs = RuntimeParserAgl.forExpectedAt
        val startSkipFailures = rp.start(0, possibleEndOfText, parseArgs)
        var seasons = 1

        while (rp.graph.canGrow && (rp.graph.goals.isEmpty() || rp.graph.goalMatchedAll.not())) {
            rp.grow3(possibleEndOfText, parseArgs) //TODO: maybe no need to build tree!
            seasons++
        }
        // if the text parsed is a complete sentence, then end-of-text is a valid next 'terminal'
        val eotSpines: Set<RuntimeSpine> = when {
            rp.graph.goals.isNotEmpty() && rp.graph.goalMatchedAll -> setOf(RuntimeSpineEndOfText(rp.stateSet.finishRulePosition, usedPosition))
            else -> emptySet()
        }
        return eotSpines + if (rp.failedReasons.isEmpty()) {
            emptySet()
        } else {
            // need to include the 'startSkipFailures',
            val map = rp.failedReasons //no need to clone it as it will not be modified after this point
            startSkipFailures.forEach {
                val l = map[it.key] ?: mutableListOf()
                l.addAll(it.value)
                map[it.key] = l
            }
            val validFailReasons = map[map.keys.max()]?.flatMap {
                when (it) {
                    is FailedParseReasonEmbedded -> it.embededFailedParseReasons.filter { it.skipFailure.not() && it.failedAtPosition == usedPosition }
                    else -> when {
                        it.skipFailure.not() && it.failedAtPosition == usedPosition -> listOf(it)
                        else -> emptyList()
                    }
                }
            } ?: emptyList()
            // parse might fail BEFORE the requested 'position' - so filter to get {} if it does
            validFailReasons.map { it.spine }.toSet()
        }
    }

    override fun expectedTerminalsAt(sentenceText: String, position: Int, options: ParseOptions): Set<Rule> {
        val res = this.expectedAt(sentenceText, position, options)
        return res.spines.flatMap { it.expectedNextTerminals }.flatMap {
            when {
                it.isTerminal -> listOf(it)
                //RuntimeRuleKind.NON_TERMINAL -> this.runtimeRuleSet.firstTerminals[it.ruleNumber]
                else -> emptyList() // TODO()
            }
        }.toSet()
    }

    private fun findReverseStartPosition(sentenceText: String, position: Int, options: ParseOptions): Int {
        val previousText = sentenceText.substring(0, position)
        return when {
            options.reverseFindWordStartBySkipRules -> {
                //TODO: actually parse rather than simply OR the regexes
                val matchables = this.runtimeRuleSet.skipParserStateSet?.let {
                    it.usedTerminalRules.mapNotNull { (it.rhs as RuntimeRuleRhsTerminal).matchable }
                }
                when (matchables) {
                    null -> 0
                    else -> {
                        val revPos = findLastMatchOf2(previousText, matchables) ?: position
                        revPos
                    }
                }
            }

            null != options.reverseFindWordStartRegex -> {
                val regex = options.reverseFindWordStartRegex!!.toRegex()
                previousText.reversed().let {
                    regex.find(it)?.range?.start
                } ?: 0//position
            }

            else -> 0
        }
    }

    private fun findLastMatchOf(text:String, matchables: List<Matchable>):Int ?{
        //Does not really work, as regExs are not reversed
        val str = matchables.joinToString(separator = "|") {
            when (it.kind) {
                MatchableKind.EOT -> ""
                else -> it.expression.escapedForRegex
            }
        }
        val regex = str.toRegex()
        return text.reversed().let {
            regex.find(it)?.range?.start
        }
    }

    /**
     * Finds the start of the word being typed at the end of [text]:
     * the end of the last skip match before the end of [text].
     * Returns how many characters before the end of [text] the word starts,
     * or null if no skip text is found (i.e. the word starts at the beginning of [text]).
     */
    private fun findLastMatchOf2(text: String, matchables: List<Matchable>): Int? {
        val exprs = matchables
            .filter { it.kind != MatchableKind.EOT }
            .map { "(${it.expression.escapedForRegex})" }
        if (exprs.isEmpty()) return null
        val regex = exprs.joinToString(separator = "|").toRegex()
        var pos = text.length - 1
        while (pos >= 0) {
            val match = regex.matchAt(text, pos)
            if (null != match && match.value.isNotEmpty()) {
                val wordStart = pos + match.value.length
                return text.length - wordStart
            }
            pos -= 1
        }
        return null
    }
}