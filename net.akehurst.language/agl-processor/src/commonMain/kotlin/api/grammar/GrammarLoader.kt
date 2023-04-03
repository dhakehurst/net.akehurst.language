package net.akehurst.language.api.grammar

interface GrammarLoader {

	fun resolve(vararg qualifiedGrammarNames: String): List<Grammar>

}