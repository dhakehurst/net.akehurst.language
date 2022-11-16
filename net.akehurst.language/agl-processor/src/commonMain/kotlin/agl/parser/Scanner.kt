package net.akehurst.language.agl.agl.parser

import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.sppt.SPPTLeafFromInput
import net.akehurst.language.api.sppt.SPPTLeaf

internal class Scanner(
    internal val runtimeRuleSet:RuntimeRuleSet
) {

    fun scan(inputText: String, includeSkipRules: Boolean): List<SPPTLeaf> {
        val undefined = RuntimeRuleSet.UNDEFINED_RULE
        //TODO: improve this algorithm...it is not efficient I think, also doesn't work!
        val input = InputFromString(this.runtimeRuleSet.terminalRules.size, inputText)
        var terminals = if (includeSkipRules) this.runtimeRuleSet.terminalRules else this.runtimeRuleSet.nonSkipTerminals
        val result = mutableListOf<SPPTLeaf>()

        //eliminate tokens that are empty matches
        terminals = terminals.filter { it.isEmptyTerminal.not() }

        var startPosition = 0
        var nextInputPosition = 0
        while (!input.isEnd(nextInputPosition)) {
            val matches: List<SPPTLeaf> = terminals.mapNotNull {
                val match = input.tryMatchText(nextInputPosition, it)
                if (null == match) {
                    null
                } else {
                    val ni = nextInputPosition + match.matchedText.length
                    val leaf = SPPTLeafFromInput(input, it, startPosition, ni, (if (it.isPattern) 0 else 1))
                    //leaf.eolPositions = match.eolPositions
                    leaf
                }
            }
            // prefer literals over patterns
            val longest = matches.maxWithOrNull(Comparator<SPPTLeaf> { l1, l2 ->
                when {
                    l1.isLiteral && l2.isPattern -> 1
                    l1.isPattern && l2.isLiteral -> -1
                    else -> when {
                        l1.matchedTextLength > l2.matchedTextLength -> 1
                        l2.matchedTextLength > l1.matchedTextLength -> -1
                        else -> 0
                    }
                }
            })
            when {
                (null == longest || longest.matchedTextLength == 0) -> {
                    //TODO: collate unscanned, rather than make a separate token for each char
                    val text = inputText[nextInputPosition].toString()
                    nextInputPosition += text.length
                    val eolPositions = emptyList<Int>()//TODO calulat
                    val unscanned = SPPTLeafFromInput(input, undefined, startPosition, nextInputPosition, 0)
                    //unscanned.eolPositions = input.eolPositions(text)
                    result.add(unscanned)
                }
                else -> {
                    result.add(longest)
                    nextInputPosition += longest.matchedTextLength
                }
            }
            startPosition = nextInputPosition
        }
        return result
    }


}