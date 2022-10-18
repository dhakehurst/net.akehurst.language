package net.akehurst.language.agl.agl.grammar.grammar

import net.akehurst.language.api.grammar.*

/**
 * creates the names of pseudo rules for a real rule.
 * It is necessary that the names of the pseudo rules, used
 * when constructing the parse tree, map to the names of pseudo rule types
 * constructed as part of the TypeModelFromGrammar.
 * So that if a property type is 'ANY' then the actual type
 * for the parse tree node can be deduced.
 */
internal class PseudoRuleNames(val grammar: Grammar) {

    private val _nextGroupNumber = mutableMapOf<String, Int>()
    private val _nextChoiceNumber = mutableMapOf<String, Int>()
    private val _nextSimpleListNumber = mutableMapOf<String, Int>()
    private val _nextSeparatedListNumber = mutableMapOf<String, Int>()

    private val _nameForRuleItem = mutableMapOf<RuleItem, String>()
    private val _itemForPseudoRuleName = mutableMapOf<String, RuleItem>()

    init {
        grammar.allNonTerminalRule.forEach {
            val pseudoRuleNames = pseudoRulesFor(it.rhs)
            pseudoRuleNames.forEach {
                _nameForRuleItem[it.first] = it.second
                _itemForPseudoRuleName[it.second] = it.first
            }
        }
    }

    fun nameForRuleItem(item: RuleItem): String {
        return _nameForRuleItem[item] ?: error("Internal Error: name for RuleItem '$item' not found")
    }

    fun itemForPseudoRuleName(name: String): RuleItem {
        return _itemForPseudoRuleName[name] ?: error("Internal Error: RuleItem with name '$name' not found")
    }

    private fun pseudoRulesFor(item: RuleItem): Set<Pair<RuleItem, String>> {
        return when (item) {
            is Choice ->item.alternative.flatMap { pseudoRulesFor(it) }.toSet()

            is Concatenation -> when (item.items.size) {
                1 -> item.items.flatMap { pseudoRulesFor(it) }.toSet()
                else -> item.items.flatMap { pseudoRulesFor(it) }.toSet() + Pair(item, createChoiceRuleName(item.owningRule.name))
            }

            is ConcatenationItem -> when (item) {
                is SimpleItem -> when (item) {
                    is Group -> pseudoRulesFor(item.choice) + Pair(item, createGroupRuleName(item.owningRule.name))
                    is TangibleItem -> emptySet()
                    else -> error("Internal Error: subtype of ${SimpleItem::class.simpleName} ${item::class.simpleName} not handled")
                }

                is ListOfItems -> when (item) {
                    is SimpleList -> pseudoRulesFor(item.item) + Pair(item, createSimpleListRuleName(item.owningRule.name))
                    is SeparatedList -> pseudoRulesFor(item.item) + pseudoRulesFor(item.separator) + Pair(item, createSimpleListRuleName(item.owningRule.name))
                    else -> error("Internal Error: subtype of ${ListOfItems::class.simpleName} ${item::class.simpleName} not handled")
                }

                else -> error("Internal Error: subtype of ${ConcatenationItem::class.simpleName} ${item::class.simpleName} not handled")
            }

            else -> error("Internal Error: subtype of ${RuleItem::class.simpleName} ${item::class.simpleName} not handled")
        }
    }

    private fun createGroupRuleName(parentRuleName: String): String {
        var n = _nextGroupNumber[parentRuleName] ?: 0
        n++
        _nextGroupNumber[parentRuleName] = n
        return "§${parentRuleName.removePrefix("§")}§group$n" //TODO: include original rule name fo easier debug
    }

    private fun createChoiceRuleName(parentRuleName: String): String {
        var n = _nextChoiceNumber[parentRuleName] ?: 0
        n++
        _nextChoiceNumber[parentRuleName] = n
        return "§${parentRuleName.removePrefix("§")}§choice$n" //TODO: include original rule name fo easier debug
    }

    private fun createSimpleListRuleName(parentRuleName: String): String {
        var n = _nextSimpleListNumber[parentRuleName] ?: 0
        n++
        _nextSimpleListNumber[parentRuleName] = n
        return "§${parentRuleName.removePrefix("§")}§multi$n" //TODO: include original rule name fo easier debug
    }

    private fun createSeparatedListRuleName(parentRuleName: String): String {
        var n = _nextSeparatedListNumber[parentRuleName] ?: 0
        n++
        _nextSeparatedListNumber[parentRuleName] = n
        return "§${parentRuleName.removePrefix("§")}§sepList$n" //TODO: include original rule name fo easier debug
    }

}