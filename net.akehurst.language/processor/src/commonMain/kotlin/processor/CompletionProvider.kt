package net.akehurst.language.processor

import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.processor.CompletionItem

class CompletionProvider {

	fun provideFor(item: RuleItem, desiredDepth: Int): List<CompletionItem> {
		//TODO:
		return emptyList<CompletionItem>()
	}
}