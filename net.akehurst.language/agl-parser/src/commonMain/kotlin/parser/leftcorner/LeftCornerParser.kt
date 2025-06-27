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
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.scanner.api.Scanner
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
    override var cacheSkip: Boolean = true
) : ParseOptions {
    override fun clone() = ParseOptionsDefault(
        enabled = enabled,
        goalRuleName = goalRuleName,
        sentenceIdentity = sentenceIdentity,
        reportErrors = reportErrors,
        reportGrammarAmbiguities = reportGrammarAmbiguities,
        cacheSkip = cacheSkip
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
        val startSkipFailures = rp.start(0, possibleEndOfText, parseArgs)
        var seasons = 1
        var maxNumHeads = rp.graph.numberOfHeads
        var totalWork = maxNumHeads

        while (rp.graph.hasNextHead && (rp.graph.goals.isEmpty() || rp.graph.goalMatchedAll.not())) {
            if (Debug.OUTPUT_RUNTIME) println("season $seasons ===================================")
            val steps = rp.grow3(possibleEndOfText, parseArgs)
            seasons += steps
            maxNumHeads = max(maxNumHeads, rp.graph.numberOfHeads)
            totalWork += rp.graph.numberOfHeads
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

            // find embedded failures
            val map2 = map1.values.flatten().flatMap { v ->
                when(v) {
                    is FailedParseReasonEmbedded -> v.embededFailedParseReasons.map { it }
                    else -> listOf(v)
                }
            }.groupBy { it.failedAtPosition }

            if (map2.isNotEmpty()) {
                val failedAtPosition = map2.keys.max()
                val nextExpected = map2[failedAtPosition]?.filter { it.skipFailure.not() }?.map { it.spine } ?: emptyList()
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
                        // don't use this unless the transition.to matches text at current position
                        ParseAction.WIDTH -> {
                            val rhs = fr.transition.to.firstRule.rhs
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
                    RuntimeSpineDefault(fr.head, fr.gssSnapshot, setOf(fr.transition.to.firstRule), fr.head.numNonSkipChildren)
                )

                is FailedParseExpectedSkipAfter -> TODO()

                is FailedParseReasonGraftRTG -> {
                    val exp = fr.transition.runtimeGuard.expectedWhenFailed(fr.prevNumNonSkipChildren)
                    Pair(sentence.locationFor(fr.failedAtPosition, 0), RuntimeSpineDefault(fr.head, fr.gssSnapshot, exp, fr.head.numNonSkipChildren))
                }

                is FailedParseReasonLookahead -> {
                    val expected: Set<RuntimeRule> = fr.possibleEndOfText.flatMap { eot ->
                        fr.runtimeLhs.flatMap { rt ->
                            fr.transition.lookahead.flatMap { lh ->
                                lh.guard.resolve(eot, rt).fullContent
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
                    val embeddedRhs = fr.transition.to.runtimeRules.first().rhs as RuntimeRuleRhsEmbedded // should only ever be one
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
            ?: Pair(InputLocation(failedAtPosition, 0, 0, 1,options.sentenceIdentity?.invoke()), emptySet())

    }

    override fun expectedAt(sentenceText: String, position: Int, options: ParseOptions): ExpectedAtResult {
        val goalRuleName = options.goalRuleName ?: error("Must define a goal rule in options")
        val cacheSkip = options.cacheSkip

        val previousText = sentenceText.substring(0, position)
        val revWordStartPosition = previousText.reversed().let {
            Regex("\\s+").find(it)?.range?.start //TODO: use scanner to find this using skip terminals
        } ?: position

        val wordStartPosition = position - revWordStartPosition
        val usedText = sentenceText.substring(0, wordStartPosition) // parse from start (0) to position - ignore rest of text
        scanner.reset()
        val rp = createRuntimeParser(SentenceDefault(usedText, options.sentenceIdentity?.invoke()), goalRuleName, scanner, automatonKind, cacheSkip)
        this.runtimeParser = rp

        val possibleEndOfText = setOf(LookaheadSet.EOT)
        val parseArgs = RuntimeParserAgl.forExpectedAt
        val startSkipFailures = rp.start(0, possibleEndOfText, parseArgs)
        var seasons = 1

        while (rp.graph.canGrow && (rp.graph.goals.isEmpty() || rp.graph.goalMatchedAll.not())) {
            rp.grow3(possibleEndOfText, parseArgs) //TODO: maybe no need to build tree!
            seasons++
        }
        /*
        return rp.nextExpected.flatMap {
            when(it) {
                is RuntimeRule -> listOf(it)
                is Set<*> -> it as Set<RuntimeRule>
                is List<*> -> it as List<RuntimeRule>
                is LookaheadSet -> it.fullContent
                else -> error("Internl Error: Not handled - $it")
            }
        }.toSet()
         */
        //val nextExpected = this.findNextExpected(rp, matches, possibleEndOfText)
        return if (rp.failedReasons.isEmpty()) {
            ExpectedAtResultDefault(wordStartPosition,emptySet())
        } else {
            // need to include the 'startSkipFailures',
            val map = rp.failedReasons //no need to clone it as it will not be modified after this point
            startSkipFailures.forEach {
                val l = map[it.key] ?: mutableListOf()
                l.addAll(it.value)
                map[it.key] = l
            }
            //val nextExpected = this.findNextExpectedAfterError3(scanner.sentence, map, rp.stateSet.automatonKind, rp.stateSet, position)
            //nextExpected.second
            val validFailReasons = map[map.keys.max()]?.flatMap {
                when (it) {
                    is FailedParseReasonEmbedded -> it.embededFailedParseReasons.filter { it.skipFailure.not() && it.failedAtPosition == wordStartPosition }
                    else -> when {
                        it.skipFailure.not() && it.failedAtPosition == wordStartPosition -> listOf(it)
                        else -> emptyList()
                    }
                }
            } ?: emptyList()
            // parse might fail BEFORE the requested 'position' - so filter to get {} if it does
            val rspines = validFailReasons.map { it.spine }
            return ExpectedAtResultDefault(wordStartPosition,rspines.toSet())
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
}