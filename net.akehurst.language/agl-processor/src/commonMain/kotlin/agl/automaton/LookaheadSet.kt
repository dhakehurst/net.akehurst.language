package net.akehurst.language.agl.automaton

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet

internal class Lookahead(
    val guard: LookaheadSet,
    val up: LookaheadSet
) {
    companion object {
        val EMPTY = Lookahead(LookaheadSet.EMPTY, LookaheadSet.EMPTY)
        fun merge(automaton: ParserStateSet, initial: Set<Lookahead>): Set<Lookahead> {
            return when (initial.size) {
                1 -> initial
                else -> {
                    val merged = initial
                        .groupBy { it.up }
                        .map { me2 ->
                            val up = me2.key
                            val guard = me2.value.map { it.guard }.reduce { acc, l -> acc.union(automaton, l) }
                            Lookahead(guard, up)
                        }.toSet()
                        .groupBy { it.guard }
                        .map { me ->
                            val guard = me.key
                            val up = me.value.map { it.up }.reduce { acc, l -> acc.union(automaton, l) }
                            Lookahead(guard, up)
                        }.toSet()
                    when (merged.size) {
                        1 -> merged
                        else -> {
                            val sortedMerged = merged.sortedByDescending { it.guard.fullContent.size }
                            val result = mutableSetOf<Lookahead>()
                            val mergedIntoOther = mutableSetOf<Lookahead>()
                            for (i in sortedMerged.indices) {
                                var lh1 = sortedMerged[i]
                                if (mergedIntoOther.contains(lh1)) {
                                    //do nothing
                                } else {
                                    for (j in i + 1 until sortedMerged.size) {
                                        val lh2 = sortedMerged[j]
                                        if (lh1.guard.containsAll(lh2.guard)) {
                                            lh1 = Lookahead(lh1.guard, lh1.up.union(automaton, lh2.up))
                                            mergedIntoOther.add(lh2)
                                        } else {
                                            //result.add(lh2)
                                        }
                                    }
                                    result.add(lh1)
                                }
                            }
                            result
                        }
                    }
                }
            }
        }
    }

    override fun hashCode(): Int = arrayOf(guard, up).contentHashCode()
    override fun equals(other: Any?): Boolean = when {
        other !is Lookahead -> false
        this.up != other.up -> false
        this.guard != other.guard -> false
        else -> true
    }

    override fun toString(): String = "LH($guard, $up)"
}

internal class LookaheadSet(
    val number: Int,
    val includesRT: Boolean,
    val includesEOT: Boolean,
    val matchANY: Boolean,
    val content: Set<RuntimeRule>
) {
    companion object {
        val EMPTY = LookaheadSet(-1, false, false, false, emptySet())
        val ANY = LookaheadSet(-2, false, false, true, emptySet())
        val EOT = LookaheadSet(-3, false, true, false, emptySet())
        val RT = LookaheadSet(-4, true, false, false, emptySet())
        val RT_EOT = LookaheadSet(-5, true, true, false, emptySet())
        val UNCACHED_NUMBER = -6

        fun createFromRuntimeRules(automaton: ParserStateSet, fullContent: Set<RuntimeRule>): LookaheadSet {
            return when {
                fullContent.isEmpty() -> LookaheadSet.EMPTY
                else -> {
                    val includeUP = fullContent.contains(RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD)
                    val includeEOT = fullContent.contains(RuntimeRuleSet.END_OF_TEXT)
                    val matchAny = fullContent.contains(RuntimeRuleSet.ANY_LOOKAHEAD)
                    val content = fullContent.minus(RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD).minus(RuntimeRuleSet.END_OF_TEXT).minus(RuntimeRuleSet.ANY_LOOKAHEAD)
                    automaton.createLookaheadSet(includeUP, includeEOT, matchAny, content)
                }
            }
        }
    }

    val fullContent: Set<RuntimeRule>
        get() {
            val c = mutableSetOf<RuntimeRule>()
            if (this.includesRT) c.add(RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD)
            if (this.includesEOT) c.add(RuntimeRuleSet.END_OF_TEXT)
            if (this.matchANY) c.add(RuntimeRuleSet.ANY_LOOKAHEAD)
            c.addAll(this.content)
            return c
        }

    val regex by lazy {
        val str = this.content.joinToString(prefix = "(", separator = ")|(", postfix = ")") {
            if (it.isPattern) it.value else "\\Q${it.value}\\E"
        }
        Regex(str)
    }

    val part get() = LookaheadSetPart(this.includesRT, this.includesEOT, this.matchANY, this.content)

    /**
     * eotLookahead && runtimeLookahead must not include RT
     * replace RT in this with runtimeLookahead
     * replace EOT in this with eotLookahead
     */
    fun resolve(eotLookahead: LookaheadSet, runtimeLookahead: LookaheadSet): LookaheadSetPart {
        return when {
            eotLookahead.includesRT -> error("EOT lookahead must be real lookahead values") //TODO: could remove this for speed, it should never happen
            runtimeLookahead.includesRT -> error("EOT lookahead must be real lookahead values") //TODO: could remove this for speed, it should never happen
            //EOT == this -> eotLookahead.part
            //RT == this -> runtimeLookahead.part
            else -> {
                var resolvedContent = this.content
                resolvedContent = if (this.includesRT) {
                    val resolvedRT = if(runtimeLookahead.includesEOT) runtimeLookahead.content.union(eotLookahead.content) else runtimeLookahead.content
                    resolvedContent.union(resolvedRT)
                } else {
                    resolvedContent
                }
                resolvedContent = if (this.includesEOT) resolvedContent.union(eotLookahead.content) else resolvedContent
                val eot = this.includesEOT || (this.includesRT && runtimeLookahead.includesEOT)
                val ma = this.matchANY || (this.includesEOT && eotLookahead.matchANY) || (this.includesRT && runtimeLookahead.matchANY)
                LookaheadSetPart(false, eot, ma, resolvedContent)
            }
        }
    }

    fun union(automaton: ParserStateSet, lookahead: LookaheadSet): LookaheadSet {
        val rt = this.includesRT || lookahead.includesRT
        val eot = this.includesEOT || lookahead.includesEOT
        val ma = this.matchANY || lookahead.matchANY
        return automaton.createLookaheadSet(rt, eot, ma, this.content.union(lookahead.content))
    }

    fun unionContent(automaton: ParserStateSet, additionalContent: Set<RuntimeRule>): LookaheadSet {
        val rt = this.includesRT
        val eot = this.includesEOT
        val ma = this.matchANY
        return automaton.createLookaheadSet(rt, eot, ma, this.content.union(additionalContent))
    }

    fun containsAll(other: LookaheadSet): Boolean = when {
        this.matchANY -> true
        this.includesEOT.not() && other.includesEOT -> false
        this.includesRT.not() && other.includesRT -> false
        else -> this.fullContent.containsAll(other.fullContent)
    }

    override fun hashCode(): Int = number
    override fun equals(other: Any?): Boolean = when {
        other is LookaheadSet -> this.number == other.number
        else -> false
    }

    override fun toString(): String = when {
        this == EMPTY -> "LookaheadSet{$number,[EMPTY]}"
        this == ANY -> "LookaheadSet{$number,[ANY]}"
        this == RT -> "LookaheadSet{$number,[RT]}"
        this == EOT -> "LookaheadSet{$number,[EOT]}"
        else -> "LookaheadSet{$number,${this.fullContent.joinToString(prefix = "[", postfix = "]", separator = ",") { it.tag }}}"
    }

}