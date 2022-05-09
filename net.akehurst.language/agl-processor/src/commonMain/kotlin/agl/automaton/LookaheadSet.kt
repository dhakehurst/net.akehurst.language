package net.akehurst.language.agl.automaton

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet

internal data class Lookahead(
    val guard: LookaheadSet,
    val up: LookaheadSet
) {
    companion object {
        val EMPTY = Lookahead(LookaheadSet.EMPTY, LookaheadSet.EMPTY)
    }
}

internal class LookaheadSet(
    val number: Int,
    val includesUP: Boolean,
    val includesEOT: Boolean,
    val matchANY: Boolean,
    val content: Set<RuntimeRule>
) {
    companion object {
        val EMPTY = LookaheadSet(-1, false, false, false, emptySet())
        val ANY = LookaheadSet(-2, false, false, true, emptySet())
        val EOT = LookaheadSet(-3, false, true, false, emptySet())
        val UP = LookaheadSet(-4, true, false, false, emptySet())
        val UNCACHED_NUMBER = -5

        fun createFromRuntimeRules(automaton: ParserStateSet, fullContent:Set<RuntimeRule>): LookaheadSet {
            return when {
                fullContent.isEmpty() -> LookaheadSet.EMPTY
                else -> {
                    val includeUP = fullContent.contains(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
                    val includeEOT = fullContent.contains(RuntimeRuleSet.END_OF_TEXT)
                    val matchAny = fullContent.contains(RuntimeRuleSet.ANY_LOOKAHEAD)
                    val content = fullContent.minus(RuntimeRuleSet.USE_PARENT_LOOKAHEAD).minus(RuntimeRuleSet.END_OF_TEXT).minus(RuntimeRuleSet.ANY_LOOKAHEAD)
                    automaton.createLookaheadSet(includeUP, includeEOT, matchAny, content)
                }
            }
        }
    }

    val fullContent:Set<RuntimeRule> get() {
        val c = mutableSetOf<RuntimeRule>()
        if (this.includesUP) c.add(RuntimeRuleSet.USE_PARENT_LOOKAHEAD)
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

    val part get() = LookaheadSetPart(this.includesUP, this.includesEOT, this.matchANY, this.content)

    /**
     * runtimeLookahead must not include UP
     * replace UP in this with runtimeLookahead
     */
    fun resolveUP(runtimeLookahead: LookaheadSet): LookaheadSetPart {
        return when {
            runtimeLookahead.includesUP -> error("Runtime lookahead must be real lookahead values") //TODO: could remove this for speed, it should never happen
            UP == this -> runtimeLookahead.part
            else -> {
                val content = if (this.includesUP) this.content.union(runtimeLookahead.content) else this.content
                val eol = this.includesEOT || (this.includesUP && runtimeLookahead.includesEOT)
                val ma = this.matchANY || (this.includesUP && runtimeLookahead.matchANY)
                LookaheadSetPart(false, eol, ma, content)
            }
        }
    }

    fun union(automaton: ParserStateSet, lookahead: LookaheadSet): LookaheadSet {
        val up = this.includesUP || lookahead.includesUP
        val eol = this.includesEOT || lookahead.includesEOT
        val ma = this.matchANY || lookahead.matchANY
        return automaton.createLookaheadSet(up, eol, ma, this.content.union(lookahead.content))
    }
    fun unionContent(automaton: ParserStateSet, additionalContent: Set<RuntimeRule>): LookaheadSet {
        val up = this.includesUP
        val eol = this.includesEOT
        val ma = this.matchANY
        return automaton.createLookaheadSet(up, eol, ma, this.content.union(additionalContent))
    }

    override fun hashCode(): Int = number
    override fun equals(other: Any?): Boolean = when {
        other is LookaheadSet -> this.number == other.number
        else -> false
    }

    override fun toString(): String = when {
        this == EMPTY -> "LookaheadSet{$number,[EMPTY]}"
        this == ANY -> "LookaheadSet{$number,[ANY]}"
        this == UP -> "LookaheadSet{$number,[UP]}"
        this == EOT -> "LookaheadSet{$number,[EOT]}"
        else -> "LookaheadSet{$number,${this.fullContent.joinToString(prefix = "[", postfix = "]", separator = ",") { it.tag }}}"
    }

}